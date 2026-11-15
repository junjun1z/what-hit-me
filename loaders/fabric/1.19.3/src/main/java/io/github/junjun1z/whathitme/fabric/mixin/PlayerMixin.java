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
 * Fabric API has no "after damage" event before Minecraft 1.21: report the health and absorption left once armor,
 * effects and absorption were applied (end of Player#actuallyHurt). Returns at once for anything but a server player.
 */
@Mixin(Player.class)
public abstract class PlayerMixin {

    @Inject(method = "actuallyHurt", at = @At("TAIL"))
    private void whathitme$afterDamage(DamageSource source, float amount, CallbackInfo ci) {
        Object self = this;
        if (self instanceof ServerPlayer) WhatHitMeFabric.afterDamage((ServerPlayer) self, source);
    }
}
