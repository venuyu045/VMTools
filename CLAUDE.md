# CLAUDE.md — VMTools 项目指南

> 本文件为 AI 助手提供项目上下文、开发约定和工作流指引。
> **每次涉及代码修改的任务，必须调用软件开发团队（Agent）来完成。**

## 项目概览

**VMTools (Venus Mc Tools)** 是一个 Minecraft Fabric 客户端模组，核心功能是路径点管理和快速传送。

- **仓库**: https://github.com/venuyu045/VMTools
- **作者**: Venus (venuyu045)
- **许可证**: MIT
- **环境**: 纯客户端模组（`environment: "client"`）

## 版本矩阵

项目维护两个 Minecraft 版本：

| 版本 | 目录 | MC 版本 | Java | Fabric Loader | Loom | Mappings |
|------|------|---------|------|---------------|------|----------|
| 1.x  | `vmtools/` 分支 `mc-26.1` 之前的 main | 1.21.11 | 21 | 0.16.14 | 1.16.2 | Yarn |
| 2.x  | `vmtools/` 分支 `mc-26.1` | 26.1 | 25 | 0.19.2 | 1.16-SNAPSHOT | Mojang (非混淆) |

**⚠️ 版本敏感**：用户多次强调不要擅自更改 Minecraft 版本号。修改前必须确认。

## 构建命令

```bash
# 1.21.11 版本
cd vmtools
./gradlew.bat build

# 26.1 版本（需要 Java 25）
cd vmtools
JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-25.0.3.9-hotspot" ./gradlew.bat build
```

输出：`build/libs/vmtools-{version}.jar`

## 项目结构

```
vmtools/
├── build.gradle                  # 构建配置（Loom + Fabric API）
├── gradle.properties             # 版本号、依赖版本
├── settings.gradle               # 插件仓库配置
├── src/
│   ├── client/java/com/venus/vmtools/   # 客户端代码（split source set）
│   │   ├── VMToolsClient.java           # 模组入口点
│   │   ├── config/
│   │   │   └── ModConfig.java           # UI 常量 + 配置项
│   │   ├── feature/waypoint/
│   │   │   ├── Waypoint.java            # 路径点数据模型
│   │   │   ├── WaypointGroup.java       # 分组数据模型
│   │   │   ├── WaypointColor.java       # 颜色枚举（9 色）
│   │   │   ├── WaypointManager.java     # CRUD + 持久化（Gson）
│   │   │   ├── WaypointIO.java          # JSON 导入导出
│   │   │   └── TeleportService.java     # 传送命令发送
│   │   ├── gui/
│   │   │   ├── WaypointScreen.java      # 主界面（路径点列表）
│   │   │   ├── EditWaypointScreen.java  # 编辑路径点
│   │   │   ├── EditGroupScreen.java     # 编辑分组
│   │   │   ├── ConfirmScreen.java       # 通用确认对话框
│   │   │   ├── ImportConfirmScreen.java # 导入预览确认
│   │   │   └── component/
│   │   │       ├── ToastWidget.java     # Toast 通知
│   │   │       ├── SearchField.java     # 搜索框组件
│   │   │       └── ColorPicker.java     # 颜色选择器组件
│   │   ├── keybind/
│   │   │   └── KeybindManager.java      # 快捷键注册（M 键）
│   │   └── util/
│   │       ├── ChatUtil.java            # 聊天/命令工具
│   │       └── FileUtil.java            # 文件操作工具
│   └── main/resources/
│       ├── fabric.mod.json              # 模组元数据
│       └── assets/vmtools/lang/
│           ├── zh_cn.json               # 中文翻译
│           └── en_us.json               # 英文翻译
```

## 核心架构

### 数据流

```
用户操作 → GUI Screen → WaypointManager → WaypointIO (持久化)
                ↓
         TeleportService → ClientPacketListener.sendCommand() → 服务器
```

### 传送机制

本模组通过向服务器发送聊天命令实现传送（原版服务器无法直接传送）：
- 命令格式：`/res tp xxx`、`/home`、`/warp xxx` 等
- 调用 `ClientPacketListener.sendCommand(command)` 发送
- 不显示在聊天栏

### 数据持久化

- 存储路径：`.minecraft/config/vmtools/waypoints.json`
- 自动备份：`waypoints.backup.json`
- Gson 序列化，自定义 `LocalDateTime` TypeAdapter
- 数据版本字段 `dataVersion`，支持向后兼容

### UI 设计

- 暗色主题（Catppuccin 风格）
- 颜色常量集中在 `ModConfig.UI` 和各 Screen 类顶部
- 自定义绘制圆角矩形、颜色选择器、Toast 通知
- 右键菜单系统（分组/路径点各有独立菜单）

## API 映射速查（26.1 Mojang 名称）

