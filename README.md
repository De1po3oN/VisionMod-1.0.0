# Vision Mod 1.1.0 — Open Beta

Minecraft Java 1.21.11 / Fabric Loader >=0.19.3 / Java 21.

## What's new in 1.1.0 (source/CI candidate; runtime not yet tested)
- Free Look: smoothed, independent third-person camera orbit; camera perspective cleanup and configurable smoothing. Hold Left Alt, release to keep the chosen view, F5 to reset.
- Atmospheric Air Hearts / Snowflakes: configurable density (1–3) and placement (near player in world space or distributed in the viewing direction). Effects remain explicitly enabled in MAX FPS.
- GUI refresh: original graphite/lavender design, refined module switches and navigation; no copied reference-mod files or assets.
- Extras page 3 contains Heart FX mode, Ambient density and Smooth Free Look settings. These three settings are mouse-controlled and have no new hotkey IDs.
- This is **not** an in-game verified build. Test camera and visual behavior before calling it fixed.

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
`gradle clean build --no-daemon --stacktrace` (Java 21, Gradle 9.2.1). The nested JEI JAR will be downloaded on the build runner from Modrinth Maven. Build result: `build/libs/visionmod-1.1.0.jar`.

Status: 1.1.0 source update. GitHub Actions compilation is checked separately; in-game performance and visual behavior are **not yet verified**. Exact FPS gains depend on CPU/GPU, entity count, shaders and baseline draw distance.

**JEI server caveat**: on Minecraft 1.21.11, certain server-provided/custom recipes may require JEI or recipe synchronization installed server-side to appear in the client.


## 1.1.0 GUI and ambient options
- New graphite/lavender Vision theme with original geometry, muted contrast and clearer module switches. Does not contain Fading Light code or assets.
- Extras → Settings (page 3) lets users choose Ambient Mode (WORLD/CAMERA), Ambient Density (LOW/MEDIUM/HIGH), Smooth Free Look (ON/OFF). Right-click Air Hearts or Snowflakes to jump to this page; right-click Free Look for quick smooth toggle.
- Free Look controls the independent third-person camera on Left Alt; release keeps the orbit; F5 cancels. Smooth interpolation can be disabled.
- AIR HEARTS and Snowflakes are still vanilla particle types, but emit across a world-space area or an optional camera-space halo. No custom heart texture was copied. Visual correctness requires a client test.
