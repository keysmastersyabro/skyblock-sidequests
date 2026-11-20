package dev.sidequests.client;

import com.google.gson.*;
import com.mojang.blaze3d.platform.InputConstants;
import dev.sidequests.core.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class SidequestsClient implements ClientModInitializer {
    public static final SidequestsClient STATE = new SidequestsClient();
    final HypixelApi api = new HypixelApi();
    HypixelApi.Snapshot snapshot;
    QuestEngine.Board board = new QuestEngine.Board(List.of(), List.of());
    QuestEngine.Focus focus = QuestEngine.Focus.QUICK_WINS;
    String apiKey = "", uuid = "", status = "Connect your profile or try the demo.";
    String pinned = "";
    int profileIndex;
    boolean busy, demo;

    @Override public void onInitializeClient() {
        KeyMapping open = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.sidequests.open",
                InputConstants.Type.KEYBOARD, InputConstants.KEY_K,
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath("sidequests", "board"))));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (open.consumeClick()) client.gui.setScreen(new QuestScreen());
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> dispatcher.register(
                literal("sidequests").executes(context -> {
                    Minecraft.getInstance().execute(() -> Minecraft.getInstance().gui.setScreen(new QuestScreen()));
                    return 1;
                })));
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("sidequests", "pinned"), (g, dt) -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player == null || mc.gui.screen() != null || STATE.pinned.isEmpty()) return;
                    Quest goal = STATE.board.quests().stream().filter(q -> q.id().equals(STATE.pinned)).findFirst().orElse(null);
                    if (goal == null) return;
                    g.fill(6, 6, 232, 51, 0xCC19202E);
                    g.text(mc.font, STATE.demo ? "SIDEQUEST / DEMO" : "SIDEQUEST / SNAPSHOT", 12, 11, 0xFF9AA9BD, false);
                    g.text(mc.font, mc.font.plainSubstrByWidth(goal.title(), 210), 12, 23, 0xFFFFD787, false);
                    g.text(mc.font, goal.remaining() + " to go  |  K: quest board", 12, 35, 0xFFCED4E2, false);
                    g.fill(12, 46, 225, 48, 0xFF343E50);
                    g.fill(12, 46, 12 + (int)(213 * goal.progress()), 48, 0xFF67D9B1);
                });
    }

    JsonObject profile() { return snapshot.profiles().get(profileIndex).getAsJsonObject(); }
    String profileName() {
        return snapshot == null ? "No profile connected" : QuestEngine.string(profile(), "cute_name", "Profile")
                + (demo ? " [DEMO]" : "");
    }
    String updated() {
        return snapshot == null ? "" : "Snapshot " + DateTimeFormatter.ofPattern("HH:mm:ss")
                .withZone(ZoneId.systemDefault()).format(snapshot.fetchedAt());
    }
    void regenerate() {
        if (snapshot != null) board = QuestEngine.generate(profile(), uuid, snapshot.collections(), snapshot.skills(), focus);
    }
    void nextProfile() {
        if (snapshot == null) return;
        profileIndex = (profileIndex + 1) % snapshot.profiles().size();
        pinned = "";
        regenerate();
    }
    void refresh() {
        if (busy) return;
        Minecraft client = Minecraft.getInstance();
        String playerId = client.getUser().getProfileId().toString().replace("-", "");
        String key = apiKey.trim();
        String previousId = snapshot == null || demo ? "" : QuestEngine.string(profile(), "profile_id", "");
        status = "Fetching your SkyBlock profile...";
        busy = true;
        CompletableFuture.runAsync(() -> {
            try {
                HypixelApi.Snapshot loaded = api.fetch(playerId, key);
                client.execute(() -> {
                    snapshot = loaded;
                    uuid = playerId;
                    demo = false;
                    profileIndex = 0;
                    for (int i = 0; i < loaded.profiles().size(); i++) {
                        JsonObject p = loaded.profiles().get(i).getAsJsonObject();
                        if (p.has("selected") && p.get("selected").getAsBoolean()) profileIndex = i;
                    }
                    for (int i = 0; i < loaded.profiles().size(); i++)
                        if (!previousId.isEmpty() && previousId.equals(QuestEngine.string(
                                loaded.profiles().get(i).getAsJsonObject(), "profile_id", ""))) profileIndex = i;
                    regenerate();
                    status = "Profile loaded. Choose a focus and pin a goal.";
                    busy = false;
                });
            } catch (Exception e) {
                if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                String message = e instanceof HypixelApi.ApiException ? e.getMessage() : "Could not load profile data. Try again later.";
                client.execute(() -> { status = message; busy = false; });
            }
        });
    }
    void loadDemo() {
        if (busy) return;
        try (var stream = SidequestsClient.class.getResourceAsStream("/demo.json")) {
            if (stream == null) { status = "Demo fixture is missing."; return; }
            JsonObject fixture = JsonParser.parseReader(new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            snapshot = new HypixelApi.Snapshot(fixture.getAsJsonArray("profiles"),
                    fixture.getAsJsonObject("collections"), fixture.getAsJsonObject("skills"), java.time.Instant.now());
            uuid = fixture.get("uuid").getAsString();
            profileIndex = 0;
            pinned = "";
            demo = true;
            status = "Demo data. Connect to see quests for your real profile.";
            regenerate();
        } catch (Exception e) { status = "Could not load demo data."; }
    }
}
