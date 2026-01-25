package com.zhemu.paperinsight.model.dto.paper;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class PaperAddRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 标题
     */
    @NotBlank(message = "标题不能为空")
    private String title;

    /**
     * 作者
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
     * COS地址
     */
    private String cosUrl;

    /**
     * 封面图片地址
     */
    private String coverUrl;

    /**
     * 文件夹ID
     */
    private Long folderId;

    /**
     * 是否公开
     */
    private Integer isPublic;

    /**
     * 发表日期
     */
    private LocalDateTime publishDate;
}
