package com.venus.vmtools.feature.freeze;

import com.venus.vmtools.VMToolsClient;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

/**
 * 冻结管理器 — 在延迟传送等待期间冻结玩家移动包发送
 *
 * 核心原理：
 * 1. 服务端的延迟传送插件通过 PlayerMoveEvent 检测玩家是否移动
 * 2. 客户端收到移动包后才会触发 PlayerMoveEvent
 * 3. 冻结期间拦截所有改变位置的移动包（Pos / PosRot），保留纯视角包（Rot）
 * 4. 服务端看不到位置变化 → 传送不会被取消
 *
 * 触发方式：点击路径点传送时自动激活
 */
public class FreezeManager {

    private static FreezeManager instance;

    /** 是否处于冻结状态 */
    private boolean frozen = false;

    /** 冻结开始时间（System.currentTimeMillis） */
    private long freezeStartTime = 0;

    /** 冻结持续时间（毫秒），默认 3 秒 */
    private long freezeDuration = 3000;

    private FreezeManager() {}

    /**
     * 获取单例
     */
    public static FreezeManager getInstance() {
        if (instance == null) {
            instance = new FreezeManager();
        }
        return instance;
    }

    /**
     * 注册事件监听器
     */
    public void register() {
        VMToolsClient.LOGGER.info("注册冻结管理器");

        // Tick 事件：自动解除冻结 + Overlay 显示倒计时
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    /**
     * 全局静态方法，供 Mixin 调用（无需持有实例引用）
     */
    public static boolean isActive() {
        return instance != null && instance.frozen;
    }

    /**
     * 是否处于冻结状态
     */
    public boolean isFrozen() {
        return frozen;
    }

    /**
     * 切换冻结状态
     */
    public void toggle() {
        if (frozen) {
            deactivate();
        } else {
            activate(freezeDuration);
        }
    }

    /**
     * 激活冻结模式
     *
     * @param durationMs 冻结持续时间（毫秒）
     */
    public void activate(long durationMs) {
        frozen = true;
        freezeStartTime = System.currentTimeMillis();
        this.freezeDuration = durationMs;

        VMToolsClient.LOGGER.info("冻结模式已激活，持续 {} ms", durationMs);
    }

    /**
     * 解除冻结模式
     */
    public void deactivate() {
        if (!frozen) return;

        frozen = false;
        freezeStartTime = 0;

        VMToolsClient.LOGGER.info("冻结模式已解除");
    }

    /**
     * 客户端 Tick — 自动解除 + Overlay 显示
     */
    private void onClientTick(Minecraft client) {
        if (!frozen) return;
        if (client.player == null) return;

        long elapsed = System.currentTimeMillis() - freezeStartTime;

        // 超时自动解除
        if (elapsed >= freezeDuration) {
            deactivate();
        }
    }

    // ---- 配置相关 ----

    public long getFreezeDuration() {
        return freezeDuration;
    }

    public void setFreezeDuration(long durationMs) {
        this.freezeDuration = Math.max(1000, durationMs); // 最少 1 秒
    }

    /**
     * 获取剩余冻结时间（毫秒），未冻结时返回 0
     */
    public long getRemainingTime() {
        if (!frozen) return 0;
        return Math.max(0, freezeDuration - (System.currentTimeMillis() - freezeStartTime));
    }
}
