package com.venus.vmtools.gui;

import com.venus.vmtools.VMToolsClient;
import com.venus.vmtools.feature.waypoint.WaypointColor;
import com.venus.vmtools.feature.waypoint.WaypointGroup;
import com.venus.vmtools.feature.waypoint.WaypointManager;
import com.venus.vmtools.gui.component.ToastWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/**
 * 分组编辑界面（添加/编辑）- 自定义颜色选择器
 */
public class EditGroupScreen extends Screen {

    private static final int PANEL_WIDTH = 260;
    private static final int PANEL_HEIGHT = 160;
    private static final int PADDING = 12;

    // 颜色定义
    private static final int PANEL_COLOR = 0xFF2D2D44;
    private static final int HEADER_COLOR = 0xFF3D3D5C;
    private static final int TEXT_COLOR = 0xFFE0E0E0;
    private static final int ACCENT_COLOR = 0xFF7C3AED;
    private static final int SUBTLE_COLOR = 0xFF888888;
    private static final int HOVER_COLOR = 0xFF4A4A6A;

    private final WaypointScreen parentScreen;
    private final WaypointGroup editingGroup;
    private final boolean isEditMode;

    private TextFieldWidget nameField;
    private WaypointColor selectedColor = WaypointColor.BLUE;
    private WaypointManager waypointManager;

    // 颜色选择器布局
    private int colorPickerX, colorPickerY;
    private static final int COLOR_SIZE = 22;
    private static final int COLOR_SPACING = 4;

    public EditGroupScreen(WaypointScreen parentScreen, WaypointGroup editingGroup) {
        super(Text.of(editingGroup == null ? "添加分组" : "编辑分组"));
        this.parentScreen = parentScreen;
        this.editingGroup = editingGroup;
        this.isEditMode = editingGroup != null;
        this.waypointManager = VMToolsClient.getInstance().getWaypointManager();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int panelX = centerX - PANEL_WIDTH / 2;
        int panelY = centerY - PANEL_HEIGHT / 2;

        // 名称输入框
        nameField = new TextFieldWidget(this.textRenderer,
                panelX + PADDING + 60, panelY + 35,
                PANEL_WIDTH - PADDING * 2 - 60, 18,
                Text.of("分组名称"));
        nameField.setPlaceholder(Text.literal("输入分组名称...").styled(s -> s.withColor(SUBTLE_COLOR)));
        if (isEditMode) {
            nameField.setText(editingGroup.getName());
        }
        this.addDrawableChild(nameField);

        // 颜色选择器位置
        WaypointColor[] colors = WaypointColor.values();
        int totalColorsWidth = colors.length * (COLOR_SIZE + COLOR_SPACING) - COLOR_SPACING;
        colorPickerX = centerX - totalColorsWidth / 2;
        colorPickerY = panelY + 70;

        if (isEditMode) {
            selectedColor = editingGroup.getColor();
        }

        // 保存按钮
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal(isEditMode ? "保存修改" : "添加分组"),
                button -> saveGroup()
        ).dimensions(panelX + PADDING, panelY + PANEL_HEIGHT - 35, 100, 22).build());

        // 取消按钮
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("取消"),
                button -> this.close()
        ).dimensions(panelX + PANEL_WIDTH - PADDING - 60, panelY + PANEL_HEIGHT - 35, 60, 22).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 半透明背景
        context.fill(0, 0, this.width, this.height, 0xCC000000);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int panelX = centerX - PANEL_WIDTH / 2;
        int panelY = centerY - PANEL_HEIGHT / 2;

        // 面板背景
        fillRoundedRect(context, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, PANEL_COLOR);

        // 头部
        fillRoundedRect(context, panelX, panelY, PANEL_WIDTH, 25, HEADER_COLOR);
        String title = isEditMode ? "编辑分组" : "添加新分组";
        drawCenteredText(context, title, centerX, panelY + 7, ACCENT_COLOR);

        // 标签
        drawText(context, "名称:", panelX + PADDING, panelY + 39, TEXT_COLOR);
        drawText(context, "颜色:", panelX + PADDING, panelY + 76, TEXT_COLOR);

        // 渲染颜色选择器
        renderColorPicker(context, mouseX, mouseY);

        super.render(context, mouseX, mouseY, delta);
    }

    /**
     * 渲染颜色选择器
     */
    private void renderColorPicker(DrawContext context, int mouseX, int mouseY) {
        WaypointColor[] colors = WaypointColor.values();

        for (int i = 0; i < colors.length; i++) {
            int x = colorPickerX + i * (COLOR_SIZE + COLOR_SPACING);
            int y = colorPickerY;

            boolean isHovered = mouseX >= x && mouseX <= x + COLOR_SIZE &&
                    mouseY >= y && mouseY <= y + COLOR_SIZE;
            boolean isSelected = colors[i] == selectedColor;

            // 背景
            int bgColor = isHovered ? HOVER_COLOR : 0xFF1E1E2E;
            fillRoundedRect(context, x, y, COLOR_SIZE, COLOR_SIZE, bgColor);

            // 颜色方块（居中，留边距）
            int innerSize = COLOR_SIZE - 6;
            int innerX = x + 3;
            int innerY = y + 3;
            context.fill(innerX, innerY, innerX + innerSize, innerY + innerSize, colors[i].getARGB());

            // 选中边框
            if (isSelected) {
                drawBorder(context, x, y, COLOR_SIZE, COLOR_SIZE, 0xFFFFFFFF);
            }

            // 悬浮提示
            if (isHovered) {
                drawText(context, colors[i].getDisplayName(), x, y - 12, TEXT_COLOR);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        // 左键点击颜色选择器
        if (button == 0) {
            WaypointColor[] colors = WaypointColor.values();
            for (int i = 0; i < colors.length; i++) {
                int x = colorPickerX + i * (COLOR_SIZE + COLOR_SPACING);
                int y = colorPickerY;

                if (mouseX >= x && mouseX <= x + COLOR_SIZE &&
                        mouseY >= y && mouseY <= y + COLOR_SIZE) {
                    selectedColor = colors[i];
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * 保存分组
     */
    private void saveGroup() {
        String name = nameField.getText().trim();

        // 验证输入
        if (name.isEmpty()) {
            ToastWidget.showError("分组名称不能为空");
            return;
        }

        if (isEditMode) {
            // 编辑模式：更新现有分组
            editingGroup.setName(name);
            editingGroup.setColor(selectedColor);
            waypointManager.save();
            ToastWidget.showSuccess("分组已更新");
        } else {
            // 添加模式：创建新分组
            WaypointGroup newGroup = new WaypointGroup(name);
            newGroup.setColor(selectedColor);
            waypointManager.addGroup(newGroup);
            ToastWidget.showSuccess("分组已创建");
        }

        // 返回父界面
        this.close();
    }

    @Override
    public void close() {
        this.client.setScreen(parentScreen);
    }

    // ==================== 绘制工具方法 ====================

    private void fillRoundedRect(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x + 2, y, x + width - 2, y + height, color);
        context.fill(x, y + 2, x + width, y + height - 2, color);
    }

    private void drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 2, color);
        context.fill(x, y + height - 2, x + width, y + height, color);
        context.fill(x, y, x + 2, y + height, color);
        context.fill(x + width - 2, y, x + width, y + height, color);
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
