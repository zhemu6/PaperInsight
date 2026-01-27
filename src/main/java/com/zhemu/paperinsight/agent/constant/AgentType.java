package com.zhemu.paperinsight.agent.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 智能体类型枚举
 * 管理智能体的唯一标识和配置Key
 * 
 * @author lushihao
 */
@Getter
@AllArgsConstructor
public enum AgentType {

    /**
     * 摘要智能体
     */
    SUMMARY("summary", "summary_agent"),

    /**
     * 创新点智能体
     */
    INNOVATION("innovation", "innovation_agent"),

    /**
     * 方法论智能体
     */
    METHODOLOGY("methodology", "methodology_agent"),

    /**
     * 评分智能体
     */
    SCORE("score", "score_agent"),

    /**
     * 对话智能体
     */
    CHAT("chat", "chat_agent"),

    /**
     * 关键词智能体
     */
    KEYWORD("keyword", "keyword_agent"),

    /**
     * 推荐智能体
     */
    RECOMMEND("recommend", "recommend_agent");

    /**
     * 配置文件中的Key (application.yml)
     */
    private final String configKey;

    /**
     * 智能体运行时的Name (ReActAgent.name)
     */
    private final String agentName;
}
