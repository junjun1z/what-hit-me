package io.github.junjun1z.whathitme.server;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.github.junjun1z.whathitme.config.WhmConfig;
import io.github.junjun1z.whathitme.data.DamageTracker;
import io.github.junjun1z.whathitme.data.Entry;
import io.github.junjun1z.whathitme.data.PlayerHistory;
import io.github.junjun1z.whathitme.data.SearchEngine;
import io.github.junjun1z.whathitme.data.Snapshot;
import io.github.junjun1z.whathitme.net.PacketEntries;
import io.github.junjun1z.whathitme.net.PacketHello;
import io.github.junjun1z.whathitme.net.PacketMessage;
import io.github.junjun1z.whathitme.net.PacketRecap;
import io.github.junjun1z.whathitme.net.PacketRequest;
import io.github.junjun1z.whathitme.net.PacketServerInfo;
import io.github.junjun1z.whathitme.net.WhmNetwork;
import io.github.junjun1z.whathitme.text.DisplayOptions;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.FMLCommonHandler;

/** Server side of the network protocol and per-player settings of players without the mod. */
public final class ServerNet {

    private static final String KEY_CHAT_OFF = "whathitme_chatOff";
    private static final String KEY_IGNORED = "whathitme_ignored";

    public void onHello(EntityPlayerMP p, PacketHello msg) {
        p.getServerWorld().addScheduledTask(() -> {
            DamageTracker.SERVER.setModded(p.getUniqueID());
            WhmNetwork.channel.sendTo(info(), p);
        });
    }

    public static PacketServerInfo info() {
        WhmConfig.Common c = WhmConfig.common;
        PacketServerInfo i = new PacketServerInfo();
        i.mask = c.mask;
        i.summarySeconds = c.summarySeconds;
        i.ignored = new ArrayList<>(c.ignoredDamageIds);
        for (java.util.Map.Entry<String, String> e : c.renameMap.entrySet()) i.renames.add(e.getKey() + " = " + e.getValue());
        for (java.util.Map.Entry<String, String> e : c.hintMap.entrySet()) i.hints.add(e.getKey() + " = " + e.getValue());
        return i;
    }

    /** After a reload / config change: tell every client with the mod */
    public static void broadcastInfo() {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;
        for (UUID id : DamageTracker.SERVER.modded()) {
            EntityPlayerMP p = server.getPlayerList().getPlayerByUUID(id);
            if (p != null) WhmNetwork.channel.sendTo(info(), p);
        }
    }

    static boolean isAdmin(EntityPlayerMP p) {
        if (p.canUseCommand(2, "whm")) return true;
        MinecraftServer s = p.getServer();
        return s != null && !s.isDedicatedServer() && p.getName().equals(s.getServerOwner());
    }

    public void onRequest(EntityPlayerMP p, PacketRequest r) {
        p.getServerWorld().addScheduledTask(() -> handle(p, r));
    }

