package com.venus.vmtools.feature.waypoint;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.venus.vmtools.VMToolsClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 路径点管理器 - 负责路径点的增删改查和持久化
 */
public class WaypointManager {

    private static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final String DATA_DIR = "vmtools";
    private static final String DATA_FILE = "waypoints.json";
    private static final String BACKUP_FILE = "waypoints.backup.json";
    private static final String VERSION_KEY = "dataVersion";
    private static final int CURRENT_DATA_VERSION = 1;

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                            new JsonPrimitive(src.format(DT_FORMATTER)))
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                            LocalDateTime.parse(json.getAsString(), DT_FORMATTER))
            .create();

    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve(DATA_DIR);
    private static final Path DATA_PATH = CONFIG_DIR.resolve(DATA_FILE);
    private static final Path BACKUP_PATH = CONFIG_DIR.resolve(BACKUP_FILE);

    private List<WaypointGroup> groups;

    public WaypointManager() {
        this.groups = new ArrayList<>();
        load();
    }

    /**
     * 从文件加载数据（带备份恢复）
     */
    public void load() {
        // 确保目录存在
        try {
            Files.createDirectories(CONFIG_DIR);
        } catch (IOException e) {
            VMToolsClient.LOGGER.error("创建配置目录失败", e);
        }

        // 尝试加载主文件
        if (loadFromFile(DATA_PATH)) {
            VMToolsClient.LOGGER.info("从主文件加载了 {} 个分组", groups.size());
            return;
        }

        // 主文件失败，尝试加载备份
        if (loadFromFile(BACKUP_PATH)) {
            VMToolsClient.LOGGER.warn("主文件损坏，从备份恢复了 {} 个分组", groups.size());
            // 恢复后立即保存到主文件
            save();
            return;
        }

        // 都失败，创建默认分组
        VMToolsClient.LOGGER.info("未找到数据文件，创建默认分组");
        createDefaultGroups();
        save();
    }

    /**
     * 从指定文件加载数据
     */
    private boolean loadFromFile(Path path) {
        if (!Files.exists(path)) {
            return false;
        }

        try {
            String json = Files.readString(path);

            // 尝试解析为 Wrapper 对象（新格式）
            try {
                DataWrapper wrapper = GSON.fromJson(json, DataWrapper.class);
                if (wrapper != null && wrapper.groups != null) {
                    this.groups = wrapper.groups;
                    return true;
                }
            } catch (Exception ignored) {}

            // 兼容旧格式（直接是数组）
            try {
                Type listType = new TypeToken<List<WaypointGroup>>(){}.getType();
                List<WaypointGroup> loaded = GSON.fromJson(json, listType);
                if (loaded != null && !loaded.isEmpty()) {
                    this.groups = loaded;
                    return true;
                }
            } catch (Exception ignored) {}

            return false;
        } catch (IOException e) {
            VMToolsClient.LOGGER.error("读取文件失败: {}", path, e);
            return false;
        }
    }

    /**
     * 保存数据到文件（带备份）
     */
    public void save() {
        try {
            Files.createDirectories(CONFIG_DIR);

            // 如果主文件存在，先备份
            if (Files.exists(DATA_PATH)) {
                try {
                    Files.copy(DATA_PATH, BACKUP_PATH, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    VMToolsClient.LOGGER.warn("备份文件创建失败", e);
                }
            }

            // 保存新数据
            DataWrapper wrapper = new DataWrapper();
            wrapper.dataVersion = CURRENT_DATA_VERSION;
            wrapper.groups = this.groups;
            wrapper.lastSaved = LocalDateTime.now().format(DT_FORMATTER);

            Files.writeString(DATA_PATH, GSON.toJson(wrapper));
            VMToolsClient.LOGGER.info("已保存 {} 个分组", groups.size());
        } catch (IOException e) {
            VMToolsClient.LOGGER.error("保存路径点数据失败", e);
        }
    }

    /**
     * 数据包装类（带版本信息）
     */
    private static class DataWrapper {
        int dataVersion = CURRENT_DATA_VERSION;
        String lastSaved;
        List<WaypointGroup> groups;
    }

    /**
     * 创建默认分组
     */
    private void createDefaultGroups() {
        WaypointGroup defaultGroup = new WaypointGroup("常用地点");
        defaultGroup.setColor(WaypointColor.BLUE);
        groups.add(defaultGroup);

        WaypointGroup mineGroup = new WaypointGroup("矿洞");
        mineGroup.setColor(WaypointColor.GRAY);
        groups.add(mineGroup);

        WaypointGroup villageGroup = new WaypointGroup("村庄");
        villageGroup.setColor(WaypointColor.GREEN);
        groups.add(villageGroup);

        save();
    }

    // ==================== 分组操作 ====================

    /**
     * 添加分组
     */
    public void addGroup(WaypointGroup group) {
        groups.add(group);
        save();
    }

    /**
     * 删除分组
     */
    public boolean removeGroup(String groupId) {
        boolean removed = groups.removeIf(g -> g.getId().equals(groupId));
        if (removed) {
            save();
        }
        return removed;
    }

    /**
     * 查找分组
     */
    public WaypointGroup findGroup(String groupId) {
        return groups.stream()
                .filter(g -> g.getId().equals(groupId))
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取所有分组
     */
    public List<WaypointGroup> getGroups() {
        return groups;
    }

    /**
     * 获取非空分组
     */
    public List<WaypointGroup> getNonEmptyGroups() {
        return groups.stream()
                .filter(g -> g.getWaypointCount() > 0)
                .collect(Collectors.toList());
    }

    // ==================== 路径点操作 ====================

    /**
     * 添加路径点到指定分组
     */
    public boolean addWaypoint(String groupId, Waypoint waypoint) {
        WaypointGroup group = findGroup(groupId);
        if (group != null) {
            group.addWaypoint(waypoint);
            save();
            return true;
        }
        return false;
    }

    /**
     * 删除路径点
     */
    public boolean removeWaypoint(String groupId, String waypointId) {
        WaypointGroup group = findGroup(groupId);
        if (group != null) {
            boolean removed = group.removeWaypoint(waypointId);
            if (removed) {
                save();
            }
            return removed;
        }
        return false;
    }

    /**
     * 查找路径点（在所有分组中搜索）
     */
    public Waypoint findWaypoint(String waypointId) {
        for (WaypointGroup group : groups) {
            Waypoint wp = group.findWaypoint(waypointId);
            if (wp != null) {
                return wp;
            }
        }
        return null;
    }

    /**
     * 获取路径点所在的分组
     */
    public WaypointGroup findWaypointGroup(String waypointId) {
        for (WaypointGroup group : groups) {
            if (group.findWaypoint(waypointId) != null) {
                return group;
            }
        }
        return null;
    }

    /**
     * 执行传送命令
     */
    public boolean executeTeleport(Waypoint waypoint) {
        boolean success = TeleportService.sendCommand(waypoint.getCommand());
        if (success) {
            // 记录使用
            waypoint.recordUse();
            save();
        }
        return success;
    }

    /**
     * 搜索路径点
     */
    public List<Waypoint> searchWaypoints(String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        List<Waypoint> results = new ArrayList<>();
        for (WaypointGroup group : groups) {
            for (Waypoint wp : group.getWaypoints()) {
                if (wp.getName().toLowerCase().contains(lowerKeyword) ||
                    wp.getCommand().toLowerCase().contains(lowerKeyword)) {
                    results.add(wp);
                }
            }
        }
        return results;
    }

    /**
     * 导入路径点（合并模式）
     */
    public int importWaypoints(List<WaypointGroup> importedGroups) {
        int count = 0;
        for (WaypointGroup importedGroup : importedGroups) {
            // 查找同名分组，或创建新分组
            WaypointGroup existingGroup = groups.stream()
                    .filter(g -> g.getName().equals(importedGroup.getName()))
                    .findFirst()
                    .orElse(null);

            if (existingGroup == null) {
                existingGroup = new WaypointGroup(importedGroup.getName());
                existingGroup.setColor(importedGroup.getColor());
                groups.add(existingGroup);
            }

            for (Waypoint wp : importedGroup.getWaypoints()) {
                wp.setGroupId(existingGroup.getId());
                existingGroup.addWaypoint(wp);
                count++;
            }
        }
        save();
        return count;
    }

    /**
     * 获取总路径点数
     */
    public int getTotalWaypointCount() {
        return groups.stream()
                .mapToInt(WaypointGroup::getWaypointCount)
                .sum();
    }
}
