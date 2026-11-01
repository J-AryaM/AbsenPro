# Product Requirements Document (PRD)
## AbsenPro — Aplikasi Absensi Karyawan Berbasis Lokasi (Geofencing)

| Item | Detail |
|---|---|
| Nama Proyek | AbsenPro |
| Jenis Dokumen | Product Requirements Document (PRD) |
| Mata Kuliah | Pemrograman Perangkat Bergerak (UAS Praktikum) |
| Platform | Android (Native) |
| Versi Dokumen | 1.1 (revisi: pemangkasan scope untuk beban kerja individu) |
| Tanggal | 4 Juli 2026 |
| Sifat Pengerjaan | Individu |

---

> **Catatan Revisi v1.1:** Dua penyesuaian scope dari versi awal untuk menyesuaikan beban kerja individu: (1) export laporan diganti dari PDF menjadi **CSV sederhana**, dan (2) pembuatan akun karyawan oleh admin diganti menjadi **registrasi mandiri (self-register)** oleh karyawan.

## 1. Latar Belakang

Proses absensi karyawan secara manual (tanda tangan/kertas) atau berbasis kode QR statis rawan dimanipulasi karena tidak memverifikasi keberadaan fisik karyawan di lokasi kerja. **AbsenPro** dikembangkan untuk menjawab masalah ini dengan memvalidasi absensi berdasarkan **titik koordinat GPS (latitude & longitude)** karyawan dibandingkan titik koordinat kantor yang telah ditentukan administrator, dalam radius toleransi tertentu (geofencing). Selain itu, aplikasi ini menyediakan alur pengajuan izin/cuti digital lengkap dengan bukti pendukung, sehingga seluruh proses administrasi kehadiran dapat dilakukan dari perangkat mobile.

## 2. Tujuan Produk

1. Memvalidasi kehadiran karyawan secara objektif menggunakan data lokasi GPS, bukan sekadar interaksi tap tombol.
2. Menyederhanakan proses pengajuan dan persetujuan izin karyawan secara digital (paperless).
3. Memberikan admin kontrol penuh atas jam operasional absensi (buka/tutup sesi masuk & pulang) serta pemantauan data karyawan.
4. Menghasilkan rekap kehadiran yang dapat diekspor dalam format CSV sederhana untuk kebutuhan administrasi.
5. Menerapkan praktik keamanan dasar (hashing kredensial & enkripsi data sensitif) pada aplikasi yang terhubung ke backend API.

## 3. Ruang Lingkup (Scope)

### 3.1 In-Scope
- Aplikasi Android (Kotlin + Jetpack Compose) untuk dua peran: **Karyawan** dan **Admin**.
- Backend REST API yang terhubung ke database MySQL (lingkungan lokal via Laragon).
- Fitur absensi berbasis geofencing (radius dari titik kantor).
- Fitur pengajuan izin dengan upload file bukti (gambar/PDF) dan proses approval oleh admin.
- Fitur registrasi mandiri (self-register) untuk karyawan baru membuat akunnya sendiri.
- Fitur kontrol sesi absensi (buka/tutup jendela waktu absen masuk & pulang) oleh admin.
- Fitur rekap absensi dan export ke CSV sederhana.
- Fitur pengaturan titik koordinat kantor (lat/long acuan) oleh admin.
- Keamanan dasar: hashing password & enkripsi data penting saat transmisi/penyimpanan.

### 3.2 Out-of-Scope
- Multi-cabang kantor (hanya satu titik lokasi acuan per versi ini).
- Integrasi payroll/penggajian otomatis.
- Notifikasi push real-time (dapat menjadi pengembangan lanjutan).
- Deployment ke server produksi/cloud (pengujian hanya di lingkungan lokal).
- Login sosial (Google/Facebook, dsb).
- Generate PDF laporan (diganti dengan export CSV).
- Pembuatan akun karyawan oleh admin (diganti dengan registrasi mandiri oleh karyawan).

## 4. Aktor & Peran Pengguna

| Aktor | Deskripsi |
|---|---|
| **Admin** | Memantau data karyawan, mengatur lokasi kantor, sesi absensi, approval izin, dan rekap laporan. |
| **Karyawan** | Mendaftar mandiri (registrasi akun sendiri), melakukan absen masuk/pulang berbasis lokasi, mengajukan izin, dan melihat riwayat kehadiran pribadi. |

## 5. Kebutuhan Fungsional (Functional Requirements)

### 5.1 Modul Karyawan

