package com.venus.vmtools.gui;

import com.venus.vmtools.VMToolsClient;
import com.venus.vmtools.config.ModConfig;
import com.venus.vmtools.feature.waypoint.Waypoint;
import com.venus.vmtools.feature.waypoint.WaypointGroup;
import com.venus.vmtools.feature.waypoint.WaypointIO;
import com.venus.vmtools.feature.waypoint.WaypointManager;
import com.venus.vmtools.feature.waypoint.UIState;
import com.venus.vmtools.gui.component.ToastWidget;
import net.minecraft.client.MinecraftClient;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 路径点管理主界面 - 分组窗口布局（1.21.11 Yarn mappings）
 */
public class WaypointScreen extends Screen {

    // 窗口尺寸常量
    private static final int WINDOW_WIDTH = 300; // 默认宽度，仅作为初始值
    private static final int TITLE_BAR_HEIGHT = 22;
    private static final int WAYPOINT_ROW_HEIGHT = 22;
    private static final int WAYPOINT_AREA_MAX_HEIGHT = 150;
    private static final int WINDOW_SPACING = 4;
    private static final int BUTTON_HEIGHT = 20;
    private static final int PADDING = 8;
    private static final int DRAG_HANDLE_WIDTH = 14; // 拖拽手柄宽度

    // 颜色定义
    private static final int PANEL_COLOR = 0xFF2D2D44;
    private static final int HEADER_COLOR = 0xFF3D3D5C;
    private static final int TEXT_COLOR = 0xFFE0E0E0;
    private static final int ACCENT_COLOR = 0xFF7C3AED;
    private static final int HOVER_COLOR = 0xFF4A4A6A;
    private static final int SUCCESS_COLOR = 0xFF4ADE80;
    private static final int DANGER_COLOR = 0xFFF87171;
    private static final int SUBTLE_COLOR = 0xFF888888;
    private static final int SCROLLBAR_COLOR = 0xFF5A5A7A;
    private static final int SCROLLBAR_BG_COLOR = 0xFF1E1E2E;
    private static final int DRAG_HANDLE_COLOR = 0xFF999999;
    private static final int DROP_INDICATOR_COLOR = 0xFF7C3AED;

    // 菜单类型
    private static final int MENU_TYPE_WAYPOINT = 0;
    private static final int MENU_TYPE_GROUP = 1;

    // 拖拽阈值（像素），移动距离小于此值视为点击
    private static final int DRAG_THRESHOLD = 5;

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

    // 分组窗口状态
    private Map<String, GroupWindowState> windowStates = new HashMap<>();
    private UIState uiState;

    // 分组窗口拖拽状态
    private String draggingGroupId = null;
    private int dragOffsetX, dragOffsetY; // 鼠标相对于窗口左上角的偏移
    private double dragStartMouseX, dragStartMouseY; // 拖拽开始时的鼠标位置

    // 窗口宽度调整状态
    private String resizingGroupId = null;
    private boolean resizingWidth = false; // true=调整宽度, false=调整高度
    private int resizeStartWidth = 0;
    private int resizeStartHeight = 0;
    private int resizeStartMouseX = 0;
    private int resizeStartMouseY = 0;
    private static final int MIN_WINDOW_WIDTH = 120;
    private static final int MAX_WINDOW_WIDTH = 400;
    private static final int MIN_WINDOW_HEIGHT = 50;
    private static final int MAX_WINDOW_HEIGHT = 500;
    private static final int RESIZE_HANDLE_SIZE = 6; // 拖拽区域宽度/高度

    // 分组渲染顺序（最后渲染 = 最上层）
    private List<String> groupRenderOrder = new ArrayList<>();

    // 路径点拖拽排序状态
    private String draggingWpGroupId = null;     // 路径点所在分组ID
    private String draggingWpId = null;          // 正在拖拽的路径点ID
    private int draggingWpStartY = 0;            // 拖拽开始时的Y位置
    private int draggingWpCurrentIndex = -1;     // 当前拖拽到的索引位置
    private boolean wpDragTracking = false;       // 是否正在追踪路径点拖拽（按下但未超过阈值）
    private double wpDragStartMouseX = 0;         // 路径点拖拽开始时鼠标X
    private double wpDragStartMouseY = 0;         // 路径点拖拽开始时鼠标Y

    /**
     * 分组窗口状态
     */
    private static class GroupWindowState {
        int x, y;                    // 窗口位置
        int width = 200;             // 窗口宽度（动态计算）
        boolean customWidth = false; // 用户是否手动设置了宽度
        int maxHeight = 150;         // 内容区最大高度（可拖动调整）
        boolean customHeight = false;// 用户是否手动设置了高度
        boolean expanded = false;    // 是否展开
        int scrollOffset = 0;        // 滚动偏移
        int maxScroll = 0;           // 最大滚动值

