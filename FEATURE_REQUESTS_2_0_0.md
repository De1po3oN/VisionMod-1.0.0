# Vision Mod 2.0.0 — feature migration tracker (2026-11-14)

Reference inspected without running it: user-supplied `fading-beta-2.0.0-fabric.jar`.
Its `fabric.mod.json` declares `license: All-Rights-Reserved`. No source, bytecode, textures, UI assets, fonts or sounds from that JAR are copied into Vision Mod. Recreate behavior with original code and test in the game.

**IMPORTANT:** Version 2.0.0 in metadata is a development baseline, NOT evidence that all items are finished or tested. Do not install the untrusted reference JAR just to analyze it.

## Implemented or previously present (scope specified)

| Requested | Current Vision Mod implementation | State |
| --- | --- | --- |
| armorcolorful | Optional colored **armor durability HUD**, not dyed 3D armor | 2.0 client HUD prototype |
| arrows name | Nearest arrow **name/distance HUD**, not floating 3D labels | 2.0 client HUD prototype |
| aspect ratio | Optional 2.39:1 cinematic letterbox bars | 2.0 client HUD prototype |
| hit range | Range displayed when aiming at a living entity | 2.0 client HUD prototype |
| bloodhit | Subtle red screen-edge flash when aimed entity enters hurt animation | 2.0 client HUD prototype, not a genuine damage-source detector |
| hit effect | Crosshair ring on aimed entity's hurt animation | 2.0 client HUD prototype |
| health indicator | Target name, client-observed HP/max HP and bar | 2.0 client HUD prototype |
| tnt indicator | Nearest TNT distance warning; no fuse timer yet | 2.0 client HUD prototype |
| particles | Snowflakes, Air Hearts and category-based particle suppression already exist | inherited from 1.1.0; runtime QA needed |
| smooth camera | Independent Free Look, smoothing option | inherited from 1.1.0; runtime QA needed |
| item scroller | Fast Scroller | inherited; not yet equivalent to reference |
| optimizer | VisionPerformance and VisionRenderBoost | inherited; FPS gains not benchmarked |
| swing animation | Small Sword / Compact Hand exists, **not** full custom swing animation | partial prior feature |
| target esp | Block Target Outline exists, **not** full entity target ESP | different prior feature |
| animations | Main menu transition animation exists | inherited, not reference UI |
| interface | Configurable theme/sidebar GUI | inherited; original design |
| shulker viewer | Shulker Summary, tooltip and grid | inherited; runtime QA needed |
| customsound | Arrow Hit Sound option exists | inherited; custom sound suite pending |

## Not implemented / not claimed done

- Block Overlay: configurable world-space block fill and outline (separate from Target Outline).
- China Hat: optional proper 3D head-attached cosmetic rendering, compatible with Free Look; no online cosmetics system unless requested.
- Clear Render: individual controls for fog, weather, overlays and unwanted visual clutter beyond existing filters.
- Custom Models: configurable local models/resource-pack support without external downloads.
- Hit Color: color customization for hit effects and entity outlines.
- Item Physics: client-only dropped-item rotations/physics; prevent conflicts with other item renderers.
- Jump Circles: world-space rings at landing points with bounded GPU budget.
- Kill Effect: actual player-related kill event effects, not an arbitrary nearby-death flash.
- Motion Blur: opt-in effect with frame-time limits and shader compatibility; heavy GPU cost requires FPS benchmarking.
- Spear Lunge / Spear Pierce: define exact game semantics and verify 1.21.11 item availability; do not fake server-authoritative combat.
- Full Swing Animation: player hand animation options and mixin compatibility testing.
- Entity Target ESP: configurable 3D targeting effect (distinct from HUD).
- Trails: world-space motion trails using bounded history.
- World Customizer: safe client-only visual settings with profiles and shader coexistence.
- World Lyrics: world-space anchored text, locally supplied content (no copyrighted song lyrics bundled).
- Additional GUI animations/layout parity; no exact copying of licensed assets.
- Pet: local visual companion, bounded animations, no remote texture tracking.
- Fast Use: clarify desired permitted interaction; do not assume client-only changes bypass server cooldown.
- Social: optional local UI first, never transmit personal data without opt-in.
- Wider CustomSound feature set (event/volume/sound presets).
- Full TNT fuse countdown and world-space arrows name overlay.

## Next iterations, in safe buildable groups

1. Re-test 2.0.0 compilation and in-game HUD functionality; fix errors before merging more features.
2. Visual world-rendering layer: Block Overlay, Entity ESP, Jump Circles, Trails, China Hat, World Lyrics (test mixins/shaders).
3. Combat and animations: Hit Color, true Hit/Kill effects, Swing Animation, Spear effects, Item Physics.
4. Interface/HUD UX: draggable layout, expanded effects/animations, Pet, Shulker viewer, richer customization, custom sounds, social.
5. Performance and safety: cap particle/object counts, profile render costs, compare average FPS and 1% low in same scene, preserve server TPS and client settings.

## Verify after each code group

- GitHub Actions compile, nested JEI checks and jar upload.
- Real in-game Minecraft 1.21.11 / Fabric client launch, toggles, UI mouse interactions, shaders, multiplayer, Sodium.
- Record tests/bugs in FOR_AI.txt and QA_CHECKLIST_RU.md. A green build alone is not a full test.
