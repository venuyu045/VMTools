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

    /**
     * 注册所有快捷键
     */
    public void register() {
        KeyMapping.Category vmtoolsCategory = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("vmtools", "main"));

        // 注册路径点管理快捷键
        waypointKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.vmtools.waypoints",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                vmtoolsCategory
        ));

        // 注册 Tick 事件，检测快捷键按下
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);

        VMToolsClient.LOGGER.info("快捷键注册完成");
    }

    /**
     * 客户端 Tick 事件处理
     */
    private void onClientTick(Minecraft client) {
        while (waypointKey.consumeClick()) {
            if (client.screen == null) {
                client.setScreen(new WaypointScreen());
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
     * 获取快捷键的显示名称
     */
    public String getWaypointKeyName() {
        return waypointKey.getTranslatedKeyMessage().getString();
    }
}
