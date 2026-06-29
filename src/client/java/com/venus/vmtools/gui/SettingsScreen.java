package com.venus.vmtools.gui;

import com.venus.vmtools.VMToolsClient;
import com.venus.vmtools.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/**
 * 设置界面 - 统一管理模组配置（MC 26.2 Mojang mappings）
 */
public class SettingsScreen extends Screen {

    private final Screen parent;
    private final ModConfig config;

    private Button autoConfirmToggle;
    private EditBox confirmCommandField;
    private EditBox confirmDelayField;
    private Button freezeToggle;

    public SettingsScreen(Screen parent) {
        super(Component.literal("VMTools 设置"));
        this.parent = parent;
        this.config = VMToolsClient.getInstance().getConfig();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = 50;
        int rowHeight = 30;

        // 自动确认传送开关
        autoConfirmToggle = Button.builder(
                getToggleText("自动确认传送", config.isAutoConfirmTeleport()),
                button -> {
                    config.setAutoConfirmTeleport(!config.isAutoConfirmTeleport());
                    button.setMessage(getToggleText("自动确认传送", config.isAutoConfirmTeleport()));
                    confirmCommandField.setVisible(config.isAutoConfirmTeleport());
                    confirmDelayField.setVisible(config.isAutoConfirmTeleport());
                }
        ).bounds(centerX - 100, startY, 200, 20).build();
        this.addRenderableWidget(autoConfirmToggle);

        // 确认命令输入框
        confirmCommandField = new EditBox(this.font,
                centerX - 100, startY + rowHeight,
                200, 20,
                Component.literal("确认命令"));
        confirmCommandField.setHint(Component.literal("例: res tpconfirm"));
        confirmCommandField.setValue(config.getConfirmCommand());
        confirmCommandField.setResponder(text -> config.setConfirmCommand(text));
        confirmCommandField.setVisible(config.isAutoConfirmTeleport());
        this.addRenderableWidget(confirmCommandField);

        // 延迟输入框
        confirmDelayField = new EditBox(this.font,
                centerX - 100, startY + rowHeight * 2,
                200, 20,
                Component.literal("延迟(ms)"));
        confirmDelayField.setHint(Component.literal("默认 200ms"));
        confirmDelayField.setValue(String.valueOf(config.getConfirmDelay()));
        confirmDelayField.setResponder(text -> {
            try {
                config.setConfirmDelay(Integer.parseInt(text));
            } catch (NumberFormatException ignored) {}
        });
        confirmDelayField.setVisible(config.isAutoConfirmTeleport());
        this.addRenderableWidget(confirmDelayField);

        // 绕过移动检测开关
        freezeToggle = Button.builder(
                getToggleText("绕过移动检测", config.isFreezeEnabled()),
                button -> {
                    config.setFreezeEnabled(!config.isFreezeEnabled());
                    button.setMessage(getToggleText("绕过移动检测", config.isFreezeEnabled()));
                }
        ).bounds(centerX - 100, startY + rowHeight * 4, 200, 20).build();
        this.addRenderableWidget(freezeToggle);

        // 返回按钮
        this.addRenderableWidget(Button.builder(
                Component.literal("返回保存"),
                button -> {
                    config.save();
                    this.minecraft.setScreen(parent);
                }
        ).bounds(centerX - 50, this.height - 30, 100, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // 半透明背景
        context.fill(0, 0, this.width, this.height, 0xC0000000);

        int centerX = this.width / 2;

        // 标题
        String title = "VMTools 设置";
        int titleWidth = this.font.width(title);
        context.text(this.font, title, centerX - titleWidth / 2, 15, 0xFF7C3AED);

        // 说明文字
        if (config.isAutoConfirmTeleport()) {
            int infoY = 140;
            String line1 = "开启后，点击传送会自动执行确认命令";
            String line2 = "例：传送后自动执行 /res tpconfirm";
            String line3 = "命令不需要带 / 前缀";
            context.text(this.font, line1, centerX - this.font.width(line1) / 2, infoY, 0xFF888888);
            context.text(this.font, line2, centerX - this.font.width(line2) / 2, infoY + 14, 0xFF888888);
            context.text(this.font, line3, centerX - this.font.width(line3) / 2, infoY + 28, 0xFF888888);
        }

        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        config.save();
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private Component getToggleText(String label, boolean enabled) {
        return Component.literal(label + ": " + (enabled ? "§a开启" : "§c关闭"));
    }
}
