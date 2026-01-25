package com.zhemu.paperinsight.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件夹表
 * 
 * @author lushihao
 * @TableName folder
 */
@TableName(value = "folder")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Folder implements Serializable {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文件夹名称
     */
    private String name;

    /**
     * 创建人ID
     */
    private Long userId;

    /**
     * 父文件夹ID (0代表根目录)
     */
    private Long parentId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
