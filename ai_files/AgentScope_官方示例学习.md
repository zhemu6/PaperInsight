# AgentScope Java 官方示例学习文档

> **学习来源**: boba-tea-shop 官方示例项目  
> **文档时间**: 2026-01-25  
> **目的**: 学习官方推荐的智能体实现方式，为 PaperInsight 项目提供参考

---

## 📋 目录

1. [项目架构概览](#项目架构概览)
2. [核心实现模式](#核心实现模式)
3. [智能体创建方式](#智能体创建方式)
4. [工具系统实现](#工具系统实现)
5. [配置管理方式](#配置管理方式)
6. [流式响应处理](#流式响应处理)
7. [会话管理](#会话管理)
8. [应用到 PaperInsight](#应用到-paperinsight)

---

## 项目架构概览

### 多智能体架构

```
Supervisor Agent (监督者智能体)
    ├── 接收用户请求
    ├── 路由到子智能体
    └── 整合响应返回
         │
         ├── Business Sub Agent (业务子智能体)
         │   └── 通过 MCP 调用 Business MCP Server
         │
         └── Consult Sub Agent (咨询子智能体)
             └── 使用 RAG 知识库检索
```

### 关键组件

1. **Supervisor Agent**: 主入口，协调子智能体
2. **Business Sub Agent**: 处理业务逻辑（订单、反馈等）
3. **Consult Sub Agent**: 处理咨询（产品信息、推荐等）
4. **Business MCP Server**: 提供业务能力的 MCP 工具

---

## 核心实现模式

### 1. 智能体包装类模式

**关键发现**: 官方使用**包装类**而不是直接使用 `ReActAgent`

```java
public class SupervisorAgent {
    private final Model model;
    private final A2aAgentTools tools;
    private final String sysPrompt;

    // 为每个请求创建新的 ReActAgent 实例
    public Flux<Event> stream(Msg msg, String sessionId, String userId) {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(tools);

        AutoContextMemory memory = new AutoContextMemory(autoContextConfig, model);
        MysqlSession mysqlSession = new MysqlSession(dataSource, dbName, null, true);

        ReActAgent agent = createAgent(toolkit, memory);
        agent.loadIfExists(mysqlSession, sessionId);  // 加载会话

        return agent.stream(msg)
            .doFinally(signalType -> {
                agent.saveTo(mysqlSession, sessionId);  // 保存会话
            });
    }

    private ReActAgent createAgent(Toolkit toolkit, Memory memory) {
        return ReActAgent.builder()
            .name("supervisor_agent")
            .sysPrompt(sysPrompt)
            .toolkit(toolkit)
            .hook(new MonitoringHook())
            .model(model)
            .memory(memory)
            .build();
    }
}
```

**设计要点**:

- ✅ **请求隔离**: 每个请求创建新的 Agent 实例，保证隔离
- ✅ **会话持久化**: 使用 `MysqlSession` 保存和恢复会话状态
- ✅ **内存管理**: 使用 `AutoContextMemory` 自动压缩上下文

---

### 2. 配置类模式

#### 模型配置

```java
@Configuration
public class AgentScopeModelConfig {

    @Value("${agentscope.model.provider}")
    private String modelProvider;

    @Value("${agentscope.dashscope.api-key}")
    private String dashscopeApiKey;

    @Value("${agentscope.dashscope.model-name:qwen-max}")
    private String dashscopeModelName;

    @Bean
    public Model model() {
        if ("openai".equalsIgnoreCase(modelProvider)) {
            return OpenAIChatModel.builder()
                .apiKey(openaiApiKey)
                .modelName(openaiModelName)
                .stream(true)
                .formatter(new OpenAIChatFormatter())
                .build();
        } else {
            return DashScopeChatModel.builder()
                .apiKey(dashscopeApiKey)
                .modelName(dashscopeModelName)
                .formatter(new DashScopeChatFormatter())
                .build();
        }
    }
}
```

**要点**:

- 支持多模型提供商（DashScope、OpenAI）
- 使用 `@Value` 从配置文件读取
- 支持环境变量覆盖

#### 智能体配置

```java
@Configuration
public class SupervisorAgentConfig {

    @Autowired
    private SupervisorAgentPromptConfig promptConfig;

    @Bean
    public SupervisorAgent supervisorAgent(Model model, A2aAgentTools tools) {
        return new SupervisorAgent(model, tools, promptConfig.getSupervisorAgentInstruction());
    }
}
```

**要点**:

- 使用 `@Bean` 创建智能体实例
- 提示词从配置类读取（支持 YAML 配置）

#### 提示词配置

```java
@Configuration
@ConfigurationProperties(prefix = "agent.prompts")
public class SupervisorAgentPromptConfig {
    private String supervisorAgentInstruction;
    // getters and setters
}
```

**YAML 配置**:

```yaml
agent:
  prompts:
    supervisor-agent-instruction: |
      角色与职责:
      你是云边奶茶铺的监督者智能体...
```

---

### 3. 工具实现模式

#### A2A 工具（调用其他智能体）

```java
@Component
public class A2aAgentTools {

    private final ObjectProvider<A2aAgent> consultAgentProvider;
    private final ObjectProvider<A2aAgent> businessAgentProvider;

    @Tool(description = "Agent for handling consultation-related requests...")
    public String callConsultAgent(
        @ToolParam(name = "context", description = "Complete context") String context,
        @ToolParam(name = "userId", description = "User's UserId") String userId) {

        context = "<userId>" + userId + "</userId>" + context;
        Msg msg = Msg.builder()
            .content(TextBlock.builder().text(context).build())
            .build();

        A2aAgent consultAgent = consultAgentProvider.getObject();
        return combineAgentResponse(consultAgent.call(msg).block());
    }

    private String combineAgentResponse(Msg responseMsg) {
        if (null == responseMsg) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        responseMsg.getContent().stream()
            .filter(block -> block instanceof TextBlock)
            .forEach(block -> result.append(((TextBlock) block).getText()));
        return result.toString();
    }
}
```

**要点**:

- 使用 `@Tool` 注解定义工具
- 使用 `@ToolParam` 定义参数
- 通过 `ObjectProvider` 获取 A2A Agent（延迟加载）
- 使用 `TextBlock` 构建消息内容

---

### 4. 流式响应处理

#### Controller 实现

```java
@RestController
@RequestMapping("/api/assistant/")
public class SupervisorAgentController {

    private final SupervisorAgent supervisorAgent;

    @GetMapping(path = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(
        @RequestParam(name = "chat_id") String chatID,
        @RequestParam(name = "user_query") String userQuery,
        @RequestParam(name = "user_id") String userID) {

        String userInput = userQuery + "<userId>" + userID + "</userId>";
        Sinks.Many<ServerSentEvent<String>> sink =
            Sinks.many().unicast().onBackpressureBuffer();

        Msg msg = Msg.builder()
            .role(MsgRole.USER)
            .content(TextBlock.builder().text(userInput).build())
            .build();

        processStream(supervisorAgent.stream(msg, chatID, userID), sink);

        return sink.asFlux()
            .doOnCancel(() -> logger.info("Client disconnected"))
            .doOnError(e -> logger.error("Error occurred", e));
    }

    public void processStream(Flux<Event> generator, Sinks.Many<ServerSentEvent<String>> sink) {
        generator
            .doOnNext(output -> logger.info("output = {}", output))
            .filter(event -> !event.isLast())  // 过滤最后事件
            .map(event -> {
                Msg msg = event.getMessage();
                return msg.getContent().stream()
                    .filter(block -> block instanceof TextBlock)
                    .map(block -> ((TextBlock) block).getText())
                    .toList();
            })
            .flatMap(Flux::fromIterable)
            .map(content -> ServerSentEvent.builder(content).build())
            .doOnNext(sink::tryEmitNext)
            .doOnError(e -> {
                logger.error("Error in stream processing", e);
                sink.tryEmitNext(ServerSentEvent.builder("System error...").build());
            })
            .doOnComplete(() -> {
                logger.info("Stream processing completed");
                sink.tryEmitComplete();
            })
            .subscribe();
    }
}
```

**要点**:

- 使用 **Server-Sent Events (SSE)** 实现流式响应
- 使用 `Sinks.Many` 管理流
- 过滤 `isLast()` 事件
- 提取 `TextBlock` 内容
- 错误处理和完成回调

---

### 5. 会话管理

#### MySQL 会话持久化

```java
// 创建会话
MysqlSession mysqlSession = new MysqlSession(
    dataSource,
    System.getenv("DB_NAME"),
    null,
    true
);

// 加载会话
agent.loadIfExists(mysqlSession, sessionId);

// 保存会话
agent.saveTo(mysqlSession, sessionId);
```

**要点**:

- 使用 `agentscope-extensions-session-mysql` 扩展
- 会话 ID 作为唯一标识
- 自动保存和恢复对话历史

---

### 6. 内存管理

#### AutoContextMemory（自动上下文压缩）

```java
AutoContextConfig autoContextConfig = AutoContextConfig.builder()
    .tokenRatio(0.4)    // 压缩到 40% 的 token
    .lastKeep(10)      // 保留最后 10 条消息
    .build();

AutoContextMemory memory = new AutoContextMemory(autoContextConfig, model);
```

**要点**:

- 自动压缩历史对话，节省 token
- 保留最近的对话（`lastKeep`）
- 压缩比例可配置（`tokenRatio`）

---

### 7. A2A Server 实现（子智能体）

#### AgentRunner 实现

```java
@Configuration
public class AgentScopeRunner {

    @Bean
    public AgentRunner agentRunner(
        AgentPromptConfig promptConfig,
        ConsultTools consultTools,
        Knowledge knowledge,
        Model model) {

        Toolkit toolkit = new NacosToolkit();
        toolkit.registerTool(consultTools);

        AutoContextMemory memory = new AutoContextMemory(autoContextConfig, model);

        ReActAgent.Builder builder = ReActAgent.builder()
            .name("consult_agent")
            .sysPrompt(promptConfig.getConsultAgentInstruction())
            .memory(memory)
            .hooks(List.of(new MonitoringHook()))
            .model(model)
            .toolkit(toolkit)
            .knowledge(knowledge)  // RAG 知识库
            .ragMode(RAGMode.AGENTIC);  // Agentic RAG 模式

        return new CustomAgentRunner(builder);
    }

    private static class CustomAgentRunner implements AgentRunner {
        private final ReActAgent.Builder agentBuilder;
        private final Map<String, ReActAgent> agentCache;

        @Override
        public Flux<Event> stream(List<Msg> requestMessages, AgentRequestOptions options) {
            String userId = parseUserIdFromMessages(requestMessages);
            ReActAgent agent = buildReActAgent(userId);
            agentCache.put(options.getTaskId(), agent);

            return agent.stream(requestMessages)
                .doFinally(signal -> agentCache.remove(options.getTaskId()));
        }

        @Override
        public void stop(String taskId) {
            ReActAgent agent = agentCache.remove(taskId);
            if (null != agent) {
                agent.interrupt();  // 中断智能体
            }
        }
    }
}
```

**要点**:

- 实现 `AgentRunner` 接口
- 使用 `agentCache` 管理多个 Agent 实例
- 支持 `stop()` 方法中断智能体
- 集成 RAG 知识库（`Knowledge` + `ragMode`）

---

## 配置文件结构

### application.yml

```yaml
agentscope:
  model:
    provider: ${MODEL_PROVIDER:dashscope}
  dashscope:
    api-key: ${MODEL_API_KEY:-}
    model-name: ${MODEL_NAME:qwen-max}
    base-url: ${MODEL_BASE_URL:-}
  mcp:
    nacos:
      server-addr: ${NACOS_SERVER_ADDR:127.0.0.1:8848}
      namespace: ${NACOS_NAMESPACE:public}
  a2a:
    nacos:
      server-addr: ${NACOS_SERVER_ADDR:127.0.0.1:8848}
      namespace: ${NACOS_NAMESPACE:public}
    server:
      card:
        name: consult_agent
        description: 咨询助手

agent:
  prompts:
    supervisor-agent-instruction: |
      角色与职责...
```

**要点**:

- 使用环境变量配置（`${VAR:default}`）
- 支持多模型提供商
- 提示词放在 YAML 中，便于修改

---

## 应用到 PaperInsight

### 1. 智能体包装类设计

```java
@Component
public class PaperAnalysisAgent {

    private final Model model;
    private final String sysPrompt;
    private final PdfExtractionTool pdfTool;

    public Mono<PaperInsight> analyzePaper(Long paperId, String pdfUrl) {
        // 1. 提取 PDF 文本
        String paperText = pdfTool.extractPdfText(pdfUrl);

        // 2. 创建智能体
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(pdfTool);

        AutoContextMemory memory = new AutoContextMemory(
            AutoContextConfig.builder().tokenRatio(0.4).lastKeep(10).build(),
            model
        );

        ReActAgent agent = ReActAgent.builder()
            .name("paper_analyzer")
            .sysPrompt(sysPrompt)
            .toolkit(toolkit)
            .model(model)
            .memory(memory)
            .build();

        // 3. 调用智能体
        Msg msg = Msg.builder()
            .content(TextBlock.builder().text("请分析这篇论文：\n" + paperText).build())
            .build();

        return agent.call(msg)
            .map(response -> parseAnalysisResult(response));
    }
}
```

### 2. 多智能体并行处理

```java
@Service
public class PaperInsightServiceImpl {

    private final SummaryAgent summaryAgent;
    private final InnovationAgent innovationAgent;
    private final MethodologyAgent methodologyAgent;
    private final ScoreAgent scoreAgent;

    public PaperInsight analyzePaper(Long paperId, String paperText) {
        // 并行调用多个智能体
        Mono<Msg> summaryMono = summaryAgent.analyze(paperText);
        Mono<Msg> innovationMono = innovationAgent.analyze(paperText);
        Mono<Msg> methodologyMono = methodologyAgent.analyze(paperText);
        Mono<Msg> scoreMono = scoreAgent.analyze(paperText);

        // 等待所有结果
        return Mono.zip(summaryMono, innovationMono, methodologyMono, scoreMono)
            .map(tuple -> {
                PaperInsight insight = new PaperInsight();
                insight.setSummaryMarkdown(extractText(tuple.getT1()));
                insight.setInnovationPoints(extractText(tuple.getT2()));
                insight.setMethods(extractText(tuple.getT3()));
                insight.setScore(parseScore(tuple.getT4()));
                return insight;
            })
            .block(Duration.ofMinutes(5));
    }
}
```

### 3. 配置类设计

```java
@Configuration
@ConfigurationProperties(prefix = "agent")
public class AgentConfig {

    @Bean
    public Model model() {
        return DashScopeChatModel.builder()
            .apiKey("${agentscope.dashscope.api-key}")
            .modelName("${agentscope.dashscope.model-name:qwen-plus}")
            .build();
    }

    @Bean
    public SummaryAgent summaryAgent(Model model, AgentPromptConfig promptConfig) {
        return new SummaryAgent(model, promptConfig.getSummaryInstruction());
    }

    // 其他智能体...
}
```

### 4. 工具实现

```java
@Component
public class PdfExtractionTool {

    @Tool(name = "extract_pdf_text",
          description = "从PDF文件中提取文本内容")
    public String extractPdfText(
        @ToolParam(name = "pdf_url", description = "PDF文件URL") String pdfUrl) {

        // 实现 PDF 提取逻辑
        // 使用 Apache PDFBox 或 iText
        return extractedText;
    }
}
```

---

## 关键设计模式总结

### 1. **包装类模式**

- 不直接暴露 `ReActAgent`
- 封装创建逻辑和会话管理
- 便于扩展和维护

### 2. **配置分离**

- 提示词放在 YAML 中
- 使用 `@ConfigurationProperties` 绑定
- 支持环境变量覆盖

### 3. **工具注册**

- 使用 `@Component` + `@Tool` 注解
- 通过 `Toolkit` 注册
- 支持 A2A 和 MCP 工具

### 4. **流式响应**

- 使用 SSE 实现流式输出
- 使用 `Sinks.Many` 管理流
- 支持错误处理和完成回调

### 5. **会话管理**

- 使用 MySQL 持久化会话
- 每个请求独立的会话 ID
- 自动保存和恢复

### 6. **内存优化**

- 使用 `AutoContextMemory` 自动压缩
- 配置压缩比例和保留消息数
- 节省 token 成本

---

## 最佳实践

### ✅ 推荐做法

1. **使用包装类**: 封装智能体创建逻辑
2. **配置分离**: 提示词放在 YAML 中
3. **工具注解**: 使用 `@Tool` 和 `@ToolParam`
4. **流式响应**: 使用 SSE 提供实时反馈
5. **会话管理**: 使用 MySQL 持久化
6. **内存优化**: 使用 AutoContextMemory
7. **错误处理**: 完善的异常处理和日志

### ❌ 避免的做法

1. ❌ 直接使用 `ReActAgent` 作为 Bean（应该用包装类）
2. ❌ 硬编码提示词（应该放在配置中）
3. ❌ 忽略会话管理（应该持久化）
4. ❌ 不使用内存压缩（成本高）
5. ❌ 缺少错误处理

---

## 与 PaperInsight 的对应关系

| 官方示例           | PaperInsight 对应               |
| ------------------ | ------------------------------- |
| SupervisorAgent    | 不需要（直接调用分析智能体）    |
| Business Sub Agent | 不需要（业务逻辑在 Service 层） |
| Consult Sub Agent  | 可参考（RAG 知识库检索）        |
| A2aAgentTools      | 不需要（单应用内调用）          |
| PdfExtractionTool  | **需要实现**                    |
| AutoContextMemory  | **推荐使用**                    |
| MysqlSession       | **推荐使用**（会话管理）        |
| AgentRunner        | 不需要（A2A Server）            |

---

## 实施建议

### 第一阶段：基础实现

1. ✅ 创建 `PaperAnalysisAgent` 包装类
2. ✅ 实现 `PdfExtractionTool`
3. ✅ 创建 4 个核心智能体（摘要、创新点、方法论、评分）
4. ✅ 配置模型和提示词

### 第二阶段：优化

1. ✅ 集成 `AutoContextMemory`
2. ✅ 实现会话管理（如果需要）
3. ✅ 添加流式响应（如果需要实时反馈）
4. ✅ 完善错误处理

### 第三阶段：扩展

1. ✅ 添加 RAG 知识库（用于推荐）
2. ✅ 实现更多工具
3. ✅ 性能优化

---

**文档维护**: 随着开发进度，持续更新实际使用中的经验和问题。
