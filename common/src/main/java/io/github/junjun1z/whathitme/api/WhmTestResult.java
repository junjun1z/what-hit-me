package io.github.junjun1z.whathitme.api;

import java.util.List;

/** What a death from a damage id would show for a player (the data of /whm test). */
public interface WhmTestResult {

    /** Display name (English, plain) */
    String getName();

    /** Tip (English, plain), "" = none */
    String getHint();

    /** The matching rule as "match (file #n)", "" = none */
    String getRule();

    /** Hidden fields */
    List<String> getHidden();

    /** The custom / hidden death message, "" = vanilla */
    String getDeathMessage();

    /** Commands death triggers and the rule would run */
    List<String> getCommands();
}
