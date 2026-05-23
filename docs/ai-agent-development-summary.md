# VMTools — AI Agent 驱动开发成果描述

## 项目概述

VMTools 是一个 Minecraft Fabric 客户端模组，核心功能是路径点管理与快速传送。整个项目从零到发布，**全程由 AI Agent（MiMo）驱动开发**，涵盖需求分析、架构设计、代码实现、多版本适配、Bug 修复、文档编写、GitHub 发布的完整软件开发生命周期。

## AI Agent 驱动的具体成果

### 1. 全栈代码生成（18+ Java 源文件）
AI Agent 一次性生成了完整的模组代码，包括：
- **GUI 系统**：WaypointScreen、EditWaypointScreen、EditGroupScreen、ConfirmScreen、ImportConfirmScreen — 共 5 个独立界面类
- **数据模型**：Waypoint、WaypointGroup、WaypointColor、UIState — 完整的数据层
- **业务逻辑**：WaypointManager（CRUD+持久化）、WaypointIO（导入导出）、TeleportService（传送服务）
- **组件库**：ToastWidget（通知组件）、SearchField（搜索框）、ColorPicker（颜色选择器）
- **系统集成**：KeybindManager（快捷键绑定）、ChatUtil（聊天工具）、ModConfig（配置管理）

### 2. 复杂 UI 交互实现
AI Agent 实现了多个非平凡的交互特性：
- **窗口拖拽系统**：左键长按拖动分组窗口到屏幕任意位置，含阈值判断区分点击与拖拽（5px DRAG_THRESHOLD）
- **路径点拖拽排序**：拖拽手柄 + 实时位置指示线 + 可见索引到实际索引的转换（考虑搜索过滤）
- **动态 z-order**：点击分组自动置顶，渲染顺序持久化
- **动态窗口宽度**：根据分组名和路径点名长度自适应（180-350px 范围）
- **滚动条系统**：路径点超出高度时自动显示，支持鼠标滚轮

### 3. 双版本并行开发
AI Agent 同时维护两个 Minecraft 版本的代码：
- **1.21.11（Yarn mappings）**：`main` 分支，Java 21+
- **26.1（Mojang non-obfuscated）**：`mc-26.1` 分支，Java 25+

两个版本共享业务逻辑，仅 API 层不同。AI Agent 自动处理了 50+ 处 API 映射差异（如 `DrawContext → GuiGraphicsExtractor`、`Click → MouseButtonEvent`、`Text → Component`、`render → extractRenderState` 等）。

### 4. 迭代式 Bug 修复（8 个版本迭代）
AI Agent 通过用户反馈持续迭代，从 v1.6.0 到 v1.10.4：
- v1.8.0：基础路径点管理功能
- v1.9.0：UI 重构（可拖动窗口+动态宽度+滚动条）
- v1.9.1：拖动联动 Bug 修复
- v1.9.2：文件选择器 null 错误修复 + 备选方案
- v1.10.0：路径点拖拽排序 + 分组动态置顶
- v1.10.1：排序方向 Bug 修复（可见索引→实际索引转换）
- v1.10.2：Toast 通知位置修复
- v1.10.3：Toast 多行支持
- v1.10.4：UI 状态限频保存（修复每帧写磁盘导致的游戏启动卡死）

### 5. 工程化实践
- **配置持久化**：JSON 格式保存路径点数据 + UI 状态，支持自动备份和恢复
- **版本管理**：Git 双分支策略，语义化版本号
- **CI/CD**：自动构建两个版本的 JAR，通过 GitHub API 创建 Release 并上传产物
- **文档**：完整的 README（功能说明、安装指南、命令示例、技术信息）

## 技术栈
- Minecraft 1.21.11 / 26.1
- Fabric Loader + Fabric API
- Java 21 / 25
- Gson（JSON 序列化）
- GLFW（输入处理）
- LWJGL（窗口管理）

## 成果数据
- **源文件数**：18 个 Java 文件
- **代码行数**：约 3000 行
- **版本迭代**：8 个版本（v1.6.0 → v1.10.4）
- **双版本支持**：1.21.11 + 26.1
- **GitHub Release**：https://github.com/venuyu045/VMTools/releases/tag/v1.10.4
