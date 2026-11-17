package io.github.junjun1z.whathitme.devtest;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.client.ClientCore;
import io.github.junjun1z.whathitme.core.config.WhmConfig;
import io.github.junjun1z.whathitme.core.text.Fmt;
import io.github.junjun1z.whmdriver.VanillaCommands;

/**
 * Quest mod condition providers in single player (flag "quests"): Game Stages, Better Questing, FTB Quests, whichever are
 * installed. Each gets a rule on the generic damage id and is checked with /whm test before and after the progress.
 */
final class QuestPlan {

    static final int BQ_ID = 9001;
    /** Quest id of the GTNH fork (1.7.10), where quest ids are UUIDs */
    static final java.util.UUID BQ_UUID = java.util.UUID.fromString("6c1f6a3e-9b0d-4c55-8f7e-1d2a3b4c5d6e");

    private final SelfTest t;
    private final DevPort p;
    private final boolean modern;
    /** FTB Quests quest id found in game */
    private String ftbId = "";

    QuestPlan(SelfTest t) {
        this.t = t;
        this.p = t.port;
        this.modern = VanillaCommands.of(p.mcVersion()).atLeast(1, 13, 0);
    }

    private static boolean mod(String id) {
        return Platform.mods.modIds().contains(id);
    }

    private boolean has(String text) {
        return Fmt.strip(t.chatText()).contains(text);
    }

    private void vanilla(Supplier<String> cmd, int wait) {
        t.act(() -> {
            String c = cmd.get();
            t.info("send " + c);
            p.say(c);
        });
        t.sleep(wait);
    }

    private void test(String expect, String name, boolean present) {
        t.clearChat();
        t.act(() -> p.command("/whm test " + SelfTest.id("generic")));
        t.waitFor("/whm test", () -> has("Chat preview"), 80);
        t.check(name, () -> has(expect) == present, (Supplier<?>) () -> t.chatText());
    }

    private static Object field(Object o, String name) throws Exception {
        try {
            return o.getClass().getField(name).get(o);
        } catch (NoSuchFieldException e) {
            // 1902: private list with a getter
            Field f = o.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(o);
        }
    }

    /** ServerQuestFile.INSTANCE, or getInstance() since 2111.x */
    private static Object ftbFile(Class<?> c) throws Exception {
        try {
            return c.getField("INSTANCE").get(null);
        } catch (NoSuchFieldException e) {
            Object f = c.getMethod("getInstance").invoke(null);
            return f instanceof java.util.Optional ? ((java.util.Optional<?>) f).orElse(null) : f;
        }
    }

    /** The code string of the first FTB Quests quest ("" = none) */
    static String firstFtbQuest() {
        try {
            Class<?> modern = cls("dev.ftb.mods.ftbquests.quest.ServerQuestFile");
            if (modern != null) {
                Object file = ftbFile(modern);
                List<Object> qs = new ArrayList<>();
                if (!ftbForAll()) {
                    // 1.16.5 (1605.x): the generate command does not add its quests to the id map; getAllChapters() and their quests lists
                    file.getClass().getMethod("refreshIDMap").invoke(file);
                    for (Object ch : (List<?>) file.getClass().getMethod("getAllChapters").invoke(file)) {
                        for (Object q : (List<?>) field(ch, "quests")) return (String) q.getClass().getMethod("getCodeString").invoke(q);
                    }
                    return "";
                }
                // 1802 - 2004: the generate command does not add its quests to the id map either
                try {
                    file.getClass().getMethod("refreshIDMap").invoke(file);
                } catch (NoSuchMethodException ignored) {
                    // newer builds keep the map up to date
                }
                Method m = file.getClass().getMethod("forAllQuests", Consumer.class);
                m.invoke(file, (Consumer<Object>) qs::add);
                if (qs.isEmpty()) {
                    List<Object> ch = new ArrayList<>();
                    file.getClass().getMethod("forAllChapters", Consumer.class).invoke(file, (Consumer<Object>) ch::add);
                    return ch.isEmpty() ? "" : "no quests in " + ch.size() + " chapters";
                }
                return (String) qs.get(0).getClass().getMethod("getCodeString").invoke(qs.get(0));
            }
            Class<?> legacy = cls("com.feed_the_beast.ftbquests.quest.ServerQuestFile");
            if (legacy == null) return "";
            Object file = legacy.getField("INSTANCE").get(null);
            for (Object ch : (List<?>) field(file, "chapters")) {
                for (Object q : (List<?>) field(ch, "quests")) return (String) q.getClass().getMethod("getCodeString").invoke(q);
            }
        } catch (Throwable e) {
            return "error " + e;
        }
        return "";
    }

