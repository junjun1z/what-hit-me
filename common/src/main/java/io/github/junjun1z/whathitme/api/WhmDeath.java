package io.github.junjun1z.whathitme.api;

import java.util.List;

/** A death (or alert) recap: the hits shortly before it (read only). */
public interface WhmDeath {

    /** "death", "totem", "heavy", "prevented" */
    String getKind();

    long getTime();

    /** Oldest first */
    List<WhmDamage> getRecords();

    /** The killing blow / trigger hit, null when unknown */
    WhmDamage getKillingBlow();

    /** The vanilla death message as plain English text ("" = none) */
    String getDeathMessage();

    /** Health + absorption lost in the recap */
    float getTotalAmount();
}
