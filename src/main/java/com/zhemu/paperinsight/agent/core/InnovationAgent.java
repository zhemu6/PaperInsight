package com.zhemu.paperinsight.agent.core;

import com.zhemu.paperinsight.agent.config.AgentPromptConfig;
import com.zhemu.paperinsight.agent.constant.AgentType;
import io.agentscope.core.model.Model;
import org.springframework.stereotype.Component;

/**
 * 创新点提取智能体
 *
 * @author lushihao
 */
@Component
public class InnovationAgent extends BaseAnalysisAgent {
    /**
     *
     * @param model        模型
     * @param promptConfig prompt 配置文件
     */
    public InnovationAgent(Model model, AgentPromptConfig promptConfig) {
        super(model,
                promptConfig.getAgents().get(AgentType.INNOVATION.getConfigKey()),
                AgentType.INNOVATION.getAgentName());
    }
}
