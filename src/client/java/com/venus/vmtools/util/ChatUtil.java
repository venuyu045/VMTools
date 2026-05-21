package com.venus.vmtools.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * 聊天工具类
 */
public class ChatUtil {

    /**
     * 发送命令到服务器（不显示在聊天栏）
     */
    public static void sendCommand(String command) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.getNetworkHandler() == null) {
            return;
        }

        // 去掉开头的 /
        if (command.startsWith("/")) {
            command = command.substring(1);
        }

        final String finalCommand = command;
        client.execute(() -> {
            client.getNetworkHandler().sendChatCommand(finalCommand);
        });
    }

    /**
     * 发送聊天消息（显示在聊天栏）
     */
    public static void sendMessage(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.getNetworkHandler() == null) {
            return;
        }

        client.execute(() -> {
            client.getNetworkHandler().sendChatMessage(message);
        });
    }

    /**
     * 显示本地提示消息（仅客户端可见）
     */
    public static void showLocalMessage(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        client.execute(() -> {
            client.player.sendMessage(
                    Text.literal("[VMTools] " + message),
                    false
            );
        });
    }

    /**
     * 显示动作栏消息
     */
    public static void showActionBar(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        client.execute(() -> {
            client.player.sendMessage(
                    Text.literal(message),
                    true
            );
        });
    }
}
