# 安全维度审查报告

## 审查范围

**分支（5 个）**：`main`(MC 1.21.11, Yarn) / `mc-1.21.6`(1.21.6, Yarn) / `mc-26.1` / `mc-26.2` / `mc-26.3`(Mojang mappings)。

**审查文件**（源码根 `src/client/java/com/venus/vmtools/`，资源 `src/main/resources/`）：
`feature/waypoint/{TeleportService,WaypointIO,WaypointManager,Waypoint,WaypointGroup,UIState}.java`、
`feature/escape/{AutoEscapeManager,AutoEscapeConfig,HealthMonitorManager,HealthMonitorConfig}.java`、
`feature/freeze/FreezeManager.java`、`util/{ChatUtil,FileUtil}.java`、`config/ModConfig.java`、
`mixin/LocalPlayerMixin.java`、`keybind/KeybindManager.java`、
`gui/{WaypointScreen,EditWaypointScreen,EditGroupScreen,ImportConfirmScreen,SettingsScreen,AutoEscapeScreen}.java`、
`vmtools.mixins.json`、`fabric.mod.json`、`build.gradle`、`gradle.properties`。

**方法**：逐分支 `read`/`grep -n` 交叉比对 + `git show <branch>:<path>` 差异确认；`sendCommand` 行为通过反编译 MC 26.3 `ClientPacketListener` / `ServerboundChatCommandPacket` / `FriendlyByteBuf` 字节码核实。仅读，未改动任何源码。

> **审查基线**：本报告所有行号均对应各分支的**提交态**（`mc-26.3` = `4a117a8`，`main` = `fddabbb`，`mc-1.21.6` = `c6a7f81`，`mc-26.1` = `37d5e51`，`mc-26.2` = `3285822`）。审查期间有其他协作者并行向工作区提交修复补丁，故工作区当前文件行号可能已偏移，引用时请以对应 commit 的 `git show <branch>:<path>` 为准。

## 问题汇总

| 编号 | 级别 | 标题 | 文件 | 影响分支 |
|------|------|------|------|----------|
| SEC-01 | 严重 | 传送命令无白名单/无非法字符过滤，用户可控字符串直发服务器 | `TeleportService.java:82-100` | 全部 5 个 |
| SEC-02 | 严重 | 导入 JSON 无大小/数量上限，可致 OOM 与界面卡死 | `WaypointIO.java:85,122-144` | 全部 5 个 |
| SEC-03 | 严重 | 主数据文件损坏时静默重建默认分组，且备份被覆盖 → 数据丢失 | `WaypointManager.java:58-84,126-150` | 全部 5 个 |
| SEC-04 | 严重 | Gson 反序列化后字段可为 null，`waypoints`/`id` 空指针 | `WaypointGroup.java:16,70` | 全部 5 个 |
| SEC-05 | 严重 | 26.1/26.2 在每次 `/res tp` 前静默发送未公开命令 `edithome resback relocate` | `TeleportService.java:40-50` | mc-26.1 / mc-26.2 |
| SEC-06 | 严重 | 配置文件为 `null`/空时 `load()` 返回 null，后续 `getConfig()` NPE | `ModConfig.java:84`、`AutoEscapeConfig.java:50`、`HealthMonitorConfig.java:50` | 全部 5 个 |
| SEC-07 | 中等 | 命令长度无显式上限，磁盘/导入路径完全不受限 | `EditWaypointScreen.java:124-134` | 全部 5 个 |
| SEC-08 | 中等 | 导入预览不显示命令内容，用户无法察觉恶意命令 | `ImportConfirmScreen.java:115-121` | 全部 5 个 |
| SEC-09 | 中等 | 日志泄露完整命令与文件绝对路径 | `TeleportService.java:46` 等 | 全部 5 个 |
| SEC-10 | 中等 | `confirmBeforeTeleport` 配置项存在但从未生效（虚假安全感） | `ModConfig.java:51,108` | 全部 5 个 |
| SEC-11 | 中等 | 自动确认命令可被设为任意命令，且每次传送后自动执行 | `SettingsScreen.java:56`、`WaypointScreen.java:1118-1135` | 全部 5 个 |
| SEC-12 | 中等 | AWT `FileDialog`/`Desktop` 在 headless 下抛异常，并全局改写 `java.awt.headless` | `WaypointScreen.java:1235,1237,1327` | 全部 5 个 |
| SEC-13 | 中等 | `fabric.mod.json` 声明 `icon.png`，5 个分支均不存在该文件 | `fabric.mod.json:14` | 全部 5 个 |
| SEC-14 | 中等 | `minecraft` 版本约束与实际 MC 版本不符（main / mc-1.21.6） | `fabric.mod.json:26` | main / mc-1.21.6 |
| SEC-15 | 中等 | `fabric-api: "*"` 无版本约束；loader 下限与 gradle.properties 不一致 | `fabric.mod.json:25,28` | 全部 5 个 |
| SEC-16 | 中等 | Mixin 静态块 `System.out.println` 调试输出残留 | `LocalPlayerMixin.java:27-29` | mc-26.1 / 26.2 / 26.3 / main |
| SEC-17 | 中等 | `FreezeManager` 状态字段非 `volatile`，跨线程读写 | `FreezeManager.java:20,23,26` | main / 26.1 / 26.2 / 26.3 |
| SEC-18 | 中等 | 冻结 `sendPosition()` 连带取消 `sendIsSprintingIfNeeded`，且恢复后发送位移突变 | `LocalPlayerMixin.java:31-36` | main / 26.1 / 26.2 / 26.3 |
| SEC-19 | 轻微 | `exportToFile` 对 `getParent()==null` 无保护（NPE） | `WaypointIO.java:66` | 全部 5 个 |
| SEC-20 | 轻微 | 单例懒加载非线程安全、`instance` 非 `volatile` | `FreezeManager.java:36-41` 等 | 全部 5 个 |
| SEC-21 | 轻微 | `UIState` 从磁盘读取的 x/y/width/height 未做范围校验 | `WaypointScreen.java:281-289` | 全部 5 个 |
| SEC-22 | 轻微 | `dataVersion` 只写不读，无版本迁移与兼容校验 | `WaypointManager.java:31,156` | 全部 5 个 |
| SEC-23 | 轻微 | `contact.sources` URL 无效（404），实际仓库为 `venuyu045/VMTools` | `fabric.mod.json:11` | 全部 5 个 |
| SEC-24 | 轻微 | 声明 MIT 但 5 个分支均无 LICENSE 文件，`jar` 任务引用亦缺失 | `build.gradle:51` | 全部 5 个 |
| SEC-25 | 轻微 | `confirmDelay` 无上下限校验；`ChatUtil`/`FileUtil` 为死代码 | `AutoEscapeConfig.java:118` | 全部 5 个 |

**统计**：共 25 条 —— 严重 6、中等 12、轻微 7。

## 详细问题

