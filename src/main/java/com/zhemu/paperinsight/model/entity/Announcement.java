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
 * 系统公告表
 * @author lushihao
 */
@TableName(value = "announcement")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Announcement implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    /**
     * draft/published
     */
    private String status;

    private LocalDateTime publishTime;

    private Long publisherId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDelete;

    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
