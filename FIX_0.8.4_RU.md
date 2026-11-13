# VisionMod 0.8.4 — исправление вылета при запуске

## Причина
Ошибка `InvalidInjectionException` в `LowFireMixin` при загрузке Minecraft 1.21.11. Старый метод инъекции ожидал `(Minecraft, PoseStack, CallbackInfo)`; реальный `renderFire` в 1.21.11 принимает `(PoseStack, MultiBufferSource, TextureAtlasSprite)`.

## Патч
Заменён `src/main/java/com/visionmod/mixin/LowFireMixin.java`: оба обработчика HEAD/RETURN теперь имеют правильные параметры. Функция Low Fire и 51 бинд сохранены. Версия в `build.gradle` и имя артефакта Actions обновлены на 0.8.4.

## Установка
1. Загрузи исходники в GitHub. Если используешь мини-патч, замени `LowFireMixin.java` и `build.gradle`/`.github/workflows/build.yml` по тем же путям.
2. В GitHub Actions запусти Build Vision Mod 0.8.4.
3. После успешной сборки извлеки JAR из раздела Artifacts, убери старый visionmod-0.8.3.jar из папки mods и установи новый.
4. Запусти Minecraft и проверь Low Fire с включённым и выключенным переключателем.

## Статус
Исходники проходят синтаксическую проверку; игра и полный Gradle build здесь не запускались. Успех исправления подтверждается только новым запуском Minecraft без `LowFireMixin InvalidInjectionException`.
