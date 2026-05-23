package com.venus.vmtools.gui;

import com.venus.vmtools.VMToolsClient;
import com.venus.vmtools.feature.waypoint.TeleportService;
import com.venus.vmtools.feature.waypoint.Waypoint;
import com.venus.vmtools.feature.waypoint.WaypointColor;
import com.venus.vmtools.feature.waypoint.WaypointGroup;
import com.venus.vmtools.feature.waypoint.WaypointManager;
import com.venus.vmtools.gui.component.ToastWidget;
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

    // 自动确认传送
    private int autoConfirmState = 0; // 0=跟随全局, 1=开启, 2=关闭
    private int autoConfirmX, autoConfirmY;
    private static final int AUTO_CONFIRM_HEIGHT = 14;

    // 命令补全
    private static final String[] COMMAND_SUGGESTIONS = {
            "res tp ", "warp ", "home", "tpa ", "tpask ", "ehome"
    };
    private java.util.List<String> suggestionList = new java.util.ArrayList<>();
    private int selectedSuggestionIndex = 0; // 键盘选中的索引
    private int suggestionPanelX, suggestionPanelY, suggestionPanelW;
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
        } else {
            commandField.setText("/"); // 新建时默认带 /
        }
        commandField.setChangedListener(text -> {
            // 更新命令补全建议
            updateSuggestion(text);
        });
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
            // 加载自动确认状态
            Boolean wpAC = editingWaypoint.getAutoConfirm();
            autoConfirmState = (wpAC != null && wpAC) ? 1 : 0;
        }

        // 自动确认勾选框位置
        autoConfirmX = panelX + PADDING;
        autoConfirmY = panelY + 160;

        // 保存按钮
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal(isEditMode ? "保存修改" : "添加路径点"),
                button -> saveWaypoint()
        ).dimensions(panelX + PADDING, panelY + PANEL_HEIGHT - 35, 100, 20).build());

        // 取消按钮
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("取消"),
                button -> this.close()
        ).dimensions(panelX + PANEL_WIDTH - PADDING - 60, panelY + PANEL_HEIGHT - 35, 60, 20).build());
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

        // 3. 头部栏 + 标题
        fillRoundedRect(context, panelX, panelY, PANEL_WIDTH, 25, HEADER_COLOR);
        String title = isEditMode ? "编辑路径点" : "添加新路径点";
        drawCenteredText(context, title, centerX, panelY + 7, ACCENT_COLOR);

        // 4. 标签
        drawText(context, "名称:", panelX + PADDING, panelY + 39, TEXT_COLOR);
        drawText(context, "命令:", panelX + PADDING, panelY + 69, TEXT_COLOR);
        drawText(context, "分组:", panelX + PADDING, panelY + 99, TEXT_COLOR);
        drawText(context, "颜色:", panelX + PADDING, panelY + 136, TEXT_COLOR);

        // 渲染自动确认勾选框
        String acLabel = getAutoConfirmLabel();
        int acColor = getAutoConfirmColor();
        context.drawTextWithShadow(this.textRenderer, acLabel, autoConfirmX, autoConfirmY + 3, acColor);

        // 提示文字
        drawCenteredText(context, "命令示例: /res tp xxx, /home, /warp xxx",
                centerX, panelY + PANEL_HEIGHT - 55, SUBTLE_COLOR);

        // 6. 渲染所有 widgets（按钮、文本框等）
        super.render(context, mouseX, mouseY, delta);

        // 7. 渲染分组选择器
        renderGroupCycler(context, mouseX, mouseY);

        // 8. 渲染颜色选择器
        renderColorPicker(context, mouseX, mouseY);

        // 9. 自动确认悬浮提示（置顶）
        int acLabelWidth = this.textRenderer.getWidth(acLabel);
        if (mouseX >= autoConfirmX && mouseX <= autoConfirmX + acLabelWidth &&
                mouseY >= autoConfirmY && mouseY <= autoConfirmY + AUTO_CONFIRM_HEIGHT) {
            String tip1 = "自动确认传送";
            String tip2 = "请务必保证目的地安全";
            int tipX = autoConfirmX + acLabelWidth + 10;
            int tipY = autoConfirmY;
            int tipW = Math.max(this.textRenderer.getWidth(tip1), this.textRenderer.getWidth(tip2)) + 10;
            context.fill(tipX - 3, tipY - 2, tipX + tipW + 3, tipY + 32, 0xE0000000);
            context.fill(tipX - 3, tipY - 2, tipX + tipW + 3, tipY - 1, 0xFF7C3AED);
            context.drawTextWithShadow(this.textRenderer, tip1, tipX + 2, tipY + 1, 0xFFFFCC00);
            context.drawTextWithShadow(this.textRenderer, tip2, tipX + 2, tipY + 14, 0xFFFF6666);
        }

        // 10. 命令补全面板（最上层，必须最后渲染）
        if (!suggestionList.isEmpty() && commandField.isFocused()) {
            int itemHeight = 16;
            int sugX = panelX + PADDING + 50;
            int sugY = panelY + 82;
            int maxW = 0;
            for (String s : suggestionList) {
                maxW = Math.max(maxW, this.textRenderer.getWidth(s));
            }
            int sugW = Math.max(100, maxW + 20);
            int sugH = suggestionList.size() * itemHeight + 4;
            context.fill(sugX - 2, sugY - 2, sugX + sugW + 2, sugY + sugH + 2, 0xF0000000);
            context.fill(sugX - 2, sugY - 2, sugX + sugW + 2, sugY - 1, ACCENT_COLOR);
            for (int i = 0; i < suggestionList.size(); i++) {
                int itemY = sugY + 2 + i * itemHeight;
                boolean isSelected = (i == selectedSuggestionIndex);
                boolean isHovered = mouseX >= sugX && mouseX <= sugX + sugW &&
                        mouseY >= itemY && mouseY <= itemY + itemHeight;
                if (isSelected) {
                    context.fill(sugX, itemY, sugX + sugW, itemY + itemHeight, 0xFF3D3D5C);
                } else if (isHovered) {
                    context.fill(sugX, itemY, sugX + sugW, itemY + itemHeight, 0xFF2D2D44);
                }
                int textColor = isSelected ? 0xFF4ADE80 : 0xFFCCCCCC;
                drawText(context, suggestionList.get(i), sugX + 6, itemY + 2, textColor);
                if (isSelected) {
                    int tabW = this.textRenderer.getWidth("[Tab]");
                    drawText(context, "[Tab]", sugX + sugW - tabW - 4, itemY + 2, 0xFF888888);
                }
            }
        }
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

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

        // 左键点击命令补全面板
        if (button == 0 && !suggestionList.isEmpty() && commandField.isFocused()) {
            int itemHeight = 16;
            int centerX = this.width / 2;
            int panelX = centerX - PANEL_WIDTH / 2;
            int panelY = (this.height / 2) - PANEL_HEIGHT / 2;
            int sugX = panelX + PADDING + 50;
            int sugY = panelY + 52; // 命令输入框上方

            for (int i = 0; i < suggestionList.size(); i++) {
                int itemY = sugY + 2 + i * itemHeight;
                if (mouseX >= sugX && mouseX <= sugX + 200 &&
                        mouseY >= itemY && mouseY <= itemY + itemHeight) {
                    acceptSuggestion(suggestionList.get(i));
                    return true;
                }
            }
        }

        // 左键点击自动确认勾选框
        if (button == 0) {
            String acLbl = getAutoConfirmLabel();
            int acW = this.textRenderer.getWidth(acLbl);
            if (mouseX >= autoConfirmX && mouseX <= autoConfirmX + acW &&
                    mouseY >= autoConfirmY && mouseY <= autoConfirmY + AUTO_CONFIRM_HEIGHT) {
                autoConfirmState = autoConfirmState == 0 ? 1 : 0;
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
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
            // 保存自动确认设置
            editingWaypoint.setAutoConfirm(autoConfirmState == 1);

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
            newWaypoint.setAutoConfirm(autoConfirmState == 1);
            selectedGroup.addWaypoint(newWaypoint);
            waypointManager.save();
            ToastWidget.showSuccess("路径点已创建");
        }

        this.close();
    }

    private String getAutoConfirmLabel() {
        return autoConfirmState == 0 ? "☐ 自动确认" : "☑ 自动确认";
    }

    private int getAutoConfirmColor() {
        return autoConfirmState == 0 ? 0xFF888888 : 0xFF4ADE80;
    }

    /**
     * 更新命令补全建议列表
     */
    private void updateSuggestion(String text) {
        suggestionList.clear();
        selectedSuggestionIndex = 0;
        if (text == null || text.isEmpty()) return;

        String input = text.startsWith("/") ? text.substring(1) : text;
        if (input.isEmpty()) return;

        String lowerInput = input.toLowerCase();
        for (String cmd : COMMAND_SUGGESTIONS) {
            if (cmd.toLowerCase().startsWith(lowerInput) && !cmd.equalsIgnoreCase(input)) {
                suggestionList.add(cmd);
            }
        }
    }

    /**
     * 接受选中的补全建议
     */
    private void acceptSuggestion(String suggestion) {
        if (suggestion != null) {
            String text = commandField.getText();
            if (text.startsWith("/")) {
                commandField.setText("/" + suggestion);
            } else {
                commandField.setText(suggestion);
            }
            suggestionList.clear();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!suggestionList.isEmpty()) {
            // 上箭头 (GLFW_KEY_UP = 265)
            if (keyCode == 265) {
                selectedSuggestionIndex = (selectedSuggestionIndex - 1 + suggestionList.size()) % suggestionList.size();
                return true;
            }
            // 下箭头 (GLFW_KEY_DOWN = 264)
            if (keyCode == 264) {
                selectedSuggestionIndex = (selectedSuggestionIndex + 1) % suggestionList.size();
                return true;
            }
            // Tab 键接受选中项 (GLFW_KEY_TAB = 258)
            if (keyCode == 258) {
                acceptSuggestion(suggestionList.get(selectedSuggestionIndex));
                return true;
            }
            // Enter 键也接受选中项 (GLFW_KEY_ENTER = 257)
            if (keyCode == 257) {
                acceptSuggestion(suggestionList.get(selectedSuggestionIndex));
                return true;
            }
            // Esc 键关闭补全面板 (GLFW_KEY_ESCAPE = 256)
            if (keyCode == 256) {
                suggestionList.clear();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
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
