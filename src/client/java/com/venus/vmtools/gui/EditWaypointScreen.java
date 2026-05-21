package com.venus.vmtools.gui;

import com.venus.vmtools.VMToolsClient;
import com.venus.vmtools.feature.waypoint.TeleportService;
import com.venus.vmtools.feature.waypoint.Waypoint;
import com.venus.vmtools.feature.waypoint.WaypointColor;
import com.venus.vmtools.feature.waypoint.WaypointGroup;
import com.venus.vmtools.feature.waypoint.WaypointManager;
import com.venus.vmtools.gui.component.ToastWidget;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;

/**
 * 路径点编辑界面（添加/编辑）- 支持选择分组
 */
public class EditWaypointScreen extends Screen {

    private static final int PANEL_WIDTH = 260;
    private static final int PANEL_HEIGHT = 230;
    private static final int PADDING = 12;

    // 颜色定义
    private static final int PANEL_COLOR = 0xFF2D2D44;
    private static final int HEADER_COLOR = 0xFF3D3D5C;
    private static final int TEXT_COLOR = 0xFFE0E0E0;
    private static final int ACCENT_COLOR = 0xFF7C3AED;
    private static final int SUBTLE_COLOR = 0xFF888888;
    private static final int HOVER_COLOR = 0xFF4A4A6A;

    private final WaypointScreen parentScreen;
    private final Waypoint editingWaypoint;
    private final WaypointGroup initialTargetGroup;
    private final boolean isEditMode;

    private TextFieldWidget nameField;
    private TextFieldWidget commandField;
    private WaypointColor selectedColor = WaypointColor.GREEN;
    private WaypointManager waypointManager;

    // 分组选择器
    private List<WaypointGroup> allGroups;
    private int selectedGroupIndex = 0;
    private WaypointGroup selectedGroup;

    // 分组选择器布局
    private int cyclerX, cyclerY, cyclerWidth;
    private static final int CYCLER_HEIGHT = 20;

    // 颜色选择器布局
    private int colorPickerX, colorPickerY;
    private static final int COLOR_SIZE = 22;
    private static final int COLOR_SPACING = 4;

    public EditWaypointScreen(WaypointScreen parentScreen, Waypoint editingWaypoint, WaypointGroup targetGroup) {
        super(Text.of(editingWaypoint == null ? "添加路径点" : "编辑路径点"));
        this.parentScreen = parentScreen;
        this.editingWaypoint = editingWaypoint;
        this.initialTargetGroup = targetGroup;
        this.isEditMode = editingWaypoint != null;
        this.waypointManager = VMToolsClient.getInstance().getWaypointManager();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int panelX = centerX - PANEL_WIDTH / 2;
        int panelY = centerY - PANEL_HEIGHT / 2;

        // 初始化分组列表
        allGroups = waypointManager.getGroups();
        if (isEditMode) {
            for (int i = 0; i < allGroups.size(); i++) {
                if (allGroups.get(i).getId().equals(editingWaypoint.getGroupId())) {
                    selectedGroupIndex = i;
                    break;
                }
            }
        } else if (initialTargetGroup != null) {
            for (int i = 0; i < allGroups.size(); i++) {
                if (allGroups.get(i).getId().equals(initialTargetGroup.getId())) {
                    selectedGroupIndex = i;
                    break;
                }
            }
        }
        selectedGroup = allGroups.isEmpty() ? null : allGroups.get(selectedGroupIndex);

        // 名称输入框
        nameField = new TextFieldWidget(this.textRenderer,
                panelX + PADDING + 50, panelY + 35,
                PANEL_WIDTH - PADDING * 2 - 50, 18,
                Text.of("路径点名称"));
        nameField.setPlaceholder(Text.literal("输入名称...").styled(s -> s.withColor(SUBTLE_COLOR)));
        if (isEditMode) {
            nameField.setText(editingWaypoint.getName());
        }
        this.addDrawableChild(nameField);

        // 命令输入框
        commandField = new TextFieldWidget(this.textRenderer,
                panelX + PADDING + 50, panelY + 65,
                PANEL_WIDTH - PADDING * 2 - 50, 18,
                Text.of("传送命令"));
        commandField.setPlaceholder(Text.literal("/res tp xxx").styled(s -> s.withColor(SUBTLE_COLOR)));
        if (isEditMode) {
            commandField.setText(editingWaypoint.getCommand());
        }
        this.addDrawableChild(commandField);

        // 分组选择器布局
        cyclerX = panelX + PADDING + 50;
        cyclerY = panelY + 95;
        cyclerWidth = PANEL_WIDTH - PADDING * 2 - 50;

        // 颜色选择器位置
        WaypointColor[] colors = WaypointColor.values();
        int totalColorsWidth = colors.length * (COLOR_SIZE + COLOR_SPACING) - COLOR_SPACING;
        colorPickerX = centerX - totalColorsWidth / 2;
        colorPickerY = panelY + 130;

        if (isEditMode) {
            selectedColor = editingWaypoint.getColor();
        }

        // 保存按钮
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal(isEditMode ? "保存修改" : "添加路径点"),
                button -> saveWaypoint()
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
        String title = isEditMode ? "编辑路径点" : "添加新路径点";
        drawCenteredText(context, title, centerX, panelY + 7, ACCENT_COLOR);

        // 标签
        drawText(context, "名称:", panelX + PADDING, panelY + 39, TEXT_COLOR);
        drawText(context, "命令:", panelX + PADDING, panelY + 69, TEXT_COLOR);
        drawText(context, "分组:", panelX + PADDING, panelY + 99, TEXT_COLOR);
        drawText(context, "颜色:", panelX + PADDING, panelY + 136, TEXT_COLOR);

        // 渲染分组选择器
        renderGroupCycler(context, mouseX, mouseY);

        // 渲染颜色选择器
        renderColorPicker(context, mouseX, mouseY);

        // 提示文字
        drawCenteredText(context, "命令示例: /res tp xxx, /home, /warp xxx",
                centerX, panelY + PANEL_HEIGHT - 55, SUBTLE_COLOR);

        super.render(context, mouseX, mouseY, delta);
    }

