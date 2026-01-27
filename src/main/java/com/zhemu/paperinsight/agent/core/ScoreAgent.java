package com.zhemu.paperinsight.agent.core;

import com.zhemu.paperinsight.agent.config.AgentPromptConfig;
import com.zhemu.paperinsight.agent.constant.AgentType;
import io.agentscope.core.model.Model;
import org.springframework.stereotype.Component;

/**
 * 评分智能体
 *
 * @author lushihao
 */
@Component
public class ScoreAgent extends BaseAnalysisAgent {

    public ScoreAgent(Model model, AgentPromptConfig promptConfig) {
        super(model,
                promptConfig.getAgents().get(AgentType.SCORE.getConfigKey()),
                AgentType.SCORE.getAgentName());
    }
}
