# VMTools 本地开发环境说明

> 环境初始化时生成，记录本机（容器）中已验证可用的工具链、构建方式与已知限制。

## 1. 仓库状态

| 项 | 值 |
|---|---|
| 仓库 | https://github.com/venuyu045/VMTools |
| 本地路径 | `/root/vmtools-mod` |
| 当前分支 | `main`（MC 1.21.11） |
| 远端 | `origin` → `https://github.com/venuyu045/VMTools.git` |
| 历史 | 完整（30 commits），4 个分支全部已 fetch |

### 分支与版本矩阵

| 分支 | Minecraft | Java | Gradle | Loom | Mappings | 产物名 |
|------|-----------|------|--------|------|----------|--------|
| `main` | 1.21.11 | 21 | 9.4.0 | 1.16.2 | Yarn | `vmtools-mc1.21.11-<ver>.jar` |
| `mc-1.21.6` | 1.21.6 | 21 | 9.4.0 | 1.9-SNAPSHOT | Yarn | `vmtools-mc1.21.6-<ver>.jar` |
| `mc-26.1` | 26.1 | 25 | 9.4.0 | 1.16.3 | Mojang | `vmtools-mc26.1-<ver>.jar` |
| `mc-26.2` | 26.2 | 25 | 9.5.1 | 1.17.7 | Mojang | `vmtools-mc26.2-<ver>.jar` |
| `mc-26.3` | 26.3 | 25 | 9.7.1 | 1.18.4 | Mojang | `vmtools-mc26.3-<ver>.jar` |

## 2. 已安装工具链

| 工具 | 版本 | 路径 |
|------|------|------|
| JDK 21 (Temurin) | 21.0.12.1 | `/opt/jdk/current` |
| JDK 25 (Temurin) | 25.0.4.1 | `/opt/jdk25/current` |
| Gradle | 9.4.0 | `/opt/gradle/gradle-9.4.0` |
| Gradle | 9.5.1 | `/opt/gradle/gradle-9.5.1` |
| Gradle | 9.7.1 | 预置于 `GRADLE_USER_HOME` 的 wrapper dists（`mc-26.3`） |

默认 `java` / `javac` / `gradle` 指向 **JDK 21 + Gradle 9.4.0**
（`/etc/profile.d/vmtools-dev.sh` + `/usr/local/bin` 软链）。

### 切换工具链

```bash
vm-jdk21 ./gradlew build   # main / mc-1.21.6（Java 21）
vm-jdk25 ./gradlew build   # mc-26.1 / mc-26.2（Java 25）
```

## 3. 构建

```bash
cd /root/vmtools-mod
vm-jdk21 ./gradlew build   # MC 1.21.11 / 1.21.6
vm-jdk25 ./gradlew build   # MC 26.1 / 26.2
```

产物：`build/libs/*.jar`

| 命令 | 说明 |
|---|---|
| `./gradlew build` | 完整构建（编译 + remap + 打包） |
| `./gradlew compileClientJava` | 只编译客户端源码（最快的语法检查） |
| `./gradlew runClient` | 启动开发客户端（需图形环境，本容器无 GUI） |
| `./gradlew --stop` | 停止 daemon |

> 首次构建需下载 Minecraft、映射、Fabric API（400MB+），耗时 10–15 分钟；
> 缓存预热后增量构建约 **10 秒**。

## 4. 已验证的构建结果

| 分支 | 命令 | 结果 | 产物 |
|------|------|------|------|
| `main` | `vm-jdk21 ./gradlew build` | ✅ BUILD SUCCESSFUL | `vmtools-mc1.21.11-3.2.0.jar` (124 KB, 41 classes) |
| `mc-26.2` | `vm-jdk25 ./gradlew build` | ✅ BUILD SUCCESSFUL | `vmtools-mc26.2-3.2.0.jar` (118 KB, 41 classes) |
| `mc-26.1` | `vm-jdk25 ./gradlew build` | ✅ BUILD SUCCESSFUL | `vmtools-mc26.1-3.2.0.jar` (118 KB, 41 classes) |
| `mc-26.3` | `vm-jdk25 ./gradlew build` | ✅ BUILD SUCCESSFUL | `vmtools-mc26.3-3.2.0.jar` (118 KB, 41 classes) |
| `mc-1.21.6` | `vm-jdk21 ./gradlew build` | ⚠️ 未完成（见 §7） | — |

## 5. 本环境已修复的上游问题

### `gradlew` 无法执行（4 个分支全部存在）

**症状**

