package com.venus.vmtools.gui;

import com.venus.vmtools.gui.component.ToastWidget;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * 通用确认对话框
 */
public class ConfirmScreen extends Screen {

    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 100;

    // 颜色
    private static final int PANEL_COLOR = 0xFF2D2D44;
    private static final int HEADER_COLOR = 0xFF3D3D5C;
    private static final int TEXT_COLOR = 0xFFE0E0E0;
    private static final int DANGER_COLOR = 0xFFF87171;

    private final Screen parent;
    private final String title;
    private final String message;
    private final String confirmText;
    private final Runnable onConfirm;
    private final Runnable onCancel;

    public ConfirmScreen(Screen parent, String title, String message, String confirmText,
                         Runnable onConfirm, Runnable onCancel) {
        super(Component.literal(title));
        this.parent = parent;
        this.title = title;
        this.message = message;
        this.confirmText = confirmText;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
    }

    /**
     * 创建删除确认对话框
     */
    public static ConfirmScreen createDeleteConfirm(Screen parent, String itemName, Runnable onConfirm) {
        return new ConfirmScreen(
                parent,
                "确认删除",
                "确定要删除 " + itemName + " 吗？",
                "删除",
                onConfirm,
                () -> {}
        );
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int panelX = centerX - PANEL_WIDTH / 2;
        int panelY = centerY - PANEL_HEIGHT / 2;

        int buttonY = panelY + PANEL_HEIGHT - 30;
        int buttonWidth = 70;

        // 确认按钮
        this.addRenderableWidget(Button.builder(
                Component.literal(confirmText),
                button -> {
                    if (onConfirm != null) onConfirm.run();
                    onClose();
                }
        ).bounds(centerX - buttonWidth - 10, buttonY, buttonWidth, 20).build());

        // 取消按钮
        this.addRenderableWidget(Button.builder(
                Component.literal("取消"),
                button -> {
                    if (onCancel != null) onCancel.run();
                    onClose();
                }
        ).bounds(centerX + 10, buttonY, buttonWidth, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // 半透明背景
        context.fill(0, 0, this.width, this.height, 0x80000000);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int panelX = centerX - PANEL_WIDTH / 2;
        int panelY = centerY - PANEL_HEIGHT / 2;

        // 面板背景
        fillRoundedRect(context, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, PANEL_COLOR);

        // 头部
        fillRoundedRect(context, panelX, panelY, PANEL_WIDTH, 25, HEADER_COLOR);
        drawCenteredText(context, title, centerX, panelY + 7, DANGER_COLOR);

        // 消息
        drawCenteredText(context, message, centerX, panelY + 45, TEXT_COLOR);

        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return super.mouseClicked(event, doubleClick);
    }

    // 绘制工具
    private void fillRoundedRect(GuiGraphicsExtractor context, int x, int y, int width, int height, int color) {
        context.fill(x + 2, y, x + width - 2, y + height, color);
        context.fill(x, y + 2, x + width, y + height - 2, color);
    }

    private void drawCenteredText(GuiGraphicsExtractor context, String text, int centerX, int y, int color) {
        int textWidth = this.font.width(text);
        context.text(this.font, text, centerX - textWidth / 2, y, color);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
