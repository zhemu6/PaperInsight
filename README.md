# PaperInsight (学术洞察)

> **智能研读 · 深度洞察 · 高效科研**

PaperInsight 是一个融合了 **多智能体框架 (AgentScope)** 的下一代学术科研辅助平台。它不仅仅是一个论文管理工具，更是您的 AI 科研助手，能够自动提取论文核心创新点、分析研究方法并提供智能评分，帮助科研人员从浩若烟海的文献中快速捕捉高价值信息。

---

## 🌟 核心亮点 (Key Features)

### 1. 🧠 AI 驱动的深度研读

基于 **AgentScope** 多智能体协同框架，系统能够对上传的论文进行深层次的自动化分析：

- **智能摘要**：生成结构化的 Markdown 格式论文摘要。
- **创新点提取**：精准识别论文的 Core Contribution 和 Innovation Points。
- **方法论解析**：自动拆解研究方法与技术路线。
- **价值评分**：基于多维度指标对论文质量进行智能打分 (0-100)。

### 2. 📂 高效文献管理

- **层级化文件夹**：支持无限层级的文件夹结构，像管理本地文件一样管理云端文献。
- **云端存储**：集成腾讯云 COS，实现论文 PDF 的安全存储与秒级预览。
- **隐私控制**：支持私有/公开权限设置，打造个人知识库或通过“论文广场”分享成果。

### 3. 🔍 全局精准检索

- **高级搜索**：支持组合查询（标题、作者、关键词、摘要内容）。
- **全文索引**：(规划中) 基于 Elasticsearch 的深度全文检索能力。

### 4. 🎨 极致用户体验 (UX)

- **现代化 UI**：基于 Element Plus 与 UnoCSS 打造的响应式界面。
- **深色模式**：全站自动适配 Dark Mode，沉浸式夜间阅读体验。
- **交互细节**：流畅的路由过渡、动态加载与实时的状态反馈。

---

## 🏗️ 系统架构与技术栈

### 前端 (Frontend)

采用现代化的 Vue 3 生态系统构建，追求高性能与开发体验。

- **框架**: Vue 3 + TypeScript + Vite
- **UI 组件库**: Element Plus (按需引入)
- **样式引擎**: UnoCSS (实用主义 CSS 引擎) + SCSS
- **状态管理**: Pinia + Pinia-Plugin-Persistedstate (持久化)
- **路由**: Vue Router (自动路由配置)
- **网络请求**: Axios (封装全局拦截器)

### 后端 (Backend)

基于 Spring Boot 3 构建的高可用微服务架构基础。

- **核心框架**: Spring Boot 3.5.10 (Java 21)
- **AI 框架**: [AgentScope](https://github.com/modelscope/agentscope) (v1.0.7)
- **ORM**: MyBatis-Plus 3.5.10
- **数据库**: MySQL 8.0
- **缓存/会话**: Redis + Spring Session Data Redis + Caffeine (本地缓存)
- **消息队列**: RabbitMQ (异步处理 AI 分析任务)
- **对象存储**: 腾讯云 COS
- **工具库**: Hutool, Lombok, Knife4j (接口文档)

### 数据模型

主要包含以下核心实体：

- **SysUser**: 用户信息（账号、密码、角色、状态等）。
- **Folder**: 树形结构的文件夹系统。
- **PaperInfo**: 论文元数据（标题、作者、关键词、COS地址、公开状态）。
- **PaperInsight**: 存放 AI 分析结果（摘要、创新点、方法、评分）。

---

## 📂 目录结构说明

```text
PaperInsight/
├── frontend/                 # 前端项目根目录
│   ├── src/
│   │   ├── api/             # 后端接口定义
│   │   ├── assets/          # 静态资源
│   │   ├── components/      # 公共组件 (BaseHeader, etc.)
│   │   ├── layouts/         # 页面布局 (BasicLayout, BlankLayout)
│   │   ├── pages/           # 页面视图 (PaperSquare, User, Common)
│   │   ├── stores/          # Pinia 状态仓库
│   │   └── styles/          # 全局样式 (Dark mode适配)
│   └── index.html
├── src/                      # 后端 Java 源码
│   └── main/java/com/zhemu/paperinsight/
│       ├── controller/       # Web 控制层
│       ├── service/          # 业务逻辑层
│       ├── mapper/           # 数据访问层
│       └── model/            # 实体类 (Entity, DTO, VO)
└── sql/                      # 数据库初始化脚本
    └── creat_table.sql       # 建表语句
```

---

## 🚀 快速开始

### 1. 环境准备

- JDK 21+
- Node.js 18+
- MySQL 8.0
- Redis

### 2. 后端启动

1. 导入项目至 IDEA。
2. 配置 `application.yml` 中的 MySQL、Redis 和 COS 密钥信息。
3. 运行 `PaperInsightApplication.java`。

### 3. 前端启动

```bash
cd frontend
npm install
npm run dev
```

---

## 🤝 贡献与反馈

PaperInsight 正在积极开发中。如果您有任何建议或发现了 Bug，欢迎提交 Issue 或 Pull Request。

---

© 2026 PaperInsight Team. All Rights Reserved.
