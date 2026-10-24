package com.venus.vmtools.mixin;

import com.venus.vmtools.feature.freeze.FreezeManager;
import net.minecraft.client.network.ClientPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class LocalPlayerMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("VMTools/Freeze");

    /**
     * 类加载时打印验证信息，确认 Mixin 被 Fabric Loader 注入
     * （改用 SLF4J 而非 System.out，避免绕过日志级别直接写 stdout）
     */
    static {
        LOGGER.debug("LocalPlayerMixin loaded - freeze movement system active");
    }

    @Inject(method = "sendMovementPackets", at = @At("HEAD"), cancellable = true)
    private void onSendMovementPackets(CallbackInfo ci) {
        if (FreezeManager.isActive()) ci.cancel();
    }
}
