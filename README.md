<img width="216" height="480" alt="Screenshot_2026-10-27-02-21-50-10_ca23d1134e1000ead75102e6a5a0078f" src="https://github.com/user-attachments/assets/50d051fe-a665-4e13-af97-512e807d8f5c" />
<img width="216" height="480" alt="Screenshot_2026-10-27-02-22-02-06_ca23d1134e1000ead75102e6a5a0078f" src="https://github.com/user-attachments/assets/28891099-0968-4fdc-bc51-fb321e0f7929" />
<img width="216" height="480" alt="Screenshot_2026-10-27-02-22-08-33_ca23d1134e1000ead75102e6a5a0078f" src="https://github.com/user-attachments/assets/0e8b5d16-2a5a-42b9-959a-7c3fa041b856" />


# [EN]Auto Clicker

An open-source, rootless, and fully offline auto-clicking and swiping application built for Android.

## Features

- **Click & Swipe:** Add as many click or swipe targets as you need, with freely draggable on-screen coordinates.
- **Independent Timing:** Configure delay intervals and press durations independently for each individual target.
- **Touch Protection:** Built-in safety mode preventing conflicting simultaneous taps (can be toggled off).
- **Jitter (Randomized Offset):** Optional randomized coordinate deviation to bypass strict bot-detection mechanisms.
- **Safe Sandbox Mode:** Preview and test your gesture sequences inside the app before dispatching them to the live screen.
- **Auto-Stop Failsafe:** Immediately halts clicking whenever the notification shade is pulled down or the screen turns off.
- **Floating Overlay:** Minimizes to the edge when active to keep your screen clear and unobstructed.
- **Scenario Management:** Save, load, and manage custom click scripts directly on your device storage.
- **Themes & Localization:** English and Turkish language support; Light, Dark, and AMOLED theme options.

## Privacy & Security

- **Zero Network Permissions:** `android.permission.INTERNET` is not requested. The app cannot connect to any network or transmit data off the device.
- **Blind Accessibility Service:** Configured with `canRetrieveWindowContent="false"` and `canRequestFilterKeyEvents="false"`. It cannot inspect UI hierarchies, read passwords, or capture keystrokes.
- **No Trackers or Ads:** Zero third-party analytics, crash loggers, ad networks, or telemetry libraries.
- **100% Local Storage:** Saved configurations stay strictly inside your device's sandboxed SQLite (Room) database.

## Tech Stack

- Kotlin & Jetpack Compose (Material 3)
- Android AccessibilityService & WindowManager
- Dagger Hilt
- Room & DataStore Preferences
- Kotlin Coroutines & Flow

## Build

To build from source:

```bash
git clone [https://github.com/salihfalanfilan/AutoClicker.git](https://github.com/salihfalanfilan/AutoClicker.git)
cd AutoClicker
./gradlew assembleDebug
```
# [TR]Otomatik Tıklayıcı

Android için geliştirilmiş, root gerektirmeyen, internet erişimi olmayan açık kaynaklı bir otomatik tıklama ve kaydırma uygulamasıdır.

## Özellikler

- **Tıklama ve Kaydırma:** Ekrana istenen sayıda tıklama veya kaydırma noktası eklenebilir, koordinatlar serbestçe sürüklenebilir.
- **Bağımsız Zamanlama:** Her hedefin tıklama aralığı ve basılı tutma süresi diğer hedeflerden bağımsız olarak ayarlanabilir.
- **Dokunma Koruması:** Farklı noktalara aynı anda basılmasını engelleyen güvenlik modu (istenirse kapatılabilir).
- **Sapma (Jitter):** Bot algılayıcıları için tıklama koordinatlarına hafif rastgele sapmalar ekleme seçeneği.
- **Güvenli Test Alanı:** Hareketleri gerçek ekrana göndermeden önce uygulama içinde görsel olarak deneyebilme.
- **Otomatik Durdurma:** Bildirim çekmecesi açıldığında veya ekran kapatıldığında işlemi anında keser.
- **Yüzen Panel:** Tıklama başladığında ekranı kapatmamak için kenara çekilip küçülebilir.
- **Senaryo Kaydı:** Oluşturulan tıklama düzenleri cihaz hafızasına kaydedilip tekrar yüklenebilir.
- **Tema ve Dil:** Türkçe ve İngilizce desteği; Açık, Koyu ve AMOLED tema seçenekleri.

## Gizlilik ve Güvenlik

- **İnternet İzni Yok:** `android.permission.INTERNET` izni bulunmaz. Uygulama ağa bağlanamaz ve dışarıya hiçbir veri gönderemez.
- **Ekran İçeriği Okunamaz:** Erişilebilirlik yapılandırmasında `canRetrieveWindowContent="false"` ve `canRequestFilterKeyEvents="false"` olarak ayarlıdır. Ekrandaki metinler, şifreler veya basılan tuşlar dinlenemez.
- **Takip / Reklam Yok:** Kod tabanında reklam, analitik veya harici telemetri kütüphaneleri yer almaz.
- **Yerel Depolama:** Kaydedilen senaryolar yalnızca cihazın yerel SQLite (Room) veritabanında tutulur.

## Kullanılan Teknolojiler

- Kotlin & Jetpack Compose (Material 3)
- Android AccessibilityService & WindowManager
- Dagger Hilt
- Room & DataStore Preferences
- Kotlin Coroutines & Flow

## Derleme

Kaynak koddan derlemek için:

```bash
git clone https://github.com/salihfalanfilan/AutoClicker.git
cd AutoClicker
./gradlew assembleDebug
