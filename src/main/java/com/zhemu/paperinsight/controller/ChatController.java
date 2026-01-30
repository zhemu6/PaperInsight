package com.zhemu.paperinsight.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhemu.paperinsight.agent.core.ChatAgent;
import com.zhemu.paperinsight.annotation.AuthCheck;
import com.zhemu.paperinsight.model.dto.chat.ChatEvent;
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
            HttpServletRequest request) {
        long userId = sysUserService.getLoginUser(request).getId();
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
    public void stopStream(@RequestParam String chatId) {
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
    public List<HistoryItem> getHistory(@RequestParam String chatId) {
        List<HistoryItem> list = chatAgent.getHistory(chatId).stream()
                // 过滤掉系统消息（保留工具消息）
                .filter(msg -> msg.getRole() != null && !MsgRole.SYSTEM.equals(msg.getRole()))
                // 过滤掉中断提示消息
                .filter(msg -> msg.getContent().stream()
                        .noneMatch(block -> block instanceof TextBlock
                                && ((TextBlock) block).getText().contains("I noticed that you have interrupted me")))
                .map(msg -> {
                    String roleName = msg.getRole().name().toLowerCase();

                    // 统一角色映射：将 model 或 assistant 统一为前端的 assistant
                    String role = roleName;
                    if ("model".equals(roleName) || "assistant".equals(roleName)) {
                        role = "assistant";
                    } else if ("tool".equals(roleName)) {
                        role = "tool";
                    }

                    var contentList = msg.getContent().stream()
                            .filter(block -> block instanceof TextBlock
                                    || block instanceof ThinkingBlock
                                    || block instanceof ToolUseBlock
                                    || block instanceof ToolResultBlock)
                            .map(block -> {
                                if (block instanceof ThinkingBlock) {
                                    return new ThinkingItem("thinking", ((ThinkingBlock) block).getThinking());
                                } else if (block instanceof ToolUseBlock) {
                                    ToolUseBlock toolBlock = (ToolUseBlock) block;
                                    return new ToolItem("tool_use", toolBlock.getId(), toolBlock.getName(),
                                            String.valueOf(toolBlock.getInput()), null);
                                } else if (block instanceof ToolResultBlock) {
                                    ToolResultBlock toolBlock = (ToolResultBlock) block;
                                    String output = extractToolOutput(toolBlock);
                                    return new ToolItem("tool_result", toolBlock.getId(), toolBlock.getName(), null, output);
                                } else if (block instanceof TextBlock) {
                                    return new TextItem("text", ((TextBlock) block).getText());
                                }
                                return new TextItem("text", String.valueOf(block));
                            })
                            .map(item -> {
                                if (MsgRole.TOOL.equals(msg.getRole()) && item instanceof TextItem textItem) {
                                    return new ToolItem("tool_result", null, "tool", null, textItem.text());
                                }
                                return item;
                            })
                            .toList();

                    return new HistoryItem(msg.getId(), role, contentList);
                })
                .filter(item -> {
                    if (item.content() instanceof List) {
                        return !((List<?>) item.content()).isEmpty();
                    }
                    return true;
                })
                .toList();
        return list;
    }

    record HistoryItem(String id, String role, Object content) {}

    record ThinkingItem(String type, String thinking) {}

    record TextItem(String type, String text) {}

    record ToolItem(String type, String id, String name, String input, String output) {}

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
