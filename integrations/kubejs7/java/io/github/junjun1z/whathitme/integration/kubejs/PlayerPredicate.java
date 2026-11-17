package io.github.junjun1z.whathitme.integration.kubejs;

/** A script function (player) =&gt; boolean */
@FunctionalInterface
public interface PlayerPredicate {

    boolean test(Object player);
}
