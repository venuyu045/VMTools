package com.venus.vmtools.feature.waypoint;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import com.venus.vmtools.VMToolsClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 路径点数据导入导出工具
 */
public class WaypointIO {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                    context.serialize(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                    LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .create();

    /**
     * 导出数据结构
     */
    public static class ExportData {
        public String version = "1.0";
        public String exportDate;
        public String modVersion = "1.0.0";
        public List<GroupData> groups = new ArrayList<>();

        public ExportData() {
            this.exportDate = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
    }

    public static class GroupData {
        public String name;
        public String color;
        public List<WaypointData> waypoints = new ArrayList<>();
    }

    public static class WaypointData {
        public String name;
        public String command;
        public String color;
        public String createdAt;
    }

    /**
     * 导出所有分组到 JSON 文件
     */
    public static boolean exportToFile(Path filePath, List<WaypointGroup> groups) {
        try {
            ExportData data = convertToExportData(groups);
            String json = GSON.toJson(data);
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, json);
            VMToolsClient.LOGGER.info("成功导出路径点到: {}", filePath);
            return true;
        } catch (IOException e) {
            VMToolsClient.LOGGER.error("导出路径点失败", e);
            return false;
        }
    }

    /**
     * 从 JSON 文件导入路径点
     */
    public static List<WaypointGroup> importFromFile(Path filePath) {
        try {
            if (!Files.exists(filePath)) {
                VMToolsClient.LOGGER.warn("导入文件不存在: {}", filePath);
                return null;
            }
            String json = Files.readString(filePath);
            ExportData data = GSON.fromJson(json, ExportData.class);
            List<WaypointGroup> groups = convertFromExportData(data);
            VMToolsClient.LOGGER.info("成功导入 {} 个分组", groups.size());
            return groups;
        } catch (IOException e) {
            VMToolsClient.LOGGER.error("导入路径点失败", e);
            return null;
        }
    }

    /**
     * 将分组列表转换为导出数据
     */
    private static ExportData convertToExportData(List<WaypointGroup> groups) {
        ExportData data = new ExportData();
        for (WaypointGroup group : groups) {
            GroupData groupData = new GroupData();
            groupData.name = group.getName();
            groupData.color = group.getColor().name();
            for (Waypoint wp : group.getWaypoints()) {
                WaypointData wpData = new WaypointData();
                wpData.name = wp.getName();
                wpData.command = wp.getCommand();
                wpData.color = wp.getColor().name();
                wpData.createdAt = wp.getCreatedAt() != null ?
                        wp.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
                groupData.waypoints.add(wpData);
            }
            data.groups.add(groupData);
        }
        return data;
    }

    /**
     * 将导出数据转换为分组列表
     */
    private static List<WaypointGroup> convertFromExportData(ExportData data) {
        List<WaypointGroup> groups = new ArrayList<>();
        if (data == null || data.groups == null) {
            return groups;
        }
        for (GroupData groupData : data.groups) {
            WaypointGroup group = new WaypointGroup(groupData.name);
            if (groupData.color != null) {
                group.setColor(WaypointColor.fromName(groupData.color));
            }
            if (groupData.waypoints != null) {
                for (WaypointData wpData : groupData.waypoints) {
                    Waypoint wp = new Waypoint(wpData.name, wpData.command, group.getId());
                    if (wpData.color != null) {
                        wp.setColor(WaypointColor.fromName(wpData.color));
                    }
                    group.addWaypoint(wp);
                }
            }
            groups.add(group);
        }
        return groups;
    }
}
