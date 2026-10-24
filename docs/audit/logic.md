# 后端逻辑审查报告

> 审查对象：VMTools（Fabric 客户端模组）全部 5 个 MC 版本分支 ｜ 方式：只读静态审查 + Gson 2.13.1 隔离沙盒实证（`/tmp` 副本，未改动仓库）
> **行号基线**：以 git 提交状态（HEAD）为准。审查期间其他成员已在工作区修改部分源码，引用请以 `git show HEAD:<path>` 为准。

## 审查范围

| 分支 | worktree | MC | Mappings |
|------|----------|-----|----------|
| `mc-26.3`（当前检出） | `/root/vmtools-mod` | 26.3 | Mojang |
| `main` | `/tmp/wt/main` | 1.21.11 | Yarn |
| `mc-1.21.6` | `/tmp/wt/mc-1.21.6` | 1.21.6 | Yarn |
| `mc-26.1` | `/tmp/wt/mc-26.1` | 26.1 | Mojang |
| `mc-26.2` | `/tmp/wt/mc-26.2` | 26.2 | Mojang |

源码根：`src/client/java/com/venus/vmtools/`。文件级一致性（`git rev-parse <branch>:<path>` blob hash 比对）：**五分支完全一致**：`WaypointManager`、`WaypointIO`、`UIState`、`WaypointGroup`、`WaypointColor`、`FileUtil`
- **存在差异**：`TeleportService`、`Waypoint`、`AutoEscapeManager/Config`、`HealthMonitorManager/Config`、`VMToolsClient`、`ModConfig`、`ChatUtil`、`WaypointScreen` 及若干 GUI；**仅 `mc-1.21.6` 缺失** `feature/freeze/FreezeManager.java`、`mixin/LocalPlayerMixin.java`、`ModConfig.freezeEnabled`

## 问题汇总

| 编号 | 级别 | 标题 | 文件 | 影响分支 |
|------|------|------|------|----------|
| LOG-01 | 严重 | 备份恢复流程被 `save()` 反向覆盖，不可逆数据丢失 | `WaypointManager.java:73-78` + `:131-137` | 全部 5 个 |
| LOG-02 | 严重 | Gson 绕过构造函数，`waypoints`/`color` 为 null → NPE | `WaypointGroup.java:16,21-27` | 全部 5 个 |
| LOG-03 | 严重 | `loadFromFile()` 两处 `catch (Exception ignored) {}` 静默吞异常 | `WaypointManager.java:104,114` | 全部 5 个 |
| LOG-04 | 中等 | 空分组列表被误判为读取失败 → 重建默认分组 | `WaypointManager.java:110` | 全部 5 个 |
| LOG-05 | 中等 | `dataVersion`/`VERSION_KEY`/`CURRENT_DATA_VERSION` 死代码 | `WaypointManager.java:31-32,141,156` | 全部 5 个 |
| LOG-06 | 中等 | 三个单例 `getInstance()` 懒加载非线程安全 | `FreezeManager.java:36-41` 等 | 26.3/main/26.1/26.2（1.21.6 仅 escape 两个） |
| LOG-07 | 中等 | 裸 `Thread`+`sleep` 后跨线程操作 `client`，断线 NPE | `AutoEscapeManager:129`、`HealthMonitorManager:129`、`WaypointScreen:1123` | 全部 5 个 |
| LOG-08 | 中等 | `TeleportService.sendCommand()` 返回 true 早于实际异步发送 | `TeleportService.java:42-60` | 全部 5 个 |
| LOG-09 | 中等 | 三处命令发送职责重叠；`ChatUtil`/`FileUtil` 死代码 | `ChatUtil.java:14-29` 等 | 全部 5 个 |
| LOG-10 | 中等 | `resetTriggerState()` 定义但从不调用，跨世界状态残留 | `AutoEscapeManager:253`、`HealthMonitorManager:253` | 全部 5 个 |
| LOG-11 | 中等 | 配置 `load()` 只捕获 `IOException`，损坏 JSON 直接崩溃 | `ModConfig.java:87` 等 3 处 | 全部 5 个 |
| LOG-12 | 中等 | 阈值输入 `NaN`/`Infinity` 可写入 → 静默失效 + 保存崩溃 | `AutoEscapeScreen:185,193`+`AutoEscapeConfig:118-120` | 全部 5 个 |
| LOG-13 | 中等 | 导入"替换全部"先 `clear()` 再导入，失败即清空且不回滚 | `ImportConfirmScreen.java:143-152` | 全部 5 个 |
| LOG-14 | 中等 | Toast 仅在 `WaypointScreen` 渲染，其他界面提示不可见 | `ToastWidget.java:64`/`WaypointScreen:388` | 全部 5 个 |
| LOG-15 | 轻微 | `saveAll()` 遗漏 `UIState`/两个 escape 配置 | `VMToolsClient.java:66-71` | 全部 5 个 |
| LOG-16 | 轻微 | `getDisplayCommand()` 死代码 + null NPE + 宽字符截断 | `Waypoint.java:47-52` | 全部 5 个 |
| LOG-17 | 轻微 | `searchWaypoints(keyword)` 无 null 校验 | `WaypointManager.java:298-299` | 全部 5 个 |
| LOG-18 | 轻微 | `ModConfig.load()` 可能返回 null | `ModConfig.java:84-86` | 全部 5 个 |
| LOG-19 | 轻微 | `Waypoint` 构造函数重复赋值 `autoConfirm` | `Waypoint.java:33-34` | main / mc-1.21.6 |
| LOG-20 | 轻微 | `importWaypoints()` 合并语义缺陷（无去重、颜色不覆盖） | `WaypointManager.java:315-338` | 全部 5 个 |

