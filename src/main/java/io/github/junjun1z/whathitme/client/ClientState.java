package io.github.junjun1z.whathitme.client;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.github.junjun1z.whathitme.config.WhmConfig;
import io.github.junjun1z.whathitme.config.WhmConfig.Flag;
import io.github.junjun1z.whathitme.data.DamageCatalog;
import io.github.junjun1z.whathitme.data.DamageRecord;
import io.github.junjun1z.whathitme.data.DumpWriter;
import io.github.junjun1z.whathitme.data.Entry;
import io.github.junjun1z.whathitme.data.PlayerHistory;
import io.github.junjun1z.whathitme.data.SearchEngine;
import io.github.junjun1z.whathitme.data.Snapshot;
import io.github.junjun1z.whathitme.net.PacketEntries;
import io.github.junjun1z.whathitme.net.PacketMessage;
import io.github.junjun1z.whathitme.net.PacketRecap;
import io.github.junjun1z.whathitme.net.PacketServerInfo;
import io.github.junjun1z.whathitme.text.DisplayOptions;
import io.github.junjun1z.whathitme.text.Msg;
import io.github.junjun1z.whathitme.text.RecapFormatter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Client side state. Only touched on the client thread. */
@SideOnly(Side.CLIENT)
public final class ClientState {

    /** The server answered our hello: accurate data comes from it */
    public static boolean serverHasMod;
    public static PacketServerInfo serverInfo;
    static boolean helloSent;

    /** Latest death recap (server or client estimate) */
    public static Snapshot currentDeath;
    /** Death waiting for the respawn chat summary */
    static Snapshot pendingSummary;
    static boolean sawDead;
    static long pendingSince;

    /** Client fallback history (server without the mod) */
    public static final PlayerHistory FALLBACK = new PlayerHistory(50);
    public static final DamageCatalog CATALOG = new DamageCatalog();

    private ClientState() {}

    static Minecraft mc() {
        return Minecraft.getMinecraft();
    }

    public static void reset() {
        serverHasMod = false;
        serverInfo = null;
        helloSent = false;
        currentDeath = null;
        pendingSummary = null;
        FALLBACK.clear();
        ClientFallback.INSTANCE.reset();
    }

    public static DisplayOptions options() {
        WhmConfig.Common c = WhmConfig.common;
        WhmConfig.Client cl = WhmConfig.client;
        PacketServerInfo i = serverHasMod ? serverInfo : null;
        int mask = (i != null ? i.mask : c.mask) & cl.mask;
        DisplayOptions o = new DisplayOptions(mask, true, i != null ? i.summarySeconds : c.summarySeconds);
        if (i != null) {
            o.ignore(i.ignored).renames(WhmConfig.parseMap(i.renames.toArray(new String[0]))).hints(WhmConfig.parseMap(i.hints.toArray(new String[0])));
        } else {
            o.ignore(c.ignoredDamageIds).renames(c.renameMap).hints(c.hintMap);
        }
        return o.ignore(cl.ignoredDamageIds).renames(cl.renameMap).hints(cl.hintMap);
    }

    public static boolean on(Flag f) {
        return options().on(f);
    }

    public static RecapFormatter formatter() {
        return new RecapFormatter(Msg.LOCAL, options());
    }

    static void print(ITextComponent c) {
        if (mc().ingameGUI != null) mc().ingameGUI.getChatGUI().printChatMessage(c);
    }

    static void print(List<ITextComponent> lines) {
        for (ITextComponent c : lines) print(c);
    }

    static void register(Snapshot s) {
        if (s == null) return;
        for (DamageRecord r : s.records) CATALOG.register(r);
    }

    // ------------------------------------------------------------ packets (already on the client thread)

    static void onServerInfo(PacketServerInfo msg) {
        boolean first = !serverHasMod;
        serverHasMod = true;
        serverInfo = msg;
        if (first) {
            FALLBACK.clear();
            ClientFallback.INSTANCE.reset();
        }
    }

    static void onDeath(Snapshot s) {
        register(s);
        currentDeath = s;
        pendingSummary = s;
        sawDead = false;
        pendingSince = System.currentTimeMillis();
        DeathPanel.INSTANCE.onNewData();
    }

