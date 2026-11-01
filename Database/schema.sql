-- =====================================================================
-- AbsenPro - schema.sql
-- Skema database MySQL untuk aplikasi absensi berbasis geofencing.
-- Dijalankan di lingkungan lokal (Laragon / phpMyAdmin).
--
-- Cara pakai:
--   1. Buat/mport file ini lewat phpMyAdmin atau: mysql -u root < schema.sql
--   2. Lalu jalankan seed.sql untuk mengisi akun admin + lokasi default.
--
-- Catatan desain:
--   - Semua tabel pakai InnoDB agar FOREIGN KEY benar-benar ditegakkan
--     (MyISAM mengabaikan FK). Ini penting supaya data absensi/izin
--     tidak bisa "menggantung" tanpa karyawan pemiliknya.
--   - Charset utf8mb4 supaya nama/keterangan yang mengandung emoji atau
--     karakter non-latin tetap tersimpan utuh.
-- =====================================================================

-- Buat database bila belum ada, lalu pakai database tersebut.
CREATE DATABASE IF NOT EXISTS absenpro
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
USE absenpro;

-- Urutan DROP dibalik dari urutan CREATE: tabel anak (punya FK) dihapus
-- lebih dulu daripada tabel induk, supaya tidak ditolak constraint FK.
DROP TABLE IF EXISTS absensi;
DROP TABLE IF EXISTS izin;
DROP TABLE IF EXISTS sesi_absen;
DROP TABLE IF EXISTS auth_tokens;
DROP TABLE IF EXISTS karyawan;
DROP TABLE IF EXISTS lokasi_kantor;
DROP TABLE IF EXISTS users;

-- ---------------------------------------------------------------------
-- users : akun login untuk dua peran (admin & karyawan).
-- Password DISIMPAN SEBAGAI HASH bcrypt (hasil password_hash() PHP),
-- makanya kolomnya dibuat VARCHAR(255) — bcrypt menghasilkan string 60
-- karakter, tapi 255 memberi ruang bila algoritma hash diganti nanti.
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(100) NOT NULL,
    password      VARCHAR(255) NOT NULL,          -- selalu hash, tidak pernah plain text
    role          ENUM('admin','karyawan') NOT NULL DEFAULT 'karyawan',
    status_aktif  TINYINT(1) NOT NULL DEFAULT 1,  -- 1 = aktif, 0 = dinonaktifkan admin
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- username dijadikan UNIQUE supaya tidak ada dua akun login yang sama;
    -- ini juga jadi acuan saat register mengecek "username sudah dipakai".
    UNIQUE KEY uq_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- karyawan : data diri yang menempel pada satu akun user.
