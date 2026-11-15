package io.github.junjun1z.whathitme.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.junjun1z.whathitme.fabric.WhatHitMeFabric;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

/** Respawn and logout of a player (no Fabric API events on this version). */
@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

    @Inject(method = "respawn", at = @At("RETURN"))
    private void whathitme$respawn(ServerPlayer player, boolean alive, CallbackInfoReturnable<ServerPlayer> cir) {
        WhatHitMeFabric.onRespawn(cir.getReturnValue(), alive);
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void whathitme$remove(ServerPlayer player, CallbackInfo ci) {
        WhatHitMeFabric.onLogout(player);
    }
}
