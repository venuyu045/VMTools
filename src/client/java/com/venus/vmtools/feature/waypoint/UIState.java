package com.venus.vmtools.feature.waypoint;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.venus.vmtools.VMToolsClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UI 状态持久化 - 保存分组窗口位置、展开状态和渲染顺序
 */
public class UIState {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path STATE_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("vmtools").resolve("ui_state.json");

    // 分组窗口状态
    private Map<String, WindowState> windowStates = new HashMap<>();

    // 分组渲染顺序（最后 = 最上层）
    private List<String> groupRenderOrder = new ArrayList<>();

    public static class WindowState {
        public int x;
        public int y;
        public boolean expanded;
        public int width = -1; // -1 表示使用默认动态宽度
        public int maxHeight = -1; // -1 表示使用默认高度

        public WindowState() {}

        public WindowState(int x, int y, boolean expanded) {
            this.x = x;
            this.y = y;
            this.expanded = expanded;
        }
    }

    /**
     * 加载 UI 状态
     */
    public static UIState load() {
        if (Files.exists(STATE_PATH)) {
            try {
                String json = Files.readString(STATE_PATH);
                UIState state = GSON.fromJson(json, UIState.class);
                if (state != null && state.windowStates != null) {
                    // 确保 groupRenderOrder 不为 null（兼容旧版本数据）
                    if (state.groupRenderOrder == null) {
                        state.groupRenderOrder = new ArrayList<>();
                    }
                    return state;
                }
            } catch (Exception e) {
                VMToolsClient.LOGGER.warn("加载 UI 状态失败", e);
            }
        }
        return new UIState();
    }

    /**
     * 保存 UI 状态
     */
    public void save() {
        try {
            Files.createDirectories(STATE_PATH.getParent());
            Files.writeString(STATE_PATH, GSON.toJson(this));
        } catch (IOException e) {
            VMToolsClient.LOGGER.error("保存 UI 状态失败", e);
        }
    }

    /**
     * 获取分组窗口状态
     */
    public WindowState getWindowState(String groupId) {
        return windowStates.get(groupId);
    }

    /**
     * 设置分组窗口状态
     */
    public void setWindowState(String groupId, int x, int y, boolean expanded, int width, int maxHeight) {
        WindowState ws = new WindowState(x, y, expanded);
        ws.width = width;
        ws.maxHeight = maxHeight;
        windowStates.put(groupId, ws);
    }

    /**
     * 删除分组窗口状态
     */
    public void removeWindowState(String groupId) {
        windowStates.remove(groupId);
    }

    public Map<String, WindowState> getWindowStates() {
        return windowStates;
    }

    /**
     * 获取分组渲染顺序
     */
    public List<String> getGroupRenderOrder() {
        return groupRenderOrder;
    }

    /**
     * 设置分组渲染顺序
     */
    public void setGroupRenderOrder(List<String> groupRenderOrder) {
        this.groupRenderOrder = groupRenderOrder;
    }
}
