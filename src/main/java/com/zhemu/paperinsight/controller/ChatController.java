package com.zhemu.paperinsight.controller;

import com.zhemu.paperinsight.agent.core.ChatAgent;
import com.zhemu.paperinsight.annotation.AuthCheck;
import com.zhemu.paperinsight.common.UserContext;
import io.agentscope.core.agent.Event;
import io.agentscope.core.message.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 论文对话控制器
 * 提供 SSE 流式对话接口
 *
 * @author lushihao
 */
@Slf4j
@RestController
@RequestMapping("/assistant")
@RequiredArgsConstructor
public class ChatController {

        private final ChatAgent chatAgent;
        private final ObjectMapper objectMapper;

        /**
         * 流式对话接口 (SSE)
         *
         * @param chatId    对话ID
         * @param userQuery 用户问题
         * @return SSE 事件流
         */
        @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        @AuthCheck
        public Flux<ServerSentEvent<String>> chatStream(
                        @RequestParam String chatId,
                        @RequestParam String userQuery,
                        @RequestParam String userId) {
                log.info("Chat stream request - chatId: {}, userId: {}, userQuery: {}", chatId,
                                userId, userQuery);
                try {
                        Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().unicast().onBackpressureBuffer();
                        Msg msg = Msg.builder()
                                        .role(MsgRole.USER)
                                        .content(TextBlock.builder().text(userQuery).build())
                                        .build();

                        processStream(chatAgent.stream(msg, chatId, userId), sink);
                        return sink.asFlux()
                                        .doOnCancel(
                                                        () -> {
                                                                log.info("Client disconnected from stream");
                                                        })
                                        .doOnError(
                                                        e -> {
                                                                // log.error("Error occurred during streaming", e);
                                                        });

                } catch (Exception e) {
                        // log.error("Failed to process user query: {}", userQuery, e);
                        return Flux.just(
                                        ServerSentEvent.builder("System processing error, please try again later.")
                                                        .build());
                }
        }

        /**
         * 停止生成
         *
         * @param chatId 对话ID
         */
        @PostMapping("/stop")
        @AuthCheck
        public void stopStream(@RequestParam String chatId) {
                log.info("Received stop request for chat: {}", chatId);
                chatAgent.stop(chatId);
        }

        /**
         * 处理流式响应核心逻辑
         * 仿照官方 SupervisorAgentController 实现
         *
         * @param generator AgentScope 产生的事件流
         * @param sink      Spring WebFlux 的数据接收器（用于发送 SSE）
         */
        public void processStream(Flux<Event> generator, Sinks.Many<ServerSentEvent<String>> sink) {
                generator
                                // 1. 记录原始输出日志
//                                .doOnNext(output -> log.info("output = {}", output))
                                // 2. 过滤掉最后的一条结束消息（通常是系统状态消息）
                                // .filter(event -> !event.isLast())
                                // 3. 提取消息内容并包装为 SSE 事件
                                .flatMap(event -> {
                                        Msg msg = event.getMessage();
                                        List<ContentBlock> content = msg.getContent();

                                        // 遍历内容块，转换为 SSE 事件流
                                        // 使用 flatMap 允许每个块映射为 1 个 SSE 事件
                                        return Flux.fromIterable(content)
                                                        .filter(block -> {
                                                                // 始终允许工具调用和工具结果
                                                                if (block instanceof ToolResultBlock
                                                                                || (block instanceof ToolUseBlock
                                                                                                && ((ToolUseBlock) block)
                                                                                                                .getName() != null
                                                                                                && !((ToolUseBlock) block)
                                                                                                                .getName()
                                                                                                                .contains("__fragment__"))) {
                                                                        return true;
                                                                }
                                                                // 如果是结束事件（isLast=true），过滤掉文本和思考块以避免重复
                                                                if (event.isLast()) {
                                                                        return false;
                                                                }
                                                                // 其他情况允许
                                                                return block instanceof TextBlock
                                                                                || block instanceof ThinkingBlock;
                                                        })
                                                        .map(block -> {
                                                                try {
                                                                        if (block instanceof ThinkingBlock) {
                                                                                String thinking = ((ThinkingBlock) block)
                                                                                                .getThinking();
                                                                                return ServerSentEvent.builder(thinking)
                                                                                                .event("thinking")
                                                                                                .build();
                                                                        } else if (block instanceof ToolUseBlock) {
                                                                                ToolUseBlock tool = (ToolUseBlock) block;
                                                                                // 直接发送 ToolUseBlock，不做任何缓冲或过滤
                                                                                ToolItem item = new ToolItem("tool_use",
                                                                                                tool.getName(),
                                                                                                String.valueOf(tool
                                                                                                                .getInput()),
                                                                                                null);
                                                                                return ServerSentEvent
                                                                                                .builder(objectMapper
                                                                                                                .writeValueAsString(
                                                                                                                                item))
                                                                                                .event("tool_use")
                                                                                                .build();
                                                                        } else if (block instanceof ToolResultBlock) {
                                                                                ToolResultBlock tool = (ToolResultBlock) block;
                                                                                String output = tool.getOutput()
                                                                                                .stream()
                                                                                                .map(Object::toString)
                                                                                                .collect(java.util.stream.Collectors
                                                                                                                .joining("\n"));

                                                                                // 直接发送 ToolResultBlock
                                                                                ToolItem item = new ToolItem(
                                                                                                "tool_result",
                                                                                                tool.getName(),
                                                                                                null,
                                                                                                output);
                                                                                return ServerSentEvent
                                                                                                .builder(objectMapper
                                                                                                                .writeValueAsString(
                                                                                                                                item))
                                                                                                .event("tool_result")
                                                                                                .build();
                                                                        } else {
                                                                                // TextBlock
                                                                                String text = ((TextBlock) block)
                                                                                                .getText();
                                                                                return ServerSentEvent.builder(text)
                                                                                                .build();
                                                                        }
                                                                } catch (Exception e) {
                                                                        log.error("Failed to serialize block", e);
                                                                        return ServerSentEvent.<String>builder()
                                                                                        .event("error")
                                                                                        .data("Serialization error")
                                                                                        .build();
                                                                }
                                                        });
                                })
                                // 4. 手动推送到 Sink 池中
                                .doOnNext(sink::tryEmitNext)
                                // 5. 错误处理
                                .doOnError(e ->

                                {
                                        log.error("Unexpected error in stream processing: {}", e.getMessage(), e);
                                        sink.tryEmitNext(ServerSentEvent
                                                        .builder("System processing error, please try again later.")
                                                        .event("error")
                                                        .build());
                                })
                                // 6. 完成处理
                                .doOnComplete(sink::tryEmitComplete)
                                // 7. 订阅执行
                                .subscribe();
        }