    private void handle(EntityPlayerMP p, PacketRequest r) {
        PlayerHistory h = DamageTracker.SERVER.get(p.getUniqueID());
        long now = System.currentTimeMillis();
        switch (r.kind) {
            case PacketRequest.HISTORY:
                send(p, new PacketRecap(PacketRecap.HISTORY, "", 0, h.snapshots().size(), new Snapshot(Snapshot.HISTORY, now, h.records(), -1, "")));
                break;
            case PacketRequest.LAST:
                send(p, new PacketRecap(PacketRecap.LAST, "", 0, 0, h.lastDeath()));
                break;
            case PacketRequest.SNAPSHOT: {
                int n = parse(r.arg, 1);
                List<Snapshot> list = h.snapshots();
                Snapshot s = n >= 1 && n <= list.size() ? list.get(n - 1) : null;
                send(p, new PacketRecap(PacketRecap.SNAPSHOT, "", n, list.size(), s));
                break;
            }
            case PacketRequest.INSPECT: {
                if (!isAdmin(p)) {
                    send(p, new PacketMessage(true, "whathitme.error.noperm"));
                    break;
                }
                EntityPlayerMP t = p.getServer().getPlayerList().getPlayerByUsername(r.arg);
                if (t == null) {
                    send(p, new PacketMessage(true, "whathitme.inspect.notfound", r.arg));
                    break;
                }
                PlayerHistory th = DamageTracker.SERVER.get(t.getUniqueID());
                send(p, new PacketRecap(PacketRecap.INSPECT_DEATH, t.getName(), 0, 0, th.lastDeath()));
                send(p, new PacketRecap(PacketRecap.INSPECT_HISTORY, t.getName(), 0, 0, new Snapshot(Snapshot.HISTORY, now, th.records(), -1, "")));
                break;
            }
            case PacketRequest.EFFECTS: {
                List<Entry> e = SearchEngine.effects(p);
                send(p, new PacketEntries(PacketEntries.EFFECTS, "", e.size(), e));
                break;
            }
            case PacketRequest.SEARCH: {
                List<Entry> all = SearchEngine.search(r.arg, DamageTracker.SERVER.catalog);
                List<Entry> part = all.size() > 40 ? new ArrayList<>(all.subList(0, 40)) : all;
                send(p, new PacketEntries(PacketEntries.SEARCH, r.arg, all.size(), part));
                break;
            }
            case PacketRequest.DUMP: {
                List<Entry> all = DamageTracker.SERVER.catalog.entries();
                send(p, new PacketEntries(PacketEntries.DUMP, "", all.size(), all.size() > 4000 ? new ArrayList<>(all.subList(0, 4000)) : all));
                break;
            }
            case PacketRequest.RELOAD:
                if (!isAdmin(p)) break; // the client already reloaded its own files
                WhmConfig.reloadCommon();
                broadcastInfo();
                send(p, new PacketMessage(false, "whathitme.reload.server"));
                break;
            default:
                break;
        }
    }

    private static void send(EntityPlayerMP p, net.minecraftforge.fml.common.network.simpleimpl.IMessage m) {
        WhmNetwork.channel.sendTo(m, p);
    }

    static int parse(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    // ------------------------------------------------ per-player settings (players without the mod), kept across deaths

    private static NBTTagCompound persisted(EntityPlayer p) {
        NBTTagCompound root = p.getEntityData();
        if (!root.hasKey(EntityPlayer.PERSISTED_NBT_TAG, Constants.NBT.TAG_COMPOUND)) root.setTag(EntityPlayer.PERSISTED_NBT_TAG, new NBTTagCompound());
        return root.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
    }

    public static boolean chatOff(EntityPlayer p) {
        return persisted(p).getBoolean(KEY_CHAT_OFF);
    }

    /** @return true when the summary is on now */
    public static boolean toggleChat(EntityPlayer p) {
        NBTTagCompound t = persisted(p);
        boolean off = !t.getBoolean(KEY_CHAT_OFF);
        t.setBoolean(KEY_CHAT_OFF, off);
        return !off;
    }

    public static List<String> personalIgnored(EntityPlayer p) {
        List<String> out = new ArrayList<>();
        NBTTagList l = persisted(p).getTagList(KEY_IGNORED, Constants.NBT.TAG_STRING);
        for (int i = 0; i < l.tagCount(); i++) out.add(l.getStringTagAt(i));
        return out;
    }

    /** @return true when the id is ignored now */
    public static boolean togglePersonalIgnore(EntityPlayer p, String id) {
        List<String> cur = personalIgnored(p);
        boolean now = !cur.remove(id);
        if (now) cur.add(id);
        NBTTagList l = new NBTTagList();
        for (String s : cur) l.appendTag(new NBTTagString(s));
        persisted(p).setTag(KEY_IGNORED, l);
        return now;
    }

    /** Display options for chat the server itself sends (players without the mod, console) */
    public static DisplayOptions options(EntityPlayer p, boolean clientActions) {
        WhmConfig.Common c = WhmConfig.common;
        DisplayOptions o = new DisplayOptions(c.mask, clientActions, c.summarySeconds).ignore(c.ignoredDamageIds).renames(c.renameMap).hints(c.hintMap);
        if (p != null) o.ignore(personalIgnored(p));
        return o;
    }
}
