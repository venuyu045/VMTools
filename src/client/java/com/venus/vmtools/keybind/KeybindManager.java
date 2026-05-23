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

    // 快捷传送快捷键（默认 Ctrl+T）
    private KeyBinding quickTeleportKey;

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

        // 注册快捷传送快捷键
        quickTeleportKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vmtools.quick_teleport",       // 翻译键
                InputUtil.Type.KEYSYM,              // 键类型
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
    private void onClientTick(MinecraftClient client) {
        // 检测路径点快捷键
        while (waypointKey.wasPressed()) {
            if (client.currentScreen == null) {
                client.setScreen(new WaypointScreen());
            }
        }

        // 检测快捷传送快捷键（需要 Ctrl 修饰）
        while (quickTeleportKey.wasPressed()) {
            // 检查是否按住了 Ctrl
            long window = client.getWindow().getHandle();
            boolean ctrlPressed = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS ||
                                  GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
            if (ctrlPressed && client.currentScreen == null) {
                // TODO: 打开快捷传送界面（后续实现）
                VMToolsClient.LOGGER.info("快捷传送功能待实现");
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
     * 获取快捷传送快捷键
     */
    public KeyBinding getQuickTeleportKey() {
        return quickTeleportKey;
    }

    /**
     * 获取快捷键的显示名称
     */
    public String getWaypointKeyName() {
        return waypointKey.getBoundKeyLocalizedText().getString();
    }

    public String getQuickTeleportKeyName() {
        return quickTeleportKey.getBoundKeyLocalizedText().getString();
    }
}
