package com.zhemu.paperinsight.agent.core;

import com.zhemu.paperinsight.agent.config.AgentPromptConfig;
import com.zhemu.paperinsight.agent.constant.AgentType;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Event;
import io.agentscope.core.memory.LongTermMemoryMode;
import io.agentscope.core.memory.Memory;
import io.agentscope.core.memory.autocontext.AutoContextConfig;
import io.agentscope.core.memory.autocontext.AutoContextHook;
import io.agentscope.core.memory.autocontext.AutoContextMemory;
import io.agentscope.core.memory.mem0.Mem0LongTermMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.Model;
import io.agentscope.core.rag.Knowledge;
import io.agentscope.core.rag.RAGMode;
import io.agentscope.core.session.mysql.MysqlSession;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.mcp.McpClientBuilder;
import io.agentscope.core.tool.mcp.McpClientWrapper;
import io.agentscope.extensions.higress.HigressMcpClientBuilder;
import io.agentscope.extensions.higress.HigressMcpClientWrapper;
import io.agentscope.extensions.higress.HigressToolkit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Flux;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 论文对话智能体
 * 使用 AgentScope 原生 Agentic RAG 模式，支持流式输出
 * Agent 自主决定何时检索知识库
 *
 * @author lushihao
 */
@Slf4j
public class ChatAgent {
    @Value("${agent.agents.mem0.api-key:}")
    private String memApikey;
    // 模型
    private final Model model;
    // 模型和 prompt 配置文件
    private final AgentPromptConfig.AgentProperties properties;
    // 模型名称
    private final String agentName;
    // 知识库
    private final Knowledge knowledge;

    // Spring 上下文，用于获取工具 Bean
    private final org.springframework.context.ApplicationContext applicationContext;
    private final AgentPromptConfig promptConfig;

    // 缓存预解析的工具组：Map<GroupName, List<ToolBean>>
    private final Map<String, List<Object>> cachedToolGroups = new ConcurrentHashMap<>();

    // Mysql数据库存储 session
    private final DataSource dataSource;

    // 活跃 Agent 注册表: Map<SessionId, ReActAgent>
    private final Map<String, ReActAgent> activeAgents = new ConcurrentHashMap<>();

    public ChatAgent(Model model,
            AgentPromptConfig promptConfig,
            Knowledge knowledge,
            org.springframework.context.ApplicationContext applicationContext,
            DataSource dataSource) {
        this.model = model;
        this.promptConfig = promptConfig;
        this.properties = promptConfig.getAgents().get(AgentType.CHAT.getConfigKey());
        this.agentName = AgentType.CHAT.getAgentName();
        this.knowledge = knowledge;
        this.applicationContext = applicationContext;
        this.dataSource = dataSource;

        // 初始化时预加载所有配置的工具组
        preloadToolGroups();
    }

    /**
     * 预加载工具组 Bean，避免每次请求都去 Map 查找
     */
    private void preloadToolGroups() {
        if (promptConfig.getToolGroups() == null) {
            return;
        }
        promptConfig.getToolGroups().forEach((groupName, groupDef) -> {
            if (groupDef.getTools() != null) {
                List<Object> beans = groupDef.getTools().stream()
                        .map(name -> {
                            if (applicationContext.containsBean(name)) {
                                return applicationContext.getBean(name);
                            }
                            log.warn("Tool bean not found: {}", name);
                            return null;
                        })
                        .filter(Objects::nonNull)
                        .toList();
                cachedToolGroups.put(groupName, beans);
                log.info("Preloaded tool group '{}' with {} tools", groupName, beans.size());
            }
        });
    }

    /**
     * 停止指定会话的 Agent 执行
     *
     * @param sessionId 会话ID
     */
    public void stop(String sessionId) {
        ReActAgent agent = activeAgents.remove(sessionId);
        if (agent != null) {
            log.info("Stopping agent for session: {}", sessionId);
            agent.interrupt();
        } else {
            log.warn("No active agent found to stop for session: {}", sessionId);
        }
    }

