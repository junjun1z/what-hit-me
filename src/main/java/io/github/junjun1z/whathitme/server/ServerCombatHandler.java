package io.github.junjun1z.whathitme.server;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.github.junjun1z.whathitme.config.WhmConfig;
import io.github.junjun1z.whathitme.config.WhmConfig.Flag;
import io.github.junjun1z.whathitme.data.DamageRecord;
import io.github.junjun1z.whathitme.data.DamageTracker;
import io.github.junjun1z.whathitme.data.PlayerHistory;
import io.github.junjun1z.whathitme.data.Snapshot;
import io.github.junjun1z.whathitme.data.Sources;
import io.github.junjun1z.whathitme.net.PacketRecap;
import io.github.junjun1z.whathitme.net.WhmNetwork;
import io.github.junjun1z.whathitme.text.Msg;
import io.github.junjun1z.whathitme.text.RecapFormatter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Records damage taken by players on the logical server. Every handler returns on its first line for anything that is
 * not a player, so mob fights cost nothing. No packet is sent for normal hits.
 */
public final class ServerCombatHandler {

    /** Lethal / heavy hits whose outcome (death, totem) is known at the end of the tick */
    private static final class Pending {
        final UUID id;
        final DamageRecord record;
        final boolean lethal;
        final boolean heavy;
        final int totems;

        Pending(UUID id, DamageRecord record, boolean lethal, boolean heavy, int totems) {
            this.id = id;
            this.record = record;
            this.lethal = lethal;
            this.heavy = heavy;
            this.totems = totems;
        }
    }

    private final List<Pending> pending = new ArrayList<>();

    private static boolean tracked(EntityPlayer p) {
        return p instanceof EntityPlayerMP && !(p instanceof FakePlayer) && !p.world.isRemote;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onHurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer p = (EntityPlayer) event.getEntityLiving();
        if (!tracked(p) || event.getAmount() <= 0) return;
        PlayerHistory h = DamageTracker.SERVER.get(p.getUniqueID());
        long tick = p.world.getTotalWorldTime();
        h.pendingRaw = event.getAmount();
        h.pendingAbsorption = p.getAbsorptionAmount();
        h.pendingSourceHash = System.identityHashCode(event.getSource());
        h.pendingTick = tick;
        h.pendingBlocking = p.isActiveItemStackBlocking();
        // a fresh hit (hurtResistantTime just reset to max) shortly after the previous one: i-frames were bypassed
        long since = tick - h.lastHurtTick;
        h.pendingIFrameBypass = p.hurtResistantTime >= p.maxHurtResistantTime && since >= 1 && since < p.maxHurtResistantTime / 2;
        h.lastHurtTick = tick;
    }

