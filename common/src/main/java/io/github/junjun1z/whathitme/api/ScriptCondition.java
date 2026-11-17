package io.github.junjun1z.whathitme.api;

/** A condition rules can use as {@code script:<name>}. */
@FunctionalInterface
public interface ScriptCondition {

    /**
     * @param player the player the rule is evaluated for
     * @return true when the condition is fulfilled; an exception counts as false (logged once)
     */
    boolean test(WhmPlayer player) throws Exception;
}