### SEC-01 [严重] 传送命令无白名单、无非法字符过滤
- **文件**：`src/client/java/com/venus/vmtools/feature/waypoint/TeleportService.java:82`
- **影响分支**：全部 5 个（`main:75`、`mc-1.21.6:62`、`mc-26.1/26.2/26.3:82`）
- **问题描述**：`normalizeCommand()` 只做 `trim()` 和去掉首个 `/`，没有命令前缀白名单、没有非法字符/控制字符过滤、没有长度校验。任何字符串都会被 `sendCommand()` 原样交给 `ClientPacketListener.sendCommand`。`isValidCommand()` 直接复用该函数，因此 UI 层"验证通过"实际等于"非空即合法"。
- **影响**：用户在路径点里填什么就发什么，等于把客户端变成任意命令发射器。虽然最终权限受服务端限制，但配合 SEC-02/SEC-08（导入文件静默带入命令）可让受害者在不知情时执行任意命令。`trim()` 只处理首尾空白，字符串内部的 `\n` / `\r` / `§` 会原样进入 `ServerboundChatCommandPacket`（该包用 `FriendlyByteBuf.writeUtf` 写裸字符串，上限 32767，不做内容校验）。
- **证据**：
  ```
  82:    private static String normalizeCommand(String command) {
  83:        if (command == null || command.trim().isEmpty()) {
  84:            return null;
  85:        }
  86:
  87:        command = command.trim();
  88:
  89:        // 去掉开头的 /
  90:        if (command.startsWith("/")) {
  91:            command = command.substring(1);
  92:        }
  ```
  ```
  108:    public static boolean isValidCommand(String command) {
  109:        return normalizeCommand(command) != null;
  ```
  ```
  45:                client.getConnection().sendCommand(finalCommand);
  ```
- **修复建议**：白名单化并拒绝控制字符，命令长度设上限：
  ```java
  private static final java.util.regex.Pattern CMD = java.util.regex.Pattern.compile("^[a-z0-9_]+( [\\w.@:/\\-\\[\\]{}'\"+~]*)?$");
  private static final int MAX_CMD_LEN = 256;
  private static String normalizeCommand(String command) {
      if (command == null) return null;
      String c = command.trim();
      if (c.startsWith("/")) c = c.substring(1);
      if (c.isEmpty() || c.length() > MAX_CMD_LEN) return null;
      for (int i = 0; i < c.length(); i++) {          // 拒绝控制字符（含 \n \r \0 §）
          char ch = c.charAt(i);
          if (ch < 0x20 || ch == 0x7F || ch == 0xA7) return null;
      }
      if (!CMD.matcher(c.toLowerCase()).matches()) return null;   // 前缀白名单
      return c;
  }
  ```

### SEC-02 [严重] 导入 JSON 无大小与数量上限
- **文件**：`src/client/java/com/venus/vmtools/feature/waypoint/WaypointIO.java:85`
- **影响分支**：全部 5 个（`WaypointIO.java` 在 5 分支内容完全一致）
- **问题描述**：`importFromFile()` 用 `Files.readString` 一次性读入任意大小的文件，`GSON.fromJson` 后 `convertFromExportData()` 逐条构造对象，全程无文件大小上限、无分组数量上限、无单组路径点数量上限。`importWaypoints()` 还会把结果全部并入内存并立即 `save()` 落盘。
- **影响**：一个几百 MB 的 JSON 或含数十万路径点的文件即可造成 OOM / GC 抖动 / 界面长时间无响应；写入后 `waypoints.json` 膨胀，下次启动同样受影响（持久化拒绝服务）。
- **证据**：
  ```
  85:            String json = Files.readString(filePath);
  86:            ExportData data = GSON.fromJson(json, ExportData.class);
  87:            List<WaypointGroup> groups = convertFromExportData(data);
  ```
  ```
  127:        for (GroupData groupData : data.groups) {
  128:            WaypointGroup group = new WaypointGroup(groupData.name);
  ...
  133:                for (WaypointData wpData : groupData.waypoints) {
  134:                    Waypoint wp = new Waypoint(wpData.name, wpData.command, group.getId());
  ```
- **修复建议**：读取前先限长，并在转换时计数熔断：
  ```java
  private static final long MAX_IMPORT_BYTES = 4L * 1024 * 1024;
  private static final int MAX_GROUPS = 500, MAX_WAYPOINTS = 20_000;
  if (Files.size(filePath) > MAX_IMPORT_BYTES) { LOGGER.warn("导入文件过大，拒绝"); return null; }
  ...
  if (data.groups.size() > MAX_GROUPS) return null;
  // 循环内：if (++count > MAX_WAYPOINTS) throw new IllegalArgumentException("路径点数量超限");
  ```

### SEC-03 [严重] 数据文件损坏时静默重建，且备份被覆盖 → 数据丢失
- **文件**：`src/client/java/com/venus/vmtools/feature/waypoint/WaypointManager.java:58`
- **影响分支**：全部 5 个（`WaypointManager.java` 在 5 分支内容完全一致）
- **问题描述**：`loadFromFile()` 内层两个 `catch (Exception ignored)` 把 JSON 语法错误、类型错误、`LocalDateTime.parse` 异常全部吞掉，返回 `false`。`load()` 随后依次尝试主文件、备份，两者都失败就 `createDefaultGroups()` + `save()`。而 `save()` 在写新文件**之前**会把当前 `DATA_PATH`（即那份损坏文件）复制到 `BACKUP_PATH`，覆盖掉原本可能完好的备份。
- **影响**：用户数据被静默清空，且唯一可恢复的备份也被损坏文件覆盖，属不可逆数据丢失。用户只会看到"未找到数据文件，创建默认分组"。
- **证据**：
  ```
   99:                DataWrapper wrapper = GSON.fromJson(json, DataWrapper.class);
  100:                if (wrapper != null && wrapper.groups != null) {
  101:                    this.groups = wrapper.groups;
  102:                    return true;
  103:                }
  104:            } catch (Exception ignored) {}
  ...
  109:                List<WaypointGroup> loaded = GSON.fromJson(json, listType);
  ...
  114:            } catch (Exception ignored) {}
  ```
  ```
  131:            if (Files.exists(DATA_PATH)) {
  132:                try {
  133:                    Files.copy(DATA_PATH, BACKUP_PATH, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
  ```
  ```
   81:        VMToolsClient.LOGGER.info("未找到数据文件，创建默认分组");
   82:        createDefaultGroups();
   83:        save();
  ```
- **修复建议**：区分"文件不存在"与"解析失败"；解析失败时**禁止**保存，并把损坏文件改名保留（`waypoints.corrupt.<ts>.json`），备份不得被损坏文件覆盖：
  ```java
  } catch (Exception e) {
      LOGGER.error("数据文件解析失败，已保留原文件，跳过本次保存", e);
      this.loadFailed = true;              // 全局标志，save() 首行 if (loadFailed) return;
      Files.move(DATA_PATH, DATA_PATH.resolveSibling("waypoints.corrupt." + ts + ".json"));
      return false;
  }
  ```

