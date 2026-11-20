# Validation

- Target: Minecraft Java 26.3, Fabric Loader 0.20.1, Fabric API 0.166.2+26.3.
- Toolchain: Temurin JDK 25, Gradle 9.8.2, Fabric Loom 1.19.4.
- Full Gradle build compiled both the quest engine and the actual Minecraft client integration and produced the mod JAR.
- 13 JUnit tests passed, with zero failures or errors. Coverage includes next collection thresholds, exact threshold transitions, maxed collections, co-op totals, missing co-op data, deleted co-op members, unavailable collection data, known unlocked tiers, current skill schema, combat filtering, farming level caps, missing player profiles, demo generation and ranking, and credential input validation.
- JAR contents checked for all client entry points, the quest engine, demo fixture, language file, and the exact Minecraft dependency/version metadata.
- Public Hypixel collection and skill endpoints were fetched successfully to check current definition formats. Demo collection definitions are a small subset of those public definitions; demo player progress is fictional.

Not performed: a graphical Minecraft launch, gameplay testing on Hypixel, or authenticated profile fetching using a real application key. No user API key was available, and this environment has no graphical Minecraft session. The demo and pure quest calculations were tested; live profile access depends on the user's key and SkyBlock sharing settings.
