package io.github.junjun1z.whathitme.devtools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.github.junjun1z.whathitme.client.ClientState;
import io.github.junjun1z.whathitme.data.DamageRecord;
import io.github.junjun1z.whathitme.data.PlayerHistory;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.IMob;
import net.minecraft.world.EnumDifficulty;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/** Compatibility run in a heavy modpack: modded mobs attack the player for real, then it dies. */
final class PackPlan {

    private final SelfTest t;
    private final List<String> spawned = new ArrayList<>();

    PackPlan(SelfTest t) {
        this.t = t;
    }

    private static boolean modded(PlayerHistory h) {
        if (h == null) return false;
        for (DamageRecord r : h.records()) if (!r.modId.equals("minecraft") && !r.modId.isEmpty()) return true;
        return false;
    }

    void plan() {
        t.loadWorld("whm_packtest");
        t.waitFor("server info received", () -> ClientState.serverHasMod, 400);
        t.srv(p -> {
            p.getServer().setDifficultyForAllWorlds(EnumDifficulty.HARD);
            p.world.getGameRules().setOrCreateGameRule("doDaylightCycle", "false");
            p.world.getGameRules().setOrCreateGameRule("keepInventory", "true");
            p.world.setWorldTime(6000);
            p.inventory.clear();
        });
        t.sleep(100);
        t.srv(p -> {
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
            t.srv(p -> {
                if (p.getHealth() > 0) p.setHealth(p.getMaxHealth());
            });
            t.sleep(40);
            t.act(() -> {
                if (SelfTest.mc().currentScreen instanceof GuiGameOver) {
                    SelfTest.mc().player.respawnPlayer();
                    SelfTest.mc().displayGuiScreen(null);
                }
            });
        }
        t.check("damage from modded mobs recorded with their mod", () -> modded(SelfTest.serverHistory()), (java.util.function.Supplier<?>) SelfTest::ids);
        t.act(() -> t.info("records: " + SelfTest.ids()));
        t.cmd("/whm", "Damage History", 60);
        t.cmd("/whm search @lycanitesmobs", "Search: @lycanitesmobs", 60);
        t.cmd("/whm effects", "Active Effects", 60);
        // now let them kill the player
        t.waitFor("killed by the pack (or timeout)", () -> SelfTest.mc().currentScreen instanceof GuiGameOver, 1200);
        t.act(() -> {
            if (!(SelfTest.mc().currentScreen instanceof GuiGameOver)) t.srv(p -> p.attackEntityFrom(net.minecraft.util.DamageSource.OUT_OF_WORLD, 10000f));
        });
        t.waitFor("death recap", () -> ClientState.currentDeath != null && ClientState.currentDeath.key() != null, 200);
        t.act(() -> t.info("killing blow: " + (ClientState.currentDeath == null || ClientState.currentDeath.key() == null ? "-" : ClientState.currentDeath.key().damageId
            + " / " + ClientState.currentDeath.key().attackerName + " / " + ClientState.currentDeath.key().modName)));
        t.sleep(30);
        t.screenshot("whm_selftest_pack_death");
        t.act(() -> {
            SelfTest.mc().player.respawnPlayer();
            SelfTest.mc().displayGuiScreen(null);
        });
        t.waitFor("respawn summary", () -> SelfTest.chatHas("Killing Blow"), 200);
        t.srv(p -> {
            for (Entity e : new ArrayList<>(p.world.loadedEntityList)) if (e instanceof EntityLiving) e.setDead();
        });
        t.cmd("/whm last", "Death Recap", 60);
        t.cmd("/whm dump", "Wrote", 80);
        t.sleep(20);
    }
}