### SEC-04 [严重] Gson 反序列化后 `waypoints`/`id` 为 null 导致 NPE
- **文件**：`src/client/java/com/venus/vmtools/feature/waypoint/WaypointGroup.java:16`
- **影响分支**：全部 5 个（`WaypointGroup.java` 5 分支完全一致）
- **问题描述**：`waypoints` 只在构造器里初始化（`26: this.waypoints = new ArrayList<>()`），Gson 走 `Unsafe`/无参路径**不会**调用该构造器，JSON 里缺少 `waypoints` 字段时字段保持 `null`。同理 `Waypoint.id` / `WaypointGroup.id` 缺字段即为 `null`。`getWaypointCount()`、`getWaypoints()`、`getId().equals(...)` 均未判空。
- **影响**：手工编辑或第三方生成的 `waypoints.json` / 导入文件即可让主界面渲染直接 NPE 崩溃（每次打开界面必崩，且因 SEC-03 会连带触发数据重建）。`WaypointManager.removeGroup/findGroup`（`194`/`206`）的 `g.getId().equals(groupId)` 同样会 NPE。
- **证据**：
  ```
   16:    private List<Waypoint> waypoints;
  ...
   26:        this.waypoints = new ArrayList<>();
  ...
   69:    public int getWaypointCount() {
   70:        return waypoints.size();
  ```
  ```
  194:        boolean removed = groups.removeIf(g -> g.getId().equals(groupId));
  206:                .filter(g -> g.getId().equals(groupId))
  ```
- **修复建议**：字段声明处即初始化，并加 Gson 后置校验：
  ```java
  private List<Waypoint> waypoints = new ArrayList<>();     // 声明处初始化
  private String id = java.util.UUID.randomUUID().toString().substring(0, 8);
  // 加载后统一体检
  private static void sanitize(List<WaypointGroup> gs) {
      gs.removeIf(g -> g == null || g.getId() == null || g.getName() == null);
      for (WaypointGroup g : gs) {
          if (g.waypoints == null) g.setWaypoints(new ArrayList<>());
          g.getWaypoints().removeIf(w -> w == null || w.getId() == null || w.getCommand() == null);
      }
  }
  ```

### SEC-05 [严重] 26.1/26.2 静默发送未公开命令 `edithome resback relocate`
- **文件**：`src/client/java/com/venus/vmtools/feature/waypoint/TeleportService.java:49`（mc-26.1 / mc-26.2）
- **影响分支**：mc-26.1、mc-26.2（`main`、`mc-1.21.6`、`mc-26.3` 无此代码）
- **问题描述**：只要命令以 `res tp` 开头，客户端会在发送用户命令**之前**额外发一条 `edithome resback relocate`，把玩家当前位置写入服务器上的 `resback` home 点。该行为：① 不在任何 UI/文档中告知；② 不写日志（`LOGGER.info` 只打印用户命令）；③ 不受 `freezeEnabled` 等任何开关控制；④ 属于对服务端的有状态写操作。
- **影响**：用户在不知情下被修改了服务器端家园点数据；与"只发送我配置的命令"的预期严重不符。若服务端 home 数量有限制或 `resback` 已被占用，还会造成用户既有数据被覆盖。mc-26.3 已删除该逻辑，说明上游已认定为不当行为。
- **证据**：
  ```
   40:        // 对 /res tp 命令：传送前先保存当前坐标到 resback
   41:        final boolean saveBack = isResTp(normalizedCommand);
  ...
   47:                // /res tp 传送前：将当前位置保存为 resback home 点
   48:                if (saveBack) {
   49:                    client.getConnection().sendCommand("edithome resback relocate");
   50:                }
  ```
  ```
   89:    private static boolean isResTp(String normalizedCommand) {
   90:        String lower = normalizedCommand.toLowerCase();
   91:        return lower.startsWith("res tp ") || lower.equals("res tp");
   92:    }
  ```
- **修复建议**：回退到 mc-26.3 的实现（删除 `isResTp`/`saveBack` 整段）。若确需保留，必须：① 做成显式配置项且默认关闭；② 执行前后写日志；③ 在 UI 上明示。

### SEC-06 [严重] 配置反序列化为 null 后未兜底，后续 `getConfig()` NPE
- **文件**：`src/client/java/com/venus/vmtools/config/ModConfig.java:84`；`feature/escape/AutoEscapeConfig.java:50`；`feature/escape/HealthMonitorConfig.java:50`
- **影响分支**：全部 5 个
- **问题描述**：三处 `load()` 都写成 `XxxConfig c = GSON.fromJson(json, XxxConfig.class); return c;`。当文件内容为空、为字面量 `null`、或只含空白时，Gson 返回 `null` 并**不抛异常**，`catch (IOException)` 接不住，于是 `load()` 返回 `null` 并被保存到字段。`ModConfig` 无字段初始值，`VMToolsClient.config` 即为 `null`。
- **影响**：`VMToolsClient.getInstance().getConfig()` 返回 null，`TeleportService.shouldFreeze()`（`68`）、`WaypointScreen.executeTeleport()`（`1116`）、`SettingsScreen` 构造器（`28`）等全部 NPE。玩家改配置时误清空文件即可让模组完全不可用，且每次点击路径点都崩溃。
- **证据**：
  ```
   83:                String json = Files.readString(CONFIG_PATH);
   84:                ModConfig config = GSON.fromJson(json, ModConfig.class);
   85:                VMToolsClient.LOGGER.info("配置文件已加载");
   86:                return config;
   87:            } catch (IOException e) {
   88:                VMToolsClient.LOGGER.error("加载配置文件失败", e);
  ```
  （`AutoEscapeConfig.java:50-52`、`HealthMonitorConfig.java:50-52` 结构相同）
- **修复建议**：统一加 null 兜底与 JSON 异常捕获：
  ```java
  ModConfig config = GSON.fromJson(json, ModConfig.class);
  if (config == null) { LOGGER.warn("配置为空，使用默认值"); return new ModConfig(); }
  return config;
  } catch (IOException | com.google.gson.JsonSyntaxException e) { ... }
  ```

