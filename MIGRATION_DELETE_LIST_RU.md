# Полное удаление Cosmetics при переходе на исходники 1.0.0

Обычная загрузка файлов в GitHub **не удаляет** старые файлы. Необходимо удалить их в старом репозитории вручную или через `git rm -r`, иначе удалённые исходники останутся в репозитории.

Удалить из GitHub:

- `COSMETICS_3D_README_RU.txt`
- `COSMETICS_FIXES_RU.txt`
- `COSMETICS_SYNC_FIX_RU.txt`
- `EXAMPLE_CUSTOM_COSMETICS/back/small_wings.json`
- `EXAMPLE_CUSTOM_COSMETICS/head/horns.json`
- `VISION_COSMETICS_README_RU.txt`
- `cloudflare/worker.js`
- `cloudflare/worker.mjs`
- `src/main/java/com/visionmod/CosmeticsFolder.java`
- `src/main/java/com/visionmod/CosmeticsScreen.java`
- `src/main/java/com/visionmod/VisionCosmetics.java`
- `src/main/resources/assets/visionmod/textures/cosmetics/back.png`
- `src/main/resources/assets/visionmod/textures/cosmetics/head.png`


Кроме того, удалены устаревшие README/инструкции, которые содержали старые сведения.

Для безопасного обновления через git: создайте новую ветку, замените содержимое проекта чистой папкой из полного ZIP (сохранив `.git`), затем выполните `git add -A`, `git commit -m "Vision 1.0.0: remove cosmetics, bundle JEI, fix FPS and FX"`, `git push`.

**Не удаляется автоматически:** локальная папка пользователя `config/visionmod/cosmetics`, старые приватные ключи и внешний Cloudflare Worker, уже развёрнутый в облаке. При желании удалите их вручную в лаунчере и Cloudflare Dashboard. Исходный проект больше не обращается к Worker.