-- Dipisah dari tabel users agar tabel login tetap ramping dan agar
-- profil karyawan bisa berkembang (jabatan, kontak, dll) tanpa
-- mengubah struktur autentikasi.
-- ---------------------------------------------------------------------
CREATE TABLE karyawan (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NOT NULL,
    nama        VARCHAR(150) NOT NULL,
    nomor_induk VARCHAR(50) NULL,                 -- opsional; tidak diisi saat self-register
    jabatan     VARCHAR(100) NULL,
    kontak      VARCHAR(100) NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Satu user hanya boleh punya satu baris karyawan.
    UNIQUE KEY uq_karyawan_user (user_id),
    -- Bila user dihapus, data karyawan ikut terhapus (ON DELETE CASCADE)
    -- karena data karyawan tidak berarti tanpa akunnya.
    CONSTRAINT fk_karyawan_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- auth_tokens : token sesi yang dihasilkan saat login berhasil.
-- Dipakai sebagai pengganti sesi PHP klasik karena klien-nya Android
-- (stateless). Android mengirim token ini di header Authorization pada
-- tiap request yang butuh login. Token disimpan terenkripsi di sisi HP.
-- ---------------------------------------------------------------------
CREATE TABLE auth_tokens (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT NOT NULL,
    token      CHAR(64) NOT NULL,                 -- 64 hex char = hasil bin2hex(random_bytes(32))
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Token wajib unik supaya lookup by-token pasti menunjuk satu user.
    UNIQUE KEY uq_auth_token (token),
    -- Index user_id mempercepat "hapus semua token milik user" saat logout.
    KEY idx_auth_user (user_id),
    CONSTRAINT fk_token_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- lokasi_kantor : titik acuan geofencing (hanya satu baris dipakai).
-- PRD membatasi satu titik kantor (tidak multi-cabang), tapi tabel
-- tetap dibuat bertabel agar admin bisa meng-update koordinat & radius.
-- ---------------------------------------------------------------------
CREATE TABLE lokasi_kantor (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    nama_kantor  VARCHAR(150) NOT NULL DEFAULT 'Kantor Pusat',
    -- DECIMAL(10,7) menyimpan koordinat sampai ~1cm presisi, cukup untuk
    -- geofencing dan menghindari galat pembulatan tipe FLOAT.
    latitude     DECIMAL(10,7) NOT NULL,
    longitude    DECIMAL(10,7) NOT NULL,
    radius_meter INT NOT NULL DEFAULT 150,        -- toleransi radius (PRD sarankan 100-200 m)
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- sesi_absen : jendela waktu absen yang dibuka/ditutup admin.
-- 'tipe' membedakan sesi masuk vs pulang agar keduanya bisa dikontrol
-- independen (PRD F-11). 'tanggal' membuat sesi berlaku per hari.
-- ---------------------------------------------------------------------
CREATE TABLE sesi_absen (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    tipe       ENUM('masuk','pulang') NOT NULL,
    tanggal    DATE NOT NULL,
    jam_buka   TIME NOT NULL,                     -- batas awal jendela absen
    jam_tutup  TIME NOT NULL,                     -- batas akhir jendela absen
    status     ENUM('aktif','nonaktif') NOT NULL DEFAULT 'nonaktif',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Cegah dua sesi dengan tipe sama pada tanggal yang sama; jadi tiap
    -- hari maksimal satu sesi 'masuk' dan satu sesi 'pulang'.
    UNIQUE KEY uq_sesi_tipe_tanggal (tipe, tanggal)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- absensi : catatan satu kali absen (masuk atau pulang) oleh karyawan.
-- Menyimpan koordinat & jarak hasil hitung Haversine di server sebagai
-- bukti bahwa validasi radius benar-benar terjadi di backend.
-- ---------------------------------------------------------------------
CREATE TABLE absensi (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    karyawan_id     INT NOT NULL,
    sesi_id         INT NOT NULL,
    latitude_absen  DECIMAL(10,7) NOT NULL,       -- posisi karyawan saat menekan absen
    longitude_absen DECIMAL(10,7) NOT NULL,
    jarak_ke_kantor DOUBLE NOT NULL,              -- meter, hasil Haversine server-side
    waktu_absen     DATETIME NOT NULL,            -- kapan absen tercatat
    status          ENUM('hadir','telat') NOT NULL DEFAULT 'hadir',
    -- Satu karyawan hanya boleh mengisi satu absensi per sesi (anti dobel absen).
    UNIQUE KEY uq_absensi_karyawan_sesi (karyawan_id, sesi_id),
    KEY idx_absensi_karyawan (karyawan_id),
    KEY idx_absensi_sesi (sesi_id),
    CONSTRAINT fk_absensi_karyawan FOREIGN KEY (karyawan_id)
        REFERENCES karyawan(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_absensi_sesi FOREIGN KEY (sesi_id)
        REFERENCES sesi_absen(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- izin : pengajuan izin/cuti berikut file bukti dan status approval.
-- file_bukti menyimpan NAMA FILE (bukan blob) — file fisiknya ada di
-- folder uploads/izin, sehingga database tetap ringan.
-- ---------------------------------------------------------------------
CREATE TABLE izin (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    karyawan_id     INT NOT NULL,
    jenis_izin      VARCHAR(50) NOT NULL,          -- mis. "Sakit", "Cuti", "Dinas Luar"
    tanggal_mulai   DATE NOT NULL,
    tanggal_selesai DATE NOT NULL,
    keterangan      TEXT NULL,
    file_bukti      VARCHAR(255) NULL,             -- nama file di uploads/izin (boleh kosong)
    status_approval ENUM('menunggu','disetujui','ditolak') NOT NULL DEFAULT 'menunggu',
    catatan_admin   VARCHAR(255) NULL,             -- alasan approve/reject dari admin
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_izin_karyawan (karyawan_id),
    CONSTRAINT fk_izin_karyawan FOREIGN KEY (karyawan_id)
        REFERENCES karyawan(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