    /** FTB Quests 1.18+ (forAllQuests; generate_chapter from_entire_creative_list from 2101); 1.16.5 has neither */
    static boolean ftbForAll() {
        Class<?> c = cls("dev.ftb.mods.ftbquests.quest.ServerQuestFile");
        if (c == null) return false;
        for (Method m : c.getMethods()) if (m.getName().equals("forAllQuests")) return true;
        return false;
    }

    private static final String FTB_OLD = "com.feed_the_beast.ftbquests.quest.";

    /** FTB Quests 1.15.2 (1502.x): no generate command, change_progress only for the quest the player looks at */
    boolean ftb1502() {
        return modern && cls(FTB_OLD + "ServerQuestFile") != null;
    }

    private static void newId(Object o, Object file) throws Exception {
        o.getClass().getField("id").set(o, file.getClass().getMethod("newID").invoke(file));
        o.getClass().getMethod("onCreated").invoke(o);
    }

    /** FTB Quests 1.15.2: a chapter with one quest and an empty item task, made by reflection; the quest code */
    static String createFtb1502Quest() {
        try {
            Object file = cls(FTB_OLD + "ServerQuestFile").getField("INSTANCE").get(null);
            Class<?> chapter = cls(FTB_OLD + "Chapter"), quest = cls(FTB_OLD + "Quest");
            Object ch = chapter.getConstructor(cls(FTB_OLD + "QuestFile")).newInstance(file);
            newId(ch, file);
            Object q = quest.getConstructor(chapter).newInstance(ch);
            newId(q, file);
            newId(cls(FTB_OLD + "task.ItemTask").getConstructor(quest).newInstance(q), file);
            file.getClass().getMethod("refreshIDMap").invoke(file);
            return (String) q.getClass().getMethod("getCodeString").invoke(q);
        } catch (Throwable e) {
            return "error " + e;
        }
    }

    /** FTB Quests 1.15.2: quest.changeProgress(file.getData(player), COMPLETE / RESET) */
    static String ftb1502Progress(java.util.UUID player, String code, boolean complete) {
        try {
            Object file = cls(FTB_OLD + "ServerQuestFile").getField("INSTANCE").get(null);
            Object id = file.getClass().getMethod("getID", Object.class).invoke(file, "#" + code);
            Object q = file.getClass().getMethod("get", int.class).invoke(file, id);
            Object data = file.getClass().getMethod("getData", java.util.UUID.class).invoke(file, player);
            Class<?> cp = cls(FTB_OLD + "ChangeProgress");
            for (Method m : q.getClass().getMethods()) {
                if (m.getName().equals("changeProgress") && m.getParameterCount() == 2) {
                    m.invoke(q, data, cp.getField(complete ? "COMPLETE" : "RESET").get(null));
                    return "ok";
                }
            }
            return "no changeProgress";
        } catch (Throwable e) {
            return "error " + e;
        }
    }

    /** Completes / resets the FTB Quests quest ftbId for the player */
    private void ftbProgress(boolean complete) {
        if (ftb1502()) {
            t.act(() -> t.info("ftb 1502 progress " + complete + ": " + ftb1502Progress(p.playerId(), ftbId, complete)));
            t.sleep(20);
            return;
        }
        String c = complete ? "complete " : "reset ";
        vanilla(() -> modern ? "/ftbquests change_progress @p " + c + ftbId : "/ftbquests change_progress " + c + legacyTeam(p.playerId()) + " " + ftbId, 40);
    }

    /** FTB Quests 1.12.2: the team id of the player (FTB Library creates one per player) */
    static String legacyTeam(java.util.UUID player) {
        try {
            Class<?> legacy = cls("com.feed_the_beast.ftbquests.quest.ServerQuestFile");
            Object file = legacy.getField("INSTANCE").get(null);
            Object data = file.getClass().getMethod("getData", java.util.UUID.class).invoke(file, player);
            return data == null ? "" : String.valueOf(data.getClass().getMethod("getTeamID").invoke(data));
        } catch (Throwable e) {
            return "";
        }
    }

    private static Class<?> cls(String n) {
        try {
            return Class.forName(n);
        } catch (Throwable e) {
            return null;
        }
    }

    /** The GTNH fork of Better Questing (UUID quest ids) */
    static boolean bqUuids() {
        return cls("betterquesting.api2.storage.IUuidDatabase") != null;
    }

