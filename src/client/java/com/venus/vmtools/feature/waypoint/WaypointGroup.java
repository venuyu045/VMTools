package com.venus.vmtools.feature.waypoint;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 路径点分组数据模型
 */
public class WaypointGroup {

    private String id;
    private String name;
    private WaypointColor color;
    private boolean expanded;
    private List<Waypoint> waypoints;

    /**
     * 创建新的分组
     */
    public WaypointGroup(String name) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.name = name;
        this.color = WaypointColor.BLUE;
        this.expanded = true;
        this.waypoints = new ArrayList<>();
    }

    /**
     * 添加路径点
     */
    public void addWaypoint(Waypoint waypoint) {
        waypoint.setGroupId(this.id);
        waypoints.add(waypoint);
    }

    /**
     * 移除路径点
     */
    public boolean removeWaypoint(String waypointId) {
        return waypoints.removeIf(wp -> wp.getId().equals(waypointId));
    }

    /**
     * 查找路径点
     */
    public Waypoint findWaypoint(String waypointId) {
        return waypoints.stream()
                .filter(wp -> wp.getId().equals(waypointId))
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取路径点数量
     */
    public int getWaypointCount() {
        return waypoints.size();
    }

    /**
     * 切换展开/折叠状态
     */
    public void toggleExpanded() {
        this.expanded = !this.expanded;
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

    public WaypointColor getColor() {
        return color;
    }

    public void setColor(WaypointColor color) {
        this.color = color;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public List<Waypoint> getWaypoints() {
        return waypoints;
    }

    public void setWaypoints(List<Waypoint> waypoints) {
        this.waypoints = waypoints;
    }
}