### SEC-07 [中等] 命令长度无显式上限
- **文件**：`src/client/java/com/venus/vmtools/gui/EditWaypointScreen.java:124`
- **影响分支**：全部 5 个
- **问题描述**：`commandField` 建好后从未调用 `setMaxLength(...)`（对比 `AutoEscapeScreen.java:207` 的 `field.setMaxLength(maxLen)`，逃逸命令框限 50 字符）。`EditBox` 构造器默认 `maxLength = 32`，因此键盘输入恰好被隐式截断，但：① 该限制纯属默认值巧合，代码中无任何声明，任何人后续改构造方式即失效；② 从磁盘 `waypoints.json` 与导入 JSON 读入的 `Waypoint.command` **完全绕过** `EditBox`，长度不受任何限制，会直接进入 `sendCommand`。
- **影响**：超长命令进入 `ServerboundChatCommandPacket`（`writeUtf` 上限 32767，超限抛异常被 `catch (Exception)` 吞掉后只弹"传送失败"），服务端也会因超长命令报解析错误；恶意导入文件可写入数万字符的命令使 UI 与网络层异常。
- **证据**：
  ```
  124:        commandField = new EditBox(this.font,
  125:                panelX + PADDING + 50, panelY + 65,
  126:                PANEL_WIDTH - PADDING * 2 - 50, 18,
  127:                Component.literal("传送命令"));
  128:        commandField.setHint(Component.literal("/res tp xxx").withStyle(s -> s.withColor(SUBTLE_COLOR)));
  129:        if (isEditMode) {
  130:            commandField.setValue(editingWaypoint.getCommand());
  131:        } else {
  132:            commandField.setValue("/"); // 新建时默认带 /
  133:        }
  134:        commandField.setResponder(this::updateSuggestion);
  ```
- **修复建议**：`commandField.setMaxLength(256);` 并在 SEC-01 的 `normalizeCommand()` 内做长度硬校验（磁盘/导入路径也必须过这一关）。

### SEC-08 [中等] 导入预览不显示命令内容
- **文件**：`src/client/java/com/venus/vmtools/gui/ImportConfirmScreen.java:115`
- **影响分支**：全部 5 个
- **问题描述**：导入确认界面只展示分组名、emoji、路径点数量，**从不展示每个路径点实际要执行的命令**。用户点击"追加导入/替换全部"即把外部文件中的任意命令写入本地库，之后左键单击路径点就会执行。
- **影响**：这是 SEC-01 的实际利用链——分享一个"路径点包"，受害者在预览界面看不到任何命令，导入后点一下就执行攻击者指定的命令。预览界面形同虚设。
- **证据**：
  ```
  115:        for (int i = 0; i < Math.min(importGroups.size(), 3); i++) {
  116:            WaypointGroup group = importGroups.get(i);
  117:            String text = "  " + group.getColor().getEmoji() + " " + group.getName() +
  118:                    " (" + group.getWaypointCount() + "个)";
  119:            drawCenteredText(context, text, centerX, contentY, TEXT_COLOR);
  ```
- **修复建议**：预览列表改为展示 `name → /command`（命令过长截断），并对非常见前缀（非 `res tp`/`home`/`warp`/`tpa`）的命令标红警示；替换模式（`manager.getGroups().clear()`，`145`）应额外二次确认。

### SEC-09 [中等] 日志泄露完整命令与绝对路径
- **文件**：`src/client/java/com/venus/vmtools/feature/waypoint/TeleportService.java:46`
- **影响分支**：全部 5 个
- **问题描述**：日志以 INFO 级别打印完整命令原文；`FileUtil`/`WaypointIO`/`WaypointManager` 打印文件绝对路径（含系统用户名）。`AutoEscapeManager`/`HealthMonitorManager` 还会打印用户配置的逃逸/确认命令（`96`、`138`、`199`、`217`）。
- **影响**：`logs/latest.log` 常被玩家在求助时整份上传到论坛/issue，命令里可能含服务器域名、residence 名称、传送暗号；绝对路径泄露操作系统用户名与目录结构。属于低成本信息泄露。
- **证据**：
  ```
   46:                VMToolsClient.LOGGER.info("已发送传送命令: /{}", finalCommand);
  ```
  ```
   79:            VMToolsClient.LOGGER.info("已备份文件到: {}", backupPath);      // FileUtil.java:79
   68:            VMToolsClient.LOGGER.info("成功导出路径点到: {}", filePath);     // WaypointIO.java:68
  ```
- **修复建议**：命令降级为 `debug` 并脱敏（只保留前缀 + 参数个数），路径只打印相对 `config` 目录的部分：
  ```java
  LOGGER.debug("发送传送命令: {}*** (len={})", prefixOf(finalCommand), finalCommand.length());
  LOGGER.info("已备份到: {}", CONFIG_DIR.relativize(backupPath));
  ```

### SEC-10 [中等] `confirmBeforeTeleport` 从未生效
- **文件**：`src/client/java/com/venus/vmtools/config/ModConfig.java:51`
- **影响分支**：全部 5 个
- **问题描述**：`confirmBeforeTeleport` 有字段（`51`）与 getter/setter（`108`/`112`），但全仓库无任何调用点。左键单击路径点名称区域即直接 `executeTeleport(wp)`（`WaypointScreen.java:788-790`），没有任何确认环节。
- **影响**：配置项给出"传送前会确认"的错觉，实际单击即发命令；结合 SEC-08 使误触/恶意导入的后果被放大。属于安全相关的虚假配置。
- **证据**：
  ```
   51:    private boolean confirmBeforeTeleport = false;
  108:    public boolean isConfirmBeforeTeleport() {
  109:        return confirmBeforeTeleport;
  ```
  ```
  787:                            // 左键点击路径点名称区域 - 传送
  788:                            if (button == 0 && mouseX >= nameX) {
  789:                                executeTeleport(wp);
  ```
- **修复建议**：在 `executeTeleport()` 入口加分支，或直接删除该配置项以免误导：
  ```java
  if (config.isConfirmBeforeTeleport()) {
      this.minecraft.gui.setScreen(ConfirmScreen.createTeleportConfirm(this, waypoint, () -> doTeleport(waypoint)));
      return;
  }
  ```

### SEC-11 [中等] 自动确认命令可设为任意命令并自动执行
- **文件**：`src/client/java/com/venus/vmtools/gui/SettingsScreen.java:56`
- **影响分支**：全部 5 个
- **问题描述**：设置界面的"确认命令"输入框 `setResponder(text -> config.setConfirmCommand(text))`，无前缀白名单、无长度限制、无确认。该命令在每次传送成功后由独立线程延时自动发送（`WaypointScreen.java:1123-1134`）。同理 `AutoEscapeConfig.confirmCommand` / `HealthMonitorConfig.confirmCommand` 也可自由设置并在触发时自动发送。
- **影响**：一个被社工诱导填入的"确认命令"会在每次传送后静默重复执行，形成持久化的自动命令执行通道；且 `ModConfig` 保存后长期有效。
- **证据**：
  ```
   55:        confirmCommandField.setValue(config.getConfirmCommand());
   56:        confirmCommandField.setResponder(text -> config.setConfirmCommand(text));
  ```
  ```
  1118:                if (shouldConfirm && !config.getConfirmCommand().trim().isEmpty()) {
  1119:                    String confirmCmd = config.getConfirmCommand().trim();
  1120:                    if (confirmCmd.startsWith("/")) confirmCmd = confirmCmd.substring(1);
  ...
  1129:                                    mc.getConnection().sendCommand(cmd);
  ```