    /**
     * 渲染分组选择器（◀ 分组名 ▶）
     */
    private void renderGroupCycler(DrawContext context, int mouseX, int mouseY) {
        // 背景框
        fillRoundedRect(context, cyclerX, cyclerY, cyclerWidth, CYCLER_HEIGHT, 0xFF1E1E2E);

        if (allGroups.isEmpty()) {
            drawText(context, "无分组", cyclerX + cyclerWidth / 2 - 15, cyclerY + 4, SUBTLE_COLOR);
            return;
        }

        // 左箭头
        int arrowSize = 14;
        boolean leftHovered = mouseX >= cyclerX + 2 && mouseX <= cyclerX + arrowSize &&
                mouseY >= cyclerY + 2 && mouseY <= cyclerY + CYCLER_HEIGHT - 2;
        int leftColor = leftHovered ? TEXT_COLOR : SUBTLE_COLOR;
        drawText(context, "<", cyclerX + 4, cyclerY + 4, leftColor);

        // 分组名称（居中）
        String groupName = selectedGroup != null ?
                selectedGroup.getColor().getEmoji() + " " + selectedGroup.getName() : "无分组";
        int nameWidth = this.textRenderer.getWidth(groupName);
        int nameX = cyclerX + (cyclerWidth - nameWidth) / 2;
        drawText(context, groupName, nameX, cyclerY + 4, ACCENT_COLOR);

        // 右箭头
        boolean rightHovered = mouseX >= cyclerX + cyclerWidth - arrowSize && mouseX <= cyclerX + cyclerWidth - 2 &&
                mouseY >= cyclerY + 2 && mouseY <= cyclerY + CYCLER_HEIGHT - 2;
        int rightColor = rightHovered ? TEXT_COLOR : SUBTLE_COLOR;
        drawText(context, ">", cyclerX + cyclerWidth - 12, cyclerY + 4, rightColor);
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
    public boolean mouseClicked(Click click, boolean doubleClick) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        if (button == 0 && !allGroups.isEmpty()) {
            // 左箭头
            if (mouseX >= cyclerX + 2 && mouseX <= cyclerX + 16 &&
                    mouseY >= cyclerY + 2 && mouseY <= cyclerY + CYCLER_HEIGHT - 2) {
                selectedGroupIndex = (selectedGroupIndex - 1 + allGroups.size()) % allGroups.size();
                selectedGroup = allGroups.get(selectedGroupIndex);
                return true;
            }
            // 右箭头
            if (mouseX >= cyclerX + cyclerWidth - 16 && mouseX <= cyclerX + cyclerWidth - 2 &&
                    mouseY >= cyclerY + 2 && mouseY <= cyclerY + CYCLER_HEIGHT - 2) {
                selectedGroupIndex = (selectedGroupIndex + 1) % allGroups.size();
                selectedGroup = allGroups.get(selectedGroupIndex);
                return true;
            }
        }

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

        return super.mouseClicked(click, doubleClick);
    }

    /**
     * 保存路径点
     */
    private void saveWaypoint() {
        String name = nameField.getText().trim();
        String command = commandField.getText().trim();

        // 验证输入
        if (name.isEmpty()) {
            ToastWidget.showError("路径点名称不能为空");
            return;
        }
        if (command.isEmpty()) {
            ToastWidget.showError("传送命令不能为空");
            return;
        }

        // 验证命令格式
        if (!TeleportService.isValidCommand(command)) {
            ToastWidget.showError("无效的命令格式");
            return;
        }

        if (selectedGroup == null) {
            ToastWidget.showError("请先创建一个分组");
            return;
        }

        if (isEditMode) {
            // 编辑模式
            editingWaypoint.setName(name);
            editingWaypoint.setCommand(command);
            editingWaypoint.setColor(selectedColor);

            // 处理分组变更
            if (!editingWaypoint.getGroupId().equals(selectedGroup.getId())) {
                WaypointGroup oldGroup = waypointManager.findGroup(editingWaypoint.getGroupId());
                if (oldGroup != null) {
                    oldGroup.removeWaypoint(editingWaypoint.getId());
                }
                editingWaypoint.setGroupId(selectedGroup.getId());
                selectedGroup.addWaypoint(editingWaypoint);
            }

            waypointManager.save();
            ToastWidget.showSuccess("路径点已更新");
        } else {
            // 添加模式
            Waypoint newWaypoint = new Waypoint(name, command, selectedGroup.getId());
            newWaypoint.setColor(selectedColor);
            selectedGroup.addWaypoint(newWaypoint);
            waypointManager.save();
            ToastWidget.showSuccess("路径点已创建");
        }

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
