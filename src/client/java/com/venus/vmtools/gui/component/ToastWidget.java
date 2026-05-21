package com.venus.vmtools.gui.component;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

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
    public static void render(DrawContext context, int screenWidth, int screenHeight) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.textRenderer == null) return;

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
                // 淡入
                alpha = (float) age / FADE_DURATION;
            } else if (age > TOAST_DURATION - FADE_DURATION) {
                // 淡出
                alpha = (float) (TOAST_DURATION - age) / FADE_DURATION;
            } else {
                alpha = 1.0f;
            }

            // 计算位置（右上角）
            int x = screenWidth - TOAST_WIDTH - TOAST_MARGIN;
            int y = TOAST_MARGIN + yOffset;

            // 渲染背景
            int backgroundColor = toast.type.getBackgroundColor(alpha);
            context.fill(x, y, x + TOAST_WIDTH, y + TOAST_HEIGHT, backgroundColor);

            // 渲染边框
            int borderColor = toast.type.getBorderColor(alpha);
            context.fill(x, y, x + TOAST_WIDTH, y + 1, borderColor);
            context.fill(x, y + TOAST_HEIGHT - 1, x + TOAST_WIDTH, y + TOAST_HEIGHT, borderColor);
            context.fill(x, y, x + 1, y + TOAST_HEIGHT, borderColor);
            context.fill(x + TOAST_WIDTH - 1, y, x + TOAST_WIDTH, y + TOAST_HEIGHT, borderColor);

            // 渲染图标
            String icon = toast.type.getIcon();
            context.drawTextWithShadow(client.textRenderer, icon, x + 8, y + 8, 0xFFFFFFFF);

            // 渲染文字
            int textColor = toast.type.getTextColor(alpha);
            context.drawTextWithShadow(client.textRenderer, toast.message, x + 28, y + 8, textColor);

            yOffset += TOAST_HEIGHT + TOAST_MARGIN;
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
