# Исправление сборки Vision Mod 1.0.0 — Fabric Loom / JEI

Симптом из GitHub Actions:
`Mod was built with a newer version of Loom (1.17.20), you are using Loom (1.14.10)`

Изменения:
- `build.gradle`: Fabric Loom 1.14.10 -> 1.17.20
- `.github/workflows/build.yml`: Gradle 9.2.1 -> 9.7.1
- Версия мода 1.0.0 НЕ изменена.

Применение: замените эти два файла в корне существующего репозитория, затем Actions -> Build Vision Mod 1.0.0 -> Run workflow.
Если загрузить файлы поверх старых через GitHub Upload files, убедитесь, что скрытая папка `.github` попала в коммит.

Проверено: наличие файлов в архиве, замены двух строк, версия мода, целостность ZIP.
Не проверено: Gradle-компиляция и запуск Minecraft; последующие ошибки могут потребовать отдельного исправления.
