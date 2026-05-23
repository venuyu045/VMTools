package com.venus.vmtools.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.venus.vmtools.VMToolsClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 模组配置管理
 */
public class ModConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("vmtools.json");

    // ==================== UI 常量 ====================
    public static class UI {
        // 面板尺寸
        public static final int PANEL_WIDTH = 220;
        public static final int PANEL_HEIGHT = 180;
        public static final int BUTTON_HEIGHT = 20;
        public static final int PADDING = 8;

        // 颜色定义
        public static final int BG_COLOR = 0xFF1E1E2E;
        public static final int PANEL_COLOR = 0xFF2D2D44;
        public static final int HEADER_COLOR = 0xFF3D3D5C;
        public static final int TEXT_COLOR = 0xFFE0E0E0;
        public static final int ACCENT_COLOR = 0xFF7C3AED;
        public static final int HOVER_COLOR = 0xFF4A4A6A;
        public static final int SUCCESS_COLOR = 0xFF4ADE80;
        public static final int DANGER_COLOR = 0xFFF87171;
        public static final int BORDER_COLOR = 0xFF4A4A6A;
        public static final int MUTED_COLOR = 0xFF888888;

        // Toast 通知
        public static final int TOAST_WIDTH = 200;
        public static final int TOAST_HEIGHT = 30;
        public static final int TOAST_MARGIN = 10;
        public static final long TOAST_DURATION = 3000;
        public static final int TOAST_FADE_DURATION = 500;
    }

    // ==================== 配置项 ====================

    // 传送前是否显示确认提示
    private boolean confirmBeforeTeleport = false;

    // UI 缩放比例 (0.5 - 2.0)
    private float uiScale = 1.0f;

    // 界面打开动画时长 (毫秒)
    private int animationDuration = 200;

    // 是否显示路径点图标颜色
    private boolean showWaypointColors = true;

    // 是否显示使用次数
    private boolean showUseCount = true;

    // 自动确认传送（传送后自动执行确认命令）
    private boolean autoConfirmTeleport = false;

    // 确认传送命令（如 "res tpconfirm"）
    private String confirmCommand = "res tpconfirm";

    // 确认传送延迟（毫秒）
    private int confirmDelay = 200;

    /**
     * 从文件加载配置
     */
    public static ModConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                ModConfig config = GSON.fromJson(json, ModConfig.class);
                VMToolsClient.LOGGER.info("配置文件已加载");
                return config;
            } catch (IOException e) {
                VMToolsClient.LOGGER.error("加载配置文件失败", e);
            }
        }
        return new ModConfig();
    }

    /**
     * 保存配置到文件
     */
    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
        } catch (IOException e) {
            VMToolsClient.LOGGER.error("保存配置文件失败", e);
        }
    }

    // Getters and Setters

    public boolean isConfirmBeforeTeleport() {
        return confirmBeforeTeleport;
    }

    public void setConfirmBeforeTeleport(boolean confirmBeforeTeleport) {
        this.confirmBeforeTeleport = confirmBeforeTeleport;
    }

    public float getUiScale() {
        return uiScale;
    }

    public void setUiScale(float uiScale) {
        this.uiScale = Math.max(0.5f, Math.min(2.0f, uiScale));
    }

    public int getAnimationDuration() {
        return animationDuration;
    }

    public void setAnimationDuration(int animationDuration) {
        this.animationDuration = Math.max(0, Math.min(1000, animationDuration));
    }

    public boolean isShowWaypointColors() {
        return showWaypointColors;
    }

    public void setShowWaypointColors(boolean showWaypointColors) {
        this.showWaypointColors = showWaypointColors;
    }

    public boolean isShowUseCount() {
        return showUseCount;
    }

    public void setShowUseCount(boolean showUseCount) {
        this.showUseCount = showUseCount;
    }

    public boolean isAutoConfirmTeleport() {
        return autoConfirmTeleport;
    }

    public void setAutoConfirmTeleport(boolean autoConfirmTeleport) {
        this.autoConfirmTeleport = autoConfirmTeleport;
    }

    public String getConfirmCommand() {
        return confirmCommand;
    }

    public void setConfirmCommand(String confirmCommand) {
        this.confirmCommand = confirmCommand;
    }

    public int getConfirmDelay() {
        return confirmDelay;
    }

    public void setConfirmDelay(int confirmDelay) {
        this.confirmDelay = Math.max(100, Math.min(2000, confirmDelay));
    }
}
