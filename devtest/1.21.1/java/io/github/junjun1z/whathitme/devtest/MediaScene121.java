package io.github.junjun1z.whathitme.devtest;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.datafixers.util.Pair;

import io.github.junjun1z.whathitme.core.client.ClientCore;
import io.github.junjun1z.whathitme.core.client.CoreScreen;
import io.github.junjun1z.whathitme.core.client.DeathPanel;
import io.github.junjun1z.whathitme.core.client.PackEditor;
import io.github.junjun1z.whathitme.core.client.TemplateEditor;
import io.github.junjun1z.whathitme.mc.ConfigScreen121;
import io.github.junjun1z.whathitme.mc.TemplateEditorScreen121;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/**
 * Dev-only scenes for the wiki screenshots and GIFs (flag "media"): config/whathitme/media.txt lists the scenes to run, one
 * per line. Stills go to screenshots/media_*.png, recordings to mediarec/&lt;name&gt;/f_*.png (one frame per client tick).
 * The player is shown as "Steve" (the profile name is replaced in this dev run only).
 */
public final class MediaScene121 {

    private static Minecraft mc() {
        return Minecraft.getInstance();
    }

    private final SelfTest t;
    private final File root;
    private ExecutorService pool;
    private File recDir;
    private int frame;
    private final List<String> marks = new ArrayList<>();

    private boolean cursor;
    private float cx, cy, fromX, fromY, toX, toY;
    private int moveLeft, moveTotal;
    private Field mouseX, mouseY;

    private double bx, by, bz;
    private final List<Entity> mobs = new ArrayList<>();
    private Zombie zombie;
    private Skeleton skeleton;
    private Creeper creeper;
    private WitherSkeleton sentinel;

    public MediaScene121(SelfTest t) {
        this.t = t;
        this.root = new File(t.port.gameDir(), "mediarec");
    }

    private static Boolean ja;

    /** Language of this run (read from options.txt: the plan is built before the options are loaded) */
    static boolean ja() {
        if (ja == null) {
            try {
                ja = Files.readAllLines(new File(mc().gameDirectory, "options.txt").toPath(), StandardCharsets.UTF_8).contains("lang:ja_jp");
            } catch (Exception e) {
                ja = false;
            }
        }
        return ja;
    }

    static String L(String en, String ja) {
        return ja() ? ja : en;
    }

    // ------------------------------------------------------------------ plan

