# AbsenPro — Aplikasi Absensi Karyawan Berbasis Lokasi (Geofencing)

Proyek UAS Pemrograman Perangkat Bergerak. Terdiri dari **dua bagian**:

1. **Backend REST API** — PHP + MySQL (dijalankan lokal via Laragon).
2. **Aplikasi Android** — Kotlin + Jetpack Compose (arsitektur MVVM).

Karyawan mendaftar mandiri, login, lalu absen masuk/pulang yang divalidasi
berdasarkan jarak GPS ke titik kantor (formula Haversine) dan jendela waktu
sesi yang dibuka admin. Karyawan juga bisa mengajukan izin dengan file bukti.
Admin mengelola karyawan, lokasi, sesi, approval izin, serta rekap + export CSV.

> Ringkasan kebutuhan lengkap ada di `PRD_AbsenPro.md`.

---

## Struktur Folder

```
AbsenPro/
├── PRD_AbsenPro.md          # dokumen kebutuhan (sumber kebenaran)
├── Database/
│   ├── schema.sql           # struktur tabel MySQL
│   ├── seed.sql             # data awal (admin + lokasi kantor)
│   
├── Backend_api/             # REST API PHP  (lihat Backend_api/README.md)
│   ├── config/db.php
│   ├── helpers/             # response, cors, auth_middleware, geo
│   ├── auth/                # register, login, logout
│   ├── karyawan/  lokasi_kantor/  sesi_absen/
│   ├── absensi/             # index (submit+riwayat), rekap, export_csv
│   ├── izin/                # index (ajukan/list), approval
│   └── uploads/izin/        # penyimpanan file bukti
└── app/                     # Aplikasi Android (Kotlin + Compose)
    └── src/main/java/com/julian/absenpro/
        ├── data/model/          # DTO cocok dg JSON backend
        ├── data/remote/         # Retrofit ApiService + RetrofitClient
        ├── data/local/          # SessionManager (EncryptedSharedPreferences)
        ├── data/repository/     # Repository (jembatan ke API)
        ├── di/ServiceLocator.kt # wadah dependency sederhana
        ├── ui/viewmodel/        # ViewModel (StateFlow + UiState)
        ├── ui/screen/           # Composable: auth, karyawan, admin
        ├── ui/navigation/       # NavGraph + Routes
        └── util/                # LocationUtils (Haversine), Constants, UiState
```

---

## Langkah Setup (di laptop ber-Android Studio + Laragon)

### 1. Backend + Database
1. Jalankan **Laragon** (Start All → Apache + MySQL menyala).
2. Copy folder `Backend_api/` ke `C:\laragon\www\absenpro_api\`.
3. Buka **phpMyAdmin**, Import `Database/schema.sql` lalu `Database/seed.sql`.
4. Buka `http://localhost/absenpro_api/` → harus muncul JSON `"AbsenPro API aktif."`.
5. (Opsional) bila login admin gagal, jalankan `Database/seed_admin.php`.

Detail endpoint & kontrak ada di **`Backend_api/README.md`**.

### 2. Aplikasi Android
1. Buka folder `AbsenPro/` (root) di Android Studio, tunggu Gradle sync.
2. Set alamat server di `app/.../util/Constants.kt` → `BASE_URL`:
   - **Emulator**: `http://10.0.2.2/absenpro_api/` (default, tidak perlu diubah).
   - **HP fisik** (satu WiFi dg laptop): ganti ke `http://<IP-laptop>/absenpro_api/`.
3. Run ke emulator/HP. Beri izin **Lokasi** saat diminta (dibutuhkan untuk absen).

### Akun default
- **Admin**: `admin` / `admin123` (dari seed).
- **Karyawan**: daftar sendiri lewat tombol "Daftar" di layar login.

---

## Alur Uji Cepat (Definition of Done)
1. Login admin → **Atur Lokasi Kantor** (isi koordinat lokasi ujimu) → simpan.
2. Admin → **Kontrol Sesi Absen** → buat sesi *masuk* status *aktif* (jam
   mencakup waktu sekarang).
3. Daftar akun karyawan → login sebagai karyawan.
4. Karyawan → **Absen Masuk**: berhasil bila dalam radius; ditolak bila di luar.
5. Karyawan → **Izin**: ajukan izin + file bukti → admin **Approval Izin**.
6. Admin → **Rekap** → filter → **Export CSV**.

---

## Catatan Keamanan (PRD bagian 10)
- Password di-hash `password_hash()` (bcrypt) di server — tidak ada plain text.
- Token sesi di Android disimpan **terenkripsi** via `EncryptedSharedPreferences`
  (kunci dikelola Android Keystore), bukan SharedPreferences biasa.
- Validasi radius & jendela waktu **diulang di server** karena input klien
  tidak bisa dipercaya sepenuhnya.

## Batasan (sesuai PRD, sengaja tidak dikerjakan)
Tanpa PDF (pakai CSV), tanpa push notification, tanpa login sosial, tanpa
pembuatan akun oleh admin (karyawan self-register), satu titik kantor saja.
