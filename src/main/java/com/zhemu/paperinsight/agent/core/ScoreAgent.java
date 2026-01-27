package com.zhemu.paperinsight.agent.core;

import com.zhemu.paperinsight.agent.config.AgentPromptConfig;
import com.zhemu.paperinsight.agent.config.AgentScopeModelConfig;
import com.zhemu.paperinsight.agent.constant.AgentType;
import org.springframework.stereotype.Component;

/**
 * 评分智能体
 *
 * @author lushihao
 */
@Component
public class ScoreAgent extends BaseAnalysisAgent {

    public ScoreAgent(AgentScopeModelConfig.ModelFactory modelFactory, AgentPromptConfig promptConfig) {
        super(modelFactory,
                promptConfig.getAgents().get(AgentType.SCORE.getConfigKey()),
                AgentType.SCORE.getAgentName());
    }
}
