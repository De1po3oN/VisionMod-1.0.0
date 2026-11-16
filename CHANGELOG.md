## 2026-11-16 — Проверенный рендер мира и чистый мод
- Мировые эффекты перенесены в активный проход AFTER_ENTITIES: исправлена невидимость Pet, Hat, Trails и других геометрических оверлеев на 1.21.11.
- F1 скрывает HUD; стандартные панели избегают пересечений, пустой Social Radar компактен, сохранённые drag-позиции имеют приоритет.
- CI: настоящий Fabric JAR, 54 переключателя, восемь кадров редакторов и шесть кадров игрового мира. Проверены отправка геометрии, приземление и прекращение рендера при global OFF; Windows native API smoke PASS.
- Локально 271 core / 274 с MPRIS, ещё12 source assertions и CPU mesh checks PASS. Это не проверка живого Spotify, Sodium/Iris или FPS; полная приёмка35 требований не заявляется. Точные SHA и доказательства: NATIVE_LYRICS_QA_RU.md.

## 2026-11-16 — Визуальная доработка (ветка codex/visual-refresh)
- Общий стиль HUD; иконки брони, плавный Target HUD, кольцо TNT, Social Radar, уведомления.
- Мягкие Trails/Jump Circles, градиентная China Hat, углы Target ESP; округлая анимированная модель Pet.
- Настоящая форма Block Overlay, улучшения частиц/моделей/Camera Streaks, звуковые пресеты.
- Адаптивные редакторы, рисование прицела с перетаскиванием, настройки оформления.
- Локально 267 core + 12 source checks PASS. Для окончательных результатов см. актуальную запись выше.

## 2026-11-16 — GUI и нативные World Lyrics (рабочая копия, не релиз)
- Удалены расширение браузера и локальный HTTP bridge; системные медиасессии Windows/Linux.
- Компактные lyrics, ограниченный LRCLIB lookup с повтором/кэшем, исправлен разбор LRC.
- Адаптивный dashboard и полноразмерные окна деталей/руд, доступные опции и смена темы.
- Сохранение основных флагов и новых групп в профилях, сброс HUD, интерполяция рендера, правильный NO_OVERLAY.
- Core 126 PASS, MPRIS fixture 129 PASS. Полная сборка и игровой QA заблокированы сетевым доступом к Fabric. Подробности NATIVE_LYRICS_QA_RU.md.

# 2.0.0 — Pet crash hardening + visible China Hat/Trails + Web Music Bridge (2026-11-15)

- Pet rendering now uses one entity-translucent buffer and a guaranteed finally-pop; on render exception it suspends itself instead of repeatedly crashing. Imported Custom Models get a 64-KiB JSON limit and render-fault protection.
- Nondegenerate, double-sided China Hat mesh; Trails lifted above terrain to avoid z-fighting. Existing configurable gradient/rainbow and alpha/width retained.
- New opt-in browser extension for Spotify, SoundCloud and YouTube Music. Pair via local 128-bit token; metadata only to 127.0.0.1:38567. Timed lyrics obtained asynchronously from LRCLIB when available; title fallback when not.
- GitHub Build #257 SUCCESS (Java/Fabric plus Node script checks, two distribution artifacts), Minecraft headless startup #122 SUCCESS. Actual world rendering and browser site selectors not yet confirmed live. See PET_TRAILS_BROWSER_LYRICS_QA_RU.md.

---

# VisionMod 2.0.0 — Draggable HUD and local LRC source (2026-11-15)

- Added a shared normalized coordinate store for 11 movable client HUD overlays with Fabric chat-screen mouse input events. Drag with LMB inside chat; RMB resets widget position; world-space overlays and vanilla reticle are not draggable to keep aiming consistent.
- Moved HUD drawing paths in VisionExtras, VisionFeaturePack, VisionExtendedFeatures, VisionTabHeads, and VisionWorldLyrics to the shared coordinate system. Conditional overlays have draggable layout handles while chat is open.
- Added an optional local `lyrics.lrc` timed-lyric reader (mm:ss.xx), bounded file size and once/second modification polling. Existing user-authored lyric lines remain supported.
- Lyrics are NOT connected to the active music player; this requires a separate Spotify/Yandex/YouTube/etc. integration.
- Actions Build #228 + client startup smoke #109 SUCCESS (54 existing toggle tests, extra LRC + hitbox parsing assertions). Dragging/mouse and actual song sync not tested in-game. See HUD_DRAG_AND_LYRICS_RU.md.

---


# 2.0.0 — Trail RGB, gradient, TAB Heads and item-physics fixes (2026-11-15)

