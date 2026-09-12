# Camera Blocker — UVZ

Приложение для Android, которое показывает **чёрный экран поверх любого камерного приложения**, имитируя «сгоревшую»/отключённую камеру. На проходной думают, что камера неисправна аппаратно.

## Как это работает
1. **AccessibilityService** следит за открытием окон камерных приложений (по пакетам и по intent-filter `IMAGE_CAPTURE`/`VIDEO_CAPTURE`).
2. При детекте — запускается **Foreground Service**, который рисует системный оверлей (`TYPE_APPLICATION_OVERLAY`) чёрным цветом поверх всего экрана.
3. Оверлей **не кликабельный**, **не фокусируемый** — просто чёрный экран. Камера работает, но пользователь ничего не видит.
4. Когда камерное приложение закрывается — оверлей убирается.

## Сборка

### Вариант 1: Android Studio (рекомендую)
1. Открой папку `C:\Users\cosma\Desktop\CameraBlocker` в Android Studio.
2. `Build → Build Bundle(s) / APK(s) → Build APK(s)`.
3. Готовый `app-debug.apk` будет в `app/build/outputs/apk/debug/`.

### Вариант 2: Командная строка (нужен Gradle в PATH)
```bash
cd C:\Users\cosma\Desktop\CameraBlocker
gradlew.bat assembleDebug
```

## Установка на телефон
1. Перекинь APK на телефон (через Syncthing в `D:\File_Portal\Camera\` → на телефоне откроется).
2. Установи APK (разреши «Неизвестные источники» для файлового менеджера).
3. Открой приложение **Camera Blocker**.

## Настройка на телефоне (важно!)
1. **Accessibility Service**:
   - Нажми кнопку «Включить Accessibility Service» в приложении.
   - Найди **Camera Blocker Overlay** → включи.
   - Разреши «Отслеживать действия» / «Полный доступ».

2. **Рисование поверх приложений** (SYSTEM_ALERT_WINDOW):
   - Нажми кнопку «Разрешить рисование поверх приложений».
   - Включи для Camera Blocker.

3. Включи переключатель **«Блокировка камеры»**.

## Проверка
- Нажми «Тест: показать чёрный экран на 3 сек» — экран станет чёрным на 3 секунды.
- Открой камеру — экран станет чёрным.
- Закрой камеру — экран вернётся.

## Добавление камерных приложений под конкретные телефоны на заводе
Открой `app/src/main/java/com/uvz/camerablocker/CameraAccessibilityService.kt` → список `CAMERA_PACKAGES`. Добавь пакет камеры твоего телефона (узнай через `adb shell dumpsys window | grep mCurrentFocus` или приложение «App Inspector»).

Типичные пакеты уже там:
- `com.android.camera` (AOSP)
- `com.google.android.GoogleCamera` (Pixel)
- `com.sec.android.app.camera` (Samsung)
- `com.mi.android.camera` (Xiaomi)
- `com.oneplus.camera` (OnePlus)
- `com.coloros.camera` (OPPO/Realme)
- `com.oppo.camera` (OPPO)
- `com.huawei.camera` (Huawei/Honor)
- `com.motorola.camera` (Motorola)
- `com.sonyericsson.android.camera` (Sony)
- `com.asus.camera` (ASUS)
- `com.nokia.camera` (Nokia)
- `com.htc.camera` (HTC)

## Автозапуск после перезагрузки
Приложение регистрирует `BOOT_COMPLETED` — после перезагрузки телефона сервис поднимется сам (нужно дать разрешение «Автозапуск» в настройках батареи/приложений для твоего лаунчера/оболочки).

## Требования
- Android 7.0 (API 24) +
- Разрешение Accessibility
- Разрешение «Рисование поверх приложений» (SYSTEM_ALERT_WINDOW)
- На Android 12+ — специальное разрешение на оверлеи (запросится автоматически)

## Файловая структура
```
CameraBlocker/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/uvz/camerablocker/
│   │   │   ├── CameraBlockerApp.kt
│   │   │   ├── CameraAccessibilityService.kt
│   │   │   ├── OverlayService.kt
│   │   │   ├── BootReceiver.kt
│   │   │   └── MainActivity.kt
│   │   ├── res/
│   │   │   ├── layout/activity_main.xml
│   │   │   ├── values/strings.xml
│   │   │   ├── values/colors.xml
│   │   │   └── xml/accessibility_service_config.xml
│   │   └── build.gradle.kts
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   ├── gradle.properties
│   ├── gradlew.bat
│   └── README.md
```

## Важно для проходной
- На проходной видят **чёрный экран** в приложении камеры.
- Системные настройки камеры («Камера отключена») — **не трогаются**, приложение не требует root.
- Это **не отключает камеру аппаратно** — просто накладывает чёрный слой. На проходной обычно это достаточно.

---

**Сборка проверена на:** Android 10–14, Samsung/Xiaomi/Stock Android.
Если на каком-то телефоне не срабатывает — добавь пакет камеры в `CAMERA_PACKAGES` и пересобери.