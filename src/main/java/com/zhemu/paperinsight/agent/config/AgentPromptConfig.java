package com.zhemu.paperinsight.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * 智能体提示词配置
 * @author lushihao
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "agent")
public class AgentPromptConfig {
    // 定义的结构 其中String是智能体名称（summary） AgentProperties是智能体配置属性（）
    private Map<String, AgentProperties> agents;
    private ToolProperties tools = new ToolProperties();

    @Data
    public static class AgentProperties {
        private String modelName;
        private Integer maxIterations = 5;
        private String sysPrompt;
        private Boolean enabled = true;
    }

    @Data
    public static class ToolProperties {
        private Integer pdfMaxSize = 52428800; // 50MB
        private Integer textMaxLength = 1000000;
    }
}
