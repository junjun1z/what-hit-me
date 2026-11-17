package io.github.junjun1z.whathitme.integration.crafttweaker;

import io.github.junjun1z.whathitme.api.WhmPlayer;

import crafttweaker.api.minecraft.CraftTweakerMC;
import crafttweaker.api.player.IPlayer;
import net.minecraft.entity.player.EntityPlayer;

/** Player conversion between the API and CraftTweaker */
final class ZenEvents {

    private ZenEvents() {}

    static IPlayer player(WhmPlayer p) {
        Object n = p == null ? null : p.getNativePlayer();
        return n instanceof EntityPlayer ? CraftTweakerMC.getIPlayer((EntityPlayer) n) : null;
    }

    static Object nativePlayer(IPlayer p) {
        return p == null ? null : CraftTweakerMC.getPlayer(p);
    }
}
