package com.venus.vmtools.feature.escape;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 自动逃脱管理器 - 核心逻辑类
 * 监控玩家Y坐标，当低于阈值时触发逃逸命令
 */
public class AutoEscapeManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("VMTools/AutoEscape");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int MAX_LOG_ENTRIES = 20;
    
    private static AutoEscapeManager instance;
    
    private final AutoEscapeConfig config;
    private boolean triggered = false;
    private final List<String> logEntries = new ArrayList<>();
    
    private AutoEscapeManager() {
        this.config = AutoEscapeConfig.load();
    }
    
    /**
     * 获取单例实例
     */
    public static AutoEscapeManager getInstance() {
        if (instance == null) {
            instance = new AutoEscapeManager();
        }
        return instance;
    }
    
    /**
     * 注册tick事件监听器
     */
    public void register() {
        LOGGER.info("注册自动逃脱管理器");
        
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }
    
    /**
     * 客户端tick回调 - 每tick检查玩家位置
     */
    private void onClientTick(MinecraftClient client) {
        if (!config.isEnabled()) {
            return;
        }
        
        if (client.player == null) {
            return;
        }
        
        double playerY = client.player.getY();
        
        // 检查是否需要触发逃逸
        if (playerY < config.getThreshold() && !triggered) {
            triggered = true;
            executeEscapeCommand(client, playerY);
        }
        
        // 检查是否需要重置触发状态（玩家回到安全高度）
        if (playerY >= config.getThreshold() && triggered) {
            triggered = false;
            LOGGER.debug("玩家Y坐标已恢复到安全高度，重置触发状态");
        }
    }
    
    /**
     * 执行逃逸命令
     */
    private void executeEscapeCommand(MinecraftClient client, double playerY) {
        String command = config.getCommand();
        if (command == null || command.isEmpty()) {
            LOGGER.warn("逃逸命令为空，跳过执行");
            return;
        }
        
        // 移除命令开头的斜杠
        String actualCommand = command.startsWith("/") ? command.substring(1) : command;
        
        // 发送命令
        try {
            client.getNetworkHandler().sendChatCommand(actualCommand);
            LOGGER.info("执行逃逸命令: {}", actualCommand);
        } catch (Exception e) {
            LOGGER.error("发送逃逸命令失败: {}", actualCommand, e);
            return;
        }
        
        // 显示action bar消息
        String message = String.format("[VMTools] 自动逃逸已触发: Y=%.1f", playerY);
        client.player.sendMessage(Text.literal(message), true);
        
        // 记录日志
        addLogEntry(playerY, actualCommand);
        
        // 如果启用自动确认，延迟发送确认命令
        if (config.isAutoConfirm()) {
            scheduleConfirmCommand(client);
        }
    }
    
    /**
     * 调度确认命令
     */
    private void scheduleConfirmCommand(MinecraftClient client) {
        String confirmCommand = config.getConfirmCommand();
        if (confirmCommand == null || confirmCommand.isEmpty()) {
            LOGGER.warn("确认命令为空，跳过自动确认");
            return;
        }
        
        int delay = config.getConfirmDelay();
        LOGGER.debug("调度确认命令: {} (延迟{}ms)", confirmCommand, delay);
        
        // 在新线程中延迟执行
        Thread confirmThread = new Thread(() -> {
            try {
                Thread.sleep(delay);
                client.execute(() -> {
                    try {
                        String actualConfirm = confirmCommand.startsWith("/") 
                                ? confirmCommand.substring(1) 
                                : confirmCommand;
                        client.getNetworkHandler().sendChatCommand(actualConfirm);
                        LOGGER.info("执行确认命令: {}", actualConfirm);
                    } catch (Exception e) {
                        LOGGER.error("发送确认命令失败: {}", confirmCommand, e);
                    }
                });
            } catch (InterruptedException e) {
                LOGGER.warn("确认命令线程被中断", e);
                Thread.currentThread().interrupt();
            }
        }, "VMTools-AutoEscape-Confirm");
        confirmThread.setDaemon(true);
        confirmThread.start();
    }
    
    /**
     * 添加日志条目
     */
    private void addLogEntry(double playerY, String command) {
        String timestamp = LocalTime.now().format(TIME_FORMATTER);
        String entry = String.format("[%s] Y=%.1f → 执行 %s", timestamp, playerY, command);
        
        logEntries.add(entry);
        
        // 保持日志条目数量在最大限制内
        while (logEntries.size() > MAX_LOG_ENTRIES) {
            logEntries.remove(0);
        }
        
        LOGGER.debug("添加日志条目: {}", entry);
    }
    
    /**
     * 切换启用状态
     */
    public void toggle() {
        boolean newState = !config.isEnabled();
        config.setEnabled(newState);
        config.save();
        LOGGER.info("自动逃脱功能已{}", newState ? "启用" : "禁用");
        
        // 如果禁用功能，重置触发状态
        if (!newState) {
            triggered = false;
        }
    }
    
    /**
     * 设置Y坐标阈值
     */
    public void setThreshold(double threshold) {
        config.setThreshold(threshold);
        config.save();
        LOGGER.info("设置逃逸阈值: {}", threshold);
    }
    
    /**
     * 设置逃逸命令
     */
    public void setCommand(String command) {
        config.setCommand(command);
        config.save();
        LOGGER.info("设置逃逸命令: {}", command);
    }
    
    /**
     * 设置自动确认状态
     */
    public void setAutoConfirm(boolean autoConfirm) {
        config.setAutoConfirm(autoConfirm);
        config.save();
        LOGGER.info("自动确认已{}", autoConfirm ? "启用" : "禁用");
    }
    
    /**
     * 设置确认命令
     */
    public void setConfirmCommand(String confirmCommand) {
        config.setConfirmCommand(confirmCommand);
        config.save();
        LOGGER.info("设置确认命令: {}", confirmCommand);
    }
    
    /**
     * 设置确认延迟
     */
    public void setConfirmDelay(int confirmDelay) {
        config.setConfirmDelay(confirmDelay);
        config.save();
        LOGGER.info("设置确认延迟: {}ms", confirmDelay);
    }
    
    /**
     * 获取配置
     */
    public AutoEscapeConfig getConfig() {
        return config;
    }
    
    /**
     * 获取触发状态
     */
    public boolean isTriggered() {
        return triggered;
    }
    
    /**
     * 获取日志条目（不可修改视图）
     */
    public List<String> getLogEntries() {
        return Collections.unmodifiableList(logEntries);
    }
    
    /**
     * 重置触发状态（用于玩家断开连接或世界切换）
     */
    public void resetTriggerState() {
        triggered = false;
        LOGGER.debug("重置触发状态");
    }
}
