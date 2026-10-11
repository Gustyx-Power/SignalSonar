# SignalSonar (Pencari Sinyal Seluler)

<div align="center">
  <img src="app/src/main/res/drawable/ic_launcher_foreground.xml" width="128" height="128" alt="SignalSonar Logo" />
  <h3>Radar & Acoustic Geiger-Counter untuk Menemukan Titik Sinyal Seluler Terkuat</h3>
  <p><strong>Package:</strong> <code>id.gustyx.signalsonar</code> | <strong>Platform:</strong> Android Native (Kotlin & Jetpack Compose)</p>
</div>

---

## 📌 Ringkasan Produk
**SignalSonar** adalah aplikasi Android Native berarsitektur modern yang berfungsi layaknya radar dan Geiger counter untuk mendeteksi serta memetakan sinyal seluler (4G LTE / 5G NR / 3G). Aplikasi ini dirancang untuk memudahkan pengguna menemukan posisi sinyal terkuat saat memasang antena modem, perangkat Wi-Fi Orbit/CPE, atau saat mencari sinyal di daerah pelosok tanpa perlu terus-menerus menatap layar ponsel.

Aplikasi menghadirkan **umpan balik akustik real-time (Acoustic Sonar Feedback)** menggunakan engine audio berlatensi rendah (`SoundPool`), di mana frekuensi dan kecepatan detak *beep* berakselerasi seiring dengan meningkatnya kekuatan sinyal (dBm).

---

## ✨ Fitur Utama

### 1. 📡 Real-time Cellular Signal Monitor
- Membaca telemetri kekuatan sinyal seluler secara langsung dalam satuan **dBm** dan **ASU**.
- Mendeteksi generasi dan tipe jaringan aktif: **5G NR**, **4G LTE**, **3G HSPA**, dan **2G GSM**.
- Mendukung pemantauan khusus pada slot kartu **Default Data SIM** dalam skenario Dual-SIM.
- Menampilkan nama operator jaringan dan status bar sinyal secara real-time.

### 2. 🔊 Acoustic Sonar Feedback (Geiger Beep Engine)
- Memanfaatkan **SoundPool** untuk pemutaran audio berlatensi ultra-rendah dan efisiensi memori maksimal tanpa risiko leak.
- **Logika Mapping Interval Audio:**
  - 🟢 **Sangat Kuat ( $\ge$ -75 dBm):** Beep sangat cepat (~120ms - 200ms).
  - 🔵 **Bagus / Optimal (-76 s/d -90 dBm):** Beep menengah (~400ms).
  - 🟡 **Cukup / Lemah (-91 s/d -105 dBm):** Beep lambat (~800ms).
  - 🔴 **Sangat Buruk / Kritis ($\le$ -106 dBm):** Beep lambat (~1500ms) dengan nada flat warning tone.
- Dilengkapi tombol sakelar di antarmuka untuk menyalakan/mematikan suara sonar kapan saja.

### 3. 🏆 Peak Signal Hold
- Mengunci dan mencatat nilai dBm tertinggi (sinyal terkuat) yang berhasil ditemukan selama sesi pencarian berlangsung beserta tipe jaringannya.
- Dilengkapi tombol **Reset Peak** untuk memulai pencarian dari awal.

### 4. 📱 Keep Screen On & Lifecycle Management
- Mengaktifkan `FLAG_KEEP_SCREEN_ON` saat mode radar aktif agar layar ponsel tidak mati ketika pengguna berjalan memindai ruangan/atap.
- Menghentikan coroutine timer dan mematikan stream SoundPool pada callback `onPause` dan `onDestroy` untuk mencegah audio bocor di latar belakang.

### 5. 🎨 Material You Expressive & Custom Radar Visualizer
- Menggunakan tema **Material 3 Expressive** dengan dukungan **Dynamic Color (Android 12+)**.
- **Hero Radar Visualizer:** Animasi radar kustom berbasis Canvas Jetpack Compose dengan gelombang ripple yang kecepatannya disinkronkan secara presisi dengan interval beep suara.
- Color indicator dinamis: Neon Green (Kuat), Cyan/Teal (Optimal), Amber (Sedang), Coral Red (Kritis).

---

## 🏗️ Arsitektur & Spesifikasi Teknis

- **Bahasa Pemrograman:** Kotlin 2.0
- **UI Framework:** Jetpack Compose & Material 3 (`androidx.compose.material3`)
- **Pola Desain:** MVVM (Model-View-ViewModel) dengan Unidirectional Data Flow (UDF) via `StateFlow`
- **Audio Engine:** `SoundPool` dengan audio attributes sonifikasi assistif
- **Telephony API:**
  - **API 31+ (Android 12+):** `TelephonyCallback` (`SignalStrengthsListener`, `DisplayInfoListener`, `ServiceStateListener`)
  - **API 29-30 (Android 10 & 11 Fallback):** `PhoneStateListener`
- **Minimum SDK:** API 29 (Android 10)
- **Target SDK:** API 34 (Android 14)
- **Dependency Injection / Lifecycle:** AndroidX Lifecycle ViewModel Compose, Kotlin Coroutines Flow

---

## 🔐 Manajemen Izin (Permissions)

SignalSonar mengelola izin runtime secara transparan:
1. `READ_PHONE_STATE`: Membaca slot SIM data default dan operator.
2. `ACCESS_FINE_LOCATION` & `ACCESS_COARSE_LOCATION`: Diwajibkan oleh platform Android 10+ untuk mengakses telemetri tower seluler.

Jika izin ditolak, aplikasi menyajikan **Interactive Empty State** dengan instruksi jelas serta tombol cepat menuju pengaturan sistem aplikasi.

---

## 🛠️ Build & Kompilasi

Proyek ini dibangun menggunakan Gradle 8.9 dan Android Gradle Plugin 8.5+:

```bash
# Menjalankan Unit Test
./gradlew testDebugUnitTest

# Membangun Debug APK
./gradlew assembleDebug

# Lokasi APK yang dihasilkan:
# app/build/outputs/apk/debug/app-debug.apk
```