    static void onRecap(PacketRecap msg) {
        long now = System.currentTimeMillis();
        Snapshot s = msg.snapshot;
        register(s);
        RecapFormatter f = formatter();
        switch (msg.kind) {
            case PacketRecap.DEATH:
                if (s != null) onDeath(s);
                break;
            case PacketRecap.HISTORY:
                print(f.history(s == null ? new ArrayList<>() : s.records, now, WhmConfig.client.chatHistoryLines, msg.count, false, null));
                break;
            case PacketRecap.LAST:
                if (s == null) print(Msg.LOCAL.tr(TextFormatting.GRAY, "whathitme.last.none"));
                else {
                    currentDeath = s;
                    print(f.fullRecap(s, now, null));
                }
                break;
            case PacketRecap.ALERT:
                if (s == null) break;
                Flag fl = s.kind == Snapshot.HEAVY ? Flag.HEAVY_HIT_SNAPSHOT : Flag.TOTEM_CHAT_ALERT;
                if (f.o.on(fl)) print(f.alert(s, 1));
                break;
            case PacketRecap.SNAPSHOT:
                if (s == null) print(Msg.LOCAL.tr(TextFormatting.GRAY, "whathitme.snapshot.none"));
                else {
                    print(f.fullRecap(s, now, null));
                    print(Msg.LOCAL.tr(TextFormatting.GRAY, "whathitme.snapshot.position", msg.index, msg.count));
                }
                break;
            case PacketRecap.INSPECT_DEATH:
                if (s == null) print(Msg.LOCAL.tr(TextFormatting.GRAY, "whathitme.inspect.nodeath", msg.who));
                else print(new RecapFormatter(Msg.LOCAL, withoutActions(f.o)).fullRecap(s, now, msg.who));
                break;
            case PacketRecap.INSPECT_HISTORY:
                print(f.history(s == null ? new ArrayList<>() : s.records, now, WhmConfig.client.chatHistoryLines, 0, false, msg.who));
                break;
            default:
                break;
        }
    }

    private static DisplayOptions withoutActions(DisplayOptions o) {
        // inspect shows another player's records: no [Copy ID] / [Mute] buttons there
        return o.without(Flag.ACTION_BUTTONS);
    }

    static void onEntries(PacketEntries msg) {
        RecapFormatter f = formatter();
        switch (msg.kind) {
            case PacketEntries.SEARCH:
                print(f.search(msg.query, msg.entries.size() > 30 ? msg.entries.subList(0, 30) : msg.entries, msg.total));
                break;
            case PacketEntries.EFFECTS:
                print(f.effects(msg.entries, false));
                break;
            case PacketEntries.DUMP:
                writeDump(msg.entries);
                break;
            default:
                break;
        }
    }

    static void onMessage(PacketMessage msg) {
        print(Msg.LOCAL.tr(msg.error ? TextFormatting.RED : TextFormatting.GREEN, msg.key, (Object[]) msg.args));
    }

    /** Damage entries (from the server or the local catalog) + local mobs / effects + settings */
    static void writeDump(List<Entry> damage) {
        List<Entry> all = new ArrayList<>(damage);
        for (Entry e : SearchEngine.all(new DamageCatalog())) all.add(e);
        File file = new File(mc().gameDir, "whathitme_dump.csv");
        try {
            int n = DumpWriter.write(file, all, WhmConfig.common, WhmConfig.client);
            print(Msg.LOCAL.tr(TextFormatting.GREEN, "whathitme.dump.done", n, file.getAbsolutePath()));
            File tpl = new File(new File(net.minecraftforge.fml.common.Loader.instance().getConfigDir(), "whathitme"), "hint_template.cfg");
            int ids = DumpWriter.writeTemplate(tpl, all);
            print(Msg.LOCAL.tr(TextFormatting.GREEN, "whathitme.dump.template", ids, tpl.getAbsolutePath()));
        } catch (Exception e) {
            print(Msg.LOCAL.tr(TextFormatting.RED, "whathitme.dump.failed", String.valueOf(e.getMessage())));
        }
    }
}
