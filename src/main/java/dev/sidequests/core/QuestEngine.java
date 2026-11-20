package dev.sidequests.core;

import com.google.gson.*;
import java.util.*;

/** Pure transformation of a single user-requested snapshot; never polls the API. */
public final class QuestEngine {
    public enum Focus { QUICK_WINS, COMBAT, MONEY, COMPLETION }
    public record Board(List<Quest> quests, List<String> notices) {}

    public static Board generate(JsonObject profile, String uuid, JsonObject resources,
                                 JsonObject skills, Focus focus) {
        var quests = new ArrayList<Quest>();
        var notices = new ArrayList<String>();
        JsonObject members = object(profile, "members");
        String playerId = uuid.replace("-", "");
        JsonObject member = object(members, playerId);
        if (!members.has(playerId) || !members.get(playerId).isJsonObject()) {
            return new Board(List.of(), List.of("Your player is not present in this profile."));
        }
        boolean collectionsVisible = member.has("collection") && member.get("collection").isJsonObject();
        if (!collectionsVisible) notices.add("Collection data is unavailable. Enable collection API sharing in SkyBlock.");

        // Collection requirements are co-op totals, not just this player's contribution.
        Map<String, Double> totals = new HashMap<>();
        boolean incompleteCoop = false;
        for (var entry : members.entrySet()) {
            if (!entry.getValue().isJsonObject()) continue;
            JsonObject peer = entry.getValue().getAsJsonObject();
            if (object(peer, "profile").has("deletion_notice")) continue;
            if (!peer.has("collection") || !peer.get("collection").isJsonObject()) incompleteCoop = true;
            for (var item : object(peer, "collection").entrySet()) {
                double n = number(item.getValue());
                if (n >= 0) totals.merge(item.getKey(), n, Double::sum);
            }
        }
        if (collectionsVisible && incompleteCoop) {
            notices.add("Co-op collection data is incomplete; collection suggestions are hidden to avoid incorrect unlocks.");
        } else if (collectionsVisible) {
            for (var category : object(resources, "collections").entrySet()) {
                if (!category.getValue().isJsonObject()) continue;
                JsonObject group = category.getValue().getAsJsonObject();
                for (var item : object(group, "items").entrySet()) {
                    if (!item.getValue().isJsonObject()) continue;
                    JsonObject definition = item.getValue().getAsJsonObject();
                    double count = totals.getOrDefault(item.getKey(), 0.0);
                    int unlocked = unlockedTier(member, item.getKey());
                    JsonObject next = null;
                    for (JsonElement e : array(definition, "tiers")) {
                        if (!e.isJsonObject()) continue;
                        JsonObject tier = e.getAsJsonObject();
                        double target = value(tier, "amountRequired");
                        if (target > count && value(tier, "tier") > unlocked &&
                                (next == null || target < value(next, "amountRequired"))) next = tier;
                    }
                    if (next == null) continue;
                    String name = string(definition, "name", item.getKey());
                    String tier = Integer.toString((int) value(next, "tier"));
                    String detail = "Reach the next collection tier";
                    JsonArray unlocks = array(next, "unlocks");
                    if (unlocks.size() > 0 && unlocks.get(0).isJsonPrimitive()) detail = unlocks.get(0).getAsString();
                    quests.add(new Quest("collection:" + item.getKey() + ":" + tier,
                            name + " " + tier, detail, category.getKey(), count, value(next, "amountRequired")));
                }
            }
        }

        JsonObject experience = object(object(member, "player_data"), "experience");
        boolean anySkills = false;
        for (var entry : object(skills, "skills").entrySet()) {
            if (!entry.getValue().isJsonObject()) continue;
            String skill = entry.getKey();
            JsonElement xp = experience.get("SKILL_" + skill.toUpperCase(Locale.ROOT));
            if (xp == null) xp = member.get("experience_skill_" + skill.toLowerCase(Locale.ROOT));
            double current = number(xp);
            if (current < 0) continue;
            anySkills = true;
            JsonObject definition = entry.getValue().getAsJsonObject();
            double cap = value(definition, "maxLevel");
            if (skill.equals("FARMING")) {
                double purchased = value(object(member, "jacobs_contest"), "perks", "farming_level_cap");
                cap = Math.min(cap, 50 + purchased);
            }
            for (JsonElement e : array(definition, "levels")) {
                if (!e.isJsonObject()) continue;
                JsonObject level = e.getAsJsonObject();
                double target = value(level, "totalExpRequired");
                double index = value(level, "level");
                if (target > current && index <= cap) {
                    quests.add(new Quest("skill:" + skill + ":" + (int) index,
                            string(definition, "name", skill) + " " + (int) index,
                            "Earn skill XP to reach the next level", skill, current, target));
                    break;
                }
            }
        }
        if (!anySkills) notices.add("Skill XP is unavailable. Enable skill API sharing in SkyBlock.");
        if (object(resources, "collections").size() == 0) notices.add("Collection definitions are unavailable.");
        if (object(skills, "skills").size() == 0) notices.add("Skill definitions are unavailable.");

        quests.removeIf(q -> switch (focus) {
            case COMBAT -> !Set.of("COMBAT", "SLAYER").contains(q.category());
            case MONEY -> !Set.of("MINING", "FARMING", "FISHING", "FORAGING").contains(q.category());
            default -> false;
        });
        quests.sort(Comparator.comparingDouble(Quest::progress).reversed().thenComparing(Quest::id));
        if (focus == Focus.MONEY) notices.add("Money focus shows gathering progression, not profit estimates.");
        if (focus == Focus.QUICK_WINS) notices.add("Sorted by progress toward a goal; completion times vary.");
        return new Board(List.copyOf(quests), List.copyOf(notices));
    }

    private static int unlockedTier(JsonObject member, String id) {
        int max = 0;
        for (JsonElement e : array(member, "unlocked_coll_tiers")) {
            if (!e.isJsonPrimitive()) continue;
            String s = e.getAsString();
            if (s.startsWith(id + "_")) {
                try { max = Math.max(max, Integer.parseInt(s.substring(id.length() + 1))); }
                catch (NumberFormatException ignored) { /* Unknown future tier format. */ }
            }
        }
        return max;
    }
    public static JsonObject object(JsonObject root, String key) {
        JsonElement e = root.get(key);
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : new JsonObject();
    }
    public static JsonArray array(JsonObject root, String key) {
        JsonElement e = root.get(key);
        return e != null && e.isJsonArray() ? e.getAsJsonArray() : new JsonArray();
    }
    public static double value(JsonObject root, String... path) {
        for (int i = 0; i < path.length - 1; i++) root = object(root, path[i]);
        return Math.max(0, number(root.get(path[path.length - 1])));
    }
    private static double number(JsonElement e) {
        try {
            double n = e == null || e.isJsonNull() ? -1 : e.getAsDouble();
            return Double.isFinite(n) ? n : -1;
        } catch (RuntimeException ignored) { return -1; }
    }
    public static String string(JsonObject root, String key, String fallback) {
        JsonElement e = root.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : fallback;
    }
}
