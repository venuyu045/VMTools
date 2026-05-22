package com.venus.vmtools.keybind;

import com.venus.vmtools.VMToolsClient;
import com.venus.vmtools.gui.WaypointScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * 快捷键管理器 - 负责注册和管理所有工具的快捷键
 */
public class KeybindManager {

    // 路径点管理界面快捷键（默认 M 键）
    private KeyMapping waypointKey;

    // 快捷传送快捷键（默认 Ctrl+T）
    private KeyMapping quickTeleportKey;

    /**
     * 注册所有快捷键
     */
    public void register() {
        // 创建快捷键分类
        KeyMapping.Category vmtoolsCategory = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("vmtools", "main"));

        // 注册路径点管理快捷键
        waypointKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.vmtools.waypoints",           // 翻译键
                InputConstants.Type.KEYSYM,              // 键类型
                GLFW.GLFW_KEY_M,                    // 默认键 M
                vmtoolsCategory                     // 分类
        ));

        // 注册快捷传送快捷键
        quickTeleportKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.vmtools.quick_teleport",       // 翻译键
                InputConstants.Type.KEYSYM,              // 键类型
                GLFW.GLFW_KEY_T,                    // 默认键 T
                vmtoolsCategory                     // 分类
        ));

        // 注册 Tick 事件，检测快捷键按下
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);

        VMToolsClient.LOGGER.info("快捷键注册完成");
    }

    /**
     * 客户端 Tick 事件处理
     */
    private void onClientTick(Minecraft client) {
        // 检测路径点快捷键
        while (waypointKey.consumeClick()) {
            if (client.screen == null) {
                client.setScreen(new WaypointScreen());
            }
        }

        // 检测快捷传送快捷键（需要 Ctrl 修饰）
        while (quickTeleportKey.consumeClick()) {
            // 检查是否按住了 Ctrl
            long window = client.getWindow().handle();
            boolean ctrlPressed = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS ||
                                  GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
            if (ctrlPressed && client.screen == null) {
                // TODO: 打开快捷传送界面（后续实现）
                VMToolsClient.LOGGER.info("快捷传送功能待实现");
            }
        }
    }

    /**
     * 获取路径点快捷键
     */
    public KeyMapping getWaypointKey() {
        return waypointKey;
    }

    /**
     * 获取快捷传送快捷键
     */
    public KeyMapping getQuickTeleportKey() {
        return quickTeleportKey;
    }

    /**
     * 获取快捷键的显示名称
     */
    public String getWaypointKeyName() {
        return waypointKey.getTranslatedKeyMessage().getString();
    }

    public String getQuickTeleportKeyName() {
        return quickTeleportKey.getTranslatedKeyMessage().getString();
    }
}
