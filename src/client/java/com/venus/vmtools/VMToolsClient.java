package com.venus.vmtools;

import com.venus.vmtools.config.ModConfig;
import com.venus.vmtools.feature.waypoint.WaypointManager;
import com.venus.vmtools.keybind.KeybindManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Venus Mc Tools - 客户端模组入口
 * 一个集成各种小工具的 Minecraft 模组
 */
public class VMToolsClient implements ClientModInitializer {

    public static final String MOD_ID = "vmtools";
    public static final String MOD_NAME = "Venus Mc Tools";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static VMToolsClient instance;
    private ModConfig config;
    private WaypointManager waypointManager;
    private KeybindManager keybindManager;

    @Override
    public void onInitializeClient() {
        instance = this;
        LOGGER.info("[{}] 正在初始化...", MOD_NAME);

        // 初始化配置
        config = ModConfig.load();

        // 初始化路径点管理器
        waypointManager = new WaypointManager();

        // 初始化快捷键管理器
        keybindManager = new KeybindManager();
        keybindManager.register();

        // 注册关闭事件，保存数据
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            saveAll();
        });

        LOGGER.info("[{}] 初始化完成！", MOD_NAME);
    }

    /**
     * 保存所有数据
     */
    public void saveAll() {
        LOGGER.info("[{}] 正在保存数据...", MOD_NAME);
        config.save();
        waypointManager.save();
        LOGGER.info("[{}] 数据保存完成", MOD_NAME);
    }

    /**
     * 获取模组配置
     */
    public ModConfig getConfig() {
        return config;
    }

    /**
     * 获取路径点管理器
     */
    public WaypointManager getWaypointManager() {
        return waypointManager;
    }

    /**
     * 获取快捷键管理器
     */
    public KeybindManager getKeybindManager() {
        return keybindManager;
    }

    /**
     * 获取模组实例
     */
    public static VMToolsClient getInstance() {
        return instance;
    }
}