合计 **20 条**：严重 3、中等 11、轻微 6。

## 详细问题

### LOG-01 [严重] 备份恢复流程被 `save()` 反向覆盖，不可逆数据丢失
- **文件**：`WaypointManager.java:73-78`（触发点）、`:126-137`（覆盖点） ｜ **影响分支**：全部 5 个（该文件 blob 完全一致）
- **问题描述**：`load()` 从 `BACKUP_PATH` 恢复后调用 `save()` 写回主文件；但 `save()` 第一步是"主文件存在就先备份"——此时主文件正是那份**损坏**的文件，于是 `Files.copy(DATA_PATH, BACKUP_PATH, REPLACE_EXISTING)` 用损坏数据覆盖了刚读出的唯一完好备份。
- **影响**：备份恢复机制在真正需要它的那一刻自我摧毁。主文件损坏（写盘中断/崩溃/断电/磁盘满半截写入）→ 恢复成功 → 备份被同一份损坏数据覆盖 → 后续任一次 `save()` 出错即**永久丢失且无副本**。与 CLAUDE.md 中"WaypointManager 有备份恢复机制"的承诺直接矛盾。
- **证据**：
```java
 73:        if (loadFromFile(BACKUP_PATH)) {
 75:            // 恢复后立即保存到主文件
 76:            save();
 77:            return;
...
131:            if (Files.exists(DATA_PATH)) {
133:                    Files.copy(DATA_PATH, BACKUP_PATH, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
```
- **修复建议**：恢复路径跳过备份轮转（`save(boolean skipBackup)` 或独立 `writeDataFile()`）；先把损坏主文件 `Files.move` 为 `waypoints.json.corrupt.<ts>` 保留现场；`save()` 改为"写临时文件 + `Files.move(ATOMIC_MOVE)`"。

### LOG-02 [严重] Gson 绕过构造函数，`waypoints`/`color` 为 null → NPE
- **文件**：`WaypointGroup.java:16,21-27,69-71`；同源 `Waypoint.java:11-34` ｜ **影响分支**：全部 5 个
- **问题描述**：`waypoints` 仅在构造函数 `new ArrayList<>()`，类中无无参构造、无字段初始值、无 null 兜底。Gson 走 `Unsafe` 分配，**不调用构造函数**，字段缺失即为 `null`。`Waypoint` 的 `command`/`color`/`id` 同理。
- **影响**：任何"字段缺失/被手改/旧版本无该字段/导入第三方文件"的数据都能让模组在渲染线程直接 NPE（`getWaypointCount()` 在 `WaypointScreen:443` 每帧调用）。沙盒实证（Gson 2.13.1）：`{"id":"abc","name":"g1"}` → `waypoints=null, color=null`，`getWaypointCount()` → `NullPointerException`；缺 `command` 的路径点 → `getDisplayCommand()` → NPE。
- **证据**：
```java
 16:    private List<Waypoint> waypoints;
 21:    public WaypointGroup(String name) {
 26:        this.waypoints = new ArrayList<>();
 69:    public int getWaypointCount() {
 70:        return waypoints.size();     // 反序列化后为 null -> NPE
```
- **修复建议**：字段声明处直接初始化 + `getWaypoints()` 内 null 懒修复；`WaypointColor` 缺失回退默认色；`loadFromFile()` 后统一跑 `sanitize()` 校验。

### LOG-03 [严重] `loadFromFile()` 两处 `catch (Exception ignored) {}` 静默吞异常
- **文件**：`WaypointManager.java:104`、`:114` ｜ **影响分支**：全部 5 个
- **问题描述**：新/旧格式两次解析尝试各自空 `catch` 吞掉全部异常（含 `JsonSyntaxException`），无日志、无返回区分，最终只返回 `false`。
- **影响**：数据损坏时日志只有"未找到数据文件，创建默认分组"（`:81`）这种**误导性**提示，无法判断是语法错误还是字段不匹配。这是 LOG-01 丢失链路上的"隐形推手"——没人能发现恢复失败。
- **证据**：
```java
 99:                DataWrapper wrapper = GSON.fromJson(json, DataWrapper.class);
100:                if (wrapper != null && wrapper.groups != null) {
102:                    return true;
104:            } catch (Exception ignored) {}
109:                List<WaypointGroup> loaded = GSON.fromJson(json, listType);
110:                if (loaded != null && !loaded.isEmpty()) {
114:            } catch (Exception ignored) {}
```
- **修复建议**：至少 `LOGGER.warn(..., e)`；区分"不存在/语法错误/结构合法但为空"（返回 `enum LoadResult`）；语法错误时主动重命名损坏文件并提示用户。

