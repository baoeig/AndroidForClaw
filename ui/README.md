# AndroidForClaw Web Control UI

OpenClaw-aligned web control interface for AndroidForClaw Android automation platform.

## 🏗️ 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| **Lit** | 3.3.2 | Web Components 框架 (轻量级 5KB) |
| **TypeScript** | 5.7+ | 类型安全 |
| **Vite** | 7.3.1 | 构建工具和开发服务器 |
| **Signals** | 1.0.2 | 响应式状态管理 |
| **marked** | 17.0.3 | Markdown 渲染 |
| **DOMPurify** | 3.3.1 | HTML 消毒 |

**设计理念**: 与 OpenClaw 保持完全一致的架构和技术选型。

---

## 📂 目录结构

```
ui/
├── src/
│   ├── main.ts                    # 入口文件
│   ├── ui/                        # UI 组件和逻辑
│   │   ├── app.ts                 # 主应用组件 (LitElement)
│   │   ├── gateway.ts             # WebSocket 客户端
│   │   ├── types.ts               # TypeScript 类型定义
│   │   ├── controllers/           # 数据加载逻辑 (TODO)
│   │   └── views/                 # UI 视图组件 (TODO)
│   └── styles/                    # 样式
│       ├── styles.css             # 总入口
│       ├── base.css               # 基础样式 (变量、排版)
│       ├── layout.css             # 布局工具类
│       └── components.css         # 组件样式
├── public/                        # 静态资源
├── index.html                     # HTML 模板
├── vite.config.ts                 # Vite 配置
├── tsconfig.json                  # TypeScript 配置
└── package.json                   # 依赖定义
```

---

## 🚀 快速开始

### 1. 安装依赖

```bash
cd ui
npm install
# 或
pnpm install
```

### 2. 开发模式

```bash
npm run dev
# 或
pnpm dev
```

开发服务器将在 `http://localhost:5173` 启动，并自动在局域网内可访问（`0.0.0.0:5173`）。

**局域网访问**:
- 确保手机和电脑在同一局域网
- 访问: `http://<电脑IP>:5173`
- 例如: `http://192.168.1.100:5173`

### 3. 生产构建

```bash
npm run build
# 或
pnpm build
```

构建产物输出到: `../app/src/main/assets/webui/`

### 4. 预览生产版本

```bash
npm run preview
# 或
pnpm preview
```

---

## 🔌 WebSocket 通信

### 连接配置

**开发模式**:
- WebSocket 通过 Vite proxy 转发到 `ws://localhost:8080/ws`
- HTTP API 通过 Vite proxy 转发到 `http://localhost:8080/api`

**生产模式**:
- WebSocket 自动连接到当前主机的 `/ws` 路径
- 例如: `ws://192.168.1.100:8080/ws`

### 协议格式

参考 OpenClaw Gateway 协议:

**请求**:
```json
{
  "type": "req",
  "id": "req-1",
  "method": "device.screenshot",
  "params": { "width": 1080 }
}
```

**响应**:
```json
{
  "type": "res",
  "id": "req-1",
  "ok": true,
  "payload": { "data": "base64..." }
}
```

**事件**:
```json
{
  "type": "event",
  "event": "chat.message",
  "payload": { ... }
}
```

---

## 🎨 主题系统

支持浅色和深色主题:

```typescript
// 切换主题
document.documentElement.className = 'light-theme';  // 浅色
document.documentElement.className = 'dark-theme';   // 深色
```

