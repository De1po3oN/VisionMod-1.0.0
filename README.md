## Важное обновление 2.0.0 / критические исправления (2026-11-14)
**Free Look:** удерживать Left Alt => камера вращается отдельно от игрока, отпускание => автоматический возврат предыдущей перспективы и сброс угла. Удержание орбиты после отпускания отменено по прямому требованию пользователя; F5 для сброса не нужен.

**Реальные баги исправлены в исходниках:** в MC 1.21.11 метод ParticleEngine называется `createParticle` / `add`, а не `addParticle`; прежний particle filter мог вообще не применяться. Исправлены точки внедрения Mixin и включено требование успешной инъекции. Система MAX FPS/Hide All теперь должна пропускать явно включённые частицы Vision. Hit/Blood/Kill/Sound FX наблюдают событие нашей атаки, а не потерянную после удара цель под прицелом; исправлены пороги и позиционирование spear particles. Убран переход `ADVANCED` в старое главное GUI; текст World Lyrics и имена Social Radar вводятся в новом интерфейсе.

**Проверки:** Actions Build #146 SUCCESS, Minecraft startup smoke #45 SUCCESS, `VISIONMOD_SELFTEST` проверил **54 переключателя и сохранение их конфигураций**. Это **НЕ** равно 54 успешно работающим визуальным функциям. Реальная приемка 35 требований 2.0.0, FPS и визуальные тесты всё ещё не завершены — `QA_35_QUALITY_GATE_RU.md`. До этого нельзя обещать 80% уверенности по каждому модулю. Скачать тестовый JAR из [GitHub Actions #146](https://github.com/De1po3oN/VisionMod-1.0.0/actions/runs/43595887218).

---
## Обновление GUI и исправление запуска (2026-11-14)

**Новый главный экран:** Right Shift теперь открывает `VisionDashboardScreen` с категоризированными карточками, поиском, подробностями по ПКМ, биндами старых модулей по СКМ, настройками Air Hearts и Snowflakes. Старый `VisionScreen` сохранён в **Advanced** для сложных редакторов.

**Критичный фикс запуска:** JEI 27.54.0.113 требует `mezz_config` >=0.6.3,<1.0.0. Теперь внутрь JAR вложен **MezzConfig 0.6.11 Fabric 1.21.11** с MIT уведомлением. Startup smoke #1/#2 обнаружили отсутствие зависимости; smoke #3/#4 после исправления достиг загрузки Minecraft ResourceManager. При отсутствии звуковой карты на runner Minecraft отключает звук, но продолжает выполнение.

Проверенная сборка: [Actions #98](https://github.com/De1po3oN/VisionMod-1.0.0/actions/runs/43584531967), SHA исходников `d5a1c941`; артефакт `VisionMod-2.0.0`. SHA256 JAR `b0dd9a7e2f0e4840ccaca2d981f7c0627ed89f93d46d742179f198924b340bc0`. Инструкция установки: [GUI_2_0_TEST_GUIDE_RU.md](GUI_2_0_TEST_GUIDE_RU.md). **Игровой визуальный QA, все 35 функций и FPS замеры ещё не подтверждены.**

---

## VisionMod 2.0.0 — актуальная проверенная сборка кода

Последний проверенный **кодовый** коммит: `19c713cd`. [GitHub Actions #80](https://github.com/De1po3oN/VisionMod-1.0.0/actions/runs/43579928856) — SUCCESS; артефакт VisionMod-2.0.0 содержит `visionmod-2.0.0.jar` и вложенный JEI 27.54.0.113. Это CI-подтверждение, **не игровой QA**.

EXTRAS содержит 8 страниц. 2.0 группы:
- **HUD & COMBAT:** Aspect Bars, colored armor durability, Target Health, Hit Range, TNT distance, nearest Arrow Name, Hit Pulse, Blood Hit.
- **WORLD FX:** Target ESP outline, Trail dots, Jump Circle dots, wireframe China Hat.
- **EXTRA FX:** Block Overlay, target death notice, screen color grading, own-text lyrics HUD, local wire pet, friend name radar, vanilla hit sound, rain/snow particle suppression.
- **MOTION FX:** optional first-person Swing Animation, dropped-item pose tilt/spin, visual spear lunge/pierce particle feedback, Camera Streaks (not actual motion blur).
- **TOOLS:** Fast Use (bounded normal vanilla use action), original JSON custom box models in `config/visionmod/custom-model.json`, selectable Hit Color (RMB changes color).

All new options are OFF by default; toggles are in Extras with persistent local .properties settings. Some features are prototypes or only partial equivalents. **Motion Blur itself, source-accurate UI/animations and full behavior of all 35 features are not finished.** See `FEATURE_REQUESTS_2_0_0.md`. Avoid marketing this as a complete recreation of any proprietary visual mod.


---

# Vision Mod 2.0.0 — Open Beta development

Minecraft Java 1.21.11 / Fabric Loader >=0.19.3 / Java 21.

## What's new in 2.0.0 (work in progress)
- Added eight optional HUD/combat modules in **EXTRAS / 2.0 HUD & COMBAT**: Aspect Bars, Armor Colorful (durability HUD), Target Health, Hit Range, TNT Indicator (distance), Arrow Names (nearest arrow HUD), Hit Pulse, Blood Hit (edge tint).
- Added four optional original world-space effects in **EXTRAS / 2.0 WORLD FX**: Target ESP outline, capped Trails, ring of Jump Circles dots, low-poly third-person China Hat. No external cosmetics networking.
- All new effects are OFF by default. Settings persist in config/visionmod/features-2.properties and world-effects-2.properties.
- The user supplied a third-party 2.0 JAR for reference, but its All-Rights-Reserved code/assets/fonts/sounds have not been copied.
- **Incomplete:** Many requested modules remain pending; see FEATURE_REQUESTS_2_0_0.md for full status. CI build is not an in-game runtime test.

## What's new in 1.1.0 (source/CI candidate; runtime not yet tested)
- Free Look: smoothed, independent third-person camera orbit; camera perspective cleanup and configurable smoothing. Hold Left Alt, release to restore the previous perspective immediately.
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
`gradle clean build --no-daemon --stacktrace` (Java 21, Gradle 9.2.1). The nested JEI JAR will be downloaded on the build runner from Modrinth Maven. Build result: `build/libs/visionmod-2.0.0.jar`.

Status: 1.1.0 source update. GitHub Actions compilation is checked separately; in-game performance and visual behavior are **not yet verified**. Exact FPS gains depend on CPU/GPU, entity count, shaders and baseline draw distance.

**JEI server caveat**: on Minecraft 1.21.11, certain server-provided/custom recipes may require JEI or recipe synchronization installed server-side to appear in the client.


## 1.1.0 GUI and ambient options
- New graphite/lavender Vision theme with original geometry, muted contrast and clearer module switches. Does not contain Fading Light code or assets.
- Extras → Settings (page 3) lets users choose Ambient Mode (WORLD/CAMERA), Ambient Density (LOW/MEDIUM/HIGH), Smooth Free Look (ON/OFF). Right-click Air Hearts or Snowflakes to jump to this page; right-click Free Look for quick smooth toggle.
- Free Look controls the independent third-person camera on Left Alt; release resets the orbit and restores the previous camera perspective. Smooth interpolation can be disabled.
- AIR HEARTS and Snowflakes are still vanilla particle types, but emit across a world-space area or an optional camera-space halo. No custom heart texture was copied. Visual correctness requires a client test.
