package com.venus.vmtools.gui;

import com.venus.vmtools.VMToolsClient;
import com.venus.vmtools.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/**
 * 设置界面 - 统一管理模组配置
 */
public class SettingsScreen extends Screen {

    private final Screen parent;
    private final ModConfig config;

    private ButtonWidget autoConfirmToggle;
    private TextFieldWidget confirmCommandField;
    private TextFieldWidget confirmDelayField;

    public SettingsScreen(Screen parent) {
        super(Text.of("VMTools 设置"));
        this.parent = parent;
        this.config = VMToolsClient.getInstance().getConfig();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = 50;
        int rowHeight = 30;

        // 自动确认传送开关
        autoConfirmToggle = ButtonWidget.builder(
                getToggleText("自动确认传送", config.isAutoConfirmTeleport()),
                button -> {
                    config.setAutoConfirmTeleport(!config.isAutoConfirmTeleport());
                    button.setMessage(getToggleText("自动确认传送", config.isAutoConfirmTeleport()));
                    confirmCommandField.setVisible(config.isAutoConfirmTeleport());
                    confirmDelayField.setVisible(config.isAutoConfirmTeleport());
                }
        ).dimensions(centerX - 100, startY, 200, 20).build();
        this.addDrawableChild(autoConfirmToggle);

        // 确认命令输入框
        confirmCommandField = new TextFieldWidget(this.textRenderer,
                centerX - 100, startY + rowHeight,
                200, 20,
                Text.of("确认命令"));
        confirmCommandField.setPlaceholder(Text.of("例: res tpconfirm"));
        confirmCommandField.setText(config.getConfirmCommand());
        confirmCommandField.setChangedListener(text -> config.setConfirmCommand(text));
        confirmCommandField.setVisible(config.isAutoConfirmTeleport());
        this.addDrawableChild(confirmCommandField);

        // 延迟输入框
        confirmDelayField = new TextFieldWidget(this.textRenderer,
                centerX - 100, startY + rowHeight * 2,
                200, 20,
                Text.of("延迟(ms)"));
        confirmDelayField.setPlaceholder(Text.of("默认 200ms"));
        confirmDelayField.setText(String.valueOf(config.getConfirmDelay()));
        confirmDelayField.setChangedListener(text -> {
            try {
                config.setConfirmDelay(Integer.parseInt(text));
            } catch (NumberFormatException ignored) {}
        });
        confirmDelayField.setVisible(config.isAutoConfirmTeleport());
        this.addDrawableChild(confirmDelayField);

        // 返回按钮
        this.addDrawableChild(ButtonWidget.builder(
                Text.of("返回保存"),
                button -> {
                    config.save();
                    MinecraftClient.getInstance().setScreen(parent);
                }
        ).dimensions(centerX - 50, this.height - 30, 100, 20).build());
    }

    @Override
    public void render(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
        // 半透明背景
        context.fill(0, 0, this.width, this.height, 0xC0000000);

        int centerX = this.width / 2;

        // 标题
        String title = "VMTools 设置";
        int titleWidth = this.textRenderer.getWidth(title);
        context.drawTextWithShadow(this.textRenderer, title, centerX - titleWidth / 2, 15, 0xFF7C3AED);

        // 说明文字
        if (config.isAutoConfirmTeleport()) {
            int infoY = 140;
            String line1 = "开启后，点击传送会自动执行确认命令";
            String line2 = "例：传送后自动执行 /res tpconfirm";
            String line3 = "命令不需要带 / 前缀";
            context.drawTextWithShadow(this.textRenderer, line1, centerX - this.textRenderer.getWidth(line1) / 2, infoY, 0xFF888888);
            context.drawTextWithShadow(this.textRenderer, line2, centerX - this.textRenderer.getWidth(line2) / 2, infoY + 14, 0xFF888888);
            context.drawTextWithShadow(this.textRenderer, line3, centerX - this.textRenderer.getWidth(line3) / 2, infoY + 28, 0xFF888888);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        config.save();
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private Text getToggleText(String label, boolean enabled) {
        return Text.of(label + ": " + (enabled ? "§a开启" : "§c关闭"));
    }
}
