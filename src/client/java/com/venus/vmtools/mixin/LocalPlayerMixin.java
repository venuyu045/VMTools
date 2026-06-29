package com.venus.vmtools.mixin;

import com.venus.vmtools.feature.freeze.FreezeManager;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin: 在 LocalPlayer.sendPosition() 冻结移动包发送
 *
 * MC 26.2 中，LocalPlayer.sendPosition() 是客户端每 tick 发送移动包的核心方法。
 * 它检测玩家的位置/视角变化，创建对应的 ServerboundMovePlayerPacket 并发送。
 *
 * 冻结模式下直接取消 sendPosition() 执行 → 所有移动包（含 Pos/PosRot/Rot）
 * 都不发送 → 服务端看不到任何位置变化 → PlayerMoveEvent 不会触发 → 传送不被取消。
 *
 * 3 秒内不发移动包不会被踢（keep-alive 由 ping 包处理），经验证安全。
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    /**
     * 类加载时打印验证信息，确认 Mixin 被 Fabric Loader 注入
     */
    static {
        System.out.println("[VMTools] LocalPlayerMixin loaded - freeze movement system active");
    }

    @Inject(method = "sendPosition", at = @At("HEAD"), cancellable = true)
    private void onSendPosition(CallbackInfo ci) {
        if (FreezeManager.isActive()) {
            ci.cancel();
        }
    }
}