| ID | Kebutuhan | Deskripsi |
|---|---|---|
| F-00 | Registrasi Mandiri | Karyawan baru mendaftar sendiri melalui form register (nama, username/email, password, data diri dasar) tanpa perlu dibuatkan akun oleh admin. Password langsung di-hash sebelum dikirim/disimpan. |
| F-01 | Login | Karyawan login menggunakan akun yang telah didaftarkan sendiri (username/email + password). |
| F-02 | Absen Masuk | Sistem mengambil lokasi GPS aktual, menghitung jarak ke koordinat kantor (formula Haversine), dan mengizinkan absen hanya jika dalam radius & jendela waktu yang dibuka admin. |
| F-03 | Absen Pulang | Sama seperti F-02, namun untuk sesi pulang, dan hanya aktif jika admin telah membuka sesi absen pulang. |
| F-04 | Riwayat Absensi Pribadi | Karyawan dapat melihat histori kehadiran (tanggal, jam masuk/pulang, status: hadir/telat/tidak hadir). |
| F-05 | Pengajuan Izin | Karyawan mengisi form izin (jenis izin, tanggal, keterangan) dan mengunggah file bukti (foto/dokumen/PDF). |
| F-06 | Status Pengajuan Izin | Karyawan dapat memantau status pengajuan izin (Menunggu / Disetujui / Ditolak). |
| F-07 | Profil | Karyawan dapat melihat data profil dasar miliknya. |

### 5.2 Modul Admin

| ID | Kebutuhan | Deskripsi |
|---|---|---|
| F-08 | Login Admin | Autentikasi khusus admin dengan hak akses berbeda dari karyawan. |
| F-09 | Kelola Karyawan | Admin dapat melihat daftar karyawan yang sudah mendaftar mandiri, mengubah data, dan menonaktifkan akun karyawan (tanpa membuat akun secara manual — akun dibuat sendiri oleh karyawan lewat F-00). |
| F-10 | Atur Lokasi Kantor | Admin dapat mengatur/mengubah titik koordinat (latitude, longitude) dan radius toleransi absen. |
| F-11 | Kontrol Sesi Absen | Admin dapat membuka dan menutup jendela waktu absen masuk dan absen pulang secara independen (misalnya membuka sesi masuk 07.00–09.00). |
| F-12 | Approval Izin | Admin dapat melihat daftar pengajuan izin beserta bukti file, lalu menyetujui atau menolak dengan catatan. |
| F-13 | Rekap Absensi | Admin dapat melihat rekap kehadiran seluruh karyawan berdasarkan rentang tanggal, dengan filter per karyawan/status. |
| F-14 | Export CSV | Admin dapat mengunduh rekap absensi dalam format CSV sederhana untuk periode/filter tertentu. |

## 6. Kebutuhan Non-Fungsional

| Kategori | Kebutuhan |
|---|---|
| **Keamanan** | Password disimpan dalam bentuk hash (bukan plain text) di database; data penting (misalnya token sesi/kredensial tersimpan di perangkat) dienkripsi menggunakan mekanisme penyimpanan aman (EncryptedSharedPreferences/DataStore terenkripsi). |
| **Usability** | UI dibangun dengan Jetpack Compose mengikuti prinsip Material Design agar mudah digunakan oleh karyawan non-teknis. |
| **Performa** | Proses pengambilan lokasi dan validasi radius harus responsif (idealnya < 3 detik pada jaringan lokal). |
| **Kompatibilitas** | Minimal mendukung Android versi terbaru yang tersedia di lingkungan pengujian (ditentukan saat setup project). |
| **Maintainability** | Kode disusun dengan pola **MVVM** agar terpisah jelas antara UI (Compose/View), logika presentasi (ViewModel), dan sumber data (Repository/Model). |
| **Reliabilitas Data Lokasi** | Sistem sebaiknya memberi toleransi radius wajar (mis. 100–200 meter) untuk mengakomodasi margin error akurasi GPS smartphone. |

## 7. Spesifikasi Teknis

| Komponen | Teknologi |
|---|---|
| Bahasa Pemrograman Android | Kotlin 100% |
| UI Framework | Jetpack Compose |
| Arsitektur | MVVM (Model – View – ViewModel), dengan Repository sebagai penghubung ke data source |
| Networking | Retrofit + OkHttp untuk konsumsi REST API |
| Database Lokal (opsional) | Room (untuk cache/offline-first, jika diperlukan) |
| Location Service | Fused Location Provider API (Google Play Services) |
| Backend API | REST API (bahasa backend ditentukan sesuai arahan dosen/kebutuhan, dijalankan lokal via Laragon) |
| Database Server | MySQL (via Laragon, lingkungan lokal) |
| Format Ekspor Laporan | CSV sederhana (di-generate di sisi backend, diunduh/dibuka oleh admin) |
| Keamanan Transmisi & Data | Hashing password (misal bcrypt/SHA-256+salt) & enkripsi data penting |

### 7.1 Gambaran Alur Arsitektur MVVM
```
[Compose UI/View] → mengamati state dari → [ViewModel (StateFlow/LiveData)]
        ↑                                            ↓
   User Interaction                          memanggil fungsi ke
                                                       ↓
                                          [Repository]
                                          ↙            ↘
                         [Retrofit → REST API]   [Room → Lokal (opsional)]
                                  ↓
                         [Backend API + MySQL]
```

## 8. Kebutuhan Data (Gambaran Entitas)