        /**
         * 获取历史消息记录
         *
         * @param chatId 对话ID
         * @return 历史消息列表
         */
        @GetMapping("/history")
        @AuthCheck
        public List<HistoryItem> getHistory(@RequestParam String chatId) {
                List<HistoryItem> list = chatAgent.getHistory(chatId).stream()
                                // 过滤掉系统消息（保留工具消息）
                                .filter(msg -> msg.getRole() != null &&
                                                !MsgRole.SYSTEM.equals(msg.getRole()))
                                // 过滤掉中断提示消息
                                .filter(msg -> msg.getContent().stream()
                                                .noneMatch(block -> block instanceof TextBlock &&
                                                                ((TextBlock) block).getText().contains(
                                                                                "I noticed that you have interrupted me")))
                                .map(msg -> {
                                        String roleName = msg.getRole().name().toLowerCase();

                                        // 统一角色映射：将 model 或 assistant 统一为前端的 assistant
                                        String role = roleName;
                                        if ("model".equals(roleName) || "assistant".equals(roleName)) {
                                                role = "assistant";
                                        } else if ("tool".equals(roleName)) {
                                                role = "tool";
                                        }

                                        // 构造结构化的内容列表
                                        var contentList = msg.getContent().stream()
                                                        .filter(block -> block instanceof TextBlock
                                                                        || block instanceof ThinkingBlock
                                                                        || block instanceof ToolUseBlock
                                                                        || block instanceof ToolResultBlock)
                                                        .map(block -> {
                                                                if (block instanceof ThinkingBlock) {
                                                                        return new ThinkingItem("thinking",
                                                                                        ((ThinkingBlock) block)
                                                                                                        .getThinking());
                                                                } else if (block instanceof ToolUseBlock) {
                                                                        ToolUseBlock toolBlock = (ToolUseBlock) block;
                                                                        return new ToolItem("tool_use",
                                                                                        toolBlock.getName(),
                                                                                        toolBlock.getInput().toString(),
                                                                                        null);
                                                                } else if (block instanceof ToolResultBlock) {
                                                                        ToolResultBlock toolBlock = (ToolResultBlock) block;
                                                                        String output = toolBlock.getOutput().stream()
                                                                                        .map(c -> {
                                                                                                if (c instanceof TextBlock) {
                                                                                                        return ((TextBlock) c)
                                                                                                                        .getText();
                                                                                                }
                                                                                                return String.valueOf(
                                                                                                                c);
                                                                                        })
                                                                                        .collect(java.util.stream.Collectors
                                                                                                        .joining("\n"));
                                                                        return new ToolItem("tool_result",
                                                                                        toolBlock.getName(),
                                                                                        null,
                                                                                        output);
                                                                } else if (block instanceof TextBlock) {
                                                                        return new TextItem("text",
                                                                                        ((TextBlock) block).getText());
                                                                }
                                                                return new TextItem("text", block.toString());
                                                        })
                                                        .map(item -> {
                                                                if (MsgRole.TOOL.equals(msg.getRole())
                                                                                && item instanceof TextItem textItem) {
                                                                        return new ToolItem("tool_result",
                                                                                        "tool",
                                                                                        null,
                                                                                        textItem.text());
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
                log.info(list.toString());
                return list;
        }

        record HistoryItem(String id, String role, Object content) {
        }

        record ThinkingItem(String type, String thinking) {
        }

        record TextItem(String type, String text) {
        }

        record ToolItem(String type, String name, String input, String output) {
        }

}
