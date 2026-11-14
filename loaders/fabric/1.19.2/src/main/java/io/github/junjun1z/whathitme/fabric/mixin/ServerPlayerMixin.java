package io.github.junjun1z.whathitme.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.junjun1z.whathitme.fabric.WhatHitMeFabric;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

/** Fabric API for 1.19 / 1.19.1 has no "after death" event: the start of ServerPlayer#die (the death message is still known). */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @Inject(method = "die", at = @At("HEAD"))
    private void whathitme$die(DamageSource source, CallbackInfo ci) {
        WhatHitMeFabric.onDeath((ServerPlayer) (Object) this, source);
    }
}
