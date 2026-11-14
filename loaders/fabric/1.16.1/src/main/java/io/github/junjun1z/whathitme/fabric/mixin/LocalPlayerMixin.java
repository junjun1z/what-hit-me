package io.github.junjun1z.whathitme.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.junjun1z.whathitme.fabric.WhatHitMeFabricClient;
import net.minecraft.client.player.LocalPlayer;

/** Client /whm (no Fabric API client commands on this version): chat lines starting with /whm are run here. */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    @Inject(method = "chat", at = @At("HEAD"), cancellable = true)
    private void whathitme$chat(String message, CallbackInfo ci) {
        if (WhatHitMeFabricClient.onChat(message)) ci.cancel();
    }
}
