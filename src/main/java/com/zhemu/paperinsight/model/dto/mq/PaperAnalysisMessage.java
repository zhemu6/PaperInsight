package com.zhemu.paperinsight.model.dto.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 论文分析任务消息
 * 
 * @author lushihao
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaperAnalysisMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 论文ID
     */
    private Long paperId;

    /**
     * PDF文件地址 (COS URL)
     */
    private String pdfUrl;

    /**
     * 用户ID (可选，用于后续通知)
     */
    private Long userId;
}
