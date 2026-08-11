# Venus Mc Tools (VMTools)

轻量实用的 Minecraft Fabric 客户端模组，核心功能：路径点管理与快速传送、传送移动检测绕过、逃逸小工具。

> 支持 Minecraft 1.21.11、26.1、26.2 三版本

## 功能特性

### 路径点管理
- **M 键打开**管理界面，分组窗口式布局，可拖拽/缩放/折叠
- **左键点击路径点直接传送**，右键弹出编辑/复制/删除菜单
- **拖拽排序**（⋮⋮ 手柄），**搜索过滤**（实时匹配名称和命令）
- **9 色标记**，**导入/导出** JSON，**Toast 通知**

### 绕过移动检测 v3.1 新增
- 对 `/res tp` 和 `/home` 路径点传送时自动冻结移动包 3 秒
- 服务端看不到位置变化 → 延迟传送插件的 `PlayerMoveEvent` 不会触发 → 传送不被取消
- 设置页面可开关

### Back 返回
- 点击底部 **Back** 按钮 → `/back` → 回到上一个位置

### 自动确认传送
- 全局开关 + 每个路径点独立开关
- 传送后延迟自动发送确认命令（如 `/res tpconfirm`）

### 逃逸小工具
- **虚空逃逸**：Y 坐标低于阈值自动执行命令（默认 /ehomes home）
- **血量监控**：血量低于阈值自动执行命令（默认 /home）
- 各自独立配置、触发日志、可拖动窗口

### 命令补全
- 编辑路径点时命令输入框自动建议（res tp / warp / home / tpa / ehome）

## 安装

1. 安装 [Fabric Loader](https://fabricmc.net/)
2. 下载 [Fabric API](https://modrinth.com/mod/fabric-api) 放入 `mods`
3. 下载对应版本的 VMTools JAR 放入 `mods`
4. 启动游戏

### 版本选择

| MC 版本 | 文件 | Java | Fabric Loader | Fabric API |
|---------|------|------|--------------|------------|
| 1.21.11 | `vmtools-mc1.21.11-3.1.0.jar` | 21+ | 0.16.14+ | 0.141.4+ |
| 26.1 | `vmtools-mc26.1-3.1.1.jar` | 25+ | 0.19.2+ | 0.145.1+ |
| 26.2 | `vmtools-mc26.2-3.1.0.jar` | 25+ | 0.19.3+ | 0.153.0+ |

## 快捷键

| 按键 | 功能 |
|------|------|
| `M` | 打开路径点管理界面 |

可在游戏设置 → 控制 → VMTools 中修改。

## 使用方法

### 路径点传送
1. 按 `M` 打开界面
2. 点「+ 路径点」或「+ 分组」创建
3. 左键点路径点名称 → 传送（自动冻结 + Back 保存）
4. 右键路径点 → 编辑/复制/删除

### 设置
点击底部「设置」按钮：
- **自动确认传送**：开启后可配置确认命令和延迟
- **绕过移动检测**：传送时冻结移动包

## 命令示例

| 插件 | 格式 | 示例 |
|------|------|------|
| Residence | `/res tp <名称>` | `/res tp main_city` |
| EssentialsX | `/home <名称>` | `/home base` |
| MyWarp | `/warp <名称>` | `/warp shop` |
| eHomes | `/ehomes home` | `/ehomes home` |

## 数据文件

```
.minecraft/config/vmtools/
├── waypoints.json
├── waypoints.backup.json
├── ui_state.json
├── auto_escape.json
├── health_monitor.json
├── tool_windows.json
└── vmtools-export-*.json
```

## 技术信息

| | 1.21.11 | 26.1 | 26.2 |
|--|---------|------|------|
| Mappings | Yarn | Mojang | Mojang |
| Loom | 1.16.2 | 1.16.3 | 1.17.7 |
| Mixin | 0.8.7 | 0.8.7 | 0.8.7 |

## 许可证

MIT License

## 链接

- [GitHub](https://github.com/venuyu045/VMTools)
- [Releases](https://github.com/venuyu045/VMTools/releases)
