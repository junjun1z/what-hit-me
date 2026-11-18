package io.github.junjun1z.whathitme.core.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import io.github.junjun1z.whathitme.core.Safe;
import io.github.junjun1z.whathitme.core.data.Json;
import io.github.junjun1z.whathitme.core.pack.PackBackend;
import io.github.junjun1z.whathitme.core.pack.PackClient;
import io.github.junjun1z.whathitme.core.pack.PackModel;
import io.github.junjun1z.whathitme.core.pack.PackModel.Cat;
import io.github.junjun1z.whathitme.core.pack.PackModel.Ent;
import io.github.junjun1z.whathitme.core.pack.PackModel.Node;
import io.github.junjun1z.whathitme.core.pack.PackPersonal;
import io.github.junjun1z.whathitme.core.rules.Conditions;
import io.github.junjun1z.whathitme.core.text.Fmt;
import io.github.junjun1z.whathitme.core.text.Msg;
import io.github.junjun1z.whathitme.core.text.Template;
import io.github.junjun1z.whathitme.core.text.TemplateVars;
import io.github.junjun1z.whathitme.core.text.TxtCodec;

/**
 * The in-game pack editor (/whm pack): every pack JSON section and the server settings, organised like FTB Quests
 * (category tree, entry list, detail form). Pack mode sends each confirmed entry to the server, which validates,
 * writes and reloads the pack; personal mode edits the player's own renameMap / hintMap / ignoredDamageIds. Drawn
 * with {@link Gfx} through {@link Ui}; a version only needs the thin screen host of {@link CoreScreen}.
 */
public final class PackEditor implements CoreScreen {

    public static final String[] SERVER_TABS = {"renames", "hints", "rules", "deathmessages", "templates", "triggers", "ignored", "locked", "settings"};
    public static final String[] PERSONAL_TABS = {"renames", "hints", "ignored"};
    private static final int M = 4;
    private static final int GAP = 2;
    /** Three panes from this width on, one pane with back navigation below */
    private static final int WIDE = 400;

    /** The open editor (self-test), null when none */
    public static volatile PackEditor current;

    public static String section(String tab) {
        switch (tab) {
            case "renames":
                return "renameMap";
            case "hints":
                return "hintMap";
            case "rules":
                return "rules";
            case "deathmessages":
                return "deathMessages";
            case "templates":
                return "templates";
            case "triggers":
                return "deathTriggers";
            case "ignored":
                return "defaultIgnoredDamageIds";
            case "locked":
                return "lockedFeatures";
            default:
                return "";
        }
    }

    /** One entry being edited */
    final class Draft {
        final String section;
        final Ent orig;
        final TextField key = new TextField("");
        final TextField value = new TextField("");
        final TextField memo = new TextField("");
        final TextField name = new TextField("");
        final TextField hint = new TextField("");
        final TextField command = new TextField("");
        final TextField cooldown = new TextField("");
        final List<TextField> ifs = new ArrayList<>();
        final List<TextField> anys = new ArrayList<>();
        final List<TextField> unlesses = new ArrayList<>();
        final List<TextField> commands = new ArrayList<>();
        final Set<String> hide = new LinkedHashSet<>();
        /** 0 = automatic, 1 = on, 2 = off */
        int enforce;
        boolean once;
        boolean noTip;
        List<String> lines = new ArrayList<>();
        /** Saved form of the original (dirty check) */
        String base;

        Draft(String section, Ent orig) {
            this.section = section;
            this.orig = orig;
            if (orig != null) load(orig);
            memo.placeholder = tr("whathitme.pack.memo.placeholder");
            base = signature();
        }

        private void load(Ent e) {
            memo.text = e.memo;
            if (PackModel.isObjectList(section)) {
                Map<String, Object> o = Json.obj(e.value);
                key.text = PackModel.str(o.get("match"));
                fill(ifs, o.get("if"));
                fill(anys, o.get("any"));
                fill(unlesses, o.get("unless"));
                name.text = o.containsKey("name") ? PackModel.str(o.get("name")) : "";
                if (o.containsKey("hint")) {
                    hint.text = PackModel.str(o.get("hint"));
                    noTip = hint.text.isEmpty();
                }
                for (Object h : o.get("hide") instanceof String ? Collections.singletonList(o.get("hide")) : Json.list(o.get("hide"))) hide.add(PackModel.str(h));
                if (o.containsKey("enforce")) enforce = Json.bool(o.get("enforce"), false) ? 1 : 2;
                Object cv = o.containsKey("commands") ? o.get("commands") : o.get("command");
                if ("deathTriggers".equals(section)) command.text = PackModel.str(o.get("command"));
                else fill(commands, cv);
                once = Json.bool(o.get("oncePerPlayer"), false);
                double cd = Json.num(o.get("cooldownSeconds"), 0);
                cooldown.text = cd == 0 ? "" : PackModel.str(o.get("cooldownSeconds"));
            } else if (PackModel.isMap(section)) {
                key.text = e.key;
                if ("templates".equals(section)) {
                    lines = new ArrayList<>();
                    for (Object l : Json.list(e.value)) lines.add(PackModel.str(l));
                } else value.text = PackModel.str(e.value);
            } else {
                key.text = e.key;
            }
            for (TextField f : all()) f.cursor = f.anchor = f.text.length();
        }

        private void fill(List<TextField> into, Object v) {
            List<Object> l = v instanceof String ? Collections.singletonList(v) : Json.list(v);
            for (Object o : l) into.add(new TextField(PackModel.str(o)));
        }

        List<TextField> all() {
            List<TextField> out = new ArrayList<>(Arrays.asList(key, value, name, hint, command, cooldown, memo));
            out.addAll(ifs);
            out.addAll(anys);
            out.addAll(unlesses);
            out.addAll(commands);
            return out;
        }

        private List<Object> texts(List<TextField> l) {
            List<Object> out = new ArrayList<>();
            for (TextField f : l) if (!f.text.trim().isEmpty()) out.add(f.text.trim());
            return out;
        }

        /** The value sent to the server */
        Object value() {
            if (PackModel.isObjectList(section)) {
                Map<String, Object> o = new LinkedHashMap<>();
                if (orig != null) o.putAll(Json.obj(orig.value));
                o.put("match", key.text.trim());
                putList(o, "if", texts(ifs));
                putList(o, "any", texts(anys));
                putList(o, "unless", texts(unlesses));
                if ("rules".equals(section)) {
                    put(o, "name", name.text);
                    if (noTip) o.put("hint", "");
                    else put(o, "hint", hint.text);
                    putList(o, "hide", new ArrayList<Object>(hide));
                    if (enforce == 0) o.remove("enforce");
                    else o.put("enforce", enforce == 1);
                    o.remove("command");
                    putList(o, "commands", texts(commands));
                } else {
                    o.put("command", command.text.trim());
                }
                if (once) o.put("oncePerPlayer", true);
                else o.remove("oncePerPlayer");
                double cd = Json.num(cooldown.text.trim(), 0);
                if (cd > 0) o.put("cooldownSeconds", (double) Math.round(cd));
                else o.remove("cooldownSeconds");
                put(o, "memo", memo.text);
                // keep "match" first and the memo last for readable files
                return o;
            }
            if ("templates".equals(section)) return new ArrayList<Object>(lines);
            if (PackModel.isMap(section)) return value.text;
            return key.text.trim();
        }

        private void put(Map<String, Object> o, String k, String v) {
            if (v == null || v.trim().isEmpty()) o.remove(k);
            else o.put(k, v);
        }

        private void putList(Map<String, Object> o, String k, List<Object> v) {
            if (v.isEmpty()) o.remove(k);
            else o.put(k, v);
        }

        String signature() {
            return Json.write(Arrays.asList(key.text, Json.write(value()), memo.text));
        }

        boolean dirty() {
            return orig == null || !signature().equals(base);
        }

        boolean readOnly() {
            return orig != null && orig.cat.readOnly() || selectedCat() != null && selectedCat().readOnly();
        }
    }

    private boolean canPack;
    private PackBackend backend;
    private boolean personal;
    private final PackModel model = new PackModel();
    private List<Node> tree = new ArrayList<>();
    private String tab;
    /** Selected tree node: "cat:<path>" or "dir:<folder>" */
    private String selNode;
    private Draft draft;
    private String settingKey;
    private final TextField settingField = new TextField("");
    private int treeScroll;
    private int listScroll;
    private int formScroll;
    private int settingsScroll;
    private int previewScroll;
    private final TextField search = new TextField("");
    private TextField focused;
    private Dialog dialog;
    private String status = "";
    private boolean statusOk = true;
    private long statusAt;
    private String banner = "";
    private final List<String> previewLines = new ArrayList<>();
    private String previewAs = "";
    private final TextField assume = new TextField("");
    private boolean showPreview;
    private int pane;
    private TemplateEditor child;
    private boolean close;
    private boolean loaded;
    private boolean denied;
    private final Ui ui = new Ui();
    private int reqSeq;
    /** Drafts sent and not answered yet: req -> draft (restored when the server refuses it) */
    private final Map<String, Draft> pending = new HashMap<>();
    /** After the next state: select this entry (section, key or value JSON) */
    private String[] reselect;
    private final long opened = System.currentTimeMillis();
    private boolean blink;
    private List<String> tip;
    private int lastW = 400;
    private int lastH = 240;
    private int formRows;
    /** Last result message (self-test) */
    private String lastResult = "";
    private volatile int results;
    /** Last "someone else changed ..." notice (self-test) */
    private volatile String lastNotice = "";
    private volatile int states;

    /**
     * @param personalMode start in personal mode (also forced when the player may not edit the pack)
     */
    public PackEditor(boolean personalMode) {
        canPack = Safe.get("permission", () -> ClientCore.serverHasMod && ClientCore.op(), false);
        assume.placeholder = tr("whathitme.pack.assume.placeholder");
        search.placeholder = tr("whathitme.pack.search");
        start(personalMode || !canPack);
        current = this;
    }

    private void start(boolean personalMode) {
        personal = personalMode;
        backend = personal ? new PackPersonal() : new PackClient();
        backend.listen(this::onMessage);
        loaded = false;
        denied = false;
        draft = null;
        selNode = null;
        settingKey = null;
        tab = "renames";
        pane = personal ? 1 : 0;
        model.cats.clear();
        tree = new ArrayList<>();
        send(op("open"));
    }

    private static String tr(String key, Object... args) {
        return Msg.LOCAL.text(key, args);
    }

    // ------------------------------------------------------------ public API (self-test, hosts)

    public boolean personal() {
        return personal;
    }

    public boolean canEditPack() {
        return canPack;
    }

    public boolean loaded() {
        return loaded;
    }

    public boolean denied() {
        return denied;
    }

    public PackModel model() {
        return model;
    }

    public String tab() {
        return tab;
    }

    public String status() {
        return Fmt.strip(status);
    }

    public String lastResult() {
        return lastResult;
    }

    /** Number of results / states received (self-test waits on them) */
    public int results() {
        return results;
    }

    public String lastNotice() {
        return lastNotice;
    }

    public int states() {
        return states;
    }

    public String banner() {
        return Fmt.strip(banner);
    }

    public List<String> previewLines() {
        List<String> out = new ArrayList<>();
        for (String l : previewLines) out.add(Fmt.strip(l));
        return out;
    }

    /** Area of a clickable element of the last frame (self-test) */
    public int[] area(String id) {
        return ui.area(id);
    }

    public List<String> areaIds() {
        return ui.ids();
    }

    /** Clicks an element of the last frame by id (self-test); false when it was not shown */
    public boolean press(String id) {
        return Safe.get("pack editor", () -> ui.press(id), false);
    }

    public boolean dialogOpen() {
        return dialog != null;
    }

    public String dialogTitle() {
        return dialog == null ? "" : dialog.title;
    }

    public TemplateEditor child() {
        return child;
    }

    public boolean wantsClose() {
        return close;
    }

    public void closed() {
        if (child != null) child.closed();
        if (current == this) current = null;
        send(op("close"));
    }

    public void switchMode(boolean toPersonal) {
        if (toPersonal == personal || (!toPersonal && !canPack)) return;
        if (!confirmDraft()) return;
        send(op("close"));
        start(toPersonal);
    }

    public void selectTab(String t) {
        if (!Arrays.asList(tabs()).contains(t) || t.equals(tab)) return;
        if (!confirmDraft()) return;
        tab = t;
        draft = null;
        listScroll = 0;
        formScroll = 0;
        search.text = "";
        focused = null;
        if (pane == 2) pane = 1;
        if ("settings".equals(t)) pane = 1;
        else if (!personal && selectedCat() == null) pane = 0;
    }

    public String[] tabs() {
        return personal ? PERSONAL_TABS : SERVER_TABS;
    }

    public Cat selectedCat() {
        if (personal) return model.cats.isEmpty() ? null : model.cats.get(0);
        return selNode != null && selNode.startsWith("cat:") ? model.cat(selNode.substring(4)) : null;
    }

    public String selectedFolder() {
        if (selNode != null && selNode.startsWith("dir:")) return selNode.substring(4);
        Cat c = selectedCat();
        return c == null || c.script ? "" : c.folder();
    }

    /** Selects a category (path) or folder ("dir:path") */
    public void selectNode(String id) {
        if (id == null || id.equals(selNode)) {
            if (id != null && id.startsWith("cat:")) pane = 1;
            return;
        }
        if (!confirmDraft()) return;
        selNode = id;
        draft = null;
        listScroll = 0;
        if (id.startsWith("cat:")) pane = 1;
    }

