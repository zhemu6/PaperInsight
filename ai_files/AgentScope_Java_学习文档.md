# AgentScope Java 学习文档

> **文档目的**: 为 PaperInsight 项目的 AI 论文分析功能开发提供 AgentScope Java 框架的学习指南  
> **文档时间**: 2026-01-25  
> **参考资源**: 
> - 官方文档: https://java.agentscope.io/
> - GitHub: https://github.com/agentscope-ai/agentscope-java
> - 版本: 1.0.7

---

## 📚 目录

1. [框架概述](#框架概述)
2. [核心概念](#核心概念)
3. [快速开始](#快速开始)
4. [模型配置](#模型配置)
5. [ReActAgent 详解](#reactagent-详解)
6. [工具系统](#工具系统)
7. [结构化输出](#结构化输出)
8. [多智能体协作](#多智能体协作)
9. [在 Spring Boot 中使用](#在-spring-boot-中使用)
10. [项目集成方案](#项目集成方案)

---

## 框架概述

### 什么是 AgentScope Java?

AgentScope Java 是一个**面向智能体的编程框架**，用于构建基于大语言模型（LLM）的应用程序。它提供了构建智能代理所需的一切：ReAct 推理、工具调用、内存管理、多智能体协作等。

### 核心特性

1. **ReAct 推理范式**: 智能体可以自主规划和执行复杂任务
2. **工具系统**: 支持同步/异步工具调用
3. **结构化输出**: 自动解析和验证 LLM 输出
4. **多智能体协作**: 支持智能体之间的协作
5. **内存管理**: 长期记忆和语义搜索
6. **生产级特性**: 高性能、安全沙箱、可观测性

### 技术架构

- **响应式架构**: 基于 Project Reactor，非阻塞执行
- **GraalVM 支持**: 支持原生镜像编译，冷启动时间 200ms
- **OpenTelemetry 集成**: 分布式追踪
- **Spring Boot 集成**: 提供 Spring Boot Starter

---

## 核心概念

### 1. Agent (智能体)

智能体是 AgentScope 的核心概念，代表一个可以执行任务的 AI 实体。

**主要类型**:
- `ReActAgent`: 基于 ReAct 范式的智能体（推理-行动循环）
- `PipelineAgent`: 管道式智能体
- `MsgHubAgent`: 消息中心智能体

### 2. Model (模型)

模型是智能体使用的 LLM 后端，支持多种提供商：

- **DashScope**: 阿里云通义千问系列
- **OpenAI**: GPT 系列
- **Anthropic**: Claude 系列
- **Gemini**: Google Gemini
- **Ollama**: 本地模型

### 3. Tool (工具)

工具是智能体可以调用的函数，用于执行具体任务。

**工具类型**:
- 同步工具
- 异步工具
- MCP 协议工具

### 4. Msg (消息)

消息是智能体之间通信的基本单位，包含文本内容、工具调用结果等。

### 5. Structured Output (结构化输出)

将 LLM 的非结构化输出解析为类型安全的 Java 对象。

---

## 快速开始

### 基础依赖

项目已包含依赖：
```xml
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-spring-boot-starter</artifactId>
    <version>1.0.7</version>
</dependency>
```

### 最简单的示例

```java
import io.agentscope.agent.ReActAgent;
import io.agentscope.model.DashScopeChatModel;
import io.agentscope.msg.Msg;
import reactor.core.publisher.Mono;

// 1. 创建模型
DashScopeChatModel model = DashScopeChatModel.builder()
    .apiKey(System.getenv("DASHSCOPE_API_KEY"))
    .modelName("qwen-max")  // 或 "qwen-plus", "qwen-turbo"
    .build();

// 2. 创建智能体
ReActAgent agent = ReActAgent.builder()
    .name("Assistant")
    .sysPrompt("You are a helpful AI assistant.")
    .model(model)
    .build();

// 3. 调用智能体
Msg response = agent.call(Msg.builder()
    .textContent("Hello!")
    .build()).block();

// 4. 获取响应
System.out.println(response.getTextContent());
```

---

## 模型配置

### DashScopeChatModel (通义千问)

**推荐用于本项目**，因为：
- 国内访问稳定
- 支持中文理解
- 性价比高

```java
DashScopeChatModel model = DashScopeChatModel.builder()
    .apiKey("your-api-key")  // 从环境变量或配置文件读取
    .modelName("qwen-max")   // 可选: qwen-max, qwen-plus, qwen-turbo
    .baseUrl("https://dashscope.aliyuncs.com/api/v1")  // 可选，默认值
    .stream(false)  // 是否流式输出，默认 false
    .build();
```

**模型选择建议**:
- `qwen-max`: 最强性能，适合复杂分析任务
- `qwen-plus`: 平衡性能和成本
- `qwen-turbo`: 快速响应，适合简单任务

### 其他模型示例

```java
// OpenAI
OpenAIChatModel model = OpenAIChatModel.builder()
    .apiKey("sk-...")
    .modelName("gpt-4")
    .build();

// Anthropic Claude
AnthropicChatModel model = AnthropicChatModel.builder()
    .apiKey("sk-ant-...")
    .modelName("claude-3-opus-20240229")
    .build();
```

---

## ReActAgent 详解

### ReAct 范式

ReAct (Reasoning + Acting) 是一种结合推理和行动的范式：

1. **Reasoning**: 智能体思考下一步行动
2. **Acting**: 执行工具调用
3. **Observing**: 观察工具执行结果
4. **循环**: 重复上述过程直到完成任务

### 创建 ReActAgent

```java
ReActAgent agent = ReActAgent.builder()
    .name("PaperAnalyzer")  // 智能体名称
    .sysPrompt("你是一个专业的学术论文分析助手...")  // 系统提示词
    .model(model)  // 使用的模型
    .maxIterations(10)  // 最大迭代次数，防止无限循环
    .maxToolCallPerIteration(5)  // 每次迭代最大工具调用数
    .build();
```

### 系统提示词设计

系统提示词对智能体行为至关重要：

```java
String sysPrompt = """
    你是一个专业的学术论文分析助手。你的任务是：
    1. 仔细阅读论文内容
    2. 提取关键信息（摘要、创新点、方法等）
    3. 提供结构化的分析结果
    
    请用中文回答，保持专业和准确。
    """;
```

### 调用智能体

```java
// 同步调用（阻塞）
Msg response = agent.call(Msg.builder()
    .textContent("请分析这篇论文：...")
    .build()).block();

// 异步调用（推荐）
Mono<Msg> responseMono = agent.call(Msg.builder()
    .textContent("请分析这篇论文：...")
    .build());

responseMono.subscribe(
    msg -> System.out.println(msg.getTextContent()),
    error -> System.err.println("Error: " + error.getMessage())
);
```

---

## 工具系统

### 什么是工具？

工具是智能体可以调用的函数，用于执行具体任务，如：
- 提取 PDF 文本
- 搜索数据库
- 调用外部 API
- 执行计算

### 创建工具

```java
import io.agentscope.tool.Tool;
import io.agentscope.tool.ToolFunction;

// 方式1: 使用注解
@ToolFunction(name = "extract_pdf_text", description = "从PDF文件中提取文本内容")
public String extractPdfText(
    @Tool.Param(name = "pdf_path", description = "PDF文件路径") String pdfPath
) {
    // 实现PDF文本提取逻辑
    return "提取的文本内容...";
}

// 方式2: 使用 ToolFunction 对象
ToolFunction tool = ToolFunction.builder()
    .name("calculate_score")
    .description("计算论文质量评分")
    .function(args -> {
        // 实现评分逻辑
        return "85";
    })
    .build();
```

### 注册工具到智能体

```java
ReActAgent agent = ReActAgent.builder()
    .name("PaperAnalyzer")
    .sysPrompt("...")
    .model(model)
    .tools(Arrays.asList(
        extractPdfTextTool,
        calculateScoreTool
    ))
    .build();
```

### 工具调用流程

1. 智能体分析任务
2. 决定需要调用哪些工具
3. 调用工具并获取结果
4. 基于结果继续推理
5. 重复直到完成任务

---

## 结构化输出

### 为什么需要结构化输出？

LLM 的输出是文本，但我们需要结构化的数据（如 JSON 对象）。AgentScope 提供了自动解析和验证机制。

### 使用示例

```java
import io.agentscope.output.StructuredOutput;
import io.agentscope.output.parser.MarkdownJsonDictParser;

// 1. 定义输出结构
public class PaperAnalysis {
    private String summary;
    private List<String> innovationPoints;
    private String methodology;
    private Integer score;
    
    // getters and setters
}

// 2. 创建解析器
MarkdownJsonDictParser parser = new MarkdownJsonDictParser();

// 3. 在智能体中配置
ReActAgent agent = ReActAgent.builder()
    .name("PaperAnalyzer")
    .sysPrompt("""
        请分析论文并返回JSON格式的结果：
        {
            "summary": "论文摘要",
            "innovationPoints": ["创新点1", "创新点2"],
            "methodology": "方法论描述",
            "score": 85
        }
        """)
    .model(model)
    .structuredOutput(parser)
    .build();

// 4. 调用并解析
Msg response = agent.call(Msg.builder()
    .textContent("分析这篇论文...")
    .build()).block();

// 5. 解析为对象
PaperAnalysis analysis = parser.parse(response, PaperAnalysis.class);
```

---

## 多智能体协作

### 为什么需要多智能体？

对于复杂的论文分析任务，可以拆分为多个子任务，每个智能体负责一个方面：

- **摘要智能体**: 专门生成摘要
- **创新点智能体**: 专门提取创新点
- **方法论智能体**: 专门分析研究方法
- **评分智能体**: 专门进行评分

### 多智能体协作模式

#### 1. Pipeline 模式（管道）

```java
import io.agentscope.agent.PipelineAgent;

// 创建多个智能体
ReActAgent summaryAgent = ReActAgent.builder()
    .name("SummaryAgent")
    .sysPrompt("你的任务是生成论文摘要...")
    .model(model)
    .build();

ReActAgent innovationAgent = ReActAgent.builder()
    .name("InnovationAgent")
    .sysPrompt("你的任务是提取创新点...")
    .model(model)
    .build();

// 创建管道
PipelineAgent pipeline = PipelineAgent.builder()
    .agents(Arrays.asList(summaryAgent, innovationAgent))
    .build();

// 执行
Msg result = pipeline.call(inputMsg).block();
```

#### 2. MsgHub 模式（消息中心）

```java
import io.agentscope.agent.MsgHubAgent;

MsgHubAgent hub = MsgHubAgent.builder()
    .agents(Arrays.asList(agent1, agent2, agent3))
    .build();

// 智能体之间可以互相通信
```

### 推荐方案：独立调用

对于论文分析，推荐**独立调用多个智能体**，而不是复杂的协作模式：

```java
// 1. 提取PDF文本（工具）
String paperText = extractPdfText(pdfPath);

// 2. 并行调用多个智能体
Mono<Msg> summaryMono = summaryAgent.call(
    Msg.builder().textContent("生成摘要：" + paperText).build()
);

Mono<Msg> innovationMono = innovationAgent.call(
    Msg.builder().textContent("提取创新点：" + paperText).build()
);

Mono<Msg> methodologyMono = methodologyAgent.call(
    Msg.builder().textContent("分析方法论：" + paperText).build()
);

Mono<Msg> scoreMono = scoreAgent.call(
    Msg.builder().textContent("评分：" + paperText).build()
);

// 3. 等待所有结果
List<Msg> results = Mono.zip(
    summaryMono, innovationMono, methodologyMono, scoreMono
).block();

// 4. 解析结果并保存
```

---

## 在 Spring Boot 中使用

### 配置 AgentScope

在 `application.yml` 中配置：

```yaml
agent:
  scope:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY:your-default-key}
      model-name: qwen-max
      base-url: https://dashscope.aliyuncs.com/api/v1
```

### 创建配置类

```java
@Configuration
public class AgentScopeConfig {
    
    @Value("${agent.scope.dashscope.api-key}")
    private String apiKey;
    
    @Value("${agent.scope.dashscope.model-name:qwen-max}")
    private String modelName;
    
    @Bean
    public DashScopeChatModel dashScopeModel() {
        return DashScopeChatModel.builder()
            .apiKey(apiKey)
            .modelName(modelName)
            .build();
    }
    
    @Bean
    public ReActAgent summaryAgent(DashScopeChatModel model) {
        return ReActAgent.builder()
            .name("SummaryAgent")
            .sysPrompt("你是一个专业的论文摘要生成助手...")
            .model(model)
            .maxIterations(5)
            .build();
    }
    
    // 其他智能体...
}
```

### 在 Service 中使用

```java
@Service
@RequiredArgsConstructor
public class PaperInsightServiceImpl implements PaperInsightService {
    
    private final ReActAgent summaryAgent;
    private final ReActAgent innovationAgent;
    // ...
    
    public PaperInsight analyzePaper(Long paperId, String paperText) {
        // 调用智能体分析
        Msg summaryMsg = summaryAgent.call(
            Msg.builder().textContent("生成摘要：" + paperText).build()
        ).block();
        
        // 解析结果并保存
        // ...
    }
}
```

---

## 项目集成方案

### 整体架构

```
用户上传论文
    ↓
PDF 上传到 COS
    ↓
发送分析任务到 RabbitMQ
    ↓
消费者处理：
    1. 从 COS 下载 PDF
    2. 提取 PDF 文本
    3. 调用多个智能体分析
    4. 保存分析结果到数据库
    ↓
通知用户（WebSocket 或轮询）
```

### 1. PDF 文本提取工具

```java
@Component
public class PdfExtractionTool {
    
    @ToolFunction(name = "extract_pdf_text", 
                  description = "从PDF文件中提取文本内容")
    public String extractPdfText(
        @Tool.Param(name = "pdf_url", description = "PDF文件URL") String pdfUrl
    ) {
        try {
            // 1. 从 COS 下载 PDF
            byte[] pdfBytes = cosManager.downloadFile(pdfUrl);
            
            // 2. 使用 PDFBox 提取文本
            PDDocument document = PDDocument.load(pdfBytes);
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            document.close();
            
            return text;
        } catch (Exception e) {
            throw new RuntimeException("PDF提取失败", e);
        }
    }
}
```

### 2. 创建多个分析智能体

```java
@Configuration
public class PaperAnalysisAgentsConfig {
    
    @Bean
    public ReActAgent summaryAgent(DashScopeChatModel model) {
        return ReActAgent.builder()
            .name("SummaryAgent")
            .sysPrompt("""
                你是一个专业的学术论文摘要生成助手。
                请仔细阅读论文内容，生成结构化的Markdown格式摘要。
                摘要应包含：研究背景、研究目标、主要方法、关键结果、结论。
                """)
            .model(model)
            .maxIterations(5)
            .build();
    }
    
    @Bean
    public ReActAgent innovationAgent(DashScopeChatModel model) {
        return ReActAgent.builder()
            .name("InnovationAgent")
            .sysPrompt("""
                你是一个专业的论文创新点提取助手。
                请识别论文的核心贡献（Core Contribution）和创新点（Innovation Points）。
                返回格式：每个创新点一行，用列表形式。
                """)
            .model(model)
            .maxIterations(5)
            .build();
    }
    
    @Bean
    public ReActAgent methodologyAgent(DashScopeChatModel model) {
        return ReActAgent.builder()
            .name("MethodologyAgent")
            .sysPrompt("""
                你是一个专业的研究方法论分析助手。
                请分析论文的研究方法、技术路线和实验设计。
                返回结构化的方法论描述。
                """)
            .model(model)
            .maxIterations(5)
            .build();
    }
    
    @Bean
    public ReActAgent scoreAgent(DashScopeChatModel model) {
        return ReActAgent.builder()
            .name("ScoreAgent")
            .sysPrompt("""
                你是一个专业的论文质量评分助手。
                请从以下维度对论文进行评分（0-100分）：
                1. 创新性（30分）
                2. 技术性（30分）
                3. 实用性（20分）
                4. 写作质量（20分）
                
                返回格式：{"score": 85, "reasoning": "评分理由"}
                """)
            .model(model)
            .maxIterations(5)
            .build();
    }
}
```

### 3. 分析服务实现

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class PaperInsightServiceImpl implements PaperInsightService {
    
    private final PaperInsightMapper paperInsightMapper;
    private final PaperInfoMapper paperInfoMapper;
    private final CosManager cosManager;
    private final PdfExtractionTool pdfExtractionTool;
    
    private final ReActAgent summaryAgent;
    private final ReActAgent innovationAgent;
    private final ReActAgent methodologyAgent;
    private final ReActAgent scoreAgent;
    
    /**
     * 分析论文
     */
    public void analyzePaper(Long paperId) {
        try {
            // 1. 获取论文信息
            PaperInfo paper = paperInfoMapper.selectById(paperId);
            if (paper == null) {
                throw new BusinessException("论文不存在");
            }
            
            // 2. 提取PDF文本
            String paperText = pdfExtractionTool.extractPdfText(paper.getCosUrl());
            if (StringUtils.isBlank(paperText)) {
                throw new BusinessException("PDF文本提取失败");
            }
            
            // 3. 并行调用多个智能体分析
            Mono<Msg> summaryMono = summaryAgent.call(
                Msg.builder().textContent("请生成论文摘要：\n" + paperText).build()
            );
            
            Mono<Msg> innovationMono = innovationAgent.call(
                Msg.builder().textContent("请提取创新点：\n" + paperText).build()
            );
            
            Mono<Msg> methodologyMono = methodologyAgent.call(
                Msg.builder().textContent("请分析方法论：\n" + paperText).build()
            );
            
            Mono<Msg> scoreMono = scoreAgent.call(
                Msg.builder().textContent("请评分：\n" + paperText).build()
            );
            
            // 4. 等待所有结果
            Tuple4<Msg, Msg, Msg, Msg> results = Mono.zip(
                summaryMono, innovationMono, methodologyMono, scoreMono
            ).block(Duration.ofMinutes(5)); // 设置超时
            
            // 5. 解析结果
            String summary = results.getT1().getTextContent();
            String innovation = results.getT2().getTextContent();
            String methodology = results.getT3().getTextContent();
            String scoreText = results.getT4().getTextContent();
            
            // 解析评分（从JSON中提取）
            Integer score = parseScore(scoreText);
            
            // 6. 保存分析结果
            PaperInsight insight = PaperInsight.builder()
                .paperId(paperId)
                .summaryMarkdown(summary)
                .innovationPoints(innovation)
                .methods(methodology)
                .score(score)
                .build();
            
            // 检查是否已存在
            PaperInsight existing = paperInsightMapper.selectOne(
                new QueryWrapper<PaperInsight>()
                    .eq("paper_id", paperId)
                    .eq("is_delete", 0)
            );
            
            if (existing != null) {
                insight.setId(existing.getId());
                paperInsightMapper.updateById(insight);
            } else {
                paperInsightMapper.insert(insight);
            }
            
            log.info("论文分析完成，paperId: {}", paperId);
            
        } catch (Exception e) {
            log.error("论文分析失败，paperId: {}", paperId, e);
            throw new BusinessException("论文分析失败: " + e.getMessage());
        }
    }
    
    private Integer parseScore(String scoreText) {
        try {
            // 尝试从JSON中解析
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(scoreText);
            if (node.has("score")) {
                return node.get("score").asInt();
            }
        } catch (Exception e) {
            // 如果解析失败，尝试正则提取数字
            Pattern pattern = Pattern.compile("\\d+");
            Matcher matcher = pattern.matcher(scoreText);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group());
            }
        }
        return null;
    }
}
```

### 4. RabbitMQ 消费者

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class PaperAnalysisConsumer {
    
    private final PaperInsightService paperInsightService;
    
    @RabbitListener(queues = "paper.analysis.queue")
    public void handleAnalysisTask(Long paperId) {
        log.info("收到论文分析任务，paperId: {}", paperId);
        try {
            paperInsightService.analyzePaper(paperId);
        } catch (Exception e) {
            log.error("论文分析任务处理失败，paperId: {}", paperId, e);
            // 可以发送到死信队列或重试
        }
    }
}
```

### 5. 触发分析

在论文上传后触发分析：

```java
@Service
public class PaperInfoServiceImpl {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    public Long addPaper(PaperAddRequest request) {
        // ... 保存论文信息
        
        // 发送分析任务到队列
        rabbitTemplate.convertAndSend("paper.analysis.queue", paperId);
        
        return paperId;
    }
}
```

---

## 最佳实践

### 1. 错误处理

```java
try {
    Msg response = agent.call(msg).block();
} catch (Exception e) {
    if (e instanceof TimeoutException) {
        // 处理超时
    } else if (e instanceof IllegalArgumentException) {
        // 处理参数错误
    } else {
        // 处理其他错误
    }
}
```

### 2. 超时控制

```java
Msg response = agent.call(msg)
    .timeout(Duration.ofMinutes(5))
    .block();
```

### 3. 重试机制

```java
Msg response = agent.call(msg)
    .retry(3)  // 重试3次
    .block();
```

### 4. 流式输出（可选）

```java
DashScopeChatModel model = DashScopeChatModel.builder()
    .apiKey(apiKey)
    .modelName("qwen-max")
    .stream(true)  // 启用流式输出
    .build();

agent.call(msg)
    .doOnNext(msg -> {
        // 实时处理流式输出
        System.out.print(msg.getTextContent());
    })
    .blockLast();
```

### 5. 成本控制

- 使用 `qwen-turbo` 进行简单任务
- 使用 `qwen-plus` 进行标准任务
- 仅在必要时使用 `qwen-max`
- 设置合理的 `maxIterations` 限制

---

## 常见问题

### Q1: 如何获取 API Key?

A: 访问阿里云 DashScope 控制台：https://dashscope.console.aliyun.com/

### Q2: 如何处理长文本？

A: 可以分段处理，或者使用支持长上下文的模型（如 qwen-max-128k）

### Q3: 如何提高分析质量？

A: 
- 优化系统提示词
- 使用更强大的模型
- 增加迭代次数
- 使用结构化输出

### Q4: 如何监控和分析成本？

A: 在 DashScope 控制台查看 API 调用统计

---

## 下一步行动

1. ✅ 配置 DashScope API Key
2. ✅ 创建 PDF 提取工具
3. ✅ 创建多个分析智能体
4. ✅ 实现分析服务
5. ✅ 集成 RabbitMQ 异步处理
6. ✅ 测试和优化

---

**文档维护**: 随着开发进度，持续更新此文档，记录实际使用中的经验和问题。