```
Error: Could not find or load main class "-Xmx64m"
Caused by: java.lang.ClassNotFoundException: "-Xmx64m"
```

**原因**

1. `gradlew` 第 9 行 `DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'` —— 内层引号被当作
   字面量传给 JVM，导致 JVM 把 `"-Xmx64m"` 当成主类名。
2. 该文件在 git 中记录为 `100644`，无执行权限，`./gradlew` 直接 `Permission denied`。

**修复**（4 个分支均已本地提交）

```diff
-DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'
+DEFAULT_JVM_OPTS='-Xmx64m -Xms64m'
```

外加 `chmod +x gradlew` 与 `git update-index --chmod=+x gradlew`。

> 这些提交目前**仅在本地**（各分支领先 origin 1 个提交），尚未推送——本环境无 GitHub 凭据。

## 6. 网络与镜像

本容器 `github.com:443` 与 `services.gradle.org` 均不可达，已做如下适配：

| 用途 | 方案 |
|------|------|
| git clone/fetch/push | 全局 `url.<proxy>.insteadOf https://github.com/`，经 `https://ghfast.top/` 代理 |
| Gradle 发行包 | `mirrors.cloud.tencent.com` 下载后预置到 `~/.gradle/wrapper/dists/` |
| JDK | `mirrors.tuna.tsinghua.edu.cn/Adoptium` |
| npm | `NPM_CONFIG_REGISTRY=https://registry.npmmirror.com`（环境自带） |

`origin` 保留规范 URL，代理通过 git 全局配置透明重写：

```bash
git config --global --get-regexp 'url\..*'
# url.https://ghfast.top/https://github.com/.insteadof https://github.com/
```

可达的 Maven 源：`maven.fabricmc.net`、`repo1.maven.org`、`libraries.minecraft.net`、
`piston-meta.mojang.com`、`meta.fabricmc.net`。

### 下载限速经验

- `piston-data.mojang.com`（Minecraft jar）实测 **1.3 MB/s**，很快。
- `maven.fabricmc.net`（Loom、Fabric API）**限速严重**，实测 4 KB/s ~ 200 KB/s 波动；
  大文件建议直接 `curl` 下载后注入 `~/.gradle/caches/fabric-loom/<mcver>/`。
- 卡住时先看 `/root/.gradle/.tmp/gradle_download*` 的大小增长速率判断是否在动。

## 7. 已知限制：`mc-1.21.6` 构建

`mc-1.21.6`（旧分支，v2.1.0）依赖 `fabric-loom 1.9-SNAPSHOT`，其传递依赖需从
限速的 `maven.fabricmc.net` 下载。本环境实测该连接长期处于 4 KB/s 量级，
构建在依赖解析阶段长时间无进展。

已完成的前置工作：

- 手动下载 MC 1.21.6 client(29MB)/server(58MB) jar 并注入
  `~/.gradle/caches/fabric-loom/1.21.6/`，绕过 Mojang 下载。
- 该分支 `gradlew` 缺陷已修复并提交。

**建议**：网络恢复后直接重跑 `vm-jdk21 ./gradlew build`，或先在网络良好环境
预热 `~/.gradle/caches` 再拷贝过来。该分支为已被 `main` 取代的历史版本，
不影响主线开发。

## 8. 推送代码

本环境**没有** GitHub 凭据，推送前需配置：

```bash
# 方式一：PAT
git config --global credential.helper store
printf 'https://<user>:<token>@github.com\n' > ~/.git-credentials

# 方式二：SSH（注意本环境仅 ssh.github.com:443 可达，22 端口不通）
ssh-keygen -t ed25519 -f ~/.ssh/id_ed25519_github -N ""
cat ~/.ssh/id_ed25519_github.pub   # 添加到 GitHub
```

然后 `git push origin <branch>`。

## 9. 项目结构（main 分支）

```
src/client/java/com/venus/vmtools/
├── VMToolsClient.java          # 模组入口
├── config/ModConfig.java       # UI 常量 + 配置
├── feature/waypoint/           # 路径点数据层（CRUD/持久化/传送）
├── feature/escape/             # 虚空逃逸 + 血量监控
├── feature/freeze/             # 移动包冻结（绕过移动检测）
├── gui/                        # 7 个界面 + 3 个组件
├── keybind/KeybindManager.java # 快捷键（默认 M）
├── mixin/LocalPlayerMixin.java
└── util/                       # ChatUtil / FileUtil
```

- 28 个 Java 文件，约 6000 行，纯客户端模组（`environment: "client"`）
- 数据目录：`.minecraft/config/vmtools/*.json`