- **修复建议**：确认命令同样走 SEC-01 的白名单校验；把自动确认默认值改为关闭（现为 `false` 但 `AutoEscape` 侧默认 `true`），并在 UI 上明示"该命令将在每次传送后自动执行"。

### SEC-12 [中等] AWT 在 headless/沙箱环境抛异常，且全局改写 headless 属性
- **文件**：`src/client/java/com/venus/vmtools/gui/WaypointScreen.java:1235`
- **影响分支**：全部 5 个（`main:1235`、`mc-1.21.6:1232`、`mc-26.1/26.2:1259`、`mc-26.3:1235`）
- **问题描述**：导入流程在裸线程里 `System.setProperty("java.awt.headless", "false")` 后直接 `new FileDialog(...)`；导出后 `Desktop.getDesktop().open(...)`。`System.setProperty` 是**进程级全局副作用**，在 MC 已按 headless 初始化 AWT/GLFW 之后再改，行为未定义；`Desktop.getDesktop()` 在无桌面环境（服务端式 Linux、容器、部分 macOS Java 运行时）抛 `HeadlessException`/`UnsupportedOperationException`。
- **影响**：导入按钮在沙箱/无桌面环境必然失败并打印整段异常栈；全局属性被污染后可能影响其他模组对 AWT 的使用。虽被 `catch (Exception)` 兜住（`1249`、`1328`），但 `setProperty` 已经生效且不会回滚。
- **证据**：
  ```
 1234:                // 强制设置 AWT 非 headless 模式
 1235:                System.setProperty("java.awt.headless", "false");
 1236:
 1237:                FileDialog dialog = new FileDialog((Frame) null, "选择导入文件", FileDialog.LOAD);
  ```
  ```
 1327:                Desktop.getDesktop().open(configDir.toAbsolutePath().toFile());
  ```
- **修复建议**：删除 `System.setProperty`；用 `GraphicsEnvironment.isHeadless()` 与 `Desktop.isDesktopSupported()` 先行判断，不可用时直接走已有的 `showImportFallback()`；导出后只提示路径，不强制打开目录。

### SEC-13 [中等] 声明的 `icon.png` 在全部 5 个分支都不存在
- **文件**：`src/main/resources/fabric.mod.json:14`
- **影响分支**：全部 5 个
- **问题描述**：`"icon": "assets/vmtools/icon.png"`，但 `git ls-tree -r` 在 5 个分支中均查不到 `icon.png`（计数 0），`src/main/resources/assets/vmtools/` 下只有 `lang/`。
- **影响**：Fabric Loader 解析元数据时找不到图标资源，模组列表/ModMenu 中显示破损图标并可能打印告警；属于元数据不实。
- **证据**：
  ```
   13:  "license": "MIT",
   14:  "icon": "assets/vmtools/icon.png",
   15:  "environment": "client",
  ```
  ```
  $ for b in main mc-1.21.6 mc-26.1 mc-26.2 mc-26.3; do git ls-tree -r --name-only $b | grep -c icon.png; done
  0 0 0 0 0
  ```
- **修复建议**：补入 `src/main/resources/assets/vmtools/icon.png`（128×128），或删除 `icon` 字段。

### SEC-14 [中等] `minecraft` 版本约束与实际版本不符
- **文件**：`src/main/resources/fabric.mod.json:26`
- **影响分支**：main（`~1.21.1` vs 实际 1.21.11）、mc-1.21.6（`~1.21.1` vs 实际 1.21.6）
- **问题描述**：`main` 与 `mc-1.21.6` 的 `depends.minecraft` 都写 `~1.21.1`。`~1.21.1` 的语义是 `>=1.21.1 <1.22.0`，会**错误地**同时匹配 1.21.1~1.21.11 全部版本，但这两个分支的字节码/API 分别只对应 1.21.11 与 1.21.6（`main` 用 `sendMovementPackets`、`mc-1.21.6` 无 mixin），实际加载到错误版本会因 `NoSuchMethodError` 崩溃。
- **影响**：用户把 1.21.11 构建装进 1.21.1 客户端时 Loader 不拦截，启动期 Mixin 应用失败 → 直接崩溃；反之 1.21.6 构建装进 1.21.11 也放行。属于依赖约束失效导致的崩溃面。
- **证据**：
  ```
  main / mc-1.21.6:
   25:    "fabricloader": ">=0.16.0",
   26:    "minecraft": "~1.21.1",
   27:    "java": ">=21",
  ```
  ```
  $ grep minecraft_version
  main:      minecraft_version=1.21.11
  mc-1.21.6: minecraft_version=1.21.6
  ```
- **修复建议**：改为精确范围：`main` → `"~1.21.11"`，`mc-1.21.6` → `"~1.21.6"`（26.x 三个分支已正确写 `~26.1/~26.2/~26.3`）。

### SEC-15 [中等] `fabric-api: "*"` 无版本约束；loader 下限与构建版本不一致
- **文件**：`src/main/resources/fabric.mod.json:28`
- **影响分支**：全部 5 个
- **问题描述**：`"fabric-api": "*"` 接受任意 Fabric API 版本，包括远低于编译期 API 的旧版本。同时 `fabricloader` 下限普遍低于实际构建所用 loader：`mc-26.3` 构建用 `loader_version=0.20.1` 而依赖写 `>=0.19.5`；`mc-26.1`（0.19.2 / `>=0.16.0`）、`mc-26.2`（0.19.3 / `>=0.16.0`）差距更大。
- **影响**：用户在旧 Fabric API 上启动会因缺少 `KeyMappingHelper`/`ClientTickEvents` 等符号而崩溃，且错误信息指向模组而非依赖版本，排查成本高；过宽的下限等于放弃 Loader 的依赖解析保护。
- **证据**：
  ```
   24:  "depends": {
   25:    "fabricloader": ">=0.19.5",
   26:    "minecraft": "~26.3",
   27:    "java": ">=25",
   28:    "fabric-api": "*"
   29:  }
  ```
  ```
  mc-26.3 gradle.properties: loader_version=0.20.1  fabric_api_version=0.165.0+26.3
  ```
- **修复建议**：`"fabric-api": ">=0.165.0"`（按分支取对应值），`fabricloader` 下限对齐构建版本（`mc-26.3` → `>=0.20.1`）。另注意 `mc-26.1`/`mc-26.2` 的 `build.gradle:29-30` 用 `implementation` 而非 `modImplementation` 声明 fabric-api/loader，依赖未参与 remap，建议一并修正。

