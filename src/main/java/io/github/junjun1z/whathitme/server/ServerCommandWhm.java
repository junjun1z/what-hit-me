package io.github.junjun1z.whathitme.server;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.github.junjun1z.whathitme.config.WhmConfig;
import io.github.junjun1z.whathitme.data.DamageTracker;
import io.github.junjun1z.whathitme.data.DumpWriter;
import io.github.junjun1z.whathitme.data.Entry;
import io.github.junjun1z.whathitme.data.PlayerHistory;
import io.github.junjun1z.whathitme.data.SearchEngine;
import io.github.junjun1z.whathitme.data.Snapshot;
import io.github.junjun1z.whathitme.text.Msg;
import io.github.junjun1z.whathitme.text.RecapFormatter;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

/**
 * /whm on the server. Players with the mod use the client command instead (it asks the server for data); this one
 * serves players without the mod (English text), the console, command blocks and "/whm server ..." from modded clients.
 */
public final class ServerCommandWhm extends CommandBase {

    static final String[] SUBS = {"history", "last", "effects", "togglechat", "ignore", "rename", "sethint", "config", "search", "inspect", "dump",
        "reload", "snapshot", "help"};

    @Override
    public String getName() {
        return "whm";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("whathitme");
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/whm [history|last|effects|togglechat|ignore|rename|sethint|config|search|inspect|dump|reload|snapshot|help]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }

    private static boolean modded(ICommandSender s) {
        return s instanceof EntityPlayerMP && DamageTracker.SERVER.isModded(((EntityPlayerMP) s).getUniqueID());
    }

