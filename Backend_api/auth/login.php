<?php
// =====================================================================
// Backend_api/auth/login.php
// Autentikasi admin & karyawan (PRD F-01/F-08). Verifikasi password
// terhadap hash di DB, lalu terbitkan token acak yang disimpan di
// auth_tokens. Token inilah yang dipakai Android untuk request berikutnya.
//
// Kontrak:
//   Method : POST
//   Body   : JSON { username, password }
//   Sukses : 200 { success:true, data:{ token, role, user_id, karyawan_id, nama } }
// =====================================================================

require_once __DIR__ . '/../helpers/cors.php';
require_once __DIR__ . '/../helpers/response.php';
require_once __DIR__ . '/../config/db.php';

requireMethod('POST');

$input    = getJsonInput();
$username = trim($input['username'] ?? '');
$password = $input['password'] ?? '';

if ($username === '' || $password === '') {
    sendError('Username dan password wajib diisi.', 422);
}

$pdo = getConnection();

// Ambil user berdasarkan username. Sengaja pesan error gagal-login dibuat
// sama untuk "user tak ada" maupun "password salah" agar tidak membocorkan
// username mana yang terdaftar (praktik keamanan sederhana).
$stmt = $pdo->prepare(
    "SELECT id, username, password, role, status_aktif
     FROM users WHERE username = ? LIMIT 1"
);
$stmt->execute([$username]);
$user = $stmt->fetch();

// password_verify membandingkan password teks dengan hash bcrypt tersimpan.
// Ini kebalikan dari password_hash; kita tidak pernah men-decrypt hash.
if (!$user || !password_verify($password, $user['password'])) {
    sendError('Username atau password salah.', 401);
}

// Akun dinonaktifkan admin tidak boleh login walau kredensial benar.
if ((int) $user['status_aktif'] !== 1) {
    sendError('Akun Anda dinonaktifkan. Hubungi admin.', 403);
}

// Buat token acak 32 byte -> 64 karakter hex. random_bytes() bersifat
// cryptographically secure sehingga token sulit ditebak.
$token = bin2hex(random_bytes(32));

// Simpan token. Endpoint berikutnya memvalidasi request dengan mencari
// token ini di auth_tokens (lihat auth_middleware.php).
$insToken = $pdo->prepare("INSERT INTO auth_tokens (user_id, token) VALUES (?, ?)");
$insToken->execute([(int) $user['id'], $token]);

// Untuk karyawan, sertakan karyawan_id & nama agar Android tidak perlu
// memanggil endpoint tambahan hanya untuk tahu identitas dirinya.
$karyawanId = null;
$nama       = $user['username']; // fallback bila admin (tak punya baris karyawan)
if ($user['role'] === 'karyawan') {
    $k = $pdo->prepare("SELECT id, nama FROM karyawan WHERE user_id = ? LIMIT 1");
    $k->execute([(int) $user['id']]);
    if ($row = $k->fetch()) {
        $karyawanId = (int) $row['id'];
        $nama       = $row['nama'];
    }
}

// Kirim token + identitas ringkas. Android menyimpan token ini terenkripsi.
sendSuccess([
    'token'       => $token,
    'role'        => $user['role'],       // 'admin' | 'karyawan' -> menentukan navigasi di app
    'user_id'     => (int) $user['id'],
    'karyawan_id' => $karyawanId,         // null untuk admin
    'nama'        => $nama,
], 'Login berhasil.');
