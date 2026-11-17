package io.github.junjun1z.whathitme.api;

/** One registration (for /whm reload and the pack editor, which shows script entries read only). Immutable. */
public final class WhmRegistration {

    /** condition, variable, listener, rename, hint, rule, deathMessage, template, hook */
    public final String kind;
    public final String owner;
    /** Name / match key / output / event name */
    public final String name;
    /** Value: rename / hint / death message text, template lines (\n), rule JSON, description of a condition / variable */
    public final String value;
    /** Made inside a register hook (rebuilt on every reload) */
    public final boolean fromHook;
    /** The function threw at least once (conditions, variables, listeners) */
    public final boolean failed;

    public WhmRegistration(String kind, String owner, String name, String value, boolean fromHook, boolean failed) {
        this.kind = kind;
        this.owner = owner;
        this.name = name;
        this.value = value == null ? "" : value;
        this.fromHook = fromHook;
        this.failed = failed;
    }

    @Override
    public String toString() {
        return kind + " " + name + " (" + owner + ")";
    }
}