    /** The quest id as Better Questing's commands write it (GTNH: Base64 of the UUID) */
    static String bqCommandId() {
        if (!bqUuids()) return String.valueOf(BQ_ID);
        try {
            return (String) cls("betterquesting.api.utils.UuidConverter").getMethod("encodeUuid", java.util.UUID.class).invoke(null, BQ_UUID);
        } catch (Throwable e) {
            return "error " + e;
        }
    }

    /** Creates Better Questing quest BQ_ID / BQ_UUID (no tasks) */
    static String createBqQuest() {
        if (bqUuids()) {
            try {
                Object db = Class.forName("betterquesting.api.api.QuestingAPI").getMethod("getAPI", Class.forName("betterquesting.api.api.ApiKey"))
                    .invoke(null, Class.forName("betterquesting.api.api.ApiReference").getField("QUEST_DB").get(null));
                if (((java.util.Map<?, ?>) db).get(BQ_UUID) != null) return "exists";
                db.getClass().getMethod("createNew", java.util.UUID.class).invoke(db, BQ_UUID);
                return "created " + BQ_UUID;
            } catch (Throwable e) {
                return "error " + e;
            }
        }
        try {
            Class<?> api = Class.forName("betterquesting.api.api.QuestingAPI");
            Object key = Class.forName("betterquesting.api.api.ApiReference").getField("QUEST_DB").get(null);
            Object db = null;
            for (Method m : api.getMethods()) if (m.getName().equals("getAPI")) db = m.invoke(null, key);
            for (Method m : db.getClass().getMethods()) {
                if (m.getName().equals("getValue") && m.getParameterCount() == 1 && m.getParameterTypes()[0] == int.class && m.invoke(db, BQ_ID) != null) return "exists";
            }
            for (Method m : db.getClass().getMethods()) {
                if (m.getName().equals("createNew") && m.getParameterCount() == 1 && m.getParameterTypes()[0] == int.class) {
                    m.invoke(db, BQ_ID);
                    return "created";
                }
            }
            return "no createNew";
        } catch (Throwable e) {
            return "error " + e;
        }
    }

    private static final String HER_ID = "whmt_quest";

