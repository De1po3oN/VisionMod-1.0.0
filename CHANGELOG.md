# Vision Mod 1.0.0 — Open Beta (version unchanged)

- Removed cosmetics feature at source level, including external Worker and texture files.
- Bundled the unmodified MIT-licensed JEI Fabric mod via Loom nested JAR dependency, not just a download button.
- Fixed Snowflakes and Air Hearts spawning by calling the client particle engine directly and allowing only these locally-generated particles through Vision Mod particle filters.
- Kept explicit effects visible on crowded spawn; reduce frequency rather than silently disabling.
- MAX FPS preset now enables actual vanilla render-distance / entity-distance changes, shadows and biome blending, reversible after disabling.
- Added conservative adaptive chunk cap (10-second sampling, delayed downshift and gradual recovery) and user FPS target 40/50/60/75.
- Removed cosmetic settings from profile export while maintaining tolerance for legacy input fields.
- No change to version number; Minecraft runtime and server performance still need validation.
