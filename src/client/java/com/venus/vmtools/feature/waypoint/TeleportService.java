package com.venus.vmtools.feature.waypoint;

import com.venus.vmtools.VMToolsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * 传送服务 - 专门处理传送命令发送
 */
public class TeleportService {

    private static final MinecraftClient client = MinecraftClient.getInstance();

    /**
     * 发送传送命令
     *
     * @param command 传送命令（如 /res tp main_city）
     * @return 是否成功发送
     */
    public static boolean sendCommand(String command) {
        if (client.player == null || client.getNetworkHandler() == null) {
            VMToolsClient.LOGGER.warn("无法传送：玩家未连接到服务器");
            return false;
        }

        // 确保命令格式正确
        String normalizedCommand = normalizeCommand(command);
        if (normalizedCommand == null) {
            VMToolsClient.LOGGER.error("无效的命令格式: {}", command);
            return false;
        }

        // 发送命令到服务器
        final String finalCommand = normalizedCommand;
        client.execute(() -> {
            try {
                // 使用 networkHandler.sendChatCommand 发送命令（不显示在聊天栏）
                client.getNetworkHandler().sendChatCommand(finalCommand);
                VMToolsClient.LOGGER.info("已发送传送命令: /{}", finalCommand);

                // 显示成功提示
                client.player.sendMessage(
                        Text.literal("[VMTools] 已发送传送命令"),
                        true
                );
            } catch (Exception e) {
                VMToolsClient.LOGGER.error("发送命令失败", e);
                client.player.sendMessage(
                        Text.literal("[VMTools] 传送失败: " + e.getMessage()),
                        true
                );
            }
        });

        return true;
    }

    /**
     * 规范化命令格式
     * 去掉开头的 /，确保命令有效
     */
    private static String normalizeCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            return null;
        }

        command = command.trim();

        // 去掉开头的 /
        if (command.startsWith("/")) {
            command = command.substring(1);
        }

        // 检查命令是否为空
        if (command.isEmpty()) {
            return null;
        }

        return command;
    }

    /**
     * 验证命令格式是否有效
     *
     * @param command 命令字符串
     * @return 是否有效
     */
    public static boolean isValidCommand(String command) {
        return normalizeCommand(command) != null;
    }

    /**
     * 获取命令的显示格式（带 / 前缀）
     *
     * @param command 原始命令
     * @return 显示格式
     */
    public static String getDisplayCommand(String command) {
        String normalized = normalizeCommand(command);
        if (normalized == null) {
            return "";
        }
        return "/" + normalized;
    }
}