- Custom trail colors: two HEX/RGB inputs, solid/gradient/rainbow modes, six palettes, configurable alpha and width, persisted alongside existing World FX settings. Trail no longer joins teleportation gaps and color interpolation follows the ribbon.
- Air Hearts/Snowflakes: fix inverted world sprite UV mapping so soft hearts display right-side up.
- TAB Heads: optional local cached player-skin head portraits while TAB is held; capped list independent of vanilla player list.
- Pet: add original patterned 64×64 fabric with paw prints, subtle stars; batch fur/face/aura by RenderType to prevent shared buffer errors.
- Item Physics: use per-entity render state for grounded vs airborne item orientation; client-only look, server-authoritative physics untouched.
- Critical multiplayer renderer fix: stop interleaving line/fill and multi-texture consumers, addressing the confirmed `Not building!` crash.
- Build #213 and startup #97 pass, self-tests cover RGB, gradient, config persistence, TAB toggling, PNG and state metadata. Full visual/compatibility/FPS QA still needed.

---

# 2.0.0 — Soft Air Hearts / Snowflakes (2026-11-14)

- Replaced vanilla `ParticleTypes.HEART` and vanilla snowflake ambient spawning with an original shared world-space billboard sprite renderer.
- Created two original translucent RGBA heart/snowflake textures; soft snowfall-like downward drift, rotation, sway, fade and frame interpolation.
- Retained WORLD/CAMERA, LOW/MEDIUM/HIGH config and explicit MAX FPS visibility. Cap pooled sprites at 96, clear on world switch/toggle/global End; no external network effects.
- Caught an actually corrupted PNG via headless Minecraft startup (first smoke failed), regenerated both images as standard RGBA8 and retested.
- Build #186 SUCCESS, Minecraft startup #76 SUCCESS, 54 toggle tests + PNG alpha + pool lifecycle checks. No claim of shader, real-world or FPS testing.
- Documentation and visual acceptance test: AMBIENT_HEARTS_QA_RU.md.

---

# 2.0.0 Visuals Rework (2026-11-14, DEV TESTED BUILD)

- New original filled animated local pet, not former wireframe: eyes, ears, paws, moving wings/tail, original soft texture and six persisted colors.
- True third-person World Lyrics 3D billboard; first-person HUD and off-screen fallback. User-owned `|` separated lines, ~3.5 sec automatic cycling, visible original sample on first activation. No media-player integration.
- Continuous translucent Trail ribbons (max 25 samples), double expanding filled Jump Circles, filled translucent 36-sector China Hat, improved Target ESP foot halo.
- Solid JSON Custom Models (max 32 boxes), full-block tinted Block Overlay, gradient World Color, improved animated Hit FX and Blood Hit vignette.
- Common VisionGeometry for bounded quads/cubes/lines; fully independently authored effects and 32x32 PNG; no proprietary resources copied from the reference.
- Build #171 SUCCESS, startup #66 SUCCESS (54 persisted toggles + lyric text cycle + PNG validation). Not yet tested inside a world. Remaining acceptance across 35 modules, true Motion Blur, and real FPS measurements are open.

---

# VisionMod 2.0.0 — новая архитектура GUI + критичный фикс JEI (2026-11-14)

- Вместо устаревшего главного экрана, который раньше только перекрашивался, Right Shift теперь открывает новый самостоятельный `VisionDashboardScreen` (7 категорий, поисковая строка, сетка модулей, детализация по ПКМ, MMB сохранённых ID биндов). Дополнительные настройки Air Hearts/Snowflakes и доступ к старым расширенным редакторам через Advanced.
- Добавлен отдельный виртуальный запуск Minecraft в CI (`client-smoke.yml`) для выявления ошибок, которые компиляция не ловит.
- Исправлен реальный crash на стадии Fabric Loader: JEI 27.54.0.113 требовал mezz_config >=0.6.3,<1.0.0. Встроен MezzConfig 0.6.11 для Fabric 1.21.11, лицензия MIT включена.
- Actions #98: Build+Verify JEI+MezzConfig успешны, JAR проверен. Startup smoke #3/#4: клиент загрузил игровые ресурсы без ошибки разрешения модов; звуковое устройство отсутствует в CI.
- Качество и внешний вид GUI, взаимодействие в мире и полнота всех 35 функций ещё НЕ проверены вручную и не являются полностью готовыми. Fading Light имеет All-Rights-Reserved, чужой код/ассеты не скопированы.

---

# VisionMod 2.0.0 — expanded development code after #57

- Added 8 optional extended HUD/world utilities (Block Overlay, client-only kill notice, screen color, lyrics overlay, local Pet, local Social Radar, hit sound, precipitation particle filter).
- Added 5 optional motion settings (custom swing transform, dropped-item pose transform, visual spear lunge/pierce particles and camera streaks) and protected mixins.
- Added configurable original JSON third-person box-model support with bounded geometry (32 boxes), Fast Use with 7-tick throttle and selectable Hit Color.
- Extended Extras to 8 pages, left click toggles, right click changes world filter/hit color. Existing hotkey IDs preserved.
- Fixed two MC 1.21.11 compile errors in intermediate builds; Actions #80 compiled, verified JEI nested and uploaded 2.0.0 artifact.
- Important differences: Camera Streaks != temporal Motion Blur; lyrics render in HUD, not 3D; some world effects are bounded dotted/line prototypes. Never claim full feature parity or tested FPS gain. Remaining work tracked in FEATURE_REQUESTS_2_0_0.md.


---

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
