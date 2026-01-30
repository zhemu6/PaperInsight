package com.zhemu.paperinsight.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhemu.paperinsight.agent.core.ChatAgent;
import com.zhemu.paperinsight.annotation.AuthCheck;
import com.zhemu.paperinsight.model.dto.chat.ChatEvent;
import com.zhemu.paperinsight.model.vo.ChatHistoryMessageVO;
import com.zhemu.paperinsight.exception.ErrorCode;
import com.zhemu.paperinsight.exception.ThrowUtils;
import com.zhemu.paperinsight.service.PaperChatSessionService;
import com.zhemu.paperinsight.service.SysUserService;
import io.agentscope.core.agent.Event;
import io.agentscope.core.agent.EventType;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ThinkingBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolUseBlock;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 论文对话控制器
 * 提供 SSE 流式对话接口
 */
@Slf4j
@RestController
@RequestMapping("/assistant")
@RequiredArgsConstructor
public class ChatController {

    private final ChatAgent chatAgent;
    private final ObjectMapper objectMapper;
    private final SysUserService sysUserService;
    private final PaperChatSessionService paperChatSessionService;

    /**
     * SSE 流式聊天接口。
     *
     * @param chatId 会话 id
     * @param userQuery 用户输入的问题/指令
     * @param request 当前请求（用于获取登录用户）
     * @return SSE 事件流（JSON 格式的 ChatEvent）
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @AuthCheck
    public Flux<ServerSentEvent<String>> chatStream(
            @RequestParam String chatId,
            @RequestParam String userQuery,
            @RequestParam(required = false) String title,
            HttpServletRequest request) {
        long userId = sysUserService.getLoginUser(request).getId();
        // Verify ownership (anti-IDOR)
        if (paperChatSessionService.getOwnedSession(chatId, userId) == null) {
            return Flux.just(ServerSentEvent.builder(serializeSafely(ChatEvent.error(0, "NO_AUTH", "No permission")))
                    .build());
        }

        paperChatSessionService.ensureTitle(chatId, userId, title != null ? title : userQuery);
        paperChatSessionService.touchSession(chatId, userId);
        log.info("Chat stream request - chatId: {}, userId: {}, userQuery: {}", chatId, userId, userQuery);
        // 1. 创建一个管道 sink 和 递增序号 seq
        Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().unicast().onBackpressureBuffer();
        AtomicLong seq = new AtomicLong(0);
        // 2. 构建用户消息
        Msg msg = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text(userQuery).build())
                .build();
        // 3. 处理流失对话
        processStream(chatAgent.stream(msg, chatId, String.valueOf(userId)), sink, seq);
        // 4. 流式返回
        return sink.asFlux()
                .doOnCancel(() -> log.info("Client disconnected from stream"));
    }

    /**
     * 停止生成
     * @param chatId 对话id
     */
    @PostMapping("/stop")
    @AuthCheck
    public void stopStream(@RequestParam String chatId, HttpServletRequest request) {
        long userId = sysUserService.getLoginUser(request).getId();
        // 不属于当前对话
        if (paperChatSessionService.getOwnedSession(chatId, userId) == null) {
            return;
        }
        log.info("Received stop request for chat: {}", chatId);
        chatAgent.stop(chatId);
    }

    /**
     * 处理流式响应核心逻辑
     */
    public void processStream(Flux<Event> generator, Sinks.Many<ServerSentEvent<String>> sink, AtomicLong seq) {
        generator
                .flatMap(event -> Flux.fromIterable(convertEvent(event, seq))
                        .map(e -> ServerSentEvent.builder(serializeSafely(e)).build()))
                .doOnNext(sink::tryEmitNext)
                // 出现异常时
                .doOnError(e -> {
                    log.error("Unexpected error in stream processing: {}", e.getMessage(), e);
                    // 先发送一条错误事件
                    sink.tryEmitNext(ServerSentEvent.builder(
                            serializeSafely(ChatEvent.error(seq.incrementAndGet(), "STREAM", e.getMessage()))).build());
                    // 发送一个Complete事件
                    sink.tryEmitNext(ServerSentEvent.builder(
                            serializeSafely(ChatEvent.complete(seq.incrementAndGet()))).build());
                    // 结束sink
                    sink.tryEmitComplete();
                })
                .doOnComplete(() -> {
                    sink.tryEmitNext(ServerSentEvent.builder(
                            serializeSafely(ChatEvent.complete(seq.incrementAndGet()))).build());
                    sink.tryEmitComplete();
                })
                .subscribe();
    }

