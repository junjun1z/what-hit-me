package io.github.junjun1z.whathitme.api;

import java.util.Map;

/** Death statistics of a player (the data of /whm stats). */
public interface WhmStats {

    int getDeaths();

    /** Health points lost in total */
    double getDamageTaken();

    /** Deaths per cause id (damage id, or mob:id), most deaths first */
    Map<String, Integer> getCauses();
}
