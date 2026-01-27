package com.zhemu.paperinsight.agent.core;

import com.zhemu.paperinsight.agent.config.AgentPromptConfig;
import com.zhemu.paperinsight.agent.constant.AgentType;
import com.zhemu.paperinsight.agent.tools.AnaTools;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Event;
import io.agentscope.core.memory.Memory;
import io.agentscope.core.memory.autocontext.AutoContextConfig;
import io.agentscope.core.memory.autocontext.AutoContextHook;
import io.agentscope.core.memory.autocontext.AutoContextMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.session.SessionManager;
import io.agentscope.core.model.Model;
import io.agentscope.core.rag.Knowledge;
import io.agentscope.core.rag.RAGMode;
import io.agentscope.core.session.mysql.MysqlSession;
import io.agentscope.core.tool.Toolkit;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
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
    // 模型
    private final Model model;
    // 模型和 prompt 配置文件
    private final AgentPromptConfig.AgentProperties properties;
    // 模型名称
    private final String agentName;
    // 知识库
    private final Knowledge knowledge;
    // 工具
    private final AnaTools anaTools;

    // Mysql数据库存储 session
    private final DataSource dataSource;

    // 活跃 Agent 注册表: Map<SessionId, ReActAgent>
    private final Map<String, ReActAgent> activeAgents = new ConcurrentHashMap<>();

    public ChatAgent(Model model,
            AgentPromptConfig promptConfig,
            Knowledge knowledge,
            AnaTools anaTools,
            DataSource dataSource) {
        this.model = model;
        this.properties = promptConfig.getAgents().get(AgentType.CHAT.getConfigKey());
        this.agentName = AgentType.CHAT.getAgentName();
        this.knowledge = knowledge;
        this.anaTools = anaTools;
        this.dataSource = dataSource;
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
        toolkit.registerTool(anaTools);
        AutoContextConfig autoContextConfig = AutoContextConfig.builder().tokenRatio(0.4).lastKeep(10).build();
        // Use AutoContextMemory, support context auto compression
        AutoContextMemory memory = new AutoContextMemory(autoContextConfig, model);
        MysqlSession mysqlSession = new MysqlSession(dataSource, "paper_insight", null, true);
        ReActAgent agent = createAgent(toolkit, memory);

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
    private ReActAgent createAgent(Toolkit toolkit, Memory memory) {
        return ReActAgent.builder()
                .name(agentName)
                .sysPrompt(properties.getSysPrompt())
                .maxIters(properties.getMaxIterations())
                .toolkit(toolkit)
//                .hooks(List.of(new MonitoringHook(), new AutoContextHook()))
                .hooks(List.of(new AutoContextHook()))
                .model(model)
                .memory(memory)
//                .enablePlan() // 启用计划功能
                // Agentic RAG 配置 - Agent 自主决定何时检索
                .knowledge(knowledge)
                .ragMode(RAGMode.AGENTIC)
                .build();
    }

    /**
     * 获取历史会话消息
     *
     * @param sessionId 会话ID
     * @return 消息列表
     */
    public List<Msg> getHistory(String sessionId) {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(anaTools);
        // 上下文管理配置
        AutoContextConfig autoContextConfig = AutoContextConfig.builder().tokenRatio(0.4).lastKeep(10).build();
        // 智能上下文内存管理系统，自动压缩、卸载和摘要对话历史。
        AutoContextMemory memory = new AutoContextMemory(autoContextConfig, model);
        MysqlSession mysqlSession = new MysqlSession(dataSource, "paper_insight", null, true);

        ReActAgent agent = createAgent(toolkit, memory);

        if (agent.loadIfExists(mysqlSession, sessionId)) {
            // Memory 中已经加载了历史条目
            return memory.getMessages();
        }
        return List.of();
    }
}
