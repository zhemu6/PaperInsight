package com.zhemu.paperinsight.model.dto.paper;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author lushihao
 */
@Data
public class PaperUpdateRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @NotNull(message = "id不能为空")
    @Min(value = 1, message = "id必须大于0")
    private Long id;

    /**
     * 标题
     */
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
    /**
     * 封面图片地址
     */
    private String coverUrl;

    /**
     * 文件夹ID (移动文件夹)
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
