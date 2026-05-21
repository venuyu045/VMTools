package com.venus.vmtools.feature.waypoint;

/**
 * 路径点颜色枚举
 */
public enum WaypointColor {

    RED("红色", 0xFF5555, "🔴"),
    ORANGE("橙色", 0xFFAA00, "🟠"),
    YELLOW("黄色", 0xFFFF55, "🟡"),
    GREEN("绿色", 0x55FF55, "🟢"),
    CYAN("青色", 0x55FFFF, "🔵"),
    BLUE("蓝色", 0x5555FF, "🔵"),
    PURPLE("紫色", 0xFF55FF, "🟣"),
    WHITE("白色", 0xFFFFFF, "⚪"),
    GRAY("灰色", 0xAAAAAA, "⚫");

    private final String displayName;
    private final int colorValue;
    private final String emoji;

    WaypointColor(String displayName, int colorValue, String emoji) {
        this.displayName = displayName;
        this.colorValue = colorValue;
        this.emoji = emoji;
    }

    /**
     * 获取 ARGB 格式的颜色值（完全不透明）
     */
    public int getARGB() {
        return 0xFF000000 | colorValue;
    }

    /**
     * 获取带透明度的 ARGB 颜色值
     */
    public int getARGB(int alpha) {
        return ((alpha & 0xFF) << 24) | colorValue;
    }

    /**
     * 获取颜色的 RGB 分量
     */
    public float getRed() {
        return ((colorValue >> 16) & 0xFF) / 255.0f;
    }

    public float getGreen() {
        return ((colorValue >> 8) & 0xFF) / 255.0f;
    }

    public float getBlue() {
        return (colorValue & 0xFF) / 255.0f;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getColorValue() {
        return colorValue;
    }

    public String getEmoji() {
        return emoji;
    }

    /**
     * 根据名称查找颜色
     */
    public static WaypointColor fromName(String name) {
        for (WaypointColor color : values()) {
            if (color.name().equalsIgnoreCase(name) || color.displayName.equals(name)) {
                return color;
            }
        }
        return GREEN; // 默认绿色
    }
}