    private static boolean admin(ICommandSender s) {
        if (s instanceof EntityPlayerMP) return ServerNet.isAdmin((EntityPlayerMP) s);
        return s.canUseCommand(2, "whm");
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        boolean loc = modded(sender);
        Msg m = Msg.of(loc);
        EntityPlayerMP player = sender instanceof EntityPlayerMP ? (EntityPlayerMP) sender : null;
        RecapFormatter f = new RecapFormatter(m, ServerNet.options(player, loc));
        String sub = args.length == 0 ? "history" : args[0].toLowerCase(java.util.Locale.ROOT);
        String rest = args.length > 2 ? buildString(args, 2) : "";
        long now = System.currentTimeMillis();
        WhmConfig.Common c = WhmConfig.common;
        switch (sub) {
            case "history": {
                if (player == null) throw err(m, "whathitme.error.player");
                PlayerHistory h = DamageTracker.SERVER.get(player.getUniqueID());
                send(sender, f.history(h.records(), now, WhmConfig.client != null ? WhmConfig.client.chatHistoryLines : 15, h.snapshots().size(), false, null));
                break;
            }
            case "last": {
                if (player == null) throw err(m, "whathitme.error.player");
                Snapshot s = DamageTracker.SERVER.get(player.getUniqueID()).lastDeath();
                if (s == null) sender.sendMessage(m.tr(TextFormatting.GRAY, "whathitme.last.none"));
                else send(sender, f.fullRecap(s, now, null));
                break;
            }
            case "snapshot": {
                if (player == null) throw err(m, "whathitme.error.player");
                List<Snapshot> list = DamageTracker.SERVER.get(player.getUniqueID()).snapshots();
                int n = args.length > 1 ? ServerNet.parse(args[1], 1) : 1;
                if (n < 1 || n > list.size()) sender.sendMessage(m.tr(TextFormatting.GRAY, "whathitme.snapshot.none"));
                else {
                    send(sender, f.fullRecap(list.get(n - 1), now, null));
                    sender.sendMessage(m.tr(TextFormatting.GRAY, "whathitme.snapshot.position", n, list.size()));
                }
                break;
            }
            case "effects": {
                if (player == null) throw err(m, "whathitme.error.player");
                send(sender, f.effects(SearchEngine.effects(player), false));
                break;
            }
            case "togglechat": {
                if (player == null) throw err(m, "whathitme.error.player");
                boolean on = ServerNet.toggleChat(player);
                sender.sendMessage(m.tr(TextFormatting.YELLOW, on ? "whathitme.togglechat.on" : "whathitme.togglechat.off"));
                break;
            }
            case "ignore": {
                if (args.length < 2) throw usage(m, "/whm ignore <damage_id>");
                boolean now2;
                if (admin(sender)) {
                    now2 = c.toggleIgnore(args[1]);
                    ServerNet.broadcastInfo();
                } else if (player != null) {
                    now2 = ServerNet.togglePersonalIgnore(player, args[1]);
                } else {
                    throw err(m, "whathitme.error.noperm");
                }
                sender.sendMessage(m.tr(TextFormatting.YELLOW, now2 ? "whathitme.ignore.added" : "whathitme.ignore.removed", args[1]));
                break;
            }
            case "rename":
            case "sethint": {
                if (!admin(sender)) throw err(m, "whathitme.error.noperm");
                if (args.length < 2) throw usage(m, "/whm " + sub + " <damage_id> <text...>");
                boolean rn = sub.equals("rename");
                c.putMapEntry(rn ? "renameMap" : "hintMap", args[1], rest);
                ServerNet.broadcastInfo();
                if (rest.isEmpty()) sender.sendMessage(m.tr(TextFormatting.YELLOW, rn ? "whathitme.rename.cleared" : "whathitme.sethint.cleared", args[1]));
                else sender.sendMessage(m.tr(TextFormatting.YELLOW, rn ? "whathitme.rename.set" : "whathitme.sethint.set", args[1], rest.replace('&', '§')));
                break;
            }
            case "config": {
                if (!admin(sender)) throw err(m, "whathitme.error.noperm");
                config(sender, m, c, args);
                ServerNet.broadcastInfo();
                break;
            }
            case "search": {
                if (args.length < 2) throw usage(m, "/whm search <query | #tag | @modid>");
                String q = buildString(args, 1);
                List<Entry> all = SearchEngine.search(q, DamageTracker.SERVER.catalog);
                send(sender, f.search(q, all.size() > 30 ? all.subList(0, 30) : all, all.size()));
                break;
            }
            case "inspect": {
                if (!admin(sender)) throw err(m, "whathitme.error.noperm");
                if (args.length < 2) throw usage(m, "/whm inspect <player>");
                EntityPlayerMP t = server.getPlayerList().getPlayerByUsername(args[1]);
                if (t == null) throw err(m, "whathitme.inspect.notfound", args[1]);
                PlayerHistory th = DamageTracker.SERVER.get(t.getUniqueID());
                if (th.lastDeath() == null) sender.sendMessage(m.tr(TextFormatting.GRAY, "whathitme.inspect.nodeath", t.getName()));
                else send(sender, f.fullRecap(th.lastDeath(), now, t.getName()));
                send(sender, f.history(th.records(), now, WhmConfig.client != null ? WhmConfig.client.chatHistoryLines : 15, 0, false, t.getName()));
                break;
            }
            case "dump": {
                if (!admin(sender)) throw err(m, "whathitme.error.noperm");
                File file = new File(server.getDataDirectory(), "whathitme_dump.csv");
                try {
                    List<Entry> all = SearchEngine.all(DamageTracker.SERVER.catalog);
                    int n = DumpWriter.write(file, all, c);
                    sender.sendMessage(m.tr(TextFormatting.GREEN, "whathitme.dump.done", n, file.getAbsolutePath()));
                    File tpl = new File(new File(Loader.instance().getConfigDir(), "whathitme"), "hint_template.cfg");
                    int ids = DumpWriter.writeTemplate(tpl, all);
                    sender.sendMessage(m.tr(TextFormatting.GREEN, "whathitme.dump.template", ids, tpl.getAbsolutePath()));
                } catch (Exception e) {
                    throw err(m, "whathitme.dump.failed", String.valueOf(e.getMessage()));
                }
                break;
            }
            case "reload": {
                if (!admin(sender)) throw err(m, "whathitme.error.noperm");
                WhmConfig.reloadCommon();
                ServerNet.broadcastInfo();
                sender.sendMessage(m.tr(TextFormatting.GREEN, "whathitme.reload.server"));
                break;
            }
            case "help":
                help(sender, m, false);
                break;
            default:
                sender.sendMessage(m.tr(TextFormatting.RED, "whathitme.error.unknown", sub));
                help(sender, m, false);
        }
    }

