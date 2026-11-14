package io.github.junjun1z.whathitme.core.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.github.junjun1z.whathitme.core.config.WhmConfig.Flag;
import io.github.junjun1z.whathitme.core.data.DamageRecord;
import io.github.junjun1z.whathitme.core.data.Entry;
import io.github.junjun1z.whathitme.core.data.Snapshot;

/** Builds every chat / tooltip text of the mod. Shared by the client (localized) and the server (English for vanilla clients). */
public final class RecapFormatter {

    public final Msg m;
    public final DisplayOptions o;

    public RecapFormatter(Msg m, DisplayOptions o) {
        this.m = m;
        this.o = o;
    }

    // ---------------------------------------------------------------- numbers

    /** Health points as hearts: 5 HP -> "2.5" */
    public static String hearts(float hp) {
        float h = hp / 2f;
        if (Float.isInfinite(h) || Float.isNaN(h) || h >= 100000f) return "∞";
        if (h >= 100f) return String.valueOf(Math.round(h));
        String s = String.format(Locale.ROOT, "%.1f", h);
        return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
    }

    public static String age(long ms) {
        ms = Math.max(0, ms);
        if (ms < 10000) return String.format(Locale.ROOT, "%.1fs", ms / 1000.0);
        long s = ms / 1000;
        if (s < 60) return s + "s";
        if (s < 3600) return String.format(Locale.ROOT, "%dm %02ds", s / 60, s % 60);
        return String.format(Locale.ROOT, "%dh %02dm", s / 3600, (s / 60) % 60);
    }

    public static String oneDecimal(float v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    public Txt heartsText(float hp) {
        return m.tr("whathitme.hearts", hearts(hp));
    }

    // ---------------------------------------------------------------- names

    public Txt typeName(String damageType) {
        String key = "whathitme.damage." + damageType;
        if (m.has(key)) return m.tr(key);
        return Msg.str(pretty(damageType));
    }

    /** "inFire" -> "In Fire", "draconic.chaos" -> "Chaos" */
    public static String pretty(String type) {
        if (type == null || type.isEmpty()) return "?";
        String t = type;
        int cut = Math.max(t.lastIndexOf('.'), t.lastIndexOf(':'));
        if (cut >= 0 && cut < t.length() - 1) t = t.substring(cut + 1);
        StringBuilder sb = new StringBuilder();
        boolean up = true;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c == '_' || c == '-' || c == ' ') {
                sb.append(' ');
                up = true;
                continue;
            }
            if (Character.isUpperCase(c) && i > 0 && !Character.isUpperCase(t.charAt(i - 1)) && sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ') sb.append(' ');
            sb.append(up ? Character.toUpperCase(c) : c);
            up = false;
        }
        return sb.toString();
    }

    public Txt attacker(DamageRecord r) {
        return m.name(r.attackerKey, r.attackerName);
    }

    /** Main name of a damage line: custom name > attacker > damage type */
    public Txt displayName(DamageRecord r) {
        String ren = o.rename(r.damageId);
        if (ren != null) return custom(ren);
        if (o.on(Flag.ATTACKER_NAME) && !r.attackerName.isEmpty()) {
            Txt c = Txt.empty().append(attacker(r));
            if (!r.directName.isEmpty() && !r.directName.equals(r.attackerName)) {
                c.append(" ").append(m.tr(Fmt.GRAY, "whathitme.line.via", r.directName));
            } else if (!r.damageId.startsWith("mob:")) {
                c.append(" ").append(Msg.str(Fmt.GRAY, "(")).append(typeName(r.damageType).restyle(Fmt.GRAY))
                    .append(Msg.str(Fmt.GRAY, ")"));
            }
            return c;
        }
        if (r.damageId.startsWith("mob:") && !r.attackerName.isEmpty() && !o.on(Flag.ATTACKER_NAME)) return typeName(r.damageType);
        return typeName(r.damageType);
    }

