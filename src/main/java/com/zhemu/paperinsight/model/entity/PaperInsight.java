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
 * 论文AI分析结果表
 * @author lushihao
 * @TableName paper_insight
 */
@TableName(value = "paper_insight")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaperInsight implements Serializable {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 论文ID
     */
    private Long paperId;

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
