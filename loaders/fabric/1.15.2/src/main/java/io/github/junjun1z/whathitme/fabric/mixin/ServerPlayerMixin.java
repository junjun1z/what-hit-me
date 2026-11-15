package io.github.junjun1z.whathitme.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.junjun1z.whathitme.fabric.WhatHitMeFabric;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

/** A player's death (no Fabric API event on this version), before the combat tracker is reset. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @Inject(method = "die", at = @At("HEAD"))
    private void whathitme$die(DamageSource source, CallbackInfo ci) {
        WhatHitMeFabric.onDie((ServerPlayer) (Object) this, source);
    }
}
