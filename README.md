# 智能研发助理

基于 Spring Boot + Vue 3 + AI 构建的一站式代码辅助平台。

## ✨ 功能特性

### 代码审查
- 支持多语言代码审查（Java、Python、JavaScript、Go、Ruby）
- 安全漏洞检测、性能问题分析、代码规范检查
- 异步任务处理，支持任务状态查询

### 智能助理（对话模式）
- **⚡ 代码生成**：根据自然语言需求描述生成高质量代码
- **🔍 逐行分析**：逐行检查代码潜在问题，输出问题表格
- **🚀 代码优化**：从可读性、性能、健壮性等维度优化代码
- **✨ 代码补全**：补全带 `//TODO` 的不完整代码
- **🧪 单元测试生成**：为源代码生成完整单元测试
- **📊 复杂度分析**：分析圈复杂度、认知复杂度等指标

## 🛠️ 技术栈

| 分类 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.2.x |
| 语言 | Java | 21 |
| 前端框架 | Vue | 3.4.x |
| UI 组件 | Element Plus | 2.8.x |
| 代码编辑器 | Monaco Editor | latest |
| 构建工具 | Vite | 8.0.x |
| 数据库 | PostgreSQL / MySQL | 14+ / 8+ |

## 🚀 快速开始

### 环境要求
- JDK 21+
- Node.js 18+
- PostgreSQL 14+ 或 MySQL 8+

### 后端启动

```bash
cd code-review-agent
mvn spring-boot:run
```

服务将在 `http://localhost:8080` 启动

### 前端启动

```bash
cd code-review-frontend
npm install
npm run dev
```

前端将在 `http://localhost:5173` 启动

### 生产构建

```bash
# 后端打包
cd code-review-agent
mvn clean package

# 前端打包
cd code-review-frontend
npm run build
```

## ⚙️ 配置说明

### 后端配置 (`application.yml`)

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/example_db
    username: admin
    password: password
    driver-class-name: org.postgresql.Driver

# AI 配置
ai:
  api-key: your-api-key
  api-url: https://api.example.com/v1/chat/completions
```

### 前端配置 (`vite.config.js`)

开发环境自动代理到后端，生产环境需配置正确的 API 地址。

## 📁 项目结构

```
.
├── code-review-agent/          # 后端服务
│   ├── src/main/java/com/agent/codereview/
│   │   ├── agent/              # Agent 实现（核心业务逻辑）
│   │   ├── controller/         # REST API 控制层
│   │   ├── service/            # 业务逻辑层
│   │   ├── repository/         # 数据访问层（JPA）
│   │   ├── dto/                # 数据传输对象
│   │   ├── config/             # 配置类
│   │   └── entity/             # 数据库实体
│   └── src/main/resources/
│       └── application.yml     # 应用配置
├── code-review-frontend/       # 前端应用
│   ├── src/
│   │   ├── components/         # Vue 组件
│   │   │   ├── ChatAssistant.vue    # 对话模式智能助理
│   │   │   └── CodeEditor.vue       # Monaco 代码编辑器
│   │   ├── api/                # API 调用封装
│   │   ├── App.vue             # 主应用组件
│   │   └── main.js             # 入口文件
│   ├── index.html              # HTML 模板
│   ├── vite.config.js          # Vite 配置
│   └── package.json            # 依赖配置
├── md文档/                     # 项目文档
│   └── 生成数据库.md          # 数据库创建指导
└── README.md                   # 项目说明文档
```

## 📝 API 接口

### 代码审查

| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/review/submit` | POST | 提交代码审查任务 |
| `/api/review/task/{id}` | GET | 查询任务状态和结果 |

### 智能助理

| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/assistant/execute` | POST | 执行智能助理任务 |
| `/api/assistant/agents` | GET | 获取可用 Agent 列表 |

### 请求示例

**提交代码审查**
```json
POST /api/review/submit
{
  "codeContent": "public class HelloWorld { ... }",
  "language": "java"
}
```

**执行智能助理任务**
```json
POST /api/assistant/execute
{
  "actionType": "CodeGenerationAgent",
  "input": "生成一个Java爬虫示例",
  "language": "java"
}
```

## 🗄️ 数据库设计

### review_task 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| code_content | TEXT | 代码内容 |
| language | VARCHAR(50) | 编程语言 |
| task_type | VARCHAR(50) | 任务类型（REVIEW/Agent名称） |
| status | INT | 状态（0-待处理，1-处理中，2-成功，3-失败） |
| result_summary | TEXT | 结果摘要 |
| result_details | TEXT | 详细结果 |
| extra_context | TEXT | 额外上下文 |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

## 📄 许可证

MIT License

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📧 联系方式

如有问题或建议，请通过以下方式联系：
- 提交 GitHub Issue
- 发送邮件至 1597338110@qq.com

---

**项目已成功升级为智能研发助理，支持六大核心 AI 研发能力！** 🚀
