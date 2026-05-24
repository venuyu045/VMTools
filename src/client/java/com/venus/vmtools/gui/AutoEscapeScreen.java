package com.venus.vmtools.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.venus.vmtools.feature.escape.AutoEscapeConfig;
import com.venus.vmtools.feature.escape.AutoEscapeManager;
import com.venus.vmtools.feature.escape.HealthMonitorConfig;
import com.venus.vmtools.feature.escape.HealthMonitorManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 逃逸小工具 - 多工具窗口式布局
 * 工具：虚空逃逸、血量监控
 */
public class AutoEscapeScreen extends Screen {

    // 颜色
    private static final int BG_COLOR = 0xCC000000;
    private static final int PANEL_COLOR = 0xFF2D2D44;
    private static final int HEADER_COLOR = 0xFF3D3D5C;
    private static final int TEXT_COLOR = 0xFFE0E0E0;
    private static final int ACCENT_COLOR = 0xFF7C3AED;
    private static final int SUBTLE_COLOR = 0xFF888888;
    private static final int SUCCESS_COLOR = 0xFF4ADE80;
    private static final int DANGER_COLOR = 0xFFF87171;
    private static final int HOVER_COLOR = 0xFF4A4A6A;

    // Tab 栏
    private static final int TAB_H = 22;
    private static final int TAB_W = 110;

    // 窗口通用
    private static final int TITLE_BAR_H = 20;
    private static final int MIN_W = 180;
    private static final int MAX_W = 400;
    private static final int PAD = 8;
    private static final int LINE_H = 22;
    private static final int RESIZE_ZONE = 6;
    private static final int DRAG_THRESHOLD = 5;

    // 工具窗口状态
    private final List<ToolWindow> windows = new ArrayList<>();

    // 拖拽状态
    private int dragWinIdx = -1;
    private int dragOffX, dragOffY;
    private double dragStartX, dragStartY;
    private int resizeWinIdx = -1;
    private boolean resizeRight = false;
    private boolean resizeBottom = false;
    private int resizeStartW, resizeStartH, resizeStartMX, resizeStartMY;

    // 输入框缓存
    private TextFieldWidget voidThresholdField, voidCommandField;
    private TextFieldWidget healthThresholdField, healthCommandField;

    // 日志滚动
    private int voidLogScroll = 0;
    private int healthLogScroll = 0;
    private static final int LOG_VISIBLE = 4;
    private static final int LOG_LINE_H = 14;

    // 工具窗口数据类
    private static class ToolWindow {
        String id;
        String title;
        int x, y, w;
        int maxHeight;
        boolean expanded;
        boolean enabled;
        String statusText;
        int statusColor;

        ToolWindow(String id, String title, int x, int y, int w, int maxH, boolean expanded) {
            this.id = id;
            this.title = title;
            this.x = x; this.y = y; this.w = w;
            this.maxHeight = maxH;
            this.expanded = expanded;
        }
    }

    // 窗口状态持久化
    private static class ToolWindowState {
        int x = -1, y = -1;
        int w = 300;
        int maxHeight = 260;
        boolean expanded = true;
    }

    private static class AllToolStates {
        ToolWindowState voidEscape = new ToolWindowState();
        ToolWindowState healthMonitor = new ToolWindowState();
    }

    private static final Path STATE_PATH = FabricLoader.getInstance().getConfigDir()
            .resolve("vmtools").resolve("tool_windows.json");
    private AllToolStates toolStates;

    public AutoEscapeScreen() {
        super(Text.of("VMTools - 逃逸小工具"));
        loadToolStates();
    }

    private void loadToolStates() {
        try {
            if (Files.exists(STATE_PATH)) {
                String json = Files.readString(STATE_PATH);
                toolStates = new Gson().fromJson(json, AllToolStates.class);
            }
        } catch (Exception ignored) {}
        if (toolStates == null) toolStates = new AllToolStates();
    }