        GroupWindowState(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    public WaypointScreen() {
        super(Text.of("VMTools - 路径点管理"));
        this.waypointManager = VMToolsClient.getInstance().getWaypointManager();
        this.groups = waypointManager.getGroups();
        this.uiState = UIState.load();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        // 搜索框 - 顶部居中
        searchField = new TextFieldWidget(this.textRenderer,
                centerX - 100, 8,
                200, 18,
                Text.of("搜索..."));
        searchField.setPlaceholder(Text.literal("搜索路径点...").styled(style -> style.withColor(SUBTLE_COLOR)));
        searchField.setChangedListener(this::onSearchChanged);
        this.addDrawableChild(searchField);

        // 底部按钮 - 在屏幕最底部居中
        int btnWidth = 60;
        int btnSpacing = 4;
        int totalBtnWidth = btnWidth * 5 + btnSpacing * 4;
        int btnStartX = centerX - totalBtnWidth / 2;
        int btnY = this.height - 30;

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("+ 路径点"),
                button -> openAddWaypointScreen()
        ).dimensions(btnStartX, btnY, btnWidth, BUTTON_HEIGHT).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("+ 分组"),
                button -> openAddGroupScreen()
        ).dimensions(btnStartX + (btnWidth + btnSpacing), btnY, btnWidth, BUTTON_HEIGHT).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("导入"),
                button -> importWaypoints()
        ).dimensions(btnStartX + (btnWidth + btnSpacing) * 2, btnY, btnWidth, BUTTON_HEIGHT).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("导出"),
                button -> exportWaypoints()
        ).dimensions(btnStartX + (btnWidth + btnSpacing) * 3, btnY, btnWidth, BUTTON_HEIGHT).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("设置"),
                button -> MinecraftClient.getInstance().setScreen(new SettingsScreen(this))
        ).dimensions(btnStartX + (btnWidth + btnSpacing) * 4, btnY, btnWidth, BUTTON_HEIGHT).build());

        // 初始化窗口位置（仅新分组）
        updateWindowPositions();

        // 初始化分组渲染顺序
        initGroupRenderOrder();
    }

    /**
     * 初始化分组渲染顺序：从 UIState 恢复，或按 groups 默认顺序，新分组追加到末尾（最上层）
     */
    private void initGroupRenderOrder() {
        List<String> savedOrder = uiState.getGroupRenderOrder();
        if (savedOrder != null && !savedOrder.isEmpty()) {
            // 从保存的顺序恢复，但只保留当前仍存在的分组
            groupRenderOrder = new ArrayList<>();
            for (String id : savedOrder) {
                if (findGroupById(id) != null && !groupRenderOrder.contains(id)) {
                    groupRenderOrder.add(id);
                }
            }
            // 把不在保存列表中的新分组追加到末尾（最上层）
            for (WaypointGroup group : groups) {
                if (!groupRenderOrder.contains(group.getId())) {
                    groupRenderOrder.add(group.getId());
                }
            }
        } else {
            // 首次使用，按 groups 默认顺序
            groupRenderOrder = new ArrayList<>();
            for (WaypointGroup group : groups) {
                groupRenderOrder.add(group.getId());
            }
        }
    }

    /**
     * 将分组移到最上层（渲染顺序末尾）
     */
    private void bringGroupToFront(String groupId) {
        groupRenderOrder.remove(groupId);
        groupRenderOrder.add(groupId);
        markUIStateDirty();
    }

    /**
     * 根据 ID 查找分组
     */
    private WaypointGroup findGroupById(String groupId) {
        for (WaypointGroup group : groups) {
            if (group.getId().equals(groupId)) {
                return group;
            }
        }
        return null;
    }

    /**
     * 更新窗口位置（为新分组分配默认位置，已有状态的分组保持原位置）
     */
    private void updateWindowPositions() {
        int centerX = this.width / 2 - WINDOW_WIDTH / 2;
        int startY = 30;

        for (WaypointGroup group : groups) {
            String groupId = group.getId();
            if (!windowStates.containsKey(groupId)) {
                // 检查是否有已保存的状态
                UIState.WindowState saved = uiState.getWindowState(groupId);
                if (saved != null) {
                    GroupWindowState state = new GroupWindowState(saved.x, saved.y);
                    state.expanded = saved.expanded;
                    if (saved.width > 0) {
                        state.width = saved.width;
                        state.customWidth = true;
                    }
                    if (saved.maxHeight > 0) {
                        state.maxHeight = saved.maxHeight;
                        state.customHeight = true;
                    }
                    windowStates.put(groupId, state);
                } else {
                    windowStates.put(groupId, new GroupWindowState(centerX, startY));
                }
            }
        }
    }

    /**
     * 计算分组窗口所需的动态宽度
     *
     * @param group 路径点分组
     * @return 计算后的窗口宽度（在 minWidth 和 maxWidth 之间）
     */
    private int calculateWindowWidth(WaypointGroup group) {
        int minWidth = 180;
        int maxWidth = 350;

        // 基于分组名称计算标题宽度
        String expandIcon = "▶";
        String titleText = expandIcon + " " + group.getColor().getEmoji() + " " + group.getName();
        int titleWidth = this.textRenderer.getWidth(titleText) + PADDING * 4 + 30;

        // 基于最长路径点名称计算内容宽度（加上拖拽手柄宽度）
        int maxWpWidth = 0;
        for (Waypoint wp : group.getWaypoints()) {
            int wpWidth = this.textRenderer.getWidth(wp.getColor().getEmoji() + " " + wp.getName())
                    + DRAG_HANDLE_WIDTH + 60; // 60 = padding + 传送按钮
            maxWpWidth = Math.max(maxWpWidth, wpWidth);
        }

        int width = Math.max(titleWidth, maxWpWidth);
        width = Math.max(minWidth, Math.min(maxWidth, width));
        return width;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 渲染标题
        drawCenteredText(context, "VMTools - 路径点管理", this.width / 2, 0, ACCENT_COLOR);

        // 渲染分组窗口（按 groupRenderOrder 顺序，最后渲染的在最上层）
        int autoLayoutY = 30;
        int centerX = this.width / 2 - WINDOW_WIDTH / 2;

        for (String groupId : groupRenderOrder) {
            WaypointGroup group = findGroupById(groupId);
            if (group == null) continue;

            // 搜索过滤：检查分组是否有匹配的路径点
            if (!searchKeyword.isEmpty()) {
                boolean hasMatch = group.getWaypoints().stream()
                        .anyMatch(wp -> wp.getName().toLowerCase().contains(searchKeyword.toLowerCase()) ||
                                wp.getCommand().toLowerCase().contains(searchKeyword.toLowerCase()));
                if (!hasMatch) continue;
            }

            GroupWindowState state = windowStates.get(group.getId());
            if (state == null) {
                // 新分组，分配默认位置
                state = new GroupWindowState(centerX, autoLayoutY);
                windowStates.put(group.getId(), state);
            }

            // 动态计算宽度（用户手动设置过则不覆盖）
            if (!state.customWidth) {
                state.width = calculateWindowWidth(group);
            }

            renderGroupWindow(context, group, state, mouseX, mouseY);

            // 更新自动布局 Y（仅用于下一个新窗口的默认位置）
            int windowHeight = TITLE_BAR_HEIGHT;
            if (state.expanded) {
                windowHeight += getWindowHeight(group, state);
            }
            autoLayoutY = state.y + windowHeight + WINDOW_SPACING;
        }

        // 如果正在拖拽路径点，渲染拖拽指示线（在所有窗口之上）
        if (draggingWpId != null && draggingWpCurrentIndex >= 0) {
            WaypointGroup dragGroup = findGroupById(draggingWpGroupId);
            if (dragGroup != null) {
                GroupWindowState dragState = windowStates.get(draggingWpGroupId);
                if (dragState != null && dragState.expanded) {
                    renderDropIndicator(context, dragGroup, dragState);
                }
            }
        }

        // 渲染底部提示
        drawCenteredText(context, "左键展开  |  右键菜单  |  ESC 关闭",
                this.width / 2, this.height - 12, SUBTLE_COLOR);

        // 渲染右键菜单（最上层）
        if (showContextMenu) {
            renderContextMenu(context, mouseX, mouseY);
        }

        // 渲染 Toast 通知
        ToastWidget.render(context, this.width, this.height);

        // 保存 UI 状态（限制频率）
        saveUIStateIfNeeded();

        super.render(context, mouseX, mouseY, delta);
    }

    /**
     * 渲染路径点拖拽的放置指示线
     */
    private void renderDropIndicator(DrawContext context, WaypointGroup group, GroupWindowState state) {
        int x = state.x;
        int y = state.y;
        int w = state.width;
        int contentY = y + TITLE_BAR_HEIGHT;
        int contentHeight = getWindowHeight(group, state);

        // 计算指示线的 Y 位置
        int indicatorY = contentY - state.scrollOffset + draggingWpCurrentIndex * (WAYPOINT_ROW_HEIGHT + 2);
        // 限制在内容区域内
        if (indicatorY >= contentY && indicatorY <= contentY + contentHeight) {
            context.fill(x + 4, indicatorY - 1, x + w - 4, indicatorY + 1, DROP_INDICATOR_COLOR);
        }
    }

    /**
     * 渲染单个分组窗口
     */
    private void renderGroupWindow(DrawContext context, WaypointGroup group, GroupWindowState state, int mouseX, int mouseY) {
        int x = state.x;
        int y = state.y;
        int w = state.width;

        // 计算窗口高度
        int windowHeight = TITLE_BAR_HEIGHT;
        if (state.expanded) {
            windowHeight += getWindowHeight(group, state);
        }

        // 窗口背景
        fillRoundedRect(context, x, y, w, windowHeight, PANEL_COLOR);

        // 标题栏
        boolean isTitleHovered = mouseX >= x && mouseX <= x + w &&
                mouseY >= y && mouseY <= y + TITLE_BAR_HEIGHT;
        int titleBg = isTitleHovered ? HOVER_COLOR : HEADER_COLOR;
        fillRoundedRect(context, x, y, w, TITLE_BAR_HEIGHT, titleBg);

        // 展开图标 + 色彩表情符号 + 分组名称 + 路径点数量
        String expandIcon = state.expanded ? "▼" : "▶";
        String titleText = expandIcon + " " + group.getColor().getEmoji() + " " + group.getName();
        drawText(context, titleText, x + PADDING, y + 5, TEXT_COLOR);

        // 路径点数量（右对齐）
        String count = "[" + group.getWaypointCount() + "]";
        int countWidth = this.textRenderer.getWidth(count);
        drawText(context, count, x + w - countWidth - PADDING, y + 5, SUBTLE_COLOR);

        // 右边框悬浮高亮（宽度拖拽区域）- 全窗口高度
        boolean isRightBorderHovered = mouseX >= x + w - RESIZE_HANDLE_SIZE && mouseX <= x + w + 2 &&
                mouseY >= y && mouseY <= y + windowHeight;
        if (isRightBorderHovered || (resizingGroupId != null && resizingGroupId.equals(group.getId()) && resizingWidth)) {
            context.fill(x + w - 1, y, x + w, y + windowHeight, 0x887C3AED);
        }

        // 展开的路径点列表
        if (state.expanded) {
            int contentY = y + TITLE_BAR_HEIGHT;
            int contentHeight = getWindowHeight(group, state);

            // 启用裁剪区域（防止绘制超出窗口内容区）
            context.enableScissor(x, contentY, x + w, contentY + contentHeight);

            int waypointY = contentY - state.scrollOffset;
            int teleportBtnWidth = 0;
            int nameX = x + PADDING + DRAG_HANDLE_WIDTH + 2; // 名称 X 起点（留出手柄空间）
            int hoverStartX = nameX; // 悬停检测起始 X

            for (int wpIndex = 0; wpIndex < group.getWaypoints().size(); wpIndex++) {
                Waypoint wp = group.getWaypoints().get(wpIndex);

                // 搜索过滤
                if (!searchKeyword.isEmpty() &&
                        !wp.getName().toLowerCase().contains(searchKeyword.toLowerCase()) &&
                        !wp.getCommand().toLowerCase().contains(searchKeyword.toLowerCase())) {
                    continue;
                }

                // 如果该路径点正在被拖拽，以半透明绘制
                boolean isDraggingThis = draggingWpId != null && draggingWpId.equals(wp.getId());

                // 渲染路径点行
                boolean isWpHovered = mouseX >= hoverStartX && mouseX <= x + w - teleportBtnWidth - 12 &&
                        mouseY >= waypointY && mouseY >= contentY &&
                        mouseY <= waypointY + WAYPOINT_ROW_HEIGHT && mouseY <= contentY + contentHeight;

                // 路径点行背景
                int wpBg = isWpHovered ? 0xFF3A3A55 : 0x00000000;
                if (wpBg != 0x00000000 && !isDraggingThis) {
                    fillRoundedRect(context, hoverStartX, waypointY, w - (hoverStartX - x) - 4 - teleportBtnWidth, WAYPOINT_ROW_HEIGHT, wpBg);
                }

                // 拖拽手柄区域
                int handleX = x + PADDING;
                int handleY = waypointY;
                int handleW = DRAG_HANDLE_WIDTH;
                int handleH = WAYPOINT_ROW_HEIGHT;

                // 手柄悬停高亮
                boolean isHandleHovered = mouseX >= handleX && mouseX <= handleX + handleW &&
                        mouseY >= handleY && mouseY >= contentY &&
                        mouseY <= handleY + handleH && mouseY <= contentY + contentHeight;
                if (isHandleHovered && !isDraggingThis) {
                    context.fill(handleX, handleY, handleX + handleW, handleY + handleH, 0x33FFFFFF);
                }

                // 绘制拖拽手柄 ⋮⋮（6 个点，两列三行）
                int dotSize = 2;
                int dotSpacing = 4;
                int dotsStartX = handleX + (handleW - dotSize * 2 - dotSpacing + 1) / 2;
                int dotsStartY = handleY + (handleH - dotSize * 3 - 2 * 2) / 2;
                int handleColor = isDraggingThis ? (DRAG_HANDLE_COLOR & 0x80FFFFFF) : DRAG_HANDLE_COLOR;
                for (int row = 0; row < 3; row++) {
                    for (int col = 0; col < 2; col++) {
                        int dotX = dotsStartX + col * (dotSize + dotSpacing);
                        int dotY = dotsStartY + row * (dotSize + 2);
                        context.fill(dotX, dotY, dotX + dotSize, dotY + dotSize, handleColor);
                    }
                }

                // 路径点名称（半透明如果正在拖拽）
                int textColor = isDraggingThis ? (TEXT_COLOR & 0x80FFFFFF) : TEXT_COLOR;
                drawText(context, wp.getColor().getEmoji() + " " + wp.getName(),
                        nameX, waypointY + 4, textColor);


                waypointY += WAYPOINT_ROW_HEIGHT + 2;
            }

            // 关闭裁剪区域
            context.disableScissor();

            // 渲染滚动条
            if (state.maxScroll > 0) {
                renderScrollbar(context, x + w - 6, contentY, 4, contentHeight, state);
            }

            // 底部边框高亮（高度拖拽区域）- 在 scissor 外渲染
            int bottomY = contentY + contentHeight;
            boolean isBottomHovered = mouseX >= x && mouseX <= x + w &&
                    mouseY >= bottomY - RESIZE_HANDLE_SIZE && mouseY <= bottomY + 2;
            boolean isResizingThis = resizingGroupId != null && resizingGroupId.equals(group.getId()) && !resizingWidth;
            if (isBottomHovered || isResizingThis) {
                context.fill(x + 2, bottomY - 2, x + w - 2, bottomY, 0x887C3AED);
            }
        }
    }

    /**
     * 获取窗口内容区域高度
     */
    /**
     * 将可见索引转换为实际索引（考虑搜索过滤）
     * @param visibleIndex 可见列表中的索引（0-based）
     * @return 实际 waypoints 列表中的索引，如果超出范围则返回 -1
     */
    private int visibleIndexToActualIndex(WaypointGroup group, int visibleIndex) {
        int visibleCount = 0;
        for (int i = 0; i < group.getWaypoints().size(); i++) {
            Waypoint wp = group.getWaypoints().get(i);
            if (!searchKeyword.isEmpty() &&
                    !wp.getName().toLowerCase().contains(searchKeyword.toLowerCase()) &&
                    !wp.getCommand().toLowerCase().contains(searchKeyword.toLowerCase())) {
                continue;
            }
            if (visibleCount == visibleIndex) {
                return i;
            }
            visibleCount++;
        }
        // 超出范围，返回列表末尾
        return group.getWaypoints().size();
    }

    /**
     * 获取可见路径点数量（考虑搜索过滤）
     */
    private int getVisibleWaypointCount(WaypointGroup group) {
        int count = 0;
        for (Waypoint wp : group.getWaypoints()) {
            if (!searchKeyword.isEmpty() &&
                    !wp.getName().toLowerCase().contains(searchKeyword.toLowerCase()) &&
                    !wp.getCommand().toLowerCase().contains(searchKeyword.toLowerCase())) {
                continue;
            }
            count++;
        }
        return count;
    }

    private int getWindowHeight(WaypointGroup group, GroupWindowState state) {
        int visibleCount = 0;
        for (Waypoint wp : group.getWaypoints()) {
            if (!searchKeyword.isEmpty() &&
                    !wp.getName().toLowerCase().contains(searchKeyword.toLowerCase()) &&
                    !wp.getCommand().toLowerCase().contains(searchKeyword.toLowerCase())) {
                continue;
            }
            visibleCount++;
        }
        int totalHeight = visibleCount * (WAYPOINT_ROW_HEIGHT + 2);
        int maxH = state.maxHeight > 0 ? state.maxHeight : WAYPOINT_AREA_MAX_HEIGHT;
        state.maxScroll = Math.max(0, totalHeight - maxH);
        return Math.min(totalHeight, maxH);
    }

    /**
     * 渲染滚动条
     */
    private void renderScrollbar(DrawContext context, int x, int y, int width, int height, GroupWindowState state) {
        // 滚动条背景
        context.fill(x, y, x + width, y + height, SCROLLBAR_BG_COLOR);

        // 滚动条滑块
        int thumbHeight = Math.max(10, (int) ((float) height / (state.maxScroll + height) * height));
        int thumbY = y + (int) ((float) state.scrollOffset / state.maxScroll * (height - thumbHeight));
        context.fill(x, thumbY, x + width, thumbY + thumbHeight, SCROLLBAR_COLOR);
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
            labels = new String[]{"编辑", "复制", "删除"};
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

        // 检查是否点击了分组窗口（按渲染顺序反向遍历，优先处理最上层的窗口）
        for (int gi = groupRenderOrder.size() - 1; gi >= 0; gi--) {
            String groupId = groupRenderOrder.get(gi);
            WaypointGroup group = findGroupById(groupId);
            if (group == null) continue;

            // 搜索过滤
            if (!searchKeyword.isEmpty()) {
                boolean hasMatch = group.getWaypoints().stream()
                        .anyMatch(wp -> wp.getName().toLowerCase().contains(searchKeyword.toLowerCase()) ||
                                wp.getCommand().toLowerCase().contains(searchKeyword.toLowerCase()));
                if (!hasMatch) continue;
            }

            GroupWindowState state = windowStates.get(group.getId());
            if (state == null) continue;

            int x = state.x;
            int y = state.y;
            int w = state.width;
            int windowHeight = TITLE_BAR_HEIGHT;
            if (state.expanded) {
                windowHeight += getWindowHeight(group, state);
            }

            // 右边框宽度拖拽
            if (button == 0 && mouseX >= x + w - RESIZE_HANDLE_SIZE && mouseX <= x + w + 2 &&
                    mouseY >= y && mouseY <= y + windowHeight) {
                resizingGroupId = group.getId();
                resizingWidth = true;
                resizeStartWidth = w;
                resizeStartMouseX = (int) mouseX;
                bringGroupToFront(group.getId());
                return true;
            }

            // 底部边框高度拖拽
            if (state.expanded && button == 0 &&
                    mouseX >= x && mouseX <= x + w &&
                    mouseY >= y + windowHeight - RESIZE_HANDLE_SIZE && mouseY <= y + windowHeight + 2) {
                resizingGroupId = group.getId();
                resizingWidth = false;
                // 用当前实际渲染高度而非 maxHeight，这样拖拽立即跟手
                resizeStartHeight = windowHeight - TITLE_BAR_HEIGHT;
                resizeStartMouseY = (int) mouseY;
                bringGroupToFront(group.getId());
                return true;
            }

            // 检查点击是否在窗口内
            if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + windowHeight) {
                // 点击标题栏
                if (mouseY <= y + TITLE_BAR_HEIGHT) {
                    if (button == 0) { // 左键：开始拖拽
                        draggingGroupId = group.getId();
                        dragOffsetX = (int) mouseX - x;
                        dragOffsetY = (int) mouseY - y;
                        dragStartMouseX = mouseX;
                        dragStartMouseY = mouseY;
                        bringGroupToFront(group.getId());
                        return true;
                    } else if (button == 1) { // 右键显示分组菜单
                        showContextMenu = true;
                        contextMenuType = MENU_TYPE_GROUP;
                        contextMenuX = (int) mouseX;
                        contextMenuY = (int) mouseY;
                        contextMenuGroup = group;
                        contextMenuWaypoint = null;
                        bringGroupToFront(group.getId());
                    }
                    return true;
                }

                // 点击路径点区域
                if (state.expanded && mouseY > y + TITLE_BAR_HEIGHT) {
                    int contentY = y + TITLE_BAR_HEIGHT;
                    int contentHeight = getWindowHeight(group, state);
                    int waypointY = contentY - state.scrollOffset;
                    int teleportBtnWidth = 0;
                    int nameX = x + PADDING + DRAG_HANDLE_WIDTH + 2;

                    // 确保新点击不会被误认为是正在拖拽路径点
                    draggingWpId = null;
                    draggingWpCurrentIndex = -1;

                    for (int wpIndex = 0; wpIndex < group.getWaypoints().size(); wpIndex++) {
                        Waypoint wp = group.getWaypoints().get(wpIndex);

                        // 搜索过滤
                        if (!searchKeyword.isEmpty() &&
                                !wp.getName().toLowerCase().contains(searchKeyword.toLowerCase()) &&
                                !wp.getCommand().toLowerCase().contains(searchKeyword.toLowerCase())) {
                            continue;
                        }

                        // 检查当前路径点行
                        if (mouseY >= waypointY && mouseY < waypointY + WAYPOINT_ROW_HEIGHT) {
                            // 检查是否点击了拖拽手柄区域
                            int handleX = x + PADDING;
                            int handleRight = handleX + DRAG_HANDLE_WIDTH;
                            if (button == 0 && mouseX >= handleX && mouseX <= handleRight) {
                                // 开始追踪路径点拖拽（不立即开始拖拽，等超过阈值）
                                wpDragTracking = true;
                                wpDragStartMouseX = mouseX;
                                wpDragStartMouseY = mouseY;
                                draggingWpGroupId = group.getId();
                                // 找到在未过滤列表中的索引
                                draggingWpCurrentIndex = wpIndex;
                                return true;
                            }

                            // 左键点击路径点名称 - 传送
                            if (button == 0 && mouseX >= nameX) {
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

                        waypointY += WAYPOINT_ROW_HEIGHT + 2;
                    }
                }

                // 点击了窗口内部但不是任何交互元素
                if (button == 1) {
                    bringGroupToFront(group.getId());
                }
                return true;
            }
        }

        // 点击外部关闭菜单
        showContextMenu = false;
        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        // 路径点拖拽追踪处理
        if (wpDragTracking && button == 0) {
            double distX = Math.abs(mouseX - wpDragStartMouseX);
            double distY = Math.abs(mouseY - wpDragStartMouseY);
            if (distX > DRAG_THRESHOLD || distY > DRAG_THRESHOLD) {
                // 超过阈值，正式开始路径点拖拽
                wpDragTracking = false;
                draggingWpId = getWaypointIdByIndex(draggingWpGroupId, draggingWpCurrentIndex);
                draggingWpStartY = (int) wpDragStartMouseY;
            }
        }

        // 正式路径点拖拽进行中
        if (draggingWpId != null && button == 0) {
            // 计算当前鼠标悬浮的目标索引（基于可见路径点）
            WaypointGroup group = findGroupById(draggingWpGroupId);
            GroupWindowState state = windowStates.get(draggingWpGroupId);
            if (group != null && state != null && state.expanded) {
                int contentY = state.y + TITLE_BAR_HEIGHT;
                int relY = (int) mouseY - contentY + state.scrollOffset;
                // 计算可见路径点数量（考虑搜索过滤）
                int visibleCount = getVisibleWaypointCount(group);
                int targetIndex = Math.round((float) relY / (WAYPOINT_ROW_HEIGHT + 2));
                // 钳制到有效范围（0 到可见数量，size 表示末尾）
                targetIndex = Math.max(0, Math.min(visibleCount, targetIndex));
                draggingWpCurrentIndex = targetIndex;
            }
            return true;
        }

        // 分组窗口尺寸调整
        if (resizingGroupId != null && button == 0) {
            GroupWindowState state = windowStates.get(resizingGroupId);
            if (state != null) {
                if (resizingWidth) {
                    int deltaW = (int) mouseX - resizeStartMouseX;
                    state.width = Math.max(MIN_WINDOW_WIDTH, Math.min(MAX_WINDOW_WIDTH, resizeStartWidth + deltaW));
                    state.customWidth = true;
                } else {
                    int deltaH = (int) mouseY - resizeStartMouseY;
                    state.maxHeight = Math.max(MIN_WINDOW_HEIGHT, Math.min(MAX_WINDOW_HEIGHT, resizeStartHeight + deltaH));
                    state.customHeight = true;
                }
            }
            return true;
        }

        // 分组窗口拖拽
        if (draggingGroupId != null && button == 0) {
            GroupWindowState state = windowStates.get(draggingGroupId);
            if (state != null) {
                state.x = (int) mouseX - dragOffsetX;
                state.y = (int) mouseY - dragOffsetY;
                // 钳制到屏幕边界
                state.x = Math.max(0, Math.min(this.width - state.width, state.x));
                state.y = Math.max(0, Math.min(this.height - TITLE_BAR_HEIGHT, state.y));
            }
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        // 路径点拖拽释放
        if (button == 0 && (draggingWpId != null || wpDragTracking)) {
            if (wpDragTracking) {
                // 未超过阈值，视为点击（不做任何操作，恢复追踪状态）
                wpDragTracking = false;
            }
            if (draggingWpId != null) {
                // 执行路径点排序
                WaypointGroup group = findGroupById(draggingWpGroupId);
                if (group != null && draggingWpCurrentIndex >= 0) {
                    // 找到拖拽路径点在当前列表中的实际索引
                    int fromIndex = -1;
                    for (int i = 0; i < group.getWaypoints().size(); i++) {
                        if (group.getWaypoints().get(i).getId().equals(draggingWpId)) {
                            fromIndex = i;
                            break;
                        }
                    }
                    // 将可见索引转换为实际索引
                    int toActualIndex = visibleIndexToActualIndex(group, draggingWpCurrentIndex);
                    if (fromIndex >= 0 && toActualIndex >= 0 && fromIndex != toActualIndex) {
                        group.moveWaypoint(fromIndex, toActualIndex);
                        waypointManager.save();
                    }
                }
            }
            // 重置所有路径点拖拽状态
            draggingWpId = null;
            draggingWpGroupId = null;
            draggingWpCurrentIndex = -1;
            draggingWpStartY = 0;
            wpDragTracking = false;
            return true;
        }

        // 分组窗口拖拽释放
        if (draggingGroupId != null && button == 0) {
            // 判断是点击还是拖拽：移动距离小于阈值则视为点击（切换展开/折叠）
            double distX = Math.abs(mouseX - dragStartMouseX);
            double distY = Math.abs(mouseY - dragStartMouseY);
            if (distX < DRAG_THRESHOLD && distY < DRAG_THRESHOLD) {
                // 是点击操作，切换展开/折叠
                GroupWindowState state = windowStates.get(draggingGroupId);
                if (state != null) {
                    state.expanded = !state.expanded;
                    state.scrollOffset = 0;
                    markUIStateDirty();
                }
            }
            draggingGroupId = null;
            markUIStateDirty();
            return true;
        }

        // 宽度调整释放
        if (button == 0 && resizingGroupId != null) {
            resizingGroupId = null;
            markUIStateDirty();
            return true;
        }

        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // 检查鼠标是否在某个展开的分组窗口内（按渲染顺序反向，优先最上层）
        for (int gi = groupRenderOrder.size() - 1; gi >= 0; gi--) {
            String groupId = groupRenderOrder.get(gi);
            WaypointGroup group = findGroupById(groupId);
            if (group == null) continue;

            GroupWindowState state = windowStates.get(group.getId());
            if (state == null || !state.expanded) continue;

            int x = state.x;
            int y = state.y;
            int w = state.width;
            int windowHeight = TITLE_BAR_HEIGHT + getWindowHeight(group, state);

            if (mouseX >= x && mouseX <= x + w && mouseY >= y + TITLE_BAR_HEIGHT && mouseY <= y + windowHeight) {
                // 滚动路径点列表
                int scrollDelta = (int) (verticalAmount * 20);
                state.scrollOffset = Math.max(0, Math.min(state.maxScroll, state.scrollOffset - scrollDelta));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    /**
     * 根据分组ID和可见索引获取路径点ID
     */
    private String getWaypointIdByIndex(String groupId, int visibleIndex) {
        WaypointGroup group = findGroupById(groupId);
        if (group == null) return null;

        int actualIndex = 0;
        for (int i = 0; i < group.getWaypoints().size(); i++) {
            Waypoint wp = group.getWaypoints().get(i);
            if (!searchKeyword.isEmpty() &&
                    !wp.getName().toLowerCase().contains(searchKeyword.toLowerCase()) &&
                    !wp.getCommand().toLowerCase().contains(searchKeyword.toLowerCase())) {
                continue;
            }
            if (actualIndex == visibleIndex) {
                return wp.getId();
            }
            actualIndex++;
        }
        return null;
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
            menuItems = 3;
        }

        int menuHeight = menuItemHeight * menuItems + 4;
        int menuY = Math.min(contextMenuY, this.height - menuHeight - 5);

        for (int i = 0; i < menuItems; i++) {
            int itemY = menuY + 2 + i * menuItemHeight;
            if (mouseX >= menuX && mouseX <= menuX + menuWidth &&
                    mouseY >= itemY && mouseY <= itemY + menuItemHeight) {

                if (contextMenuType == MENU_TYPE_GROUP) {
                    // 分组菜单
                    GroupWindowState state = windowStates.get(contextMenuGroup.getId());
                    switch (i) {
                        case 0: // 展开/折叠
                            if (contextMenuGroup != null && state != null) {
                                state.expanded = !state.expanded;
                                state.scrollOffset = 0;
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
                        case 0: // 编辑
                            openEditWaypointScreen(contextMenuWaypoint, contextMenuGroup);
                            break;
                        case 1: // 复制
                            copyWaypoint(contextMenuWaypoint, contextMenuGroup);
                            break;
                        case 2: // 删除
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
        String groupId = group.getId();
        this.client.setScreen(ConfirmScreen.createDeleteConfirm(
                this,
                "分组 \"" + group.getName() + "\"",
                () -> {
                    waypointManager.removeGroup(groupId);
                    this.groups = waypointManager.getGroups();
                    windowStates.remove(groupId);
                    uiState.removeWindowState(groupId);
                    groupRenderOrder.remove(groupId);
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

                // 自动确认传送（检查路径点级别设置 → 全局设置）
                ModConfig config = VMToolsClient.getInstance().getConfig();
                boolean shouldConfirm = waypoint.shouldAutoConfirm(config.isAutoConfirmTeleport());
                if (shouldConfirm && !config.getConfirmCommand().trim().isEmpty()) {
                    String confirmCmd = config.getConfirmCommand().trim();
                    if (confirmCmd.startsWith("/")) confirmCmd = confirmCmd.substring(1);
                    int delay = config.getConfirmDelay();
                    final String cmd = confirmCmd;
                    new Thread(() -> {
                        try {
                            Thread.sleep(delay);
                            MinecraftClient.getInstance().execute(() -> {
                                MinecraftClient mc = MinecraftClient.getInstance();
                                if (mc.player != null && mc.getNetworkHandler() != null) {
                                    mc.getNetworkHandler().sendChatCommand(cmd);
                                    ToastWidget.showInfo("已自动确认: /" + cmd);
                                }
                            });
                        } catch (InterruptedException ignored) {}
                    }, "vmtools-auto-confirm").start();
                }

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
        // 重置所有窗口的滚动位置
        for (GroupWindowState state : windowStates.values()) {
            state.scrollOffset = 0;
        }
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
        // 使用 FabricLoader 获取绝对路径
        Path configDir = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("vmtools");

        // 在新线程中打开系统文件选择器（避免阻塞渲染线程导致崩溃）
        new Thread(() -> {
            try {
                // 确保目录存在
                java.nio.file.Files.createDirectories(configDir);

                // 强制设置 AWT 非 headless 模式
                System.setProperty("java.awt.headless", "false");

                FileDialog dialog = new FileDialog((Frame) null, "选择导入文件", FileDialog.LOAD);
                dialog.setDirectory(configDir.toAbsolutePath().toString());
                dialog.setFilenameFilter((dir, name) -> name.toLowerCase().endsWith(".json"));
                dialog.setVisible(true);

                String dir = dialog.getDirectory();
                String file = dialog.getFile();

                if (dir != null && file != null) {
                    Path selectedFile = Path.of(dir, file);
                    doImport(selectedFile);
                }
            } catch (Exception e) {
                VMToolsClient.LOGGER.warn("文件选择器不可用，使用备选方案", e);
                // 备选方案：扫描配置目录中的 JSON 文件，让用户选择
                this.client.send(() -> showImportFallback(configDir));
            }
        }, "vmtools-file-dialog").start();
    }

    /**
     * 导入备选方案 — 扫描配置目录中的 JSON 文件，弹出选择列表
     */
    private void showImportFallback(Path configDir) {
        try {
            java.nio.file.Files.createDirectories(configDir);
            java.util.List<Path> jsonFiles = new java.util.ArrayList<>();
            try (var stream = java.nio.file.Files.list(configDir)) {
                stream.filter(p -> p.toString().endsWith(".json") && !p.getFileName().toString().equals("ui_state.json"))
                        .sorted()
                        .forEach(jsonFiles::add);
            }

            if (jsonFiles.isEmpty()) {
                ToastWidget.showError("未找到可导入的文件，请将 JSON 文件放到: " + configDir.toAbsolutePath());
                return;
            }

            // 用第一个找到的文件直接导入（简化处理）
            // 如果有多个文件，提示用户放到目录里
            if (jsonFiles.size() == 1) {
                doImport(jsonFiles.get(0));
            } else {
                // 多个文件，依次尝试第一个
                ToastWidget.showInfo("找到 " + jsonFiles.size() + " 个文件，导入: " + jsonFiles.get(0).getFileName());
                doImport(jsonFiles.get(0));
            }
        } catch (Exception e) {
            VMToolsClient.LOGGER.error("导入备选方案失败", e);
            ToastWidget.showError("导入失败: " + e.getMessage());
        }
    }

    /**
     * 执行导入
     */
    private void doImport(Path selectedFile) {
        List<WaypointGroup> importedGroups = WaypointIO.importFromFile(selectedFile);

        this.client.send(() -> {
            if (importedGroups != null && !importedGroups.isEmpty()) {
                this.client.setScreen(new ImportConfirmScreen(this, importedGroups));
            } else {
                ToastWidget.showError("导入失败：无法读取文件 " + selectedFile.getFileName());
            }
        });
    }

    /**
     * 导出路径点 — 导出后自动打开文件夹
     */
    private void exportWaypoints() {
        // 使用 FabricLoader 获取绝对路径
        Path configDir = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("vmtools");

        try {
            java.nio.file.Files.createDirectories(configDir);
        } catch (Exception e) {
            VMToolsClient.LOGGER.error("创建导出目录失败", e);
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm"));
        String fileName = "vmtools-export-" + timestamp + ".json";
        Path exportFile = configDir.resolve(fileName);

        boolean success = WaypointIO.exportToFile(exportFile, waypointManager.getGroups());
        if (success) {
            ToastWidget.showSuccess("已导出到: " + exportFile.toAbsolutePath());
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

    /**
     * 保存当前 UI 状态到文件（限制频率，避免每帧写磁盘）
     */
    private long lastSaveTime = 0;
    private boolean uiStateDirty = false;

    private void markUIStateDirty() {
        uiStateDirty = true;
    }

    private void saveUIStateIfNeeded() {
        if (!uiStateDirty) return;
        long now = System.currentTimeMillis();
        if (now - lastSaveTime < 1000) return; // 最多每秒保存一次
        uiStateDirty = false;
        lastSaveTime = now;
        doSaveUIState();
    }

    private void doSaveUIState() {
        for (Map.Entry<String, GroupWindowState> entry : windowStates.entrySet()) {
            GroupWindowState state = entry.getValue();
            uiState.setWindowState(entry.getKey(), state.x, state.y, state.expanded, state.width, state.maxHeight);
        }
        uiState.setGroupRenderOrder(new ArrayList<>(groupRenderOrder));
        uiState.save();
    }

    @Override
    public void removed() {
        // 关闭界面时强制保存 UI 状态
        doSaveUIState();
        super.removed();
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
