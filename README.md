# GREY Puzzle AI — Aplikasi Android Mandiri (APK)

Aplikasi Android native (Kotlin, minSdk 30 / Android 11+) yang berjalan langsung di HP tanpa perlu PC:
- **Otak AI via API Key (`GeminiPuzzlePlanner.kt`):** Mengirim tangkapan layar papan ke Gemini API menggunakan API key yang Anda masukkan di aplikasi.
- **Kontrol Sentuh HP (`PuzzleAccessibilityService.kt`):** Mengambil screenshot layar game (`takeScreenshot`) dan mengeksekusi gerakan *drag-and-drop* (`dispatchGesture`) langsung di atas layar HP.
- **Kontrol Melayang (`OverlayControlService.kt`):** Tombol melayang (`1 Langkah AI` dan `Auto: ON/OFF`) yang muncul di atas game block puzzle.

## Cara Menjadi File `.apk` untuk Diunduh & Diinstal di HP
1. **Opsi 1 — Gratis via GitHub Actions (tanpa instal Android Studio di laptop):**
   - Unggah isi folder proyek ini ke repositori GitHub Anda.
   - Buka tab **Actions** -> jalankan workflow **Build Android Debug APK** -> unduh artefak **`GREY-Puzzle-AI-debug-apk`** (`app-debug.apk`) langsung ke HP lalu instal.
2. **Opsi 2 — Lokal via Android Studio / Gradle:**
   - Buka folder proyek di Android Studio, lalu pilih **Build -> Build Bundle(s) / APK(s) -> Build APK(s)**.

## Cara Menjalankan di HP Setelah APK Terinstal
1. Buka **GREY Puzzle AI**, masukkan **Gemini API Key**, lalu tekan **1. Simpan Pengaturan**.
2. Tekan **2. Aktifkan Accessibility Service** -> aktifkan layanan **GREY Puzzle AI**.
3. Tekan **3. Izinkan Tombol Melayang (Overlay)** -> beri izin tampil di atas aplikasi lain.
4. Tekan **4. Tampilkan Kontrol Melayang di Game**, buka game *block puzzle* Anda, lalu tekan **1 Langkah AI** atau **Auto: ON**.
