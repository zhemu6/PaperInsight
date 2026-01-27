package com.zhemu.paperinsight.controller;

import com.zhemu.paperinsight.agent.core.ChatAgent;
import com.zhemu.paperinsight.common.UserContext;
import io.agentscope.core.agent.Event;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.List;

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

    /**
     * 流式对话接口 (SSE)
     *
     * @param chatId    对话ID
     * @param userQuery 用户问题
     * @return SSE 事件流
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
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
                                log.error("Error occurred during streaming", e);
                            });

        } catch (Exception e) {
            log.error("Failed to process user query: {}", userQuery, e);
            return Flux.just(
                    ServerSentEvent.builder("System processing error, please try again later.")
                            .build());
        }
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
                .doOnNext(output -> log.info("output = {}", output))
                // 2. 过滤掉最后的一条结束消息（通常是系统状态消息）
                .filter(event -> !event.isLast())
                // 3. 提取消息中的文本内容
                .map(
                        event -> {
                            Msg msg = event.getMessage();
                            // 遍历内容块，提取所有 TextBlock
                            return msg.getContent().stream()
                                    .filter(block -> block instanceof TextBlock)
                                    .map(block -> ((TextBlock) block).getText())
                                    .toList();
                        })
                // 4. 将 List<String> 铺平成单个 String 流
                .flatMap(Flux::fromIterable)
                // 5. 包装成 SSE 事件对象
                .map(content -> ServerSentEvent.builder(content).build())
                // 6. 手动推送到 Sink 池中
                .doOnNext(sink::tryEmitNext)
                // 7. 错误处理：记录日志并发送错误消息给前端
                .doOnError(
                        e -> {
                            log.error(
                                    "Unexpected error in stream processing: {}", e.getMessage(), e);
                            sink.tryEmitNext(
                                    ServerSentEvent.builder(
                                            "System processing error, please try again"
                                                    + " later.")
                                            .build());
                        })
                // 8. 完成处理：发送 [DONE] 标记并关闭 Sink
                .doOnComplete(
                        () -> {
                            log.info("Stream processing completed successfully");
                            sink.tryEmitNext(ServerSentEvent.builder("[DONE]").build());
                            sink.tryEmitComplete();
                        })
                // 9. 真正订阅并触发流的执行
                .subscribe(
                        // onNext - 已经在 doOnNext 中处理了，这里留空
                        null,
                        // onError - 最后的防线，确保 Sink 收到错误信号
                        e -> {
                            log.error("Stream processing failed: {}", e.getMessage(), e);
                            sink.tryEmitError(e);
                        });
    }

    /**
     * 获取历史消息记录
     *
     * @param chatId 对话ID
     * @return 历史消息列表
     */
    @GetMapping("/history")
    public List<HistoryItem> getHistory(@RequestParam String chatId) {
        return chatAgent.getHistory(chatId).stream()
                // 过滤掉系统消息（SYSTEM 角色）
                // .filter(msg -> msg.getRole() != null &&
                // !MsgRole.SYSTEM.equals(msg.getRole()))
                .map(msg -> {
                    // 使用 name() 方法并转为小写，解决 getValue() 报错问题
                    String roleName = msg.getRole().name().toLowerCase();

                    // 统一角色映射：将 model 或 assistant 统一为前端的 assistant
                    String role = roleName;
                    if ("model".equals(roleName) || "assistant".equals(roleName)) {
                        role = "assistant";
                    }

                    String content = msg.getContent().stream()
                            .filter(block -> block instanceof TextBlock)
                            .map(block -> ((TextBlock) block).getText())
                            .reduce("", (a, b) -> a + b);

                    return new HistoryItem(msg.getId(), role, content);
                })
                .toList();
    }

    record HistoryItem(String id, String role, String content) {
    }

}