    public void plan() {
        List<String> scenes;
        try {
            scenes = Files.readAllLines(new File(t.port.gameDir(), "config/whathitme/media.txt").toPath(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            t.ok("media.txt", false, e);
            return;
        }
        t.then(() -> {
            if (!(mc().screen instanceof TitleScreen)) return false;
            openWorld("whm_media");
            return true;
        });
        t.waitFor("world loaded", t.port::worldReady, 3000);
        t.waitFor("server info", () -> ClientCore.serverHasMod, 400);
        t.act(this::hideName);
        server(this::setup);
        t.sleep(160);
        server(this::reset);
        t.sleep(60);
        for (String raw : scenes) {
            String s = raw.trim();
            if (s.isEmpty() || s.startsWith("#")) continue;
            t.act(() -> t.info("scene " + s));
            scene(s);
        }
    }

    private void scene(String s) {
        switch (s) {
            case "view": shot("view"); break;
            case "death": death(); break;
            case "deathmsg": deathMessage(); break;
            case "templates": templates(); break;
            case "rules": rules(false); break;
            case "rulesftb": rules(true); break;
            case "settings": settings(); break;
            case "tools": tools(); break;
            case "editor": templateEditor(); break;
            case "editorlocked": templateEditorLocked(); break;
            case "pack": packEditor(); break;
            case "packadd": packAddRule(); break;
            case "packundo": packUndo(); break;
            case "packpersonal": packPersonal(); break;
            case "condftb": conditionPicker(); break;
            case "test": testCommand(); break;
            case "reload": reloadCommand(); break;
            case "kick": kick(); break;
            default: t.ok("unknown scene " + s, false, null);
        }
    }

    // ------------------------------------------------------------------ scenes

    private void death() {
        server(this::reset);
        t.sleep(30);
        t.act(t.port::clearChat);
        startRec("death");
        fight();
        t.waitFor("recap", () -> ClientCore.currentDeath != null && t.port.deathScreen(), 200);
        t.sleep(50);
        shotNow("death-screen");
        t.act(() -> {
            cx = width() / 2f + 60;
            cy = height() - 60;
            moveTo(cx, cy, 1);
            cursor = true;
        });
        t.sleep(10);
        t.act(() -> moveToLine(0, 16));
        t.sleep(30);
        shotNow("death-hover");
        t.sleep(10);
        t.act(() -> moveTo(width() / 2f + 30, height() / 4f + 82, 14));
        t.sleep(20);
        t.act(() -> {
            cursor = false;
            t.port.respawn();
        });
        t.waitFor("alive", () -> t.port.health() > 0f && t.port.noScreen(), 100);
        t.sleep(30);
        shotNow("respawn-chat");
        t.sleep(10);
        stopRec();
        // hover of the summary in the chat screen
        openChat();
        t.act(() -> hoverChat(false));
        t.sleep(8);
        shotNow("chat-hover");
        t.act(() -> {
            cursor = false;
            mc().setScreen(null);
        });
        t.sleep(5);
    }

    private void deathMessage() {
        server(this::reset);
        t.sleep(30);
        t.act(t.port::clearChat);
        server(p -> p.hurt(p.damageSources().lava(), 100f));
        t.waitFor("death screen", () -> ClientCore.currentDeath != null && t.port.deathScreen(), 200);
        t.sleep(30);
        shotNow("death-message");
        respawn();
    }

    private void templates() {
        writePack("templates_media.json", "{\n  \"templates\": {\n"
            + "    \"summary\": [\n      \"" + L("&c☠ {killing_blow} &7({damage} ❤, {total} ❤ in {total_seconds}s)", "&c☠ {killing_blow} &7（{damage} ❤、{total_seconds} 秒で {total} ❤）") + "\",\n"
            + "      \"" + L("&a✎ {tip} &b{recap_link}", "&a✎ {tip} &b{recap_link}") + "\"\n    ],\n"
            + "    \"line\": [\"&f{name} &c{damage:0}❤ &7({max_hp_percent|?}%) &e{badges}\"]\n  }\n}\n");
        t.act(t.port::clearChat);
        server(this::reset);
        t.sleep(30);
        fight();
        t.waitFor("recap", () -> ClientCore.currentDeath != null && t.port.deathScreen(), 200);
        respawn();
        t.sleep(20);
        t.act(() -> t.port.command("/whm reload"));
        t.sleep(60);
        server(this::reset);
        t.sleep(30);
        fight();
        t.waitFor("recap", () -> ClientCore.currentDeath != null && t.port.deathScreen(), 200);
        t.sleep(30);
        shotNow("templates-panel");
        respawn();
        t.sleep(20);
        openChat();
        shotNow("templates-chat");
        t.act(() -> mc().setScreen(null));
        deletePack("templates_media.json");
        t.act(() -> t.port.command("/whm reload"));
        t.sleep(60);
    }

    private void rules(boolean ftb) {
        String done = ftb ? "/ftbquests change_progress @p complete " + FTB_QUEST : "/advancement grant @p only minecraft:nether/find_fortress";
        String undo = ftb ? "/ftbquests change_progress @p reset " + FTB_QUEST : "/advancement revoke @p only minecraft:nether/find_fortress";
        t.act(() -> t.port.say(undo));
        t.sleep(20);
        sentinelDeath();
        shotNow(ftb ? "rules-before-ftb" : "rules-before");
        respawn();
        t.act(() -> t.port.say(done));
        t.sleep(30);
        sentinelDeath();
        shotNow(ftb ? "rules-after-ftb" : "rules-after");
        respawn();
        t.act(() -> t.port.say(undo));
        t.sleep(10);
        server(this::reset);
        t.sleep(10);
    }

    static final String FTB_QUEST = "6D1E5A2C9B7F3048";

    private void sentinelDeath() {
        server(this::reset);
        server(p -> {
            ServerLevel level = p.serverLevel();
            if (sentinel != null) sentinel.discard();
            sentinel = EntityType.WITHER_SKELETON.create(level);
            sentinel.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
            prep(level, sentinel);
            place(sentinel, 2.4, 0.6);
        });
        t.sleep(30);
        t.act(t.port::clearChat);
        server(p -> {
            sentinel.swing(InteractionHand.MAIN_HAND);
            p.hurt(p.damageSources().mobAttack(sentinel), 7f);
        });
        t.sleep(16);
        server(p -> {
            sentinel.swing(InteractionHand.MAIN_HAND);
            p.invulnerableTime = 0;
            p.hurt(p.damageSources().mobAttack(sentinel), 100f);
        });
        t.waitFor("death screen", () -> ClientCore.currentDeath != null && t.port.deathScreen(), 200);
        t.sleep(30);
    }

    private void settings() {
        t.act(() -> mc().setScreen(new ConfigScreen121(null)));
        t.sleep(20);
        shotNow("settings");
        t.act(() -> mc().setScreen(null));
        t.sleep(5);
    }

    private void tools() {
        openCmd("/whm history");
        t.act(() -> hoverChat(true));
        t.sleep(8);
        shotNow("history");
        closeScreen();
        openCmd("/whm stats");
        shotNow("stats");
        closeScreen();
        openCmd("/whm search " + L("creeper", "creeper"));
        shotNow("search");
        closeScreen();
    }

    private void testCommand() {
        openCmd("/whm test minecraft:mob_attack minecraft:wither_skeleton --as Steve");
        shotNow("whm-test");
        closeScreen();
    }

    private void reloadCommand() {
        openCmd("/whm reload");
        t.sleep(20);
        shotNow("whm-reload");
        closeScreen();
    }

    // ---- template editor

    private TemplateEditor te() {
        return mc().screen instanceof TemplateEditorScreen121 ? ((TemplateEditorScreen121) mc().screen).editor : null;
    }

    private void templateEditor() {
        t.act(() -> t.port.command("/whm template"));
        t.waitFor("template editor", () -> te() != null, 60);
        t.sleep(10);
        t.act(() -> logAreas("template editor", te().area("lines") == null ? null : te()));
        startRec("template-editor");
        t.act(() -> {
            cx = width() / 2f;
            cy = height() / 2f;
            moveTo(cx, cy, 1);
            cursor = true;
        });
        click("tab:line", 14);
        t.sleep(14);
        click("field:0", 14);
        t.sleep(6);
        t.act(() -> {
            te().key(TemplateEditor.K_A, true, false);
            te().key(TemplateEditor.K_BACKSPACE, false, false);
        });
        t.sleep(10);
        typeText("&f", 3);
        click("var:name", 16);
        typeText(" &c-", 3);
        click("var:damage", 16);
        typeText(" ❤ &7(", 3);
        click("var:max_hp_percent", 16);
        typeText(L("% of max HP)", "% / 最大体力)"), 3);
        t.sleep(30);
        t.act(() -> cursor = false);
        shotNow("template-editor");
        t.sleep(6);
        stopRec();
        t.act(() -> mc().setScreen(null));
        t.sleep(5);
    }

    private void templateEditorLocked() {
        writePack("templates_locked_media.json", "{\n  \"lockedFeatures\": [\"templates.hover\"],\n  \"templates\": {\n    \"hover\": [\n"
            + "      \"&l{name}\",\n"
            + "      \"" + L("&cDamage: {damage:2} ❤ &7({mitigation}% mitigated)", "&cダメージ: {damage:2} ❤ &7（{mitigation}% 軽減）") + "\",\n"
            + "      \"" + L("&fAttacker: {mob_name|unknown}", "&f攻撃者: {mob_name|不明}") + "\",\n"
            + "      \"" + L("&7Owner: {attacker_owner|nobody}", "&7持ち主: {attacker_owner|なし}") + "\",\n"
            + "      \"&a{tip}\"\n    ]\n  }\n}\n");
        t.cmd("/whm reload", "Pack", 80);
        t.act(() -> t.port.command("/whm template"));
        t.waitFor("template editor", () -> te() != null, 60);
        t.sleep(5);
        t.act(() -> te().select("hover"));
        t.sleep(20);
        shotNow("template-editor-locked");
        t.act(() -> mc().setScreen(null));
        deletePack("templates_locked_media.json");
        t.cmd("/whm reload", "Pack", 80);
    }

    // ---- pack editor

    private static PackEditor pe() {
        return PackEditor.current;
    }

    private void openPack(String arg) {
        t.act(() -> t.port.command("/whm pack" + arg));
        t.waitFor("pack editor open", () -> t.port.packEditorOpen() && pe() != null && pe().loaded(), 200);
        t.sleep(10);
    }

    private void packEditor() {
        openPack("");
        t.act(() -> logAreas("pack renames", pe()));
        t.act(() -> {
            pe().selectTab("rules");
            pe().selectCategory("rules/bosses.json");
        });
        t.sleep(5);
        t.act(() -> {
            List<io.github.junjun1z.whathitme.core.pack.PackModel.Ent> es = pe().entries();
            if (!es.isEmpty()) pe().selectEntry(es.get(0));
        });
        t.sleep(10);
        t.act(() -> logAreas("pack rules", pe()));
        shotNow("pack-editor");
        // preview with "As" and an assumed condition
        t.act(() -> {
            pe().togglePreview(true);
            pe().setPreviewAs("Steve", "advancement:minecraft:nether/find_fortress");
            pe().runPreview();
        });
        t.sleep(40);
        shotNow("pack-editor-preview");
        t.act(() -> pe().togglePreview(false));
        // match key builder
        t.act(() -> pe().openKeyBuilder());
        t.sleep(5);
        click("kb.mobs", 1);
        t.sleep(5);
        t.act(() -> logAreas("key builder mobs", pe()));
        typeText("wither", 3);
        t.sleep(10);
        shotNow("pack-editor-keybuilder");
        t.act(() -> pe().dialogCancel());
        // condition picker (advancements)
        t.act(() -> pe().openConditionPicker());
        t.sleep(5);
        t.act(() -> logAreas("condition picker", pe()));
        t.act(() -> pe().dialogPick("advancement"));
        t.sleep(5);
        typeText("nether", 3);
        t.sleep(10);
        shotNow("pack-editor-condition");
        t.act(() -> pe().dialogCancel());
        // category memo
        t.act(() -> pe().openTreeAction("cat.memo"));
        t.sleep(10);
        shotNow("pack-editor-memo");
        t.act(() -> pe().dialogCancel());
        // server settings
        t.act(() -> {
            pe().selectTab("settings");
            pe().selectSetting("requireClientMod");
        });
        t.sleep(10);
        shotNow("pack-editor-settings");
        t.act(() -> mc().setScreen(null));
        t.sleep(5);
    }

    private void packAddRule() {
        openPack("");
        t.act(() -> {
            pe().selectTab("rules");
            pe().selectCategory("rules/bosses.json");
        });
        t.sleep(5);
        startRec("pack-editor-add-rule");
        t.act(() -> {
            cx = width() / 2f;
            cy = height() / 2f;
            moveTo(cx, cy, 1);
            cursor = true;
        });
        t.sleep(6);
        click("add", 14);
        t.sleep(8);
        click("f.key", 14);
        t.act(() -> {
            if (!pe().dialogOpen()) pe().openKeyBuilder();
        });
        t.sleep(8);
        click("kb.mobs", 12);
        t.sleep(4);
        typeText("blaze", 3);
        t.sleep(8);
        clickFirst("k[a-z]:0", 14);
        t.sleep(8);
        click("dlg.ok", 14);
        t.sleep(8);
        click("condadd:unless", 14);
        t.sleep(8);
        click("cp:advancement", 14);
        t.sleep(4);
        typeText("blaze_rod", 3);
        t.sleep(8);
        clickFirst("cv:0", 14);
        t.sleep(8);
        click("dlg.ok", 14);
        t.sleep(8);
        click("f.name", 14);
        typeText("???", 3);
        t.sleep(6);
        click("f.notip", 14);
        t.sleep(8);
        click("d.done", 16);
        t.sleep(40);
        t.act(() -> cursor = false);
        shotNow("pack-editor-added");
        stopRec();
        t.act(() -> logAreas("after add", pe()));
        t.act(() -> mc().setScreen(null));
        t.sleep(5);
    }

    private void packUndo() {
        openPack("");
        t.act(() -> {
            pe().selectTab("hints");
            pe().selectCategory("survival.json");
        });
        t.sleep(5);
        t.act(() -> {
            List<io.github.junjun1z.whathitme.core.pack.PackModel.Ent> es = pe().entries();
            if (!es.isEmpty()) pe().selectEntry(es.get(0));
        });
        t.sleep(5);
        startRec("pack-editor-undo");
        t.act(() -> {
            cx = width() / 2f;
            cy = height() / 2f;
            moveTo(cx, cy, 1);
            cursor = true;
        });
        t.sleep(10);
        click("d.delete", 16);
        t.sleep(14);
        click("dlg.ok", 14);
        t.sleep(40);
        click("undo", 18);
        t.sleep(50);
        t.act(() -> cursor = false);
        stopRec();
        t.act(() -> mc().setScreen(null));
        t.sleep(5);
    }

    private void packPersonal() {
        openPack(" personal");
        t.act(() -> pe().selectTab("renames"));
        t.sleep(5);
        String[][] own = {{"mob:minecraft:creeper", L("&aCreeper (run!)", "&aクリーパー（逃げろ！）")}, {"minecraft:fall", L("Gravity", "重力")},
            {"mob:minecraft:enderman", L("&5Tall Guy", "&5のっぽ")}};
        for (String[] o : own) {
            t.act(() -> {
                pe().newEntry();
                pe().field("key").text = o[0];
                pe().field("value").text = o[1];
            });
            t.sleep(3);
            t.act(() -> pe().done());
            t.sleep(15);
        }
        t.act(() -> {
            List<io.github.junjun1z.whathitme.core.pack.PackModel.Ent> es = pe().entries();
            if (!es.isEmpty()) pe().selectEntry(es.get(0));
        });
        t.sleep(10);
        shotNow("pack-editor-personal");
        // remove them again
        for (int i = 0; i < own.length; i++) {
            t.act(() -> {
                List<io.github.junjun1z.whathitme.core.pack.PackModel.Ent> es = pe().entries();
                if (!es.isEmpty()) {
                    pe().selectEntry(es.get(0));
                    pe().deleteEntry();
                }
            });
            t.sleep(5);
            t.act(() -> {
                if (pe().dialogOpen()) pe().dialogOk();
            });
            t.sleep(15);
        }
        t.act(() -> mc().setScreen(null));
        t.sleep(5);
    }

    private void conditionPicker() {
        openPack("");
        t.act(() -> {
            pe().selectTab("rules");
            pe().selectCategory("rules/bosses.json");
        });
        t.sleep(5);
        t.act(() -> {
            List<io.github.junjun1z.whathitme.core.pack.PackModel.Ent> es = pe().entries();
            if (!es.isEmpty()) pe().selectEntry(es.get(0));
        });
        t.act(() -> pe().openConditionPicker());
        t.sleep(5);
        click("cp:ftbquests", 1);
        t.sleep(20);
        t.act(() -> logAreas("cond ftb", pe()));
        shotNow("pack-editor-condition-ftbquests");
        t.act(() -> pe().dialogCancel());
        t.act(() -> mc().setScreen(null));
        t.sleep(5);
    }

    private void kick() {
        t.act(() -> {
            if (mc().level != null) mc().level.disconnect();
            mc().disconnect(new net.minecraft.client.gui.screens.GenericMessageScreen(Component.translatable("menu.savingLevel")));
            mc().setScreen(new TitleScreen());
        });
        t.waitFor("left the world", () -> mc().level == null, 600);
        t.sleep(20);
        t.act(() -> mc().setScreen(new DisconnectedScreen(new JoinMultiplayerScreen(new TitleScreen()), Component.translatable("disconnect.lost"),
            Component.literal("This server requires the What Hit Me? mod on your client (any version). Install it and join again."))));
        t.sleep(30);
        shotNow("require-client-mod");
    }

    // ------------------------------------------------------------------ fight

    private void fight() {
        server(p -> {
            zombie.swing(InteractionHand.MAIN_HAND);
            zombie.doHurtTarget(p);
        });
        t.sleep(18);
        server(this::shoot);
        t.sleep(16);
        server(p -> p.hurt(p.damageSources().fall(), 3f));
        t.sleep(16);
        server(p -> creeper.ignite());
        final int[] left = {140};
        t.then(() -> {
            if (t.port.deathScreen()) return true;
            if (--left[0] == 0) server2(p -> p.hurt(p.damageSources().explosion(creeper, creeper), 100f));
            return left[0] < -60;
        });
    }

    private void respawn() {
        t.act(() -> {
            cursor = false;
            if (t.port.deathScreen()) t.port.respawn();
        });
        t.waitFor("alive", () -> t.port.health() > 0f && t.port.noScreen(), 100);
        t.sleep(10);
    }

    // ------------------------------------------------------------------ helpers (screens)

    private static int width() {
        return mc().getWindow().getGuiScaledWidth();
    }

    private static int height() {
        return mc().getWindow().getGuiScaledHeight();
    }

    private void openChat() {
        t.act(() -> mc().setScreen(new ChatScreen("")));
        t.sleep(6);
    }

    private void openCmd(String cmd) {
        server(p -> p.connection.teleport(bx, by, bz, 180f, -5f));
        t.sleep(10);
        t.act(t.port::clearChat);
        t.act(() -> t.port.command(cmd));
        t.sleep(30);
        openChat();
    }

    private void closeScreen() {
        t.act(() -> {
            cursor = false;
            mc().setScreen(null);
        });
        t.sleep(4);
    }

    /** Puts the mouse over the first chat component with a hover text */
    private void hoverChat(boolean middle) {
        List<float[]> hits = new ArrayList<>();
        for (int y = height() - 200; y < height() - 14; y += 3) {
            for (int x = 4; x < 300; x += 3) {
                Style s = mc().gui.getChat().getClickedComponentStyleAt(x, y);
                if (s != null && s.getHoverEvent() != null && s.getHoverEvent().getAction() == net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT) {
                    hits.add(new float[] {x + 6, y});
                    break;
                }
            }
        }
        if (hits.isEmpty()) {
            t.info("no hover component in the chat");
            return;
        }
        float[] h = hits.get(middle ? hits.size() * 2 / 3 : 0);
        cx = h[0];
        cy = h[1];
        moveTo(cx, cy, 1);
        cursor = true;
    }

    private CoreScreen core() {
        return mc().screen instanceof TemplateEditorScreen121 ? ((TemplateEditorScreen121) mc().screen).screen : null;
    }

    private int[] area(String id) {
        CoreScreen c = core();
        if (c instanceof PackEditor) return ((PackEditor) c).area(id);
        if (c instanceof TemplateEditor) return ((TemplateEditor) c).area(id);
        return null;
    }

    private List<String> areaIds() {
        CoreScreen c = core();
        if (c instanceof PackEditor) return ((PackEditor) c).areaIds();
        return new ArrayList<>();
    }

    /** Moves the cursor to the area over the given ticks, then clicks it */
    private void click(String id, int ticks) {
        final int[][] a = {null};
        t.act(() -> {
            a[0] = area(id);
            if (a[0] == null) {
                t.info("no area " + id + " in " + areaIds());
                return;
            }
            moveTo((a[0][0] + a[0][2]) / 2f, (a[0][1] + a[0][3]) / 2f, ticks);
        });
        t.sleep(ticks + 2);
        t.act(() -> {
            if (a[0] == null || !(mc().screen instanceof TemplateEditorScreen121)) return;
            mc().screen.mouseClicked((a[0][0] + a[0][2]) / 2.0, (a[0][1] + a[0][3]) / 2.0, 0);
        });
    }

    /** Clicks the first area whose id starts with one of the prefixes */
    private void clickFirst(String regex, int ticks) {
        final String[] id = {null};
        t.act(() -> {
            for (String s : areaIds()) {
                if (s.matches(regex) && !s.startsWith("kd:")) {
                    id[0] = s;
                    break;
                }
            }
            if (id[0] == null) t.info("no area " + regex + " in " + areaIds());
        });
        t.then(() -> true);
        final int[][] a = {null};
        t.act(() -> {
            if (id[0] == null) return;
            a[0] = area(id[0]);
            if (a[0] != null) moveTo((a[0][0] + a[0][2]) / 2f, (a[0][1] + a[0][3]) / 2f, ticks);
        });
        t.sleep(ticks + 2);
        t.act(() -> {
            if (a[0] != null && mc().screen instanceof TemplateEditorScreen121) mc().screen.mouseClicked((a[0][0] + a[0][2]) / 2.0, (a[0][1] + a[0][3]) / 2.0, 0);
        });
    }

    private void typeText(String s, int ticksPerChar) {
        final int[] i = {0};
        final int[] w = {0};
        t.then(() -> {
            if (++w[0] < ticksPerChar) return false;
            w[0] = 0;
            if (i[0] >= s.length()) return true;
            Screen sc = mc().screen;
            if (sc != null) sc.charTyped(s.charAt(i[0]), 0);
            i[0]++;
            return false;
        });
    }

    private void logAreas(String where, Object core) {
        StringBuilder sb = new StringBuilder(where + " areas:");
        for (String id : areaIds()) {
            int[] a = area(id);
            sb.append(' ').append(id).append(a == null ? "" : "[" + a[0] + "," + a[1] + "," + a[2] + "," + a[3] + "]");
        }
        t.info(sb.toString());
    }

    private void moveToLine(int line, int ticks) {
        try {
            float sc = (Float) panelField("sc").get(DeathPanel.INSTANCE);
            int px = (Integer) panelField("px").get(DeathPanel.INSTANCE);
            int py = (Integer) panelField("py").get(DeathPanel.INSTANCE);
            int top = (Integer) panelField("linesTop").get(DeathPanel.INSTANCE);
            moveTo(px + 150 * sc, py + (top + line * 10 + 4) * sc, ticks);
        } catch (Exception e) {
            t.info("panel layout: " + e);
        }
    }

    private static Field panelField(String n) throws Exception {
        Field f = DeathPanel.class.getDeclaredField(n);
        f.setAccessible(true);
        return f;
    }

    private void moveTo(float x, float y, int ticks) {
        fromX = cx;
        fromY = cy;
        toX = x;
        toY = y;
        moveTotal = moveLeft = Math.max(1, ticks);
    }

    // ------------------------------------------------------------------ pack files

    private static File packDir() {
        return new File(io.github.junjun1z.whathitme.core.Platform.whmConfigDir(), "pack");
    }

    private void writePack(String name, String text) {
        t.act(() -> {
            try {
                Files.write(new File(packDir(), name).toPath(), text.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                t.ok("write " + name, false, e);
            }
        });
    }

    private void deletePack(String name) {
        t.act(() -> new File(packDir(), name).delete());
    }

    // ------------------------------------------------------------------ world / server

    private void openWorld(String folder) {
        if (new File(new File(t.port.gameDir(), "saves"), folder + "/level.dat").exists()) {
            mc().createWorldOpenFlows().openWorld(folder, () -> {});
            return;
        }
        LevelSettings ls = new LevelSettings(folder, GameType.SURVIVAL, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
        mc().createWorldOpenFlows().createFreshLevel(folder, ls, new WorldOptions(20261119L, false, false),
            access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.NORMAL).value().createWorldDimensions(), mc().screen);
    }

    /** Shows the player as "Steve": the profile objects of this run get that name (dev run only) */
    private void hideName() {
        try {
            Map<GameProfile, Boolean> ps = new IdentityHashMap<>();
            ps.put(mc().getGameProfile(), true);
            if (mc().player != null) ps.put(mc().player.getGameProfile(), true);
            MinecraftServer s = mc().getSingleplayerServer();
            if (s != null) {
                if (s.getSingleplayerProfile() != null) ps.put(s.getSingleplayerProfile(), true);
                for (ServerPlayer p : s.getPlayerList().getPlayers()) ps.put(p.getGameProfile(), true);
            }
            if (mc().getConnection() != null) {
                ps.put(mc().getConnection().getLocalGameProfile(), true);
                net.minecraft.client.multiplayer.PlayerInfo pi = mc().getConnection().getPlayerInfo(mc().player.getUUID());
                if (pi != null) ps.put(pi.getProfile(), true);
            }
            Field f = GameProfile.class.getDeclaredField("name");
            f.setAccessible(true);
            for (GameProfile gp : ps.keySet()) if (gp != null) f.set(gp, "Steve");
            t.info("profile name now " + t.port.playerName() + " (" + ps.size() + " profiles)");
        } catch (Throwable e) {
            t.info("hide name failed: " + e);
        }
    }

    private interface PlayerAct {
        void run(ServerPlayer p);
    }

    private void server(PlayerAct a) {
        t.act(() -> server2(a));
    }

    private void server2(PlayerAct a) {
        MinecraftServer s = mc().getSingleplayerServer();
        s.execute(() -> a.run(s.getPlayerList().getPlayers().get(0)));
    }

    private void setup(ServerPlayer p) {
        MinecraftServer s = p.getServer();
        ServerLevel level = p.serverLevel();
        s.setDifficulty(Difficulty.NORMAL, true);
        GameRules r = level.getGameRules();
        r.getRule(GameRules.RULE_DAYLIGHT).set(false, s);
        r.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, s);
        r.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, s);
        r.getRule(GameRules.RULE_KEEPINVENTORY).set(true, s);
        r.getRule(GameRules.RULE_MOBGRIEFING).set(false, s);
        r.getRule(GameRules.RULE_SPAWN_RADIUS).set(0, s);
        r.getRule(GameRules.RULE_SHOWDEATHMESSAGES).set(true, s);
        r.getRule(GameRules.RULE_ANNOUNCE_ADVANCEMENTS).set(false, s);
        level.setDayTime(4000);
        level.setWeatherParameters(100000, 0, false, false);
        p.setGameMode(GameType.SURVIVAL);
        p.removeAllEffects();
        p.getInventory().clearContent();
        BlockPos at = p.blockPosition();
        if (!new File(t.port.gameDir(), "saves/whm_media/whm_arena").exists()) {
            try {
                Pair<BlockPos, Holder<Biome>> b = level.findClosestBiome3d(h -> h.is(Biomes.CHERRY_GROVE), at, 6400, 32, 64);
                if (b != null) at = b.getFirst();
            } catch (Throwable e) {
                t.info("biome search: " + e);
            }
        } else {
            try {
                String[] xyz = new String(Files.readAllBytes(new File(t.port.gameDir(), "saves/whm_media/whm_arena").toPath()), StandardCharsets.UTF_8).trim().split(" ");
                at = new BlockPos(Integer.parseInt(xyz[0]), Integer.parseInt(xyz[1]), Integer.parseInt(xyz[2]));
            } catch (Exception e) {
                t.info("arena file: " + e);
            }
        }
        level.getChunk(at.getX() >> 4, at.getZ() >> 4);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ());
        // a small flat clearing: grass floor, air above
        for (int dx = -4; dx <= 13; dx++) {
            for (int dz = -8; dz <= 5; dz++) {
                BlockPos fl = new BlockPos(at.getX() + dx, y - 1, at.getZ() + dz);
                level.getChunk(fl.getX() >> 4, fl.getZ() >> 4);
                level.setBlockAndUpdate(fl, Blocks.GRASS_BLOCK.defaultBlockState());
                level.setBlockAndUpdate(fl.below(), Blocks.DIRT.defaultBlockState());
                for (int dy = 1; dy <= 6; dy++) level.setBlockAndUpdate(fl.above(dy), Blocks.AIR.defaultBlockState());
            }
        }
        try {
            Files.write(new File(t.port.gameDir(), "saves/whm_media/whm_arena").toPath(), (at.getX() + " " + y + " " + at.getZ()).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            t.info("arena file: " + e);
        }
        bx = at.getX() + 0.5;
        by = y;
        bz = at.getZ() + 0.5;
        level.setDefaultSpawnPos(BlockPos.containing(bx, by, bz), -90f);
        p.setRespawnPosition(level.dimension(), BlockPos.containing(bx, by, bz), -90f, true, false);
        p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        p.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        p.getInventory().add(new ItemStack(Items.BREAD, 12));
        p.getInventory().add(new ItemStack(Items.TORCH, 24));
        p.getInventory().add(new ItemStack(Items.OAK_PLANKS, 48));
        skeleton = EntityType.SKELETON.create(level);
        skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        skeleton.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        skeleton.setDropChance(EquipmentSlot.HEAD, 0f);
        prep(level, skeleton);
        t.info("arena at " + bx + " " + by + " " + bz);
    }

    private static void prep(ServerLevel level, Mob m) {
        m.setNoAi(true);
        m.setPersistenceRequired();
        m.setDropChance(EquipmentSlot.MAINHAND, 0f);
        level.addFreshEntity(m);
    }

    /** Player, mobs and a new creeper at their places (also after the respawn) */
    private void reset(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        p.clearFire();
        p.removeAllEffects();
        p.setHealth(p.getMaxHealth());
        p.getFoodData().setFoodLevel(20);
        p.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
        p.getAttribute(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE).setBaseValue(1.0);
        p.connection.teleport(bx, by, bz, -90f, 8f);
        if (sentinel != null) {
            sentinel.discard();
            sentinel = null;
        }
        if (zombie == null || !zombie.isAlive()) {
            if (zombie != null) zombie.discard();
            zombie = EntityType.ZOMBIE.create(level);
            ItemStack sword = new ItemStack(Items.IRON_SWORD);
            sword.enchant(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SHARPNESS), 3);
            zombie.setItemSlot(EquipmentSlot.MAINHAND, sword);
            zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            zombie.setDropChance(EquipmentSlot.HEAD, 0f);
            prep(level, zombie);
            zombie.setInvulnerable(true);
        }
        zombie.setHealth(zombie.getMaxHealth());
        place(zombie, 2.4, -1.2);
        if (skeleton != null) place(skeleton, 10.0, -6.0);
        if (creeper != null && creeper.isAlive()) creeper.discard();
        creeper = EntityType.CREEPER.create(level);
        prep(level, creeper);
        place(creeper, 3.0, 1.8);
    }

