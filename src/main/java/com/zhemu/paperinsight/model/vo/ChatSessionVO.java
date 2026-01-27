package com.zhemu.paperinsight.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对话会话 VO
 * 
 * @author lushihao
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionVO implements Serializable {
    private Long id;
    private String sessionId;
    private String title;
    private LocalDateTime lastMessageTime;
}