**CSS 变量**:
- `--bg-primary` - 主背景色
- `--bg-secondary` - 次级背景色
- `--text-primary` - 主文本色
- `--text-secondary` - 次级文本色
- `--color-primary` - 主题色 (#0066cc)
- `--color-success` - 成功色 (#28a745)
- `--color-error` - 错误色 (#dc3545)

---

## 📱 核心视图

| 视图 | 路由 | 功能 | 状态 |
|------|------|------|------|
| 对话 | `/chat` | 聊天界面、工具调用流 | 🚧 开发中 |
| 设备 | `/device` | 设备控制、权限状态 | 🚧 开发中 |
| 截图 | `/screenshot` | 实时截图预览 | 🚧 开发中 |
| 会话 | `/sessions` | 会话管理、历史记录 | 🚧 开发中 |
| 设置 | `/settings` | API 配置、主题切换 | 🚧 开发中 |

---

## 🔧 API 方法

### Device Control

```typescript
// 截图
const result = await gateway.request('device.screenshot', {
  width: 1080,
  height: 1920
});

// Tap
await gateway.request('device.tap', {
  x: 500,
  y: 800
});

// Swipe
await gateway.request('device.swipe', {
  startX: 500,
  startY: 1000,
  endX: 500,
  endY: 500,
  duration: 300
});

// Type
await gateway.request('device.type', {
  text: 'Hello AndroidForClaw'
});

// Open App
await gateway.request('device.open_app', {
  packageName: 'com.android.chrome'
});
```

### Chat

```typescript
// Send message
const result = await gateway.request('chat.send', {
  sessionId: 'session-123',
  message: '使用browser搜索openclaw'
});

// Listen to chat messages
gateway.on('chat.message', (message) => {
  console.log('New message:', message);
});
```

### Sessions

```typescript
// List sessions
const sessions = await gateway.request('sessions.list');

// Reset session
await gateway.request('sessions.reset', {
  sessionId: 'session-123'
});
```

### Status

```typescript
// Get device status
const status = await gateway.request('device.status');

// Get channel status
const channel = await gateway.request('channel.status');
```

---

## 📦 部署方式

### 方案 A: 开发模式 (独立 Vite 服务器)

```bash
# 启动 Vite dev server
cd ui
pnpm dev

# 在另一个终端启动 Gateway
cd ../gateway
node dist/index.js

# 访问
http://localhost:5173
```

**优势**: 热更新、开发体验好
**劣势**: 需要两个进程

### 方案 B: 生产模式 (内嵌到 Gateway)

```bash
# 构建 UI
cd ui
pnpm build
# 输出到: ../app/src/main/assets/webui/

# Gateway 提供静态文件
# 访问: http://192.168.1.100:8080
```

**优势**: 单一部署、无跨域问题
**劣势**: 每次 UI 修改需要重新构建

### 方案 C: Docker 容器化 (推荐生产)

```dockerfile
# Dockerfile
FROM node:20-alpine

WORKDIR /app

# Build UI
COPY ui/package.json ui/pnpm-lock.yaml ./ui/
RUN cd ui && pnpm install
COPY ui ./ui
RUN cd ui && pnpm build

# Build Gateway
COPY gateway/package.json gateway/package-lock.json ./gateway/
RUN cd gateway && npm install
COPY gateway ./gateway
RUN cd gateway && npm run build

EXPOSE 8080

CMD ["node", "gateway/dist/index.js"]
```

---

## 🧪 开发指南

### 添加新视图

1. 创建视图组件: `src/ui/views/my-view.ts`
```typescript
import { LitElement, html } from 'lit';
import { customElement } from 'lit/decorators.js';

@customElement('my-view')
export class MyView extends LitElement {
  render() {
    return html`<div>My View Content</div>`;
  }
}
```

2. 在 `app.ts` 中注册路由
```typescript
case 'my-view':
  return html`<my-view></my-view>`;
```

### 添加新 API 方法

在 Gateway 中实现相应的 RPC 方法:
```typescript
// gateway/src/handlers.ts
handlers.set('my.method', async (params) => {
  // 处理逻辑
  return { result: 'ok' };
});
```

在 UI 中调用:
```typescript
const result = await this.gateway.request('my.method', { ... });
```

### 样式开发

使用 CSS 变量和工具类:
```html
<div class="card p-4 flex gap-3">
  <button class="btn btn-primary">Click Me</button>
</div>
```

---

## 🔍 调试技巧

### WebSocket 调试

打开浏览器开发者工具 → Network → WS → 选择连接 → 查看消息

### 日志输出

```typescript
// 在 gateway.ts 中添加日志
console.log('Request:', method, params);
console.log('Response:', response);
```

### 热更新

Vite 支持热更新，修改代码后自动刷新浏览器。

---

## 📚 参考资料

### OpenClaw UI

- **源码**: 基于 OpenClaw UI 架构
- **架构**: Lit + TypeScript + Vite
- **文档**: [OpenClaw GitHub](https://github.com/openclaw/openclaw)

### Lit 文档

- [Lit 官网](https://lit.dev)
- [Lit Playground](https://lit.dev/playground)
- [Web Components](https://developer.mozilla.org/en-US/docs/Web/Web_Components)

### Vite 文档

- [Vite 官网](https://vitejs.dev)
- [Vite Config](https://vitejs.dev/config/)

---

## 🚧 开发进度

- [x] 基础架构搭建
- [x] WebSocket 通信
- [x] 主应用组件
- [x] 导航和路由
- [x] 主题系统
- [ ] 聊天视图 (Chat)
- [ ] 设备控制视图 (Device Control)
- [ ] 截图视图 (Screenshot)
- [ ] 会话管理视图 (Sessions)
- [ ] 设置视图 (Settings)
- [ ] 国际化 (i18n)
- [ ] 移动端适配
- [ ] 单元测试

---

**AndroidForClaw Web Control UI v3.0**
**Tech Stack**: Lit + TypeScript + Vite
**Architecture**: OpenClaw-aligned
