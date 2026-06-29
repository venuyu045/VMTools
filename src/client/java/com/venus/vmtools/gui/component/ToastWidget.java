package com.venus.vmtools.gui.component;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Toast 通知组件 - 显示操作反馈
 */
public class ToastWidget {

    private static final List<Toast> toasts = new ArrayList<>();
    private static final int TOAST_WIDTH = 200;
    private static final int TOAST_HEIGHT = 30;
    private static final int TOAST_MARGIN = 10;
    private static final long TOAST_DURATION = 3000; // 3秒
    private static final int FADE_DURATION = 500; // 淡入淡出时间

    /**
     * 显示成功提示
     */
    public static void showSuccess(String message) {
        addToast(message, ToastType.SUCCESS);
    }

    /**
     * 显示错误提示
     */
    public static void showError(String message) {
        addToast(message, ToastType.ERROR);
    }

    /**
     * 显示警告提示
     */
    public static void showWarning(String message) {
        addToast(message, ToastType.WARNING);
    }

    /**
     * 显示信息提示
     */
    public static void showInfo(String message) {
        addToast(message, ToastType.INFO);
    }

    private static void addToast(String message, ToastType type) {
        Toast toast = new Toast(message, type, System.currentTimeMillis());
        toasts.add(toast);

        // 限制最大数量
        if (toasts.size() > 5) {
            toasts.remove(0);
        }
    }

    /**
     * 渲染所有 Toast
     */
    public static void render(GuiGraphicsExtractor context, int screenWidth, int screenHeight) {
        Minecraft client = Minecraft.getInstance();
        if (client.font == null) return;

        long currentTime = System.currentTimeMillis();
        Iterator<Toast> iterator = toasts.iterator();
        int yOffset = 0;

        while (iterator.hasNext()) {
            Toast toast = iterator.next();
            long age = currentTime - toast.createdAt;

            // 检查是否过期
            if (age > TOAST_DURATION) {
                iterator.remove();
                continue;
            }

            // 计算透明度
            float alpha;
            if (age < FADE_DURATION) {
                alpha = (float) age / FADE_DURATION;
            } else if (age > TOAST_DURATION - FADE_DURATION) {
                alpha = (float) (TOAST_DURATION - age) / FADE_DURATION;
            } else {
                alpha = 1.0f;
            }

            // 计算自适应宽度（限制最大宽度，超出则换行）
            String icon = toast.type.getIcon();
            int maxToastWidth = 350;
            int maxTextWidth = maxToastWidth - 50; // 留给图标和边距
            int textWidth = client.font.width(toast.message);

            // 拆分为多行
            java.util.List<String> lines = new java.util.ArrayList<>();
            if (textWidth <= maxTextWidth) {
                lines.add(toast.message);
            } else {
                // 按最大宽度拆行
                String remaining = toast.message;
                while (!remaining.isEmpty()) {
                    int fitLen = 0;
                    for (int i = 1; i <= remaining.length(); i++) {
                        if (client.font.width(remaining.substring(0, i)) > maxTextWidth) {
                            fitLen = i - 1;
                            break;
                        }
                        fitLen = i;
                    }
                    if (fitLen <= 0) fitLen = 1;
                    lines.add(remaining.substring(0, fitLen));
                    remaining = remaining.substring(fitLen);
                }
            }

            // 计算实际宽度
            int actualTextWidth = 0;
            for (String line : lines) {
                actualTextWidth = Math.max(actualTextWidth, client.font.width(line));
            }
            int toastWidth = Math.min(maxToastWidth, Math.max(150, actualTextWidth + 50));
            int toastHeight = lines.size() * 12 + 12; // 每行12px + 上下padding

            // 计算位置（右上角）
            int x = screenWidth - toastWidth - TOAST_MARGIN;
            int y = TOAST_MARGIN + yOffset;

            // 渲染背景
            int backgroundColor = toast.type.getBackgroundColor(alpha);
            context.fill(x, y, x + toastWidth, y + toastHeight, backgroundColor);

            // 渲染边框
            int borderColor = toast.type.getBorderColor(alpha);
            context.fill(x, y, x + toastWidth, y + 1, borderColor);
            context.fill(x, y + toastHeight - 1, x + toastWidth, y + toastHeight, borderColor);
            context.fill(x, y, x + 1, y + toastHeight, borderColor);
            context.fill(x + toastWidth - 1, y, x + toastWidth, y + toastHeight, borderColor);

            // 渲染图标（第一行旁边）
            context.text(client.font, icon, x + 8, y + 6, 0xFFFFFFFF);

            // 渲染文字（多行）
            int textColor = toast.type.getTextColor(alpha);
            for (int i = 0; i < lines.size(); i++) {
                context.text(client.font, lines.get(i), x + 28, y + 6 + i * 12, textColor);
            }

            yOffset += toastHeight + TOAST_MARGIN;
        }
    }

    /**
     * Toast 类型枚举
     */
    private enum ToastType {
        SUCCESS(0xFF4ADE80, 0xFF22C55E, 0xFF166534, "✓"),
        ERROR(0xFFF87171, 0xFFEF4444, 0xFF7F1D1D, "✗"),
        WARNING(0xFFFBBF24, 0xFFF59E0B, 0xFF78350F, "⚠"),
        INFO(0xFF60A5FA, 0xFF3B82F6, 0xFF1E3A5F, "ℹ");

        private final int textColor;
        private final int borderColor;
        private final int backgroundColor;
        private final String icon;

        ToastType(int textColor, int borderColor, int backgroundColor, String icon) {
            this.textColor = textColor;
            this.borderColor = borderColor;
            this.backgroundColor = backgroundColor;
            this.icon = icon;
        }

        int getTextColor(float alpha) {
            return applyAlpha(textColor, alpha);
        }

        int getBorderColor(float alpha) {
            return applyAlpha(borderColor, alpha);
        }

        int getBackgroundColor(float alpha) {
            return applyAlpha(backgroundColor, alpha);
        }

        String getIcon() {
            return icon;
        }

        private int applyAlpha(int color, float alpha) {
            int a = (int) (alpha * 255) & 0xFF;
            return (color & 0x00FFFFFF) | (a << 24);
        }
    }

    /**
     * Toast 数据类
     */
    private static class Toast {
        final String message;
        final ToastType type;
        final long createdAt;

        Toast(String message, ToastType type, long createdAt) {
            this.message = message;
            this.type = type;
            this.createdAt = createdAt;
        }
    }
}