### LOG-04 [中等] 空分组列表被误判为读取失败 → 重建默认分组
- **文件**：`WaypointManager.java:110`（旧格式）、对比 `:100`（新格式） ｜ **影响分支**：全部 5 个
- **问题描述**：旧格式分支用 `!loaded.isEmpty()` 判成功，即 `[]` 视为失败；新格式分支只判 `groups != null`，`{"groups":[]}` 是成功的。两分支语义不一致。沙盒实证：`fromJson("[]", List)` 返回空 `ArrayList`（非 null），`!isEmpty()` 为 `false`。
- **影响**：用户删光所有分组（合法空状态）后，若文件恰为旧格式，重启即被判"损坏"→ 走备份 → 最终 `createDefaultGroups()` 重建"常用地点/矿洞/村庄"，并额外触发一次覆盖写入，放大 LOG-01 的丢数据窗口。
- **证据**：
```java
100:                if (wrapper != null && wrapper.groups != null) {   // 空列表 = 成功
110:                if (loaded != null && !loaded.isEmpty()) {         // 空列表 = 失败（语义不一致）
```
- **修复建议**：旧格式改为 `if (loaded != null)`，把"空"与"损坏"彻底分开；空列表记 `INFO` 而非降级默认分组。

### LOG-05 [中等] `dataVersion`/`VERSION_KEY`/`CURRENT_DATA_VERSION` 是死代码
- **文件**：`WaypointManager.java:31-32`（定义）、`:141`、`:156`（仅写入） ｜ **影响分支**：全部 5 个
- **问题描述**：`git grep` 全分支确认，`dataVersion` 只有写入路径，**无任何读取或比较**；`VERSION_KEY` 从未被引用。沙盒实证：`{"dataVersion":99,...}` 被无差别接受。
- **影响**：号称"支持向后兼容"的版本机制形同虚设。未来字段重命名/结构调整时无法识别旧数据、无法迁移、无法拒绝未来版本，只能靠 LOG-02 的 NPE 暴露。
- **证据**：
```java
 31:    private static final String VERSION_KEY = "dataVersion";   // 从未被引用
 32:    private static final int CURRENT_DATA_VERSION = 1;
141:            wrapper.dataVersion = CURRENT_DATA_VERSION;         // 只写不读
156:        int dataVersion = CURRENT_DATA_VERSION;                 // 字段只写不读
```
- **修复建议**：在 `loadFromFile()` 中真正比较：`> CURRENT` 拒绝加载并提示，`< CURRENT` 走 `migrate(from, to)`；删除无用 `VERSION_KEY`。

### LOG-06 [中等] 三个单例 `getInstance()` 懒加载非线程安全
- **文件**：`FreezeManager.java:36-41`、`AutoEscapeManager.java:37-42`、`HealthMonitorManager.java:37-42`（三处行号一致） ｜ **影响分支**：escape 两个影响全部 5 个；`FreezeManager` 影响 26.3/main/26.1/26.2（1.21.6 无此文件）
- **问题描述**：`instance` 无 `volatile`、方法无 `synchronized`。`FreezeManager.isActive()`（`:56-58`）被 Mixin 在渲染线程调用，`getInstance()` 被 tick/异步线程调用。
- **影响**：极端情况下产生两个实例：Mixin 读到的 `instance` 与实际 `activate()` 的不是同一个 → 冻结静默失效（传送被服务端移动检测取消）；`AutoEscapeManager` 双实例会让配置被两份内存副本互相覆盖。`frozen`/`freezeStartTime`/`triggered` 均非 `volatile`，跨线程可见性无保证。
- **证据**：
```java
20:    private static FreezeManager instance;
23:    private boolean frozen = false;          // 非 volatile，Mixin 跨线程读
26:    private long freezeStartTime = 0;        // 非 volatile
36:    public static FreezeManager getInstance() {
37:        if (instance == null) {
38:            instance = new FreezeManager();
```
- **修复建议**：改 `private static final FreezeManager INSTANCE = new FreezeManager();`（或 `enum` 单例）；`frozen`/`freezeStartTime`/`triggered` 加 `volatile`。初始化已在 `onInitializeClient()` 单线程完成，静态常量最简且无锁开销。

