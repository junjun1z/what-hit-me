package io.github.junjun1z.whathitme.integration.minetweaker;

import io.github.junjun1z.whathitme.api.WhmPlayer;

import minetweaker.api.minecraft.MineTweakerMC;
import minetweaker.api.player.IPlayer;
import net.minecraft.entity.player.EntityPlayer;

/** Player conversion between the API and CraftTweaker */
final class ZenEvents {

    private ZenEvents() {}

    static IPlayer player(WhmPlayer p) {
        Object n = p == null ? null : p.getNativePlayer();
        return n instanceof EntityPlayer ? MineTweakerMC.getIPlayer((EntityPlayer) n) : null;
    }

    static Object nativePlayer(IPlayer p) {
        return p == null ? null : MineTweakerMC.getPlayer(p);
    }
}
