# 智能代码审查助手 - 前端

基于 Vue 3 + Element Plus 构建的智能代码审查助手 Web 界面。

## 功能特性

- **代码提交审查**：支持 Java、Python、JavaScript、Ruby、Go 等多语言
- **实时状态轮询**：自动轮询任务状态，展示处理进度
- **Markdown 报告渲染**：美化展示 AI 生成的代码审查报告
- **历史记录管理**：本地存储历史任务，支持查看历史审查结果
- **现代化 UI 设计**：渐变背景、圆角卡片、动画效果

## 技术栈

| 技术 | 说明 |
|-----|------|
| Vue 3 | 渐进式 JavaScript 框架 |
| Element Plus | Vue 3 组件库 |
| Axios | HTTP 客户端 |
| marked | Markdown 渲染 |
| Vite | 构建工具 |

## 开发环境

- Node.js 16+
- npm 8+

## 项目安装

```bash
npm install
```

## 开发模式运行

```bash
npm run dev
```

访问 http://localhost:5173 查看应用。

## 生产构建

```bash
npm run build
```

构建产物将输出到 `dist` 目录。

## 项目结构

```
src/
├── api/
│   └── review.js      # API 调用封装
├── assets/
│   └── main.css      # 全局样式
├── App.vue           # 主界面组件
├── main.js          # Vue 入口
└── router/
    └── index.js     # 路由配置
```

## 前后端联调

前端默认连接后端地址 `http://localhost:8080`。如需修改后端地址，请编辑：

- `src/api/review.js` 中的 `BASE_URL` 配置

## API 接口

| 接口 | 方法 | 说明 |
|-----|------|------|
| `/api/review/submit` | POST | 提交代码审查任务 |
| `/api/review/task/{id}` | GET | 查询审查任务状态和结果 |

## 浏览器推荐

- Chrome / Edge / Brave（Chromium 内核浏览器）
- Firefox

推荐安装 Vue.js devtools 浏览器插件以便开发调试。
