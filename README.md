# SkyBlock Sidequests — Minecraft Java 26.3

A client-side Fabric mod that turns your SkyBlock profile into a board of small collection and skill goals. Unofficial; not affiliated with Hypixel or Mojang.

## Install

1. Create a **Minecraft Java 26.3** installation with **Fabric Loader 0.20.1 or newer**. Use Java 25 or newer (the launcher normally manages the game runtime).
2. Install **Fabric API for 26.3** and `skyblock-sidequests-0.1.0.jar` in that installation's `mods` folder.
3. Start Minecraft and press **K**, or run **/sidequests** while connected to a server. Rebind K under Options → Controls → Key Binds if another mod uses it.
4. Press **Demo** to explore without an API key. The demo always labels its fictional player data.
5. To use your account, create/register your own application in the [Hypixel Developer Dashboard](https://developer.hypixel.net/). Open **Connect**, paste the application API key, and press **Connect**. Do not post the key in chat.
6. Enable collection and skill API sharing in your SkyBlock settings if the board reports missing data.

[Fabric installer](https://fabricmc.net/use/installer/) · [Fabric API downloads](https://modrinth.com/mod/fabric-api/versions?g=26.3)

**Minecraft calls this release 26.3, rather than 1.26.3.** This JAR intentionally requires 26.3.

## Using the board

- **Quick wins:** goals sorted by the proportion already completed. This is not an estimate of completion time.
- **Combat:** combat skill and combat collections.
- **Money:** farming, mining, fishing, and foraging progression. This does not calculate profitability or recommend market trades.
- **Completion:** every available collection and skill goal.
- **Profile:** cycle through your profiles using the already-loaded snapshot. The selected SkyBlock profile is used initially.
- **Pin:** display a goal in a small overlay while playing. Click Pinned to remove it. The overlay uses the last snapshot; it does not update live. Pinning and profile data last for the current Minecraft session.
- **Refresh:** fetch a new snapshot manually. Requests have a minimum 60-second cooldown, and server rate limits can extend that delay. Opening the board does not fetch anything. If a refresh fails, the previous board remains available with its original timestamp.
- **Forget key:** clears the key from the mod's current state. Restarting Minecraft also discards it. An already-running request may finish using the key it started with.

## First-version scope

This release suggests collection unlocks and skill levels. It uses public collection and skill definitions from Hypixel, including recipe descriptions. Collection totals include visible active co-op members; incomplete co-op data hides collection suggestions. Missing API fields are not treated as zero progress. Farming levels above 50 require the profile's purchased level-cap perk.

Accessory pricing, Magical Power optimization, dungeon gear recommendations, profit estimates, automatic completion celebrations, and time estimates are not implemented. This is a planning aid; it does not automate gameplay.

## Privacy and API behavior

The API key is masked in the input field, kept only in memory, and sent in an `API-Key` header to `https://api.hypixel.net`. It is not saved to disk, placed in URLs, or logged by this mod. The mod uses the UUID of your signed-in Minecraft account. Public resource requests have no key; definitions are cached in memory for 12 hours. Redirects are disabled and requests have timeouts. There is no telemetry or background polling. Use your own registered application and follow [Hypixel's API policies](https://developer.hypixel.net/policies/).

Endpoints: `/v2/skyblock/profiles`, `/v2/resources/skyblock/collections`, `/v2/resources/skyblock/skills`.

## Build from source

Install a **JDK 25**, set `JAVA_HOME` to that JDK, and run:

```sh
./gradlew build
```

On Windows: `gradlew.bat build`.

The first build downloads Gradle, Minecraft, and Fabric dependencies. Output: `build/libs/skyblock-sidequests-0.1.0.jar`. The file ending in `-sources.jar` is source code, not the installable mod. Run `./gradlew runClient` for a local development launch.

## Validation

See `VALIDATION.md` for the checks performed for this build. A successful compile does not replace testing in your Minecraft client with your account's API permissions enabled.

License: MIT. Fabric/Minecraft dependencies retain their respective licenses and are not bundled in the source archive.
