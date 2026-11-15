package io.github.junjun1z.whathitme.devtest;

import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;

/** Never loaded: damage ids the jar scanner check (V12Plan) must find in our own jar. */
final class ScanProbe {

    static final DamageSource PLAIN = new DamageSource("whm_probe_plain").setFireDamage().setDamageBypassesArmor();

    private ScanProbe() {}

    /** Not a constant: must not be found */
    static DamageSource dynamic(long n) {
        return new DamageSource("whm_probe_dyn" + n);
    }

    static DamageSource named() {
        return new Named("whm_probe_named");
    }

    /** Anonymous subclass: javac passes the outer instance before the id */
    DamageSource anon() {
        return new DamageSource("whm_probe_anon") {
        }.setExplosion();
    }

    static final class Sub extends EntityDamageSource {
        Sub() {
            super("whm_probe_sub", null);
            setMagicDamage();
        }
    }

    static class Named extends DamageSource {
        Named(String id) {
            super(id);
        }
    }

    static final class Child extends Named {
        Child() {
            super("whm_probe_child");
        }
    }
}
