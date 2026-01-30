package com.zhemu.paperinsight.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户通知表
 * @author lushihao
 */
@TableName(value = "notification")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Notification implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 接收用户ID
     */
    private Long userId;

    /**
     * 通知类型(如: paper_analysis_success/paper_analysis_failed)
     */
    private String type;

    /**
     * 通知标题
     */
    private String title;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 扩展信息(JSON)
     */
    private String payloadJson;

    /**
     * 幂等去重Key
     */
    private String dedupKey;

    /**
     * 已读时间
     */
    private LocalDateTime readTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDelete;

    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
