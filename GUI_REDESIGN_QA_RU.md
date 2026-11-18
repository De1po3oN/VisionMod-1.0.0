# Обновлённый GUI Vision

Обновление подписи 2026-11-18: code `95134c5b6d47d3bacb17b1b4835fd44299b3c999`, evidence `fbb3ccf15654feab0f13b81ef5d44e1aa7f68583`. Внизу главного меню и настроек восстановлены **ТГК @cocok_ali · Сервер mc.showtime.su**. Кадры компактного меню и настроек просмотрены: текст читается, кнопки не перекрыты. [JAR](https://github.com/De1po3oN/VisionMod-1.0.0/raw/fbb3ccf15654feab0f13b81ef5d44e1aa7f68583/build/visionmod-2.0.0.jar) (5 825 615 байт), SHA-256 `99dc73cc4ad682a7ef080511f101de81f1fe4c1c768edd3e7a90e142477c4fbd`. Build [44274108412](https://github.com/De1po3oN/VisionMod-1.0.0/actions/runs/44274108412), client [44274108433](https://github.com/De1po3oN/VisionMod-1.0.0/actions/runs/44274108433), world [44274108458](https://github.com/De1po3oN/VisionMod-1.0.0/actions/runs/44274108458) — PASS, source.json всех трёх совпадает с этим кодом.

Код: `611d5095ae3245959332d018f22d5c40bcc904ee`, ветка `codex/visual-refresh`.

Что изменено:
- Матовая тёмная панель, более спокойные рамки и тени, сглаженные края. Отдельные поверхности для навигации, поиска и карточек. Палитры пользователя сохранены.
- Иконки разделов, компактная боковая панель для небольшого GUI и полные подписи при достаточной ширине. Активный раздел перемещает подсветку плавно.
- На минимальном GUI 320×240 видны две карточки; при GUI 640×360 — четыре; при 720×450 — шесть. Все координаты рисования и кликов используют DashboardLayout.
- Статические шрифты Vision UI из открытого Inter: текст 450, заголовки 650. Устранена прежняя слишком тонкая типографика. Шрифты Minecraft и мировой лирики не заменяются. Открытая лицензия и происхождение: THIRD_PARTY_NOTICES.md; воспроизводимый генератор tools/generate-ui-fonts.py. FontTools нужен только разработчику при повторном создании шрифтов.
- Описания модулей переведены на русский. Карточки показывают состояние и назначенную клавишу; кнопка параметров открывается ЛКМ, ПКМ по карточке и СКМ для назначения клавиши сохранены.
- Поиск получил собственный фон, общую типографику подсказки, очистку и фокусировку при клике по всей поверхности поля.
- Экраны настроек получили общую рамку, заголовки, кнопки и строки параметров. Идентификаторы модулей, настройки, горячие клавиши и память текущего экрана не менялись.

Проверки:
- Локальные регрессии: 365 core (включая границы, плотность карточек, пересечения навигации и футера), 259 pet motion, 12 world motion, 22 lyric source, 18 lyric lookup, 49 multi-provider/Yandex; lyric entrance, spear trigger и CPU-геометрия — PASS. Полная компиляция выполняется в CI настоящим JDK 21 / --release 21.
- Новый игровой GUI_DESIGN проверяет настоящие клики по карточке, разделам, кнопке параметров, полю поиска и очистке; снимки 640×480, 1280×720, 1440×900 при GUI scale 2; страницы оформления, другую палитру, закрытие и повторное открытие через Right Shift.
- Старая MENU_MEMORY проверяет категорию, страницу, запрос, окно параметров, вложенный редактор и изменение размера. Сам сценарий использует новый минимальный GUI, чтобы проверять реальную вторую страницу при повышенной вместимости.

Финальная проверка этого code SHA:
- [Полная сборка](https://github.com/De1po3oN/VisionMod-1.0.0/actions/runs/44173593757) — PASS.
- [Клиент: 54 переключателя и 8 кадров](https://github.com/De1po3oN/VisionMod-1.0.0/actions/runs/44173593622) — PASS.
- [Игровой прогон](https://github.com/De1po3oN/VisionMod-1.0.0/actions/runs/44173593625) — PASS: GUI_DESIGN, MENU_MEMORY, CONTROLS, INVENTORY, LYRICS_PRESENCE, WORLD_PLANE, PET_MOTION, SPEAR, WORLDTEST. Настоящий поиск LRCLIB вернул 58 строк с таймкодами; dedicated server — Done (3.405s).

Кадры просмотрены вручную: [большой GUI](https://raw.githubusercontent.com/De1po3oN/VisionMod-1.0.0/9289d6d3e832aad246a04ae4125757acec2322cb/world/33-gui-desktop.png), [компактный](https://raw.githubusercontent.com/De1po3oN/VisionMod-1.0.0/9289d6d3e832aad246a04ae4125757acec2322cb/world/31-gui-compact.png), [обычный](https://raw.githubusercontent.com/De1po3oN/VisionMod-1.0.0/9289d6d3e832aad246a04ae4125757acec2322cb/world/32-gui-standard.png), [настройки](https://raw.githubusercontent.com/De1po3oN/VisionMod-1.0.0/9289d6d3e832aad246a04ae4125757acec2322cb/world/34-gui-appearance.png), [мятная тема](https://raw.githubusercontent.com/De1po3oN/VisionMod-1.0.0/9289d6d3e832aad246a04ae4125757acec2322cb/world/35-gui-mint-theme.png). На компактном кадре присутствует стандартное уведомление Minecraft о рецепте; это не элемент Vision.

[Готовый JAR](https://github.com/De1po3oN/VisionMod-1.0.0/raw/9289d6d3e832aad246a04ae4125757acec2322cb/build/visionmod-2.0.0.jar) — Fabric 1.21.11, 5 825 295 байт.
SHA-256: `850134ee80b3261c78f60cb1f3ad1a8234f5abb0fc6595c8a4c0211a85283afa`.
Evidence commit: `9289d6d3e832aad246a04ae4125757acec2322cb`; build/world/smoke/source.json проверены на совпадение с code SHA выше.
В JAR проверены ресурсы обоих статических шрифтов и лицензия. Исходный мировой Inter сохранил SHA-256 `29160a80ff49ddcab2c97711247e08b1fab27a484a329ce8b813d820dc559031`. Main не менялась.