    public void selectCategory(String path) {
        selectNode("cat:" + path);
    }

    /** Entries of the selected category in the current tab */
    public List<Ent> entries() {
        return PackModel.entries(selectedCat(), section(tab));
    }

    /** Selects an entry (saving the open one first) */
    public void selectEntry(Ent e) {
        if (e == null) return;
        if (draft != null && draft.orig != null && same(draft.orig, e)) {
            pane = 2;
            return;
        }
        if (!confirmDraft()) return;
        draft = new Draft(section(tab), e);
        formScroll = 0;
        focused = null;
        pane = 2;
        autoPreview();
    }

    private static boolean same(Ent a, Ent b) {
        return a.cat.path.equals(b.cat.path) && a.section.equals(b.section) && a.index == b.index
            && (a.key == null ? b.key == null : a.key.equals(b.key));
    }

    /** Starts a new entry in the selected category */
    public void newEntry() {
        Cat c = selectedCat();
        if (c == null || c.readOnly() || "settings".equals(tab)) return;
        if (!confirmDraft()) return;
        draft = new Draft(section(tab), null);
        if ("templates".equals(tab)) draft.key.text = firstFreeOutput(c);
        formScroll = 0;
        focused = draft.key;
        pane = 2;
    }

    private String firstFreeOutput(Cat c) {
        Set<String> used = new java.util.HashSet<>();
        for (Ent e : PackModel.entries(c, "templates")) used.add(e.key);
        for (String o : Template.OUTPUTS) if (!used.contains(o)) return o;
        return Template.OUTPUTS[0];
    }

    public Draft draft() {
        return draft;
    }

    /** Text fields of the open entry by name (self-test): key, value, memo, name, hint, command, cooldown */
    public TextField field(String name) {
        if (draft == null) return null;
        switch (name) {
            case "key":
                return draft.key;
            case "value":
                return draft.value;
            case "memo":
                return draft.memo;
            case "name":
                return draft.name;
            case "hint":
                return draft.hint;
            case "command":
                return draft.command;
            case "cooldown":
                return draft.cooldown;
            default:
                return null;
        }
    }

    /** Adds a condition to the open rule / trigger: list "if", "any" or "unless" */
    public void addCondition(String list, String cond) {
        if (draft == null) return;
        List<TextField> l = "any".equals(list) ? draft.anys : "unless".equals(list) ? draft.unlesses : draft.ifs;
        l.add(new TextField(cond));
    }

    public void setHide(String field, boolean on) {
        if (draft == null) return;
        if (on) draft.hide.add(field);
        else draft.hide.remove(field);
    }

    public void setOnce(boolean on) {
        if (draft != null) draft.once = on;
    }

    public void setTemplateLines(List<String> lines) {
        if (draft != null) draft.lines = new ArrayList<>(lines);
    }

    private String nextReq() {
        return "r" + (++reqSeq);
    }

