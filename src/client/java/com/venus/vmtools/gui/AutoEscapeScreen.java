package com.venus.vmtools.gui;

import com.venus.vmtools.feature.escape.AutoEscapeConfig;
import com.venus.vmtools.feature.escape.AutoEscapeManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;

/**
 * 自动逃逸设置界面
 * 监控玩家Y坐标，当低于阈值时触发逃逸命令
 */
public class AutoEscapeScreen extends Screen {

    // 窗口尺寸常量
    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 380;
    private static final int TAB_BAR_HEIGHT = 25;
    private static final int PADDING = 12;

    // 颜色定义
    private static final int PANEL_COLOR = 0xFF2D2D44;
    private static final int HEADER_COLOR = 0xFF3D3D5C;
    private static final int TEXT_COLOR = 0xFFE0E0E0;
    private static final int ACCENT_COLOR = 0xFF7C3AED;
    private static final int SUBTLE_COLOR = 0xFF888888;
    private static final int SUCCESS_COLOR = 0xFF4ADE80;
    private static final int DANGER_COLOR = 0xFFF87171;

    // 管理器
    private AutoEscapeManager autoEscapeManager;
    private AutoEscapeConfig config;

    // 输入框
    private TextFieldWidget thresholdField;
    private TextFieldWidget commandField;
    private TextFieldWidget confirmCommandField;
    private TextFieldWidget confirmDelayField;

    // 勾选框位置
    private int enableCheckboxX, enableCheckboxY;
    private int autoConfirmCheckboxX, autoConfirmCheckboxY;
    private static final int CHECKBOX_HEIGHT = 16;

    // 日志区域
    private int logScrollOffset = 0;
    private int maxLogScroll = 0;
    private static final int LOG_VISIBLE_ENTRIES = 8;
    private static final int LOG_ENTRY_HEIGHT = 14;

    /**
     * 构造函数
     */
    public AutoEscapeScreen() {
        super(Text.of("VMTools - 自动逃逸"));
        this.autoEscapeManager = AutoEscapeManager.getInstance();
        this.config = autoEscapeManager.getConfig();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int panelX = centerX - PANEL_WIDTH / 2;
        int panelY = centerY - PANEL_HEIGHT / 2;

        // 阈值输入框
        thresholdField = new TextFieldWidget(this.textRenderer,
                panelX + PADDING + 70, panelY + TAB_BAR_HEIGHT + 85,
                PANEL_WIDTH - PADDING * 2 - 70, 18,
                Text.of("高度阈值"));
        thresholdField.setPlaceholder(Text.literal("-70.0").styled(s -> s.withColor(SUBTLE_COLOR)));
        thresholdField.setText(String.valueOf(config.getThreshold()));
        thresholdField.setMaxLength(10);
        thresholdField.setChangedListener(text -> {
            try {
                double value = Double.parseDouble(text);
                autoEscapeManager.setThreshold(value);
            } catch (NumberFormatException ignored) {
                // 忽略无效输入
            }
        });
        this.addDrawableChild(thresholdField);

        // 执行命令输入框
        commandField = new TextFieldWidget(this.textRenderer,
                panelX + PADDING + 70, panelY + TAB_BAR_HEIGHT + 115,
                PANEL_WIDTH - PADDING * 2 - 70, 18,
                Text.of("执行命令"));
        commandField.setPlaceholder(Text.literal("/ehome").styled(s -> s.withColor(SUBTLE_COLOR)));
        commandField.setText(config.getCommand());
        commandField.setMaxLength(50);
        commandField.setChangedListener(text -> autoEscapeManager.setCommand(text));
        this.addDrawableChild(commandField);

        // 确认命令输入框
        confirmCommandField = new TextFieldWidget(this.textRenderer,
                panelX + PADDING + 70, panelY + TAB_BAR_HEIGHT + 175,
                PANEL_WIDTH - PADDING * 2 - 70, 18,
                Text.of("确认命令"));
        confirmCommandField.setPlaceholder(Text.literal("res tpconfirm").styled(s -> s.withColor(SUBTLE_COLOR)));
        confirmCommandField.setText(config.getConfirmCommand());
        confirmCommandField.setMaxLength(50);
        confirmCommandField.setChangedListener(text -> autoEscapeManager.setConfirmCommand(text));
        this.addDrawableChild(confirmCommandField);

        // 确认延迟输入框
        confirmDelayField = new TextFieldWidget(this.textRenderer,
                panelX + PADDING + 70, panelY + TAB_BAR_HEIGHT + 205,
                PANEL_WIDTH - PADDING * 2 - 100, 18,
                Text.of("确认延迟"));
        confirmDelayField.setPlaceholder(Text.literal("200").styled(s -> s.withColor(SUBTLE_COLOR)));
        confirmDelayField.setText(String.valueOf(config.getConfirmDelay()));
        confirmDelayField.setMaxLength(5);
        confirmDelayField.setChangedListener(text -> {
            try {
                int value = Integer.parseInt(text);
                autoEscapeManager.setConfirmDelay(value);
            } catch (NumberFormatException ignored) {
                // 忽略无效输入
            }
        });
        this.addDrawableChild(confirmDelayField);

        // 勾选框位置
        enableCheckboxX = panelX + PADDING;
        enableCheckboxY = panelY + TAB_BAR_HEIGHT + 35;
        autoConfirmCheckboxX = panelX + PADDING;
        autoConfirmCheckboxY = panelY + TAB_BAR_HEIGHT + 145;

        // 关闭按钮
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("关闭"),
                button -> this.close()
        ).dimensions(centerX - 30, panelY + PANEL_HEIGHT - 35, 60, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 1. 半透明背景填充
        context.fill(0, 0, this.width, this.height, 0xCC000000);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int panelX = centerX - PANEL_WIDTH / 2;
        int panelY = centerY - PANEL_HEIGHT / 2;

        // 2. 面板背景
        fillRoundedRect(context, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, PANEL_COLOR);

        // 3. 头部栏
        fillRoundedRect(context, panelX, panelY, PANEL_WIDTH, TAB_BAR_HEIGHT, HEADER_COLOR);

        // 4. 渲染所有 widgets（输入框、按钮等）
        super.render(context, mouseX, mouseY, delta);

        // 5. 渲染标签栏（在widgets之上）
        renderTabBar(context, panelX, panelY, mouseX, mouseY);

        // 6. 渲染启用勾选框
        renderEnableCheckbox(context, mouseX, mouseY);

        // 7. 渲染实时状态
        renderRealTimeStatus(context, panelX, panelY);

        // 8. 渲染设置标签
        renderSettingsLabels(context, panelX, panelY);

        // 9. 渲染自动确认勾选框
        renderAutoConfirmCheckbox(context, mouseX, mouseY);

        // 10. 渲染触发日志
        renderTriggerLog(context, panelX, panelY);
    }

