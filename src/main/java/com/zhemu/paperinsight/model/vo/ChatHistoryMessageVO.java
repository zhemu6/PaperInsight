package com.zhemu.paperinsight.model.vo;

import com.zhemu.paperinsight.model.dto.chat.ChatEvent;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * 对话历史消息
 * @author lushihao
 */
@Data
public class ChatHistoryMessageVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 消息 id */
    private String id;

    /** 角色 （user | assistant） */
    private String role;

    /** 该消息下排序好的对话历史 */
    private List<ChatEvent> events;
}
