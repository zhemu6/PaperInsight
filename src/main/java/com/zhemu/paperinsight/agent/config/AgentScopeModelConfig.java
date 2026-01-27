package com.zhemu.paperinsight.agent.config;

import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.formatter.openai.OpenAIChatFormatter;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.GenerateOptions; // Add this import
import io.agentscope.core.model.Model;
import io.agentscope.core.model.OpenAIChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AgentScope 模型配置类
 * 支持 DashScope 和 OpenAI 模型提供商
 *
 * @author lushihao
 */
@Slf4j
@Configuration
public class AgentScopeModelConfig {
    // 用于兜底的模型配置
    private static final String PROVIDER_OPENAI = "openai";

    @Value("${agent.agents.common.provider:dashscope}")
    private String modelProvider;

    @Value("${agent.agents.dashscope.api-key:}")
    private String dashscopeApiKey;

    @Value("${agent.agents.dashscope.model-name:qwen-plus}")
    private String dashscopeModelName;

    @Value("${agent.agents.dashscope.base-url:}")
    private String dashscopeBaseUrl;

    @Value("${agent.agents.openai.api-key:}")
    private String openaiApiKey;

    @Value("${agent.agents.openai.model-name:gpt-3.5-turbo}")
    private String openaiModelName;

    @Value("${agent.agents.openai.base-url:}")
    private String openaiBaseUrl;

    // ChatAgent 模型配置

    @Value("${agent.agents.chat.model-name:}")
    private String chatModelName;

    @Value("${agent.agents.chat.api-key:}")
    private String chatApiKey;

    @Value("${agent.agents.chat.base-url:}")
    private String chatBaseUrl;

    @Value("${agent.agents.chat.think:false}")
    private Boolean chatThink;

    @Value("${agent.agents.chat.provider:}")
    private String chatProvider;

    @Bean
    public Model model() {
        return modelFactory().createModel(null, false);
    }

    private boolean isValidUrl(String url) {
        return url != null && !url.isEmpty() && !"-".equals(url);
    }

    /**
     * 创建具备深度思考（Reasoning）能力的模型实例
     * 专门用于 ChatAgent
     */
    @Bean("thinkingModel")
    public Model thinkingModel() {
        // 使用 chatModelName, chatApiKey, chatBaseUrl (如果有配置)
        // 开启 thinking 模式 (取决于 chatThink，默认为 false，但 ChatAgent 场景下用户可能会开启)
        return modelFactory().createModel(chatModelName, chatApiKey, chatBaseUrl,
                Boolean.TRUE.equals(chatThink), chatProvider);
    }

    @Bean
    public ModelFactory modelFactory() {
        CommonModelProperties commonProps = new CommonModelProperties(
                modelProvider,
                dashscopeApiKey,
                dashscopeModelName,
                dashscopeBaseUrl,
                openaiApiKey,
                openaiModelName,
                openaiBaseUrl);
        return new ModelFactory(commonProps);
    }

    /**
     * 通用模型配置 DTO
     */
    public record CommonModelProperties(
            String provider,
            String dsApiKey,
            String dsDefaultModel,
            String dsBaseUrl,
            String oaApiKey,
            String oaDefaultModel,
            String oaBaseUrl) {
    }

    public static class ModelFactory {
        private final CommonModelProperties config;

        public ModelFactory(CommonModelProperties config) {
            this.config = config;
        }

        public Model createModel(String customModelName) {
            return createModel(customModelName, null, null, false, null);
        }

        public Model createModel(String customModelName, boolean enableThinking) {
            return createModel(customModelName, null, null, enableThinking, null);
        }

        public Model createModel(String customModelName, String customApiKey, String customBaseUrl,
                boolean enableThinking) {
            return createModel(customModelName, customApiKey, customBaseUrl, enableThinking, null);
        }

        public Model createModel(String customModelName, String customApiKey, String customBaseUrl,
                boolean enableThinking, String customProvider) {
            // 确定最终使用的 provider，优先使用 customProvider
            String provider = (customProvider != null && !customProvider.isEmpty()) ? customProvider
                    : config.provider();

            if (PROVIDER_OPENAI.equalsIgnoreCase(provider)) {
                String modelName = (customModelName != null && !customModelName.isEmpty()) ? customModelName
                        : config.oaDefaultModel();
                String apiKey = (customApiKey != null && !customApiKey.isEmpty()) ? customApiKey : config.oaApiKey();
                String baseUrl = (customBaseUrl != null && !customBaseUrl.isEmpty()) ? customBaseUrl
                        : config.oaBaseUrl();

                OpenAIChatModel.Builder builder = OpenAIChatModel.builder()
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .stream(true)
                        .formatter(new OpenAIChatFormatter());
                if (baseUrl != null && !baseUrl.isEmpty() && !"-".equals(baseUrl)) {
                    builder.baseUrl(baseUrl);
                }
                return builder.build();
            } else {
                String modelName = (customModelName != null && !customModelName.isEmpty()) ? customModelName
                        : config.dsDefaultModel();
                String apiKey = (customApiKey != null && !customApiKey.isEmpty()) ? customApiKey : config.dsApiKey();
                String baseUrl = (customBaseUrl != null && !customBaseUrl.isEmpty()) ? customBaseUrl
                        : config.dsBaseUrl();

                DashScopeChatModel.Builder builder = DashScopeChatModel.builder()
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .formatter(new DashScopeChatFormatter())
                        .stream(true);

                if (enableThinking) {
                    builder.defaultOptions(
                            GenerateOptions.builder()
                                    .thinkingBudget(1024) // 设置思考 token 预算
                                    .build());
                }

                if (baseUrl != null && !baseUrl.isEmpty() && !"-".equals(baseUrl)) {
                    builder.baseUrl(baseUrl);
                }
                return builder.build();
            }
        }
    }

}
