package com.zhemu.paperinsight.agent.config;

import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.formatter.openai.OpenAIChatFormatter;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.OpenAIChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AgentScope 模型配置类
 * 支持 DashScope 和 OpenAI 模型提供商
 * @author lushihao
 */
@Slf4j
@Configuration
public class AgentScopeModelConfig {

    private static final String PROVIDER_OPENAI = "openai";

    @Value("${agentscope.model.provider:dashscope}")
    private String modelProvider;

    @Value("${agentscope.dashscope.api-key:}")
    private String dashscopeApiKey;

    @Value("${agentscope.dashscope.model-name:qwen-plus}")
    private String dashscopeModelName;

    @Value("${agentscope.dashscope.base-url:}")
    private String dashscopeBaseUrl;

    @Value("${agentscope.openai.api-key:}")
    private String openaiApiKey;

    @Value("${agentscope.openai.model-name:gpt-3.5-turbo}")
    private String openaiModelName;

    @Value("${agentscope.openai.base-url:}")
    private String openaiBaseUrl;

    @Bean
    public Model model() {
        if (PROVIDER_OPENAI.equalsIgnoreCase(modelProvider)) {
            log.info("Creating OpenAI Model with model: {}, baseUrl: {}", openaiModelName, openaiBaseUrl);
            OpenAIChatModel.Builder builder = OpenAIChatModel.builder()
                    .apiKey(openaiApiKey)
                    .modelName(openaiModelName)
                    .stream(true)
                    .formatter(new OpenAIChatFormatter());

            if (isValidUrl(openaiBaseUrl)) {
                builder.baseUrl(openaiBaseUrl);
            }
            return builder.build();
        } else {
            log.info("Creating DashScope Model with model: {}, baseUrl: {}", dashscopeModelName, dashscopeBaseUrl);
            DashScopeChatModel.Builder builder = DashScopeChatModel.builder()
                    .apiKey(dashscopeApiKey)
                    .modelName(dashscopeModelName)
                    .formatter(new DashScopeChatFormatter());

            if (isValidUrl(dashscopeBaseUrl)) {
                builder.baseUrl(dashscopeBaseUrl);
            }
            return builder.build();
        }
    }

    private boolean isValidUrl(String url) {
        return url != null && !url.isEmpty() && !"-".equals(url);
    }
}
