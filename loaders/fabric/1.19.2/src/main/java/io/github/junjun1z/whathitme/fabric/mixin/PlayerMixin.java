package io.github.junjun1z.whathitme.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.junjun1z.whathitme.fabric.WhatHitMeFabric;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

/**
 * Fabric API for 1.19 / 1.19.1 has no living entity damage events: report the damage before armor (start of
 * Player#actuallyHurt) and the health and absorption left after armor, effects and absorption (its end). Returns at
 * once for anything but a server player.
 */
@Mixin(Player.class)
public abstract class PlayerMixin {

    @Inject(method = "actuallyHurt", at = @At("HEAD"))
    private void whathitme$beforeDamage(DamageSource source, float amount, CallbackInfo ci) {
        Object self = this;
        if (self instanceof ServerPlayer) WhatHitMeFabric.beforeDamage((ServerPlayer) self, source, amount);
    }

    @Inject(method = "actuallyHurt", at = @At("TAIL"))
    private void whathitme$afterDamage(DamageSource source, float amount, CallbackInfo ci) {
        Object self = this;
        if (self instanceof ServerPlayer) WhatHitMeFabric.afterDamage((ServerPlayer) self, source);
    }
}
