package io.github.junjun1z.whathitme.integration.crafttweaker;

import io.github.junjun1z.whathitme.api.WhmPlayer;

import net.minecraft.world.entity.player.Player;

/** Player conversion between the API and CraftTweaker (scripts see the Minecraft player itself) */
final class CTEvents {

    private CTEvents() {}

    static Player player(WhmPlayer p) {
        Object n = p == null ? null : p.getNativePlayer();
        return n instanceof Player ? (Player) n : null;
    }
}