| 功能 | 1.21.11 (Yarn) | 26.1 (Mojang) |
|------|----------------|---------------|
| 屏幕基类 | `Screen` | `net.minecraft.client.gui.screens.Screen` |
| 按钮 | `ButtonWidget` | `Button` |
| 输入框 | `TextFieldWidget` | `EditBox` |
| 文本 | `Text.of()` / `Text.literal()` | `Component.literal()` |
| 渲染上下文 | `DrawContext` | `GuiGraphicsExtractor` |
| 渲染方法 | `render()` | `extractRenderState()` |
| 鼠标点击 | `mouseClicked(Click, boolean)` | `mouseClicked(MouseButtonEvent, boolean)` |
| 绘制文字 | `drawString()` | `text()` |
| 快捷键 | `KeyBinding` + `KeyBindingHelper` | `KeyMapping` + `KeyMappingHelper` |
| 标识符 | `Identifier.of()` | `Identifier.fromNamespaceAndPath()` |
| 发送消息 | `displayClientMessage()` | `sendSystemMessage()` / `sendOverlayMessage()` |
| 窗口句柄 | `getWindow().getWindow()` | `getWindow().handle()` |
| 文本渲染器 | `this.textRenderer` | `this.font` |
| 文本宽度 | `textRenderer.getWidth()` | `font.width()` |
| 占位符 | `setPlaceholder()` | `setHint()` |
| 文本变更 | `setChangedListener()` | `setResponder()` |
| 获取文本 | `getText()` | `getValue()` |
| 设置文本 | `setText()` | `setValue()` |
| 当前屏幕 | `client.currentScreen` | `client.screen` |
| 网络处理器 | `client.getNetworkHandler()` | `client.getConnection()` |
| 发送命令 | `sendChatCommand()` | `sendCommand()` |
| 发送聊天 | `sendChatMessage()` | `sendChat()` |
| 暂停判断 | `shouldPause()` | `isPauseScreen()` |
| 关闭屏幕 | `close()` | `onClose()` |

## 开发约定

### 代码风格

- 中文注释和 Javadoc
- 每个类顶部有简短的中文功能说明
- 颜色常量用 `0xFFRRGGBB` 格式定义在类顶部
- GUI 布局用常量控制（`PANEL_WIDTH`、`PADDING` 等）

### 版本管理

- `gradle.properties` 中的 `mod_version` 遵循语义化版本
- Bug 修复：小版本号 +1（如 1.5.0 → 1.5.1）
- 新功能：次版本号 +1（如 1.5.0 → 1.6.0）
- 重大重构：主版本号 +1（如 1.x → 2.x）

### Git 约定

- `main` 分支：1.21.11 稳定版
- `mc-26.1` 分支：26.1 移植版
- 提交信息格式：`feat:` / `fix:` / `refactor:` 前缀

## 工作流要求

### 🔴 强制规则 1：使用 Agent 完成任务

**所有涉及代码修改的任务，必须通过软件开发团队（Agent）完成：**

| 任务类型 | 使用的工作流 | 涉及成员 |
|---------|------------|---------|
| 新功能开发 | 标准 SOP | 产品经理 → 架构师 → 工程师 → QA |
| Bug 修复 | BugFix 快捷路径 | 工程师 → QA |
| 小改动（单文件） | 快速模式 | 工程师 → QA |
| 仅需求分析 | 部分工作流 | 产品经理 |
| 仅架构设计 | 部分工作流 | 架构师 |

### 🔴 强制规则 2：双版本同步更新

**每次修改项目代码时，必须同时更新 `vmtools/`（1.21.11）和 `vmtools-26.1/`（26.1）两个版本，并确保两个版本都能编译通过。**

流程：
1. 先在 `vmtools/`（1.21.11 Yarn 版本）中完成功能修改
2. 将相同的改动移植到 `vmtools-26.1/`（26.1 Mojang 版本），注意 API 映射差异
3. 分别编译两个版本，确保都 `BUILD SUCCESSFUL`
4. 两个版本的 `mod_version` 必须保持一致

**禁止只更新一个版本而遗漏另一个。**

### 何时可以不用 Agent

- 读取文件、查看代码结构
- 回答关于项目的问题
- 构建/编译操作
- Git 操作
- 纯配置修改（gradle.properties 版本号等）

### 编译验证流程

每次代码修改后必须**对两个版本都执行**：
1. 编译 1.21.11：`cd vmtools && ./gradlew.bat build`
2. 编译 26.1：`cd vmtools-26.1 && JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-25.0.3.9-hotspot" ./gradlew.bat build`
3. 确认两个版本都 `BUILD SUCCESSFUL`
4. 检查两个版本的 `build/libs/` 下的 JAR 文件
5. 如有错误，修复后重新编译

## 已知问题和注意事项

1. **26.1 的 Fabric API 使用 `implementation`**（不是 `modImplementation`），因为 Loom 1.16-SNAPSHOT 的 plugin ID 是 `net.fabricmc.fabric-loom`
2. **26.1 没有 Yarn 映射**，所有类名必须用 Mojang 官方名称
3. **Gson 序列化 LocalDateTime** 需要自定义 TypeAdapter（Java 21+ 模块系统限制反射）
4. **GUI 不要调用两次 `renderBackground()`**，会导致 "Can only blur once per frame" 崩溃
5. **快捷键翻译键**需要 `key.` 前缀（如 `key.category.vmtools.main`）
6. **数据文件兼容性**：更新 mod 版本时，配置文件必须保留（WaypointManager 有备份恢复机制）

## GitHub Release 流程

```bash
# 1. 确保代码已推送
git add -A && git commit -m "feat: ..." && git push

# 2. 创建 Release（通过 GitHub API 或网页）
# 3. 上传 JAR 到 Release Assets
```

用户 GitHub: `venuyu045`
仓库地址: `https://github.com/venuyu045/VMTools`
