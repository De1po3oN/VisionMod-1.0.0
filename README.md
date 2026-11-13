# Vision Mod 1.0.0 — Open Beta

Minecraft Java 1.21.11 / Fabric Loader >=0.19.3 / Java 21.

## Core features
- Reworked Vision Mod UI (Right Shift), adjustable module hotkeys (MMB), ore highlighting/color editor and Free Look.
- Crosshair editor, CPS/HUD, Zoom, HUD and block highlighting, particle filters.
- Snowflakes / Air Hearts rendered by Vision Mod's own client-side particle spawn and permitted through its filter, even when HIDE ALL is selected.
- Opt-in actual rendering workload reduction: 4/6/8 render-chunk cap, 50% entity distance scale, minimal vanilla particles, cloud removal, disabled entity shadows and biome blending; original settings backed up and restored.
- Optional FPS-adaptive chunk cap (checks at most once per 10 seconds), target FPS 40/50/60/75; no changes to server TPS.
- JEI 27.54.0.113 bundled in the final remapped Vision Mod JAR through Fabric Loom `include`, MIT license in THIRD_PARTY_LICENSES/JEI_MIT.txt. JEI is an independent nested mod, not rewritten.

## Removed
All project cosmetics renderer/menus, bundled textures, cloud service/worker, assets, imports/exports and settings. Previously shared profiles are still accepted, but old cosmetics fields are ignored. Existing local files in the user's config directory are **not** deleted automatically.

## Build
`gradle clean build --no-daemon --stacktrace` (Java 21, Gradle 9.2.1). The nested JEI JAR will be downloaded on the build runner from Modrinth Maven. Build result: `build/libs/visionmod-1.0.0.jar`.

Status: uncompiled source update. The complete Gradle build and in-game performance test are **not verified**. Exact FPS gains depend on CPU/GPU, entity count, shaders and baseline draw distance.

**JEI server caveat**: on Minecraft 1.21.11, certain server-provided/custom recipes may require JEI or recipe synchronization installed server-side to appear in the client.
