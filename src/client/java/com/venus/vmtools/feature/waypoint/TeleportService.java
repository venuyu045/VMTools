package com.venus.vmtools.feature.waypoint;

import com.venus.vmtools.VMToolsClient;
import com.venus.vmtools.config.ModConfig;
import com.venus.vmtools.feature.freeze.FreezeManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * 传送服务 - 专门处理传送命令发送
 */
public class TeleportService {

    private static final MinecraftClient client = MinecraftClient.getInstance();

    /** 命令最大长度，与 EditWaypointScreen 的输入框上限保持一致 */
    public static final int MAX_COMMAND_LENGTH = 50;

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

        // 仅对 /res tp 和 /home 命令启用冻结
        if (shouldFreeze(normalizedCommand)) FreezeManager.getInstance().activate(3000);

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

    private static boolean shouldFreeze(String normalizedCommand) {
        ModConfig config = VMToolsClient.getInstance().getConfig();
        if (!config.isFreezeEnabled()) return false;
        String lower = normalizedCommand.toLowerCase();
        return lower.startsWith("res tp ") || lower.equals("res tp")
                || lower.startsWith("home ") || lower.equals("home");
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

        // 拒绝控制字符（换行、回车、制表、NUL 等）。
        // 命令来自路径点数据/导入 JSON，可能被写入 \n 尝试拆分成多条命令，
        // 这里在发送前统一拦截，避免协议层出现意外行为。
        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);
            if (c < 0x20 || c == 0x7F) {
                VMToolsClient.LOGGER.warn("命令包含非法控制字符，已拒绝: U+{}", (int) c);
                return null;
            }
        }

        // 命令长度上限（与输入框限制保持一致，防止导入的超长命令）
        if (command.length() > MAX_COMMAND_LENGTH) {
            VMToolsClient.LOGGER.warn("命令超长，已拒绝（{} 字符）", command.length());
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
