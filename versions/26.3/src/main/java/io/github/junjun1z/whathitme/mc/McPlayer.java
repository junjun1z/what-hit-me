package io.github.junjun1z.whathitme.mc;

import java.util.List;
import java.util.UUID;

import io.github.junjun1z.whathitme.core.data.Entry;
import io.github.junjun1z.whathitme.core.server.PlayerHandle;
import io.github.junjun1z.whathitme.core.text.Txt;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * A 26.3 server player for the core. The loader adds the packet sending and where the per-player settings are kept.
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
        return p.getGameProfile().name();
    }

    /** Permission level 2: an operator, or the owner of a single player / LAN world with cheats */
    @Override
    public boolean isAdmin() {
        return p.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
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
        return Registries121.effects(p);
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
        return Facts.dimension(p.level());
    }

    @Override
    public double[] position() {
        return new double[] {p.getX(), p.getY(), p.getZ()};
    }

    @Override
    public Boolean hasAdvancement(String id) {
        net.minecraft.resources.Identifier rl = net.minecraft.resources.Identifier.tryParse(id);
        net.minecraft.server.MinecraftServer s = p.level().getServer();
        if (rl == null || s == null) return false;
        net.minecraft.advancements.AdvancementHolder a = s.getAdvancements().get(rl);
        return a != null && p.getAdvancements().getOrStartProgress(a).isDone();
    }

    @Override
    public Boolean hasTag(String tag) {
        return p.entityTags().contains(tag);
    }

    @Override
    public Long score(String objective) {
        net.minecraft.world.scores.Scoreboard sb = p.level().getScoreboard();
        net.minecraft.world.scores.Objective o = sb.getObjective(objective);
        if (o == null) return NO_SCORE;
        net.minecraft.world.scores.ReadOnlyScoreInfo i = sb.getPlayerScoreInfo(p, o);
        return i == null ? NO_SCORE : (long) i.value();
    }

    @Override
    public Object nativePlayer() {
        return p;
    }

    @Override
    public boolean disconnect(Txt reason) {
        p.connection.disconnect(McText.of(reason));
        return true;
    }
}
