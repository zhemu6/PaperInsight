package com.zhemu.paperinsight.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 论文分析结果 VO
 * 
 * @author lushihao
 */
@Data
public class PaperInsightVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * AI生成的摘要总结(Markdown)
     */
    private String summaryMarkdown;

    /**
     * 创新点
     */
    private String innovationPoints;

    /**
     * 方法论
     */
    private String methods;

    /**
     * 评分 (0-100)
     */
    private Integer score;

    /**
     * 评分详情 (各维度评分)
     */
    private Map<String, Object> scoreDetails;

}
