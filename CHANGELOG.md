# Vision Mod 2.0.0 — development build (2026-11-14)

- Start version 2.0.0 and switch GitHub Actions JAR/version verification to 2.0.0.
- HUD / combat panel: Aspect Bars, colored armor durability, targeted health and range, nearest TNT warning, nearest arrow label, hit pulse and red edge flash. These are **original client-side HUD prototypes** and may differ from the third-party reference.
- World FX panel: target bounding-box outline, short bounded position trails, dotted landing circle, third-person-only dotted cone hat. All off by default with saved settings. No shader/texture copy.
- Existing Free Look, ambient effects, JEI, optimizer, Shulker Viewer and other 1.1 features remain intact.
- Performance: limit world entity scanning to once per 10 client ticks, cap local trail history at 13, use shared render event/line pipeline. FPS increase or neutrality has not been measured.
- Legal: user-supplied reference declares All-Rights-Reserved; only feature behavior/design goals were studied. No code/art/audio/fonts transferred.
- **Not a complete port** of the requested 2.0 feature list. See FEATURE_REQUESTS_2_0_0.md and FOR_AI.txt. Compilation/QA must be checked against latest GitHub Actions; real client and shader testing remain outstanding.

---

# Vision Mod 1.1.0 — Open Beta candidate (2026-11-14)

- Started 1.1.0 branch of development on main.
- Free Look refined with independent camera orbit, optional smoothing and perspective cleanup.
- Ambient Air Hearts / Snowflakes now offer world-space vs camera-oriented layout and a density setting; vanilla-heart texture is still used, and the visual result must be judged in game.
- Reworked main GUI appearance using original lavender/graphite styling; no copied textures or code from the supplied third-party JAR.
- Initial build/functional testing: **not yet confirmed** for the 1.1.0 HEAD when this section was authored. Check GitHub Actions and test in Minecraft.
- Follow-up: user will test the third-party visual reference and describe the exact target behavior. Tune Free Look/FX accordingly; don't assume the current result is the final design.

---

# Vision Mod 1.1.0 — GUI / Free Look / Ambient FX (2026-11-14)

- New original graphite/lavender GUI skin inspired by the general modern visual style of the attached reference, without importing any proprietary textures/code.
- Free Look: smooth, time-based third-person orbit, mouse-delta clamp and perspective restoration when disabled. F5 cancels the orbit; configurable smoothness saved to extras.properties.
- Air Hearts/Snowflakes: two modes (WORLD and CAMERA), three density settings, controlled local emission, no hiding of explicitly enabled effects in MAX FPS. Added settings to EXTRAS page 3 and right-click shortcuts.
- Vanilla HEART/SNOWFLAKE particles are still used. Exact desired visual style and runtime behavior must be confirmed by the user in-game.
- Java/Minecraft runtime tests and FPS measurements are pending; CI build results must be verified independently.

# Vision Mod 1.0.0 — Open Beta (version unchanged)

- Removed cosmetics feature at source level, including external Worker and texture files.
- Bundled the unmodified MIT-licensed JEI Fabric mod via Loom nested JAR dependency, not just a download button.
- Fixed Snowflakes and Air Hearts spawning by calling ClientLevel.addAlwaysVisibleParticle locally and allowing only these locally-generated particles through Vision Mod particle filters.
- Kept explicit effects visible on crowded spawn; reduce frequency rather than silently disabling.
- MAX FPS preset now enables actual vanilla render-distance / entity-distance changes, shadows and biome blending, reversible after disabling.
- Added conservative adaptive chunk cap (10-second sampling, delayed downshift and gradual recovery) and user FPS target 40/50/60/75.
- Removed cosmetic settings from profile export while maintaining tolerance for legacy input fields.
- No change to version number; Minecraft runtime and server performance still need validation.

## Passive sign optimizer (1.0.0)

- Cache distant normal and hanging sign render text for up to 10 seconds (client-side only).
- Signs within 12 blocks always update visually immediately; changing glow/dye color refreshes without delay at all distances.
- Keep the sign model and text rendered every frame; no blinking or disappearing text between refreshes.
- In FPS Boost / busy-spawn mode, skip extra glowing text outline passes beyond 12 blocks, keeping the text and sign body visible. Vanilla server packets are not delayed or intercepted.
- This only reduces some client CPU/GPU rendering work, not server tick load; measure FPS at a sign-heavy spawn to confirm impact.

## Free Look / visual FX / real FPS optimizations (1.0.0)

- Camera-orbit override now intercepts the vanilla rotation arguments before third-person offset and collision. Mouse interception matches MouseHandler.turnPlayer(double).
- Preserve the chosen Free Look view angle after key release, reset via F5, and show a warning when a separate FreeLook mod is also installed. Runtime verification remains necessary.
- Spawn Snowflakes and Air Hearts as always-visible, rate-limited client particles in front of the active camera, even with Minecraft particles set to MINIMAL.
- MAX FPS no longer silently disables explicitly chosen Snowflakes and Air Hearts.
- Throttle repeated static-area ore scanning with a short cooldown to reduce CPU polling; movement forces immediate rescan.
- Add reversible AO, improved-transparency, cutout-leaves and weather-radius caps to REAL FPS BOOST; adapt entity render distance further on slow/busy frames.
- The global End master toggle restores prior video options while Vision Mod is disabled.
- Reuse player-health label components and buffers rather than allocate new formatted text and arrays every rendered frame.
- Guard FullBright's expression replacement against incompatible non-Double values.
- GitHub Actions compiles the sources and checks embedded JEI, but does NOT prove visual correctness or measured FPS gains.
