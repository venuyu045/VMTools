package com.venus.vmtools.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * 聊天工具类
 */
public class ChatUtil {

    /**
     * 发送命令到服务器（不显示在聊天栏）
     */
    public static void sendCommand(String command) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.getConnection() == null) {
            return;
        }

        // 去掉开头的 /
        if (command.startsWith("/")) {
            command = command.substring(1);
        }

        final String finalCommand = command;
        client.execute(() -> {
            client.getConnection().sendCommand(finalCommand);
        });
    }

    /**
     * 发送聊天消息（显示在聊天栏）
     */
    public static void sendMessage(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.getConnection() == null) {
            return;
        }

        client.execute(() -> {
            client.getConnection().sendChat(message);
        });
    }

    /**
     * 显示本地提示消息（仅客户端可见）
     */
    public static void showLocalMessage(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        client.execute(() -> {
            client.player.sendSystemMessage(
                    Component.literal("[VMTools] " + message)
            );
        });
    }

    /**
     * 显示动作栏消息
     */
    public static void showActionBar(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        client.execute(() -> {
            client.player.sendOverlayMessage(
                    Component.literal(message)
            );
        });
    }
}
