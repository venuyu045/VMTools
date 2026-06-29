package com.venus.vmtools.feature.waypoint;

import com.venus.vmtools.VMToolsClient;
import com.venus.vmtools.config.ModConfig;
import com.venus.vmtools.feature.freeze.FreezeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * 传送服务 - 专门处理传送命令发送
 */
public class TeleportService {

    private static final Minecraft client = Minecraft.getInstance();

    /**
     * 发送传送命令
     *
     * @param command 传送命令（如 /res tp main_city）
     * @return 是否成功发送
     */
    public static boolean sendCommand(String command) {
        if (client.player == null || client.getConnection() == null) {
            VMToolsClient.LOGGER.warn("无法传送：玩家未连接到服务器");
            return false;
        }

        // 确保命令格式正确
        String normalizedCommand = normalizeCommand(command);
        if (normalizedCommand == null) {
            VMToolsClient.LOGGER.error("无效的命令格式: {}", command);
            return false;
        }

        // 仅对 /res tp 和 /home 命令启用冻结（绕过移动检测）
        if (shouldFreeze(normalizedCommand)) {
            FreezeManager.getInstance().activate(3000);
        }

        // 对 /res tp 命令：传送前先保存当前坐标到 resback
        final boolean saveBack = isResTp(normalizedCommand);

        // 发送命令到服务器
        final String finalCommand = normalizedCommand;
        client.execute(() -> {
            try {
                // /res tp 传送前：将当前位置保存为 resback home 点
                if (saveBack) {
                    client.getConnection().sendCommand("edithome resback relocate");
                }

                // 使用 networkHandler.sendChatCommand 发送命令（不显示在聊天栏）
                client.getConnection().sendCommand(finalCommand);
                VMToolsClient.LOGGER.info("已发送传送命令: /{}", finalCommand);

                // 显示成功提示
                client.player.sendOverlayMessage(
                        Component.literal("[VMTools] 已发送传送命令")
                );
            } catch (Exception e) {
                VMToolsClient.LOGGER.error("发送命令失败", e);
                client.player.sendOverlayMessage(
                        Component.literal("[VMTools] 传送失败: " + e.getMessage())
                );
            }
        });

        return true;
    }

    /**
     * 判断是否应该对该命令启用移动冻结
     * 条件：功能开启 && 命令属于 res tp 或 home
     */
    private static boolean shouldFreeze(String normalizedCommand) {
        ModConfig config = VMToolsClient.getInstance().getConfig();
        if (!config.isFreezeEnabled()) {
            return false;
        }

        String lower = normalizedCommand.toLowerCase();
        return lower.startsWith("res tp ") || lower.equals("res tp")
                || lower.startsWith("home ") || lower.equals("home");
    }

    /**
     * 判断命令是否属于 res tp（需要保存 back 位置）
     */
    private static boolean isResTp(String normalizedCommand) {
        String lower = normalizedCommand.toLowerCase();
        return lower.startsWith("res tp ") || lower.equals("res tp");
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