    /**
     * Some mods (e.g. body-part health mods) cancel LivingHurtEvent and apply the damage their own way, so no
     * LivingDamageEvent follows. Record those hits here with the amount they took over.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onHurtCancelled(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        if (!event.isCanceled()) return;
        EntityPlayer p = (EntityPlayer) event.getEntityLiving();
        if (!tracked(p)) return;
        PlayerHistory h = DamageTracker.SERVER.peek(p.getUniqueID());
        if (h == null || h.pendingTick != p.world.getTotalWorldTime() || h.pendingSourceHash != System.identityHashCode(event.getSource())) return;
        h.pendingTick = Long.MIN_VALUE;
        float amount = Math.max(0f, event.getAmount());
        if (amount < WhmConfig.common.minDamageThreshold || amount <= 0f) return;
        DamageRecord r = build(p, event.getSource(), h.pendingRaw, amount, 0f, 0, p.getHealth());
        h.add(r, (long) (WhmConfig.common.mergeWindowSeconds * 1000f));
        DamageTracker.SERVER.catalog.register(r);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDamage(LivingDamageEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer p = (EntityPlayer) event.getEntityLiving();
        if (!tracked(p)) return;
        PlayerHistory h = DamageTracker.SERVER.get(p.getUniqueID());
        DamageSource src = event.getSource();
        long tick = p.world.getTotalWorldTime();
        boolean match = h.pendingTick == tick && h.pendingSourceHash == System.identityHashCode(src);
        h.pendingTick = Long.MIN_VALUE;
        float actual = Math.max(0f, event.getAmount());
        float absorbed = match ? Math.max(0f, h.pendingAbsorption - p.getAbsorptionAmount()) : 0f;
        float taken = actual + absorbed;
        WhmConfig.Common c = WhmConfig.common;
        if (taken < c.minDamageThreshold || taken <= 0) return;
        float raw = match ? h.pendingRaw : -1f;
        int extra = 0;
        if (match && h.pendingBlocking) extra |= DamageRecord.THROUGH_SHIELD;
        if (match && h.pendingIFrameBypass) extra |= DamageRecord.IGNORES_IFRAMES;
        float health = p.getHealth();
        DamageRecord r = build(p, src, raw, actual, absorbed, extra, Math.max(0f, health - actual));
        DamageRecord stored = h.add(r, (long) (c.mergeWindowSeconds * 1000f));
        DamageTracker.SERVER.catalog.register(r);
        boolean lethal = health - actual <= 0f;
        boolean heavy = p.getMaxHealth() > 0 && taken >= c.heavyHitThreshold * p.getMaxHealth();
        if (lethal || heavy) pending.add(new Pending(p.getUniqueID(), stored, lethal, heavy, totems(p)));
    }

    static DamageRecord build(EntityPlayer p, DamageSource src, float raw, float actual, float absorbed, int extraFlags, float healthAfter) {
        Entity atk = Sources.attacker(src);
        Entity direct = src.getImmediateSource();
        String id = Sources.damageId(src, atk);
        String modId;
        if (atk != null && !(atk instanceof EntityPlayer) && !"minecraft".equals(Sources.modOfEntity(atk))) modId = Sources.modOfEntity(atk);
        else if (id.startsWith("mob:")) modId = Sources.modOfEntity(atk);
        else modId = id.substring(0, Math.max(0, id.indexOf(':')));
        int flags = Sources.sourceFlags(src) | extraFlags;
        float dist = -1f;
        String atkName = "";
        String atkKey = "";
        String atkId = "";
        String owner = "";
        String tag = "";
        String weaponName = "";
        String weaponKey = "";
        String ench = "";
        if (atk != null && atk != p) {
            dist = p.getDistance(atk);
            atkName = Sources.entityName(atk);
            atkKey = Sources.entityNameKey(atk);
            atkId = Sources.entityId(atk);
            owner = Sources.ownerName(atk);
            tag = atk.getUniqueID().toString();
            if (atk.isInvisible()) flags |= DamageRecord.INVISIBLE;
            ItemStack w = Sources.weapon(atk);
            if (!w.isEmpty()) {
                weaponName = w.getDisplayName();
                weaponKey = Sources.itemKey(w);
                ench = Sources.enchantments(w);
            }
        }
        String directName = direct != null && direct != atk && direct != p ? Sources.entityName(direct) : "";
        return new DamageRecord(System.currentTimeMillis(), id, src.getDamageType(), atkName, atkKey, atkId, directName, modId, Sources.modName(modId), raw,
            actual, absorbed, flags, dist, weaponName, weaponKey, ench, owner, 1, healthAfter, p.getMaxHealth(), tag);
    }

    private static int totems(EntityPlayer p) {
        int n = 0;
        for (ItemStack s : new ItemStack[] {p.getHeldItemMainhand(), p.getHeldItemOffhand()}) {
            if (!s.isEmpty() && s.getItem() == Items.TOTEM_OF_UNDYING) n += s.getCount();
        }
        return n;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDeath(LivingDeathEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer p = (EntityPlayer) event.getEntityLiving();
        if (!tracked(p)) return;
        EntityPlayerMP mp = (EntityPlayerMP) p;
        UUID uid = p.getUniqueID();
        PlayerHistory h = DamageTracker.SERVER.get(uid);
        long now = System.currentTimeMillis();
        DamageRecord last = h.last();
        DamageRecord killing;
        if (last != null && now - last.time <= 1500 && !last.has(DamageRecord.KILLING)) {
            killing = last.withFlags(DamageRecord.KILLING);
            h.replace(last, killing);
        } else {
            // killed without a recorded hit (setHealth, a cancelled damage event, ...)
            killing = build(p, event.getSource(), -1f, 0f, 0f, DamageRecord.KILLING, 0f);
            h.add(killing, 0);
            DamageTracker.SERVER.catalog.register(killing);
        }
        WhmConfig.Common c = WhmConfig.common;
        List<DamageRecord> recs = h.recent(now, c.recapSeconds * 1000L, 30);
        int key = recs.indexOf(killing);
        if (key < 0) {
            recs.add(killing);
            key = recs.size() - 1;
        }
        String msg = "";
        try {
            msg = ITextComponent.Serializer.componentToJson(p.getCombatTracker().getDeathMessage());
        } catch (Throwable ignored) {
            // no message
        }
        Snapshot s = new Snapshot(Snapshot.DEATH, now, recs, key, msg);
        h.setLastDeath(s);
        h.summaryPending = true;
        h.lastHurtTick = Long.MIN_VALUE; // the respawned player starts without invulnerability frames
        for (int i = pending.size() - 1; i >= 0; i--) if (pending.get(i).id.equals(uid)) pending.remove(i);
        if (DamageTracker.SERVER.isModded(uid)) WhmNetwork.channel.sendTo(new PacketRecap(PacketRecap.DEATH, "", 0, 0, s), mp);
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.player instanceof EntityPlayerMP) || event.isEndConquered()) return;
        EntityPlayerMP p = (EntityPlayerMP) event.player;
        PlayerHistory h = DamageTracker.SERVER.peek(p.getUniqueID());
        if (h == null || !h.summaryPending || h.lastDeath() == null) return;
        h.summaryPending = false;
        if (DamageTracker.SERVER.isModded(p.getUniqueID())) return; // that client shows its own (translated) summary
        if ((WhmConfig.common.mask & Flag.RESPAWN_CHAT_SUMMARY.bit()) == 0 || ServerNet.chatOff(p)) return;
        RecapFormatter f = new RecapFormatter(Msg.ENGLISH, ServerNet.options(p, false));
        for (ITextComponent line : f.respawnSummary(h.lastDeath())) p.sendMessage(line);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || pending.isEmpty()) return;
        List<Pending> list = new ArrayList<>(pending);
        pending.clear();
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;
        long now = System.currentTimeMillis();
        WhmConfig.Common c = WhmConfig.common;
        for (Pending q : list) {
            EntityPlayerMP p = server.getPlayerList().getPlayerByUUID(q.id);
            if (p == null || p.isDead || p.getHealth() <= 0f) continue;
            int kind;
            int mark;
            if (q.lethal) {
                kind = totems(p) < q.totems ? Snapshot.TOTEM : Snapshot.PREVENTED;
                mark = DamageRecord.TOTEM;
            } else if (q.heavy && (c.mask & Flag.HEAVY_HIT_SNAPSHOT.bit()) != 0) {
                kind = Snapshot.HEAVY;
                mark = DamageRecord.HEAVY;
            } else {
                continue;
            }
            PlayerHistory h = DamageTracker.SERVER.get(q.id);
            DamageRecord marked = q.record.withFlags(mark);
            if (!h.replace(q.record, marked)) marked = h.last() == null ? marked : h.last();
            List<DamageRecord> recs = h.recent(now, c.recapSeconds * 1000L, 30);
            int key = recs.indexOf(marked);
            if (key < 0) key = recs.size() - 1;
            Snapshot s = new Snapshot(kind, now, recs, key, "");
            h.addSnapshot(s, c.maxSnapshots);
            if (DamageTracker.SERVER.isModded(q.id)) {
                WhmNetwork.channel.sendTo(new PacketRecap(PacketRecap.ALERT, "", 1, h.snapshots().size(), s), p);
            } else {
                Flag f = kind == Snapshot.HEAVY ? Flag.HEAVY_HIT_SNAPSHOT : Flag.TOTEM_CHAT_ALERT;
                if ((c.mask & f.bit()) != 0) p.sendMessage(new RecapFormatter(Msg.ENGLISH, ServerNet.options(p, false)).alert(s, 1));
            }
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.player.getUniqueID();
        DamageTracker.SERVER.remove(id);
        for (int i = pending.size() - 1; i >= 0; i--) if (pending.get(i).id.equals(id)) pending.remove(i);
    }

    public void clear() {
        pending.clear();
    }
}
