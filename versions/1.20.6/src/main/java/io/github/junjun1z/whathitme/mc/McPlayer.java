package io.github.junjun1z.whathitme.mc;

import java.util.List;
import java.util.UUID;

import io.github.junjun1z.whathitme.core.data.Entry;
import io.github.junjun1z.whathitme.core.server.PlayerHandle;
import io.github.junjun1z.whathitme.core.text.Txt;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * A 1.20.6 server player for the core. The loader adds the packet sending and where the per-player settings are kept.
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
    public boolean isAdmin() {
        if (p.hasPermissions(2)) return true;
        MinecraftServer s = p.getServer();
        return s != null && s.isSingleplayerOwner(p.getGameProfile());
    }

    @Override
    public boolean isAlive() {
        return p.isAlive() && !p.isRemoved();
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
            if (!s.isEmpty() && s.is(Items.TOTEM_OF_UNDYING)) n += s.getCount();
        }
        return n;
    }

    @Override
    public void chat(Txt line) {
        p.sendSystemMessage(McText.of(line));
    }

    @Override
    public List<Entry> effects() {
        return Registries1206.effects(p);
    }
}
