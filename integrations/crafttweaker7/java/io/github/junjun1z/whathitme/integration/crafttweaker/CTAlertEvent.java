package io.github.junjun1z.whathitme.integration.crafttweaker;

import io.github.junjun1z.whathitme.api.AlertEvent;

import com.blamejared.crafttweaker.api.annotations.ZenRegister;
import org.openzen.zencode.java.ZenCodeType;
import net.minecraft.world.entity.player.Player;

/** A totem / heavy hit / prevented death alert */
@ZenRegister
@ZenCodeType.Name("mods.whathitme.AlertEvent")
public class CTAlertEvent {

    final AlertEvent e;

    CTAlertEvent(AlertEvent e) {
        this.e = e;
    }

    @ZenCodeType.Getter("player")
    public Player player() {
        return CTEvents.player(e.getPlayer());
    }

    @ZenCodeType.Getter("kind")
    public String kind() {
        return e.getKind();
    }

    @ZenCodeType.Getter("damage")
    public CTDamage damage() {
        return CTDamage.of(e.getDamage());
    }

    @ZenCodeType.Getter("snapshot")
    public CTDeath snapshot() {
        return CTDeath.of(e.getSnapshot());
    }
}
