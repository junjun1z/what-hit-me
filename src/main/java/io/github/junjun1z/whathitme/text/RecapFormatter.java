package io.github.junjun1z.whathitme.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.github.junjun1z.whathitme.config.WhmConfig.Flag;
import io.github.junjun1z.whathitme.data.DamageRecord;
import io.github.junjun1z.whathitme.data.Entry;
import io.github.junjun1z.whathitme.data.Snapshot;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

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

    public ITextComponent heartsText(float hp) {
        return m.tr("whathitme.hearts", hearts(hp));
    }

    // ---------------------------------------------------------------- names

    public ITextComponent typeName(String damageType) {
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

    public ITextComponent attacker(DamageRecord r) {
        return m.name(r.attackerKey, r.attackerName);
    }

    /** Main name of a damage line: custom name > attacker > damage type */
    public ITextComponent displayName(DamageRecord r) {
        String ren = o.rename(r.damageId);
        if (ren != null) return custom(ren);
        if (o.on(Flag.ATTACKER_NAME) && !r.attackerName.isEmpty()) {
            ITextComponent c = new TextComponentString("").appendSibling(attacker(r));
            if (!r.directName.isEmpty() && !r.directName.equals(r.attackerName)) {
                c.appendText(" ").appendSibling(m.tr(TextFormatting.GRAY, "whathitme.line.via", r.directName));
            } else if (!r.damageId.startsWith("mob:")) {
                c.appendText(" ").appendSibling(Msg.str(TextFormatting.GRAY, "(")).appendSibling(typeName(r.damageType).setStyle(new Style().setColor(TextFormatting.GRAY)))
                    .appendSibling(Msg.str(TextFormatting.GRAY, ")"));
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

    public ITextComponent properties(int flags) {
        List<String> keys = propertyKeys(flags);
        if (keys.isEmpty()) return m.tr("whathitme.prop.none");
        ITextComponent c = new TextComponentString("");
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0) c.appendText(", ");
            c.appendSibling(m.tr(keys.get(i)));
        }
        return c;
    }

    /**
     * A value from renameMap / hintMap: literal text (&amp; or section sign color codes) or "lang:translation.key" so packs can
     * ship translations in a resource pack.
     */
    @SuppressWarnings("deprecation")
    public ITextComponent custom(String value) {
        String v = value.trim();
        if (v.startsWith("lang:")) {
            String key = v.substring(5).trim();
            if (m.localized) return new TextComponentString("").appendSibling(new net.minecraft.util.text.TextComponentTranslation(key)).appendText(TextFormatting.RESET.toString());
            if (net.minecraft.util.text.translation.I18n.canTranslate(key)) return Msg.str(net.minecraft.util.text.translation.I18n.translateToLocal(key) + TextFormatting.RESET);
            return Msg.str(key);
        }
        return Msg.str(v.replace('&', '§') + TextFormatting.RESET);
    }

    // ---------------------------------------------------------------- tips

    public ITextComponent tip(DamageRecord r) {
        String custom = o.hint(r.damageId);
        if (custom != null) return custom(custom);
        return m.tr("whathitme.hint." + Hints.autoKey(r));
    }

    // ---------------------------------------------------------------- weapon

    public ITextComponent weapon(DamageRecord r) {
        ITextComponent c = new TextComponentString("").appendSibling(m.name(r.weaponKey, r.weaponName));
        String ench = enchantments(r);
        if (!ench.isEmpty()) c.appendSibling(Msg.str(TextFormatting.LIGHT_PURPLE, " [" + ench + "]"));
        return c;
    }

    @SuppressWarnings("deprecation")
    public String enchantments(DamageRecord r) {
        if (r.weaponEnchants.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String part : r.weaponEnchants.split(";")) {
            String[] p = part.split("\\|", 3);
            if (p.length < 3) continue;
            if (sb.length() > 0) sb.append(", ");
            if (m.localized && m.has(p[0])) {
                sb.append(net.minecraft.util.text.translation.I18n.translateToLocal(p[0]));
                String lk = "enchantment.level." + p[1];
                sb.append(' ').append(m.has(lk) ? net.minecraft.util.text.translation.I18n.translateToLocal(lk) : p[1]);
            } else {
                sb.append(p[2]);
            }
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------- hover

    public List<ITextComponent> hoverLines(DamageRecord r) {
        List<ITextComponent> l = new ArrayList<>();
        l.add(displayName(r).setStyle(new Style().setBold(true)));
        if (r.has(DamageRecord.ESTIMATED)) l.add(m.tr(TextFormatting.GOLD, "whathitme.hover.estimated"));
        if (o.on(Flag.INTERNAL_DAMAGE_ID)) l.add(m.tr(TextFormatting.DARK_GRAY, "whathitme.hover.id", r.damageId));
        if (o.on(Flag.DAMAGE_AMOUNT)) l.add(m.tr(TextFormatting.RED, "whathitme.hover.damage", heartsText(r.taken()), oneDecimal(r.taken())));
        if (o.on(Flag.MITIGATION_DETAILS)) {
            if (r.raw >= 0) {
                l.add(m.tr(TextFormatting.GRAY, "whathitme.hover.raw", heartsText(r.raw)));
                l.add(m.tr(TextFormatting.GRAY, "whathitme.hover.mitigated", mitigation(r)));
            }
            if (r.absorbed > 0) l.add(m.tr(TextFormatting.YELLOW, "whathitme.hover.absorbed", heartsText(r.absorbed)));
            if (r.maxHealth > 0) l.add(m.tr(TextFormatting.GRAY, "whathitme.hover.health", hearts(r.healthAfter), hearts(r.maxHealth)));
        }
        if (o.on(Flag.HIT_COUNT) && r.hits > 1) l.add(m.tr(TextFormatting.YELLOW, "whathitme.hover.hits", r.hits));
        if (o.on(Flag.ATTACKER_NAME) && !r.attackerName.isEmpty()) {
            l.add(m.tr(TextFormatting.WHITE, "whathitme.hover.attacker", attacker(r)));
            if (!r.directName.isEmpty() && !r.directName.equals(r.attackerName)) l.add(m.tr(TextFormatting.GRAY, "whathitme.hover.direct", r.directName));
        }
        if (o.on(Flag.ATTACKER_WEAPON) && !r.weaponName.isEmpty()) l.add(m.tr(TextFormatting.AQUA, "whathitme.hover.weapon", weapon(r)));
        if (o.on(Flag.ATTACKER_DISTANCE) && r.distance >= 0) l.add(m.tr(TextFormatting.GRAY, "whathitme.hover.distance", oneDecimal(r.distance)));
        if (o.on(Flag.ATTACKER_INVISIBILITY) && r.has(DamageRecord.INVISIBLE)) l.add(m.tr(TextFormatting.LIGHT_PURPLE, "whathitme.badge.invisible"));
        if (o.on(Flag.ENTITY_OWNER) && !r.ownerName.isEmpty()) l.add(m.tr(TextFormatting.GRAY, "whathitme.hover.owner", r.ownerName));
        if (o.on(Flag.DAMAGE_PROPERTIES)) l.add(m.tr(TextFormatting.DARK_AQUA, "whathitme.hover.properties", properties(r.flags)));
        if (o.on(Flag.SOURCE_MOD_NAME) && !r.modName.isEmpty()) l.add(m.tr(TextFormatting.BLUE, "whathitme.hover.mod", r.modName));
        if (r.has(DamageRecord.TOTEM)) l.add(m.tr(TextFormatting.GOLD, "whathitme.badge.totem"));
        if (r.has(DamageRecord.HEAVY)) l.add(m.tr(TextFormatting.RED, "whathitme.badge.heavy"));
        if (o.on(Flag.HINTS_AND_TIPS)) l.add(m.tr(TextFormatting.GREEN, "whathitme.hover.tip", tip(r)));
        return l;
    }

    public ITextComponent hover(DamageRecord r) {
        ITextComponent c = new TextComponentString("");
        List<ITextComponent> lines = hoverLines(r);
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) c.appendText("\n");
            c.appendSibling(lines.get(i));
        }
        return c;
    }

    public static String mitigation(DamageRecord r) {
        if (r.raw <= 0) return "0";
        float pct = (r.raw - r.actual - r.absorbed) / r.raw * 100f;
        return String.valueOf(Math.round(Math.max(0f, Math.min(100f, pct))));
    }

    // ---------------------------------------------------------------- buttons

    public ITextComponent copyIdButton(String id) {
        ITextComponent b = m.tr(TextFormatting.AQUA, "whathitme.button.copyid");
        Style s = b.getStyle();
        if (o.clientActions) {
            s.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/whm copy " + id));
            s.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, m.tr("whathitme.button.copyid.hover", id)));
        } else {
            s.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, id));
            s.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, m.tr("whathitme.button.copyid.suggest", id)));
        }
        return b;
    }

    public ITextComponent muteButton(String id) {
        ITextComponent b = m.tr(TextFormatting.RED, "whathitme.button.mute");
        b.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/whm ignore " + id))
            .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, m.tr("whathitme.button.mute.hover", id)));
        return b;
    }

    public ITextComponent copyRecapButton() {
        ITextComponent b = m.tr(TextFormatting.AQUA, "whathitme.button.copyrecap");
        b.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/whm copyrecap"))
            .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, m.tr("whathitme.button.copyrecap.hover")));
        return b;
    }

    private ITextComponent link(TextFormatting color, String key, String command, String hoverKey) {
        ITextComponent b = m.tr(color, key);
        b.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
            .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, m.tr(hoverKey, command)));
        return b;
    }

    // ---------------------------------------------------------------- lines

    /** One history line */
    public ITextComponent line(DamageRecord r, long now) {
        ITextComponent root = new TextComponentString("");
        root.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover(r)));
        if (r.has(DamageRecord.ESTIMATED)) root.appendSibling(m.tr(TextFormatting.GOLD, "whathitme.badge.estimated")).appendText(" ");
        if (r.has(DamageRecord.KILLING)) root.appendSibling(Msg.str(TextFormatting.DARK_RED, "☠ "));
        if (o.on(Flag.TIMESTAMP)) root.appendSibling(Msg.str(TextFormatting.DARK_GRAY, age(now - r.time) + " "));
        root.appendSibling(displayName(r).setStyle(new Style().setColor(TextFormatting.WHITE)));
        if (o.on(Flag.DAMAGE_AMOUNT)) root.appendText(" ").appendSibling(Msg.str(TextFormatting.RED, "-")).appendSibling(heartsText(r.taken()).setStyle(new Style().setColor(TextFormatting.RED)));
        if (o.on(Flag.MITIGATION_DETAILS)) {
            List<ITextComponent> parts = new ArrayList<>();
            if (r.raw >= 0 && r.raw > r.taken() + 0.01f) {
                parts.add(m.tr("whathitme.line.raw", hearts(r.raw)));
                parts.add(m.tr("whathitme.line.mitigated", mitigation(r)));
            }
            if (r.absorbed > 0) parts.add(m.tr(TextFormatting.YELLOW, "whathitme.line.absorbed", hearts(r.absorbed)));
            if (!parts.isEmpty()) {
                ITextComponent p = Msg.str(TextFormatting.GRAY, " (");
                for (int i = 0; i < parts.size(); i++) {
                    if (i > 0) p.appendText(", ");
                    p.appendSibling(parts.get(i));
                }
                p.appendText(")");
                root.appendSibling(p);
            }
        }
        if (o.on(Flag.HIT_COUNT) && r.hits > 1) root.appendText(" ").appendSibling(m.tr(TextFormatting.YELLOW, "whathitme.line.hits", r.hits));
        if (o.on(Flag.DAMAGE_PROPERTIES) && (r.flags & DamageRecord.PROPERTY_MASK) != 0) {
            root.appendSibling(Msg.str(TextFormatting.DARK_AQUA, " [")).appendSibling(properties(r.flags).setStyle(new Style().setColor(TextFormatting.DARK_AQUA)))
                .appendSibling(Msg.str(TextFormatting.DARK_AQUA, "]"));
        }
        if (o.on(Flag.SOURCE_MOD_NAME) && !r.modName.isEmpty() && !"minecraft".equals(r.modId)) root.appendSibling(Msg.str(TextFormatting.BLUE, " (" + r.modName + ")"));
        if (o.on(Flag.ATTACKER_DISTANCE) && r.distance >= 0) root.appendSibling(Msg.str(TextFormatting.GRAY, " " + oneDecimal(r.distance) + "m"));
        if (o.on(Flag.ATTACKER_INVISIBILITY) && r.has(DamageRecord.INVISIBLE)) root.appendText(" ").appendSibling(m.tr(TextFormatting.LIGHT_PURPLE, "whathitme.badge.invisible"));
        if (o.on(Flag.ENTITY_OWNER) && !r.ownerName.isEmpty()) root.appendText(" ").appendSibling(m.tr(TextFormatting.GRAY, "whathitme.line.owner", r.ownerName));
        if (r.has(DamageRecord.TOTEM)) root.appendText(" ").appendSibling(m.tr(TextFormatting.GOLD, "whathitme.badge.totem"));
        if (r.has(DamageRecord.HEAVY)) root.appendText(" ").appendSibling(m.tr(TextFormatting.RED, "whathitme.badge.heavy"));
        if (o.on(Flag.ACTION_BUTTONS)) root.appendText(" ").appendSibling(copyIdButton(r.damageId)).appendText(" ").appendSibling(muteButton(r.damageId));
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

    public ITextComponent header(ITextComponent title) {
        return new TextComponentString("").appendSibling(Msg.str(TextFormatting.DARK_GRAY, "========== ")).appendSibling(title.setStyle(new Style().setColor(TextFormatting.GOLD)))
            .appendSibling(Msg.str(TextFormatting.DARK_GRAY, " =========="));
    }

    public List<ITextComponent> history(List<DamageRecord> records, long now, int max, int snapshotCount, boolean estimated, String who) {
        List<ITextComponent> out = new ArrayList<>();
        List<DamageRecord> vis = visible(records, max);
        out.add(header(who == null ? m.tr("whathitme.history.header", records.size()) : m.tr("whathitme.inspect.history", who, records.size())));
        if (estimated) out.add(m.tr(TextFormatting.GOLD, "whathitme.history.estimated"));
        if (vis.isEmpty()) out.add(m.tr(TextFormatting.GRAY, "whathitme.history.empty"));
        for (DamageRecord r : vis) out.add(line(r, now));
        if (records.size() > 0) out.add(total(records, now));
        if (snapshotCount > 0 && who == null) {
            out.add(new TextComponentString("").appendSibling(m.tr(TextFormatting.GRAY, "whathitme.history.snapshots", snapshotCount)).appendText(" ")
                .appendSibling(link(TextFormatting.AQUA, "whathitme.button.view", "/whm snapshot 1", "whathitme.button.run")));
        }
        return out;
    }

    /** "Last 10s Total" relative to the newest record */
    public ITextComponent total(List<DamageRecord> records, long end) {
        long window = o.summarySeconds * 1000L;
        float sum = 0;
        int hits = 0;
        for (DamageRecord r : records) {
            if (end - r.time <= window && end >= r.time - 50) {
                sum += r.taken();
                hits += Math.max(1, r.hits);
            }
        }
        return m.tr(TextFormatting.GOLD, "whathitme.summary.total", o.summarySeconds, heartsText(sum), hits);
    }

    private ITextComponent killingLine(Snapshot s, String key) {
        DamageRecord k = s.key();
        ITextComponent c = new TextComponentString("");
        if (k == null) {
            ITextComponent dm = deathMessage(s);
            c.appendSibling(m.tr(TextFormatting.RED, key, dm == null ? m.tr("whathitme.unknown") : dm));
            return c;
        }
        ITextComponent name = new TextComponentString("").appendSibling(displayName(k).setStyle(new Style().setColor(TextFormatting.WHITE)));
        if (o.on(Flag.DAMAGE_AMOUNT)) name.appendSibling(Msg.str(TextFormatting.RED, " [")).appendSibling(heartsText(k.taken()).setStyle(new Style().setColor(TextFormatting.RED)))
            .appendSibling(Msg.str(TextFormatting.RED, "]"));
        if (o.on(Flag.SOURCE_MOD_NAME) && !k.modName.isEmpty()) name.appendSibling(Msg.str(TextFormatting.BLUE, " (" + k.modName + ")"));
        if (o.on(Flag.ATTACKER_INVISIBILITY) && k.has(DamageRecord.INVISIBLE)) name.appendText(" ").appendSibling(m.tr(TextFormatting.LIGHT_PURPLE, "whathitme.badge.invisible"));
        if (o.on(Flag.ENTITY_OWNER) && !k.ownerName.isEmpty()) name.appendText(" ").appendSibling(m.tr(TextFormatting.GRAY, "whathitme.line.owner", k.ownerName));
        ITextComponent line = m.tr(TextFormatting.RED, key, name);
        line.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover(k)));
        c.appendSibling(line);
        if (k.has(DamageRecord.ESTIMATED)) c.appendText(" ").appendSibling(m.tr(TextFormatting.GOLD, "whathitme.badge.estimated"));
        if (o.on(Flag.ACTION_BUTTONS)) c.appendText(" ").appendSibling(copyIdButton(k.damageId)).appendText(" ").appendSibling(muteButton(k.damageId));
        return c;
    }

    public ITextComponent deathMessage(Snapshot s) {
        if (s.deathMessage.isEmpty()) return null;
        try {
            return ITextComponent.Serializer.jsonToComponent(s.deathMessage);
        } catch (Exception e) {
            return null;
        }
    }

    /** Chat summary after respawning */
    public List<ITextComponent> respawnSummary(Snapshot s) {
        List<ITextComponent> out = new ArrayList<>();
        out.add(m.tr(TextFormatting.GOLD, "whathitme.summary.header"));
        out.add(killingLine(s, "whathitme.summary.killing"));
        DamageRecord k = s.key();
        if (k != null && o.on(Flag.DAMAGE_PROPERTIES)) out.add(m.tr(TextFormatting.DARK_AQUA, "whathitme.summary.properties", properties(k.flags)));
        out.add(total(s.records, s.time));
        if (k != null && o.on(Flag.HINTS_AND_TIPS)) out.add(m.tr(TextFormatting.GREEN, "whathitme.summary.tip", tip(k)));
        ITextComponent recap = m.tr(TextFormatting.AQUA, "whathitme.summary.recap.link");
        recap.getStyle().setUnderlined(true).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/whm last"))
            .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, m.tr("whathitme.button.run", "/whm last")));
        out.add(m.tr(TextFormatting.AQUA, "whathitme.summary.recap", recap, "/whm last"));
        if (o.on(Flag.TOGGLE_CHAT_NOTICE)) {
            ITextComponent cmd = Msg.str(TextFormatting.YELLOW, "/whm togglechat");
            cmd.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/whm togglechat"));
            out.add(m.tr(TextFormatting.GRAY, "whathitme.summary.toggle", cmd));
        }
        out.add(m.tr(TextFormatting.GOLD, "whathitme.summary.footer"));
        return out;
    }

    public ITextComponent kindName(int kind) {
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
    public List<ITextComponent> fullRecap(Snapshot s, long now, String who) {
        List<ITextComponent> out = new ArrayList<>();
        ITextComponent title;
        if (who != null) title = m.tr("whathitme.inspect.death", who, age(now - s.time));
        else if (s.kind == Snapshot.DEATH) title = m.tr("whathitme.last.header", age(now - s.time));
        else title = m.tr("whathitme.snapshot.header", kindName(s.kind), age(now - s.time));
        out.add(header(title));
        ITextComponent dm = deathMessage(s);
        if (dm != null) out.add(new TextComponentString("").appendSibling(Msg.str(TextFormatting.GRAY, "“")).appendSibling(dm).appendSibling(Msg.str(TextFormatting.GRAY, "”")));
        out.add(killingLine(s, s.kind == Snapshot.DEATH ? "whathitme.summary.killing" : "whathitme.snapshot.trigger"));
        DamageRecord k = s.key();
        if (k != null && o.on(Flag.DAMAGE_PROPERTIES)) out.add(m.tr(TextFormatting.DARK_AQUA, "whathitme.summary.properties", properties(k.flags)));
        List<DamageRecord> vis = visible(s.records, 40);
        for (DamageRecord r : vis) out.add(line(r, s.time).appendSibling(Msg.str(TextFormatting.DARK_GRAY, "")));
        if (vis.isEmpty()) out.add(m.tr(TextFormatting.GRAY, "whathitme.history.empty"));
        out.add(total(s.records, s.time));
        if (k != null && o.on(Flag.HINTS_AND_TIPS)) out.add(m.tr(TextFormatting.GREEN, "whathitme.summary.tip", tip(k)));
        if (o.on(Flag.ACTION_BUTTONS) && o.clientActions && who == null && s.kind == Snapshot.DEATH) out.add(copyRecapButton());
        return out;
    }

    /** Chat alert after a totem pop / heavy hit */
    public ITextComponent alert(Snapshot s, int index) {
        DamageRecord k = s.key();
        String key = s.kind == Snapshot.TOTEM ? "whathitme.alert.totem" : s.kind == Snapshot.HEAVY ? "whathitme.alert.heavy" : "whathitme.alert.prevented";
        ITextComponent name = k == null ? m.tr("whathitme.unknown") : displayName(k).setStyle(new Style().setColor(TextFormatting.WHITE));
        ITextComponent amount = k == null ? Msg.str("?") : heartsText(k.taken()).setStyle(new Style().setColor(TextFormatting.RED));
        ITextComponent c = new TextComponentString("").appendSibling(m.tr(TextFormatting.GOLD, key, name, amount));
        if (k != null) c.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover(k)));
        c.appendText(" ").appendSibling(link(TextFormatting.AQUA, "whathitme.button.details", "/whm snapshot " + index, "whathitme.button.run"));
        return c;
    }

    // ---------------------------------------------------------------- effects / search

    public ITextComponent entryName(Entry e) {
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

    public List<ITextComponent> effects(List<Entry> list, boolean estimated) {
        List<ITextComponent> out = new ArrayList<>();
        out.add(header(m.tr("whathitme.effects.header", list.size())));
        if (list.isEmpty()) out.add(m.tr(TextFormatting.GRAY, "whathitme.effects.none"));
        for (Entry e : list) {
            boolean bad = (e.flags & 1) != 0;
            ITextComponent c = new TextComponentString("");
            ITextComponent name = entryName(e).setStyle(new Style().setColor(bad ? TextFormatting.RED : TextFormatting.GREEN));
            c.appendSibling(Msg.str(bad ? TextFormatting.RED : TextFormatting.GREEN, bad ? "✖ " : "✔ ")).appendSibling(name);
            if (e.amplifier > 0) {
                String lk = "enchantment.level." + (e.amplifier + 1);
                c.appendText(" ").appendSibling(m.localized && m.has(lk) ? m.tr(lk) : Msg.str(String.valueOf(e.amplifier + 1)));
            }
            c.appendSibling(Msg.str(TextFormatting.GRAY, " " + duration(e.duration)));
            if (o.on(Flag.SOURCE_MOD_NAME) && !e.modName.isEmpty()) c.appendSibling(Msg.str(TextFormatting.BLUE, " (" + e.modName + ")"));
            c.appendText(" ").appendSibling(e.milk ? m.tr(TextFormatting.AQUA, "whathitme.effects.milk.yes") : m.tr(TextFormatting.DARK_GRAY, "whathitme.effects.milk.no"));
            ITextComponent hov = new TextComponentString("").appendSibling(entryName(e).setStyle(new Style().setBold(true)));
            hov.appendText("\n").appendSibling(m.tr(TextFormatting.DARK_GRAY, "whathitme.hover.id", e.id));
            hov.appendText("\n").appendSibling(m.tr(bad ? TextFormatting.RED : TextFormatting.GREEN, bad ? "whathitme.effects.bad" : "whathitme.effects.good"));
            hov.appendText("\n").appendSibling(m.tr(TextFormatting.GRAY, "whathitme.effects.curative", e.extra.isEmpty() ? m.text("whathitme.effects.curative.none") : e.extra));
            if (!e.modName.isEmpty()) hov.appendText("\n").appendSibling(m.tr(TextFormatting.BLUE, "whathitme.hover.mod", e.modName));
            c.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hov));
            out.add(c);
        }
        if (estimated) out.add(m.tr(TextFormatting.GOLD, "whathitme.effects.estimated"));
        return out;
    }

    public List<ITextComponent> search(String query, List<Entry> list, int total) {
        List<ITextComponent> out = new ArrayList<>();
        out.add(header(m.tr("whathitme.search.header", query, total)));
        if (list.isEmpty()) out.add(m.tr(TextFormatting.GRAY, "whathitme.search.none"));
        for (Entry e : list) {
            ITextComponent c = new TextComponentString("");
            String typeKey = e.type == Entry.DAMAGE ? "whathitme.search.type.damage" : e.type == Entry.MOB ? "whathitme.search.type.mob" : "whathitme.search.type.effect";
            TextFormatting col = e.type == Entry.DAMAGE ? TextFormatting.RED : e.type == Entry.MOB ? TextFormatting.YELLOW : TextFormatting.LIGHT_PURPLE;
            c.appendSibling(Msg.str(col, "[")).appendSibling(m.tr(col, typeKey)).appendSibling(Msg.str(col, "] "));
            c.appendSibling(entryName(e).setStyle(new Style().setColor(TextFormatting.WHITE)));
            c.appendSibling(Msg.str(TextFormatting.DARK_GRAY, " " + e.id));
            if (e.type == Entry.DAMAGE && (e.flags & DamageRecord.PROPERTY_MASK) != 0 && o.on(Flag.DAMAGE_PROPERTIES)) {
                c.appendSibling(Msg.str(TextFormatting.DARK_AQUA, " [")).appendSibling(properties(e.flags).setStyle(new Style().setColor(TextFormatting.DARK_AQUA)))
                    .appendSibling(Msg.str(TextFormatting.DARK_AQUA, "]"));
            }
            if (e.type == Entry.EFFECT) c.appendText(" ").appendSibling((e.flags & 1) != 0 ? m.tr(TextFormatting.RED, "whathitme.effects.bad") : m.tr(TextFormatting.GREEN, "whathitme.effects.good"));
            if (!e.modName.isEmpty() && o.on(Flag.SOURCE_MOD_NAME)) c.appendSibling(Msg.str(TextFormatting.BLUE, " (" + e.modName + ")"));
            if (e.type == Entry.DAMAGE && e.count > 0) c.appendText(" ").appendSibling(m.tr(TextFormatting.GRAY, "whathitme.search.seen", e.count));
            if (e.type == Entry.DAMAGE && o.on(Flag.ACTION_BUTTONS)) {
                if (o.ignored(e.id)) c.appendText(" ").appendSibling(m.tr(TextFormatting.DARK_GRAY, "whathitme.search.muted"));
                c.appendText(" ").appendSibling(copyIdButton(e.id)).appendText(" ").appendSibling(muteButton(e.id));
            } else {
                c.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, e.id));
            }
            c.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, m.tr("whathitme.hover.id", e.id)));
            out.add(c);
        }
        if (total > list.size()) out.add(m.tr(TextFormatting.GRAY, "whathitme.search.more", total - list.size()));
        return out;
    }

    /** Plain text (no formatting codes), e.g. for the clipboard */
    public static String plain(List<ITextComponent> lines) {
        StringBuilder sb = new StringBuilder();
        for (ITextComponent c : lines) {
            sb.append(TextFormatting.getTextWithoutFormattingCodes(c.getUnformattedText())).append('\n');
        }
        return sb.toString();
    }
}
