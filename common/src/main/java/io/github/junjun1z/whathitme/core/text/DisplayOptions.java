package io.github.junjun1z.whathitme.core.text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.junjun1z.whathitme.core.config.WhmConfig.Flag;
import io.github.junjun1z.whathitme.core.data.DamageRecord;
import io.github.junjun1z.whathitme.core.data.MatchKey;

/**
 * What to show and how, for one recipient: effective display flags, ignore list, renames, tips. Renames and tips come in
 * layers: a layer added later has priority (pack folder, then common file, then personal file); inside a layer the most
 * specific matching composite key wins ({@link MatchKey}).
 */
public final class DisplayOptions {

    private static final class Layer {
        final Map<String, String> map;
        final List<MatchKey> keys;

        Layer(Map<String, String> m) {
            map = new LinkedHashMap<>(m);
            keys = MatchKey.parseAll(map.keySet());
        }
    }

    public final int mask;
    private final List<MatchKey> ignore = new ArrayList<>();
    private final List<Layer> rename = new ArrayList<>();
    private final List<Layer> hint = new ArrayList<>();
    /** The recipient has the mod: buttons run client commands (clipboard) */
    public final boolean clientActions;
    public final int summarySeconds;
    /** Templates of the outputs (output name -> template); missing = built-in output */
    private Map<String, Template> templates = new LinkedHashMap<>();
    /** pack deathMessages ("match key" -> template) */
    private Map<String, String> deathMessages = new LinkedHashMap<>();
    /** Name of the reader ({player}) */
    public String player = "";

    public DisplayOptions(int mask, boolean clientActions, int summarySeconds) {
        this.mask = mask;
        this.clientActions = clientActions;
        this.summarySeconds = summarySeconds;
    }

    public DisplayOptions ignore(Collection<String> keys) {
        for (String s : keys) {
            if (s != null && !s.trim().isEmpty()) ignore.add(MatchKey.parse(s));
        }
        return this;
    }

    /** Adds a layer of renames with priority over the layers added before */
    public DisplayOptions renames(Map<String, String> m) {
        if (m != null && !m.isEmpty()) rename.add(0, new Layer(m));
        return this;
    }

    public DisplayOptions hints(Map<String, String> m) {
        if (m != null && !m.isEmpty()) hint.add(0, new Layer(m));
        return this;
    }

    /** A copy of these options with one flag turned off */
    public DisplayOptions without(Flag f) {
        return copy(mask & ~f.bit());
    }

    /** A copy with every display switch on (death triggers) */
    public DisplayOptions withAll() {
        return copy(io.github.junjun1z.whathitme.core.config.WhmConfig.ALL_FLAGS);
    }

    private DisplayOptions copy(int newMask) {
        DisplayOptions o = new DisplayOptions(newMask, clientActions, summarySeconds);
        o.ignore.addAll(ignore);
        o.rename.addAll(rename);
        o.hint.addAll(hint);
        o.templates.putAll(templates);
        o.deathMessages.putAll(deathMessages);
        o.player = player;
        return o;
    }

    /** A copy where one output uses the given template (null = built-in); used by the template editor preview */
    public DisplayOptions withTemplate(String output, Template t) {
        DisplayOptions o = copy(mask);
        if (t == null) o.templates.remove(output);
        else o.templates.put(output, t);
        return o;
    }

    public DisplayOptions templates(Map<String, Template> t) {
        if (t != null) templates.putAll(t);
        return this;
    }

    public DisplayOptions deathMessages(Map<String, String> d) {
        if (d != null) deathMessages.putAll(d);
        return this;
    }

    public DisplayOptions player(String name) {
        player = name == null ? "" : name;
        return this;
    }

    /** The template of an output, null = built-in */
    public Template template(String output) {
        Template t = templates.get(output);
        return t == null || t.lines.isEmpty() ? null : t;
    }

    public Map<String, String> deathMessages() {
        return deathMessages;
    }

    public boolean on(Flag f) {
        return (mask & f.bit()) != 0;
    }

    public boolean ignored(String id) {
        return MatchKey.any(ignore, MatchKey.Target.id(id));
    }

    public boolean ignored(DamageRecord r) {
        return MatchKey.any(ignore, MatchKey.Target.of(r));
    }

    public String rename(String id) {
        return lookup(rename, MatchKey.Target.id(id));
    }

    public String rename(DamageRecord r) {
        return lookup(rename, MatchKey.Target.of(r));
    }

    public String hint(String id) {
        return lookup(hint, MatchKey.Target.id(id));
    }

    public String hint(DamageRecord r) {
        return lookup(hint, MatchKey.Target.of(r));
    }

    public String hint(MatchKey.Target t) {
        return lookup(hint, t);
    }

    public String rename(MatchKey.Target t) {
        return lookup(rename, t);
    }

    private static String lookup(List<Layer> layers, MatchKey.Target t) {
        for (Layer l : layers) {
            String v = MatchKey.best(l.map, l.keys, t);
            if (v != null) return v;
        }
        return null;
    }
}
