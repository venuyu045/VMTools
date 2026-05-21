package com.venus.vmtools.feature.waypoint;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 路径点数据模型
 */
public class Waypoint {

    private String id;
    private String name;
    private String command;
    private WaypointColor color;
    private String groupId;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
    private int useCount;

    /**
     * 创建新的路径点
     */
    public Waypoint(String name, String command, String groupId) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.name = name;
        this.command = command;
        this.color = WaypointColor.GREEN;
        this.groupId = groupId;
        this.createdAt = LocalDateTime.now();
        this.lastUsedAt = null;
        this.useCount = 0;
    }

    /**
     * 记录使用
     */
    public void recordUse() {
        this.lastUsedAt = LocalDateTime.now();
        this.useCount++;
    }

    /**
     * 获取显示用的命令（截断过长的命令）
     */
    public String getDisplayCommand() {
        if (command.length() > 30) {
            return command.substring(0, 27) + "...";
        }
        return command;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public WaypointColor getColor() {
        return color;
    }

    public void setColor(WaypointColor color) {
        this.color = color;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public int getUseCount() {
        return useCount;
    }
}
