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
 * A 1.20.2 server player for the core. The loader adds the packet sending and where the per-player settings are kept.
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
        return Registries1202.effects(p);
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
        net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.tryParse(id);
        net.minecraft.server.MinecraftServer s = p.getServer();
        if (rl == null || s == null) return false;
        net.minecraft.advancements.AdvancementHolder a = s.getAdvancements().get(rl);
        return a != null && p.getAdvancements().getOrStartProgress(a).isDone();
    }

    @Override
    public Boolean hasTag(String tag) {
        return p.getTags().contains(tag);
    }

    // this source also runs on 1.20.3 / 1.20.4 (ScoreHolder instead of String owners): score methods found by signature
    private static java.lang.reflect.Method scoreHas, scoreGet, scoreInfo;
    private static volatile java.lang.reflect.Method scoreValue;

    @Override
    public Long score(String objective) {
        net.minecraft.world.scores.Scoreboard sb = p.getScoreboard();
        net.minecraft.world.scores.Objective o = sb.getObjective(objective);
        if (o == null) return NO_SCORE;
        try {
            if (scoreValue == null) findScoreMethods();
            if (scoreInfo != null) {
                Object i = scoreInfo.invoke(sb, p, o);
                return i == null ? NO_SCORE : ((Number) scoreValue.invoke(i)).longValue();
            }
            String name = p.getScoreboardName();
            // hasPlayerScore first: getOrCreatePlayerScore would create one
            if (!(Boolean) scoreHas.invoke(sb, name, o)) return NO_SCORE;
            return ((Number) scoreValue.invoke(scoreGet.invoke(sb, name, o))).longValue();
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new IllegalStateException("scoreboard API not found", e);
        }
    }

    private static synchronized void findScoreMethods() {
        if (scoreValue != null) return;
        for (java.lang.reflect.Method m : net.minecraft.world.scores.Scoreboard.class.getMethods()) {
            Class<?>[] t = m.getParameterTypes();
            if (t.length != 2 || t[1] != net.minecraft.world.scores.Objective.class) continue;
            Class<?> r = m.getReturnType();
            if (t[0] == String.class) {
                if (r == boolean.class) scoreHas = m; // hasPlayerScore
                else if (r != void.class) scoreGet = m; // getOrCreatePlayerScore -> Score
            } else if (r.isInterface() && t[0].isAssignableFrom(ServerPlayer.class) && !hasVoidMethod(r)) {
                scoreInfo = m; // getPlayerScoreInfo -> ReadOnlyScoreInfo (ScoreAccess has setters)
            }
        }
        Class<?> holder = scoreInfo != null ? scoreInfo.getReturnType() : scoreGet != null && scoreHas != null ? scoreGet.getReturnType() : null;
        if (holder == null) throw new IllegalStateException("no scoreboard getter");
        for (java.lang.reflect.Method m : holder.getMethods()) {
            if (m.getParameterCount() == 0 && m.getReturnType() == int.class && !java.lang.reflect.Modifier.isStatic(m.getModifiers())
                && (!holder.isInterface() || java.lang.reflect.Modifier.isAbstract(m.getModifiers()))) {
                scoreValue = m; // Score#getScore / ReadOnlyScoreInfo#value
                return;
            }
        }
        throw new IllegalStateException("no score value getter");
    }

    private static boolean hasVoidMethod(Class<?> c) {
        for (java.lang.reflect.Method m : c.getMethods()) if (m.getReturnType() == void.class) return true;
        return false;
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