    private void saveToolStates() {
        // 从窗口列表同步状态
        for (ToolWindow win : windows) {
            ToolWindowState ws = getToolState(win.id);
            ws.x = win.x; ws.y = win.y; ws.w = win.w;
            ws.maxHeight = win.maxHeight;
            ws.expanded = win.expanded;
        }
        try {
            Files.createDirectories(STATE_PATH.getParent());
            Files.writeString(STATE_PATH, new GsonBuilder().setPrettyPrinting().create().toJson(toolStates));
        } catch (Exception ignored) {}
    }

    private ToolWindowState getToolState(String id) {
        return switch (id) {
            case "void_escape" -> toolStates.voidEscape;
            case "health_monitor" -> toolStates.healthMonitor;
            default -> new ToolWindowState();
        };
    }

    @Override
    protected void init() {
        windows.clear();

        // 虚空逃逸窗口
        ToolWindowState vs = toolStates.voidEscape;
        ToolWindowState hs = toolStates.healthMonitor;

        windows.add(new ToolWindow("void_escape", "虚空逃逸",
                vs.x >= 0 ? vs.x : this.width / 2 - 170,
                vs.y >= 0 ? vs.y : TAB_H + 10,
                vs.w, vs.maxHeight, vs.expanded));

        // 血量监控窗口
        windows.add(new ToolWindow("health_monitor", "血量监控",
                hs.x >= 0 ? hs.x : this.width / 2 + 10,
                hs.y >= 0 ? hs.y : TAB_H + 10,
                hs.w, hs.maxHeight, hs.expanded));

        rebuildFields();
    }

    private void rebuildFields() {
        if (voidThresholdField != null) this.remove(voidThresholdField);
        if (voidCommandField != null) this.remove(voidCommandField);
        if (healthThresholdField != null) this.remove(healthThresholdField);
        if (healthCommandField != null) this.remove(healthCommandField);
        voidThresholdField = null; voidCommandField = null;
        healthThresholdField = null; healthCommandField = null;

        AutoEscapeConfig vc = AutoEscapeManager.getInstance().getConfig();
        HealthMonitorConfig hc = HealthMonitorManager.getInstance().getConfig();

        for (ToolWindow win : windows) {
            if (!win.expanded) continue;
            int fy = win.y + TITLE_BAR_H + PAD + LINE_H; // 跳过启用开关
            int fx = win.x + PAD + 70;
            int fw = win.w - PAD * 2 - 75;

            if (win.id.equals("void_escape")) {
                voidThresholdField = makeField(fx, fy, fw, String.valueOf(vc.getThreshold()), "-70.0", 10, text -> {
                    try { AutoEscapeManager.getInstance().setThreshold(Double.parseDouble(text)); } catch (NumberFormatException ignored) {}
                });
                this.addDrawableChild(voidThresholdField);
                fy += LINE_H;
                voidCommandField = makeField(fx, fy, fw, vc.getCommand(), "/ehomes home", 50, text -> AutoEscapeManager.getInstance().setCommand(text));
                this.addDrawableChild(voidCommandField);
            } else if (win.id.equals("health_monitor")) {
                healthThresholdField = makeField(fx, fy, fw, String.valueOf(hc.getThreshold()), "10.0", 10, text -> {
                    try { HealthMonitorManager.getInstance().setThreshold(Double.parseDouble(text)); } catch (NumberFormatException ignored) {}
                });
                this.addDrawableChild(healthThresholdField);
                fy += LINE_H;
                healthCommandField = makeField(fx, fy, fw, hc.getCommand(), "/home", 50, text -> HealthMonitorManager.getInstance().setCommand(text));
                this.addDrawableChild(healthCommandField);
            }
        }
    }

    private TextFieldWidget makeField(int x, int y, int w, String value, String placeholder, int maxLen, java.util.function.Consumer<String> listener) {
        TextFieldWidget field = new TextFieldWidget(this.textRenderer, x, y, w, 16, Text.of(""));
        field.setPlaceholder(Text.literal(placeholder).styled(s -> s.withColor(SUBTLE_COLOR)));
        field.setText(value);
        field.setMaxLength(maxLen);
        field.setChangedListener(listener);
        return field;
    }

