package io.github.junjun1z.whathitme.integration.crafttweaker;

import io.github.junjun1z.whathitme.api.WhmPlayer;

import com.blamejared.crafttweaker.impl.entity.player.MCPlayerEntity;
import net.minecraft.world.entity.player.Player;

/** Player conversion between the API and CraftTweaker 5 / 6 (scripts see the MCPlayerEntity wrapper) */
final class CTEvents {

    private CTEvents() {}

    static MCPlayerEntity player(WhmPlayer p) {
        Object n = p == null ? null : p.getNativePlayer();
        return n instanceof Player ? new MCPlayerEntity((Player) n) : null;
    }

    static Object nativePlayer(MCPlayerEntity p) {
        return p == null ? null : p.getInternal();
    }
}
