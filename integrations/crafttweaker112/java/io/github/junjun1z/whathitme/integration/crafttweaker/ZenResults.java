package io.github.junjun1z.whathitme.integration.crafttweaker;

import io.github.junjun1z.whathitme.api.WhmStats;
import io.github.junjun1z.whathitme.api.WhmTestResult;

import crafttweaker.annotations.ZenRegister;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;

/** Statistics and test results for ZenScript */
public final class ZenResults {

    private ZenResults() {}

    @ZenRegister
    @ZenClass("mods.whathitme.Stats")
    public static class Stats {
        final WhmStats s;

        Stats(WhmStats s) {
            this.s = s;
        }

        @ZenGetter("deaths")
        public int deaths() {
            return s.getDeaths();
        }

        @ZenGetter("damageTaken")
        public double damageTaken() {
            return s.getDamageTaken();
        }
    }

    @ZenRegister
    @ZenClass("mods.whathitme.TestResult")
    public static class TestResult {
        final WhmTestResult r;

        TestResult(WhmTestResult r) {
            this.r = r;
        }

        @ZenGetter("name")
        public String name() {
            return r.getName();
        }

        @ZenGetter("hint")
        public String hint() {
            return r.getHint();
        }

        @ZenGetter("rule")
        public String rule() {
            return r.getRule();
        }

        @ZenGetter("deathMessage")
        public String deathMessage() {
            return r.getDeathMessage();
        }
    }
}
