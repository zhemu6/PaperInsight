package com.zhemu.paperinsight.agent.core;

import com.zhemu.paperinsight.agent.config.AgentPromptConfig;
import com.zhemu.paperinsight.agent.config.AgentScopeModelConfig;
import com.zhemu.paperinsight.agent.constant.AgentType;
import org.springframework.stereotype.Component;

/**
 * 研究方法论分析智能体
 *
 * @author lushihao
 */
@Component
public class MethodologyAgent extends BaseAnalysisAgent {

    public MethodologyAgent(AgentScopeModelConfig.ModelFactory modelFactory, AgentPromptConfig promptConfig) {
        super(modelFactory,
                promptConfig.getAgents().get(AgentType.METHODOLOGY.getConfigKey()),
                AgentType.METHODOLOGY.getAgentName());
    }
}
