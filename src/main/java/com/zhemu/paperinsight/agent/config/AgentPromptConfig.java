package com.zhemu.paperinsight.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * 智能体提示词配置
 * 
 * @author lushihao
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "agent")
public class AgentPromptConfig {
    // 定义的结构 其中String是智能体名称（summary） AgentProperties是智能体配置属性（）
    private Map<String, AgentProperties> agents;
    // 定义工具组配置，Key为组名（如 "common-tools"）
    private Map<String, ToolGroupDef> toolGroups;
    private ToolProperties tools = new ToolProperties();

    @Data
    public static class AgentProperties {
        private String provider;
        private String modelName;
        private String apiKey;
        private Boolean think;
        private String baseUrl;
        private String sysPrompt;
        private Integer maxIterations = 5;
        private Boolean enabled = true;
        // 该智能体启用的工具组名称列表
        private java.util.List<String> equippedToolGroups;
        // 是否开启 meta-tool (reset_equipped_tools)
        private Boolean enableMetaTool = false;
    }

    @Data
    public static class ToolProperties {
        private Integer pdfMaxSize = 52428800; // 50MB
        private Integer textMaxLength = 1000000;
    }

    /**
     * 工具组定义
     */
    @Data
    public static class ToolGroupDef {
        private String description; // 工具组描述，用于 meta-tool
        private Boolean initialActive = false; // 是否初始激活
        private java.util.List<String> tools; // 包含的 Tool Bean 名称列表
    }
}
