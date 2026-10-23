# VMTools → Minecraft 26.3 全量移植审计

> 本文件记录 26.3 移植的基线、API 差异与待补齐的功能缺口。
> 分支：`mc-26.3`（基于 `mc-26.2`）

## 1. 基线判定

| 分支 | MC | mod_version | 对应 main 提交 |
|------|----|-------------|----------------|
| `main` | 1.21.11 | 3.2.0（README 标 v4.0.1） | `fddabbb` |
| `mc-26.1` | 26.1 | 3.2.0 | `6ee280b` |
| `mc-26.2` | 26.2 | 3.2.0 | `1ed51ea` |
| `mc-26.3` | 26.3 | 3.2.0 | 本次新建 |

**关键结论**：`mc-26.1` / `mc-26.2` 均停留在 `4e187a7`（v3.2.0）的代码状态，
**没有合入 `main` 上的 `17bcd06`（v4.0.1）**。因此 26.x 分支相对 `main` 存在
**功能缺口**，不只是 API 映射差异。26.3 若要求「全量移植」，必须同时补齐这些缺口。

## 2. 26.3 相对 26.2 的破坏性 API 变更

### 2.1 输入系统：GLFW → SDL

26.3 移除了 LWJGL GLFW，改用 SDL。这是 26.3 相对 26.2 **唯一导致编译失败的变更**。

| 项目 | 26.2 | 26.3 |
|------|------|------|
| 键类型枚举 | `InputConstants.Type.KEYSYM` | `InputConstants.Type.KEYBOARD` |
| 键值常量 | `org.lwjgl.glfw.GLFW.GLFW_KEY_M` | `InputConstants.KEY_M` |
| 键事件类 | `net.minecraft.client.input.KeyInput` | `net.minecraft.client.input.KeyEvent` |
| 依赖 | `org.lwjgl:lwjgl-glfw` | `org.lwjgl:lwjgl-sdl` |

`InputConstants.KEY_M = 16`（SDL keycode），`KEY_ESCAPE = 41`，`KEY_UP = 82`，
`KEY_DOWN = 81`，`KEY_TAB = 43`，`KEY_RETURN = 40`。**数值与 GLFW 完全不同**，
所以 26.2 里硬编码的 265/264/258/257/256 必须替换。

### 2.2 其余 API 保持 26.2 形态（已验证）

- `Screen.extractRenderState(GuiGraphicsExtractor,int,int,float)` — 不变
- `Screen.isPauseScreen()` / `onClose()` — 不变
- `Minecraft.gui.setScreen(...)` / `client.gui.screen()` — 不变
- `Button.builder(...).bounds(...)` — 不变
- `EditBox` (`setValue`/`getValue`/`setResponder`/`setHint`) — 不变
- `LocalPlayer.sendPosition()` — 存在，Mixin 目标有效
- `KeyMapping` / `KeyMapping.Category.register` / `KeyMappingHelper` — 不变

## 3. 功能缺口清单（26.2 → 需在 26.3 补齐）

来自 `main` 的 `17bcd06`（v4.0.1）：

| # | 文件 | 缺口 | 来源 |
|---|------|------|------|
| 1 | `WaypointScreen` | 顶部快捷按钮 末影箱/发光/Back；底部 Back 改为「逃逸」 | 17bcd06 |
| 2 | `WaypointScreen` | 移除顶部 Tab 栏与标题渲染；移除 Tab 点击检测 | 17bcd06 |
| 3 | `WaypointScreen` | `sendBackCommand()` → `sendQuickCommand(cmd, displayName)` | 17bcd06 |
| 4 | `TeleportService` | 删除 `/res tp` 前 `edithome resback relocate` 与 `isResTp` | 17bcd06 |

来自 `main` 更早期（26.x 分支从未同步）：

