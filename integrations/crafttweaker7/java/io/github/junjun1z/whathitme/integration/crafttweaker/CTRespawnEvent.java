package io.github.junjun1z.whathitme.integration.crafttweaker;

import io.github.junjun1z.whathitme.api.RespawnEvent;

import com.blamejared.crafttweaker.api.annotations.ZenRegister;
import org.openzen.zencode.java.ZenCodeType;
import net.minecraft.world.entity.player.Player;

/** The player respawned after a death */
@ZenRegister
@ZenCodeType.Name("mods.whathitme.RespawnEvent")
public class CTRespawnEvent {

    final RespawnEvent e;

    CTRespawnEvent(RespawnEvent e) {
        this.e = e;
    }

    @ZenCodeType.Getter("player")
    public Player player() {
        return CTEvents.player(e.getPlayer());
    }

    @ZenCodeType.Getter("death")
    public CTDeath death() {
        return CTDeath.of(e.getDeath());
    }
}
