package com.zhemu.paperinsight.model.dto.paper;

import com.zhemu.paperinsight.common.PageRequest;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author lushihao
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
public class PaperQueryRequest extends PageRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    private Long id;

    /**
     * 文件夹ID
     */
    private Long folderId;

    /**
     * 标题 (模糊)
     */
    private String title;

    /**
     * 关键词 (模糊)
     */
    private String keywords;

    /**
     * 作者 (模糊)
     */
    private String authors;

    /**
     * 摘要 (模糊)
     */
    private String abstractInfo;

    /**
     * 全文关键词 (ES 搜索)
     */
    private String contentKeyword;
}
