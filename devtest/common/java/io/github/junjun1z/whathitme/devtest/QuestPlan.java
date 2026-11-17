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
        Field f = o.getClass().getField(name);
        return f.get(o);
    }

    /** The code string of the first FTB Quests quest ("" = none) */
    static String firstFtbQuest() {
        try {
            Class<?> modern = cls("dev.ftb.mods.ftbquests.quest.ServerQuestFile");
            if (modern != null) {
                Object file = modern.getField("INSTANCE").get(null);
                List<Object> qs = new ArrayList<>();
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

    /** Creates Better Questing quest BQ_ID (no tasks) */
    static String createBqQuest() {
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

    void plan() {
        File rules = new File(Platform.whmConfigDir(), "pack/testq/rules.json");
        t.loadWorld("whm_quests");
        t.waitFor("server info received (server has the mod)", () -> ClientCore.serverHasMod, 400);
        t.srv(DevPort.Act.SETUP);
        t.sleep(40);
        t.act(() -> t.info("mods: stage=" + mod("gamestages") + " bq=" + mod("betterquesting") + " ftbquests=" + mod("ftbquests")));
        // ---- FTB Quests: a chapter with quests, its first quest id
        if (mod("ftbquests")) {
            if (modern) {
                vanilla(() -> "/ftbquests generate_chapter from_entire_creative_list", 100);
            } else {
                vanilla(() -> "/ftbquests generate_chapter_with_all_items_in_game", 100);
            }
            t.act(() -> {
                ftbId = firstFtbQuest();
                t.info("ftb quest id: " + ftbId + " chat: " + t.chatText().replace('\n', '|'));
            });
            t.check("FTB Quests: a quest exists", () -> !ftbId.isEmpty() && !ftbId.startsWith("error"), (Supplier<?>) () -> ftbId);
            // generated quests can start completed (items already owned): start from a reset
            vanilla(() -> modern ? "/ftbquests change_progress @p reset " + ftbId : "/ftbquests change_progress reset " + legacyTeam(p.playerId()) + " " + ftbId, 40);
        }
        if (mod("betterquesting")) t.act(() -> t.info("bq quest: " + createBqQuest()));
        t.act(() -> {
            StringBuilder r = new StringBuilder("{\"rules\": [\n");
            String gen = SelfTest.id("generic");
            r.append("{\"match\": \"").append(gen).append("\", \"if\": [\"stage:whmt_stage\"], \"name\": \"WHMT-STAGE\"},\n");
            r.append("{\"match\": \"").append(gen).append("\", \"if\": [\"bq:").append(BQ_ID).append("\"], \"name\": \"WHMT-BQ\"},\n");
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
            && (!mod("ftbquests") || !has("FTB Quests is not installed")) && (!mod("betterquesting") || !has("Better Questing is not installed")),
            (Supplier<?>) t::chatText);
        test("Rule: none matches", "nothing done yet: no rule", true);
        if (mod("gamestages")) {
            vanilla(() -> "/gamestage add " + p.playerName() + " whmt_stage", 20);
            test("Name: WHMT-STAGE", "Game Stages: stage:whmt_stage", true);
            vanilla(() -> "/gamestage remove " + p.playerName() + " whmt_stage", 20);
            test("WHMT-STAGE", "Game Stages: stage removed", false);
        }
        if (mod("betterquesting")) {
            vanilla(() -> "/bq_admin complete " + BQ_ID + " " + p.playerName(), 30);
            test("Name: WHMT-BQ", "Better Questing: bq:" + BQ_ID + " completed", true);
            vanilla(() -> "/bq_admin reset " + BQ_ID + " " + p.playerName(), 30);
            test("WHMT-BQ", "Better Questing: quest reset", false);
        }
        if (mod("ftbquests")) {
            vanilla(() -> modern ? "/ftbquests change_progress @p complete " + ftbId : "/ftbquests change_progress complete " + legacyTeam(p.playerId()) + " " + ftbId, 40);
            t.act(() -> t.info("change_progress chat: " + t.chatText().replace('\n', '|')));
            test("Name: WHMT-FTBQ", "FTB Quests: ftbquests:<id> completed", true);
            vanilla(() -> modern ? "/ftbquests change_progress @p reset " + ftbId : "/ftbquests change_progress reset " + legacyTeam(p.playerId()) + " " + ftbId, 40);
            test("WHMT-FTBQ", "FTB Quests: progress reset", false);
        }
        t.act(() -> {
            rules.delete();
            rules.getParentFile().delete();
        });
        t.cmd("/whm reload", "reloaded", 60);
        t.waitFor("rules gone", () -> WhmConfig.pack.rules.isEmpty(), 60);
    }
}
