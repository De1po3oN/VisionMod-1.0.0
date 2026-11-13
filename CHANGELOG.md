# Vision Mod 1.0.0 — Open Beta (version unchanged)

- Removed cosmetics feature at source level, including external Worker and texture files.
- Bundled the unmodified MIT-licensed JEI Fabric mod via Loom nested JAR dependency, not just a download button.
- Fixed Snowflakes and Air Hearts spawning by calling ClientLevel.addParticle locally and allowing only these locally-generated particles through Vision Mod particle filters.
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
