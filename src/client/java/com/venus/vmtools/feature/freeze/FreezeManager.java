package com.venus.vmtools.feature.freeze;

import com.venus.vmtools.VMToolsClient;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public class FreezeManager {

    private static FreezeManager instance;

    private boolean frozen = false;
    private long freezeStartTime = 0;
    private long freezeDuration = 3000;

    private FreezeManager() {}

    public static FreezeManager getInstance() {
        if (instance == null) instance = new FreezeManager();
        return instance;
    }

    public void register() {
        VMToolsClient.LOGGER.info("注册冻结管理器");
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    public static boolean isActive() {
        return instance != null && instance.frozen;
    }

    public boolean isFrozen() { return frozen; }

    public void toggle() {
        if (frozen) deactivate(); else activate(freezeDuration);
    }

    public void activate(long durationMs) {
        frozen = true;
        freezeStartTime = System.currentTimeMillis();
        this.freezeDuration = durationMs;
        VMToolsClient.LOGGER.info("冻结模式已激活，持续 {} ms", durationMs);
    }

    public void deactivate() {
        if (!frozen) return;
        frozen = false;
        freezeStartTime = 0;
        VMToolsClient.LOGGER.info("冻结模式已解除");
    }

    private void onClientTick(MinecraftClient client) {
        if (!frozen || client.player == null) return;
        if (System.currentTimeMillis() - freezeStartTime >= freezeDuration) deactivate();
    }

    public long getFreezeDuration() { return freezeDuration; }
    public void setFreezeDuration(long ms) { this.freezeDuration = Math.max(1000, ms); }
    public long getRemainingTime() {
        if (!frozen) return 0;
        return Math.max(0, freezeDuration - (System.currentTimeMillis() - freezeStartTime));
    }
}
