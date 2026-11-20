package dev.sidequests.core;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.io.InputStreamReader;
import static org.junit.jupiter.api.Assertions.*;

class QuestEngineTest {
    private static final String UUID = "0123456789abcdef0123456789abcdef";
    private static JsonObject json(String s) { return JsonParser.parseString(s).getAsJsonObject(); }
    private static JsonObject definitions() {
        return json("""
          {"collections":{"MINING":{"items":{"COAL":{"name":"Coal","tiers":[
            {"tier":1,"amountRequired":50,"unlocks":["Coal minion"]},
            {"tier":2,"amountRequired":100,"unlocks":["Next recipe"]}]}}}}}
          """);
    }
    private static JsonObject profile(String member) {
        return json("{\"members\":{\"" + UUID + "\":" + member + "}}");
    }
    private static QuestEngine.Board board(JsonObject profile) {
        return QuestEngine.generate(profile, UUID, definitions(), new JsonObject(), QuestEngine.Focus.COMPLETION);
    }
    @Test void findsFirstUnreachedTierAndExactRemainingAmount() {
        Quest q = board(profile("{\"collection\":{\"COAL\":80}}")).quests().getFirst();
        assertEquals("Coal 2", q.title());
        assertEquals("20", q.remaining());
        assertEquals(.8, q.progress(), .0001);
        assertEquals("Next recipe", q.detail());
    }
    @Test void exactThresholdAdvancesToNextTier() {
        Quest q = board(profile("{\"collection\":{\"COAL\":50}}")).quests().getFirst();
        assertEquals(100, q.target());
    }
    @Test void maximumCollectionDoesNotGenerateImpossibleGoal() {
        assertTrue(board(profile("{\"collection\":{\"COAL\":100}}")).quests().isEmpty());
    }
    @Test void coopContributionsAreAdded() {
        JsonObject p = profile("{\"collection\":{\"COAL\":30}}");
        p.getAsJsonObject("members").add("peer", json("{\"collection\":{\"COAL\":25}}"));
        Quest q = board(p).quests().getFirst();
        assertEquals(55, q.current());
        assertEquals(100, q.target());
    }
    @Test void missingCoopDataDoesNotInventAnUnlock() {
        JsonObject p = profile("{\"collection\":{\"COAL\":30}}");
        p.getAsJsonObject("members").add("peer", json("{}"));
        assertTrue(board(p).quests().isEmpty());
        assertTrue(board(p).notices().stream().anyMatch(s -> s.contains("Co-op")));
    }
    @Test void deletedCoopMemberDoesNotHideCollections() {
        JsonObject p = profile("{\"collection\":{\"COAL\":30}}");
        p.getAsJsonObject("members").add("peer", json("{\"profile\":{\"deletion_notice\":{\"timestamp\":1}}}"));
        assertEquals(1, board(p).quests().size());
    }
    @Test void missingCollectionsAreNotTreatedAsZeroProgress() {
        var result = board(profile("{}"));
        assertTrue(result.quests().isEmpty());
        assertTrue(result.notices().stream().anyMatch(s -> s.contains("Collection data")));
    }
    @Test void knownUnlockedTiersAreRespected() {
        assertTrue(board(profile("{\"collection\":{\"COAL\":1},\"unlocked_coll_tiers\":[\"COAL_2\"]}")).quests().isEmpty());
    }
    @Test void combatFocusFiltersGatheringAndUsesCurrentSkillSchema() {
        JsonObject skills = json("""
          {"skills":{"COMBAT":{"name":"Combat","maxLevel":60,"levels":[
          {"level":1,"totalExpRequired":50},{"level":2,"totalExpRequired":175}]}}}
          """);
        var result = QuestEngine.generate(profile("{\"collection\":{\"COAL\":1},\"player_data\":{\"experience\":{\"SKILL_COMBAT\":100}}}"),
                UUID, definitions(), skills, QuestEngine.Focus.COMBAT);
        assertEquals(1, result.quests().size());
        assertEquals("Combat 2", result.quests().getFirst().title());
        assertEquals("75", result.quests().getFirst().remaining());
    }
    @Test void unpurchasedFarmingLevelsAreNotRecommended() {
        JsonObject skills = json("{\"skills\":{\"FARMING\":{\"maxLevel\":60,\"levels\":[{\"level\":51,\"totalExpRequired\":100}]}}}");
        var result = QuestEngine.generate(profile("{\"player_data\":{\"experience\":{\"SKILL_FARMING\":90}}}"),
                UUID, new JsonObject(), skills, QuestEngine.Focus.COMPLETION);
        assertTrue(result.quests().isEmpty());
    }
    @Test void wrongProfileGivesActionableNotice() {
        assertTrue(board(json("{\"members\":{}}" )).notices().getFirst().contains("not present"));
    }
    @Test void demoIsExecutableThroughRealQuestEngine() {
        try (var reader = new InputStreamReader(getClass().getResourceAsStream("/demo.json"))) {
            JsonObject d = JsonParser.parseReader(reader).getAsJsonObject();
            var result = QuestEngine.generate(d.getAsJsonArray("profiles").get(0).getAsJsonObject(),
                    d.get("uuid").getAsString(), d.getAsJsonObject("collections"), d.getAsJsonObject("skills"), QuestEngine.Focus.QUICK_WINS);
            assertTrue(result.quests().size() >= 5);
            for (int i = 1; i < result.quests().size(); i++)
                assertTrue(result.quests().get(i - 1).progress() >= result.quests().get(i).progress());
        } catch (Exception e) { fail(e); }
    }
    @Test void invalidCredentialsAreRejectedBeforeNetwork() {
        var api = new HypixelApi();
        assertThrows(HypixelApi.ApiException.class, () -> api.fetch("bad", "bad"));
        assertThrows(HypixelApi.ApiException.class, () -> api.fetch(UUID, "bad\r\nheader"));
    }
}
