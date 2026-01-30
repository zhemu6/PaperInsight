package com.zhemu.paperinsight.model.dto.chat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.zhemu.paperinsight.model.enums.ChatEventTypeEnum;
import lombok.Data;

import java.util.Map;

/**
 * @author lushihao
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatEvent {

    /**
     * 事件类型。
     * TEXT | THINKING | TOOL_USE | TOOL_RESULT | ERROR | COMPLETE | INTERRUPTED
     */
    private ChatEventTypeEnum type;

    /** 单个 SSE 流内的单调递增序号。 */
    private Long seq;

    /** TEXT / THINKING / INTERRUPTED 的文本内容。 */
    private String content;

    /** 为 true 表示增量片段，为 false 表示最终/快照。 */
    private Boolean incremental;

    /** 工具调用关联 id。 */
    private String toolId;

    /** 工具名称。 */
    private String toolName;

    /** TOOL_USE 的工具输入。 */
    private Map<String, Object> toolInput;

    /** TOOL_RESULT 的工具输出。 */
    private String toolResult;

    /** ERROR 的错误码。 */
    private String errorCode;

    /** ERROR 的错误信息。 */
    private String error;

    /**
     * 文本输出内容
     */
    public static ChatEvent text(long seq, String content, boolean incremental) {
        ChatEvent event = new ChatEvent();
        event.type = ChatEventTypeEnum.TEXT;
        event.seq = seq;
        event.content = content;
        event.incremental = incremental;
        return event;
    }

    /**
     * 思考事件
     */
    public static ChatEvent thinking(long seq, String content, boolean incremental) {
        ChatEvent event = new ChatEvent();
        event.type = ChatEventTypeEnum.THINKING;
        event.seq = seq;
        event.content = content;
        event.incremental = incremental;
        return event;
    }

    /**
     * 工具调用事件
     */
    public static ChatEvent toolUse(long seq, String toolId, String toolName, Map<String, Object> toolInput) {
        ChatEvent event = new ChatEvent();
        event.type = ChatEventTypeEnum.TOOL_USE;
        event.seq = seq;
        event.toolId = toolId;
        event.toolName = toolName;
        event.toolInput = toolInput;
        return event;
    }

    /**
     * 工具调用结果事件
     */
    public static ChatEvent toolResult(long seq, String toolId, String toolName, String toolResult) {
        ChatEvent event = new ChatEvent();
        event.type = ChatEventTypeEnum.TOOL_RESULT;
        event.seq = seq;
        event.toolId = toolId;
        event.toolName = toolName;
        event.toolResult = toolResult;
        return event;
    }

    /**
     * 报错事件
     */
    public static ChatEvent error(long seq, String errorCode, String error) {
        ChatEvent event = new ChatEvent();
        event.type = ChatEventTypeEnum.ERROR;
        event.seq = seq;
        event.errorCode = errorCode;
        event.error = error;
        return event;
    }

    /**
     * 对话完成完成
     */
    public static ChatEvent complete(long seq) {
        ChatEvent event = new ChatEvent();
        event.type = ChatEventTypeEnum.COMPLETE;
        event.seq = seq;
        return event;
    }

    /**
     * 打断事件
     */
    public static ChatEvent interrupted(long seq, String content) {
        ChatEvent event = new ChatEvent();
        event.type = ChatEventTypeEnum.INTERRUPTED;
        event.seq = seq;
        event.content = content;
        return event;
    }
}