    /**
     * Stream method that handles user messages by creating a new agent for each
     * request.
     *
     * @param msg the user message
     * @return Flux of Events from the agent
     */
    public Flux<Event> stream(Msg msg, String sessionId, String userId) {
        // 检查是否有正在运行的实例，如果有则先终止
        if (activeAgents.containsKey(sessionId)) {
            log.warn("Active agent already exists for session {}, interrupting it first.", sessionId);
            stop(sessionId);
        }

        Toolkit toolkit = new Toolkit();
/*        // Higress Ai网关
        HigressMcpClientWrapper higressClient = HigressMcpClientBuilder
                .create("higress")
                .streamableHttpEndpoint("your higress mcp server endpoint")
                .buildAsync()
                .block();
        HigressToolkit higressToolkit = new HigressToolkit();
        higressToolkit.registerMcpClient(higressClient).block();*/
        // 使用预加载的缓存快速组装 Toolkit
        if (properties.getEquippedToolGroups() != null) {
            for (String groupName : properties.getEquippedToolGroups()) {
                AgentPromptConfig.ToolGroupDef groupDef = promptConfig.getToolGroups().get(groupName);
                if (groupDef != null) {
                    // 1. 创建工具组 (状态是 Session 级别的，所以这里还需要创建)
                    toolkit.createToolGroup(groupName, groupDef.getDescription(), groupDef.getInitialActive());

                    // 2. 从缓存直接获取 Beans 进行注册
                    List<Object> tools = cachedToolGroups.get(groupName);
                    if (tools != null) {
                        tools.forEach(toolBean -> toolkit.registration()
                                .tool(toolBean)
                                .group(groupName)
                                .apply());
                    }
                }
            }
        }

        AutoContextConfig autoContextConfig = AutoContextConfig.builder().tokenRatio(0.4).lastKeep(10).build();
        // Use AutoContextMemory, support context auto compression
        AutoContextMemory memory = new AutoContextMemory(autoContextConfig, model);
        MysqlSession mysqlSession = new MysqlSession(dataSource, "paper_insight", null, true);
        ReActAgent agent = createAgent(toolkit, memory, userId);

        // 注册到活跃列表
        activeAgents.put(sessionId, agent);
        agent.loadIfExists(mysqlSession, sessionId);
        return agent.stream(msg)
                .doFinally(
                        signalType -> {
                            log.info(
                                    "Stream terminated with signal: {}, saving session: {}",
                                    signalType,
                                    sessionId);
                            agent.saveTo(mysqlSession, sessionId);
                            // 从活跃列表移除
                            activeAgents.remove(sessionId);
                        });
    }

    /**
     * 创建 Agentic RAG ReActAgent
     */
    private ReActAgent createAgent(Toolkit toolkit, Memory memory, String userId) {



        ReActAgent.Builder builder = ReActAgent.builder()
                .name(agentName)
                .sysPrompt(properties.getSysPrompt())
                .maxIters(properties.getMaxIterations())
                .toolkit(toolkit)
//                .toolkit(higressToolkit)
                // .hooks(List.of(new MonitoringHook(), new AutoContextHook()))
                .hooks(List.of(new AutoContextHook()))
                .model(model)
                .memory(memory)
                // .enablePlan() // 启用计划功能
                // Agentic RAG 配置 - Agent 自主决定何时检索
                .enableMetaTool(Boolean.TRUE.equals(properties.getEnableMetaTool()))
                .knowledge(knowledge)
                .ragMode(RAGMode.AGENTIC);

        if (null != userId) {
            Mem0LongTermMemory longTermMemory = Mem0LongTermMemory.builder()
                    .agentName(agentName)
                    .userId(userId)
                    .apiBaseUrl("https://api.mem0.ai")
                    .apiKey(memApikey)
                    .build();

            // 集成 Context7 MCP
            // 注意：Windows 下必须使用 cmd.exe /c npx
            try {
                String mcpApiKey = "ctx7sk-3d5b0a78-9ed5-4baa-9b85-7e3e5f98af77";
                // if (properties.getMcp() != null && properties.getMcp().get("context7") !=
                // null) {
                // mcpApiKey = (String) properties.getMcp().get("context7").get("api-key");
                // }

                McpClientWrapper mcpClient = McpClientBuilder.create("context7-mcp")
                        .stdioTransport("cmd.exe", "/c", "npx", "-y", "@upstash/context7-mcp", "--api-key", mcpApiKey)
                        .buildAsync()
                        .block();
                toolkit.registerMcpClient(mcpClient).block();
                log.info("Context7 MCP client registered successfully.");
            } catch (Exception e) {
                log.error("Failed to register Context7 MCP client", e);
            }
            return builder.longTermMemory(longTermMemory).longTermMemoryMode(LongTermMemoryMode.STATIC_CONTROL).build();
        }
        return builder.build();
    }

    /**
     * 获取历史会话消息
     *
     * @param sessionId 会话ID
     * @return 消息列表
     */
    public List<Msg> getHistory(String sessionId) {
        Toolkit toolkit = new Toolkit();
        // 尝试注册基础工具组，如果存在
        if (cachedToolGroups.containsKey("common-tools")) {
            List<Object> tools = cachedToolGroups.get("common-tools");
            tools.forEach(toolkit::registerTool);
        }

        // 上下文管理配置
        AutoContextConfig autoContextConfig = AutoContextConfig.builder().tokenRatio(0.4).lastKeep(10).build();
        // 智能上下文内存管理系统，自动压缩、卸载和摘要对话历史。
        AutoContextMemory memory = new AutoContextMemory(autoContextConfig, model);
        MysqlSession mysqlSession = new MysqlSession(dataSource, "paper_insight", null, true);

        ReActAgent agent = createAgent(toolkit, memory, "");

        if (agent.loadIfExists(mysqlSession, sessionId)) {
            // Memory 中已经加载了历史条目
            return memory.getMessages();
        }
        return List.of();
    }
}