### SEC-16 [中等] Mixin 静态块调试输出残留
- **文件**：`src/client/java/com/venus/vmtools/mixin/LocalPlayerMixin.java:27`
- **影响分支**：mc-26.1、mc-26.2、mc-26.3（`28`）；main（`14`）；mc-1.21.6 无此文件
- **问题描述**：静态初始化块直接 `System.out.println`，绕过日志框架，级别不可控、不受 `log4j` 配置过滤，且每个类加载器各打一次。
- **影响**：污染 stdout（部分启动器/面板把 stdout 作为唯一日志来源）；向用户暴露内部反作弊绕过实现细节（"freeze movement system active"）；属信息泄露 + 代码卫生。
- **证据**：
  ```
   27:    static {
   28:        System.out.println("[VMTools] LocalPlayerMixin loaded - freeze movement system active");
   29:    }
  ```
- **修复建议**：删除该静态块，或改为 `VMToolsClient.LOGGER.debug(...)`。

### SEC-17 [中等] `FreezeManager` 状态字段跨线程读写且非 `volatile`
- **文件**：`src/client/java/com/venus/vmtools/feature/freeze/FreezeManager.java:23`
- **影响分支**：main、mc-26.1、mc-26.2、mc-26.3（`mc-1.21.6` 无 `FreezeManager.java`）
- **问题描述**：`instance`（`20`）、`frozen`（`23`）、`freezeStartTime`（`26`）均非 `volatile`，单例懒加载也无同步。写入发生在客户端主线程（`TeleportService.sendCommand` → `activate`、tick → `deactivate`），读取发生在 `LocalPlayerMixin.onSendPosition` —— 该注入点在客户端 tick 与网络发送路径上被高频调用。
- **影响**：理论上存在可见性问题：`frozen = true` 后 Mixin 侧可能仍读到 `false`，导致冻结失效（绕过移动检测失败，传送被服务端取消）；`instance != null && instance.frozen` 的检查在 `instance` 已发布但字段未初始化时也可能读到默认值。实际表现是"偶发冻结不生效"，极难复现定位。
- **证据**：
  ```
   20:    private static FreezeManager instance;
   21:
   22:    /** 是否处于冻结状态 */
   23:    private boolean frozen = false;
   24:
   25:    /** 冻结开始时间（System.currentTimeMillis） */
   26:    private long freezeStartTime = 0;
  ```
  ```
   56:    public static boolean isActive() {
   57:        return instance != null && instance.frozen;
  ```
- **修复建议**：
  ```java
  private static volatile FreezeManager instance;
  private volatile boolean frozen = false;
  private volatile long freezeStartTime = 0;
  public static FreezeManager getInstance() {           // 双重检查或静态持有
      FreezeManager i = instance;
      if (i == null) { synchronized (FreezeManager.class) { if ((i = instance) == null) instance = i = new FreezeManager(); } }
      return i;
  }
  ```

### SEC-18 [中等] 冻结 `sendPosition()` 的副作用：sprint 状态不同步 + 恢复后位移突变
- **文件**：`src/client/java/com/venus/vmtools/mixin/LocalPlayerMixin.java:31`
- **影响分支**：main（注入 `sendMovementPackets`）、mc-26.1、mc-26.2、mc-26.3（注入 `sendPosition`）
- **问题描述**：反编译 MC 26.3 `LocalPlayer.sendPosition()` 字节码可见，其第一条指令就是 `invokevirtual sendIsSprintingIfNeeded()`，末尾（offset 303-319）才写回 `xLast/yLast/zLast` 并清零 `positionReminder`。在 `HEAD` 处 `ci.cancel()` 会**连带**跳过 sprint 包发送与 `*Last` 快照更新。
- **影响**：① 冻结期间玩家切换疾跑状态不会同步到服务端，可能出现"客户端疾跑/服务端走路"的判定不一致；② 解冻后第一次 `sendPosition` 会一次性发送累积的全部位移（`xLast` 仍是 3 秒前的值），服务端看到瞬时大位移，反作弊（NCP/Matrix/Vulcan 的 speed/fly 检测）有概率误判并回拉或踢出。注释所称"经验证安全"仅覆盖 keep-alive，未覆盖位移突变。
- **证据**：
  ```
   31:    @Inject(method = "sendPosition", at = @At("HEAD"), cancellable = true)
   32:    private void onSendPosition(CallbackInfo ci) {
   33:        if (FreezeManager.isActive()) {
   34:            ci.cancel();
   35:        }
   36:    }
  ```
  ```
  MC 26.3 LocalPlayer.sendPosition() 字节码（javap）：
     0: invokevirtual  // Method sendIsSprintingIfNeeded:()V
   303: putfield       // Field xLast:D
   311: putfield       // Field yLast:D
   319: putfield       // Field zLast:D
  ```
- **修复建议**：改为只拦截位移包、保留 sprint 与快照更新；或在解冻时重置 `xLast/yLast/zLast` 为当前位置以避免突变（需反射/Accessor）。最低成本方案：把冻结时长压到 ≤1 tick 的倍数并限制单次位移阈值，或在 `deactivate()` 时主动发一次 `sendPosition` 前的快照同步。

### SEC-19 [轻微] `exportToFile` 对 `getParent()==null` 无保护
- **文件**：`src/client/java/com/venus/vmtools/feature/waypoint/WaypointIO.java:66`
- **影响分支**：全部 5 个
- **问题描述**：`Files.createDirectories(filePath.getParent())` —— 当 `filePath` 是单段相对路径（如 `Path.of("export.json")`）时 `getParent()` 返回 `null`，`createDirectories(null)` 抛 `NullPointerException`，而 catch 只接 `IOException`。
- **影响**：当前唯一调用点传入的是绝对路径（`WaypointScreen.java:1320-1322`）故暂不触发；但这是公共 API，任何后续调用方传相对路径即崩溃且异常逃逸到调用线程。
- **证据**：
  ```
   62:    public static boolean exportToFile(Path filePath, List<WaypointGroup> groups) {
   63:        try {
   64:            ExportData data = convertToExportData(groups);
   65:            String json = GSON.toJson(data);
   66:            Files.createDirectories(filePath.getParent());
   67:            Files.writeString(filePath, json);
  ```
- **修复建议**：`Path parent = filePath.toAbsolutePath().getParent(); if (parent != null) Files.createDirectories(parent);`，并把 catch 扩为 `catch (IOException | RuntimeException e)`。

### SEC-20 [轻微] 单例懒加载非线程安全
- **文件**：`src/client/java/com/venus/vmtools/feature/freeze/FreezeManager.java:36`；`feature/escape/AutoEscapeManager.java:37`；`feature/escape/HealthMonitorManager.java:37`；`VMToolsClient.java:24`
- **影响分支**：全部 5 个
- **问题描述**：均为 `if (instance == null) instance = new X();` 形式的非同步懒加载，`instance` 未声明 `volatile`。
- **影响**：`AutoEscapeManager`/`HealthMonitorManager` 的构造器会读磁盘配置（`AutoEscapeConfig.load()`），若并发首次访问可能创建两个实例并各自注册一次 `END_CLIENT_TICK` 监听器，导致逃逸命令被发送两次；概率低但后果是重复命令。
- **证据**：
  ```
   36:    public static FreezeManager getInstance() {
   37:        if (instance == null) {
   38:            instance = new FreezeManager();
   39:        }
   40:        return instance;
   41:    }
  ```