| Entitas | Atribut Kunci |
|---|---|
| **users** | id, username, password (hash), role (admin/karyawan), status_aktif |
| **karyawan** | id, user_id, nama, nomor_induk, jabatan, kontak |
| **lokasi_kantor** | id, latitude, longitude, radius_meter |
| **sesi_absen** | id, tipe (masuk/pulang), jam_buka, jam_tutup, status (aktif/nonaktif), tanggal |
| **absensi** | id, karyawan_id, sesi_id, latitude_absen, longitude_absen, jarak_ke_kantor, waktu_absen, status (hadir/telat) |
| **izin** | id, karyawan_id, jenis_izin, tanggal_mulai, tanggal_selesai, keterangan, file_bukti, status_approval, catatan_admin |

*(Skema detail — tipe data, relasi FK, dan normalisasi — akan disusun pada dokumen desain database terpisah.)*

## 9. Gambaran Endpoint API (Level Tinggi)

| Endpoint | Method | Deskripsi |
|---|---|---|
| `/auth/register` | POST | Registrasi mandiri akun karyawan baru (password di-hash sebelum simpan) |
| `/auth/login` | POST | Autentikasi pengguna (admin/karyawan) |
| `/karyawan` | GET/PUT | Lihat daftar & ubah/nonaktifkan data karyawan (admin) |
| `/lokasi-kantor` | GET/PUT | Ambil & atur koordinat kantor (admin) |
| `/sesi-absen` | GET/POST/PUT | Kelola & lihat status sesi absen |
| `/absensi` | POST | Karyawan submit absen (kirim lat/long) |
| `/absensi/rekap` | GET | Admin ambil rekap absensi |
| `/absensi/export-csv` | GET | Generate & unduh CSV rekap |
| `/izin` | GET/POST | Karyawan ajukan izin, admin lihat daftar |
| `/izin/{id}/approval` | PUT | Admin setujui/tolak izin |

*(Detail request/response body akan dirinci pada dokumen API Specification.)*

## 10. Kebutuhan Keamanan (Sesuai Arahan Dosen)

Sesuai batasan mata kuliah, fokus keamanan pada versi ini adalah:

1. **Hashing** — seluruh password pengguna (admin & karyawan) di-hash sebelum disimpan ke database (tidak boleh plain text).
2. **Enkripsi Data Penting** — data sensitif (misalnya token autentikasi/sesi yang tersimpan di perangkat Android) dienkripsi menggunakan penyimpanan aman berbasis Android Keystore, bukan disimpan sebagai teks biasa di SharedPreferences biasa.

Aspek keamanan lanjutan (HTTPS/SSL pinning, deteksi mock location, rate limiting, dsb.) berada di luar cakupan wajib versi ini, namun dapat dicatat sebagai *future enhancement* pada laporan.

## 11. Asumsi & Batasan

- Pengujian dilakukan pada jaringan lokal (localhost/Laragon), bukan server produksi.
- Hanya terdapat satu titik lokasi kantor acuan (tidak mendukung multi-cabang).
- Akun karyawan dibuat secara mandiri melalui form registrasi (self-registration), tidak dibuatkan oleh admin.
- Akurasi absensi bergantung pada kualitas sinyal GPS perangkat pengguna.

## 12. Kriteria Penerimaan (Definition of Done)

- [ ] Karyawan hanya bisa absen ketika berada dalam radius yang ditentukan dan sesi absen sedang dibuka admin.
- [ ] Admin dapat membuka/menutup sesi absen masuk dan pulang secara terpisah.
- [ ] Pengajuan izin tersimpan lengkap dengan file bukti dan dapat di-approve/reject oleh admin.
- [ ] Karyawan baru dapat mendaftar mandiri melalui form registrasi tanpa perlu dibuatkan akun oleh admin.
- [ ] Rekap absensi dapat difilter dan diekspor ke CSV.
- [ ] Password tersimpan dalam bentuk hash di database.
- [ ] Data penting yang tersimpan di perangkat dalam kondisi terenkripsi.
- [ ] Struktur kode mengikuti pola MVVM yang konsisten.

## 13. Rencana Pengembangan (Milestone Sementara)

| Fase | Output |
|---|---|
| 1. Perancangan | PRD (dokumen ini), desain database, desain API, wireframe UI |
| 2. Setup Backend | REST API + koneksi MySQL berjalan lokal |
| 3. Implementasi Modul Karyawan | Registrasi mandiri, login, absen geofencing, riwayat, pengajuan izin |
| 4. Implementasi Modul Admin | Pantau data karyawan, atur lokasi, kontrol sesi, approval izin |
| 5. Rekap & Export CSV | Fitur rekap + generate CSV |
| 6. Keamanan | Hashing password + enkripsi data penting |
| 7. Pengujian & Finalisasi | Uji fungsional end-to-end, penyusunan laporan akhir |

---

*Dokumen ini adalah rancangan awal (living document) dan dapat disesuaikan selama proses pengembangan sesuai temuan teknis di lapangan.*
