package io.github.junjun1z.whathitme.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import io.github.junjun1z.whathitme.fabric.WhatHitMeFabric;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.CombatTracker;

/**
 * Custom death messages (pack deathMessages): the message ServerPlayer#die builds from the combat tracker is replaced
 * before it is sent to the death screen, the chat of every player and the server log. Unchanged when no entry matches.
 */
@Mixin(ServerPlayer.class)
public abstract class DeathMessageMixin {

    @Redirect(method = "die", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/damagesource/CombatTracker;getDeathMessage()Lnet/minecraft/network/chat/Component;"))
    private Component whathitme$deathMessage(CombatTracker tracker) {
        return WhatHitMeFabric.deathMessage((ServerPlayer) (Object) this, tracker.getDeathMessage());
    }
}
