package io.github.junjun1z.whathitme.api;

/** A template variable {@code {script:<name>}}, computed on the server when an output is built. */
@FunctionalInterface
public interface ScriptVariable {

    /**
     * @param player the player the record belongs to
     * @param damage the record
     * @return the text (&amp; color codes allowed); null or "" = empty, an exception counts as empty (logged once)
     */
    String value(WhmPlayer player, WhmDamage damage) throws Exception;
}
