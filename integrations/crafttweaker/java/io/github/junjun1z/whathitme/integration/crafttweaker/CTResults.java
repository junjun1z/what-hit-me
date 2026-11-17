package io.github.junjun1z.whathitme.integration.crafttweaker;

import io.github.junjun1z.whathitme.api.WhmStats;
import io.github.junjun1z.whathitme.api.WhmTestResult;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import org.openzen.zencode.java.ZenCodeType;

/** Statistics and test results for ZenScript */
public final class CTResults {

    private CTResults() {}

    @ZenRegister
    @ZenCodeType.Name("mods.whathitme.Stats")
    public static class Stats {
        final WhmStats s;

        Stats(WhmStats s) {
            this.s = s;
        }

        @ZenCodeType.Getter("deaths")
        public int deaths() {
            return s.getDeaths();
        }

        @ZenCodeType.Getter("damageTaken")
        public double damageTaken() {
            return s.getDamageTaken();
        }
    }

    @ZenRegister
    @ZenCodeType.Name("mods.whathitme.TestResult")
    public static class TestResult {
        final WhmTestResult r;

        TestResult(WhmTestResult r) {
            this.r = r;
        }

        @ZenCodeType.Getter("name")
        public String name() {
            return r.getName();
        }

        @ZenCodeType.Getter("hint")
        public String hint() {
            return r.getHint();
        }

        @ZenCodeType.Getter("rule")
        public String rule() {
            return r.getRule();
        }

        @ZenCodeType.Getter("deathMessage")
        public String deathMessage() {
            return r.getDeathMessage();
        }
    }
}