    private Map<String, Object> op(String name) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("op", name);
        return m;
    }

    private void send(Map<String, Object> op) {
        Safe.run("pack editor send", () -> backend.send(op));
    }

    // ------------------------------------------------------------ saving

    /** Client checks of the open entry: null = fine, otherwise the first error */
    public String draftError() {
        if (draft == null) return null;
        String s = draft.section;
        if (!PackModel.isObjectList(s) && draft.key.text.trim().isEmpty()) return tr("whathitme.pack.err.empty");
        if ("templates".equals(s) && !Template.isOutput(draft.key.text.trim().toLowerCase(Locale.ROOT))) {
            return tr("whathitme.pack.err.output", draft.key.text.trim());
        }
        if ("deathTriggers".equals(s)) {
            if (draft.key.text.trim().isEmpty()) return tr("whathitme.pack.err.nomatch");
            if (draft.command.text.trim().isEmpty()) return tr("whathitme.pack.err.nocommand");
        }
        if (PackModel.isObjectList(s)) {
            for (TextField f : condFields()) {
                String p = condProblem(f.text);
                if (p != null && p.startsWith("!")) return p.substring(1);
            }
        }
        return null;
    }

    private List<TextField> condFields() {
        List<TextField> l = new ArrayList<>(draft.ifs);
        l.addAll(draft.anys);
        l.addAll(draft.unlesses);
        return l;
    }

    /** Saves the open entry when it changed (Done); false when it has an error */
    public boolean done() {
        if (draft == null || draft.readOnly()) return true;
        if (!draft.dirty()) return true;
        String err = draftError();
        if (err != null) {
            flash(err, false);
            return false;
        }
        Cat c = draft.orig != null ? draft.orig.cat : selectedCat();
        if (c == null) return true;
        Map<String, Object> o = op("entry.put");
        String req = nextReq();
        o.put("req", req);
        o.put("file", c.path);
        o.put("section", draft.section);
        Object value = draft.value();
        o.put("value", value);
        String key = draft.key.text.trim();
        o.put("key", key);
        if (draft.orig != null) {
            if (draft.orig.index >= 0) {
                o.put("index", draft.orig.index);
                o.put("expect", draft.orig.expect());
            } else o.put("oldKey", draft.orig.key);
        }
        if (!PackModel.isObjectList(draft.section)) o.put("memo", draft.memo.text);
        pending.put(req, draft);
        reselect = new String[] {draft.section, PackModel.isMap(draft.section) ? key : null, PackModel.isMap(draft.section) ? null : Json.write(value)};
        send(o);
        draft.base = draft.signature();
        return true;
    }

    /** Saves a changed entry before moving on; false keeps the user where they are */
    private boolean confirmDraft() {
        if (draft == null || !draft.dirty() || draft.readOnly()) return true;
        if (draft.orig == null && draft.key.text.trim().isEmpty() && draft.value.text.isEmpty() && draft.command.text.isEmpty()) return true;
        return done();
    }

    public void revert() {
        if (draft == null) return;
        Ent o = draft.orig;
        draft = o == null ? null : new Draft(draft.section, o);
        focused = null;
    }

    public void deleteEntry() {
        if (draft == null || draft.orig == null || draft.readOnly()) return;
        Ent e = draft.orig;
        Map<String, Object> o = op("entry.delete");
        o.put("req", nextReq());
        o.put("file", e.cat.path);
        o.put("section", e.section);
        if (e.index >= 0) {
            o.put("index", e.index);
            o.put("expect", e.expect());
        } else o.put("key", e.key);
        if (!PackModel.isObjectList(e.section) && e.index >= 0) o.put("key", e.key);
        draft = null;
        pane = 1;
        send(o);
    }

    /** Moves the open entry up (-1) or down (+1) in its list */
    public void moveEntry(int d) {
        if (draft == null || draft.orig == null || draft.readOnly()) return;
        if (!done()) return;
        Ent e = draft.orig;
        List<Ent> all = PackModel.entries(e.cat, e.section);
        int i = -1;
        for (int k = 0; k < all.size(); k++) if (same(all.get(k), e)) i = k;
        if (i < 0 || i + d < 0 || i + d >= all.size()) return;
        Map<String, Object> o = op("entry.move");
        o.put("req", nextReq());
        o.put("file", e.cat.path);
        o.put("section", e.section);
        if (e.index >= 0) {
            o.put("index", e.index);
            o.put("expect", e.expect());
        }
        if (e.key != null) o.put("key", e.key);
        o.put("to", i + d);
        reselect = new String[] {e.section, PackModel.isMap(e.section) ? e.key : null, PackModel.isMap(e.section) ? null : Json.write(e.value)};
        send(o);
    }

    /** Moves the open entry to another category */
    public void transferEntry(String toPath) {
        if (draft == null || draft.orig == null || draft.readOnly()) return;
        if (!done()) return;
        Ent e = draft.orig;
        Map<String, Object> o = op("entry.transfer");
        o.put("req", nextReq());
        o.put("file", e.cat.path);
        o.put("to", toPath);
        o.put("section", e.section);
        if (e.index >= 0) {
            o.put("index", e.index);
            o.put("expect", e.expect());
        }
        if (e.key != null) o.put("key", e.key);
        draft = null;
        selNode = "cat:" + toPath;
        reselect = new String[] {e.section, PackModel.isMap(e.section) ? e.key : null, PackModel.isMap(e.section) ? null : Json.write(e.value)};
        send(o);
    }

    public void undo() {
        Map<String, Object> o = op("undo");
        o.put("req", nextReq());
        draft = null;
        send(o);
    }

    // category / folder actions

    private void catOp(String name, Object... kv) {
        Map<String, Object> o = op(name);
        o.put("req", nextReq());
        for (int i = 0; i + 1 < kv.length; i += 2) o.put((String) kv[i], kv[i + 1]);
        send(o);
    }

    public void createCategory(String folder, String name) {
        String path = (folder.isEmpty() ? "" : folder + "/") + name.trim() + (name.trim().toLowerCase(Locale.ROOT).endsWith(".json") ? "" : ".json");
        catOp("cat.create", "path", path);
        selNode = "cat:" + path;
        pane = 1;
    }

    public void createFolder(String parent, String name) {
        String path = (parent.isEmpty() ? "" : parent + "/") + name.trim();
        catOp("dir.create", "path", path);
        selNode = "dir:" + path;
    }

    public void renameSelected(String newName) {
        Cat c = selectedCat();
        String n = newName.trim();
        if (c != null && !c.script) {
            String to = (c.folder().isEmpty() ? "" : c.folder() + "/") + n + (n.toLowerCase(Locale.ROOT).endsWith(".json") ? "" : ".json");
            catOp("cat.rename", "from", c.path, "to", to);
            selNode = "cat:" + to;
        } else if (selNode != null && selNode.startsWith("dir:")) {
            String from = selNode.substring(4);
            int i = from.lastIndexOf('/');
            String to = (i < 0 ? "" : from.substring(0, i + 1)) + n;
            catOp("dir.rename", "from", from, "to", to);
            selNode = "dir:" + to;
        }
    }

    /** Moves the selected category / folder into another folder ("" = pack root) */
    public void moveSelectedTo(String folder) {
        Cat c = selectedCat();
        if (c != null && !c.script) {
            String to = (folder.isEmpty() ? "" : folder + "/") + c.path.substring(c.path.lastIndexOf('/') + 1);
            if (to.equals(c.path)) return;
            catOp("cat.rename", "from", c.path, "to", to);
            selNode = "cat:" + to;
        } else if (selNode != null && selNode.startsWith("dir:")) {
            String from = selNode.substring(4);
            String to = (folder.isEmpty() ? "" : folder + "/") + from.substring(from.lastIndexOf('/') + 1);
            if (to.equals(from)) return;
            catOp("dir.rename", "from", from, "to", to);
            selNode = "dir:" + to;
        }
    }

    public void deleteSelected() {
        Cat c = selectedCat();
        if (c != null && !c.script) {
            catOp("cat.delete", "path", c.path);
            selNode = null;
            draft = null;
        } else if (selNode != null && selNode.startsWith("dir:")) {
            catOp("dir.delete", "path", selNode.substring(4));
            selNode = null;
        }
    }

    public void setEnabled(String path, boolean on) {
        catOp("cat.enabled", "path", path, "enabled", on);
    }

    public void setCategoryMemo(String path, String memo) {
        catOp("cat.memo", "path", path, "memo", memo);
    }

    /** Moves the selected tree node up (-1) / down (+1) among its siblings and stores the new load order */
    public void moveSelected(int d) {
        if (selNode == null) return;
        String parent;
        Object me;
        Cat c = selectedCat();
        if (c != null) {
            if (c.script) return;
            parent = c.folder();
            me = c;
        } else {
            String f = selNode.substring(4);
            int i = f.lastIndexOf('/');
            parent = i < 0 ? "" : f.substring(0, i);
            me = f;
        }
        List<Object> kids = new ArrayList<>(model.children(parent));
        int i = kids.indexOf(me);
        if (i < 0 || i + d < 0 || i + d >= kids.size()) return;
        Collections.swap(kids, i, i + d);
        List<String> order = model.treeOrder("", kids, parent);
        catOp("cat.order", "paths", new ArrayList<Object>(order));
    }

    // settings

    public void selectSetting(String key) {
        settingKey = key;
        Map<String, Object> s = setting(key);
        settingField.text = s == null ? "" : PackModel.str(s.get("value"));
        settingField.cursor = settingField.anchor = settingField.text.length();
        pane = 2;
    }

    public Map<String, Object> setting(String key) {
        if (key == null) return null;
        for (Map<String, Object> s : model.settings) if (key.equals(PackModel.str(s.get("key")))) return s;
        return null;
    }

    public void applySetting(String key, String value) {
        catOp("setting", "key", key, "value", value);
    }

    // preview

    /** /whm test arguments for the open entry: damage id, entity, item, dimension from its match key */
    public String testArgs() {
        if (draft == null) return "";
        String key = draft.key.text.trim();
        if ("ignored".equals(tab) || "locked".equals(tab) || "templates".equals(tab)) return "";
        String dmg = "";
        String ent = "-";
        String item = "-";
        String dim = "";
        for (String t : tokens(key)) {
            if (t.startsWith("@item:")) item = t.substring(6);
            else if (t.startsWith("@dim:")) dim = t.substring(5);
            else if (t.startsWith("@mob:")) ent = t.substring(5);
            else if (t.startsWith("@")) ent = t.substring(1);
            else if (dmg.isEmpty()) dmg = t;
        }
        if (dmg.isEmpty() || dmg.equals("*")) dmg = !ent.equals("-") ? "mob:" + concrete(ent, model.mobs, "minecraft:zombie") : "minecraft:generic";
        else dmg = concreteDamage(dmg);
        if (ent.indexOf('*') >= 0 || ent.indexOf('?') >= 0) ent = concrete(ent, model.mobs, "minecraft:zombie");
        if (item.indexOf('*') >= 0) item = "-";
        StringBuilder sb = new StringBuilder(dmg);
        boolean withDim = !dim.isEmpty() && dim.indexOf('*') < 0;
        if (withDim || !item.equals("-")) sb.append(' ').append(ent).append(' ').append(item);
        else if (!ent.equals("-")) sb.append(' ').append(ent);
        if (withDim) sb.append(' ').append(dim);
        if (!personal && !previewAs.isEmpty()) sb.append(" --as ").append(previewAs);
        if (!personal) for (String a : assume.text.split("[ ,]+")) if (!a.trim().isEmpty()) sb.append(" --assume ").append(a.trim());
        return sb.toString();
    }

    private String concreteDamage(String pattern) {
        if (pattern.indexOf('*') < 0 && pattern.indexOf('?') < 0) return pattern;
        java.util.regex.Pattern p = io.github.junjun1z.whathitme.core.config.WhmConfig.glob(pattern);
        for (String[] r : model.damage) if (p.matcher(r[0]).matches()) return r[0];
        for (String[] r : model.mobs) if (p.matcher("mob:" + r[0]).matches()) return "mob:" + r[0];
        return "minecraft:generic";
    }

    private static String concrete(String pattern, List<String[]> rows, String def) {
        if (pattern.indexOf('*') < 0 && pattern.indexOf('?') < 0) return pattern;
        java.util.regex.Pattern p = io.github.junjun1z.whathitme.core.config.WhmConfig.glob(pattern);
        for (String[] r : rows) if (p.matcher(r[0]).matches()) return r[0];
        return def;
    }

    /** Runs /whm test for the open entry (the server's own code with --as / --assume) */
    public void runPreview() {
        String a = testArgs();
        if (a.isEmpty()) return;
        Map<String, Object> o = op("test");
        o.put("args", a);
        send(o);
    }

    private void autoPreview() {
        if (draft != null && draft.orig != null && !testArgs().isEmpty()) runPreview();
        else previewLines.clear();
    }

    // ------------------------------------------------------------ messages

    private void onMessage(Map<String, Object> m) {
        Safe.run("pack editor message", () -> message(m));
    }

    private void message(Map<String, Object> m) {
        String type = PackModel.str(m.get("type"));
        switch (type) {
            case "denied":
                if (!personal && !Safe.get("op level", () -> ClientCore.port.knowsOpLevel(), true)) {
                    // the client could not know: the server says no, so show the own lists
                    canPack = false;
                    start(true);
                    flash(tr("whathitme.pack.noperm"), false);
                    break;
                }
                denied = true;
                loaded = true;
                flash(tr("whathitme.error.noperm"), false);
                break;
            case "lists":
                model.lists(m);
                if (previewAs.isEmpty() && !model.players.isEmpty()) {
                    String me = Safe.get("player name", () -> ClientCore.port.playerName(), "");
                    previewAs = model.players.contains(me) ? me : model.players.get(0);
                }
                break;
            case "state":
                model.state(m);
                states++;
                tree = personal ? new ArrayList<Node>() : model.tree();
                loaded = true;
                Map<String, Object> notice = Json.obj(m.get("notice"));
                if (!notice.isEmpty()) {
                    String who = PackModel.str(notice.get("who"));
                    List<String> files = new ArrayList<>();
                    for (Object f : Json.list(notice.get("files"))) files.add(PackModel.str(f));
                    flash(tr("whathitme.pack.notice", who, String.join(", ", files)), true);
                    lastNotice = who + ": " + String.join(", ", files);
                    if (draft != null && draft.dirty() && draft.orig != null && files.contains(draft.orig.cat.path)) {
                        banner = tr("whathitme.pack.conflict", who);
                    }
                }
                resolveSelection();
                break;
            case "result": {
                boolean ok = Json.bool(m.get("ok"), false);
                List<Object> args = Json.list(m.get("args"));
                String text = tr(PackModel.str(m.get("key")), args.toArray());
                lastResult = (ok ? "ok: " : "error: ") + PackModel.str(m.get("key"));
                results++;
                flash(text, ok);
                String req = PackModel.str(m.get("req"));
                Draft d = pending.remove(req);
                if (!ok && d != null) {
                    draft = d;
                    draft.base = "";
                    reselect = null;
                }
                if (ok) banner = "";
                if (ok && d != null) autoPreviewLater = true;
                break;
            }
            case "test":
                previewLines.clear();
                for (Object l : Json.list(m.get("lines"))) previewLines.add(TxtCodec.read(l).toLegacy(Msg.LOCAL.lang()));
                previewScroll = 0;
                break;
            default:
                break;
        }
    }

    private boolean autoPreviewLater;

    /** After a new state: keep the selected category / entry when it still exists */
    private void resolveSelection() {
        if (selNode != null && selNode.startsWith("cat:") && model.cat(selNode.substring(4)) == null) {
            boolean waiting = false;
            for (Draft d : pending.values()) if (d != null) waiting = true;
            if (!waiting) selNode = null;
        }
        Cat c = selectedCat();
        if (reselect != null && c != null && reselect[0].equals(section(tab))) {
            for (Ent e : PackModel.entries(c, reselect[0])) {
                boolean hit = reselect[1] != null ? reselect[1].equals(e.key) : reselect[2].equals(Json.write(e.value));
                if (hit) {
                    boolean keepFocus = draft != null;
                    TextField f = focused;
                    draft = new Draft(reselect[0], e);
                    if (keepFocus && f != null) focused = null;
                    break;
                }
            }
            reselect = null;
        } else if (draft != null && draft.orig != null) {
            // the entry may have changed on the server: reload it when the draft is clean
            Ent found = null;
            for (Ent e : PackModel.entries(model.cat(draft.orig.cat.path), draft.section)) if (same(e, draft.orig)) found = e;
            if (found == null && !draft.dirty()) draft = null;
            else if (found != null && !draft.dirty()) draft = new Draft(draft.section, found);
        }
        if (autoPreviewLater) {
            autoPreviewLater = false;
            autoPreview();
        }
        if (settingKey != null) {
            Map<String, Object> s = setting(settingKey);
            if (s != null && focused != settingField) settingField.text = PackModel.str(s.get("value"));
        }
    }

    private void flash(String text, boolean ok) {
        status = text;
        statusOk = ok;
        statusAt = System.currentTimeMillis();
    }

    // ------------------------------------------------------------ checks shown under the fields

    static List<String> tokens(String key) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (c == ' ' || c == '&' || c == '\t') {
                if (cur.length() > 0) out.add(cur.toString());
                cur.setLength(0);
            } else if (c == '@' && cur.length() > 0) {
                out.add(cur.toString());
                cur.setLength(0);
                cur.append(c);
            } else cur.append(c);
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }

    /** Warning when a match key matches no known damage id / mob (null = fine or unknown lists) */
    public String keyProblem(String key) {
        if (key.trim().isEmpty() || model.damage.isEmpty()) return null;
        for (String t : tokens(key)) {
            if (t.startsWith("@item:") || t.startsWith("@dim:")) continue;
            boolean mob = t.startsWith("@");
            String pat = t.startsWith("@mob:") ? t.substring(5) : mob ? t.substring(1) : t;
            if (pat.equals("*") || pat.isEmpty()) continue;
            java.util.regex.Pattern p = io.github.junjun1z.whathitme.core.config.WhmConfig.glob(pat);
            boolean hit = false;
            if (mob) {
                for (String[] r : model.mobs) if (p.matcher(r[0]).matches()) hit = true;
            } else {
                for (String[] r : model.damage) if (p.matcher(r[0]).matches()) hit = true;
                if (!hit) for (String[] r : model.mobs) if (p.matcher("mob:" + r[0]).matches()) hit = true;
            }
            if (!hit) return tr("whathitme.pack.warn.nomatch", t);
        }
        return null;
    }

    /** Problem of a condition: "!text" = error (refused), "text" = warning, null = fine */
    public String condProblem(String raw) {
        String c = raw.trim();
        if (c.isEmpty()) return null;
        if (c.startsWith("!")) c = c.substring(1).trim();
        int colon = c.indexOf(':');
        if (colon <= 0) return "!" + tr("whathitme.pack.err.condprefix", raw.trim());
        String prefix = c.substring(0, colon).toLowerCase(Locale.ROOT);
        String arg = c.substring(colon + 1).trim();
        if (!Conditions.known(prefix)) return "!" + tr("whathitme.pack.err.condunknown", prefix, String.join(", ", Conditions.prefixes()));
        if (arg.isEmpty()) return "!" + tr("whathitme.pack.err.condempty", raw.trim());
        if ("score".equals(prefix) && !arg.matches("[^<>=!]+(>=|<=|!=|>|<|=)\\s*-?\\d+")) return "!" + tr("whathitme.pack.err.score", arg);
        String problem = model.condProblems.get(prefix);
        if (problem != null && !problem.isEmpty()) return tr("whathitme.pack.warn.cond", prefix, problem);
        if ("script".equals(prefix) && model.hasLists) {
            boolean found = false;
            for (String s : model.conds.getOrDefault("script", Collections.<String>emptyList())) if (s.split("\t")[0].equals(arg)) found = true;
            if (!found) return tr("whathitme.pack.warn.script", arg);
        }
        return null;
    }

    private static String varProblem(String text) {
        java.util.Collection<String> u = Template.unknown(Collections.singletonList(text));
        return u.isEmpty() ? null : tr("whathitme.pack.warn.vars", String.join(", ", u));
    }

    /** Server warnings about the open entry ("file #n: ...") */
    private List<String> serverWarnings() {
        List<String> out = new ArrayList<>();
        if (draft == null || draft.orig == null) return out;
        String file = draft.orig.cat.path.toLowerCase(Locale.ROOT);
        String tag = draft.orig.index >= 0 ? file + " #" + (draft.orig.index + 1) : null;
        for (String w : model.warnings) {
            if (tag != null && (w.startsWith(tag + ":") || w.startsWith(tag + " "))) out.add(w.substring(file.length()).trim());
            else if (draft.orig.key != null && w.startsWith(file) && w.contains("\"" + draft.orig.key + "\"")) out.add(w.substring(file.length()).trim());
        }
        return out;
    }

    // ------------------------------------------------------------ input

    public boolean key(int key, boolean ctrl, boolean shift) {
        return Safe.get("pack editor", () -> keyImpl(key, ctrl, shift), false);
    }

    private boolean keyImpl(int key, boolean ctrl, boolean shift) {
        if (child != null) return child.key(key, ctrl, shift);
        if (dialog != null) return dialog.key(key, ctrl, shift);
        switch (key) {
            case TemplateEditor.K_ESCAPE:
                if (focused != null) {
                    focused = null;
                    return true;
                }
                if (confirmDraft()) close = true;
                return true;
            case TemplateEditor.K_ENTER:
                if (focused == settingField && settingKey != null) {
                    applySetting(settingKey, settingField.text);
                    return true;
                }
                if (focused == assume) {
                    runPreview();
                    return true;
                }
                if (draft != null && focused != null && focused != search) {
                    done();
                    return true;
                }
                return true;
            case TemplateEditor.K_TAB: {
                if (draft == null) return true;
                List<TextField> order = visibleFields();
                if (order.isEmpty()) return true;
                int i = order.indexOf(focused);
                focused = order.get((i + (shift ? order.size() - 1 : 1)) % order.size());
                return true;
            }
            case TemplateEditor.K_UP:
            case TemplateEditor.K_DOWN:
                if (focused == null || focused == search) {
                    stepEntry(key == TemplateEditor.K_UP ? -1 : 1);
                    return true;
                }
                return true;
            default:
                return focused != null && focused.key(key, ctrl, shift);
        }
    }

    private final List<TextField> shownFields = new ArrayList<>();

    private List<TextField> visibleFields() {
        return new ArrayList<>(shownFields);
    }

    private void stepEntry(int d) {
        List<Ent> list = filtered(entries());
        if (list.isEmpty()) return;
        int i = -1;
        if (draft != null && draft.orig != null) for (int k = 0; k < list.size(); k++) if (same(list.get(k), draft.orig)) i = k;
        int j = Math.max(0, Math.min(list.size() - 1, i + d));
        selectEntry(list.get(j));
    }

    public boolean typed(char c) {
        return Safe.get("pack editor", () -> {
            if (child != null) return child.typed(c);
            if (dialog != null) return dialog.typed(c);
            if (focused == null) return false;
            focused.typed(c);
            if (focused == search) listScroll = 0;
            return true;
        }, false);
    }

    public boolean mouseDown(int button, double x, double y, boolean shift) {
        return Safe.get("pack editor", () -> {
            if (child != null) return child.mouseDown(button, x, y, shift);
            if (button != 0) return false;
            TextField before = focused;
            boolean used = ui.click(x, y, shift);
            if (!used && dialog == null) focused = null;
            if (dialog != null && !used) return true;
            return used || before != focused;
        }, false);
    }

    public boolean scroll(double x, double y, double delta) {
        return Safe.get("pack editor", () -> {
            if (child != null) return child.scroll(x, y, delta);
            return ui.wheel(x, y, delta);
        }, false);
    }

    // ------------------------------------------------------------ drawing

    public void render(Gfx g, int w, int h, int mx, int my) {
        try {
            if (child != null) {
                child.render(g, w, h, mx, my);
                if (child.wantsClose()) {
                    child.closed();
                    child = null;
                }
                return;
            }
            renderImpl(g, w, h, mx, my);
        } catch (Throwable t) {
            Safe.warn("pack editor", t);
        }
    }

    private void renderImpl(Gfx g, int w, int h, int mx, int my) {
        lastW = w;
        lastH = h;
        ui.begin(g, mx, my);
        tip = null;
        shownFields.clear();
        blink = ((System.currentTimeMillis() - opened) / 500) % 2 == 0;
        g.fill(0, 0, w, h, 0xC0101018);
        int x = M;
        int y = 3;
        if (canPack) {
            String lp = tr("whathitme.pack.mode.pack");
            String lm = tr("whathitme.pack.mode.personal");
            x += ui.tab("mode:pack", lp, x, y, ui.width(lp) + 10, !personal, () -> switchMode(false)) + GAP;
            x += ui.tab("mode:personal", lm, x, y, ui.width(lm) + 10, personal, () -> switchMode(true)) + GAP;
        }
        String title = Fmt.GOLD + tr(personal ? "whathitme.pack.title.personal" : "whathitme.pack.title");
        if (!model.editors.isEmpty()) title += Fmt.GRAY + "  " + tr("whathitme.pack.others", String.join(", ", model.editors));
        int tw = Math.min(ui.width(title), w - x - M - 4);
        if (tw > 30) ui.text(title, w - M - tw, y + 3, tw, 0xFFFFFF);
        y += Ui.BH + 3;
        List<String> ids = Arrays.asList(tabs());
        List<String> labels = new ArrayList<>();
        for (String t : ids) labels.add(tr("whathitme.pack.tab." + t));
        y = ui.tabs("tab:", ids, labels, ids.indexOf(tab), M, y, w - 2 * M, i -> selectTab(ids.get(i)));
        int bodyTop = y + 1;
        // bottom bar
        int btnY = h - Ui.BH - 3;
        int statusY = btnY - 11;
        int bodyBottom = statusY - 3;
        if (!loaded || denied) {
            String msg = denied ? tr("whathitme.error.noperm") : tr("whathitme.pack.loading");
            ui.text(Fmt.GRAY + msg, (w - ui.width(msg)) / 2, (bodyTop + bodyBottom) / 2, 0xFFFFFF);
        } else if ("settings".equals(tab)) {
            renderSettings(bodyTop, bodyBottom, w);
        } else {
            renderPanes(bodyTop, bodyBottom, w);
        }
        // status line
        String st;
        if (!status.isEmpty() && System.currentTimeMillis() - statusAt < 8000) st = (statusOk ? Fmt.GREEN : Fmt.RED) + status;
        else if (!banner.isEmpty()) st = Fmt.RED + banner;
        else st = Fmt.DARK_GRAY + tr(personal ? "whathitme.pack.hint.personal" : "whathitme.pack.hint");
        ui.text(st, M, statusY, w - 2 * M, 0xFFFFFF);
        // bottom buttons, right aligned
        List<String[]> btns = new ArrayList<>();
        btns.add(new String[] {"undo", tr("whathitme.pack.undo"), String.valueOf(model.canUndo)});
        if (!"settings".equals(tab)) btns.add(new String[] {"preview", tr(showPreview ? "whathitme.pack.preview.hide" : "whathitme.pack.preview.show"), "true"});
        btns.add(new String[] {"close", tr("whathitme.pack.close"), "true"});
        int bx = w - M;
        for (int i = btns.size() - 1; i >= 0; i--) {
            String[] b = btns.get(i);
            int bw = ui.buttonWidth(b[1]);
            bx -= bw;
            final String id = b[0];
            ui.button(id, b[1], bx, btnY, bw, Boolean.parseBoolean(b[2]), () -> bottom(id));
            bx -= GAP;
        }
        if (dialog != null) {
            ui.clearAreas();
            // raised layer: 1.20 - 1.21.5 draw text slightly in front, so later fills at the same depth do not cover it
            g.push();
            g.translate(0, 0, 200f);
            g.fill(0, 0, w, h, 0x90000000);
            dialog.render(w, h);
            g.pop();
        }
        if (tip != null && !tip.isEmpty()) g.tooltip(tip, mx, my, w, h);
    }

    private void bottom(String id) {
        switch (id) {
            case "undo":
                undo();
                break;
            case "preview":
                showPreview = !showPreview;
                if (showPreview) autoPreview();
                break;
            default:
                if (confirmDraft()) close = true;
        }
    }

    private void tooltip(String text) {
        if (text == null || text.isEmpty()) return;
        tip = new ArrayList<>(LegacyText.wrap(ui.g, text, Math.max(80, Math.min(220, lastW - 40))));
    }

    /** Small button with a hover description */
    private int small(String id, String label, String help, int x, int y, boolean enabled, Runnable r) {
        int bw = ui.buttonWidth(label);
        ui.button(id, label, x, y, bw, enabled, r);
        if (ui.hover(x, y, x + bw, y + Ui.BH)) tooltip(help);
        return bw;
    }

    // panes

    private void renderPanes(int top, int bottom, int w) {
        boolean wide = w >= WIDE;
        int avail = w - 2 * M;
        if (personal) {
            if (wide) {
                int listW = Math.max(130, Math.min(240, avail * 40 / 100));
                renderList(M, top, M + listW, bottom, false);
                renderDetail(M + listW + GAP, top, w - M, bottom, false);
            } else if (pane == 2 && draft != null) renderDetail(M, top, w - M, bottom, true);
            else renderList(M, top, w - M, bottom, false);
            return;
        }
        if (wide) {
            int treeW = Math.max(100, Math.min(170, avail * 26 / 100));
            int listW = Math.max(110, Math.min(230, avail * 30 / 100));
            renderTree(M, top, M + treeW, bottom);
            renderList(M + treeW + GAP, top, M + treeW + GAP + listW, bottom, false);
            renderDetail(M + treeW + GAP + listW + GAP, top, w - M, bottom, false);
            return;
        }
        if (pane == 2 && draft != null) renderDetail(M, top, w - M, bottom, true);
        else if (pane >= 1 && selectedCat() != null) renderList(M, top, w - M, bottom, true);
        else renderTree(M, top, w - M, bottom);
    }

    /** Header of a pane: title, optional back button; returns the y below */
    private int header(String title, int x0, int y, int x1, boolean back, Runnable onBack) {
        int x = x0 + 3;
        if (back) {
            x += ui.button("back", "<", x0 + 2, y + 1, 14, true, onBack) + 3;
        }
        ui.text(Fmt.GOLD + title, x, y + 3, x1 - x - 3, 0xFFFFFF);
        return y + Ui.BH + 3;
    }

    private void renderTree(int x0, int top, int x1, int bottom) {
        ui.panel(x0, top, x1, bottom);
        int y = header(tr("whathitme.pack.categories"), x0, top + 1, x1, false, null);
        // buttons at the bottom, wrapped
        String[][] b = {
            {"cat.new", "+", tr("whathitme.pack.btn.newcat")},
            {"dir.new", "+□", tr("whathitme.pack.btn.newdir")},
            {"node.rename", "✎", tr("whathitme.pack.btn.rename")},
            {"node.move", "⇨", tr("whathitme.pack.btn.move")},
            {"node.up", "↑", tr("whathitme.pack.btn.up")},
            {"node.down", "↓", tr("whathitme.pack.btn.down")},
            {"cat.memo", "M", tr("whathitme.pack.btn.memo")},
            {"node.delete", "✕", tr("whathitme.pack.btn.delete")}};
        Cat c = selectedCat();
        boolean nodeSel = selNode != null && (c == null || !c.script);
        boolean[] en = {true, true, nodeSel, nodeSel, nodeSel, nodeSel, c != null && !c.readOnly(), nodeSel};
        int per = Math.max(1, (x1 - x0 - 4) / 19);
        int rows = (b.length + per - 1) / per;
        int by = bottom - 2 - rows * (Ui.BH + 1);
        for (int i = 0; i < b.length; i++) {
            int bx = x0 + 2 + (i % per) * 19;
            int byy = by + (i / per) * (Ui.BH + 1);
            final String id = b[i][0];
            ui.button(id, b[i][1], bx, byy, 18, en[i], () -> treeAction(id));
            if (ui.hover(bx, byy, bx + 18, byy + Ui.BH)) tooltip(b[i][2]);
        }
        int listBottom = by - 2;
        // rows
        List<Object[]> rowsList = new ArrayList<>();
        for (Node n : tree) rowsList.add(new Object[] {n});
        boolean header = false;
        for (Cat s : model.cats) {
            if (!s.script) continue;
            if (!header) {
                rowsList.add(new Object[] {"scripts"});
                header = true;
            }
            rowsList.add(new Object[] {new NodeRef(s)});
        }
        int fit = Math.max(1, (listBottom - y) / Ui.ROW);
        treeScroll = Math.max(0, Math.min(treeScroll, Math.max(0, rowsList.size() - fit)));
        ui.onWheel(x0, y, x1, listBottom, d -> treeScroll = Math.max(0, Math.min(Math.max(0, rowsList.size() - fit), treeScroll + d)));
        String section = section(tab);
        int ry = y;
        for (int i = treeScroll; i < rowsList.size() && i < treeScroll + fit; i++) {
            Object r = rowsList.get(i)[0];
            if ("scripts".equals(r)) {
                ui.text(Fmt.LIGHT_PURPLE + tr("whathitme.pack.scripts"), x0 + 4, ry + 2, x1 - x0 - 8, 0xFFFFFF);
                ry += Ui.ROW;
                continue;
            }
            Node n = r instanceof Node ? (Node) r : null;
            Cat cat = n != null ? n.cat : ((NodeRef) r).cat;
            String id = cat != null ? "cat:" + cat.path : n.id();
            int indent = x0 + 3 + (n == null ? 4 : n.depth * 8);
            boolean sel = id.equals(selNode);
            if (sel) ui.fill(x0 + 1, ry, x1 - 3, ry + Ui.ROW, 0x5055FFFF);
            else if (ui.hover(x0 + 1, ry, x1 - 3, ry + Ui.ROW)) ui.fill(x0 + 1, ry, x1 - 3, ry + Ui.ROW, 0x30FFFFFF);
            final String fid = id;
            ui.onClick("node:" + id, x0 + 1, ry, x1 - 3, ry + Ui.ROW, () -> selectNode(fid));
            if (cat == null) {
                ui.text(Fmt.GOLD + "▾ " + n.folder.substring(n.folder.lastIndexOf('/') + 1) + "/", indent, ry + 2, x1 - indent - 4, 0xFFFFFF);
            } else {
                int tx = indent;
                if (!cat.script && cat.content != null) {
                    final Cat fc = cat;
                    ui.checkbox("enable:" + cat.path, "", tx, ry, 12, cat.enabled, true, () -> setEnabled(fc.path, !fc.enabled));
                    if (ui.hover(tx, ry, tx + 11, ry + 10)) tooltip(tr("whathitme.pack.enabled.tip"));
                    tx += 13;
                }
                int n0 = PackModel.count(cat, section);
                String num = cat.loadPos > 0 ? Fmt.DARK_GRAY + "#" + cat.loadPos + " " : "";
                String color = cat.error != null ? Fmt.RED.toString() : cat.script ? Fmt.LIGHT_PURPLE.toString()
                    : !cat.enabled ? Fmt.DARK_GRAY.toString() : n0 == 0 ? Fmt.GRAY.toString() : Fmt.WHITE.toString();
                String label = num + color + (cat.script ? cat.path : cat.name()) + (cat.error != null ? " !" : "") + (n0 > 0 ? Fmt.DARK_GRAY + " (" + n0 + ")" : "")
                    + (cat.memo.isEmpty() ? "" : Fmt.DARK_AQUA + " ✎");
                ui.text(label, tx, ry + 2, x1 - tx - 5, 0xFFFFFF);
                if (ui.hover(x0 + 1, ry, x1 - 3, ry + Ui.ROW) && (cat.error != null || !cat.memo.isEmpty())) {
                    tooltip(cat.error != null ? Fmt.RED + cat.error : cat.memo);
                }
            }
            ry += Ui.ROW;
        }
        ui.scrollBar(x1 - 3, y, listBottom, treeScroll, fit, rowsList.size());
    }

    /** A script source in the tree */
    private static final class NodeRef {
        final Cat cat;

        NodeRef(Cat cat) {
            this.cat = cat;
        }
    }

    private void treeAction(String id) {
        Cat c = selectedCat();
        String folder = selectedFolder();
        switch (id) {
            case "cat.new":
                dialog = new PromptDialog(tr("whathitme.pack.dlg.newcat"), tr("whathitme.pack.dlg.newcat.label", folder.isEmpty() ? "pack/" : "pack/" + folder + "/"), "",
                    s -> createCategory(folder, s));
                break;
            case "dir.new":
                dialog = new PromptDialog(tr("whathitme.pack.dlg.newdir"), tr("whathitme.pack.dlg.newdir.label", folder.isEmpty() ? "pack/" : "pack/" + folder + "/"), "",
                    s -> createFolder(folder, s));
                break;
            case "node.rename": {
                String cur = c != null ? c.name() : selNode != null && selNode.startsWith("dir:") ? selNode.substring(selNode.lastIndexOf('/') + 1).replace("dir:", "") : "";
                dialog = new PromptDialog(tr("whathitme.pack.dlg.rename"), tr("whathitme.pack.dlg.rename.label"), cur, this::renameSelected);
                break;
            }
            case "node.move": {
                List<String> opts = new ArrayList<>();
                opts.add("");
                for (String d : model.dirs) if (selNode == null || !selNode.startsWith("dir:") || !(d + "/").startsWith(selNode.substring(4) + "/")) opts.add(d);
                List<String> labels = new ArrayList<>();
                for (String o : opts) labels.add(o.isEmpty() ? tr("whathitme.pack.root") : o + "/");
                dialog = new ChoiceDialog(tr("whathitme.pack.dlg.movefolder"), labels, null, i -> moveSelectedTo(opts.get(i)));
                break;
            }
            case "node.up":
                moveSelected(-1);
                break;
            case "node.down":
                moveSelected(1);
                break;
            case "cat.memo":
                if (c != null) {
                    final String path = c.path;
                    dialog = new PromptDialog(tr("whathitme.pack.dlg.memo", c.name()), tr("whathitme.pack.dlg.memo.label"), c.memo, s -> setCategoryMemo(path, s));
                    ((PromptDialog) dialog).allowEmpty = true;
                }
                break;
            case "node.delete": {
                String what = c != null ? c.path : selNode != null ? selNode.substring(4) + "/" : "";
                dialog = new ConfirmDialog(tr("whathitme.pack.dlg.delete"), tr(c != null ? "whathitme.pack.dlg.delete.cat" : "whathitme.pack.dlg.delete.dir", what),
                    this::deleteSelected);
                break;
            }
            default:
                break;
        }
    }

    private List<Ent> filtered(List<Ent> all) {
        String q = search.text.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return all;
        List<Ent> out = new ArrayList<>();
        for (Ent e : all) if (label(e).toLowerCase(Locale.ROOT).contains(q) || e.memo.toLowerCase(Locale.ROOT).contains(q)) out.add(e);
        return out;
    }

    /** One-line text of an entry in the list */
    String label(Ent e) {
        switch (e.section) {
            case "rules": {
                Map<String, Object> o = Json.obj(e.value);
                String s = PackModel.str(o.get("match"));
                int n = Json.list(o.get("if")).size() + Json.list(o.get("any")).size() + Json.list(o.get("unless")).size();
                if (n > 0) s += " [" + n + "?]";
                if (o.containsKey("name")) s += " → " + PackModel.str(o.get("name"));
                return s;
            }
            case "deathTriggers": {
                Map<String, Object> o = Json.obj(e.value);
                return PackModel.str(o.get("match")) + " → /" + PackModel.str(o.get("command"));
            }
            case "templates":
                return e.key + " (" + Json.list(e.value).size() + ")";
            default:
                return PackModel.isMap(e.section) ? e.key + " = " + PackModel.str(e.value) : e.key;
        }
    }

    private void renderList(int x0, int top, int x1, int bottom, boolean back) {
        ui.panel(x0, top, x1, bottom);
        Cat c = selectedCat();
        String title = personal ? tr("whathitme.pack.personal.title") : c == null ? tr("whathitme.pack.nocat") : (c.script ? c.path : c.name());
        int y = header(title, x0, top + 1, x1, back && !personal, () -> pane = 0);
        if (c == null) {
            for (String l : LegacyText.wrap(ui.g, Fmt.GRAY + tr("whathitme.pack.nocat.hint"), x1 - x0 - 8)) {
                ui.text(l, x0 + 4, y, 0xFFFFFF);
                y += 10;
            }
            return;
        }
        if (!c.memo.isEmpty()) {
            ui.text(Fmt.DARK_AQUA + "✎ " + c.memo, x0 + 4, y, x1 - x0 - 8, 0xFFFFFF);
            if (ui.hover(x0, y, x1, y + 9)) tooltip(c.memo);
            y += 10;
        }
        if (c.error != null) {
            for (String l : LegacyText.wrap(ui.g, Fmt.RED + tr("whathitme.pack.fileerror", c.error), x1 - x0 - 8, 3)) {
                ui.text(l, x0 + 4, y, 0xFFFFFF);
                y += 10;
            }
        } else if (c.script) {
            ui.text(Fmt.LIGHT_PURPLE + tr("whathitme.pack.readonly.script"), x0 + 4, y, x1 - x0 - 8, 0xFFFFFF);
            y += 10;
        } else if (!c.enabled) {
            ui.text(Fmt.GRAY + tr("whathitme.pack.disabled"), x0 + 4, y, x1 - x0 - 8, 0xFFFFFF);
            y += 10;
        }
        ui.field("search", search, x0 + 3, y, x1 - x0 - 6, focused == search, blink, null, () -> focused = search);
        y += Ui.BH + 2;
        boolean canAdd = !c.readOnly();
        boolean listSec = !PackModel.isMap(section(tab)) || "renameMap".equals(section(tab)) || "hintMap".equals(section(tab));
        int by = bottom - Ui.BH - 2;
        int bx = x0 + 3;
        bx += small("add", tr("whathitme.pack.add"), tr("whathitme.pack.add.tip"), bx, by, canAdd, this::newEntry) + GAP;
        boolean entrySel = draft != null && draft.orig != null && !draft.readOnly();
        if (listSec) {
            bx += small("entry.up", "↑", tr("whathitme.pack.btn.entryup"), bx, by, entrySel, () -> moveEntry(-1)) + GAP;
            small("entry.down", "↓", tr("whathitme.pack.btn.entrydown"), bx, by, entrySel, () -> moveEntry(1));
        }
        int listBottom = by - 2;
        List<Ent> list = filtered(entries());
        int fit = Math.max(1, (listBottom - y) / Ui.ROW);
        listScroll = Math.max(0, Math.min(listScroll, Math.max(0, list.size() - fit)));
        ui.onWheel(x0, y, x1, listBottom, d -> listScroll = Math.max(0, Math.min(Math.max(0, list.size() - fit), listScroll + d)));
        if (list.isEmpty() && (draft == null || draft.orig != null)) ui.text(Fmt.DARK_GRAY + tr(search.text.isEmpty() ? "whathitme.pack.empty" : "whathitme.pack.nomatch"), x0 + 5, y + 2, x1 - x0 - 10, 0xFFFFFF);
        int ry = y;
        for (int i = listScroll; i < list.size() && i < listScroll + fit; i++) {
            Ent e = list.get(i);
            boolean sel = draft != null && draft.orig != null && same(draft.orig, e);
            boolean over = ui.hover(x0 + 1, ry, x1 - 3, ry + Ui.ROW);
            if (sel) ui.fill(x0 + 1, ry, x1 - 3, ry + Ui.ROW, 0x5055FFFF);
            else if (over) ui.fill(x0 + 1, ry, x1 - 3, ry + Ui.ROW, 0x30FFFFFF);
            String text = (c.script ? Fmt.LIGHT_PURPLE : Fmt.WHITE) + label(e).replace('§', '&') + (e.memo.isEmpty() ? "" : Fmt.DARK_AQUA + " ✎");
            ui.text(text, x0 + 4, ry + 2, x1 - x0 - 10, 0xFFFFFF);
            final Ent fe = e;
            ui.onClick("entry:" + i, x0 + 1, ry, x1 - 3, ry + Ui.ROW, () -> selectEntry(fe));
            if (over) tooltip(label(e).replace('§', '&') + (e.memo.isEmpty() ? "" : "\n" + Fmt.DARK_AQUA + e.memo));
            ry += Ui.ROW;
        }
        if (draft != null && draft.orig == null) {
            if (ry + Ui.ROW <= listBottom) {
                ui.fill(x0 + 1, ry, x1 - 3, ry + Ui.ROW, 0x5055FF55);
                ui.text(Fmt.GREEN + tr("whathitme.pack.newentry"), x0 + 4, ry + 2, x1 - x0 - 10, 0xFFFFFF);
            }
        }
        ui.scrollBar(x1 - 3, y, listBottom, listScroll, fit, list.size());
    }

    // detail form

    private int fy;
    private int fTop;
    private int fBottom;
    private int fx;
    private int fw;
    private int fTotal;

    private boolean fits(int h) {
        return fy >= fTop && fy + h <= fBottom;
    }

    private void fLabel(String s) {
        if (fits(10)) ui.text(s, fx, fy + 1, fw, 0xFFFFFF);
        fy += 10;
    }

    private void fWrap(String s) {
        for (String l : LegacyText.wrap(ui.g, s, fw)) fLabel(l);
    }

    private void fField(String id, TextField f, boolean vars, boolean ro, String helper, String helpTip, Runnable onHelper) {
        int hw = helper == null ? 0 : ui.buttonWidth(helper) + GAP;
        if (fits(Ui.BH)) {
            f.editable = !ro;
            ui.field(id, f, fx, fy, fw - hw, focused == f, blink, vars ? TemplateVars::isKnown : null, () -> focused = f);
            shownFields.add(f);
            if (helper != null) small(id + ".helper", helper, helpTip, fx + fw - hw + GAP, fy, !ro, onHelper);
        }
        fy += Ui.BH + 2;
    }

    private void fProblem(String p) {
        if (p == null) return;
        boolean err = p.startsWith("!");
        fWrap((err ? Fmt.RED : Fmt.GOLD) + (err ? p.substring(1) : p));
    }

    private void fConds(String list, String label, List<TextField> fields, boolean ro) {
        fLabel(Fmt.GRAY + label);
        for (int i = 0; i < fields.size(); i++) {
            TextField f = fields.get(i);
            final int idx = i;
            if (fits(Ui.BH)) {
                f.editable = !ro;
                ui.field("cond:" + list + ":" + i, f, fx, fy, fw - 16, focused == f, blink, null, () -> focused = f);
                shownFields.add(f);
                ui.button("conddel:" + list + ":" + i, "✕", fx + fw - 14, fy, 14, !ro, () -> {
                    fields.remove(idx);
                    if (focused == f) focused = null;
                });
            }
            fy += Ui.BH + 2;
            fProblem(condProblem(f.text));
        }
        if (fits(Ui.BH)) {
            String l = tr("whathitme.pack.addcond");
            ui.button("condadd:" + list, l, fx, fy, Math.min(fw / 2, ui.buttonWidth(l)), !ro, () -> dialog = new CondDialog(s -> fields.add(new TextField(s))));
            String e = tr("whathitme.pack.addcond.free");
            ui.button("condfree:" + list, e, fx + Math.min(fw / 2, ui.buttonWidth(l)) + GAP, fy, Math.min(fw / 2 - GAP, ui.buttonWidth(e)), !ro, () -> {
                TextField nf = new TextField("");
                fields.add(nf);
                focused = nf;
            });
        }
        fy += Ui.BH + 3;
    }

    private void renderDetail(int x0, int top, int x1, int bottom, boolean back) {
        ui.panel(x0, top, x1, bottom);
        String section = section(tab);
        if (draft == null) {
            int y = header(tr("whathitme.pack.detail"), x0, top + 1, x1, back, () -> pane = 1);
            String help = tr("whathitme.pack.help." + tab);
            for (String l : LegacyText.wrap(ui.g, Fmt.GRAY + help, x1 - x0 - 8)) {
                if (y + 10 > bottom) break;
                ui.text(l, x0 + 4, y, 0xFFFFFF);
                y += 10;
            }
            return;
        }
        boolean ro = draft.readOnly();
        String title = draft.orig == null ? tr("whathitme.pack.newentry") : tr("whathitme.pack.tab." + tab);
        int y = header(title + (ro ? Fmt.LIGHT_PURPLE + " (" + tr("whathitme.pack.readonly") + ")" : draft.dirty() ? Fmt.YELLOW + " *" : ""), x0, top + 1, x1, back, () -> {
            if (confirmDraft()) pane = 1;
        });
        // buttons at the bottom
        List<String[]> btns = new ArrayList<>();
        btns.add(new String[] {"done", tr("whathitme.pack.done"), String.valueOf(!ro && draft.dirty())});
        btns.add(new String[] {"revert", tr("whathitme.pack.revert"), String.valueOf(!ro && draft.dirty() && draft.orig != null)});
        btns.add(new String[] {"delete", tr("whathitme.pack.delete"), String.valueOf(!ro && draft.orig != null)});
        if (!personal) btns.add(new String[] {"transfer", tr("whathitme.pack.transfer"), String.valueOf(!ro && draft.orig != null)});
        int bx = x0 + 3;
        int rowsB = 1;
        for (String[] b : btns) {
            int bw = ui.buttonWidth(b[1]);
            if (bx > x0 + 3 && bx + bw > x1 - 3) {
                bx = x0 + 3;
                rowsB++;
            }
            bx += bw + GAP;
        }
        int by = bottom - 2 - rowsB * (Ui.BH + 1);
        bx = x0 + 3;
        int row = 0;
        for (String[] b : btns) {
            int bw = ui.buttonWidth(b[1]);
            if (bx > x0 + 3 && bx + bw > x1 - 3) {
                bx = x0 + 3;
                row++;
            }
            final String id = b[0];
            ui.button("d." + id, b[1], bx, by + row * (Ui.BH + 1), bw, Boolean.parseBoolean(b[2]), () -> detailAction(id));
            bx += bw + GAP;
        }
        int formBottom = by - 2;
        // preview area
        boolean previewable = !testArgs().isEmpty() || "templates".equals(tab);
        if (showPreview && previewable && !"templates".equals(tab)) {
            int ph = Math.max(40, (formBottom - y) * 42 / 100);
            renderPreview(x0 + 2, formBottom - ph, x1 - 2, formBottom);
            formBottom -= ph + 2;
        }
        // form, scrollable
        fx = x0 + 4;
        fw = x1 - x0 - 10;
        fTop = y;
        fBottom = formBottom;
        fy = y - formScroll;
        int startY = fy;
        renderForm(section, ro);
        fTotal = fy - startY;
        int visible = fBottom - fTop;
        formRows = visible;
        int maxScroll = Math.max(0, fTotal - visible);
        formScroll = Math.max(0, Math.min(formScroll, maxScroll));
        ui.onWheel(x0, fTop, x1, fBottom, d -> formScroll = Math.max(0, Math.min(Math.max(0, fTotal - (fBottom - fTop)), formScroll + d * 12)));
        if (maxScroll > 0) ui.scrollBar(x1 - 3, fTop, fBottom, formScroll, visible, fTotal);
    }

    private void renderForm(String section, boolean ro) {
        Draft d = draft;
        if (!banner.isEmpty()) fWrap(Fmt.RED + banner);
        switch (section) {
            case "renameMap":
            case "hintMap":
            case "deathMessages": {
                fLabel(Fmt.GRAY + tr("whathitme.pack.f.key"));
                fField("f.key", d.key, false, ro, tr("whathitme.pack.builder"), tr("whathitme.pack.builder.tip"), () -> dialog = new KeyDialog(d.key.text, s -> d.key.text = s));
                fProblem(keyProblem(d.key.text));
                fLabel(Fmt.GRAY + tr("whathitme.pack.f.value." + section));
                fField("f.value", d.value, true, ro, null, null, null);
                fProblem(varProblem(d.value.text));
                if (d.value.text.isEmpty() && !d.key.text.isEmpty()) fWrap(Fmt.DARK_GRAY + tr("whathitme.pack.f.emptyvalue"));
                break;
            }
            case "templates": {
                fLabel(Fmt.GRAY + tr("whathitme.pack.f.output"));
                if (fits(Ui.BH)) {
                    String cur = d.key.text;
                    String label = cur.isEmpty() ? "-" : cur + " (" + tr("whathitme.editor.out." + cur) + ")";
                    ui.button("f.output", label + " ▾", fx, fy, Math.min(fw, ui.buttonWidth(label + " ▾")), !ro, () -> {
                        List<String> labels = new ArrayList<>();
                        for (String o : Template.OUTPUTS) labels.add(o + " (" + tr("whathitme.editor.out." + o) + ")");
                        dialog = new ChoiceDialog(tr("whathitme.pack.f.output"), labels, null, i -> d.key.text = Template.OUTPUTS[i]);
                    });
                }
                fy += Ui.BH + 2;
                fLabel(Fmt.GRAY + tr("whathitme.pack.f.lines", d.lines.size()));
                for (String l : d.lines) {
                    if (fits(10)) ui.text(Fmt.WHITE + l.replace('§', '&'), fx + 4, fy + 1, fw - 4, 0xFFFFFF);
                    fy += 10;
                }
                if (fits(Ui.BH)) {
                    String l = tr("whathitme.pack.editlines");
                    ui.button("f.editlines", l, fx, fy, Math.min(fw, ui.buttonWidth(l)), !ro, this::openTemplateLines);
                }
                fy += Ui.BH + 3;
                break;
            }
            case "rules":
            case "deathTriggers": {
                fLabel(Fmt.GRAY + tr("whathitme.pack.f.match"));
                fField("f.key", d.key, false, ro, tr("whathitme.pack.builder"), tr("whathitme.pack.builder.tip"), () -> dialog = new KeyDialog(d.key.text, s -> d.key.text = s));
                fProblem(keyProblem(d.key.text));
                if ("deathTriggers".equals(section)) {
                    fLabel(Fmt.GRAY + tr("whathitme.pack.f.command"));
                    fField("f.command", d.command, true, ro, null, null, null);
                }
                fConds("if", tr("whathitme.pack.f.if"), d.ifs, ro);
                fConds("any", tr("whathitme.pack.f.any"), d.anys, ro);
                fConds("unless", tr("whathitme.pack.f.unless"), d.unlesses, ro);
                if ("rules".equals(section)) {
                    fLabel(Fmt.GRAY + tr("whathitme.pack.f.name"));
                    fField("f.name", d.name, true, ro, null, null, null);
                    fProblem(varProblem(d.name.text));
                    fLabel(Fmt.GRAY + tr("whathitme.pack.f.hint"));
                    if (!d.noTip) {
                        fField("f.hint", d.hint, true, ro, null, null, null);
                        fProblem(varProblem(d.hint.text));
                    }
                    if (fits(11)) ui.checkbox("f.notip", tr("whathitme.pack.f.notip"), fx, fy, fw, d.noTip, !ro, () -> d.noTip = !d.noTip);
                    fy += 12;
                    fLabel(Fmt.GRAY + tr("whathitme.pack.f.hide"));
                    List<String> names = model.hide.isEmpty() ? Arrays.asList(io.github.junjun1z.whathitme.core.data.RuleMark.HIDE_NAMES) : model.hide;
                    int colW = 0;
                    for (String n : names) colW = Math.max(colW, ui.width(n) + 18);
                    int cols = Math.max(1, fw / Math.max(1, colW));
                    for (int i = 0; i < names.size(); i += cols) {
                        if (fits(11)) {
                            for (int k = 0; k < cols && i + k < names.size(); k++) {
                                String n = names.get(i + k);
                                ui.checkbox("f.hide:" + n, n, fx + k * colW, fy, colW, d.hide.contains(n), !ro, () -> setHide(n, !d.hide.contains(n)));
                            }
                        }
                        fy += 12;
                    }
                    fLabel(Fmt.GRAY + tr("whathitme.pack.f.enforce"));
                    if (fits(Ui.BH)) {
                        String[] opts = {tr("whathitme.pack.f.enforce.auto"), tr("whathitme.pack.f.enforce.on"), tr("whathitme.pack.f.enforce.off")};
                        int ex = fx;
                        int total = ui.width(opts[0]) + ui.width(opts[1]) + ui.width(opts[2]) + 30 + 2 * GAP;
                        for (int i = 0; i < 3; i++) {
                            final int v = i;
                            int bw = total <= fw ? ui.width(opts[i]) + 10 : (fw - 2 * GAP) / 3;
                            ui.tab("f.enforce:" + i, opts[i], ex, fy, bw, d.enforce == i, () -> {
                                if (!ro) d.enforce = v;
                            });
                            ex += bw + GAP;
                        }
                    }
                    fy += Ui.BH + 3;
                    fLabel(Fmt.GRAY + tr("whathitme.pack.f.commands"));
                    for (int i = 0; i < d.commands.size(); i++) {
                        TextField f = d.commands.get(i);
                        final int idx = i;
                        if (fits(Ui.BH)) {
                            f.editable = !ro;
                            ui.field("f.cmd:" + i, f, fx, fy, fw - 16, focused == f, blink, TemplateVars::isKnown, () -> focused = f);
                            shownFields.add(f);
                            ui.button("f.cmddel:" + i, "✕", fx + fw - 14, fy, 14, !ro, () -> d.commands.remove(idx));
                        }
                        fy += Ui.BH + 2;
                    }
                    if (fits(Ui.BH)) {
                        String l = tr("whathitme.pack.addcmd");
                        ui.button("f.cmdadd", l, fx, fy, Math.min(fw, ui.buttonWidth(l)), !ro, () -> {
                            TextField nf = new TextField("");
                            d.commands.add(nf);
                            focused = nf;
                        });
                    }
                    fy += Ui.BH + 3;
                }
                if (fits(11)) ui.checkbox("f.once", tr("whathitme.pack.f.once"), fx, fy, fw, d.once, !ro, () -> d.once = !d.once);
                fy += 12;
                fLabel(Fmt.GRAY + tr("whathitme.pack.f.cooldown"));
                fField("f.cooldown", d.cooldown, false, ro, null, null, null);
                break;
            }
            default: {
                boolean locked = "lockedFeatures".equals(section);
                fLabel(Fmt.GRAY + tr(locked ? "whathitme.pack.f.lockkey" : "whathitme.pack.f.ignoreid"));
                fField("f.key", d.key, false, ro, tr(locked ? "whathitme.pack.pick" : "whathitme.pack.builder"), tr(locked ? "whathitme.pack.pick.tip" : "whathitme.pack.builder.tip"),
                    () -> {
                        if (locked) dialog = new ChoiceDialog(tr("whathitme.pack.f.lockkey"), lockable(), null, i -> d.key.text = lockable().get(i));
                        else dialog = new KeyDialog(d.key.text, s -> d.key.text = s);
                    });
                if (!locked) fProblem(keyProblem(d.key.text));
                else if (!lockable().contains(d.key.text.trim()) && !d.key.text.trim().isEmpty()) fProblem(tr("whathitme.pack.warn.lockkey", d.key.text.trim()));
            }
        }
        if (!personal) {
            fLabel(Fmt.GRAY + tr("whathitme.pack.f.memo"));
            fField("f.memo", d.memo, false, ro, null, null, null);
        }
        for (String w : serverWarnings()) fWrap(Fmt.GOLD + tr("whathitme.pack.server") + " " + w);
        String err = draftError();
        if (err != null && d.dirty()) fWrap(Fmt.RED + err);
    }

    /** Keys pack makers may lock (lockedFeatures) */
    private List<String> lockable() {
        Set<String> out = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        if (io.github.junjun1z.whathitme.core.config.WhmConfig.client != null) out.addAll(io.github.junjun1z.whathitme.core.config.WhmConfig.client.keys());
        out.add("templates");
        for (String o : Template.OUTPUTS) out.add("templates." + o);
        return new ArrayList<>(out);
    }

    private void openTemplateLines() {
        if (draft == null) return;
        final Draft d = draft;
        String out = d.key.text.trim().toLowerCase(Locale.ROOT);
        if (!Template.isOutput(out)) {
            flash(tr("whathitme.pack.err.output", out), false);
            return;
        }
        child = new TemplateEditor(out, d.lines, lines -> {
            d.lines = new ArrayList<>(lines);
            if (draft == d) done();
        });
    }

    private void detailAction(String id) {
        switch (id) {
            case "done":
                done();
                break;
            case "revert":
                revert();
                break;
            case "delete": {
                dialog = new ConfirmDialog(tr("whathitme.pack.dlg.delentry"), tr("whathitme.pack.dlg.delentry.text", draft == null || draft.orig == null ? "" : label(draft.orig)), this::deleteEntry);
                break;
            }
            case "transfer": {
                List<String> paths = new ArrayList<>();
                List<String> labels = new ArrayList<>();
                for (Cat c : model.files()) {
                    if (c.readOnly() || draft == null || draft.orig == null || c.path.equals(draft.orig.cat.path)) continue;
                    paths.add(c.path);
                    labels.add(c.path);
                }
                dialog = new ChoiceDialog(tr("whathitme.pack.dlg.transfer"), labels, null, i -> transferEntry(paths.get(i)));
                break;
            }
            default:
                break;
        }
    }

    private void renderPreview(int x0, int y0, int x1, int y1) {
        ui.fill(x0, y0, x1, y1, 0x60000000);
        int y = y0 + 2;
        int x = x0 + 2;
        if (!personal) {
            String as = tr("whathitme.pack.preview.as");
            ui.text(Fmt.GRAY + as, x, y + 3, 0xFFFFFF);
            x += ui.width(as) + 3;
            String who = previewAs.isEmpty() ? "-" : previewAs;
            int bw = Math.min(80, ui.buttonWidth(who + " ▾"));
            ui.button("p.as", who + " ▾", x, y, bw, !model.players.isEmpty(), () -> dialog = new ChoiceDialog(tr("whathitme.pack.preview.as"), model.players, null, i -> {
                previewAs = model.players.get(i);
                runPreview();
            }));
            x += bw + GAP;
            int rw = ui.buttonWidth(tr("whathitme.pack.preview.run"));
            int aw = Math.max(30, x1 - x - rw - 4);
            ui.field("p.assume", assume, x, y, aw, focused == assume, blink, null, () -> focused = assume);
            if (ui.hover(x, y, x + aw, y + Ui.BH)) tooltip(tr("whathitme.pack.assume.tip"));
            x += aw + GAP;
            ui.button("p.run", tr("whathitme.pack.preview.run"), x, y, rw, true, this::runPreview);
        } else {
            ui.button("p.run", tr("whathitme.pack.preview.run"), x, y, ui.buttonWidth(tr("whathitme.pack.preview.run")), true, this::runPreview);
        }
        y += Ui.BH + 2;
        List<String> wrapped = new ArrayList<>();
        for (String l : previewLines) wrapped.addAll(LegacyText.wrap(ui.g, l, x1 - x0 - 8));
        if (wrapped.isEmpty()) wrapped.add(Fmt.DARK_GRAY + tr("whathitme.pack.preview.empty"));
        int fit = Math.max(1, (y1 - y - 1) / 9);
        previewScroll = Math.max(0, Math.min(previewScroll, Math.max(0, wrapped.size() - fit)));
        final int total = wrapped.size();
        ui.onWheel(x0, y, x1, y1, dd -> previewScroll = Math.max(0, Math.min(Math.max(0, total - fit), previewScroll + dd)));
        for (int i = previewScroll; i < wrapped.size() && i < previewScroll + fit; i++) {
            ui.text(wrapped.get(i), x0 + 4, y, 0xFFFFFF);
            y += 9;
        }
        ui.scrollBar(x1 - 3, y1 - fit * 9 - 1, y1 - 1, previewScroll, fit, total);
    }

    // settings tab

    private void renderSettings(int top, int bottom, int w) {
        boolean wide = w >= WIDE;
        int avail = w - 2 * M;
        if (wide) {
            int listW = Math.max(150, Math.min(260, avail * 45 / 100));
            renderSettingList(M, top, M + listW, bottom);
            renderSettingDetail(M + listW + GAP, top, w - M, bottom, false);
        } else if (pane == 2 && settingKey != null) renderSettingDetail(M, top, w - M, bottom, true);
        else renderSettingList(M, top, w - M, bottom);
    }

    private void renderSettingList(int x0, int top, int x1, int bottom) {
        ui.panel(x0, top, x1, bottom);
        int y = header(tr("whathitme.pack.settings.title"), x0, top + 1, x1, false, null);
        ui.field("search", search, x0 + 3, y, x1 - x0 - 6, focused == search, blink, null, () -> focused = search);
        y += Ui.BH + 2;
        String q = search.text.trim().toLowerCase(Locale.ROOT);
        List<Object> rows = new ArrayList<>();
        String cat = null;
        for (Map<String, Object> s : model.settings) {
            String key = PackModel.str(s.get("key"));
            if (!q.isEmpty() && !key.toLowerCase(Locale.ROOT).contains(q)) continue;
            String c = PackModel.str(s.get("cat"));
            if (!c.equals(cat)) {
                rows.add("#" + c);
                cat = c;
            }
            rows.add(s);
        }
        int fit = Math.max(1, (bottom - 2 - y) / Ui.ROW);
        settingsScroll = Math.max(0, Math.min(settingsScroll, Math.max(0, rows.size() - fit)));
        ui.onWheel(x0, y, x1, bottom, d -> settingsScroll = Math.max(0, Math.min(Math.max(0, rows.size() - fit), settingsScroll + d)));
        int ry = y;
        for (int i = settingsScroll; i < rows.size() && i < settingsScroll + fit; i++) {
            Object r = rows.get(i);
            if (r instanceof String) {
                String c = ((String) r).substring(1);
                ui.text(Fmt.GOLD + tr("whathitme.config.cat." + c), x0 + 4, ry + 2, x1 - x0 - 8, 0xFFFFFF);
                ry += Ui.ROW;
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> s = (Map<String, Object>) r;
            String key = PackModel.str(s.get("key"));
            String type = PackModel.str(s.get("type"));
            String value = PackModel.str(s.get("value"));
            boolean sel = key.equals(settingKey);
            boolean over = ui.hover(x0 + 1, ry, x1 - 3, ry + Ui.ROW);
            if (sel) ui.fill(x0 + 1, ry, x1 - 3, ry + Ui.ROW, 0x5055FFFF);
            else if (over) ui.fill(x0 + 1, ry, x1 - 3, ry + Ui.ROW, 0x30FFFFFF);
            int vx = x1 - 6;
            if ("BOOLEAN".equals(type)) {
                boolean on = Boolean.parseBoolean(value);
                ui.checkbox("set.toggle:" + key, "", vx - 10, ry, 12, on, true, () -> applySetting(key, String.valueOf(!on)));
                vx -= 14;
            } else {
                String v = LegacyText.trim(ui.g, value.isEmpty() ? "-" : value, Math.max(20, (x1 - x0) / 3));
                ui.text(Fmt.AQUA + v, vx - ui.width(v), ry + 2, 0xFFFFFF);
                vx -= ui.width(v) + 4;
            }
            ui.text(Fmt.WHITE + key, x0 + 8, ry + 2, vx - x0 - 10, 0xFFFFFF);
            ui.onClick("setting:" + key, x0 + 1, ry, vx, ry + Ui.ROW, () -> selectSetting(key));
            if (over) tooltip(PackModel.str(s.get("comment")));
            ry += Ui.ROW;
        }
        ui.scrollBar(x1 - 3, y, bottom - 2, settingsScroll, fit, rows.size());
    }

    private void renderSettingDetail(int x0, int top, int x1, int bottom, boolean back) {
        ui.panel(x0, top, x1, bottom);
        Map<String, Object> s = setting(settingKey);
        int y = header(s == null ? tr("whathitme.pack.detail") : settingKey, x0, top + 1, x1, back, () -> pane = 1);
        if (s == null) {
            for (String l : LegacyText.wrap(ui.g, Fmt.GRAY + tr("whathitme.pack.help.settings"), x1 - x0 - 8)) {
                ui.text(l, x0 + 4, y, 0xFFFFFF);
                y += 10;
            }
            return;
        }
        int x = x0 + 4;
        int fw2 = x1 - x0 - 8;
        for (String l : LegacyText.wrap(ui.g, Fmt.GRAY + PackModel.str(s.get("comment")), fw2)) {
            if (y + 10 > bottom - 30) break;
            ui.text(l, x, y, 0xFFFFFF);
            y += 10;
        }
        y += 4;
        String type = PackModel.str(s.get("type"));
        String key = settingKey;
        if ("BOOLEAN".equals(type)) {
            boolean on = Boolean.parseBoolean(PackModel.str(s.get("value")));
            ui.checkbox("set.value", tr(on ? "whathitme.pack.on" : "whathitme.pack.off"), x, y, fw2, on, true, () -> applySetting(key, String.valueOf(!on)));
            y += 14;
        } else if (!Json.list(s.get("valid")).isEmpty()) {
            List<String> valid = new ArrayList<>();
            for (Object o : Json.list(s.get("valid"))) valid.add(PackModel.str(o));
            String cur = PackModel.str(s.get("value"));
            ui.button("set.choice", cur + " ▾", x, y, Math.min(fw2, ui.buttonWidth(cur + " ▾")), true,
                () -> dialog = new ChoiceDialog(key, valid, null, i -> applySetting(key, valid.get(i))));
            y += Ui.BH + 3;
        } else {
            int aw = ui.buttonWidth(tr("whathitme.pack.apply"));
            ui.field("set.field", settingField, x, y, fw2 - aw - GAP, focused == settingField, blink, null, () -> focused = settingField);
            ui.button("set.apply", tr("whathitme.pack.apply"), x + fw2 - aw, y, aw, true, () -> applySetting(key, settingField.text));
            y += Ui.BH + 3;
            if ("LIST".equals(type)) {
                for (String l : LegacyText.wrap(ui.g, Fmt.DARK_GRAY + tr("whathitme.pack.settings.list"), fw2)) {
                    ui.text(l, x, y, 0xFFFFFF);
                    y += 10;
                }
            }
        }
        String def = PackModel.str(s.get("def"));
        String range = "INT".equals(type) || "FLOAT".equals(type)
            ? tr("whathitme.pack.settings.range", PackModel.str(s.get("min")), PackModel.str(s.get("max")), def) : tr("whathitme.pack.settings.def", def.isEmpty() ? "-" : def);
        ui.text(Fmt.DARK_GRAY + range, x, y, fw2, 0xFFFFFF);
    }

    // ------------------------------------------------------------ dialogs

    /** A modal box in the middle of the screen */
    abstract class Dialog {
        final String title;
        TextField focus;
        int x0;
        int y0;
        int x1;
        int y1;

        Dialog(String title) {
            this.title = title;
        }

        /** Preferred size */
        int prefW() {
            return 300;
        }

        int prefH() {
            return 120;
        }

        void render(int w, int h) {
            int dw = Math.min(w - 8, prefW());
            int dh = Math.min(h - 8, prefH());
            x0 = (w - dw) / 2;
            y0 = (h - dh) / 2;
            x1 = x0 + dw;
            y1 = y0 + dh;
            ui.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, 0xFFC0A040);
            ui.fill(x0, y0, x1, y1, 0xF0181822);
            ui.text(Fmt.GOLD + title, x0 + 5, y0 + 4, dw - 10, 0xFFFFFF);
            ui.onClick("dlg.blocker", 0, 0, w, h, () -> {});
            ui.onClick("dlg.body", x0, y0, x1, y1, () -> focus = null);
            body(x0 + 5, y0 + 16, x1 - 5, y1 - Ui.BH - 6);
            // OK / Cancel
            String ok = okLabel();
            int cw = ui.buttonWidth(tr("whathitme.pack.cancel"));
            ui.button("dlg.cancel", tr("whathitme.pack.cancel"), x1 - 5 - cw, y1 - Ui.BH - 4, cw, true, this::cancel);
            if (ok != null) {
                int ow = ui.buttonWidth(ok);
                ui.button("dlg.ok", ok, x1 - 5 - cw - GAP - ow, y1 - Ui.BH - 4, ow, okEnabled(), this::ok);
            }
        }

        String okLabel() {
            return tr("whathitme.pack.ok");
        }

        boolean okEnabled() {
            return true;
        }

        abstract void body(int x0, int y0, int x1, int y1);

        void ok() {
            dialog = null;
        }

        void cancel() {
            dialog = null;
        }

        boolean key(int key, boolean ctrl, boolean shift) {
            if (key == TemplateEditor.K_ESCAPE) {
                cancel();
                return true;
            }
            if (key == TemplateEditor.K_ENTER) {
                if (okLabel() != null && okEnabled()) ok();
                return true;
            }
            return focus != null && focus.key(key, ctrl, shift);
        }

        boolean typed(char c) {
            if (focus == null) return false;
            focus.typed(c);
            changed();
            return true;
        }

        void changed() {}

        void field(String id, TextField f, int x, int y, int w) {
            ui.field(id, f, x, y, w, focus == f, blink, null, () -> focus = f);
        }
    }

    final class PromptDialog extends Dialog {
        final String label;
        final TextField input;
        final Consumer<String> done;
        boolean allowEmpty;

        PromptDialog(String title, String label, String initial, Consumer<String> done) {
            super(title);
            this.label = label;
            this.input = new TextField(initial);
            this.done = done;
            focus = input;
        }

        @Override
        int prefH() {
            return 74;
        }

        @Override
        boolean okEnabled() {
            return allowEmpty || !input.text.trim().isEmpty();
        }

        @Override
        void body(int x0, int y0, int x1, int y1) {
            ui.text(Fmt.GRAY + label, x0, y0 + 2, x1 - x0, 0xFFFFFF);
            field("dlg.input", input, x0, y0 + 13, x1 - x0);
        }

        @Override
        void ok() {
            dialog = null;
            done.accept(input.text.trim());
        }
    }

    final class ConfirmDialog extends Dialog {
        final String text;
        final Runnable yes;

        ConfirmDialog(String title, String text, Runnable yes) {
            super(title);
            this.text = text;
            this.yes = yes;
        }

        @Override
        int prefH() {
            return 80;
        }

        @Override
        void body(int x0, int y0, int x1, int y1) {
            int y = y0 + 2;
            for (String l : LegacyText.wrap(ui.g, Fmt.WHITE + text, x1 - x0)) {
                if (y + 9 > y1) break;
                ui.text(l, x0, y, 0xFFFFFF);
                y += 10;
            }
        }

        @Override
        void ok() {
            dialog = null;
            yes.run();
        }
    }

    /** A searchable, scrollable list of rows {label, detail} */
    final class ListBox {
        final List<String[]> rows;
        final TextField filter = new TextField("");
        int scroll;
        int selected = -1;
        final String id;

        ListBox(String id, List<String[]> rows) {
            this.id = id;
            this.rows = rows;
            filter.placeholder = tr("whathitme.pack.search");
        }

        List<Integer> visible() {
            String q = filter.text.trim().toLowerCase(Locale.ROOT);
            List<Integer> out = new ArrayList<>();
            for (int i = 0; i < rows.size(); i++) {
                String[] r = rows.get(i);
                if (q.isEmpty()) out.add(i);
                else {
                    boolean hit = false;
                    for (String s : r) if (s != null && s.toLowerCase(Locale.ROOT).contains(q)) hit = true;
                    if (hit) out.add(i);
                }
            }
            return out;
        }

        /** Draws the filter field and the list; pick: called with the row index on a click */
        void render(Dialog d, int x0, int y0, int x1, int y1, Consumer<Integer> pick, boolean withFilter) {
            int y = y0;
            if (withFilter) {
                d.field(id + ".filter", filter, x0, y, x1 - x0);
                y += Ui.BH + 2;
            }
            ui.fill(x0, y, x1, y1, 0x60000000);
            List<Integer> vis = visible();
            int fit = Math.max(1, (y1 - y - 2) / Ui.ROW);
            scroll = Math.max(0, Math.min(scroll, Math.max(0, vis.size() - fit)));
            ui.onWheel(x0, y, x1, y1, dd -> scroll = Math.max(0, Math.min(Math.max(0, vis.size() - fit), scroll + dd)));
            int ry = y + 1;
            for (int k = scroll; k < vis.size() && k < scroll + fit; k++) {
                int i = vis.get(k);
                String[] r = rows.get(i);
                boolean over = ui.hover(x0, ry, x1 - 3, ry + Ui.ROW);
                if (i == selected) ui.fill(x0, ry, x1 - 3, ry + Ui.ROW, 0x5055FFFF);
                else if (over) ui.fill(x0, ry, x1 - 3, ry + Ui.ROW, 0x30FFFFFF);
                String main = Fmt.WHITE + r[0];
                String extra = r.length > 1 && r[1] != null && !r[1].isEmpty() ? Fmt.DARK_GRAY + "  " + r[1] : "";
                ui.text(main + extra, x0 + 3, ry + 2, x1 - x0 - 8, 0xFFFFFF);
                ui.onClick(id + ":" + k, x0, ry, x1 - 3, ry + Ui.ROW, () -> {
                    selected = i;
                    pick.accept(i);
                });
                ry += Ui.ROW;
            }
            if (vis.isEmpty()) ui.text(Fmt.DARK_GRAY + tr("whathitme.pack.nomatch"), x0 + 3, y + 3, x1 - x0 - 6, 0xFFFFFF);
            ui.scrollBar(x1 - 3, y + 1, y1 - 1, scroll, fit, vis.size());
        }
    }

    final class ChoiceDialog extends Dialog {
        final ListBox box;
        final Consumer<Integer> pick;

        ChoiceDialog(String title, List<String> labels, List<String> details, Consumer<Integer> pick) {
            super(title);
            List<String[]> rows = new ArrayList<>();
            for (int i = 0; i < labels.size(); i++) rows.add(new String[] {labels.get(i), details == null ? "" : details.get(i)});
            box = new ListBox("choice", rows);
            this.pick = pick;
            focus = box.filter;
        }

        @Override
        int prefH() {
            return 200;
        }

        @Override
        String okLabel() {
            return null;
        }

        @Override
        void body(int x0, int y0, int x1, int y1) {
            box.render(this, x0, y0, x1, y1, i -> {
                dialog = null;
                pick.accept(i);
            }, box.rows.size() > 6);
        }
    }

    /** Match-key builder: damage id or mob from the server's lists, wildcards, entity / item / dimension parts */
    final class KeyDialog extends Dialog {
        final Consumer<String> done;
        boolean mobs;
        final ListBox damageBox;
        final ListBox mobBox;
        final TextField dmg = new TextField("");
        final TextField ent = new TextField("");
        final TextField item = new TextField("");
        final TextField dim = new TextField("");
        final TextField result = new TextField("");

        KeyDialog(String initial, Consumer<String> done) {
            super(tr("whathitme.pack.builder.title"));
            this.done = done;
            List<String[]> d = new ArrayList<>();
            for (String[] r : model.damage) d.add(new String[] {r[0], (r[1].equals(r[0]) ? "" : r[1]) + (r[2].isEmpty() ? "" : " [" + r[2] + "]")});
            List<String[]> m = new ArrayList<>();
            for (String[] r : model.mobs) m.add(new String[] {r[0], (r[1].equals(r[0]) ? "" : r[1]) + (r[2].isEmpty() ? "" : " [" + r[2] + "]")});
            damageBox = new ListBox("kd", d);
            mobBox = new ListBox("km", m);
            for (String t : tokens(initial == null ? "" : initial)) {
                if (t.startsWith("@item:")) item.text = t.substring(6);
                else if (t.startsWith("@dim:")) dim.text = t.substring(5);
                else if (t.startsWith("@mob:")) ent.text = t.substring(5);
                else if (t.startsWith("@")) ent.text = t.substring(1);
                else if (dmg.text.isEmpty()) dmg.text = t;
            }
            for (TextField f : new TextField[] {dmg, ent, item, dim}) f.cursor = f.anchor = f.text.length();
            dmg.placeholder = tr("whathitme.pack.builder.dmg");
            ent.placeholder = tr("whathitme.pack.builder.ent");
            item.placeholder = tr("whathitme.pack.builder.item");
            dim.placeholder = tr("whathitme.pack.builder.dim");
            focus = damageBox.filter;
            build();
        }

        void build() {
            StringBuilder sb = new StringBuilder();
            if (!dmg.text.trim().isEmpty()) sb.append(dmg.text.trim());
            if (!ent.text.trim().isEmpty()) sb.append(sb.length() > 0 && !dmg.text.trim().isEmpty() ? "" : "").append(sb.length() == 0 ? "*@" : "@").append(ent.text.trim());
            if (!item.text.trim().isEmpty()) sb.append(sb.length() == 0 ? "" : " ").append("@item:").append(item.text.trim());
            if (!dim.text.trim().isEmpty()) sb.append(sb.length() == 0 ? "" : " ").append("@dim:").append(dim.text.trim());
            result.text = sb.toString();
        }

        @Override
        int prefW() {
            return 360;
        }

        @Override
        int prefH() {
            return 260;
        }

        @Override
        void changed() {
            if (focus != result) build();
        }

        @Override
        boolean key(int key, boolean ctrl, boolean shift) {
            boolean r = super.key(key, ctrl, shift);
            if (dialog == this && focus != null && focus != result && key != TemplateEditor.K_ENTER) build();
            return r;
        }

        @Override
        void body(int x0, int y0, int x1, int y1) {
            int x = x0;
            x += ui.tab("kb.damage", tr("whathitme.pack.builder.damage"), x, y0, ui.width(tr("whathitme.pack.builder.damage")) + 10, !mobs, () -> {
                mobs = false;
                focus = damageBox.filter;
            }) + GAP;
            ui.tab("kb.mobs", tr("whathitme.pack.builder.mobs"), x, y0, ui.width(tr("whathitme.pack.builder.mobs")) + 10, mobs, () -> {
                mobs = true;
                focus = mobBox.filter;
            });
            int y = y0 + Ui.BH + 3;
            int partsH = 3 * (Ui.BH + 2) + 12;
            ListBox box = mobs ? mobBox : damageBox;
            box.render(this, x0, y, x1, y1 - partsH, i -> {
                String id = box.rows.get(i)[0];
                if (mobs) ent.text = id;
                else dmg.text = id;
                build();
            }, true);
            y = y1 - partsH + 2;
            // wildcard buttons for the selected row's mod
            int bx = x0;
            int sel = box.selected;
            String selId = sel >= 0 ? box.rows.get(sel)[0] : "";
            String mod = selId.startsWith("mob:") ? selId.substring(4) : selId;
            mod = mod.indexOf(':') > 0 ? mod.substring(0, mod.indexOf(':')) : "";
            final String fmod = mod;
            bx += ui.button("kb.any", "*", bx, y, 16, true, () -> {
                if (mobs) ent.text = "*";
                else dmg.text = "*";
                build();
            }) + GAP;
            String lm = fmod.isEmpty() ? tr("whathitme.pack.builder.modall") : fmod + ":*";
            bx += ui.button("kb.mod", lm, bx, y, ui.buttonWidth(lm), !fmod.isEmpty(), () -> {
                if (mobs) ent.text = fmod + ":*";
                else dmg.text = fmod + ":*";
                build();
            }) + GAP;
            String lmob = "mob:" + (fmod.isEmpty() ? "*" : fmod + ":*");
            bx += ui.button("kb.mobany", lmob, bx, y, ui.buttonWidth(lmob), !mobs, () -> {
                dmg.text = lmob;
                build();
            }) + GAP;
            ui.button("kb.clear", tr("whathitme.pack.builder.clear"), bx, y, ui.buttonWidth(tr("whathitme.pack.builder.clear")), true, () -> {
                dmg.text = "";
                ent.text = "";
                item.text = "";
                dim.text = "";
                build();
            });
            y += Ui.BH + 2;
            int hw = (x1 - x0 - GAP) / 2;
            field("kb.dmg", dmg, x0, y, hw);
            field("kb.ent", ent, x0 + hw + GAP, y, hw);
            y += Ui.BH + 2;
            field("kb.item", item, x0, y, hw);
            field("kb.dim", dim, x0 + hw + GAP, y, hw);
            y += Ui.BH + 2;
            ui.text(Fmt.GRAY + tr("whathitme.pack.builder.result") + " " + Fmt.AQUA + result.text, x0, y + 1, x1 - x0, 0xFFFFFF);
        }

        @Override
        boolean okEnabled() {
            return !result.text.trim().isEmpty();
        }

        @Override
        void ok() {
            dialog = null;
            done.accept(result.text.trim());
        }
    }

    /** Condition picker: prefix, value from the server's lists (or typed), negation, score comparison */
    final class CondDialog extends Dialog {
        final Consumer<String> done;
        final List<String> prefixes = new ArrayList<>(Conditions.prefixes());
        int prefix;
        boolean negate;
        final TextField value = new TextField("");
        final TextField number = new TextField("1");
        int op;
        final String[] ops = {">=", ">", "<=", "<", "=", "!="};
        ListBox box;

        CondDialog(Consumer<String> done) {
            super(tr("whathitme.pack.cond.title"));
            this.done = done;
            value.placeholder = tr("whathitme.pack.cond.value");
            pick(0);
        }

        void pick(int i) {
            prefix = i;
            List<String[]> rows = new ArrayList<>();
            for (String s : model.conds.getOrDefault(prefixes.get(i), Collections.<String>emptyList())) {
                String[] p = s.split("\t", 2);
                rows.add(new String[] {p[0], p.length > 1 ? p[1] : ""});
            }
            box = new ListBox("cv", rows);
            focus = box.filter;
        }

        String result() {
            String p = prefixes.get(prefix);
            String v = value.text.trim();
            if (v.isEmpty()) return "";
            if ("score".equals(p)) v = v + ops[op] + number.text.trim();
            return (negate ? "!" : "") + p + ":" + v;
        }

        @Override
        int prefW() {
            return 360;
        }

        @Override
        int prefH() {
            return 250;
        }

        @Override
        boolean okEnabled() {
            return !result().isEmpty();
        }

        @Override
        void body(int x0, int y0, int x1, int y1) {
            int listW = Math.min(100, (x1 - x0) / 3);
            ui.fill(x0, y0, x0 + listW, y1, 0x60000000);
            int ry = y0 + 1;
            for (int i = 0; i < prefixes.size(); i++) {
                String p = prefixes.get(i);
                String problem = model.condProblems.get(p);
                boolean bad = problem != null && !problem.isEmpty();
                boolean sel = i == prefix;
                if (sel) ui.fill(x0, ry, x0 + listW, ry + Ui.ROW, 0x5055FFFF);
                else if (ui.hover(x0, ry, x0 + listW, ry + Ui.ROW)) ui.fill(x0, ry, x0 + listW, ry + Ui.ROW, 0x30FFFFFF);
                ui.text((bad ? Fmt.DARK_GRAY : Fmt.WHITE) + p, x0 + 3, ry + 2, listW - 6, 0xFFFFFF);
                final int idx = i;
                ui.onClick("cp:" + p, x0, ry, x0 + listW, ry + Ui.ROW, () -> pick(idx));
                if (ui.hover(x0, ry, x0 + listW, ry + Ui.ROW)) tooltip(bad ? problem : tr("whathitme.pack.cond.help." + p));
                ry += Ui.ROW;
            }
            int x = x0 + listW + 4;
            int bottomH = (Ui.BH + 2) * 2 + 12;
            box.render(this, x, y0, x1, y1 - bottomH, i -> value.text = box.rows.get(i)[0], true);
            int y = y1 - bottomH + 2;
            String p = prefixes.get(prefix);
            ui.checkbox("cp.negate", tr("whathitme.pack.cond.negate"), x, y + 2, 60, negate, true, () -> negate = !negate);
            int vx = x + 62;
            if ("score".equals(p)) {
                int vw = Math.max(40, (x1 - vx) - 70);
                field("cp.value", value, vx, y, vw);
                ui.button("cp.op", ops[op], vx + vw + GAP, y, 20, true, () -> op = (op + 1) % ops.length);
                field("cp.number", number, vx + vw + GAP + 22, y, x1 - (vx + vw + GAP + 22));
            } else {
                field("cp.value", value, vx, y, x1 - vx);
            }
            y += Ui.BH + 2;
            String problem = model.condProblems.get(p);
            String res = result();
            ui.text(Fmt.GRAY + tr("whathitme.pack.builder.result") + " " + Fmt.AQUA + (res.isEmpty() ? "-" : res)
                + (problem != null && !problem.isEmpty() ? Fmt.GOLD + "  " + problem : ""), x, y + 1, x1 - x, 0xFFFFFF);
        }

        @Override
        void ok() {
            dialog = null;
            done.accept(result());
        }
    }

    // ------------------------------------------------------------ self-test helpers

    /** Opens the condition picker for a list (self-test, screenshots) */
    public void openConditionPicker() {
        if (draft != null) dialog = new CondDialog(s -> draft.ifs.add(new TextField(s)));
    }

    /** Opens the match-key builder for the key of the open entry (self-test, screenshots) */
    public void openKeyBuilder() {
        if (draft != null) {
            final Draft d = draft;
            dialog = new KeyDialog(d.key.text, s -> d.key.text = s);
        }
    }

    public void openTreeAction(String id) {
        treeAction(id);
    }

    /** Types into the focused field of the open dialog (self-test) */
    public void dialogType(String s) {
        if (dialog == null) return;
        for (char c : s.toCharArray()) dialog.typed(c);
    }

    public void dialogOk() {
        if (dialog != null && dialog.okLabel() != null && dialog.okEnabled()) dialog.ok();
    }

    public void dialogCancel() {
        if (dialog != null) dialog.cancel();
    }

    /** Picks row i of the list of an open choice dialog (self-test) */
    public void dialogPick(String label) {
        if (dialog instanceof ChoiceDialog) {
            ChoiceDialog c = (ChoiceDialog) dialog;
            for (int i = 0; i < c.box.rows.size(); i++) {
                if (c.box.rows.get(i)[0].equals(label)) {
                    dialog = null;
                    c.pick.accept(i);
                    return;
                }
            }
        }
    }

    public void togglePreview(boolean on) {
        showPreview = on;
    }

    public void setPreviewAs(String player, String assumeText) {
        previewAs = player == null ? "" : player;
        assume.text = assumeText == null ? "" : assumeText;
    }

    /** The template editor child of the templates tab (self-test) */
    public void openTemplateLinesEditor() {
        openTemplateLines();
    }

    /** Number of rows the form needs in the last frame (layout checks) */
    public int formHeight() {
        return fTotal;
    }

    /** The visible pane in narrow windows (0 tree, 1 list, 2 detail) */
    public int pane() {
        return pane;
    }
}