| # | 文件 | 缺口 | 说明 |
|---|------|------|------|
| 5 | `EditWaypointScreen` | **命令补全系统**（`COMMAND_SUGGESTIONS`/`updateSuggestion`/`acceptSuggestion`/`keyPressed`/补全面板渲染） | main 自 v1.15.6 起就有 |
| 6 | `EditWaypointScreen` | 自动确认改为三态（跟随全局/开/关），`getAutoConfirmLabel`/`getAutoConfirmColor` | main 版本 |
| 7 | `EditWaypointScreen` | `PANEL_HEIGHT` 260（26.2 为 230） | main 版本 |

## 4. 移植完成状态

| 缺口 | 状态 |
|------|------|
| 1. 顶部快捷按钮（末影箱/发光/Back） | ✅ 已完成 |
| 2. 底部 Back → 逃逸，移除 Tab 栏与标题 | ✅ 已完成 |
| 3. `sendBackCommand` → `sendQuickCommand` | ✅ 已完成 |
| 4. TeleportService 移除 `edithome resback relocate` / `isResTp` | ✅ 已完成 |
| 5. EditWaypointScreen 命令补全系统 | ✅ 已完成（含键盘导航） |
| 6. EditWaypointScreen 三态自动确认 | ✅ 已完成 |
| 7. PANEL_HEIGHT 260 | ✅ 已确认 |

### 关键实现说明

- **命令补全键盘导航**使用 26.3 的 SDL 键码常量（`InputConstants.KEY_UP/DOWN/TAB/RETURN/ESCAPE`），
  而非 main 里硬编码的 GLFW 数值。已验证 `KeyEvent.key()` 与 `InputConstants$Key.getValue()`
  比较（见 `KeyMapping.matches(KeyEvent)` 字节码），语义正确。
- **三态自动确认**映射到既有 `Waypoint.autoConfirm`（`Boolean`：null=跟随全局 / true / false），
  与 `shouldAutoConfirm(globalSetting)` 语义一致，避免"跟随全局"被错误写成 false。
- 补全面板的**渲染坐标与点击命中坐标统一**由 `suggestionPanelX/Y/W` 提供
  （main 原版渲染用 panelY+84/宽动态、命中用 panelY+52/宽 200，本身不一致）。

### 仍与 main 存在的非功能性差异（源自 mc-26.2 基线，非本次移植范围）

| 项 | main | mc-26.3（沿用 26.2 基线） |
|----|------|--------------------------|
| 底部提示文案 | `左键展开  |  右键菜单  |  ESC 关闭` | `左键展开/传送  |  右键菜单  |  ESC 关闭` |
| 窗口宽度计算 | `DRAG_HANDLE_WIDTH + 60` | `DRAG_HANDLE_WIDTH + PADDING * 2` |
| 路径点行悬停宽度 | 预留 `teleportBtnWidth` | 无预留（该变量恒为 0，效果等价） |
| 右键菜单颜色数组 | 4 项 | 3 项（第 4 项在 main 中亦未被使用） |

> 这些差异在 `4e187a7`（v3.2.0）时就已存在，与 26.3 API 无关，不影响功能。

## 5. 编译验证结论

- `compileClientJava`：修复 `KeybindManager` 输入 API 后 **BUILD SUCCESSFUL**
- `build`（含 remap）：**BUILD SUCCESSFUL**，产出 `vmtools-mc26.3-3.2.0.jar`（118 KB，41 classes）
- 与 26.1/26.2 产物规模一致（117–124 KB，41 classes）

## 6. 构建工具链（本容器）

| 项 | 值 |
|----|-----|
| JDK | 25.0.4.1（`/opt/jdk25/current`，`vm-jdk25`） |
| Gradle | 9.7.1（wrapper，`/tmp/gradle-home` 预置） |
| Loom | 1.18.4 |
| Loader | 0.20.1 |
| Fabric API | 0.165.0+26.3 |
| Mappings | Mojang 非混淆（无需 yarn） |

> 本容器 `/root/.gradle` 只读，需 `export GRADLE_USER_HOME=/tmp/gradle-home`。
