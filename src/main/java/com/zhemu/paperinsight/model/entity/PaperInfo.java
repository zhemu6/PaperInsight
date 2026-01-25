package com.zhemu.paperinsight.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 论文信息表
 * 
 * @author lushihao
 * @TableName paper_info
 */
@TableName(value = "paper_info")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaperInfo implements Serializable {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 论文标题
     */
    private String title;

    /**
     * 作者列表
     */
    private String authors;

    /**
     * 摘要
     */
    private String abstractInfo;

    /**
     * 关键词
     */
    private String keywords;

    /**
     * COS存储地址
     */
    private String cosUrl;

    /**
     * 所属文件夹ID
     */
    private Long folderId;

    /**
     * 封面图片地址
     */
    private String coverUrl;

    /**
     * 上传用户ID
     */
    private Long userId;

    /**
     * 是否公开 (0-私有 1-公开)
     */
    private Integer isPublic;

    /**
     * 发表日期
     */
    private LocalDate publishDate;

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
