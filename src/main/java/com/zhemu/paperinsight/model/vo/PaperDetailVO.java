package com.zhemu.paperinsight.model.vo;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * 论文详情 VO (包含基础信息和AI分析结果)
 * @author lushihao
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaperDetailVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 论文基础信息
     */
    private PaperVO paperInfo;

    /**
     * AI 分析结果
     */
    private PaperInsightVO paperInsight;

}