    private int getContentHeight(ToolWindow win) {
        if (!win.expanded) return 0;
        return PAD + LINE_H + LINE_H + LINE_H + LINE_H + LINE_H + 12 + LINE_H + LOG_VISIBLE * LOG_LINE_H + 4 + PAD;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, BG_COLOR);
        renderTabBar(context, mouseX, mouseY);

        MinecraftClient mc = MinecraftClient.getInstance();

        for (int i = windows.size() - 1; i >= 0; i--) {
            ToolWindow win = windows.get(i);
            int contentH = getContentHeight(win);
            int totalH = TITLE_BAR_H + contentH;

            // 标题栏
            boolean titleHover = mouseX >= win.x && mouseX <= win.x + win.w &&
                    mouseY >= win.y && mouseY <= win.y + TITLE_BAR_H;
            context.fill(win.x, win.y, win.x + win.w, win.y + TITLE_BAR_H, titleHover ? HOVER_COLOR : HEADER_COLOR);

            // 标题文字
            AutoEscapeConfig vc = AutoEscapeManager.getInstance().getConfig();
            HealthMonitorConfig hc = HealthMonitorManager.getInstance().getConfig();
            boolean isEnabled = win.id.equals("void_escape") ? vc.isEnabled() : hc.isEnabled();
            String icon = win.expanded ? "▼" : "▶";
            drawText(context, icon + " " + win.title, win.x + PAD, win.y + 4, TEXT_COLOR);
            String status = isEnabled ? "ON" : "OFF";
            int sw = this.textRenderer.getWidth(status);
            drawText(context, status, win.x + win.w - PAD - sw - RESIZE_ZONE, win.y + 4, isEnabled ? SUCCESS_COLOR : SUBTLE_COLOR);

            // 右边框高亮
            boolean rightH = mouseX >= win.x + win.w - RESIZE_ZONE && mouseX <= win.x + win.w &&
                    mouseY >= win.y && mouseY <= win.y + totalH;
            if (rightH || (resizeWinIdx == i && resizeRight))
                context.fill(win.x + win.w - 1, win.y, win.x + win.w, win.y + totalH, 0x887C3AED);

            if (win.expanded) {
                context.fill(win.x, win.y + TITLE_BAR_H, win.x + win.w, win.y + totalH, PANEL_COLOR);
                context.enableScissor(win.x, win.y + TITLE_BAR_H, win.x + win.w, win.y + totalH);
                super.render(context, mouseX, mouseY, delta);

                int y = win.y + TITLE_BAR_H + PAD;
                int rx = win.x + PAD;
                int rw = win.w - PAD * 2;

                if (win.id.equals("void_escape")) {
                    renderVoidEscapeContent(context, mc, rx, y, rw);
                } else {
                    renderHealthMonitorContent(context, mc, rx, y, rw);
                }

                context.disableScissor();

                // 底部边框高亮
                int bottomY = win.y + totalH;
                boolean bottomH = mouseX >= win.x && mouseX <= win.x + win.w &&
                        mouseY >= bottomY - RESIZE_ZONE && mouseY <= bottomY;
                if (bottomH || (resizeWinIdx == i && resizeBottom))
                    context.fill(win.x + 2, bottomY - 1, win.x + win.w - 2, bottomY, 0x887C3AED);
            }
        }
    }

    private void renderVoidEscapeContent(DrawContext context, MinecraftClient mc, int rx, int y, int rw) {
        AutoEscapeConfig vc = AutoEscapeManager.getInstance().getConfig();

        // 启用开关
        drawText(context, vc.isEnabled() ? "☑ 启用" : "☐ 启用", rx, y + 3, vc.isEnabled() ? SUCCESS_COLOR : SUBTLE_COLOR);
        y += LINE_H;
        drawText(context, "高度阈值:", rx, y + 2, TEXT_COLOR);
        y += LINE_H;
        drawText(context, "执行命令:", rx, y + 2, TEXT_COLOR);
        y += LINE_H;

        // 自动确认
        boolean ac = vc.isAutoConfirm();
        drawText(context, ac ? "☑ 自动确认" : "☐ 自动确认", rx, y + 3, ac ? SUCCESS_COLOR : SUBTLE_COLOR);
        y += LINE_H;

        // 状态
        if (mc.player != null) {
            double py = mc.player.getY();
            drawText(context, "位置: Y:" + String.format("%.1f", py), rx, y + 3, TEXT_COLOR);
            boolean triggered = py < vc.getThreshold();
            String st = triggered ? "⚠ 触发中" : "✓ 安全";
            int stw = this.textRenderer.getWidth(st);
            drawText(context, st, rx + rw - stw, y + 3, triggered ? DANGER_COLOR : SUCCESS_COLOR);
        } else {
            drawText(context, "未连接", rx, y + 3, SUBTLE_COLOR);
        }
        y += LINE_H + 12;

        // 日志
        context.fill(rx, y, rx + rw, y + 1, 0xFF3D3D5C);
        drawText(context, "触发日志", rx, y + 3, SUBTLE_COLOR);
        y += LINE_H;
        renderLog(context, AutoEscapeManager.getInstance().getLogEntries(), voidLogScroll, rx, y, rw);
    }

    private void renderHealthMonitorContent(DrawContext context, MinecraftClient mc, int rx, int y, int rw) {
        HealthMonitorConfig hc = HealthMonitorManager.getInstance().getConfig();

        drawText(context, hc.isEnabled() ? "☑ 启用" : "☐ 启用", rx, y + 3, hc.isEnabled() ? SUCCESS_COLOR : SUBTLE_COLOR);
        y += LINE_H;
        drawText(context, "血量阈值:", rx, y + 2, TEXT_COLOR);
        y += LINE_H;
        drawText(context, "执行命令:", rx, y + 2, TEXT_COLOR);
        y += LINE_H;

        boolean ac = hc.isAutoConfirm();
        drawText(context, ac ? "☑ 自动确认" : "☐ 自动确认", rx, y + 3, ac ? SUCCESS_COLOR : SUBTLE_COLOR);
        y += LINE_H;

        if (mc.player != null) {
            float hp = mc.player.getHealth();
            drawText(context, "血量: " + String.format("%.1f", hp) + "/20", rx, y + 3, TEXT_COLOR);
            boolean triggered = hp < hc.getThreshold();
            String st = triggered ? "⚠ 血量过低" : "✓ 正常";
            int stw = this.textRenderer.getWidth(st);
            drawText(context, st, rx + rw - stw, y + 3, triggered ? DANGER_COLOR : SUCCESS_COLOR);
        } else {
            drawText(context, "未连接", rx, y + 3, SUBTLE_COLOR);
        }
        y += LINE_H + 12;

        context.fill(rx, y, rx + rw, y + 1, 0xFF3D3D5C);
        drawText(context, "触发日志", rx, y + 3, SUBTLE_COLOR);
        y += LINE_H;
        renderLog(context, HealthMonitorManager.getInstance().getLogEntries(), healthLogScroll, rx, y, rw);
    }

    private void renderLog(DrawContext context, List<String> logs, int scroll, int x, int y, int w) {
        if (logs.isEmpty()) {
            drawText(context, "暂无记录", x, y + 2, SUBTLE_COLOR);
            return;
        }
        int start = Math.max(0, logs.size() - LOG_VISIBLE - scroll);
        int end = Math.min(logs.size(), start + LOG_VISIBLE);
        int ly = y + 2;
        for (int i = start; i < end; i++) {
            drawText(context, logs.get(i), x, ly, TEXT_COLOR);
            ly += LOG_LINE_H;
        }
        if (logs.size() > LOG_VISIBLE) {
            String info = (start + 1) + "-" + end + "/" + logs.size();
            int iw = this.textRenderer.getWidth(info);
            drawText(context, info, x + w - iw, y + 2, SUBTLE_COLOR);
        }
    }

    private void renderTabBar(DrawContext context, int mouseX, int mouseY) {
        int gap = 4;
        int startX = this.width / 2 - (TAB_W * 2 + gap) / 2;

        boolean leftH = mouseX >= startX && mouseX <= startX + TAB_W && mouseY >= 0 && mouseY <= TAB_H;
        context.fill(startX, 0, startX + TAB_W, TAB_H, leftH ? HOVER_COLOR : HEADER_COLOR);
        drawCentered(context, "◄ 路径点管理", startX + TAB_W / 2, 5, SUBTLE_COLOR);

        int rx = startX + TAB_W + gap;
        context.fill(rx, 0, rx + TAB_W, TAB_H, ACCENT_COLOR);
        drawCentered(context, "逃逸小工具 ►", rx + TAB_W / 2, 5, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        double mx = click.x(), my = click.y();
        int btn = click.button();

        // Tab 栏
        if (btn == 0) {
            int gap = 4;
            int startX = this.width / 2 - (TAB_W * 2 + gap) / 2;
            if (mx >= startX && mx <= startX + TAB_W && my >= 0 && my <= TAB_H) {
                saveToolStates();
                this.client.setScreen(new WaypointScreen());
                return true;
            }
        }

        // 从上层窗口开始检测（最后渲染的在最上层）
        for (int i = windows.size() - 1; i >= 0; i--) {
            ToolWindow win = windows.get(i);
            int totalH = TITLE_BAR_H + getContentHeight(win);

            if (btn == 0) {
                // 右边缘
                if (mx >= win.x + win.w - RESIZE_ZONE && mx <= win.x + win.w + 2 &&
                        my >= win.y && my <= win.y + totalH) {
                    resizeWinIdx = i; resizeRight = true; resizeBottom = false;
                    resizeStartW = win.w; resizeStartMX = (int) mx;
                    return true;
                }
                // 底部边缘
                if (win.expanded && mx >= win.x && mx <= win.x + win.w &&
                        my >= win.y + totalH - RESIZE_ZONE && my <= win.y + totalH + 2) {
                    resizeWinIdx = i; resizeBottom = true; resizeRight = false;
                    resizeStartH = getContentHeight(win); resizeStartMY = (int) my;
                    return true;
                }
                // 标题栏 → 拖拽/折叠
                if (mx >= win.x && mx <= win.x + win.w && my >= win.y && my <= win.y + TITLE_BAR_H) {
                    dragWinIdx = i;
                    dragOffX = (int) mx - win.x;
                    dragOffY = (int) my - win.y;
                    dragStartX = mx; dragStartY = my;
                    return true;
                }
                // 启用开关
                if (win.expanded) {
                    int cy = win.y + TITLE_BAR_H + PAD;
                    if (win.id.equals("void_escape")) {
                        AutoEscapeConfig vc = AutoEscapeManager.getInstance().getConfig();
                        String el = vc.isEnabled() ? "☑ 启用" : "☐ 启用";
                        if (clickIn(mx, my, win.x + PAD, cy, el)) {
                            AutoEscapeManager.getInstance().toggle();
                            return true;
                        }
                        cy += LINE_H * 3;
                        String acl = vc.isAutoConfirm() ? "☑ 自动确认" : "☐ 自动确认";
                        if (clickIn(mx, my, win.x + PAD, cy, acl)) {
                            AutoEscapeManager.getInstance().setAutoConfirm(!vc.isAutoConfirm());
                            return true;
                        }
                    } else {
                        HealthMonitorConfig hc = HealthMonitorManager.getInstance().getConfig();
                        String el = hc.isEnabled() ? "☑ 启用" : "☐ 启用";
                        if (clickIn(mx, my, win.x + PAD, cy, el)) {
                            HealthMonitorManager.getInstance().toggle();
                            return true;
                        }
                        cy += LINE_H * 3;
                        String acl = hc.isAutoConfirm() ? "☑ 自动确认" : "☐ 自动确认";
                        if (clickIn(mx, my, win.x + PAD, cy, acl)) {
                            HealthMonitorManager.getInstance().setAutoConfirm(!hc.isAutoConfirm());
                            return true;
                        }
                    }
                }
            }
        }

        return super.mouseClicked(click, doubleClick);
    }

    private boolean clickIn(double mx, double my, int x, int y, String label) {
        int w = this.textRenderer.getWidth(label);
        return mx >= x && mx <= x + w && my >= y && my <= y + 16;
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        double mx = click.x(), my = click.y();

        if (resizeWinIdx >= 0) {
            ToolWindow win = windows.get(resizeWinIdx);
            if (resizeRight) {
                win.w = Math.max(MIN_W, Math.min(MAX_W, resizeStartW + (int) mx - resizeStartMX));
            } else if (resizeBottom) {
                win.maxHeight = Math.max(50, Math.min(500, resizeStartH + (int) my - resizeStartMY));
            }
            this.clearChildren();
            rebuildFields();
            return true;
        }

        if (dragWinIdx >= 0) {
            ToolWindow win = windows.get(dragWinIdx);
            double dist = Math.abs(mx - dragStartX) + Math.abs(my - dragStartY);
            if (dist >= DRAG_THRESHOLD) {
                win.x = Math.max(0, Math.min(this.width - win.w, (int) mx - dragOffX));
                win.y = Math.max(TAB_H, Math.min(this.height - TITLE_BAR_H, (int) my - dragOffY));
                this.clearChildren();
                rebuildFields();
            }
            return true;
        }

        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragWinIdx >= 0) {
            double mx = click.x(), my = click.y();
            double dist = Math.abs(mx - dragStartX) + Math.abs(my - dragStartY);
            if (dist < DRAG_THRESHOLD) {
                // 短按 = 折叠/展开
                ToolWindow win = windows.get(dragWinIdx);
                win.expanded = !win.expanded;
                this.clearChildren();
                rebuildFields();
                saveToolStates();
            } else {
                saveToolStates();
            }
        }
        dragWinIdx = -1;
        resizeWinIdx = -1;
        resizeRight = false;
        resizeBottom = false;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        for (int i = windows.size() - 1; i >= 0; i--) {
            ToolWindow win = windows.get(i);
            if (!win.expanded) continue;
            int totalH = TITLE_BAR_H + getContentHeight(win);
            if (mouseX >= win.x && mouseX <= win.x + win.w &&
                    mouseY >= win.y + TITLE_BAR_H && mouseY <= win.y + totalH) {
                List<String> logs = win.id.equals("void_escape") ?
                        AutoEscapeManager.getInstance().getLogEntries() :
                        HealthMonitorManager.getInstance().getLogEntries();
                int maxScroll = Math.max(0, logs.size() - LOG_VISIBLE);
                if (win.id.equals("void_escape")) {
                    voidLogScroll = Math.max(0, Math.min(maxScroll, voidLogScroll - (int)(verticalAmount * 2)));
                } else {
                    healthLogScroll = Math.max(0, Math.min(maxScroll, healthLogScroll - (int)(verticalAmount * 2)));
                }
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void close() {
        saveToolStates();
        this.client.setScreen(null);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void drawText(DrawContext context, String text, int x, int y, int color) {
        context.drawTextWithShadow(this.textRenderer, text, x, y, color);
    }

    private void drawCentered(DrawContext context, String text, int centerX, int y, int color) {
        int w = this.textRenderer.getWidth(text);
        context.drawTextWithShadow(this.textRenderer, text, centerX - w / 2, y, color);
    }
}