- **修复建议**：改为 `private static final X INSTANCE = new X();`（构造器已无副作用依赖时）或双重检查 + `volatile`。

### SEC-21 [轻微] `UIState` 磁盘值未做范围校验
- **文件**：`src/client/java/com/venus/vmtools/gui/WaypointScreen.java:281`
- **影响分支**：全部 5 个
- **问题描述**：从 `ui_state.json` 读出的 `saved.x/y/width/maxHeight` 只判断 `> 0` 就直接使用（`283`、`287`），未与当前屏幕尺寸比较、未设上限。
- **影响**：损坏或被篡改的 `ui_state.json` 可让分组窗口渲染到屏幕外或给出极端尺寸，表现为"界面打开后什么都看不到"（且因 `removed()` 会写回，状态自我固化）。属于可用性问题。
- **证据**：
  ```
  281:                    GroupWindowState state = new GroupWindowState(saved.x, saved.y);
  282:                    state.expanded = saved.expanded;
  283:                    if (saved.width > 0) {
  284:                        state.width = saved.width;
  285:                        state.customWidth = true;
  286:                    }
  287:                    if (saved.maxHeight > 0) {
  288:                        state.maxHeight = saved.maxHeight;
  ```
- **修复建议**：读取时 `Math.max(0, Math.min(saved.x, this.width))`、`Math.max(80, Math.min(saved.width, 600))`，越界则丢弃回退默认布局。

### SEC-22 [轻微] `dataVersion` 只写不读
- **文件**：`src/client/java/com/venus/vmtools/feature/waypoint/WaypointManager.java:31`
- **影响分支**：全部 5 个
- **问题描述**：`VERSION_KEY` 常量与 `DataWrapper.dataVersion` 字段存在，`save()` 写入 `CURRENT_DATA_VERSION`（`141`），但 `loadFromFile()` 从不读取或校验该字段；`VERSION_KEY` 字符串常量（`31`）无任何引用。
- **影响**：宣称的"数据版本向后兼容"实际不存在，未来格式变更时旧/新数据会被无差别反序列化，容易产生静默字段丢失或类型不匹配（结合 SEC-03 即数据丢失）。
- **证据**：
  ```
   31:    private static final String VERSION_KEY = "dataVersion";
   32:    private static final int CURRENT_DATA_VERSION = 1;
  ...
  141:            wrapper.dataVersion = CURRENT_DATA_VERSION;
  ```
- **修复建议**：加载时校验 `if (wrapper.dataVersion > CURRENT_DATA_VERSION) { 拒绝加载并提示 }`，并实现低版本迁移分支。

### SEC-23 [轻微] `contact.sources` URL 无效
- **文件**：`src/main/resources/fabric.mod.json:11`
- **影响分支**：全部 5 个
- **问题描述**：`"sources": "https://github.com/venus/vmtools"` 实测 HTTP 404（已 `web_fetch` 验证）；实际仓库为 `https://github.com/venuyu045/VMTools`（`README.md:116`）。
- **影响**：用户无法从模组列表跳转到源码/报告漏洞，供应链溯源链断裂；对安全审计而言，"源码不可达"会降低用户对构建产物与源码一致性的信任。
- **证据**：
  ```
   10:  "contact": {
   11:    "sources": "https://github.com/venus/vmtools"
   12:  },
  ```
  ```
  README.md:116: - [GitHub](https://github.com/venuyu045/VMTools)
  ```
- **修复建议**：改为 `"https://github.com/venuyu045/VMTools"`，并补充 `"issues"` 字段指向 issue 页。

### SEC-24 [轻微] 声明 MIT 但无 LICENSE 文件
- **文件**：`build.gradle:51`
- **影响分支**：全部 5 个
- **问题描述**：`fabric.mod.json` 声明 `"license": "MIT"`，`build.gradle` 的 `jar { from("LICENSE") ... }` 引用 `LICENSE`，但 5 个分支中均不存在 `LICENSE`/`LICENSE.txt`/`LICENSE.md`。
- **影响**：法律声明缺失；`from("LICENSE")` 静默跳过不报错，产物中无许可证文本，分发合规性存疑。
- **证据**：
  ```
   50: jar {
   51:     from("LICENSE") {
   52:         rename { "${it}_${project.archivesBaseName}" }
   53:     }
   54: }
  ```
  ```
  $ for b in main mc-1.21.6 mc-26.1 mc-26.2 mc-26.3; do git ls-tree -r --name-only $b | grep -cx "LICENSE"; done
  0 0 0 0 0
  ```
- **修复建议**：补入 MIT `LICENSE` 文件（版权方与 `authors` 一致）。

### SEC-25 [轻微] `confirmDelay` 无上下限；死代码
- **文件**：`src/client/java/com/venus/vmtools/feature/escape/AutoEscapeConfig.java:118`；`feature/escape/HealthMonitorConfig.java:118`
- **影响分支**：全部 5 个
- **问题描述**：两个 escape 配置的 `setConfirmDelay(int)` 直接赋值，无 clamp（对比 `ModConfig.setConfirmDelay` 有 `Math.max(100, Math.min(2000, ...))`，`ModConfig.java:169`）。该值经 `Thread.sleep(delay)` 使用，负数会抛 `IllegalArgumentException`（被 catch 后 `Thread.currentThread().interrupt()`），超大值使确认线程长期挂起。另 `util/ChatUtil.java`（76 行）与 `util/FileUtil.java`（105 行）在全部 5 个分支均无任何调用点，属死代码——`ChatUtil.sendCommand` 与 `TeleportService.sendCommand` 是两条并行的、校验强度不同的命令发送通道，保留会增加后续误用风险。
- **影响**：配置被手改为极端值时确认命令丢失或线程堆积；死代码扩大攻击面与维护歧义。
- **证据**：
  ```
  118:    public void setConfirmDelay(int confirmDelay) {
  119:        this.confirmDelay = confirmDelay;
  120:    }
  ```
  ```
  $ grep -rn "ChatUtil\.\|FileUtil\." src/client/java/ 
  （无输出）
  ```
- **修复建议**：`this.confirmDelay = Math.max(0, Math.min(5000, confirmDelay));`；删除或合并 `ChatUtil`/`FileUtil`。

## 跨分支差异要点