    /**
     * 渲染标签栏
     */
    private void renderTabBar(DrawContext context, int panelX, int panelY, int mouseX, int mouseY) {
        int tabWidth = PANEL_WIDTH / 2;
        int tabY = panelY + 2;

        // 路径点管理标签（左侧，非活动）
        boolean leftTabHovered = mouseX >= panelX && mouseX <= panelX + tabWidth &&
                mouseY >= tabY && mouseY <= tabY + TAB_BAR_HEIGHT - 4;
        int leftTabBg = leftTabHovered ? 0xFF4A4A6A : HEADER_COLOR;
        fillRoundedRect(context, panelX, tabY, tabWidth, TAB_BAR_HEIGHT - 4, leftTabBg);
        drawCenteredText(context, "◄ 路径点管理", panelX + tabWidth / 2, tabY + 6, SUBTLE_COLOR);

        // 自动逃逸标签（右侧，活动）
        boolean rightTabHovered = mouseX >= panelX + tabWidth && mouseX <= panelX + PANEL_WIDTH &&
                mouseY >= tabY && mouseY <= tabY + TAB_BAR_HEIGHT - 4;
        int rightTabBg = rightTabHovered ? 0xFF8B4AF0 : ACCENT_COLOR;
        fillRoundedRect(context, panelX + tabWidth, tabY, tabWidth, TAB_BAR_HEIGHT - 4, rightTabBg);
        drawCenteredText(context, "自动逃逸 ►", panelX + tabWidth + tabWidth / 2, tabY + 6, 0xFFFFFFFF);
    }

    /**
     * 渲染启用勾选框
     */
    private void renderEnableCheckbox(DrawContext context, int mouseX, int mouseY) {
        boolean isEnabled = config.isEnabled();
        String label = isEnabled ? "☑ 启用自动逃险" : "☐ 启用自动逃险";
        int color = isEnabled ? SUCCESS_COLOR : SUBTLE_COLOR;

        // 检查悬浮状态
        int labelWidth = this.textRenderer.getWidth(label);
        boolean isHovered = mouseX >= enableCheckboxX && mouseX <= enableCheckboxX + labelWidth &&
                mouseY >= enableCheckboxY && mouseY <= enableCheckboxY + CHECKBOX_HEIGHT;

        if (isHovered) {
            context.fill(enableCheckboxX - 2, enableCheckboxY - 2, enableCheckboxX + labelWidth + 2, enableCheckboxY + CHECKBOX_HEIGHT + 2, 0x33FFFFFF);
        }

        drawText(context, label, enableCheckboxX, enableCheckboxY + 3, color);
    }

