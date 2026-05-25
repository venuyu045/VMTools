package com.venus.vmtools.keybind;

import com.venus.vmtools.VMToolsClient;
import com.venus.vmtools.gui.WaypointScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.KeyBinding.Category;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * 快捷键管理器 - 负责注册和管理所有工具的快捷键
 */
public class KeybindManager {

    // 路径点管理界面快捷键（默认 M 键）
    private KeyBinding waypointKey;

    /**
     * 注册所有快捷键
     */
    public void register() {
        Category vmtoolsCategory = Category.create(Identifier.of("vmtools", "main"));

        // 注册路径点管理快捷键
        waypointKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vmtools.waypoints",           // 翻译键
                InputUtil.Type.KEYSYM,              // 键类型
                GLFW.GLFW_KEY_M,                    // 默认键 M
                vmtoolsCategory                     // 分类
        ));

        // 注册 Tick 事件，检测快捷键按下
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);

        VMToolsClient.LOGGER.info("快捷键注册完成");
    }

    /**
     * 客户端 Tick 事件处理
     */
    private void onClientTick(MinecraftClient client) {
        // 检测路径点快捷键
        while (waypointKey.wasPressed()) {
            if (client.currentScreen == null) {
                client.setScreen(new WaypointScreen());
            }
        }

    }

    /**
     * 获取路径点快捷键
     */
    public KeyBinding getWaypointKey() {
        return waypointKey;
    }

    /**
     * 获取快捷键的显示名称
     */
    public String getWaypointKeyName() {
        return waypointKey.getBoundKeyLocalizedText().getString();
    }
}
