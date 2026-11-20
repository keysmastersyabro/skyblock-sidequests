package dev.sidequests.core;

import com.google.gson.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.*;

/** Key is sent only in the API-Key header to api.hypixel.net, never to logs or disk. */
public final class HypixelApi {
    public record Snapshot(JsonArray profiles, JsonObject collections, JsonObject skills, Instant fetchedAt) {}
    public static final class ApiException extends IOException {
        public ApiException(String message) { super(message); }
    }
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(12))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    private JsonObject collections, skills;
    private Instant definitionsFetched;
    private Instant nextRequest = Instant.EPOCH;

    public synchronized Snapshot fetch(String uuid, String key) throws IOException, InterruptedException {
        if (!uuid.matches("[0-9a-fA-F]{32}")) throw new ApiException("Your Minecraft UUID is invalid.");
        if (!key.matches("[0-9a-fA-F-]{32,36}")) throw new ApiException("Paste a valid Hypixel application API key.");
        if (Instant.now().isBefore(nextRequest)) {
            long seconds = Math.max(1, Duration.between(Instant.now(), nextRequest).getSeconds());
            throw new ApiException("Wait " + seconds + " seconds before refreshing again.");
        }
        nextRequest = Instant.now().plusSeconds(60);
        JsonObject response = get("/v2/skyblock/profiles?uuid=" + uuid, key);
        JsonArray profiles = QuestEngine.array(response, "profiles");
        if (profiles.isEmpty()) throw new ApiException("No SkyBlock profiles found for your Minecraft account.");
        if (definitionsFetched == null || definitionsFetched.plus(Duration.ofHours(12)).isBefore(Instant.now())) {
            JsonObject newCollections = get("/v2/resources/skyblock/collections", null);
            JsonObject newSkills = get("/v2/resources/skyblock/skills", null);
            collections = newCollections;
            skills = newSkills;
            definitionsFetched = Instant.now();
        }
        return new Snapshot(profiles, collections, skills, Instant.now());
    }

    private JsonObject get(String path, String key) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("https://api.hypixel.net" + path))
                .timeout(Duration.ofSeconds(20)).header("Accept", "application/json")
                .header("User-Agent", "SkyBlockSidequests/0.1.0").GET();
        if (key != null) builder.header("API-Key", key);
        HttpResponse<String> r;
        try { r = client.send(builder.build(), HttpResponse.BodyHandlers.ofString()); }
        catch (IOException e) { throw new ApiException("Cannot reach Hypixel. Check your connection and try again."); }
        if (r.statusCode() == 403 || r.statusCode() == 401) throw new ApiException("Hypixel rejected the API key. Check its validity in the Developer Dashboard.");
        if (r.statusCode() == 429) {
            long delay = 300;
            try { delay = Math.max(delay, Long.parseLong(r.headers().firstValue("Retry-After").orElse("300"))); }
            catch (NumberFormatException ignored) { }
            nextRequest = Instant.now().plusSeconds(Math.min(delay, 86400));
            throw new ApiException("Hypixel rate limit reached. Try again in " + delay + " seconds.");
        }
        if (r.statusCode() != 200) throw new ApiException("Hypixel returned HTTP " + r.statusCode() + ". Try later.");
        try {
            JsonObject body = JsonParser.parseString(r.body()).getAsJsonObject();
            if (!body.has("success") || !body.get("success").getAsBoolean())
                throw new ApiException("Hypixel could not provide profile data. Try again later.");
            return body;
        } catch (JsonParseException | IllegalStateException e) {
            throw new ApiException("Hypixel returned an unexpected response. Try again later.");
        }
    }
}
