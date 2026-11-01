-- =====================================================================
-- AbsenPro - seed.sql
-- Mengisi data awal WAJIB agar aplikasi bisa langsung dipakai:
--   1. Satu akun ADMIN (karyawan mendaftar sendiri, admin TIDAK).
--   2. Satu baris lokasi kantor default sebagai titik acuan geofencing.
--
-- Jalankan SETELAH schema.sql.
--
-- PENTING soal password admin:
--   Kolom password menyimpan HASH bcrypt, bukan teks biasa. Hash di
--   bawah ini dibuat dengan password_hash('admin123', PASSWORD_BCRYPT)
--   pada PHP 8.1 dan sudah diverifikasi dengan password_verify().
--   -> Login admin:  username = admin   password = admin123
--
--   Jika karena versi PHP/DB yang berbeda hash ini tidak cocok, JANGAN
--   menebak-nebak — jalankan saja Database/seed_admin.php lewat browser
--   Laragon; script itu akan menulis ulang hash yang pasti valid.
-- =====================================================================

USE absenpro;

-- Kosongkan dulu supaya seed bisa dijalankan berulang tanpa duplikat.
-- (DELETE, bukan TRUNCATE, agar tidak bentrok dengan FOREIGN KEY.)
DELETE FROM lokasi_kantor;
DELETE FROM users WHERE username = 'admin';

-- ---------------------------------------------------------------------
-- Akun admin tunggal (di-seed manual sesuai PRD: admin tidak register).
-- Sengaja TIDAK membuat baris di tabel karyawan untuk admin, karena
-- admin bukan karyawan yang diabsen — ia hanya pengelola sistem.
-- ---------------------------------------------------------------------
INSERT INTO users (username, password, role, status_aktif) VALUES
('admin', '$2y$10$5onfp.GqPf91Uz5YzUlPieFBZJPlb3iXs9PGLFXpQjr.Mm20s9KWS', 'admin', 1);

-- ---------------------------------------------------------------------
-- Lokasi kantor default. Koordinat contoh menunjuk Monas, Jakarta;
-- admin bisa menggantinya lewat menu "Atur Lokasi Kantor" di aplikasi.
-- radius_meter 150 m = tengah rentang toleransi 100-200 m yang
-- disarankan PRD untuk menoleransi galat akurasi GPS ponsel.
-- ---------------------------------------------------------------------
INSERT INTO lokasi_kantor (nama_kantor, latitude, longitude, radius_meter) VALUES
('Kantor Pusat', -6.1753924, 106.8271528, 150);