| 安全相关差异 | main (1.21.11) | mc-1.21.6 | mc-26.1 | mc-26.2 | mc-26.3 |
|---|---|---|---|---|---|
| 命令发送 API | `sendChatCommand` | `sendChatCommand` | `sendCommand` | `sendCommand` | `sendCommand` |
| 命令白名单校验 | 无 | 无 | 无 | 无 | 无（**5 分支一致，均未修复**） |
| 隐藏命令 `edithome resback relocate` | 无 | 无 | **有**(`TeleportService:49`) | **有**(`TeleportService:49`) | 已删除 |
| `Back` 按钮 | `/back` 快发 | 无 | `/home resback` 硬编码(`WaypointScreen:1180`) | `/home resback` 硬编码 | `/back` 快发 |
| `FreezeManager` | 有 | **无**（无冻结能力） | 有 | 有 | 有 |
| Mixin 注入点 | `sendMovementPackets` | 无 mixin（`client` 数组为空） | `sendPosition` | `sendPosition` | `sendPosition` |
| Mixin 静态块 println | 有(`:14`) | 无 | 有(`:28`) | 有(`:28`) | 有(`:28`) |
| `mixins.json` compatibilityLevel | `JAVA_21` | `JAVA_21` | `JAVA_21` | `JAVA_21` | `JAVA_25` |
| 产物 class 版本 | 65 (Java 21) | 65 | 69 (Java 25) | 69 | 69 |
| `depends.minecraft` | `~1.21.1`（**错**，应 1.21.11） | `~1.21.1`（**错**，应 1.21.6） | `~26.1` ✓ | `~26.2` ✓ | `~26.3` ✓ |
| `depends.java` | `>=21` ✓ | `>=21` ✓ | `>=21`（loader 实为 Java 25 构建，偏宽） | `>=25` ✓ | `>=25` ✓ |
| `fabricloader` 下限 / 构建版本 | `>=0.16.0` / 0.16.14 | `>=0.16.0` / 0.16.14 | `>=0.16.0` / **0.19.2** | `>=0.16.0` / **0.19.3** | `>=0.19.5` / **0.20.1** |
| fabric-api 声明方式 | `modImplementation` ✓ | `modImplementation` ✓ | `implementation` ⚠ | `implementation` ⚠ | `implementation` ⚠ |
| `icon.png` 缺失 | 是 | 是 | 是 | 是 | 是（5 分支一致） |
| `WaypointIO`/`WaypointManager`/`WaypointGroup`/`UIState`/`FileUtil` | 与 mc-26.3 **完全一致**（md5 相同） | 一致 | 一致 | 一致 | 一致 |

**关键结论**：安全缺陷几乎全部集中在**与 MC 版本无关的公共数据层**（`WaypointIO`、`WaypointManager`、`WaypointGroup`、`UIState`、`FileUtil` 在 5 分支逐字节相同），因此**任一分支的修复都必须同步到其余 4 个分支**。分支间真正的安全差异只有两处：① `mc-26.1`/`mc-26.2` 的隐藏 `edithome` 命令（26.3 已修复，但修复未回移）；② `mc-1.21.6` 完全缺失 `FreezeManager` + Mixin，因此不受 SEC-16/17/18 影响，但也意味着它无法绕过移动检测。

**修复未回移的实例**：mc-26.3 删除了 `edithome resback relocate`（`TeleportService` 少 12 行），但 `mc-26.1`/`mc-26.2` 仍保留；反之 `main`/`mc-26.3` 已有 `sendQuickCommand` 而 26.1/26.2 仍是硬编码 `home resback`。说明各分支长期单向 cherry-pick，缺少安全回归同步机制。

## 未发现问题说明

以下面经检查确认安全或风险可接受，记录以免遗漏：

1. **命令注入不经过 shell**：`sendCommand` 走 `ClientPacketListener.sendCommand` → Brigadier `parse` → `ServerboundChatCommandPacket`，不存在 `Runtime.exec`/shell 拼接，本质是"MC 命令"而非 OS 命令注入。反编译确认 `sendCommand` 内部先做 `CommandDispatcher.parse`，`writeUtf` 上限 32767 字节，超长会抛异常并被 `catch (Exception)` 兜住，不会造成协议层越界。
2. **无硬编码凭据/密钥**：全仓库 grep 未发现 token、密码、API key、私钥；`fabric.mod.json` 无 `accessWidener` 等提权声明。
3. **无网络外联**：模组不发起任何 HTTP/socket 请求，不加载远程代码；所有数据仅落本地 `config/vmtools/`。
4. **无 `Runtime.exec` / `ProcessBuilder` / `URLClassLoader` / 反射写字段**（`Class.forName`、`setAccessible` 均未出现）。
5. **导出文件名不可控**：`exportWaypoints()` 的文件名由 `LocalDateTime.now()` 时间戳拼成（`WaypointScreen.java:1318-1320`），用户无法注入路径片段；导入路径来自 `FileDialog` 或固定 `configDir` 扫描（`1264-1267`），不存在路径穿越。`WaypointIO` 只写不删，不会覆盖任意路径（`exportToFile` 由调用方保证目标在 `config/vmtools` 下）。
6. **`data.groups == null` 已处理**：`convertFromExportData` 首行 `if (data == null || data.groups == null) return groups;`（`WaypointIO.java:124`），`groupData.waypoints` 亦有判空（`132`），这部分无 NPE。
7. **`UIState.load()` 有兜底**：`state != null && state.windowStates != null` 才返回，并对 `groupRenderOrder` 补空列表（`UIState.java:55-61`），异常被 catch 后回退新实例。
8. **`WaypointColor.fromName` 有默认值**：非法颜色名回退 `GREEN`（`WaypointColor.java:78`），不会抛异常。
9. **Mixin `require` 设置合理**：`vmtools.mixins.json` 的 `defaultRequire: 1` + `required: true`，目标方法不存在时会在启动期显式失败而非静默失效，符合预期（已用 `javap` 确认 `sendPosition`/`sendMovementPackets` 在对应版本确实存在，且产物中 `@Mixin` 目标已被正确 remap 为 `Lnet/minecraft/client/player/LocalPlayer;` / `Lnet/minecraft/class_746;`）。
10. **Mixin 无 `refmap` 但不需要**：5 分支均未声明 `refmap`，因为 Loom 在构建期已完成 remap（`main` 产物中注入方法名已是 intermediary `method_3136`），运行时无需 refmap。
11. **`FreezeManager.activate(3000)` 时长可控**：`shouldFreeze` 仅在 `freezeEnabled` 开启且命令为 `res tp`/`home` 时触发，不会因任意命令长期冻结移动。
12. **`Thread.sleep` 的确认线程均为 daemon**（`AutoEscapeManager.java:148`、`HealthMonitorManager.java:148`、`WaypointScreen.java:1134` 默认非 daemon 但为短延时），不会阻止 JVM 退出。
13. **`ToastWidget` 静态列表有上限**：`if (toasts.size() > 5) toasts.remove(0)`（`ToastWidget.java:56`），不会无界增长。
14. **`AutoEscapeManager`/`HealthMonitorManager` 日志列表有上限**：`MAX_LOG_ENTRIES = 20` + `while` 裁剪（`162-164`），无内存泄漏。
