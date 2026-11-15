package io.github.junjun1z.whathitme.mc;

import java.util.List;
import java.util.UUID;

import io.github.junjun1z.whathitme.core.data.Entry;
import io.github.junjun1z.whathitme.core.server.PlayerHandle;
import io.github.junjun1z.whathitme.core.text.Txt;
import net.minecraft.Util;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * A 1.16.5 server player for the core. The loader adds the packet sending and where the per-player settings are kept.
 */
public abstract class McPlayer implements PlayerHandle {

    public final ServerPlayer p;

    protected McPlayer(ServerPlayer p) {
        this.p = p;
    }

    @Override
    public UUID uuid() {
        return p.getUUID();
    }

    @Override
    public String name() {
        return p.getGameProfile().getName();
    }

    @Override
    /** Permission level 2: an operator, or the owner of a single player / LAN world with cheats */
    public boolean isAdmin() {
        return p.hasPermissions(2);
    }

    @Override
    public boolean isAlive() {
        return p.isAlive() && !p.removed;
    }

    @Override
    public float health() {
        return p.getHealth();
    }

    @Override
    public float maxHealth() {
        return p.getMaxHealth();
    }

    @Override
    public int totems() {
        int n = 0;
        for (ItemStack s : new ItemStack[] {p.getMainHandItem(), p.getOffhandItem()}) {
            if (!s.isEmpty() && s.getItem() == Items.TOTEM_OF_UNDYING) n += s.getCount();
        }
        return n;
    }

    @Override
    public void chat(Txt line) {
        p.sendMessage(McText.of(line), Util.NIL_UUID);
    }

    @Override
    public List<Entry> effects() {
        return Registries116.effects(p);
    }

    @Override
    public String environment() {
        return Facts.environment(p);
    }

    @Override
    public io.github.junjun1z.whathitme.core.data.Gear.Item[] armor() {
        return Facts.armor(p);
    }

    @Override
    public String dimension() {
        return Facts.dimension(p.level);
    }

    @Override
    public double[] position() {
        return new double[] {p.getX(), p.getY(), p.getZ()};
    }
}