    /** Facing east: dx forward, dz to the right */
    private void place(Mob m, double dx, double dz) {
        double x = bx + dx, z = bz + dz;
        float yaw = (float) Math.toDegrees(Math.atan2(bz - z, bx - x)) - 90f;
        m.moveTo(x, by, z, yaw, 0f);
        m.setYHeadRot(yaw);
        m.setYBodyRot(yaw);
        m.setDeltaMovement(0, 0, 0);
    }

    private void shoot(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        skeleton.swing(InteractionHand.MAIN_HAND);
        Arrow a = new Arrow(level, skeleton, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
        p.hurt(p.damageSources().arrow(a, skeleton), 4f);
    }

    // ------------------------------------------------------------------ stills and recording

    private void shot(String name) {
        t.sleep(4);
        shotNow(name);
    }

    private void shotNow(String name) {
        t.act(() -> {
            mc().getToasts().clear();
            mc().particleEngine.setLevel(mc().level);
        });
        t.then(() -> true);
        t.act(() -> {
            Screenshot.grab(t.port.gameDir(), "media_" + name + ".png", mc().getMainRenderTarget(), msg -> {});
        });
    }

    private void startRec(String name) {
        t.act(() -> {
            recDir = new File(root, name);
            recDir.mkdirs();
            File[] old = recDir.listFiles();
            if (old != null) for (File f : old) f.delete();
            pool = Executors.newFixedThreadPool(3);
            frame = 0;
            marks.clear();
        });
    }

    private void stopRec() {
        t.act(() -> {
            File d = recDir;
            recDir = null;
            pool.shutdown();
            try {
                pool.awaitTermination(60, TimeUnit.SECONDS);
                Files.write(new File(d, "marks.txt").toPath(), marks, StandardCharsets.UTF_8);
            } catch (Exception e) {
                t.info("recording: " + e);
            }
            t.ok("recorded " + d.getName() + ": " + frame + " frames", frame > 0, null);
        });
    }

    /** End of every client tick (after the plan step) */
    public void tick() {
        Minecraft mc = mc();
        mc.getToasts().clear();
        if (moveLeft > 0) {
            moveLeft--;
            float k = 1f - moveLeft / (float) moveTotal;
            k = k * k * (3 - 2 * k);
            cx = fromX + (toX - fromX) * k;
            cy = fromY + (toY - fromY) * k;
        }
        if (cursor) setMouse(cx, cy);
        if (recDir == null) return;
        NativeImage img = Screenshot.takeScreenshot(mc.getMainRenderTarget());
        File out = new File(recDir, String.format("f_%05d.png", frame++));
        pool.submit(() -> {
            try {
                img.writeToFile(out);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                img.close();
            }
        });
    }

    private void setMouse(float gx, float gy) {
        try {
            MouseHandler m = mc().mouseHandler;
            double s = mc().getWindow().getGuiScale();
            if (mouseX == null) {
                for (Field f : MouseHandler.class.getDeclaredFields()) {
                    if (f.getType() != double.class || Modifier.isStatic(f.getModifiers())) continue;
                    f.setAccessible(true);
                    double old = f.getDouble(m);
                    f.setDouble(m, 12345.0);
                    if (m.xpos() == 12345.0) mouseX = f;
                    else if (m.ypos() == 12345.0) mouseY = f;
                    f.setDouble(m, old);
                }
            }
            mouseX.setDouble(m, gx * s);
            mouseY.setDouble(m, gy * s);
        } catch (Exception e) {
            t.info("mouse: " + e);
            cursor = false;
        }
    }

    /** Drawn after every screen: a mouse pointer (the real one is not in the framebuffer) */
    public void drawCursor(GuiGraphics g) {
        if (!cursor) return;
        String[] art = {
            "B", "BB", "BWB", "BWWB", "BWWWB", "BWWWWB", "BWWWWWB", "BWWWWWWB", "BWWWWWWWB", "BWWWWWBBBB", "BWWBWWB", "BWB BWWB", "BB  BWWB",
            "B    BWWB", "     BWWB", "      BB"};
        int x0 = Math.round(cx), y0 = Math.round(cy);
        g.pose().pushPose();
        g.pose().translate(0, 0, 900f);
        for (int r = 0; r < art.length; r++) {
            for (int c = 0; c < art[r].length(); c++) {
                char ch = art[r].charAt(c);
                if (ch == ' ') continue;
                g.fill(x0 + c, y0 + r, x0 + c + 1, y0 + r + 1, ch == 'B' ? 0xFF000000 : 0xFFFFFFFF);
            }
        }
        g.pose().popPose();
    }
}