### LOG-07 [中等] 裸 `Thread`+`sleep` 后跨线程操作 `client`，断线 NPE
- **文件**：`AutoEscapeManager.java:129-149`、`HealthMonitorManager.java:129-149`、`WaypointScreen.java:1123-1134` ｜ **影响分支**：全部 5 个（三处代码逐字相同，仅类型名不同）
- **问题描述**：为延迟发送确认命令，每次触发都 `new Thread` + `Thread.sleep(delay)`，醒来后在 `client.execute()` 里直接 `client.getConnection().sendCommand(...)`。`client.execute()` 只保证主线程执行，**不保证连接仍在**。`WaypointScreen:1123` 未设 `setDaemon`，`catch (InterruptedException ignored) {}` 空吞；`delay` 无下限校验，负值使 `Thread.sleep` 抛 `IllegalArgumentException`（已实证）。
- **影响**：玩家在延迟窗口内退出/切世界 → `client.getConnection()` 为 `null` → 两个 Manager 版本无判空，仅靠 `catch (Exception e)` 兜住并吞掉真实错误。非 daemon 线程可能拖住 JVM 退出；每次触发建一个线程，无去重无取消。
- **证据**：
```java
// AutoEscapeManager.java:129-137（HealthMonitorManager 同构）
129:        Thread confirmThread = new Thread(() -> {
130:            try {
131:                Thread.sleep(delay);
132:                client.execute(() -> {
133:                    try {
137:                        client.getConnection().sendCommand(actualConfirm);   // 断线后 NPE
```
- **修复建议**：改用 tick 计数器或单线程 daemon `ScheduledExecutorService`；回调内先 `if (client.player == null || client.getConnection() == null) return;`；`delay` 钳制 `[100, 2000]`；保留 `Thread` 必须 `setDaemon(true)`。

### LOG-08 [中等] `TeleportService.sendCommand()` 返回值语义错误
- **文件**：`TeleportService.java:42-60`（mc-26.3；main `:35-60`、1.21.6 `:35-55`、26.1/26.2 `:45-68`） ｜ **影响分支**：全部 5 个
- **问题描述**：方法在把任务投递进 `client.execute()` 后**立即** `return true`，真正可能失败的发送在异步块内，异常只写日志 + 一条 overlay 提示。返回值只代表"参数校验通过且任务已入队"，却被调用方当作"发送成功"。
- **影响**：`WaypointManager.executeTeleport()`（`:285-293`）据 `success` 执行 `recordUse()` 并 `save()`；`WaypointScreen.executeTeleport()`（`:1111-1113`）据它弹"已发送"并继续走自动确认。即实际发送失败时使用次数被错误累加、UI 报成功、随后还发送一条孤立确认命令。
- **证据**：
```java
 42:        client.execute(() -> {
 43:            try {
 45:                client.getConnection().sendCommand(finalCommand);
 52:            } catch (Exception e) {
 53:                VMToolsClient.LOGGER.error("发送命令失败", e);
 60:        return true;      // 早于实际发送，异常不影响返回值
```
- **修复建议**：改 `void` 或重命名 `enqueueCommand` 且语义为"已受理"；`recordUse()` 与成功 Toast 移入异步 try 成功分支，或提供 `CompletableFuture<Boolean>`。

### LOG-09 [中等] 三处命令发送职责重叠；`ChatUtil`/`FileUtil` 整体死代码
- **文件**：`WaypointManager.java:285-293`、`TeleportService.java:22-61`、`ChatUtil.java:14-29`、`FileUtil.java:23-104` ｜ **影响分支**：全部 5 个
- **问题描述**：命令发送散落三处：`WaypointManager.executeTeleport`、`TeleportService.sendCommand`（normalize+冻结+异步）、`ChatUtil.sendCommand`（再次 normalize+异步，但**无**冻结、无返回值）。`WaypointScreen` 还内联第四处（`sendQuickCommand:1150-1158`）。`git grep` 确认 `ChatUtil` 与 `FileUtil` **零调用点**。
- **影响**：行为分叉——快捷按钮/自动确认走"无冻结、无 normalize"路径，`/res tp` 类命令在这些入口不激活 `FreezeManager`，传送可能被服务端移动检测取消（核心卖点失效）。死代码增加维护成本，后续修复易改错文件。
- **证据**：
```java
// ChatUtil.java:14-29（无调用点）
14:    public static void sendCommand(String command) {
16:        if (client.player == null || client.getConnection() == null) return;
21:        if (command.startsWith("/")) command = command.substring(1);
26:        client.execute(() -> { client.getConnection().sendCommand(finalCommand); });
// TeleportService.java:36 还多做了 if (shouldFreeze(normalizedCommand)) FreezeManager.getInstance().activate(3000);
```
- **修复建议**：收敛为唯一出口 `TeleportService.sendCommand`（normalize+freeze+异步+失败反馈），`WaypointManager`/GUI 全部委托；删除 `ChatUtil.sendCommand` 与 `FileUtil` 未引用方法。

### LOG-10 [中等] `resetTriggerState()` 定义但从不调用，跨世界状态残留
- **文件**：`AutoEscapeManager.java:250-256`、`HealthMonitorManager.java:250-256` ｜ **影响分支**：全部 5 个
- **问题描述**：两个 Manager 都提供 `resetTriggerState()`，注释写明"用于玩家断开连接或世界切换"，但 `git grep` 显示**无调用点**；`VMToolsClient` 未注册 `ClientPlayConnectionEvents.JOIN`/`DISCONNECT`。
- **影响**：`triggered` 一旦置 `true`，只有坐标/血量回到安全值才复位。玩家低血量时退出并换服/重连后，若新环境血量仍低于阈值，`triggered` 保持 `true` → 逃逸命令**静默不触发**，恰是本功能最需要工作的场景。
- **证据**：
```java
250:    /**
251:     * 重置触发状态（用于玩家断开连接或世界切换）
252:     */
253:    public void resetTriggerState() {
254:        triggered = false;
```
- **修复建议**：在 `onInitializeClient()` 注册 `ClientPlayConnectionEvents.JOIN`/`DISCONNECT` 调用两者；同时清理 `logEntries` 避免跨服日志串台。

