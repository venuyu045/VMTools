package com.venus.vmtools.gui;

import com.venus.vmtools.VMToolsClient;
import com.venus.vmtools.feature.waypoint.Waypoint;
import com.venus.vmtools.feature.waypoint.WaypointGroup;
import com.venus.vmtools.feature.waypoint.WaypointIO;
import com.venus.vmtools.feature.waypoint.WaypointManager;
import com.venus.vmtools.gui.component.ToastWidget;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.awt.Desktop;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 路径点管理主界面 - 简洁列表布局
 */
public class WaypointScreen extends Screen {

    private static final int PANEL_WIDTH = 300;
    private static final int BUTTON_HEIGHT = 20;
    private static final int PADDING = 8;

    // 颜色定义
    private static final int PANEL_COLOR = 0xFF2D2D44;
    private static final int HEADER_COLOR = 0xFF3D3D5C;
    private static final int TEXT_COLOR = 0xFFE0E0E0;
    private static final int ACCENT_COLOR = 0xFF7C3AED;
    private static final int HOVER_COLOR = 0xFF4A4A6A;
    private static final int SUCCESS_COLOR = 0xFF4ADE80;
    private static final int DANGER_COLOR = 0xFFF87171;
    private static final int SUBTLE_COLOR = 0xFF888888;

    // 菜单类型
    private static final int MENU_TYPE_WAYPOINT = 0;
    private static final int MENU_TYPE_GROUP = 1;

    private WaypointManager waypointManager;
    private List<WaypointGroup> groups;
    private TextFieldWidget searchField;
    private String searchKeyword = "";

    // 右键菜单状态
    private boolean showContextMenu = false;
    private int contextMenuX, contextMenuY;
    private int contextMenuType = MENU_TYPE_WAYPOINT;
    private Waypoint contextMenuWaypoint;
    private WaypointGroup contextMenuGroup;

    // 动态布局变量
    private int panelTop;
    private int panelHeight;
    private int panelX;

