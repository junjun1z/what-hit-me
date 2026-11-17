package io.github.junjun1z.whathitme.integration.kubejs;

import io.github.junjun1z.whathitme.api.WhmDamage;

/** A script function (player, damage) =&gt; text */
@FunctionalInterface
public interface PlayerValue {

    Object apply(Object player, WhmDamage damage);
}