### LOG-11 [中等] 配置 `load()` 只捕获 `IOException`，损坏 JSON 直接崩溃
- **文件**：`ModConfig.java:87`、`AutoEscapeConfig.java:53`、`HealthMonitorConfig.java:53` ｜ **影响分支**：全部 5 个
- **问题描述**：三处 `load()` 均 `catch (IOException e)`，但 `GSON.fromJson` 对畸形 JSON 抛 `JsonSyntaxException`（`RuntimeException`），不在捕获范围。沙盒实证：截断 JSON → `JsonSyntaxException` 未被捕获。
- **影响**：用户手改配置漏一个括号 → 客户端**启动阶段**（`onInitializeClient` 内同步调用）直接崩溃，无降级默认配置。对 `vmtools.json` 尤其严重。
- **证据**：
```java
// ModConfig.java:82-89（AutoEscapeConfig:48-55、HealthMonitorConfig:48-55 同构）
82:            try {
84:                ModConfig config = GSON.fromJson(json, ModConfig.class);
86:                return config;
87:            } catch (IOException e) {
88:                VMToolsClient.LOGGER.error("加载配置文件失败", e);
```
- **修复建议**：改 `catch (Exception e)`（或加捕 `JsonSyntaxException`），失败时把原文件备份为 `.bad` 并返回 `new ModConfig()`；加载后对关键字段做范围钳制。

### LOG-12 [中等] 阈值输入 `NaN`/`Infinity` 可写入 → 静默失效 + 保存崩溃
- **文件**：`AutoEscapeScreen.java:185,193`（`Double.parseDouble`）→ `AutoEscapeConfig.java:86-88,118-120`（无钳制） ｜ **影响分支**：全部 5 个（26.3/26.1/26.2 为 185/193；main 189/197；1.21.6 188/196）
- **问题描述**：responder 直接 `Double.parseDouble(text)`，`"NaN"`/`"Infinity"` 均可解析（已实证）并写入。`AutoEscapeConfig.setThreshold`/`setConfirmDelay` **无任何范围钳制**（对比 `ModConfig.setConfirmDelay:168-170` 有 `Math.max(100, Math.min(2000, ...))`）。
- **影响**：阈值写成 `NaN` 后 `playerY < NaN` 恒 `false`（已实证），自动逃脱**永久静默失效**且 UI 看不出异常。下一次 `config.save()` 时 `GSON.toJson` 对 `NaN` 抛 `IllegalArgumentException`（已实证），而 `save()` 只 `catch (IOException)` → 异常逃逸到 responder → 主线程崩溃。`confirmDelay` 负值使 `Thread.sleep` 抛 `IllegalArgumentException`。
- **证据**：
```java
// AutoEscapeScreen.java:184-186
184:                voidThresholdField = makeField(fx, fy, fw, String.valueOf(vc.getThreshold()), "-70.0", 10, text -> {
185:                    try { AutoEscapeManager.getInstance().setThreshold(Double.parseDouble(text)); } catch (NumberFormatException ignored) {}
// AutoEscapeConfig.java:118-120
118:    public void setConfirmDelay(int confirmDelay) {
119:        this.confirmDelay = confirmDelay;        // 无钳制（ModConfig:169 有）
```
- **修复建议**：setter 内 `if (!Double.isFinite(v)) return;`，`threshold` 限 `[-256, 320]`，`confirmDelay` 限 `[100, 2000]`；responder 侧用 `Double.isFinite` 校验并高亮非法输入而非静默忽略。

### LOG-13 [中等] 导入"替换全部"先 `clear()` 再导入，失败即清空且不回滚
- **文件**：`ImportConfirmScreen.java:140-153` ｜ **影响分支**：全部 5 个（1.21.6 行号 143/148/150，其余 145/149/151）
- **问题描述**：替换模式直接 `manager.getGroups().clear()` 清空内存列表，再 `importWaypoints`。若导入抛异常（如 LOG-02 的 null 字段 NPE）或 `save()` 失败，内存与磁盘不一致，无回滚无备份。
- **影响**：一次失败的"替换全部"即可清空用户全部路径点；配合 LOG-01，磁盘备份也已是清空后的状态。`getGroups()`（`WaypointManager:214-216`）直接返回内部可变列表，GUI 绕过封装 `clear()`，破坏"所有变更经 `save()` 并维护一致性"的约定。
- **证据**：
```java
140:    private void doImport() {
141:        WaypointManager manager = VMToolsClient.getInstance().getWaypointManager();
143:        if (importMode == 1) {
145:            manager.getGroups().clear();          // 先清空，无回滚
149:        int count = manager.importWaypoints(importGroups);
```
- **修复建议**：提供 `replaceAll(List<WaypointGroup>)`——在副本上完成导入与校验，成功后再整体替换并 `save()`；失败保持原列表。`getGroups()` 改为 `unmodifiableList`。

