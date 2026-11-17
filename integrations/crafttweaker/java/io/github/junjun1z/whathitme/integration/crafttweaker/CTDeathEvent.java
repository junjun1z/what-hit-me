package io.github.junjun1z.whathitme.integration.crafttweaker;

import io.github.junjun1z.whathitme.api.DeathEvent;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import net.minecraft.world.entity.player.Player;
import org.openzen.zencode.java.ZenCodeType;

/** The player died: change the killing blow, add summary lines */
@ZenRegister
@ZenCodeType.Name("mods.whathitme.DeathEvent")
public class CTDeathEvent {

    final DeathEvent e;

    CTDeathEvent(DeathEvent e) {
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

    @ZenCodeType.Getter("killingBlow")
    public CTDamage killingBlow() {
        return CTDamage.of(e.getKillingBlow());
    }

    @ZenCodeType.Method
    public void setName(String name) {
        e.setName(name);
    }

    @ZenCodeType.Method
    public void setHint(String hint) {
        e.setHint(hint);
    }

    @ZenCodeType.Method
    public void addBadge(String text) {
        e.addBadge(text);
    }

    @ZenCodeType.Method
    public void hide(String field) {
        e.hide(field);
    }

    @ZenCodeType.Method
    public void addSummaryLine(String line) {
        e.addSummaryLine(line);
    }
}
