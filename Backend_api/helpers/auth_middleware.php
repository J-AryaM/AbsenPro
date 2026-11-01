<?php
// =====================================================================
// Backend_api/helpers/auth_middleware.php
// "Penjaga pintu" untuk endpoint yang butuh login. Membaca token dari
// header Authorization, mencocokkannya ke tabel auth_tokens, lalu
// mengembalikan data user pemilik token. Endpoint memakainya untuk:
//   - memastikan hanya user login yang bisa akses (authentication)
//   - membatasi akses admin-only (authorization by role)
//
// Butuh response.php sudah ter-include lebih dulu (untuk sendError()).
// =====================================================================

/**
 * Ambil string token mentah dari header Authorization.
 * Format yang diharapkan dari Android:  "Authorization: Bearer <token>".
 *
 * @return string|null token, atau null bila header tidak ada
 */
function getBearerToken(): ?string
{
    // getallheaders() paling mudah, tapi tidak selalu tersedia di semua
    // konfigurasi server; sediakan fallback ke variabel $_SERVER.
    $headers = null;
    if (function_exists('getallheaders')) {
        $headers = getallheaders();
    }

    $authHeader = null;
    if ($headers) {
        // Nama header bisa beda kapitalisasi antar-server, cek keduanya.
        $authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? null;
    }
    // Fallback: sebagian server menaruh header di $_SERVER dengan prefiks.
    if ($authHeader === null) {
        $authHeader = $_SERVER['HTTP_AUTHORIZATION']
            ?? $_SERVER['REDIRECT_HTTP_AUTHORIZATION']
            ?? null;
    }

    if (!$authHeader) {
        return null;
    }

    // Buang prefiks "Bearer " (case-insensitive) untuk mengambil token murni.
    if (preg_match('/Bearer\s+(.+)/i', $authHeader, $m)) {
        return trim($m[1]);
    }
    // Bila klien mengirim token tanpa prefiks, terima apa adanya.
    return trim($authHeader);
}

/**
 * Wajibkan request membawa token valid. Bila tidak, langsung balas 401
 * dan hentikan skrip (fungsi tidak akan "return" dalam kasus gagal).
 *
 * @param PDO $pdo koneksi database aktif
 * @return array data user: [id, username, role, status_aktif]
 */
function requireAuth(PDO $pdo): array
{
    $token = getBearerToken();
    if (!$token) {
        sendError('Token tidak ditemukan. Silakan login ulang.', 401);
    }

    // Join token -> user agar sekaligus mendapat role & status akun.
    // Prepared statement mencegah injeksi lewat nilai token.
    $stmt = $pdo->prepare(
        "SELECT u.id, u.username, u.role, u.status_aktif
         FROM auth_tokens t
         JOIN users u ON u.id = t.user_id
         WHERE t.token = ?
         LIMIT 1"
    );
    $stmt->execute([$token]);
    $user = $stmt->fetch();

    // Token tidak dikenal -> anggap sesi tidak valid.
    if (!$user) {
        sendError('Sesi tidak valid atau sudah kedaluwarsa. Login ulang.', 401);
    }

    // Akun yang dinonaktifkan admin tidak boleh dipakai walau token masih ada.
    if ((int) $user['status_aktif'] !== 1) {
        sendError('Akun Anda dinonaktifkan. Hubungi admin.', 403);
    }

    return $user;
}

/**
 * Sama seperti requireAuth, tapi tambahan: user WAJIB berperan admin.
 * Dipakai endpoint khusus admin (kelola karyawan, rekap, dll).
 *
 * @param PDO $pdo koneksi database aktif
 * @return array data user admin
 */
function requireAdmin(PDO $pdo): array
{
    $user = requireAuth($pdo);
    if ($user['role'] !== 'admin') {
        // 403 Forbidden: sudah login, tapi tidak berhak mengakses resource ini.
        sendError('Akses khusus admin.', 403);
    }
    return $user;
}

/**
 * Ambil id baris karyawan milik sebuah user. Dibutuhkan karena tabel
 * absensi/izin memakai karyawan_id, bukan user_id. Dipisah jadi helper
 * supaya endpoint karyawan tidak menulis ulang query yang sama.
 *
 * @param PDO $pdo    koneksi database
 * @param int $userId id pada tabel users
 * @return int id pada tabel karyawan
 */
function getKaryawanId(PDO $pdo, int $userId): int
{
    $stmt = $pdo->prepare("SELECT id FROM karyawan WHERE user_id = ? LIMIT 1");
    $stmt->execute([$userId]);
    $row = $stmt->fetch();
    if (!$row) {
        // Terjadi bila akun admin (yang tak punya baris karyawan) mencoba
        // mengakses fitur khusus karyawan -> tolak dengan pesan jelas.
        sendError('Data karyawan tidak ditemukan untuk akun ini.', 404);
    }
    return (int) $row['id'];
}
