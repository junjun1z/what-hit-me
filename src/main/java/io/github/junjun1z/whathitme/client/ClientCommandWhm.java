package io.github.junjun1z.whathitme.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import io.github.junjun1z.whathitme.config.WhmConfig;
import io.github.junjun1z.whathitme.data.Entry;
import io.github.junjun1z.whathitme.data.PlayerHistory;
import io.github.junjun1z.whathitme.data.SearchEngine;
import io.github.junjun1z.whathitme.data.Snapshot;
import io.github.junjun1z.whathitme.net.PacketRequest;
import io.github.junjun1z.whathitme.net.WhmNetwork;
import io.github.junjun1z.whathitme.server.ServerCommandWhm;
import io.github.junjun1z.whathitme.text.Msg;
import io.github.junjun1z.whathitme.text.RecapFormatter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * /whm on a client with the mod. Data that only the server knows is requested with a packet when the server has the
 * mod; otherwise the client fallback ([Client Est.]) is used. Personal settings go to whathitme-client.cfg.
 */
@SideOnly(Side.CLIENT)
public final class ClientCommandWhm extends CommandBase {

    static final String[] SUBS = {"history", "last", "effects", "togglechat", "ignore", "rename", "sethint", "config", "search", "inspect", "dump", "reload",
        "snapshot", "copy", "copyrecap", "server", "help"};

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
        return "whathitme.command.usage";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }

    private static void request(int kind, String arg) {
        WhmNetwork.channel.sendToServer(new PacketRequest(kind, arg));
    }

    private static boolean server() {
        return ClientState.serverHasMod;
    }

    private static CommandException err(String key, Object... args) {
        return new CommandException(key, args);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        Minecraft mc = Minecraft.getMinecraft();
        Msg m = Msg.LOCAL;
        RecapFormatter f = ClientState.formatter();
        String sub = args.length == 0 ? "history" : args[0].toLowerCase(Locale.ROOT);
        String rest = args.length > 2 ? buildString(args, 2) : "";
        long now = System.currentTimeMillis();
        WhmConfig.Client cfg = WhmConfig.client;
        switch (sub) {
            case "history": {
                if (server()) {
                    request(PacketRequest.HISTORY, "");
                } else {
                    PlayerHistory h = ClientState.FALLBACK;
                    ClientState.print(f.history(h.records(), now, cfg.chatHistoryLines, h.snapshots().size(), true, null));
                }
                break;
            }
            case "last": {
                if (server()) {
                    request(PacketRequest.LAST, "");
                } else {
                    Snapshot s = ClientState.FALLBACK.lastDeath();
                    if (s == null) ClientState.print(m.tr(TextFormatting.GRAY, "whathitme.last.none"));
                    else ClientState.print(f.fullRecap(s, now, null));
                }
                break;
            }
            case "snapshot": {
                int n = args.length > 1 ? parse(args[1], 1) : 1;
                if (server()) {
                    request(PacketRequest.SNAPSHOT, String.valueOf(n));
                } else {
                    List<Snapshot> list = ClientState.FALLBACK.snapshots();
                    if (n < 1 || n > list.size()) ClientState.print(m.tr(TextFormatting.GRAY, "whathitme.snapshot.none"));
                    else {
                        ClientState.print(f.fullRecap(list.get(n - 1), now, null));
                        ClientState.print(m.tr(TextFormatting.GRAY, "whathitme.snapshot.position", n, list.size()));
                    }
                }
                break;
            }
            case "effects": {
                if (server()) request(PacketRequest.EFFECTS, "");
                else ClientState.print(f.effects(SearchEngine.effects(mc.player), true));
                break;
            }
            case "togglechat": {
                cfg.set(WhmConfig.Flag.RESPAWN_CHAT_SUMMARY.key, "toggle");
                boolean on = (cfg.mask & WhmConfig.Flag.RESPAWN_CHAT_SUMMARY.bit()) != 0;
                ClientState.print(m.tr(TextFormatting.YELLOW, on ? "whathitme.togglechat.on" : "whathitme.togglechat.off"));
                break;
            }
            case "ignore": {
                if (args.length < 2) throw err("whathitme.error.usage", "/whm ignore <damage_id>");
                boolean ign = cfg.toggleIgnore(args[1]);
                ClientState.print(m.tr(TextFormatting.YELLOW, ign ? "whathitme.ignore.added" : "whathitme.ignore.removed", args[1]));
                break;
            }
            case "rename":
            case "sethint": {
                if (args.length < 2) throw err("whathitme.error.usage", "/whm " + sub + " <damage_id> <text...>");
                boolean rn = sub.equals("rename");
                cfg.putMapEntry(rn ? "renameMap" : "hintMap", args[1], rest);
                if (rest.isEmpty()) ClientState.print(m.tr(TextFormatting.YELLOW, rn ? "whathitme.rename.cleared" : "whathitme.sethint.cleared", args[1]));
                else ClientState.print(m.tr(TextFormatting.YELLOW, rn ? "whathitme.rename.set" : "whathitme.sethint.set", args[1], rest.replace('&', '§')));
                break;
            }
            case "config": {
                ServerCommandWhm.config(sender, m, cfg, args);
                break;
            }
            case "search": {
                if (args.length < 2) throw err("whathitme.error.usage", "/whm search <query | #tag | @modid>");
                String q = buildString(args, 1);
                if (server()) {
                    request(PacketRequest.SEARCH, q);
                } else {
                    List<Entry> all = SearchEngine.search(q, ClientState.CATALOG);
                    ClientState.print(f.search(q, all.size() > 30 ? all.subList(0, 30) : all, all.size()));
                }
                break;
            }
            case "inspect": {
                if (args.length < 2) throw err("whathitme.error.usage", "/whm inspect <player>");
                if (!server()) throw err("whathitme.error.needserver");
                request(PacketRequest.INSPECT, args[1]);
                break;
            }
            case "dump": {
                if (server()) request(PacketRequest.DUMP, "");
                else ClientState.writeDump(ClientState.CATALOG.entries());
                break;
            }
            case "reload": {
                WhmConfig.reloadCommon();
                WhmConfig.reloadClient();
                ClientState.print(m.tr(TextFormatting.GREEN, "whathitme.reload.done"));
                if (server() && (mc.isSingleplayer() || mc.player.canUseCommand(2, "whm"))) request(PacketRequest.RELOAD, "");
                break;
            }
            case "copy": {
                if (args.length < 2) throw err("whathitme.error.usage", "/whm copy <text>");
                String text = buildString(args, 1);
                GuiScreen.setClipboardString(text);
                ClientState.print(m.tr(TextFormatting.GREEN, "whathitme.copy.done", text));
                break;
            }
            case "copyrecap": {
                Snapshot s = ClientState.currentDeath;
                if (s == null) {
                    ClientState.print(m.tr(TextFormatting.GRAY, "whathitme.last.none"));
                    break;
                }
                GuiScreen.setClipboardString(RecapFormatter.plain(f.fullRecap(s, now, null)));
                ClientState.print(m.tr(TextFormatting.GREEN, "whathitme.copyrecap.done"));
                break;
            }
            case "server": {
                if (!server()) throw err("whathitme.error.needserver");
                mc.player.sendChatMessage("/whm " + (args.length > 1 ? buildString(args, 1) : "help"));
                break;
            }
            case "help":
                ServerCommandWhm.help(sender, m, true);
                break;
            default:
                ClientState.print(m.tr(TextFormatting.RED, "whathitme.error.unknown", sub));
                ServerCommandWhm.help(sender, m, true);
        }
    }

    private static int parse(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, SUBS);
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            switch (sub) {
                case "ignore":
                case "rename":
                case "sethint":
                case "copy": {
                    List<String> ids = new ArrayList<>(ClientState.CATALOG.ids());
                    ids.addAll(WhmConfig.client.ignoredDamageIds);
                    ids.addAll(WhmConfig.client.renameMap.keySet());
                    return getListOfStringsMatchingLastWord(args, ids);
                }
                case "config":
                    return getListOfStringsMatchingLastWord(args, WhmConfig.client.properties().keySet());
                case "inspect": {
                    List<String> names = new ArrayList<>();
                    if (Minecraft.getMinecraft().getConnection() != null) {
                        for (NetworkPlayerInfo i : Minecraft.getMinecraft().getConnection().getPlayerInfoMap()) names.add(i.getGameProfile().getName());
                    }
                    return getListOfStringsMatchingLastWord(args, names);
                }
                case "snapshot":
                    return getListOfStringsMatchingLastWord(args, "1", "2", "3", "4", "5");
                case "server":
                    return getListOfStringsMatchingLastWord(args, "history", "last", "ignore", "rename", "sethint", "config", "search", "inspect", "dump", "reload", "help");
                default:
                    break;
            }
        }
        if (sub.equals("search")) return getListOfStringsMatchingLastWord(args, ServerCommandWhm.searchSuggestions(ClientState.CATALOG.ids()));
        if (sub.equals("config") && args.length == 3) return getListOfStringsMatchingLastWord(args, ServerCommandWhm.configValues(WhmConfig.client, args[1]));
        return Collections.emptyList();
    }
}
