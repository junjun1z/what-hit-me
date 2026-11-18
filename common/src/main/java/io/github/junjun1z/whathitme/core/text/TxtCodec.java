package io.github.junjun1z.whathitme.core.text;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.junjun1z.whathitme.core.data.Json;

/** A {@link Txt} as plain JSON values (maps / lists / strings), so the server can send lines the client translates. */
public final class TxtCodec {

    private TxtCodec() {}

    public static Object write(Txt t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("k", t.kind.name().substring(0, 1));
        m.put("t", t.text);
        Object[] args = t.args();
        if (args.length > 0) {
            List<Object> a = new ArrayList<>();
            for (Object o : args) a.add(o instanceof Txt ? write((Txt) o) : String.valueOf(o));
            m.put("a", a);
        }
        if (t.fallback() != null) m.put("f", t.fallback());
        if (t.color() != null) m.put("c", t.color().name());
        if (t.bold() != null) m.put("b", t.bold());
        if (t.underlined() != null) m.put("u", t.underlined());
        if (!t.siblings().isEmpty()) {
            List<Object> s = new ArrayList<>();
            for (Txt x : t.siblings()) s.add(write(x));
            m.put("s", s);
        }
        return m;
    }

    public static Txt read(Object v) {
        if (!(v instanceof Map)) return Txt.lit(Json.str(v, ""));
        Map<String, Object> m = Json.obj(v);
        String k = Json.str(m.get("k"), "L");
        String text = Json.str(m.get("t"), "");
        List<Object> raw = Json.list(m.get("a"));
        Object[] args = new Object[raw.size()];
        for (int i = 0; i < args.length; i++) args[i] = raw.get(i) instanceof Map ? read(raw.get(i)) : Json.str(raw.get(i), "");
        Txt t;
        if (k.equals("T")) t = m.containsKey("f") ? Txt.trFallback(text, Json.str(m.get("f"), ""), args) : Txt.tr(text, args);
        else if (k.equals("J")) t = Txt.json(text);
        else t = Txt.lit(text);
        if (m.containsKey("c")) {
            try {
                t.color(Fmt.valueOf(Json.str(m.get("c"), "WHITE")));
            } catch (IllegalArgumentException ignored) {
                // unknown color
            }
        }
        if (m.containsKey("b")) t.bold(Json.bool(m.get("b"), false));
        if (m.containsKey("u")) t.underline(Json.bool(m.get("u"), false));
        for (Object s : Json.list(m.get("s"))) t.append(read(s));
        return t;
    }
}
