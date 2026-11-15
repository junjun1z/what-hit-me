package io.github.junjun1z.whathitme.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.junjun1z.whathitme.fabric.WhatHitMeFabric;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

/**
 * Damage taken by players (no Fabric API event on this version): before armor (first line of actuallyHurt, where Forge
 * fires LivingHurtEvent) and after the damage is applied (last line). Both return at once for non-server players.
 */
@Mixin(Player.class)
public abstract class PlayerMixin {

    @Inject(method = "attack", at = @At("HEAD"))
    private void whathitme$attack(net.minecraft.world.entity.Entity target, CallbackInfo ci) {
        WhatHitMeFabric.onAttack((Player) (Object) this, target);
    }

    @Inject(method = "actuallyHurt", at = @At("HEAD"))
    private void whathitme$hurt(DamageSource source, float amount, CallbackInfo ci) {
        WhatHitMeFabric.onHurt((Player) (Object) this, source, amount);
    }

    @Inject(method = "actuallyHurt", at = @At("TAIL"))
    private void whathitme$afterHurt(DamageSource source, float amount, CallbackInfo ci) {
        WhatHitMeFabric.afterHurt((Player) (Object) this, source);
    }
}
