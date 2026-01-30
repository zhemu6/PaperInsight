package com.zhemu.paperinsight.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 论文会话表
 * @author lushihao
 */
@TableName(value = "paper_chat_session")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaperChatSession implements Serializable {

    @TableId(type = IdType.INPUT)
    private String id;
    /**
     * 论文 id
     */
    private Long paperId;
    /**
     * 用户 id
     */
    private Long userId;
    /**
     * 标题
     */
    private String title;
    /**
     *  最后一条消息发送时间
     */
    private LocalDateTime lastMessageAt;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDelete;

    @Serial
    private static final long serialVersionUID = 1L;
}
