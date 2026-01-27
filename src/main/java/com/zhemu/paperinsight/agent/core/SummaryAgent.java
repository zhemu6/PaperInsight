package com.zhemu.paperinsight.agent.core;

import com.zhemu.paperinsight.agent.config.AgentPromptConfig;
import com.zhemu.paperinsight.agent.constant.AgentType;
import io.agentscope.core.model.Model;
import org.springframework.stereotype.Component;
import com.zhemu.paperinsight.agent.config.AgentScopeModelConfig;

/**
 * 摘要总结智能体
 *
 * @author lushihao
 */
@Component
public class SummaryAgent extends BaseAnalysisAgent {

    public SummaryAgent(AgentScopeModelConfig.ModelFactory modelFactory, AgentPromptConfig promptConfig) {
        super(modelFactory,
                promptConfig.getAgents().get(AgentType.SUMMARY.getConfigKey()),
                AgentType.SUMMARY.getAgentName());
    }
}