    public WaypointScreen() {
        super(Text.of("VMTools - 路径点管理"));
        this.waypointManager = VMToolsClient.getInstance().getWaypointManager();
        this.groups = waypointManager.getGroups();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int screenHeight = this.height;

        // 面板高度动态计算
        panelHeight = screenHeight - 80;
        if (panelHeight > 250) panelHeight = 250;
        if (panelHeight < 120) panelHeight = 120;

        panelX = centerX - PANEL_WIDTH / 2;
        panelTop = 25;

        int buttonY = panelTop + panelHeight + 8;

        // 搜索框
        searchField = new TextFieldWidget(this.textRenderer,
                panelX + PADDING, panelTop + 25,
                PANEL_WIDTH - PADDING * 2, 18,
                Text.of("搜索..."));
        searchField.setPlaceholder(Text.literal("搜索路径点...").styled(style -> style.withColor(SUBTLE_COLOR)));
        searchField.setChangedListener(this::onSearchChanged);
        this.addDrawableChild(searchField);

        // 底部按钮
        int btnWidth = 70;
        int btnSpacing = 5;
        int totalBtnWidth = btnWidth * 4 + btnSpacing * 3;
        int btnStartX = centerX - totalBtnWidth / 2;

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("+ 路径点"),
                button -> openAddWaypointScreen()
        ).dimensions(btnStartX, buttonY, btnWidth, BUTTON_HEIGHT).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("+ 分组"),
                button -> openAddGroupScreen()
        ).dimensions(btnStartX + btnWidth + btnSpacing, buttonY, btnWidth, BUTTON_HEIGHT).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("导入"),
                button -> importWaypoints()
        ).dimensions(btnStartX + (btnWidth + btnSpacing) * 2, buttonY, btnWidth, BUTTON_HEIGHT).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("导出"),
                button -> exportWaypoints()
        ).dimensions(btnStartX + (btnWidth + btnSpacing) * 3, buttonY, btnWidth, BUTTON_HEIGHT).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 渲染主面板
        renderPanel(context, panelX, panelTop, mouseX, mouseY);

        // 渲染标题
        drawCenteredText(context, "VMTools - 路径点管理", this.width / 2, 8, ACCENT_COLOR);

        // 渲染底部提示
        drawCenteredText(context, "左键展开/传送  |  右键菜单  |  ESC 关闭",
                this.width / 2, this.height - 15, SUBTLE_COLOR);

        // 渲染右键菜单（最上层）
        if (showContextMenu) {
            renderContextMenu(context, mouseX, mouseY);
        }

        // 渲染 Toast 通知
        ToastWidget.render(context, this.width, this.height);

        super.render(context, mouseX, mouseY, delta);
    }

    /**
     * 渲染主面板
     */
    private void renderPanel(DrawContext context, int x, int y, int mouseX, int mouseY) {
        // 面板背景
        fillRoundedRect(context, x, y, PANEL_WIDTH, panelHeight, PANEL_COLOR);

        // 头部
        fillRoundedRect(context, x, y, PANEL_WIDTH, 22, HEADER_COLOR);
        drawText(context, "路径点列表", x + PADDING, y + 6, TEXT_COLOR);

        int itemY = y + 48;
        int groupHeight = 22;
        int wpHeight = 22;
        int teleportBtnWidth = 40;

        // 渲染分组和路径点
        for (WaypointGroup group : groups) {
            // 检查分组是否有匹配的路径点
            if (!searchKeyword.isEmpty()) {
                boolean hasMatch = group.getWaypoints().stream()
                        .anyMatch(wp -> wp.getName().toLowerCase().contains(searchKeyword.toLowerCase()) ||
                                wp.getCommand().toLowerCase().contains(searchKeyword.toLowerCase()));
                if (!hasMatch) continue;
            }

            // 超出面板底部
            if (itemY + groupHeight > y + panelHeight - 4) break;

            boolean isGroupHovered = mouseX >= x + 4 && mouseX <= x + PANEL_WIDTH - 4 &&
                    mouseY >= itemY && mouseY <= itemY + groupHeight;

            // 分组行
            int groupBg = isGroupHovered ? HOVER_COLOR : 0xFF252540;
            fillRoundedRect(context, x + 4, itemY, PANEL_WIDTH - 8, groupHeight, groupBg);

            // 分组图标和名称
            String expandIcon = group.isExpanded() ? "▼" : "▶";
            drawText(context, expandIcon + " " + group.getColor().getEmoji() + " " + group.getName(),
                    x + PADDING, itemY + 4, TEXT_COLOR);

            // 路径点数量
            String count = "[" + group.getWaypointCount() + "]";
            drawText(context, count, x + PANEL_WIDTH - 30, itemY + 4, SUBTLE_COLOR);

            itemY += groupHeight + 2;

            // 展开的路径点
            if (group.isExpanded()) {
                for (Waypoint wp : group.getWaypoints()) {
                    // 搜索过滤
                    if (!searchKeyword.isEmpty() &&
                            !wp.getName().toLowerCase().contains(searchKeyword.toLowerCase()) &&
                            !wp.getCommand().toLowerCase().contains(searchKeyword.toLowerCase())) {
                        continue;
                    }

                    // 超出面板底部
                    if (itemY + wpHeight > y + panelHeight - 4) break;

                    boolean isWpHovered = mouseX >= x + 20 && mouseX <= x + PANEL_WIDTH - teleportBtnWidth - 12 &&
                            mouseY >= itemY && mouseY <= itemY + wpHeight;

                    // 路径点行背景
                    int wpBg = isWpHovered ? 0xFF3A3A55 : 0x00000000;
                    if (wpBg != 0x00000000) {
                        fillRoundedRect(context, x + 20, itemY, PANEL_WIDTH - 24 - teleportBtnWidth - 4, wpHeight, wpBg);
                    }

                    // 路径点名称
                    drawText(context, wp.getColor().getEmoji() + " " + wp.getName(),
                            x + PADDING + 16, itemY + 4, TEXT_COLOR);

                    // 传送按钮
                    int tpBtnX = x + PANEL_WIDTH - teleportBtnWidth - 8;
                    boolean isTpHovered = mouseX >= tpBtnX && mouseX <= tpBtnX + teleportBtnWidth &&
                            mouseY >= itemY + 2 && mouseY <= itemY + wpHeight - 2;
                    int tpBtnColor = isTpHovered ? 0xFF6D28D9 : ACCENT_COLOR;
                    fillRoundedRect(context, tpBtnX, itemY + 2, teleportBtnWidth, wpHeight - 4, tpBtnColor);
                    drawCenteredText(context, "传送", tpBtnX + teleportBtnWidth / 2, itemY + 5, 0xFFFFFFFF);

                    itemY += wpHeight + 2;
                }
            }
        }

        // 空状态提示
        if (groups.isEmpty()) {
            drawCenteredText(context, "暂无路径点，点击下方按钮添加",
                    x + PANEL_WIDTH / 2, y + panelHeight / 2, SUBTLE_COLOR);
        }
    }

    /**
     * 渲染右键菜单
     */
    private void renderContextMenu(DrawContext context, int mouseX, int mouseY) {
        int menuWidth = 90;
        int menuItemHeight = 20;
        String[] labels;
        int[] colors;

        if (contextMenuType == MENU_TYPE_GROUP) {
            labels = new String[]{"展开/折叠", "编辑", "删除"};
            colors = new int[]{TEXT_COLOR, TEXT_COLOR, DANGER_COLOR};
        } else {
            labels = new String[]{"传送", "编辑", "复制", "删除"};
            colors = new int[]{SUCCESS_COLOR, TEXT_COLOR, TEXT_COLOR, DANGER_COLOR};
        }

        int menuItems = labels.length;
        int menuHeight = menuItemHeight * menuItems + 4;

        // 确保菜单不超出屏幕
        int menuX = Math.min(contextMenuX, this.width - menuWidth - 5);
        int menuY = Math.min(contextMenuY, this.height - menuHeight - 5);

        // 菜单背景
        fillRoundedRect(context, menuX, menuY, menuWidth, menuHeight, 0xFF1E1E2E);
        context.fill(menuX + 1, menuY + 1, menuX + menuWidth - 1, menuY + menuHeight - 1, 0xFF2D2D44);

        for (int i = 0; i < menuItems; i++) {
            int itemY = menuY + 2 + i * menuItemHeight;
            boolean isHovered = mouseX >= menuX && mouseX <= menuX + menuWidth &&
                    mouseY >= itemY && mouseY <= itemY + menuItemHeight;

            if (isHovered) {
                context.fill(menuX + 2, itemY, menuX + menuWidth - 2, itemY + menuItemHeight, HOVER_COLOR);
            }

            drawText(context, labels[i], menuX + PADDING + 2, itemY + 4, colors[i]);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        // 右键菜单处理
        if (showContextMenu) {
            if (button == 0) { // 左键点击菜单项
                handleContextMenuClick((int) mouseX, (int) mouseY);
                showContextMenu = false;
                return true;
            } else {
                // 其他点击关闭菜单
                showContextMenu = false;
            }
        }

        // 左键和右键都处理
        if (button != 0 && button != 1) return super.mouseClicked(click, doubleClick);

        // 面板内点击
        if (mouseX >= panelX && mouseX <= panelX + PANEL_WIDTH) {
            int itemY = panelTop + 48;
            int groupHeight = 22;
            int wpHeight = 22;
            int teleportBtnWidth = 40;

            for (WaypointGroup group : groups) {
                // 搜索过滤
                if (!searchKeyword.isEmpty()) {
                    boolean hasMatch = group.getWaypoints().stream()
                            .anyMatch(wp -> wp.getName().toLowerCase().contains(searchKeyword.toLowerCase()) ||
                                    wp.getCommand().toLowerCase().contains(searchKeyword.toLowerCase()));
                    if (!hasMatch) continue;
                }

                // 超出面板底部
                if (itemY + groupHeight > panelTop + panelHeight - 4) break;

                // 点击分组行
                if (mouseY >= itemY && mouseY <= itemY + groupHeight) {
                    if (button == 0) { // 左键展开/折叠
                        group.toggleExpanded();
                    } else if (button == 1) { // 右键显示分组菜单
                        showContextMenu = true;
                        contextMenuType = MENU_TYPE_GROUP;
                        contextMenuX = (int) mouseX;
                        contextMenuY = (int) mouseY;
                        contextMenuGroup = group;
                        contextMenuWaypoint = null;
                    }
                    return true;
                }

                itemY += groupHeight + 2;

                // 展开的路径点
                if (group.isExpanded()) {
                    for (Waypoint wp : group.getWaypoints()) {
                        // 搜索过滤
                        if (!searchKeyword.isEmpty() &&
                                !wp.getName().toLowerCase().contains(searchKeyword.toLowerCase()) &&
                                !wp.getCommand().toLowerCase().contains(searchKeyword.toLowerCase())) {
                            continue;
                        }

                        // 超出面板底部
                        if (itemY + wpHeight > panelTop + panelHeight - 4) break;

                        // 点击路径点行
                        if (mouseY >= itemY && mouseY <= itemY + wpHeight) {
                            // 检查是否点击了传送按钮
                            int tpBtnX = panelX + PANEL_WIDTH - teleportBtnWidth - 8;
                            if (mouseX >= tpBtnX && mouseX <= tpBtnX + teleportBtnWidth) {
                                // 点击传送按钮
                                executeTeleport(wp);
                                return true;
                            }

                            // 左键点击路径点名称 - 传送
                            if (button == 0 && mouseX < tpBtnX) {
                                executeTeleport(wp);
                                return true;
                            }

                            // 右键点击路径点 - 显示菜单
                            if (button == 1) {
                                showContextMenu = true;
                                contextMenuType = MENU_TYPE_WAYPOINT;
                                contextMenuX = (int) mouseX;
                                contextMenuY = (int) mouseY;
                                contextMenuWaypoint = wp;
                                contextMenuGroup = group;
                                return true;
                            }
                        }

                        itemY += wpHeight + 2;
                    }
                }
            }
        }

        // 点击面板外关闭菜单
        showContextMenu = false;
        return super.mouseClicked(click, doubleClick);
    }

    /**
     * 处理右键菜单点击
     */
    private void handleContextMenuClick(int mouseX, int mouseY) {
        int menuWidth = 90;
        int menuItemHeight = 20;
        int menuX = Math.min(contextMenuX, this.width - menuWidth - 5);

        int menuItems;
        if (contextMenuType == MENU_TYPE_GROUP) {
            menuItems = 3;
        } else {
            menuItems = 4;
        }

        int menuHeight = menuItemHeight * menuItems + 4;
        int menuY = Math.min(contextMenuY, this.height - menuHeight - 5);

        for (int i = 0; i < menuItems; i++) {
            int itemY = menuY + 2 + i * menuItemHeight;
            if (mouseX >= menuX && mouseX <= menuX + menuWidth &&
                    mouseY >= itemY && mouseY <= itemY + menuItemHeight) {

                if (contextMenuType == MENU_TYPE_GROUP) {
                    // 分组菜单
                    switch (i) {
                        case 0: // 展开/折叠
                            if (contextMenuGroup != null) {
                                contextMenuGroup.toggleExpanded();
                            }
                            break;
                        case 1: // 编辑
                            if (contextMenuGroup != null) {
                                openEditGroupScreen(contextMenuGroup);
                            }
                            break;
                        case 2: // 删除
                            if (contextMenuGroup != null) {
                                confirmDeleteGroup(contextMenuGroup);
                            }
                            break;
                    }
                } else {
                    // 路径点菜单
                    switch (i) {
                        case 0: // 传送
                            executeTeleport(contextMenuWaypoint);
                            break;
                        case 1: // 编辑
                            openEditWaypointScreen(contextMenuWaypoint, contextMenuGroup);
                            break;
                        case 2: // 复制
                            copyWaypoint(contextMenuWaypoint, contextMenuGroup);
                            break;
                        case 3: // 删除
                            confirmDeleteWaypoint(contextMenuWaypoint, contextMenuGroup);
                            break;
                    }
                }
                break;
            }
        }
    }

    /**
     * 确认删除分组
     */
    private void confirmDeleteGroup(WaypointGroup group) {
        if (group == null) return;
        this.client.setScreen(ConfirmScreen.createDeleteConfirm(
                this,
                "分组 \"" + group.getName() + "\"",
                () -> {
                    waypointManager.removeGroup(group.getId());
                    this.groups = waypointManager.getGroups();
                    ToastWidget.showSuccess("分组已删除");
                }
        ));
    }

    /**
     * 确认删除路径点
     */
    private void confirmDeleteWaypoint(Waypoint waypoint, WaypointGroup group) {
        if (waypoint == null || group == null) return;
        this.client.setScreen(ConfirmScreen.createDeleteConfirm(
                this,
                "路径点 \"" + waypoint.getName() + "\"",
                () -> {
                    waypointManager.removeWaypoint(group.getId(), waypoint.getId());
                    ToastWidget.showSuccess("路径点已删除");
                }
        ));
    }

    /**
     * 执行传送
     */
    private void executeTeleport(Waypoint waypoint) {
        if (waypoint != null) {
            boolean success = waypointManager.executeTeleport(waypoint);
            if (success) {
                ToastWidget.showSuccess("已发送: " + waypoint.getCommand());
                this.close();
            } else {
                ToastWidget.showError("传送失败：未连接到服务器");
            }
        }
    }

    /**
     * 复制路径点
     */
    private void copyWaypoint(Waypoint waypoint, WaypointGroup group) {
        if (waypoint != null && group != null) {
            Waypoint copy = new Waypoint(
                    waypoint.getName() + " (副本)",
                    waypoint.getCommand(),
                    group.getId()
            );
            copy.setColor(waypoint.getColor());
            group.addWaypoint(copy);
            waypointManager.save();
            ToastWidget.showSuccess("路径点已复制");
        }
    }

    /**
     * 搜索变更回调
     */
    private void onSearchChanged(String keyword) {
        this.searchKeyword = keyword;
    }

    /**
     * 打开添加路径点界面
     */
    private void openAddWaypointScreen() {
        WaypointGroup targetGroup = groups.isEmpty() ? null : groups.get(0);
        this.client.setScreen(new EditWaypointScreen(this, null, targetGroup));
    }

    /**
     * 打开编辑路径点界面
     */
    private void openEditWaypointScreen(Waypoint waypoint, WaypointGroup group) {
        if (waypoint != null) {
            this.client.setScreen(new EditWaypointScreen(this, waypoint, group));
        }
    }

    /**
     * 打开编辑分组界面
     */
    private void openEditGroupScreen(WaypointGroup group) {
        if (group != null) {
            this.client.setScreen(new EditGroupScreen(this, group));
        }
    }

    /**
     * 打开添加分组界面
     */
    private void openAddGroupScreen() {
        this.client.setScreen(new EditGroupScreen(this, null));
    }

    /**
     * 导入路径点 — 弹出系统文件选择窗口
     */
    private void importWaypoints() {
        Path configDir = Path.of(".minecraft/config/vmtools");

        // 在新线程中打开系统文件选择器（避免阻塞渲染线程导致崩溃）
        new Thread(() -> {
            try {
                FileDialog dialog = new FileDialog((Frame) null, "选择导入文件", FileDialog.LOAD);
                dialog.setDirectory(configDir.toAbsolutePath().toString());
                dialog.setFilenameFilter((dir, name) -> name.toLowerCase().endsWith(".json"));
                dialog.setVisible(true);

                String dir = dialog.getDirectory();
                String file = dialog.getFile();

                if (dir != null && file != null) {
                    Path selectedFile = Path.of(dir, file);
                    List<WaypointGroup> importedGroups = WaypointIO.importFromFile(selectedFile);

                    // 回到主线程更新 UI
                    this.client.send(() -> {
                        if (importedGroups != null && !importedGroups.isEmpty()) {
                            this.client.setScreen(new ImportConfirmScreen(this, importedGroups));
                        } else {
                            ToastWidget.showError("导入失败：无法读取文件");
                        }
                    });
                }
            } catch (Exception e) {
                VMToolsClient.LOGGER.error("文件选择器出错", e);
                this.client.send(() -> ToastWidget.showError("文件选择器出错: " + e.getMessage()));
            }
        }, "vmtools-file-dialog").start();
    }

    /**
     * 导出路径点 — 导出后自动打开文件夹
     */
    private void exportWaypoints() {
        Path configDir = Path.of(".minecraft/config/vmtools");

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm"));
        String fileName = "vmtools-export-" + timestamp + ".json";
        Path exportFile = configDir.resolve(fileName);

        boolean success = WaypointIO.exportToFile(exportFile, waypointManager.getGroups());
        if (success) {
            ToastWidget.showSuccess("已导出到: " + configDir.toAbsolutePath() + File.separator + fileName);
            // 打开导出目录
            try {
                Desktop.getDesktop().open(configDir.toAbsolutePath().toFile());
            } catch (Exception e) {
                VMToolsClient.LOGGER.warn("无法打开导出目录", e);
            }
        } else {
            ToastWidget.showError("导出失败");
        }
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