    public List<String> propertyKeys(int flags) {
        List<String> out = new ArrayList<>();
        if ((flags & DamageRecord.BYPASS_ARMOR) != 0) out.add("whathitme.prop.bypasses_armor");
        if ((flags & DamageRecord.ABSOLUTE) != 0) out.add("whathitme.prop.absolute");
        if ((flags & DamageRecord.MAGIC) != 0) out.add("whathitme.prop.magic");
        if ((flags & DamageRecord.FIRE) != 0) out.add("whathitme.prop.fire");
        if ((flags & DamageRecord.EXPLOSION) != 0) out.add("whathitme.prop.explosion");
        if ((flags & DamageRecord.PROJECTILE) != 0) out.add("whathitme.prop.projectile");
        if ((flags & DamageRecord.THROUGH_SHIELD) != 0) out.add("whathitme.prop.shield");
        if ((flags & DamageRecord.IGNORES_IFRAMES) != 0) out.add("whathitme.prop.iframes");
        if ((flags & DamageRecord.BYPASS_CREATIVE) != 0) out.add("whathitme.prop.creative");
        return out;
    }

    public Txt properties(int flags) {
        List<String> keys = propertyKeys(flags);
        if (keys.isEmpty()) return m.tr("whathitme.prop.none");
        Txt c = Txt.empty();
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0) c.append(", ");
            c.append(m.tr(keys.get(i)));
        }
        return c;
    }

    /**
     * A value from renameMap / hintMap: literal text (&amp; or section sign color codes) or "lang:translation.key" so packs can
     * ship translations in a resource pack.
     */
    public Txt custom(String value) {
        String v = value.trim();
        if (v.startsWith("lang:")) {
            String key = v.substring(5).trim();
            if (m.localized) return Txt.empty().append(Txt.tr(key)).append(Fmt.RESET);
            Lang server = Msg.ENGLISH_LANG;
            if (server.has(key)) return Msg.str(server.translate(key) + Fmt.RESET);
            return Msg.str(key);
        }
        return Msg.str(v.replace('&', '§') + Fmt.RESET);
    }

    // ---------------------------------------------------------------- tips

    public Txt tip(DamageRecord r) {
        String custom = o.hint(r.damageId);
        if (custom != null) return custom(custom);
        return m.tr("whathitme.hint." + Hints.autoKey(r));
    }

    // ---------------------------------------------------------------- weapon

    public Txt weapon(DamageRecord r) {
        Txt c = Txt.empty().append(m.name(r.weaponKey, r.weaponName));
        String ench = enchantments(r);
        if (!ench.isEmpty()) c.append(Msg.str(Fmt.LIGHT_PURPLE, " [" + ench + "]"));
        return c;
    }

    public String enchantments(DamageRecord r) {
        if (r.weaponEnchants.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String part : r.weaponEnchants.split(";")) {
            String[] p = part.split("\\|", 3);
            if (p.length < 3) continue;
            if (sb.length() > 0) sb.append(", ");
            if (m.localized && m.has(p[0])) {
                sb.append(m.lang().translate(p[0]));
                String lk = "enchantment.level." + p[1];
                sb.append(' ').append(m.has(lk) ? m.lang().translate(lk) : p[1]);
            } else {
                sb.append(p[2]);
            }
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------- hover

    public List<Txt> hoverLines(DamageRecord r) {
        List<Txt> l = new ArrayList<>();
        l.add(displayName(r).restyle(null).bold(true));
        if (r.has(DamageRecord.ESTIMATED)) l.add(m.tr(Fmt.GOLD, "whathitme.hover.estimated"));
        if (o.on(Flag.INTERNAL_DAMAGE_ID)) l.add(m.tr(Fmt.DARK_GRAY, "whathitme.hover.id", r.damageId));
        if (o.on(Flag.DAMAGE_AMOUNT)) l.add(m.tr(Fmt.RED, "whathitme.hover.damage", heartsText(r.taken()), oneDecimal(r.taken())));
        if (o.on(Flag.MITIGATION_DETAILS)) {
            if (r.raw >= 0) {
                l.add(m.tr(Fmt.GRAY, "whathitme.hover.raw", heartsText(r.raw)));
                l.add(m.tr(Fmt.GRAY, "whathitme.hover.mitigated", mitigation(r)));
            }
            if (r.absorbed > 0) l.add(m.tr(Fmt.YELLOW, "whathitme.hover.absorbed", heartsText(r.absorbed)));
            if (r.maxHealth > 0) l.add(m.tr(Fmt.GRAY, "whathitme.hover.health", hearts(r.healthAfter), hearts(r.maxHealth)));
        }
        if (o.on(Flag.HIT_COUNT) && r.hits > 1) l.add(m.tr(Fmt.YELLOW, "whathitme.hover.hits", r.hits));
        if (o.on(Flag.ATTACKER_NAME) && !r.attackerName.isEmpty()) {
            l.add(m.tr(Fmt.WHITE, "whathitme.hover.attacker", attacker(r)));
            if (!r.directName.isEmpty() && !r.directName.equals(r.attackerName)) l.add(m.tr(Fmt.GRAY, "whathitme.hover.direct", r.directName));
        }
        if (o.on(Flag.ATTACKER_WEAPON) && !r.weaponName.isEmpty()) l.add(m.tr(Fmt.AQUA, "whathitme.hover.weapon", weapon(r)));
        if (o.on(Flag.ATTACKER_DISTANCE) && r.distance >= 0) l.add(m.tr(Fmt.GRAY, "whathitme.hover.distance", oneDecimal(r.distance)));
        if (o.on(Flag.ATTACKER_INVISIBILITY) && r.has(DamageRecord.INVISIBLE)) l.add(m.tr(Fmt.LIGHT_PURPLE, "whathitme.badge.invisible"));
        if (o.on(Flag.ENTITY_OWNER) && !r.ownerName.isEmpty()) l.add(m.tr(Fmt.GRAY, "whathitme.hover.owner", r.ownerName));
        if (o.on(Flag.DAMAGE_PROPERTIES)) l.add(m.tr(Fmt.DARK_AQUA, "whathitme.hover.properties", properties(r.flags)));
        if (o.on(Flag.SOURCE_MOD_NAME) && !r.modName.isEmpty()) l.add(m.tr(Fmt.BLUE, "whathitme.hover.mod", r.modName));
        if (r.has(DamageRecord.TOTEM)) l.add(m.tr(Fmt.GOLD, "whathitme.badge.totem"));
        if (r.has(DamageRecord.HEAVY)) l.add(m.tr(Fmt.RED, "whathitme.badge.heavy"));
        if (o.on(Flag.HINTS_AND_TIPS)) l.add(m.tr(Fmt.GREEN, "whathitme.hover.tip", tip(r)));
        return l;
    }

    public Txt hover(DamageRecord r) {
        Txt c = Txt.empty();
        List<Txt> lines = hoverLines(r);
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) c.append("\n");
            c.append(lines.get(i));
        }
        return c;
    }

    public static String mitigation(DamageRecord r) {
        if (r.raw <= 0) return "0";
        float pct = (r.raw - r.actual - r.absorbed) / r.raw * 100f;
        return String.valueOf(Math.round(Math.max(0f, Math.min(100f, pct))));
    }

    // ---------------------------------------------------------------- buttons

    public Txt copyIdButton(String id) {
        Txt b = m.tr(Fmt.AQUA, "whathitme.button.copyid");
        if (o.clientActions) {
            b.click(Txt.Click.RUN_COMMAND, "/whm copy " + id).hover(m.tr("whathitme.button.copyid.hover", id));
        } else {
            b.click(Txt.Click.SUGGEST_COMMAND, id).hover(m.tr("whathitme.button.copyid.suggest", id));
        }
        return b;
    }

    public Txt muteButton(String id) {
        Txt b = m.tr(Fmt.RED, "whathitme.button.mute");
        b.click(Txt.Click.RUN_COMMAND, "/whm ignore " + id).hover(m.tr("whathitme.button.mute.hover", id));
        return b;
    }

    public Txt copyRecapButton() {
        Txt b = m.tr(Fmt.AQUA, "whathitme.button.copyrecap");
        b.click(Txt.Click.RUN_COMMAND, "/whm copyrecap").hover(m.tr("whathitme.button.copyrecap.hover"));
        return b;
    }

    private Txt link(Fmt color, String key, String command, String hoverKey) {
        Txt b = m.tr(color, key);
        b.click(Txt.Click.RUN_COMMAND, command).hover(m.tr(hoverKey, command));
        return b;
    }

    // ---------------------------------------------------------------- lines

    /** One history line */
    public Txt line(DamageRecord r, long now) {
        Txt root = Txt.empty();
        root.hover(hover(r));
        if (r.has(DamageRecord.ESTIMATED)) root.append(m.tr(Fmt.GOLD, "whathitme.badge.estimated")).append(" ");
        if (r.has(DamageRecord.KILLING)) root.append(Msg.str(Fmt.DARK_RED, "☠ "));
        if (o.on(Flag.TIMESTAMP)) root.append(Msg.str(Fmt.DARK_GRAY, age(now - r.time) + " "));
        root.append(displayName(r).restyle(Fmt.WHITE));
        if (o.on(Flag.DAMAGE_AMOUNT)) root.append(" ").append(Msg.str(Fmt.RED, "-")).append(heartsText(r.taken()).restyle(Fmt.RED));
        if (o.on(Flag.MITIGATION_DETAILS)) {
            List<Txt> parts = new ArrayList<>();
            if (r.raw >= 0 && r.raw > r.taken() + 0.01f) {
                parts.add(m.tr("whathitme.line.raw", hearts(r.raw)));
                parts.add(m.tr("whathitme.line.mitigated", mitigation(r)));
            }
            if (r.absorbed > 0) parts.add(m.tr(Fmt.YELLOW, "whathitme.line.absorbed", hearts(r.absorbed)));
            if (!parts.isEmpty()) {
                Txt p = Msg.str(Fmt.GRAY, " (");
                for (int i = 0; i < parts.size(); i++) {
                    if (i > 0) p.append(", ");
                    p.append(parts.get(i));
                }
                p.append(")");
                root.append(p);
            }
        }
        if (o.on(Flag.HIT_COUNT) && r.hits > 1) root.append(" ").append(m.tr(Fmt.YELLOW, "whathitme.line.hits", r.hits));
        if (o.on(Flag.DAMAGE_PROPERTIES) && (r.flags & DamageRecord.PROPERTY_MASK) != 0) {
            root.append(Msg.str(Fmt.DARK_AQUA, " [")).append(properties(r.flags).restyle(Fmt.DARK_AQUA))
                .append(Msg.str(Fmt.DARK_AQUA, "]"));
        }
        if (o.on(Flag.SOURCE_MOD_NAME) && !r.modName.isEmpty() && !"minecraft".equals(r.modId)) root.append(Msg.str(Fmt.BLUE, " (" + r.modName + ")"));
        if (o.on(Flag.ATTACKER_DISTANCE) && r.distance >= 0) root.append(Msg.str(Fmt.GRAY, " " + oneDecimal(r.distance) + "m"));
        if (o.on(Flag.ATTACKER_INVISIBILITY) && r.has(DamageRecord.INVISIBLE)) root.append(" ").append(m.tr(Fmt.LIGHT_PURPLE, "whathitme.badge.invisible"));
        if (o.on(Flag.ENTITY_OWNER) && !r.ownerName.isEmpty()) root.append(" ").append(m.tr(Fmt.GRAY, "whathitme.line.owner", r.ownerName));
        if (r.has(DamageRecord.TOTEM)) root.append(" ").append(m.tr(Fmt.GOLD, "whathitme.badge.totem"));
        if (r.has(DamageRecord.HEAVY)) root.append(" ").append(m.tr(Fmt.RED, "whathitme.badge.heavy"));
        if (o.on(Flag.ACTION_BUTTONS)) root.append(" ").append(copyIdButton(r.damageId)).append(" ").append(muteButton(r.damageId));
        return root;
    }

    /** Records to show (ignored ids removed), newest first, at most max */
    public List<DamageRecord> visible(List<DamageRecord> oldestFirst, int max) {
        List<DamageRecord> out = new ArrayList<>();
        for (int i = oldestFirst.size() - 1; i >= 0 && out.size() < max; i--) {
            DamageRecord r = oldestFirst.get(i);
            if (r.has(DamageRecord.KILLING) || !o.ignored(r.damageId)) out.add(r);
        }
        return out;
    }

    public Txt header(Txt title) {
        return Txt.empty().append(Msg.str(Fmt.DARK_GRAY, "========== ")).append(title.restyle(Fmt.GOLD))
            .append(Msg.str(Fmt.DARK_GRAY, " =========="));
    }

    public List<Txt> history(List<DamageRecord> records, long now, int max, int snapshotCount, boolean estimated, String who) {
        List<Txt> out = new ArrayList<>();
        List<DamageRecord> vis = visible(records, max);
        out.add(header(who == null ? m.tr("whathitme.history.header", records.size()) : m.tr("whathitme.inspect.history", who, records.size())));
        if (estimated) out.add(m.tr(Fmt.GOLD, "whathitme.history.estimated"));
        if (vis.isEmpty()) out.add(m.tr(Fmt.GRAY, "whathitme.history.empty"));
        for (DamageRecord r : vis) out.add(line(r, now));
        if (records.size() > 0) out.add(total(records, now));
        if (snapshotCount > 0 && who == null) {
            out.add(Txt.empty().append(m.tr(Fmt.GRAY, "whathitme.history.snapshots", snapshotCount)).append(" ")
                .append(link(Fmt.AQUA, "whathitme.button.view", "/whm snapshot 1", "whathitme.button.run")));
        }
        return out;
    }

    /** "Last 10s Total" relative to the newest record */
    public Txt total(List<DamageRecord> records, long end) {
        long window = o.summarySeconds * 1000L;
        float sum = 0;
        int hits = 0;
        for (DamageRecord r : records) {
            if (end - r.time <= window && end >= r.time - 50) {
                sum += r.taken();
                hits += Math.max(1, r.hits);
            }
        }
        return m.tr(Fmt.GOLD, "whathitme.summary.total", o.summarySeconds, heartsText(sum), hits);
    }

    private Txt killingLine(Snapshot s, String key) {
        DamageRecord k = s.key();
        Txt c = Txt.empty();
        if (k == null) {
            Txt dm = deathMessage(s);
            c.append(m.tr(Fmt.RED, key, dm == null ? m.tr("whathitme.unknown") : dm));
            return c;
        }
        Txt name = Txt.empty().append(displayName(k).restyle(Fmt.WHITE));
        if (o.on(Flag.DAMAGE_AMOUNT)) name.append(Msg.str(Fmt.RED, " [")).append(heartsText(k.taken()).restyle(Fmt.RED))
            .append(Msg.str(Fmt.RED, "]"));
        if (o.on(Flag.SOURCE_MOD_NAME) && !k.modName.isEmpty()) name.append(Msg.str(Fmt.BLUE, " (" + k.modName + ")"));
        if (o.on(Flag.ATTACKER_INVISIBILITY) && k.has(DamageRecord.INVISIBLE)) name.append(" ").append(m.tr(Fmt.LIGHT_PURPLE, "whathitme.badge.invisible"));
        if (o.on(Flag.ENTITY_OWNER) && !k.ownerName.isEmpty()) name.append(" ").append(m.tr(Fmt.GRAY, "whathitme.line.owner", k.ownerName));
        Txt line = m.tr(Fmt.RED, key, name);
        line.hover(hover(k));
        c.append(line);
        if (k.has(DamageRecord.ESTIMATED)) c.append(" ").append(m.tr(Fmt.GOLD, "whathitme.badge.estimated"));
        if (o.on(Flag.ACTION_BUTTONS)) c.append(" ").append(copyIdButton(k.damageId)).append(" ").append(muteButton(k.damageId));
        return c;
    }

    public Txt deathMessage(Snapshot s) {
        if (s.deathMessage.isEmpty()) return null;
        return Txt.json(s.deathMessage);
    }

    /** Chat summary after respawning */
    public List<Txt> respawnSummary(Snapshot s) {
        List<Txt> out = new ArrayList<>();
        out.add(m.tr(Fmt.GOLD, "whathitme.summary.header"));
        out.add(killingLine(s, "whathitme.summary.killing"));
        DamageRecord k = s.key();
        if (k != null && o.on(Flag.DAMAGE_PROPERTIES)) out.add(m.tr(Fmt.DARK_AQUA, "whathitme.summary.properties", properties(k.flags)));
        out.add(total(s.records, s.time));
        if (k != null && o.on(Flag.HINTS_AND_TIPS)) out.add(m.tr(Fmt.GREEN, "whathitme.summary.tip", tip(k)));
        Txt recap = m.tr(Fmt.AQUA, "whathitme.summary.recap.link");
        recap.underline(true).click(Txt.Click.RUN_COMMAND, "/whm last").hover(m.tr("whathitme.button.run", "/whm last"));
        out.add(m.tr(Fmt.AQUA, "whathitme.summary.recap", recap, "/whm last"));
        if (o.on(Flag.TOGGLE_CHAT_NOTICE)) {
            Txt cmd = Msg.str(Fmt.YELLOW, "/whm togglechat");
            cmd.click(Txt.Click.SUGGEST_COMMAND, "/whm togglechat");
            out.add(m.tr(Fmt.GRAY, "whathitme.summary.toggle", cmd));
        }
        out.add(m.tr(Fmt.GOLD, "whathitme.summary.footer"));
        return out;
    }

    public Txt kindName(int kind) {
        switch (kind) {
            case Snapshot.TOTEM:
                return m.tr("whathitme.snapshot.kind.totem");
            case Snapshot.HEAVY:
                return m.tr("whathitme.snapshot.kind.heavy");
            case Snapshot.PREVENTED:
                return m.tr("whathitme.snapshot.kind.prevented");
            default:
                return m.tr("whathitme.snapshot.kind.death");
        }
    }

    /** Full recap of a death or snapshot (/whm last, /whm snapshot, /whm inspect) */
    public List<Txt> fullRecap(Snapshot s, long now, String who) {
        List<Txt> out = new ArrayList<>();
        Txt title;
        if (who != null) title = m.tr("whathitme.inspect.death", who, age(now - s.time));
        else if (s.kind == Snapshot.DEATH) title = m.tr("whathitme.last.header", age(now - s.time));
        else title = m.tr("whathitme.snapshot.header", kindName(s.kind), age(now - s.time));
        out.add(header(title));
        Txt dm = deathMessage(s);
        if (dm != null) out.add(Txt.empty().append(Msg.str(Fmt.GRAY, "“")).append(dm).append(Msg.str(Fmt.GRAY, "”")));
        out.add(killingLine(s, s.kind == Snapshot.DEATH ? "whathitme.summary.killing" : "whathitme.snapshot.trigger"));
        DamageRecord k = s.key();
        if (k != null && o.on(Flag.DAMAGE_PROPERTIES)) out.add(m.tr(Fmt.DARK_AQUA, "whathitme.summary.properties", properties(k.flags)));
        List<DamageRecord> vis = visible(s.records, 40);
        for (DamageRecord r : vis) out.add(line(r, s.time).append(Msg.str(Fmt.DARK_GRAY, "")));
        if (vis.isEmpty()) out.add(m.tr(Fmt.GRAY, "whathitme.history.empty"));
        out.add(total(s.records, s.time));
        if (k != null && o.on(Flag.HINTS_AND_TIPS)) out.add(m.tr(Fmt.GREEN, "whathitme.summary.tip", tip(k)));
        if (o.on(Flag.ACTION_BUTTONS) && o.clientActions && who == null && s.kind == Snapshot.DEATH) out.add(copyRecapButton());
        return out;
    }

    /** Chat alert after a totem pop / heavy hit */
    public Txt alert(Snapshot s, int index) {
        DamageRecord k = s.key();
        String key = s.kind == Snapshot.TOTEM ? "whathitme.alert.totem" : s.kind == Snapshot.HEAVY ? "whathitme.alert.heavy" : "whathitme.alert.prevented";
        Txt name = k == null ? m.tr("whathitme.unknown") : displayName(k).restyle(Fmt.WHITE);
        Txt amount = k == null ? Msg.str("?") : heartsText(k.taken()).restyle(Fmt.RED);
        Txt c = Txt.empty().append(m.tr(Fmt.GOLD, key, name, amount));
        if (k != null) c.hover(hover(k));
        c.append(" ").append(link(Fmt.AQUA, "whathitme.button.details", "/whm snapshot " + index, "whathitme.button.run"));
        return c;
    }

    // ---------------------------------------------------------------- effects / search

    public Txt entryName(Entry e) {
        if (e.type == Entry.DAMAGE) {
            String ren = o.rename(e.id);
            if (ren != null) return custom(ren);
            if (e.id.startsWith("mob:")) return m.name(e.nameKey, e.name);
            return typeName(e.name);
        }
        return m.name(e.nameKey, e.name);
    }

    public static String duration(int ticks) {
        if (ticks >= 32767 * 20 || ticks < 0) return "**:**";
        int s = ticks / 20;
        return String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60);
    }

    public List<Txt> effects(List<Entry> list, boolean estimated) {
        List<Txt> out = new ArrayList<>();
        out.add(header(m.tr("whathitme.effects.header", list.size())));
        if (list.isEmpty()) out.add(m.tr(Fmt.GRAY, "whathitme.effects.none"));
        for (Entry e : list) {
            boolean bad = (e.flags & 1) != 0;
            Txt c = Txt.empty();
            Txt name = entryName(e).restyle(bad ? Fmt.RED : Fmt.GREEN);
            c.append(Msg.str(bad ? Fmt.RED : Fmt.GREEN, bad ? "✖ " : "✔ ")).append(name);
            if (e.amplifier > 0) {
                String lk = "enchantment.level." + (e.amplifier + 1);
                c.append(" ").append(m.localized && m.has(lk) ? m.tr(lk) : Msg.str(String.valueOf(e.amplifier + 1)));
            }
            c.append(Msg.str(Fmt.GRAY, " " + duration(e.duration)));
            if (o.on(Flag.SOURCE_MOD_NAME) && !e.modName.isEmpty()) c.append(Msg.str(Fmt.BLUE, " (" + e.modName + ")"));
            c.append(" ").append(e.milk ? m.tr(Fmt.AQUA, "whathitme.effects.milk.yes") : m.tr(Fmt.DARK_GRAY, "whathitme.effects.milk.no"));
            Txt hov = Txt.empty().append(entryName(e).restyle(null).bold(true));
            hov.append("\n").append(m.tr(Fmt.DARK_GRAY, "whathitme.hover.id", e.id));
            hov.append("\n").append(m.tr(bad ? Fmt.RED : Fmt.GREEN, bad ? "whathitme.effects.bad" : "whathitme.effects.good"));
            hov.append("\n").append(m.tr(Fmt.GRAY, "whathitme.effects.curative", e.extra.isEmpty() ? m.text("whathitme.effects.curative.none") : e.extra));
            if (!e.modName.isEmpty()) hov.append("\n").append(m.tr(Fmt.BLUE, "whathitme.hover.mod", e.modName));
            c.hover(hov);
            out.add(c);
        }
        if (estimated) out.add(m.tr(Fmt.GOLD, "whathitme.effects.estimated"));
        return out;
    }

    public List<Txt> search(String query, List<Entry> list, int total) {
        List<Txt> out = new ArrayList<>();
        out.add(header(m.tr("whathitme.search.header", query, total)));
        if (list.isEmpty()) out.add(m.tr(Fmt.GRAY, "whathitme.search.none"));
        for (Entry e : list) {
            Txt c = Txt.empty();
            String typeKey = e.type == Entry.DAMAGE ? "whathitme.search.type.damage" : e.type == Entry.MOB ? "whathitme.search.type.mob" : "whathitme.search.type.effect";
            Fmt col = e.type == Entry.DAMAGE ? Fmt.RED : e.type == Entry.MOB ? Fmt.YELLOW : Fmt.LIGHT_PURPLE;
            c.append(Msg.str(col, "[")).append(m.tr(col, typeKey)).append(Msg.str(col, "] "));
            c.append(entryName(e).restyle(Fmt.WHITE));
            c.append(Msg.str(Fmt.DARK_GRAY, " " + e.id));
            if (e.type == Entry.DAMAGE && (e.flags & DamageRecord.PROPERTY_MASK) != 0 && o.on(Flag.DAMAGE_PROPERTIES)) {
                c.append(Msg.str(Fmt.DARK_AQUA, " [")).append(properties(e.flags).restyle(Fmt.DARK_AQUA))
                    .append(Msg.str(Fmt.DARK_AQUA, "]"));
            }
            if (e.type == Entry.EFFECT) c.append(" ").append((e.flags & 1) != 0 ? m.tr(Fmt.RED, "whathitme.effects.bad") : m.tr(Fmt.GREEN, "whathitme.effects.good"));
            if (!e.modName.isEmpty() && o.on(Flag.SOURCE_MOD_NAME)) c.append(Msg.str(Fmt.BLUE, " (" + e.modName + ")"));
            if (e.type == Entry.DAMAGE && e.count > 0) c.append(" ").append(m.tr(Fmt.GRAY, "whathitme.search.seen", e.count));
            if (e.type == Entry.DAMAGE && o.on(Flag.ACTION_BUTTONS)) {
                if (o.ignored(e.id)) c.append(" ").append(m.tr(Fmt.DARK_GRAY, "whathitme.search.muted"));
                c.append(" ").append(copyIdButton(e.id)).append(" ").append(muteButton(e.id));
            } else {
                c.click(Txt.Click.SUGGEST_COMMAND, e.id);
            }
            c.hover(m.tr("whathitme.hover.id", e.id));
            out.add(c);
        }
        if (total > list.size()) out.add(m.tr(Fmt.GRAY, "whathitme.search.more", total - list.size()));
        return out;
    }

    /** Plain text (no formatting codes), e.g. for the clipboard */
    public static String plain(List<Txt> lines, Lang lang) {
        StringBuilder sb = new StringBuilder();
        for (Txt c : lines) sb.append(Fmt.strip(c.toPlain(lang))).append((char) 10);
        return sb.toString();
    }
}