### LOG-14 [中等] Toast 仅在 `WaypointScreen` 渲染，其他界面提示不可见
- **文件**：`ToastWidget.java:64`（`render`）、唯一调用点 `WaypointScreen.java:388`（main 388、1.21.6 363、26.1/26.2 369） ｜ **影响分支**：全部 5 个
- **问题描述**：`EditWaypointScreen`（6 处 `show*`）、`EditGroupScreen`（3 处）、`ImportConfirmScreen`（1 处）都调用 `ToastWidget.show*`，但这些 Screen 从不调用 `ToastWidget.render`，静态 `toasts` 只被 `WaypointScreen` 绘制。
- **影响**：用户在这些界面看到的校验错误（"路径点名称不能为空""无效的命令格式"）和成功提示**完全不可见**，只在返回主界面时"迟到"闪现。`EditWaypointScreen.saveWaypoint()` 的 4 个校验分支（`:472/476/482/487`）全依赖该提示，实际体验为"点保存没反应"。
- **证据**：
```java
// ToastWidget.java:64 —— 唯一渲染入口
 64:    public static void render(GuiGraphicsExtractor context, int screenWidth, int screenHeight) {
// 全库唯一调用点 WaypointScreen.java:388
388:        ToastWidget.render(context, this.width, this.height);
// 但 EditWaypointScreen.java:472/476/482/487 都在 show*，本类无 render 调用
```
- **修复建议**：抽 `BaseScreen` 或在各 Screen `extractRenderState` 末尾统一调用 `ToastWidget.render`；更优是改用 MC 原生 `ToastManager`/`SystemToast`，由引擎统一渲染，天然跨界面可见。

### LOG-15 [轻微] `saveAll()` 遗漏 `UIState`/两个 escape 配置
- **文件**：`VMToolsClient.java:66-71`（`CLIENT_STOPPING` 注册于 `:56-58`） ｜ **影响分支**：全部 5 个
- **问题描述**：退出时只保存 `config` 与 `waypointManager`。`UIState` 依赖 `WaypointScreen.removed()`（`:1365`），escape 配置依赖各自 setter 内即时 `save()`。若退出时界面仍打开或配置在别处被改动，最后一次变更丢失。
- **影响**：窗口位置/展开状态偶发回退；配置"改了但重启丢失"。`removed()` 只在 Screen 被替换时触发，`CLIENT_STOPPING` 阶段不一定经过。
- **证据**：
```java
 56:        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
 57:            saveAll();
 66:    public void saveAll() {
 68:        config.save();
 69:        waypointManager.save();       // 缺 UIState / AutoEscapeConfig / HealthMonitorConfig
```
- **修复建议**：补齐两个 escape 配置的 `save()`；为 `UIState` 提供静态 `saveIfDirty()`，由 `saveAll()` 调用。

### LOG-16 [轻微] `Waypoint.getDisplayCommand()` 死代码 + null NPE + 宽字符截断
- **文件**：`Waypoint.java:47-52`（26.3/26.1/26.2；main 与 1.21.6 为 `:48-53`） ｜ **影响分支**：全部 5 个
- **问题描述**：`git grep` 确认该方法**无调用点**（`WaypointScreen:520` 直接渲染 `wp.getName()`）。`command.length() > 30` 在 `command == null` 时 NPE；按 `char` 截断对中文/emoji 不友好（27 个中文 = 54 个半角宽），且可能切断代理对。
- **影响**：当前无运行时影响（死代码），一旦启用即引入 NPE 与显示错位；`TeleportService.getDisplayCommand(String)`（`:118-124`）有 null 兜底，两者行为不一致。
- **证据**：
```java
 47:    public String getDisplayCommand() {
 48:        if (command.length() > 30) {          // command == null -> NPE
 49:            return command.substring(0, 27) + "...";   // 按 char 截断
```
- **修复建议**：删除（统一走 `TeleportService.getDisplayCommand`）；若保留，先判 null，并按 `font.width` 像素宽度而非 `length()` 截断。

### LOG-17 [轻微] `searchWaypoints(keyword)` 无 null 校验
- **文件**：`WaypointManager.java:298-299` ｜ **影响分支**：全部 5 个
- **问题描述**：第一行 `keyword.toLowerCase()` 直接解引用；`:304` 的 `wp.getCommand().toLowerCase()` 在 command 为 null 时同样 NPE。当前无调用点（GUI 内联了等价逻辑），属潜在缺陷。
- **影响**：若接入搜索框或未来统一走此方法，空关键字/空命令即崩溃。GUI 内联版本（`WaypointScreen:340-341` 等 9 处）同样未判空，LOG-02 的 null `command` 会直接崩。
- **证据**：
```java
298:    public List<Waypoint> searchWaypoints(String keyword) {
299:        String lowerKeyword = keyword.toLowerCase();       // null -> NPE
303:                if (wp.getName().toLowerCase().contains(lowerKeyword) ||
304:                    wp.getCommand().toLowerCase().contains(lowerKeyword)) {   // null command -> NPE
```
- **修复建议**：`keyword == null ? "" : keyword.toLowerCase()`；`name`/`command` 用 `Objects.toString(x, "")` 兜底；GUI 侧统一改调该方法以消除 9 处重复过滤。

