package io.github.junjun1z.whathitme.core.cmd;

import io.github.junjun1z.whathitme.core.config.WhmConfig;
import io.github.junjun1z.whathitme.core.text.Fmt;
import io.github.junjun1z.whathitme.core.text.Msg;
import io.github.junjun1z.whathitme.core.text.Txt;

/** Output shared by the client and the server command: help, /whm config, errors. */
public final class CommandText {

    /** Subcommands of the server command */
    public static final String[] SERVER_SUBS = {"history", "last", "effects", "stats", "togglechat", "ignore", "rename", "sethint", "config", "search",
        "inspect", "test", "dump", "reload", "snapshot", "template", "help"};
    /** Subcommands of the client command */
    public static final String[] CLIENT_SUBS = {"history", "last", "effects", "stats", "togglechat", "ignore", "rename", "sethint", "config", "search",
        "inspect", "test", "dump", "reload", "snapshot", "template", "copy", "copyrecap", "server", "help"};
    /** Server subcommands offered after "/whm server " */
    public static final String[] SERVER_FORWARD = {"history", "last", "stats", "ignore", "rename", "sethint", "config", "search", "inspect", "test", "dump",
        "reload", "help"};

    private CommandText() {}

    /** Thrown to end a command with a red error line */
    public static final class Failure extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public final transient Txt text;

        public Failure(Txt text) {
            super(null, null, false, false);
            this.text = text;
        }
    }

    public static Failure err(Msg m, String key, Object... args) {
        return new Failure(m.tr(Fmt.RED, key, args));
    }

    public static Failure usage(Msg m, String usage) {
        return err(m, "whathitme.error.usage", usage);
    }

    public static void help(Sink out, Msg m, boolean client) {
        out.send(m.tr(Fmt.GOLD, "whathitme.help.header"));
        String[] subs = client ? new String[] {"history", "last", "effects", "stats", "togglechat", "ignore", "rename", "sethint", "config", "search", "inspect",
            "test", "dump", "reload", "snapshot", "template", "server"} : SERVER_SUBS;

        for (String s : subs) {
            if (s.equals("help")) continue;
            Txt line = Msg.str(Fmt.YELLOW, "/whm " + s + " ").append(m.tr(Fmt.GRAY, "whathitme.help." + s));
            line.click(Txt.Click.SUGGEST_COMMAND, "/whm " + s + " ");
            out.send(line);
        }
    }

    /** /whm stats */
    public static java.util.List<Txt> stats(Msg m, io.github.junjun1z.whathitme.core.text.RecapFormatter f, io.github.junjun1z.whathitme.core.data.Stats st,
                                            String who) {
        java.util.List<Txt> out = new java.util.ArrayList<>();
        out.add(f.header(m.tr("whathitme.stats.header", who)));
        out.add(m.tr(Fmt.YELLOW, "whathitme.stats.total", st.deaths, f.heartsText((float) st.damageTaken)));
        java.util.List<io.github.junjun1z.whathitme.core.data.Stats.Cause> top = st.top(5);
        if (top.isEmpty()) {
            out.add(m.tr(Fmt.GRAY, "whathitme.stats.none"));
            return out;
        }
        out.add(m.tr(Fmt.GOLD, "whathitme.stats.top"));
        int i = 1;
        for (io.github.junjun1z.whathitme.core.data.Stats.Cause c : top) {
            String ren = f.o.rename(c.id);
            Txt name = ren != null ? f.custom(ren) : c.id.startsWith("mob:") && !c.name.isEmpty() ? m.name(c.nameKey, c.name) : f.typeName(c.name);
            Txt line = m.tr(Fmt.WHITE, "whathitme.stats.line", i++, name, c.deaths);
            line.hover(m.tr("whathitme.hover.id", c.id));
            out.add(line);
        }
        return out;
    }

    public static void config(Sink out, Msg m, WhmConfig.Base cfg, String[] args) {
        if (args.length < 2) {
            out.send(m.tr(Fmt.GOLD, "whathitme.config.keys", String.join(", ", cfg.keys())));
            return;
        }
        String key = args[1];
        if (args.length == 2) {
            String v = cfg.get(key);
            if (v == null) throw err(m, "whathitme.config.unknown", key);
            out.send(m.tr(Fmt.YELLOW, "whathitme.config.value", key, v));
            return;
        }
        String value = Completion.join(args, 2);
        String res = cfg.set(key, value);
        if ("unknown".equals(res)) throw err(m, "whathitme.config.unknown", key);
        if ("locked".equals(res)) throw err(m, "whathitme.config.locked", key);
        if (res != null) throw err(m, "whathitme.config.invalid", key, value);
        out.send(m.tr(Fmt.GREEN, "whathitme.config.set", key, cfg.get(key)));
    }
}
