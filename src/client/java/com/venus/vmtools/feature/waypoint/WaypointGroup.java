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
        getWaypoints().add(waypoint);
    }

    /**
     * 移除路径点
     */
    public boolean removeWaypoint(String waypointId) {
        return getWaypoints().removeIf(wp -> wp.getId().equals(waypointId));
    }

    /**
     * 移动路径点位置（拖拽排序）
     */
    public void moveWaypoint(int fromIndex, int toIndex) {
        List<Waypoint> list = getWaypoints();
        if (fromIndex < 0 || fromIndex >= list.size() ||
            toIndex < 0 || toIndex > list.size() ||
            fromIndex == toIndex) return;
        Waypoint wp = list.remove(fromIndex);
        int insertIndex = toIndex > fromIndex ? toIndex - 1 : toIndex;
        list.add(insertIndex, wp);
    }

    /**
     * 查找路径点
     */
    public Waypoint findWaypoint(String waypointId) {
        return getWaypoints().stream()
                .filter(wp -> wp.getId().equals(waypointId))
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取路径点数量
     */
    public int getWaypointCount() {
        return getWaypoints().size();
    }

    /**
     * 切换展开/折叠状态
     */
    public void toggleExpanded() {
        this.expanded = !this.expanded;
    }

    // Getters and Setters

    public String getId() {
        // Gson 反序列化不调用构造函数，损坏/手改数据中 id 可能为 null；
        // 调用方普遍使用 getId().equals(...)，此处惰性补全避免 NPE
        if (id == null) {
            id = UUID.randomUUID().toString().substring(0, 8);
        }
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
        // Gson 反序列化不会调用构造函数，旧数据/损坏数据中 waypoints 可能为 null，
        // 这里做惰性修复，避免渲染与保存路径 NPE
        if (waypoints == null) {
            waypoints = new ArrayList<>();
        }
        return waypoints;
    }

    public void setWaypoints(List<Waypoint> waypoints) {
        this.waypoints = waypoints != null ? waypoints : new ArrayList<>();
    }
}