### LOG-18 [轻微] `ModConfig.load()` 可能返回 null
- **文件**：`ModConfig.java:84-86` ｜ **影响分支**：全部 5 个
- **问题描述**：`GSON.fromJson("null", ModConfig.class)` 返回 `null`（已实证），而 `load()` 直接 `return config` 不判空。文件内容为字面量 `null` 时返回 null。
- **影响**：`onInitializeClient` 把 null 赋给 `config` → 后续任意 `getConfig().isFreezeEnabled()` 崩溃。概率低但后果是启动即崩。
- **证据**：
```java
84:                ModConfig config = GSON.fromJson(json, ModConfig.class);
85:                VMToolsClient.LOGGER.info("配置文件已加载");
86:                return config;        // 可能为 null
```
- **修复建议**：`return config != null ? config : new ModConfig();`；`AutoEscapeConfig.load()`/`HealthMonitorConfig.load()`（`:50-52`）同样处理。

### LOG-19 [轻微] `Waypoint` 构造函数重复赋值 `autoConfirm`
- **文件**：`Waypoint.java:33-34` ｜ **影响分支**：仅 `main`、`mc-1.21.6`（26.3/26.1/26.2 只有 `:33` 一行）
- **问题描述**：两行连续赋值，前者被后者立即覆盖，属合并/移植残留。
- **影响**：无功能影响，但会误导维护者以为默认值是 `false`（实际 `null`，语义为"跟随全局设置"）。
- **证据**：
```java
 32:        this.useCount = 0;
 33:        this.autoConfirm = false;    // 立即被下一行覆盖
 34:        this.autoConfirm = null;
```
- **修复建议**：删除 `:33`，与 26.x 分支对齐。

### LOG-20 [轻微] `importWaypoints()` 合并语义缺陷
- **文件**：`WaypointManager.java:315-338` ｜ **影响分支**：全部 5 个
- **问题描述**：按"同名分组"复用（大小写敏感、不 trim）；新分组才拷贝颜色，同名分组**保留本地颜色**（意图如此但无提示）；路径点 ID 来自 `WaypointIO.convertFromExportData` 新建的随机 UUID 前 8 位（`Waypoint.java:25`），无查重，重复导入同一文件会产生**完全重复的路径点**。
- **影响**：反复导入同一备份会线性堆积重复项，用户只能手工删。
- **证据**：
```java
319:            WaypointGroup existingGroup = groups.stream()
320:                    .filter(g -> g.getName().equals(importedGroup.getName()))   // 大小写敏感、不 trim
326:                existingGroup.setColor(importedGroup.getColor());           // 仅新建时拷贝颜色
330:            for (Waypoint wp : importedGroup.getWaypoints()) {
332:                existingGroup.addWaypoint(wp);                              // 无去重
```
- **修复建议**：分组匹配改 `equalsIgnoreCase` + `trim()`；导入前按 `(name, command)` 去重并提示"跳过 N 个重复项"；返回结构化结果（新增/跳过/失败）。

## 跨分支差异要点

1. **`mc-1.21.6` 缺少整个冻结模块**：无 `FreezeManager.java`、无 `LocalPlayerMixin.java`、`vmtools.mixins.json` 的 `client` 数组为空、`ModConfig` 无 `freezeEnabled`、`TeleportService` 无 `shouldFreeze()`、`SettingsScreen` 无"绕过移动检测"开关。因此 **LOG-06 的 `FreezeManager` 部分、以及"传送被移动检测取消"相关影响不适用于 1.21.6**。其余 4 分支均含完整链路（26.x 注入 `LocalPlayer.sendPosition`，main 注入 `ClientPlayerEntity.sendMovementPackets`）。
2. **`TeleportService` 三套实现**：`mc-26.1`/`mc-26.2`（两者 blob 完全相同，141 行）多一段 `isResTp()` + `resback` 逻辑——发送 `/res tp` 前先执行 `edithome resback relocate`（`:41,48-50,89-92`）。该行为在 26.3 与 main 中已移除，**属功能回退，需确认是否有意为之**。`main` 118 行、`mc-1.21.6` 105 行（无 `shouldFreeze`）、`mc-26.3` 125 行。
3. **`WaypointScreen` 顶栏三条不同命令发送路径**：`mc-26.1`/`mc-26.2` 的 "Back" 按钮走 `sendBackCommand()` 硬编码 `home resback`（26.1 `:1174-1182`，无冻结、不走 `TeleportService`）；`mc-26.3`/`main` 改为三个快捷按钮（`末影箱/发光/Back`）走 `sendQuickCommand()`；`mc-1.21.6` 用顶部 Tab 栏（`:662-679`）进 `AutoEscapeScreen`。**三条路径均绕过 `TeleportService`**，是 LOG-09 的具体表现。
4. **`Waypoint` 构造函数差异**：`main`/`mc-1.21.6` 存在重复的 `autoConfirm = false;`（LOG-19），26.x 已清理。`ChatUtil` 有两套映射（`main`/`mc-1.21.6` 用 `sendChatCommand`/`sendMessage(text,true|false)`，26.x 用 `sendCommand`/`sendOverlayMessage`），逻辑一致且均无调用点。
5. **行号偏移**：`executeTeleport` 定义在 26.3 `:1109`、main `:1112`、1.21.6 `:1122`、26.1/26.2 `:1135`；`removed()` 在 26.3/main `:1365`、1.21.6 `:1362`、26.1/26.2 `:1389`。引用时需按分支换算。
6. **默认命令差异**：`HealthMonitorConfig` 默认命令 1.21.6 为 `/home`、其余为 `/ehomes home`；`AutoEscapeConfig` 1.21.6 为 `/ehome`、其余为 `/ehomes home`。属服务器插件适配，非缺陷，但升级分支会改变默认行为。
7. **GUI 层重写程度**：`ToastWidget` 在 26.3/26.1/26.2 同一 blob，`main`/`mc-1.21.6` 各自不同；`ImportConfirmScreen` 26.3/26.1/26.2 同一 blob。LOG-14 在五分支均成立，不受重写影响。

