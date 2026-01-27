package com.zhemu.paperinsight.agent.core;

import com.zhemu.paperinsight.agent.config.AgentPromptConfig;
import com.zhemu.paperinsight.agent.config.AgentScopeModelConfig; // Add this import
import com.zhemu.paperinsight.agent.tools.MonitoringHook;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.autocontext.AutoContextConfig;
import io.agentscope.core.memory.autocontext.AutoContextMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 分析类智能体的基类
 * 封装通用的 ReAct Agent 创建和调用逻辑
 * 
 * @author lushihao
 */
@Slf4j
public abstract class BaseAnalysisAgent {

    protected final Model model;
    protected final AgentPromptConfig.AgentProperties properties;
    protected final String agentName;

    protected BaseAnalysisAgent(AgentScopeModelConfig.ModelFactory modelFactory,
            AgentPromptConfig.AgentProperties properties, String agentName) {
        // 优先使用 properties 中的配置，如果为空则由 Factory 决定回退策略（通常是全局配置）
        this.model = modelFactory.createModel(
                properties.getModelName(),
                properties.getApiKey(),
                properties.getBaseUrl(),
                Boolean.TRUE.equals(properties.getThink()),
                properties.getProvider());
        log.info("Create {} with config: modelName {}, apiKey {}, baseURL {}, think {}, provider {}", agentName,
                properties.getModelName(), properties.getApiKey(), properties.getBaseUrl(), properties.getThink(),
                properties.getProvider());
        this.properties = properties;
        this.agentName = agentName;
    }

    /**
     * 执行分析任务
     * 
     * @param content 待分析的文本内容
     * @return 分析结果消息
     */
    public Mono<Msg> analyze(String content) {
        if (!properties.getEnabled()) {
            log.info("Agent {} is disabled, returning empty.", agentName);
            return Mono.empty();
        }
        // 创建智能体
        ReActAgent agent = createAgent();
        Msg userMsg = Msg.builder()
                .content(TextBlock.builder().text(content).build())
                .build();

        return agent.call(userMsg);
    }

    protected ReActAgent createAgent() {
        // 分析类 Agent 不需要 Memory，因为是单轮任务
        return ReActAgent.builder()
                .name(agentName)
                .sysPrompt(properties.getSysPrompt())
                // 这里应该支持根据 properties.getModelName() 覆盖，但暂时使用全局 Model
                .model(model)
                .maxIters(properties.getMaxIterations())
                // .hooks(List.of(new MonitoringHook()))
                .build();
    }

}