    public static void help(ICommandSender sender, Msg m, boolean client) {
        sender.sendMessage(m.tr(TextFormatting.GOLD, "whathitme.help.header"));
        String[] subs = client ? new String[] {"history", "last", "effects", "togglechat", "ignore", "rename", "sethint", "config", "search", "inspect", "dump", "reload",
            "snapshot", "server"} : SUBS;
        for (String s : subs) {
            if (s.equals("help")) continue;
            ITextComponent line = Msg.str(TextFormatting.YELLOW, "/whm " + s + " ").appendSibling(m.tr(TextFormatting.GRAY, "whathitme.help." + s));
            line.getStyle().setClickEvent(new net.minecraft.util.text.event.ClickEvent(net.minecraft.util.text.event.ClickEvent.Action.SUGGEST_COMMAND, "/whm " + s + " "));
            sender.sendMessage(line);
        }
    }

    public static void config(ICommandSender sender, Msg m, WhmConfig.Base cfg, String[] args) throws CommandException {
        if (args.length < 2) {
            sender.sendMessage(m.tr(TextFormatting.GOLD, "whathitme.config.keys", String.join(", ", cfg.properties().keySet())));
            return;
        }
        String key = args[1];
        if (args.length == 2) {
            String v = cfg.get(key);
            if (v == null) throw err(m, "whathitme.config.unknown", key);
            sender.sendMessage(m.tr(TextFormatting.YELLOW, "whathitme.config.value", key, v));
            return;
        }
        String value = buildString(args, 2);
        String res = cfg.set(key, value);
        if ("unknown".equals(res)) throw err(m, "whathitme.config.unknown", key);
        if (res != null) throw err(m, "whathitme.config.invalid", key, value);
        sender.sendMessage(m.tr(TextFormatting.GREEN, "whathitme.config.set", key, cfg.get(key)));
    }

    static CommandException err(Msg m, String key, Object... a) {
        return new CommandException(m.localized ? key : Msg.englishRaw(key), a);
    }

    static CommandException usage(Msg m, String u) {
        return new CommandException(m.localized ? "whathitme.error.usage" : Msg.englishRaw("whathitme.error.usage"), u);
    }

    static void send(ICommandSender s, List<ITextComponent> lines) {
        for (ITextComponent l : lines) s.sendMessage(l);
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {
        if (modded(sender)) return Collections.emptyList(); // the client command completes itself
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, SUBS);
        String sub = args[0].toLowerCase(java.util.Locale.ROOT);
        if (args.length == 2) {
            switch (sub) {
                case "ignore":
                case "rename":
                case "sethint": {
                    List<String> ids = new ArrayList<>(DamageTracker.SERVER.catalog.ids());
                    ids.addAll(WhmConfig.common.ignoredDamageIds);
                    return getListOfStringsMatchingLastWord(args, ids);
                }
                case "config":
                    return getListOfStringsMatchingLastWord(args, WhmConfig.common.properties().keySet());
                case "inspect":
                    return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
                case "snapshot":
                    return getListOfStringsMatchingLastWord(args, "1", "2", "3", "4", "5");
                default:
                    break;
            }
        }
        if (sub.equals("search")) return getListOfStringsMatchingLastWord(args, searchSuggestions(DamageTracker.SERVER.catalog.ids()));
        if (sub.equals("config") && args.length == 3) return getListOfStringsMatchingLastWord(args, configValues(WhmConfig.common, args[1]));
        return Collections.emptyList();
    }

    public static List<String> searchSuggestions(List<String> ids) {
        List<String> out = new ArrayList<>(Arrays.asList(SearchEngine.TAGS));
        for (ModContainer mc : Loader.instance().getActiveModList()) out.add("@" + mc.getModId().toLowerCase(java.util.Locale.ROOT));
        out.addAll(ids);
        return out;
    }

    public static List<String> configValues(WhmConfig.Base cfg, String key) {
        Map<String, Property> props = cfg.properties();
        Property p = props.get(key);
        if (p == null) return Collections.emptyList();
        if (p.getType() == Property.Type.BOOLEAN) return Arrays.asList("true", "false");
        if (p.getValidValues() != null && p.getValidValues().length > 0) return Arrays.asList(p.getValidValues());
        return Collections.singletonList(p.isList() ? String.join(",", p.getStringList()) : p.getString());
    }
}
