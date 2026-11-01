<?php
// =====================================================================
// Backend_api/auth/register.php
// Registrasi mandiri karyawan (PRD F-00). Membuat SEKALIGUS baris di
// tabel users (untuk login) dan karyawan (data diri), dibungkus dalam
// satu transaksi agar tidak pernah ada user tanpa profil karyawan.
//
// Kontrak:
//   Method : POST
//   Body   : JSON { nama, username, password, jabatan?, kontak? }
//   Sukses : 201 { success:true, data:{ user_id, karyawan_id } }
// =====================================================================

require_once __DIR__ . '/../helpers/cors.php';
require_once __DIR__ . '/../helpers/response.php';
require_once __DIR__ . '/../config/db.php';

// Endpoint ini hanya menerima POST (mengirim data baru).
requireMethod('POST');

// Ambil body JSON. Register tidak butuh token (belum punya akun).
$input = getJsonInput();

// Ambil & rapikan tiap field. trim() membuang spasi tak sengaja.
$nama     = trim($input['nama'] ?? '');
$username = trim($input['username'] ?? '');
$password = $input['password'] ?? '';           // password TIDAK di-trim (spasi bisa disengaja)
$jabatan  = trim($input['jabatan'] ?? '');
$kontak   = trim($input['kontak'] ?? '');

// --- Validasi input dasar dilakukan di server, bukan hanya di Android,
// karena klien bisa saja dimodifikasi/di-bypass; server adalah otoritas. ---
if ($nama === '' || $username === '' || $password === '') {
    sendError('Nama, username, dan password wajib diisi.', 422);
}
// Password minimal 6 karakter sebagai batas kewajaran keamanan.
if (strlen($password) < 6) {
    sendError('Password minimal 6 karakter.', 422);
}

$pdo = getConnection();

// Cek duplikasi username lebih dulu agar bisa memberi pesan yang ramah,
// selain UNIQUE KEY di DB yang jadi pengaman terakhir.
$check = $pdo->prepare("SELECT id FROM users WHERE username = ? LIMIT 1");
$check->execute([$username]);
if ($check->fetch()) {
    sendError('Username sudah dipakai. Silakan pilih yang lain.', 409);
}

// Hash password dengan bcrypt bawaan PHP. Alasan: bila DB bocor, yang
// tersimpan hanya hash yang tidak bisa dikembalikan ke teks asli.
$hash = password_hash($password, PASSWORD_BCRYPT);

try {
    // Transaksi: dua INSERT harus berhasil bersama. Bila INSERT karyawan
    // gagal, INSERT users di-rollback -> tidak ada akun setengah jadi.
    $pdo->beginTransaction();

    // 1) Buat akun login dengan role 'karyawan' (self-register selalu karyawan).
    $insUser = $pdo->prepare(
        "INSERT INTO users (username, password, role, status_aktif)
         VALUES (?, ?, 'karyawan', 1)"
    );
    $insUser->execute([$username, $hash]);
    $userId = (int) $pdo->lastInsertId();

    // 2) Buat profil karyawan yang menempel ke user tadi.
    //    jabatan/kontak boleh kosong -> simpan NULL agar konsisten.
    $insKar = $pdo->prepare(
        "INSERT INTO karyawan (user_id, nama, jabatan, kontak)
         VALUES (?, ?, ?, ?)"
    );
    $insKar->execute([
        $userId,
        $nama,
        $jabatan !== '' ? $jabatan : null,
        $kontak !== '' ? $kontak : null,
    ]);
    $karyawanId = (int) $pdo->lastInsertId();

    $pdo->commit();

    // 201 Created: resource baru berhasil dibuat.
    sendSuccess(
        ['user_id' => $userId, 'karyawan_id' => $karyawanId],
        'Registrasi berhasil. Silakan login.',
        201
    );
} catch (PDOException $e) {
    // Batalkan perubahan bila ada langkah yang gagal di tengah jalan.
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    sendError('Registrasi gagal: ' . $e->getMessage(), 500);
}
