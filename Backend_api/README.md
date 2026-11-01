# AbsenPro — Backend API (PHP + MySQL)

REST API untuk aplikasi AbsenPro. Dijalankan lokal via **Laragon** (Apache + MySQL).
Semua respons memakai amplop JSON yang seragam:

```json
{ "success": true,  "message": "...", "data": { ... } }   // sukses
{ "success": false, "message": "...", "data": null }        // error
```

Autentikasi memakai **Bearer token**. Setelah login, kirim token di setiap
request yang butuh sesi:

```
Authorization: Bearer <token>
```

---

## 1. Setup (di laptop yang punya Laragon)

1. Copy folder `Backend_api/` ke document root Laragon, misalnya:
   `C:\laragon\www\absenpro_api\`
   sehingga URL dasarnya menjadi `http://localhost/absenpro_api/`.
2. Buat database & tabel:
   - Buka **phpMyAdmin** (dari menu Laragon) → tab **Import** → pilih
     `Database/schema.sql` → **Go**.
   - Ulangi Import untuk `Database/seed.sql` (mengisi admin + lokasi kantor).
3. (Bila login admin gagal) jalankan `Database/seed_admin.php` sekali lewat
   browser/CLI untuk menulis ulang hash password admin yang pasti valid.
4. Cek API hidup: buka `http://localhost/absenpro_api/` → harus muncul JSON
   `"AbsenPro API aktif."`.

**Akun admin default:** `username = admin`, `password = admin123`.

> Catatan URL untuk Android: emulator mengakses host lewat `http://10.0.2.2/`.
> Jadi `BASE_URL` di aplikasi = `http://10.0.2.2/absenpro_api/`. Bila pakai HP
> fisik satu WiFi, ganti dengan IP laptop, mis. `http://192.168.1.10/absenpro_api/`.

---

## 2. Daftar Endpoint (Kontrak)

Path relatif terhadap `BASE_URL` (mis. `absenpro_api/`).

### Auth

| Endpoint | Method | Auth | Body |
|---|---|---|---|
| `auth/register.php` | POST | — | JSON `{ nama, username, password, jabatan?, kontak? }` |
| `auth/login.php` | POST | — | JSON `{ username, password }` |
| `auth/logout.php` | POST | Bearer | — |

`login` → `data: { token, role, user_id, karyawan_id, nama }`
(`role` = `"admin"` atau `"karyawan"`; `karyawan_id` = `null` untuk admin).

### Karyawan (admin only)

| Endpoint | Method | Body |
|---|---|---|
| `karyawan/index.php` | GET | — → list karyawan |
| `karyawan/index.php` | PUT | JSON `{ id, nama?, jabatan?, kontak?, nomor_induk?, is_active? }` |

Item list: `{ id, user_id, nama, nomor_induk, jabatan, kontak, username, is_active }`.

### Lokasi Kantor

| Endpoint | Method | Auth | Body |
|---|---|---|---|
| `lokasi_kantor/index.php` | GET | Bearer (semua) | — |
| `lokasi_kantor/index.php` | PUT | admin | JSON `{ latitude, longitude, radius_meter, nama_kantor? }` |

Data: `{ id, nama_kantor, latitude, longitude, radius_meter }`.

### Sesi Absen

| Endpoint | Method | Auth | Body |
|---|---|---|---|
| `sesi_absen/index.php` | GET | Bearer | query `?tanggal=YYYY-MM-DD` (opsional, default hari ini) |
| `sesi_absen/index.php` | POST | admin | JSON `{ tipe, tanggal, jam_buka, jam_tutup, status }` |
| `sesi_absen/index.php` | PUT | admin | JSON `{ id, status }` |

`tipe` = `masuk`/`pulang`; `status` = `aktif`/`nonaktif`; jam format `HH:MM`.
Item: `{ id, tipe, tanggal, jam_buka, jam_tutup, status }`.

### Absensi

| Endpoint | Method | Auth | Body/Query |
|---|---|---|---|
| `absensi/index.php` | POST | karyawan | JSON `{ tipe, latitude, longitude }` |
| `absensi/index.php` | GET | karyawan | `?limit=` (opsional) → riwayat pribadi |
| `absensi/rekap.php` | GET | admin | `?tanggal_mulai&tanggal_selesai&karyawan_id&status&tipe` |
| `absensi/export_csv.php` | GET | admin | filter sama; token boleh via `?token=`; output CSV |

POST absen → `data: { id, tipe, jarak_ke_kantor, radius_meter, status, waktu_absen }`.
Validasi radius (Haversine) & jendela waktu **diulang di server**.

### Izin

| Endpoint | Method | Auth | Body |
|---|---|---|---|
| `izin/index.php` | POST | karyawan | multipart: `jenis_izin, tanggal_mulai, tanggal_selesai, keterangan, file_bukti(file)` |
| `izin/index.php` | GET | Bearer | karyawan→miliknya, admin→semua |
| `izin/approval.php` | PUT | admin | JSON `{ id, status, catatan_admin? }` (`status`=`disetujui`/`ditolak`) |

Item izin: `{ id, karyawan_id, nama, jenis_izin, tanggal_mulai, tanggal_selesai, keterangan, file_bukti_url, status_approval, catatan_admin, created_at }`.

---

## 3. Kode Status HTTP

| Kode | Arti di API ini |
|---|---|
| 200 | Sukses (GET/PUT) |
| 201 | Resource dibuat (register, absen, izin, sesi) |
| 401 | Belum login / token tidak valid |
| 403 | Login tapi tidak berhak (mis. bukan admin, di luar radius) |
| 404 | Data tidak ditemukan |
| 409 | Konflik (dobel absen, sesi ditutup, username dipakai) |
| 422 | Input tidak valid |
| 500 | Kesalahan server/DB |
