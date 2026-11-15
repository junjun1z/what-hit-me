package io.github.junjun1z.whathitme.devtest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.github.junjun1z.whathitme.core.client.ClientCore;
import io.github.junjun1z.whathitme.core.data.DamageRecord;
import io.github.junjun1z.whathitme.core.data.PlayerHistory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.EnumDifficulty;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/** Compatibility run in a heavy 1.12.2 modpack ("pack" mode): modded mobs attack the player for real, then it dies. */
final class PackPlan112 {

    private final SelfTest t;
    private final List<String> spawned = new ArrayList<>();

    PackPlan112(SelfTest t) {
        this.t = t;
    }

    private static boolean modded(PlayerHistory h) {
        if (h == null) return false;
        for (DamageRecord r : h.records()) if (!r.modId.equals("minecraft") && !r.modId.isEmpty()) return true;
        return false;
    }

    private void srv(Consumer<EntityPlayerMP> c) {
        t.act(() -> DevPort112.server().addScheduledTask(() -> c.accept(DevPort112.server().getPlayerList().getPlayers().get(0))));
    }

    void plan() {
        t.loadWorld("whm_packtest");
        t.waitFor("server info received", () -> ClientCore.serverHasMod, 400);
        srv(p -> {
            p.getServer().setDifficultyForAllWorlds(EnumDifficulty.HARD);
            p.world.getGameRules().setOrCreateGameRule("doDaylightCycle", "false");
            p.world.getGameRules().setOrCreateGameRule("keepInventory", "true");
            p.world.setWorldTime(6000);
            p.inventory.clear();
        });
        t.sleep(100);
        srv(p -> {
            List<String> mods = Arrays.asList("lycanitesmobs", "iceandfire", "thaumcraft", "draconicevolution", "tconstruct");
            int n = 0;
            for (EntityEntry e : ForgeRegistries.ENTITIES.getValuesCollection()) {
                if (e.getRegistryName() == null || !mods.contains(e.getRegistryName().getNamespace())) continue;
                if (!IMob.class.isAssignableFrom(e.getEntityClass()) || !EntityLiving.class.isAssignableFrom(e.getEntityClass())) continue;
                String path = e.getRegistryName().getPath();
                if (path.contains("dragon") || path.contains("boss") || path.contains("guardian") || path.contains("wither") || path.contains("rahovart")
                    || path.contains("asmodeus") || path.contains("amalgalich")) continue;
                if (n >= 8) break;
                try {
                    Entity ent = EntityList.createEntityByIDFromName(e.getRegistryName(), p.world);
                    if (!(ent instanceof EntityLiving)) continue;
                    double a = n * Math.PI / 4;
                    ent.setLocationAndAngles(p.posX + Math.cos(a) * 3, p.posY, p.posZ + Math.sin(a) * 3, 0f, 0f);
                    ((EntityLiving) ent).setAttackTarget(p);
                    p.world.spawnEntity(ent);
                    spawned.add(e.getRegistryName().toString());
                    n++;
                } catch (Throwable ex) {
                    t.info("could not spawn " + e.getRegistryName() + ": " + ex);
                }
            }
        });
        t.sleep(5);
        t.act(() -> t.info("spawned: " + spawned));
        // survive a while (heal), let them hit
        for (int i = 0; i < 12; i++) {
            srv(p -> {
                if (p.getHealth() > 0) p.setHealth(p.getMaxHealth());
            });
            t.sleep(40);
            t.act(() -> {
                if (t.port.deathScreen()) t.port.respawn();
            });
        }
        t.check("damage from modded mobs recorded with their mod", () -> modded(t.serverHistory()), (Supplier<?>) t::ids);
        t.act(() -> t.info("records: " + t.ids()));
        t.cmd("/whm", "Damage History", 60);
        t.cmd("/whm search @lycanitesmobs", "Search: @lycanitesmobs", 60);
        t.cmd("/whm effects", "Active Effects", 60);
        // now let them kill the player
        t.waitFor("killed by the pack (or timeout)", t.port::deathScreen, 1200);
        t.act(() -> {
            if (!t.port.deathScreen()) t.port.server(DevPort.Act.VOID, 10000f);
        });
        t.waitFor("death recap", () -> ClientCore.currentDeath != null && ClientCore.currentDeath.key() != null, 200);
        t.act(() -> t.info("killing blow: " + (ClientCore.currentDeath == null || ClientCore.currentDeath.key() == null ? "-"
            : ClientCore.currentDeath.key().damageId + " / " + ClientCore.currentDeath.key().attackerName + " / " + ClientCore.currentDeath.key().modName)));
        t.sleep(30);
        t.screenshot("whm_selftest_pack_death");
        t.act(t.port::respawn);
        t.waitFor("respawn summary", () -> t.chatHas("Killing Blow"), 200);
        srv(p -> {
            for (Entity e : new ArrayList<>(p.world.loadedEntityList)) if (e instanceof EntityLiving) e.setDead();
        });
        t.cmd("/whm last", "Death Recap", 60);
        t.act(() -> {
            io.github.junjun1z.whathitme.core.data.DamageRecord k = ClientCore.currentDeath == null ? null : ClientCore.currentDeath.key();
            t.info("1.2: killing blow vital=" + (k != null && k.has(io.github.junjun1z.whathitme.core.data.DamageRecord.VITAL)) + " hpLeft="
                + (k == null ? -1 : k.healthAfter) + " maxHealth=" + (k == null ? -1 : k.maxHealth) + " context=" + (k == null ? "" : k.context)
                + " gear=" + (ClientCore.currentDeath != null && ClientCore.currentDeath.gear != null));
            t.info("1.2: catalog cache " + io.github.junjun1z.whathitme.core.data.CatalogCache.status + ", damage types "
                + io.github.junjun1z.whathitme.core.data.CatalogCache.damageTypeEntries().size() + ", pack errors " + io.github.junjun1z.whathitme.core.config.WhmConfig.pack.errors);
        });
        t.check("1.2: catalog cache loaded in the pack", io.github.junjun1z.whathitme.core.data.CatalogCache::loaded, null);
        t.cmd("/whm stats", "Death Statistics", 60);
        t.cmd("/whm test mob:lycanitesmobs:ventoraptor", "Chat preview", 60);
        t.cmd("/whm copyrecap", "Death recap copied", 40);
        t.waitFor("copy recap has the equipment block", () -> t.port.clipboard().contains("## Equipment"), 40);
        t.cmd("/whm dump", "Wrote", 80);
        t.sleep(20);
    }
}