    @GetMapping("/history")
    @AuthCheck
    public List<ChatHistoryMessageVO> getHistory(@RequestParam String chatId, HttpServletRequest request) {
        long userId = sysUserService.getLoginUser(request).getId();
        ThrowUtils.throwIf(paperChatSessionService.getOwnedSession(chatId, userId) == null, ErrorCode.NO_AUTH_ERROR);

        AtomicLong seq = new AtomicLong(0);
        return chatAgent.getHistory(chatId).stream()
                // 1. 过滤掉系统消息
                .filter(msg -> msg.getRole() != null && !MsgRole.SYSTEM.equals(msg.getRole()))
                // 2. 过滤掉打断消息
                .filter(msg -> msg.getContent().stream()
                        .noneMatch(block -> block instanceof TextBlock
                                && ((TextBlock) block).getText().contains("I noticed that you have interrupted me")))
                .map(msg -> {
                    // 3.1 新建一个历史 VO 对象
                    ChatHistoryMessageVO vo = new ChatHistoryMessageVO();
                    // 3.2 设置消息 id
                    vo.setId(msg.getId());
                    // 3.3 设置用户角色
                    String role;
                    if (MsgRole.USER.equals(msg.getRole())) {
                        role = "user";
                    } else {
                        role = "assistant";
                    }
                    vo.setRole(role);
                    // 3.4 设置对话历史
                    List<ChatEvent> events = new ArrayList<>();
                    if (MsgRole.USER.equals(msg.getRole())) {
                        String text = msg.getTextContent();
                        if (text != null && !text.isBlank()) {
                            events.add(ChatEvent.text(seq.incrementAndGet(), text, false));
                        }
                    } else {
                        // assistant: keep block order
                        for (ContentBlock block : msg.getContent()) {
                            if (block instanceof ThinkingBlock tb) {
                                if (tb.getThinking() != null && !tb.getThinking().isBlank()) {
                                    events.add(ChatEvent.thinking(seq.incrementAndGet(), tb.getThinking(), false));
                                }
                            } else if (block instanceof ToolUseBlock tool) {
                                if (tool.getName() != null && tool.getName().contains("__fragment__")) {
                                    continue;
                                }
                                events.add(ChatEvent.toolUse(seq.incrementAndGet(), tool.getId(), tool.getName(),
                                        convertInput(tool.getInput())));
                            } else if (block instanceof ToolResultBlock tool) {
                                events.add(ChatEvent.toolResult(seq.incrementAndGet(), tool.getId(), tool.getName(),
                                        extractToolOutput(tool)));
                            } else if (block instanceof TextBlock tb) {
                                if (tb.getText() != null && !tb.getText().isBlank()) {
                                    events.add(ChatEvent.text(seq.incrementAndGet(), tb.getText(), false));
                                }
                            }
                        }
                    }
                    vo.setEvents(events);
                    return vo;
                })
                .filter(item -> item.getEvents() != null && !item.getEvents().isEmpty())
                .toList();
    }

    // Legacy history item records removed in favor of ChatEvent-based history.

    /**
     * 转换事件
     * @param event SSE事件
     * @param seq 递增序号
     * @return
     */
    private List<ChatEvent> convertEvent(Event event, AtomicLong seq) {
        List<ChatEvent> out = new ArrayList<>();
        Msg msg = event.getMessage();

        if (event.getType() == EventType.TOOL_RESULT) {
            for (ToolResultBlock tool : msg.getContentBlocks(ToolResultBlock.class)) {
                out.add(ChatEvent.toolResult(seq.incrementAndGet(), tool.getId(), tool.getName(), extractToolOutput(tool)));
            }
            return out;
        }

        if (event.getType() == EventType.REASONING) {
            if (event.isLast() && msg.hasContentBlocks(ToolUseBlock.class)) {
                for (ToolUseBlock tool : msg.getContentBlocks(ToolUseBlock.class)) {
                    if (tool.getName() != null && tool.getName().contains("__fragment__")) {
                        continue;
                    }
                    out.add(ChatEvent.toolUse(seq.incrementAndGet(), tool.getId(), tool.getName(), convertInput(tool.getInput())));
                }
                return out;
            }

            if (!event.isLast()) {
                for (ThinkingBlock tb : msg.getContentBlocks(ThinkingBlock.class)) {
                    if (tb.getThinking() != null && !tb.getThinking().isBlank()) {
                        out.add(ChatEvent.thinking(seq.incrementAndGet(), tb.getThinking(), true));
                    }
                }

                String text = extractText(msg);
                if (text != null && !text.isEmpty()) {
                    out.add(ChatEvent.text(seq.incrementAndGet(), text, true));
                }
            }
        }

        return out;
    }

    private String extractText(Msg msg) {
        List<TextBlock> textBlocks = msg.getContentBlocks(TextBlock.class);
        if (textBlocks.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (TextBlock block : textBlocks) {
            sb.append(block.getText());
        }
        return sb.toString();
    }

    private String extractToolOutput(ToolResultBlock result) {
        List<ContentBlock> outputs = result.getOutput();
        if (outputs == null || outputs.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (ContentBlock block : outputs) {
            if (block instanceof TextBlock tb) {
                sb.append(tb.getText());
            } else {
                sb.append(String.valueOf(block));
            }
        }
        return sb.toString();
    }

    private Map<String, Object> convertInput(Object input) {
        if (input == null) {
            return Map.of();
        }
        if (input instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> typed = (Map<String, Object>) map;
            return typed;
        }
        try {
            return objectMapper.convertValue(input, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of("value", String.valueOf(input));
        }
    }

    private String serializeSafely(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{\"type\":\"ERROR\",\"errorCode\":\"SERIALIZE\",\"error\":\"Serialization error\"}";
        }
    }
}
