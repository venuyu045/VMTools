package com.venus.vmtools.gui.component;

import com.venus.vmtools.feature.waypoint.WaypointColor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * 颜色选择器组件
 */
public class ColorPicker {

    private static final int COLOR_SIZE = 20;
    private static final int COLOR_SPACING = 4;

    private final int x;
    private final int y;
    private final WaypointColor[] colors;
    private WaypointColor selectedColor;
    private Consumer<WaypointColor> onColorChanged;

    public ColorPicker(int x, int y, WaypointColor defaultColor) {
        this.x = x;
        this.y = y;
        this.colors = WaypointColor.values();
        this.selectedColor = defaultColor;
    }

    /**
     * 设置颜色变更回调
     */
    public void setOnColorChanged(Consumer<WaypointColor> callback) {
        this.onColorChanged = callback;
    }

    /**
     * 获取选中的颜色
     */
    public WaypointColor getSelectedColor() {
        return selectedColor;
    }

    /**
     * 设置选中的颜色
     */
    public void setSelectedColor(WaypointColor color) {
        this.selectedColor = color;
    }

    /**
     * 渲染颜色选择器
     */
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, int centerX) {
        int totalWidth = colors.length * (COLOR_SIZE + COLOR_SPACING) - COLOR_SPACING;
        int startX = centerX - totalWidth / 2;

        for (int i = 0; i < colors.length; i++) {
            WaypointColor color = colors[i];
            int colorX = startX + i * (COLOR_SIZE + COLOR_SPACING);

            // 渲染颜色块
            int c = color == selectedColor ? 0xFFFFFFFF : color.getARGB();
            fillRoundedRect(context, colorX + 2, y + 2, COLOR_SIZE - 4, COLOR_SIZE - 4, c);

            // 渲染选中边框
            if (color == selectedColor) {
                drawBorder(context, colorX, y, COLOR_SIZE, COLOR_SIZE, 0xFFFFFFFF);
            }

            // 渲染悬停效果
            if (mouseX >= colorX && mouseX <= colorX + COLOR_SIZE &&
                    mouseY >= y && mouseY <= y + COLOR_SIZE) {
                drawBorder(context, colorX, y, COLOR_SIZE, COLOR_SIZE, 0x80FFFFFF);
            }
        }
    }

    /**
     * 处理鼠标点击
     */
    public boolean mouseClicked(double mouseX, double mouseY, int centerX) {
        int totalWidth = colors.length * (COLOR_SIZE + COLOR_SPACING) - COLOR_SPACING;
        int startX = centerX - totalWidth / 2;

        for (int i = 0; i < colors.length; i++) {
            int colorX = startX + i * (COLOR_SIZE + COLOR_SPACING);
            if (mouseX >= colorX && mouseX <= colorX + COLOR_SIZE &&
                    mouseY >= y && mouseY <= y + COLOR_SIZE) {
                selectedColor = colors[i];
                if (onColorChanged != null) {
                    onColorChanged.accept(selectedColor);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * 获取组件高度
     */
    public int getHeight() {
        return COLOR_SIZE;
    }

    // ==================== 绘制工具方法 ====================

    private void fillRoundedRect(GuiGraphicsExtractor context, int x, int y, int width, int height, int color) {
        context.fill(x + 2, y, x + width - 2, y + height, color);
        context.fill(x, y + 2, x + width, y + height - 2, color);
    }

    private void drawBorder(GuiGraphicsExtractor context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }
}
