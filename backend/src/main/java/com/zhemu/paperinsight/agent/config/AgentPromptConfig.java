package com.zhemu.paperinsight.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * 智能体提示词配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "agent")
public class AgentPromptConfig {

    private Map<String, AgentProperties> agents;
    private ToolProperties tools = new ToolProperties();

    @Data
    public static class AgentProperties {
        private String modelName;
        private Integer maxIterations = 5;
        private Double temperature = 0.3;
        private String sysPrompt;
        private Boolean enabled = true;
    }

    @Data
    public static class ToolProperties {
        private Integer pdfMaxSize = 52428800;
        private Integer textMaxLength = 1000000;
    }
}
