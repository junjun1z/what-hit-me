package io.github.junjun1z.whathitme.core.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** The "if" (all), "any" (one of) and "unless" (none) lists of a rule or death trigger. Immutable. */
public final class Conds {

    public static final Conds NONE = new Conds(Collections.<Condition>emptyList(), Collections.<Condition>emptyList(), Collections.<Condition>emptyList());

    public final List<Condition> all;
    public final List<Condition> any;
    public final List<Condition> unless;

    public Conds(List<Condition> all, List<Condition> any, List<Condition> unless) {
        this.all = Collections.unmodifiableList(new ArrayList<>(all));
        this.any = Collections.unmodifiableList(new ArrayList<>(any));
        this.unless = Collections.unmodifiableList(new ArrayList<>(unless));
    }

    /** Reads "if", "any" and "unless" of a JSON object */
    public static Conds read(Map<String, Object> o) {
        List<Condition> a = Condition.list(o.get("if"));
        List<Condition> y = Condition.list(o.get("any"));
        List<Condition> u = Condition.list(o.get("unless"));
        if (a.isEmpty() && y.isEmpty() && u.isEmpty()) return NONE;
        return new Conds(a, y, u);
    }

    public boolean isEmpty() {
        return all.isEmpty() && any.isEmpty() && unless.isEmpty();
    }

    public List<Condition> every() {
        List<Condition> out = new ArrayList<>(all);
        out.addAll(any);
        out.addAll(unless);
        return out;
    }

    public boolean met(RuleEval e) {
        for (Condition c : all) if (!e.test(c)) return false;
        if (!any.isEmpty()) {
            boolean one = false;
            for (Condition c : any) {
                if (e.test(c)) {
                    one = true;
                    break;
                }
            }
            if (!one) return false;
        }
        for (Condition c : unless) if (e.test(c)) return false;
        return true;
    }
}
