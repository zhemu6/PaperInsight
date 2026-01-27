package com.zhemu.paperinsight.agent.core;

import com.zhemu.paperinsight.agent.config.AgentPromptConfig;
import com.zhemu.paperinsight.agent.constant.AgentType;
import io.agentscope.core.model.Model;
import org.springframework.stereotype.Component;

/**
 * 方法论总结智能体
 *
 * @author lushihao
 */
@Component
public class MethodologyAgent extends BaseAnalysisAgent {

    public MethodologyAgent(Model model, AgentPromptConfig promptConfig) {
        super(model,
                promptConfig.getAgents().get(AgentType.METHODOLOGY.getConfigKey()),
                AgentType.METHODOLOGY.getAgentName());
    }
}
