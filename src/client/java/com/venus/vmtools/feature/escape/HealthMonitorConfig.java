package com.venus.vmtools.feature.escape;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 血量监控功能配置
 */
public class HealthMonitorConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("VMTools/HealthMonitor");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir()
            .resolve("vmtools/health_monitor.json");

    // ==================== 配置项 ====================

    // 是否启血量监控功能
    private boolean enabled = false;

    // 血量阈值（最大20.0，半颗心=10.0）
    private double threshold = 10.0;

    // 触发后执行的命令
    private String command = "/home";

    // 是否自动确认（发送命令后自动执行确认命令）
    private boolean autoConfirm = true;

    // 确认命令
    private String confirmCommand = "res tpconfirm";

    // 确认命令延迟（毫秒）
    private int confirmDelay = 200;

    /**
     * 从文件加载配置
     */
    public static HealthMonitorConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                HealthMonitorConfig config = GSON.fromJson(json, HealthMonitorConfig.class);
                LOGGER.info("血量监控配置文件已加载");
                return config;
            } catch (IOException e) {
                LOGGER.warn("加载血量监控配置文件失败，使用默认配置", e);
            }
        }
        return new HealthMonitorConfig();
    }

    /**
     * 保存配置到文件
     */
    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
        } catch (IOException e) {
            LOGGER.warn("保存血量监控配置文件失败", e);
        }
    }

    // Getters and Setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public boolean isAutoConfirm() {
        return autoConfirm;
    }

    public void setAutoConfirm(boolean autoConfirm) {
        this.autoConfirm = autoConfirm;
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
        this.confirmDelay = confirmDelay;
    }
}
