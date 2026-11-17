package io.github.junjun1z.whathitme.core.script;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import io.github.junjun1z.whathitme.api.WhmDamage;
import io.github.junjun1z.whathitme.api.WhmDeath;
import io.github.junjun1z.whathitme.api.WhmPlayer;
import io.github.junjun1z.whathitme.core.Safe;
import io.github.junjun1z.whathitme.core.data.DamageRecord;
import io.github.junjun1z.whathitme.core.data.RuleMark;
import io.github.junjun1z.whathitme.core.data.Snapshot;
import io.github.junjun1z.whathitme.core.rules.Condition;
import io.github.junjun1z.whathitme.core.rules.RuleEval;
import io.github.junjun1z.whathitme.core.server.PlayerHandle;
import io.github.junjun1z.whathitme.core.server.ServerCore;
import io.github.junjun1z.whathitme.core.text.Fmt;
import io.github.junjun1z.whathitme.core.text.Msg;
import io.github.junjun1z.whathitme.core.text.RecapFormatter;

/** Read-only API views of core objects. */
public final class ScriptViews {

    private ScriptViews() {}

    public static WhmPlayer player(PlayerHandle p) {
        return p == null ? null : new Player(p);
    }

    public static WhmDamage damage(DamageRecord r) {
        return r == null ? null : new Damage(r);
    }

    public static WhmDeath death(Snapshot s) {
        return s == null ? null : new Death(s);
    }

    static final class Player implements WhmPlayer {
        final PlayerHandle h;

        Player(PlayerHandle h) {
            this.h = h;
        }

        @Override
        public UUID getUuid() {
            return h.uuid();
        }

        @Override
        public String getName() {
            return Safe.get("player name", h::name, "");
        }

        @Override
        public Object getNativePlayer() {
            return Safe.get("native player", h::nativePlayer, null);
        }

        @Override
        public boolean check(String condition) {
            if (condition == null || condition.trim().isEmpty()) return false;
            return RuleEval.of(h).test(Condition.parse(condition));
        }

        @Override
        public String toString() {
            return getName();
        }
    }

    static final class Damage implements WhmDamage {
        final DamageRecord r;

        Damage(DamageRecord r) {
            this.r = r;
        }

        @Override
        public String getDamageId() {
            return r.damageId;
        }

        @Override
        public String getDamageType() {
            return r.damageType;
        }

        @Override
        public String getName() {
            return Safe.get("script damage name", () -> {
                RecapFormatter f = new RecapFormatter(Msg.ENGLISH, ServerCore.options(null, false));
                return Fmt.strip(f.damageName(r).toPlain(Msg.ENGLISH_LANG));
            }, r.damageType);
        }

        @Override
        public String getAttackerName() {
            return r.attackerName;
        }

        @Override
        public String getAttackerId() {
            return r.attackerId;
        }

        @Override
        public String getDirectName() {
            return r.directName;
        }

        @Override
        public String getModId() {
            return r.modId;
        }

        @Override
        public String getModName() {
            return r.modName;
        }

        @Override
        public String getWeaponName() {
            return r.weaponName;
        }

        @Override
        public String getWeaponId() {
            return r.weaponId;
        }

        @Override
        public String getOwnerName() {
            return r.ownerName;
        }

        @Override
        public float getAmount() {
            return r.taken();
        }

        @Override
        public float getHearts() {
            return r.taken() / 2f;
        }

        @Override
        public float getRawAmount() {
            return r.raw;
        }

        @Override
        public float getAbsorbed() {
            return r.absorbed;
        }

        @Override
        public float getHealthAfter() {
            return r.healthAfter;
        }

        @Override
        public float getMaxHealth() {
            return r.maxHealth;
        }

        @Override
        public float getDistance() {
            return r.distance;
        }

        @Override
        public int getHits() {
            return r.hits;
        }

        @Override
        public String getDimension() {
            return r.dimension;
        }

        @Override
        public long getTime() {
            return r.time;
        }

        @Override
        public boolean isKillingBlow() {
            return r.has(DamageRecord.KILLING);
        }

        @Override
        public boolean isTotem() {
            return r.has(DamageRecord.TOTEM);
        }

        @Override
        public boolean isHeavy() {
            return r.has(DamageRecord.HEAVY);
        }

        @Override
        public boolean isReflected() {
            return r.has(DamageRecord.REFLECTED);
        }

        @Override
        public boolean isProjectile() {
            return r.has(DamageRecord.PROJECTILE);
        }

        @Override
        public boolean isFire() {
            return r.has(DamageRecord.FIRE);
        }

        @Override
        public boolean isExplosion() {
            return r.has(DamageRecord.EXPLOSION);
        }

        @Override
        public boolean isMagic() {
            return r.has(DamageRecord.MAGIC);
        }

        @Override
        public boolean isBypassArmor() {
            return r.has(DamageRecord.BYPASS_ARMOR);
        }

        @Override
        public List<String> getProperties() {
            List<String> out = new ArrayList<>();
            String[] names = {"bypasses_armor", "absolute", "magic", "fire", "explosion", "projectile", "shield", "iframes", "creative"};
            for (int i = 0; i < names.length; i++) if ((r.flags & (1 << i)) != 0) out.add(names[i]);
            return out;
        }

        @Override
        public List<String> getHidden() {
            return r.mark == null ? Collections.<String>emptyList() : r.mark.hiddenNames();
        }

        @Override
        public List<String> getBadges() {
            return r.mark == null ? Collections.<String>emptyList() : r.mark.badges;
        }

        @Override
        public String getVariable(String name) {
            RuleMark m = r.mark;
            String n = ScriptRegistry.name(name);
            if (m == null || n == null) return "";
            String v = m.vars.get(n);
            return v == null ? "" : v;
        }

        @Override
        public String toString() {
            return r.damageId;
        }
    }

    static final class Death implements WhmDeath {
        final Snapshot s;

        Death(Snapshot s) {
            this.s = s;
        }

        @Override
        public String getKind() {
            switch (s.kind) {
                case Snapshot.TOTEM:
                    return "totem";
                case Snapshot.HEAVY:
                    return "heavy";
                case Snapshot.PREVENTED:
                    return "prevented";
                case Snapshot.HISTORY:
                    return "history";
                default:
                    return "death";
            }
        }

        @Override
        public long getTime() {
            return s.time;
        }

        @Override
        public List<WhmDamage> getRecords() {
            List<WhmDamage> out = new ArrayList<>();
            for (DamageRecord r : s.records) out.add(new Damage(r));
            return out;
        }

        @Override
        public WhmDamage getKillingBlow() {
            return damage(s.key());
        }

        @Override
        public String getDeathMessage() {
            if (s.deathMessage.isEmpty()) return "";
            return Safe.get("script death message", () -> Fmt.strip(io.github.junjun1z.whathitme.core.text.Txt.json(s.deathMessage).toPlain(Msg.ENGLISH_LANG)), "");
        }

        @Override
        public float getTotalAmount() {
            float t = 0f;
            for (DamageRecord r : s.records) t += r.taken();
            return t;
        }
    }
}