    /**
     * 渲染实时状态
     */
    private void renderRealTimeStatus(DrawContext context, int panelX, int panelY) {
        int statusY = panelY + TAB_BAR_HEIGHT + 55;

        // 分隔线
        context.fill(panelX + PADDING, statusY - 5, panelX + PANEL_WIDTH - PADDING, statusY - 3, SUBTLE_COLOR);
        drawText(context, "─── 实时状态 ───", panelX + PADDING, statusY - 2, SUBTLE_COLOR);

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            double playerX = client.player.getX();
            double playerY = client.player.getY();
            double playerZ = client.player.getZ();

            // 位置信息
            String positionText = String.format("当前位置: X: %.1f  Y: %.1f  Z: %.1f", playerX, playerY, playerZ);
            drawText(context, positionText, panelX + PADDING, statusY + 15, TEXT_COLOR);

            // 状态信息
            boolean isTriggered = playerY < config.getThreshold();
            String statusText = isTriggered ? "⚠️ 触发中" : "安全 ✅";
            int statusColor = isTriggered ? DANGER_COLOR : SUCCESS_COLOR;
            drawText(context, "状态: " + statusText, panelX + PADDING, statusY + 32, statusColor);
        } else {
            drawText(context, "当前位置: 未连接到服务器", panelX + PADDING, statusY + 15, SUBTLE_COLOR);
            drawText(context, "状态: 未知", panelX + PADDING, statusY + 32, SUBTLE_COLOR);
        }
    }

    /**
     * 渲染设置标签
     */
    private void renderSettingsLabels(DrawContext context, int panelX, int panelY) {
        int settingsY = panelY + TAB_BAR_HEIGHT + 80;

        // 分隔线
        context.fill(panelX + PADDING, settingsY - 5, panelX + PANEL_WIDTH - PADDING, settingsY - 3, SUBTLE_COLOR);
        drawText(context, "─── 设置 ───", panelX + PADDING, settingsY - 2, SUBTLE_COLOR);

        // 阈值标签
        drawText(context, "高度阈值:", panelX + PADDING, settingsY + 30, TEXT_COLOR);

        // 命令标签
        drawText(context, "执行命令:", panelX + PADDING, settingsY + 60, TEXT_COLOR);

        // 确认命令标签
        drawText(context, "确认命令:", panelX + PADDING, settingsY + 120, TEXT_COLOR);

        // 确认延迟标签
        drawText(context, "确认延迟:", panelX + PADDING, settingsY + 150, TEXT_COLOR);

        // 毫秒单位
        drawText(context, "ms", panelX + PADDING + 175, settingsY + 153, SUBTLE_COLOR);
    }

    /**
     * 渲染自动确认勾选框
     */
    private void renderAutoConfirmCheckbox(DrawContext context, int mouseX, int mouseY) {
        boolean isAutoConfirm = config.isAutoConfirm();
        String label = isAutoConfirm ? "☑ 自动确认" : "☐ 自动确认";
        int color = isAutoConfirm ? SUCCESS_COLOR : SUBTLE_COLOR;

        // 检查悬浮状态
        int labelWidth = this.textRenderer.getWidth(label);
        boolean isHovered = mouseX >= autoConfirmCheckboxX && mouseX <= autoConfirmCheckboxX + labelWidth &&
                mouseY >= autoConfirmCheckboxY && mouseY <= autoConfirmCheckboxY + CHECKBOX_HEIGHT;

        if (isHovered) {
            context.fill(autoConfirmCheckboxX - 2, autoConfirmCheckboxY - 2, autoConfirmCheckboxX + labelWidth + 2, autoConfirmCheckboxY + CHECKBOX_HEIGHT + 2, 0x33FFFFFF);
        }

        drawText(context, label, autoConfirmCheckboxX, autoConfirmCheckboxY + 3, color);
    }

    /**
     * 渲染触发日志
     */
    private void renderTriggerLog(DrawContext context, int panelX, int panelY) {
        int logY = panelY + TAB_BAR_HEIGHT + 230;

        // 分隔线
        context.fill(panelX + PADDING, logY - 5, panelX + PANEL_WIDTH - PADDING, logY - 3, SUBTLE_COLOR);
        drawText(context, "─── 触发日志 ───", panelX + PADDING, logY - 2, SUBTLE_COLOR);

        List<String> logEntries = autoEscapeManager.getLogEntries();

        if (logEntries.isEmpty()) {
            drawText(context, "暂无触发记录", panelX + PADDING, logY + 15, SUBTLE_COLOR);
        } else {
            // 计算最大滚动
            maxLogScroll = Math.max(0, logEntries.size() - LOG_VISIBLE_ENTRIES);

            // 显示最后的条目（考虑滚动）
            int startIndex = Math.max(0, logEntries.size() - LOG_VISIBLE_ENTRIES - logScrollOffset);
            int endIndex = Math.min(logEntries.size(), startIndex + LOG_VISIBLE_ENTRIES);

            int currentY = logY + 15;
            for (int i = startIndex; i < endIndex; i++) {
                String entry = logEntries.get(i);
                drawText(context, entry, panelX + PADDING, currentY, TEXT_COLOR);
                currentY += LOG_ENTRY_HEIGHT;
            }

            // 滚动指示器（如果有更多条目）
            if (logEntries.size() > LOG_VISIBLE_ENTRIES) {
                String scrollInfo = String.format("显示 %d-%d / %d", startIndex + 1, endIndex, logEntries.size());
                int scrollInfoWidth = this.textRenderer.getWidth(scrollInfo);
                drawText(context, scrollInfo, panelX + PANEL_WIDTH - PADDING - scrollInfoWidth, logY - 2, SUBTLE_COLOR);
            }
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        // 左键点击
        if (button == 0) {
            // 点击左侧标签（路径点管理）
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            int panelX = centerX - PANEL_WIDTH / 2;
            int panelY = centerY - PANEL_HEIGHT / 2;
            int tabWidth = PANEL_WIDTH / 2;
            int tabY = panelY + 2;

            if (mouseX >= panelX && mouseX <= panelX + tabWidth &&
                    mouseY >= tabY && mouseY <= tabY + TAB_BAR_HEIGHT - 4) {
                this.client.setScreen(new WaypointScreen());
                return true;
            }

            // 点击启用勾选框
            String enableLabel = config.isEnabled() ? "☑ 启用自动逃险" : "☐ 启用自动逃险";
            int enableLabelWidth = this.textRenderer.getWidth(enableLabel);
            if (mouseX >= enableCheckboxX && mouseX <= enableCheckboxX + enableLabelWidth &&
                    mouseY >= enableCheckboxY && mouseY <= enableCheckboxY + CHECKBOX_HEIGHT) {
                autoEscapeManager.toggle();
                return true;
            }

            // 点击自动确认勾选框
            String autoConfirmLabel = config.isAutoConfirm() ? "☑ 自动确认" : "☐ 自动确认";
            int autoConfirmLabelWidth = this.textRenderer.getWidth(autoConfirmLabel);
            if (mouseX >= autoConfirmCheckboxX && mouseX <= autoConfirmCheckboxX + autoConfirmLabelWidth &&
                    mouseY >= autoConfirmCheckboxY && mouseY <= autoConfirmCheckboxY + CHECKBOX_HEIGHT) {
                autoEscapeManager.setAutoConfirm(!config.isAutoConfirm());
                return true;
            }
        }

        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // 检查是否在日志区域内滚动
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int panelX = centerX - PANEL_WIDTH / 2;
        int panelY = centerY - PANEL_HEIGHT / 2;
        int logY = panelY + TAB_BAR_HEIGHT + 230;

        if (mouseX >= panelX + PADDING && mouseX <= panelX + PANEL_WIDTH - PADDING &&
                mouseY >= logY + 15 && mouseY <= logY + 15 + LOG_VISIBLE_ENTRIES * LOG_ENTRY_HEIGHT) {
            int scrollDelta = (int) (verticalAmount * 2);
            logScrollOffset = Math.max(0, Math.min(maxLogScroll, logScrollOffset - scrollDelta));
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void close() {
        this.client.setScreen(null);
    }

    // ==================== 绘制工具方法 ====================

    private void fillRoundedRect(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x + 2, y, x + width - 2, y + height, color);
        context.fill(x, y + 2, x + width, y + height - 2, color);
    }

    private void drawText(DrawContext context, String text, int x, int y, int color) {
        context.drawTextWithShadow(this.textRenderer, text, x, y, color);
    }

    private void drawCenteredText(DrawContext context, String text, int centerX, int y, int color) {
        int textWidth = this.textRenderer.getWidth(text);
        context.drawTextWithShadow(this.textRenderer, text, centerX - textWidth / 2, y, color);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}