## 未发现问题说明

以下为审查重点中已排查、**确认无缺陷**或**风险不成立**的项：

- **核心数据类五分支字节级一致**（blob hash 相同）：`WaypointManager`/`WaypointIO`/`UIState`/`WaypointGroup`/`WaypointColor`/`FileUtil`，无"某分支单独修好、另一分支仍带病"的分叉风险。
- **`UIState.load()` null 防御完整**：`state != null && state.windowStates != null`（`:55`）+ `groupRenderOrder` 补空（`:57-59`），`fromJson("null")` 场景已正确拦截；`save()` 有 `createDirectories` + `IOException` 日志（`:72-79`）。自身无问题（生命周期缺口见 LOG-15）。
- **`LocalDateTime` 双适配器注册不构成缺陷**：`WaypointManager:36-41` 对同一类型先后注册 `JsonSerializer` 与 `JsonDeserializer`，实测序列化/反序列化均正常（`registerTypeAdapter` 会保留已有 adapter 的另一半能力）。CLAUDE.md 担心的 Java 21+ 模块反射限制已被该 TypeAdapter 正确规避。
- **`WaypointColor.fromName(null)` 不抛异常**：`:74` 的 `equalsIgnoreCase(null)` 与 `displayName.equals(null)` 均安全返回 `false`，最终走 `:78` 的 `GREEN` 兜底（静默降级，仅缺日志）。**`TeleportService.normalizeCommand` 对纯 `/` 处理正确**：`"/"` → trim 去前缀 → `isEmpty()` → 返回 `null`（`:90-97`），随后 `sendCommand` 在 `:30-33` 返回 `false`。
- **`ModConfig` setter 均有范围钳制**（`setConfirmDelay:168-170`、`setUiScale:120-122`、`setAnimationDuration:128-130`），是项目中**唯一**做钳制的配置类——反衬 LOG-12 的缺失。**`WaypointGroup.moveWaypoint` 边界检查完整**（`:47-54`）：负索引、越界、`fromIndex == toIndex` 均提前返回，插入位置换算 `toIndex > fromIndex ? toIndex - 1 : toIndex` 正确。
- **`ToastWidget` 自身内存管理无泄漏**：上限 5 条（`:56-58`），渲染时按 `TOAST_DURATION` 惰性 `iterator.remove()`（`:77-80`）。**`WaypointIO` 导入导出链路本身无缺陷**：`importFromFile` 对不存在文件返回 null 并 warn（`:81-84`），`exportToFile` 有 `createDirectories` + 返回值（`:66-73`），调用方 `doImport`（`WaypointScreen:1294-1302`）与 `exportWaypoints`（`:1322-1333`）都做了 null/失败分支（除 LOG-13 的替换模式）。
- **`java.awt` 有 try-catch 兜底**：`Desktop.getDesktop().open(...)` 包在 `catch (Exception e)`（`WaypointScreen:1326-1330`）；`FileDialog` 整块包在 `catch (Exception e)` 并有降级 `showImportFallback`（`:1249-1253`）。**headless 环境不会崩溃**。
- **`FreezeManager.onClientTick` 自动解除逻辑正确**（`:106-116`）：`elapsed >= freezeDuration` 时 `deactivate()`，`getRemainingTime()` 有 `Math.max(0, ...)` 钳制。**`CLIENT_STOPPING` 注册本身正确**（`VMToolsClient:56-58`），仅保存范围不全（LOG-15）。**两个 Manager 的阈值触发/复位状态机本身正确**（`:68-77`）：低于阈值且未触发 → 触发置位；回到安全值 → 复位，问题在跨世界残留（LOG-10）而非状态机逻辑。
