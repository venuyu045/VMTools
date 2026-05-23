package com.venus.vmtools.gui;

import com.venus.vmtools.VMToolsClient;
import com.venus.vmtools.feature.waypoint.WaypointGroup;
import com.venus.vmtools.feature.waypoint.WaypointIO;
import com.venus.vmtools.feature.waypoint.WaypointManager;
import com.venus.vmtools.gui.component.ToastWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

/**
 * 导入确认界面
 */
public class ImportConfirmScreen extends Screen {

    private static final int PANEL_WIDTH = 280;
    private static final int PANEL_HEIGHT = 180;

    // 颜色
    private static final int PANEL_COLOR = 0xFF2D2D44;
    private static final int HEADER_COLOR = 0xFF3D3D5C;
    private static final int TEXT_COLOR = 0xFFE0E0E0;
    private static final int ACCENT_COLOR = 0xFF7C3AED;
    private static final int SUCCESS_COLOR = 0xFF4ADE80;
    private static final int WARNING_COLOR = 0xFFFBBF24;
    private static final int DANGER_COLOR = 0xFFF87171;

    private final Screen parent;
    private final List<WaypointGroup> importGroups;
    private final int totalWaypoints;
    private int importMode = 0; // 0=追加, 1=替换

    public ImportConfirmScreen(Screen parent, List<WaypointGroup> importGroups) {
        super(Text.of("确认导入"));
        this.parent = parent;
        this.importGroups = importGroups;
        this.totalWaypoints = importGroups.stream()
                .mapToInt(WaypointGroup::getWaypointCount)
                .sum();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int panelX = centerX - PANEL_WIDTH / 2;
        int panelY = centerY - PANEL_HEIGHT / 2;

        int buttonY = panelY + PANEL_HEIGHT - 35;
        int buttonWidth = 80;

        // 追加模式按钮
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("追加导入"),
                button -> {
                    importMode = 0;
                    doImport();
                }
        ).dimensions(centerX - buttonWidth - 10, buttonY, buttonWidth, 25).build());

        // 替换模式按钮
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("替换全部"),
                button -> {
                    importMode = 1;
                    doImport();
                }
        ).dimensions(centerX + 10, buttonY, buttonWidth, 25).build());

        // 取消按钮
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("取消"),
                button -> close()
        ).dimensions(centerX - 30, buttonY + 30, 60, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
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
        drawCenteredText(context, "确认导入", centerX, panelY + 7, TEXT_COLOR);

        int contentY = panelY + 35;

        // 导入预览
        drawCenteredText(context, "检测到以下数据：", centerX, contentY, TEXT_COLOR);
        contentY += 25;

        // 统计信息
        drawCenteredText(context, "分组数量: " + importGroups.size(), centerX, contentY, SUCCESS_COLOR);
        contentY += 18;
        drawCenteredText(context, "路径点数量: " + totalWaypoints, centerX, contentY, SUCCESS_COLOR);
        contentY += 25;

        // 分组列表
        drawCenteredText(context, "分组列表:", centerX, contentY, WARNING_COLOR);
        contentY += 18;

        for (int i = 0; i < Math.min(importGroups.size(), 3); i++) {
            WaypointGroup group = importGroups.get(i);
            String text = "  " + group.getColor().getEmoji() + " " + group.getName() +
                    " (" + group.getWaypointCount() + "个)";
            drawCenteredText(context, text, centerX, contentY, TEXT_COLOR);
            contentY += 15;
        }

        if (importGroups.size() > 3) {
            drawCenteredText(context, "... 还有 " + (importGroups.size() - 3) + " 个分组",
                    centerX, contentY, 0xFF888888);
            contentY += 15;
        }

        contentY += 10;

        // 导入模式说明
        drawCenteredText(context, "追加导入: 保留现有数据，添加新路径点", centerX, contentY, 0xFF888888);
        contentY += 15;
        drawCenteredText(context, "替换全部: 清空现有数据，只保留导入的数据", centerX, contentY, DANGER_COLOR);

        // 渲染子组件
        super.render(context, mouseX, mouseY, delta);
    }

    private void doImport() {
        WaypointManager manager = VMToolsClient.getInstance().getWaypointManager();

        if (importMode == 1) {
            // 替换模式：清空现有数据
            manager.getGroups().clear();
        }

        // 导入数据
        int count = manager.importWaypoints(importGroups);

        ToastWidget.showSuccess("成功导入 " + count + " 个路径点");
        close();
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // 绘制工具
    private void fillRoundedRect(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x + 2, y, x + width - 2, y + height, color);
        context.fill(x, y + 2, x + width, y + height - 2, color);
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
