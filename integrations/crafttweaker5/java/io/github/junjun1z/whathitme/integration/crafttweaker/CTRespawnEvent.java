package io.github.junjun1z.whathitme.integration.crafttweaker;

import io.github.junjun1z.whathitme.api.RespawnEvent;

import com.blamejared.crafttweaker.api.annotations.ZenRegister;
import org.openzen.zencode.java.ZenCodeType;
import com.blamejared.crafttweaker.impl.entity.player.MCPlayerEntity;

/** The player respawned after a death */
@ZenRegister
@ZenCodeType.Name("mods.whathitme.RespawnEvent")
public class CTRespawnEvent {

    final RespawnEvent e;

    CTRespawnEvent(RespawnEvent e) {
        this.e = e;
    }

    @ZenCodeType.Getter("player")
    public MCPlayerEntity player() {
        return CTEvents.player(e.getPlayer());
    }

    @ZenCodeType.Getter("death")
    public CTDeath death() {
        return CTDeath.of(e.getDeath());
    }
}