    void plan() {
        File rules = new File(Platform.whmConfigDir(), "pack/testq/rules.json");
        // Heracles loads config/heracles/quests/*.json with the world; an empty object is a quest with defaults
        if (mod("heracles")) t.act(() -> {
            try {
                File q = new File(Platform.configDir, "heracles/quests/" + HER_ID + ".json");
                q.getParentFile().mkdirs();
                Files.write(q.toPath(), "{}".getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        // the first client resource reload must finish first (FTB Quests 2001 Fabric: theme loaded there, NPE on sync otherwise)
        t.sleep(300);
        t.loadWorld("whm_quests");
        t.waitFor("server info received (server has the mod)", () -> ClientCore.serverHasMod, 400);
        t.srv(DevPort.Act.SETUP);
        t.sleep(40);
        t.act(() -> t.info("mods: stage=" + mod("gamestages") + " bq=" + mod("betterquesting") + " ftbquests=" + mod("ftbquests") + " heracles=" + mod("heracles")));
        // ---- FTB Quests: a chapter with quests, its first quest id
        if (mod("ftbquests")) {
            if (modern && ftbForAll()) {
                t.clearChat();
                vanilla(() -> "/ftbquests generate_chapter from_entire_creative_list", 100);
                // 1802 - 2004 still have the old command name
                t.act(() -> {
                    if (has("Incorrect argument") || has("Unknown")) {
                        t.info("send /ftbquests generate_chapter_with_all_items_in_game");
                        p.say("/ftbquests generate_chapter_with_all_items_in_game");
                    }
                });
                t.sleep(100);
            } else if (ftb1502()) {
                t.act(() -> t.info("ftb 1502 quest: " + createFtb1502Quest()));
            } else {
                vanilla(() -> "/ftbquests generate_chapter_with_all_items_in_game", 100);
            }
            t.act(() -> {
                ftbId = firstFtbQuest();
                t.info("ftb quest id: " + ftbId + " chat: " + t.chatText().replace('\n', '|'));
            });
            t.check("FTB Quests: a quest exists", () -> !ftbId.isEmpty() && !ftbId.startsWith("error"), (Supplier<?>) () -> ftbId);
            // generated quests can start completed (items already owned): start from a reset
            ftbProgress(false);
        }
        if (mod("betterquesting")) t.act(() -> t.info("bq quest: " + createBqQuest()));
        t.act(() -> {
            StringBuilder r = new StringBuilder("{\"rules\": [\n");
            String gen = SelfTest.id("generic");
            r.append("{\"match\": \"").append(gen).append("\", \"if\": [\"stage:whmt_stage\"], \"name\": \"WHMT-STAGE\"},\n");
            r.append("{\"match\": \"").append(gen).append("\", \"if\": [\"bq:").append(bqUuids() ? BQ_UUID.toString() : String.valueOf(BQ_ID)).append("\"], \"name\": \"WHMT-BQ\"},\n");
            // GTNH: the Base64 form the quest book and commands show
            if (bqUuids()) r.append("{\"match\": \"").append(SelfTest.id("magic")).append("\", \"if\": [\"bq:").append(bqCommandId()).append("\"], \"name\": \"WHMT-BQ64\"},\n");
            r.append("{\"match\": \"").append(gen).append("\", \"if\": [\"heracles:").append(HER_ID).append("\"], \"name\": \"WHMT-HER\"},\n");
            r.append("{\"match\": \"").append(gen).append("\", \"if\": [\"ftbquests:").append(ftbId.isEmpty() ? "0" : ftbId).append("\"], \"name\": \"WHMT-FTBQ\"}\n");
            r.append("]}\n");
            try {
                rules.getParentFile().mkdirs();
                Files.write(rules.toPath(), r.toString().getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        t.cmd("/whm reload", "reloaded", 60);
        t.act(() -> t.info("reload chat: " + t.chatText().replace('\n', '|')));
        t.check("no provider warnings for installed mods", () -> (!mod("gamestages") || !has("Game Stages is not installed")) && !has("provider failed")
            && (!mod("ftbquests") || !has("FTB Quests is not installed")) && (!mod("betterquesting") || !has("Better Questing is not installed")) && (!mod("heracles") || !has("Heracles API")),
            (Supplier<?>) t::chatText);
        if (mod("heracles")) vanilla(() -> "/heracles reset " + HER_ID + " " + p.playerName(), 30);
        test("Rule: none matches", "nothing done yet: no rule", true);
        if (mod("gamestages")) {
            vanilla(() -> "/gamestage add " + p.playerName() + " whmt_stage", 20);
            test("Name: WHMT-STAGE", "Game Stages: stage:whmt_stage", true);
            vanilla(() -> "/gamestage remove " + p.playerName() + " whmt_stage", 20);
            test("WHMT-STAGE", "Game Stages: stage removed", false);
        }
        if (mod("betterquesting")) {
            vanilla(() -> "/bq_admin complete " + bqCommandId() + " " + p.playerName(), 30);
            t.act(() -> t.info("bq complete chat: " + t.chatText().replace('\n', '|')));
            test("Name: WHMT-BQ", "Better Questing: bq:<id> completed", true);
            if (bqUuids()) {
                t.clearChat();
                t.act(() -> p.command("/whm test " + SelfTest.id("magic")));
                t.waitFor("/whm test magic", () -> has("Chat preview"), 80);
                t.check("Better Questing (GTNH): bq:<Base64 id> completed", () -> has("Name: WHMT-BQ64"), (Supplier<?>) () -> t.chatText());
            }
            vanilla(() -> "/bq_admin reset " + bqCommandId() + " " + p.playerName(), 30);
            test("WHMT-BQ", "Better Questing: quest reset", false);
        }
        if (mod("ftbquests")) {
            ftbProgress(true);
            t.act(() -> t.info("change_progress chat: " + t.chatText().replace('\n', '|')));
            test("Name: WHMT-FTBQ", "FTB Quests: ftbquests:<id> completed", true);
            ftbProgress(false);
            test("WHMT-FTBQ", "FTB Quests: progress reset", false);
        }
        if (mod("heracles")) {
            vanilla(() -> "/heracles complete " + HER_ID + " " + p.playerName(), 30);
            t.act(() -> t.info("heracles complete chat: " + t.chatText().replace('\n', '|')));
            test("Name: WHMT-HER", "Heracles: heracles:<id> completed", true);
            vanilla(() -> "/heracles reset " + HER_ID + " " + p.playerName(), 30);
            test("WHMT-HER", "Heracles: quest reset", false);
        }
        t.act(() -> {
            rules.delete();
            rules.getParentFile().delete();
        });
        t.cmd("/whm reload", "reloaded", 60);
        t.waitFor("rules gone", () -> WhmConfig.pack.rules.isEmpty(), 60);
    }
}
