# AI 论文分析系统实施计划

## 目标

基于 AgentScope 和 RabbitMQ 实现一个健壮的异步 AI 论文分析系统。系统将从上传的 PDF 中提取文本，利用多智能体工作流（摘要、创新点、方法论、评分）进行深度分析，并将结构化结果存储至数据库。

## 需要用户审查的内容

> [!IMPORTANT]
> **模型配置**: 请确保 AgentScope 的模型配置（API Key、模型名称）已正确设置。
> **PDF 库选择**: 计划使用 `Apache PDFBox` 进行 Java 端 PDF 文本提取。

## 拟议变更

### 后端 (Java/Spring Boot)

#### [NEW] 依赖项 (Dependencies)

- 在 `pom.xml` 中添加 `pdfbox` 依赖。
- 确保包含 `spring-boot-starter-amqp` 以支持 RabbitMQ。
- 添加 `spring-boot-starter-data-elasticsearch` 或 `elasticsearch-java` 以支持后续 RAG (可选)。

#### [NEW] `com.paperinsight.backend.service.ai`

- `PdfExtractionService`: 封装 PDF 文本提取逻辑。
- `AgentScopeService`: 封装与 AgentScope 交互的客户端/包装器。
- `AnalysisTaskConsumer`: RabbitMQ 消费者监听器。
  - 步骤 1: 提取文本。
  - 步骤 2: 触发分析智能体 (Summary, Innovation, etc.)。
  - 步骤 3: 触发 RAG 入库 (索引到 ES/Knowledge)。
- `ChatService`: 管理 ChatAgent 会话与交互。

#### [NEW] `com.paperinsight.backend.agent`

- `PaperAnalysisAgent`: 分析类 Agent 的包装器。
- `ChatAgent`: 对话类 Agent 的包装器 (集成知识库)。
- `KnowledgeConfig`: AgentScope Knowledge (ES) 配置。

#### [MODIFY] `PaperInsightServiceImpl`

- 实现 `analyzePaper(Long paperId)` 方法以触发异步分析流程。
- 实现更新分析状态和保存结果的方法。

#### [MODIFY] 数据库

- 验证 `paper_insight` 表存在并包含必要字段 (summary, innovation, methodology, score, status)。

### AI / AgentScope

- 定义智能体配置 (Prompts/System Messages):
  - `SummarizerAgent` (摘要)
  - `InnovationAgent` (创新点)
  - `MethodologyAgent` (方法论)
  - `ReviewerAgent` (评分/审稿)
  - `ChatAgent` (带有搜索工具的问答 Agent)
- RAG 设置:
  - 配置 `ElasticsearchKnowledge` 或同等适配器。
  - 确保在消费者中实现 `knowledge.addDocuments()` 的逻辑。

## 验证计划

### 自动化测试

- 编写 `PdfExtractionService` 的单元测试，解析示例 PDF。
- 编写 `AnalysisTaskConsumer` 的集成测试 (Mock RabbitMQ)。

### 手动验证

- 上传 PDF 并触发分析。
- 验证数据库中的状态变更。
- 检查 RabbitMQ 队列消息。
- 验证最终生成的分析内容是否落库。
