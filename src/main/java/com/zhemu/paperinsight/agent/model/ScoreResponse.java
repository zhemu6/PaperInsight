package com.zhemu.paperinsight.agent.model;

import lombok.Data;

/**
 * 评分智能体输出结构
 * TODO 后续完成
 * @author lushihao
 */
@Deprecated
@Data
public class ScoreResponse {
    private Integer score;
    private Dimensions dimensions;
    private String reasoning;

    @Data
    public static class Dimensions {
        private Integer innovation;
        private Integer technical;
        private Integer practicality;
        private Integer writing;
    }
}
