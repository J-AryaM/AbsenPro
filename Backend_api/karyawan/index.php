<?php
// =====================================================================
// Backend_api/karyawan/index.php
// Kelola data karyawan oleh admin (PRD F-09). Admin TIDAK membuat akun
// (itu lewat self-register); di sini admin hanya melihat, mengubah data,
// dan mengaktifkan/menonaktifkan akun.
//   GET : daftar semua karyawan + status akunnya.
//   PUT : ubah data (nama/jabatan/kontak/nomor_induk) dan/atau is_active.
//
// Kontrak:
//   GET  -> data: [ { id, user_id, nama, nomor_induk, jabatan, kontak,
//                      username, is_active } ]
//   PUT  body { id, nama?, jabatan?, kontak?, nomor_induk?, is_active? }
//        (id = id pada tabel karyawan)
// =====================================================================

require_once __DIR__ . '/../helpers/cors.php';
require_once __DIR__ . '/../helpers/response.php';
require_once __DIR__ . '/../helpers/auth_middleware.php';
require_once __DIR__ . '/../config/db.php';

requireMethod(['GET', 'PUT']);

$pdo = getConnection();

// Seluruh endpoint ini khusus admin (baik lihat maupun ubah data karyawan).
requireAdmin($pdo);

$method = $_SERVER['REQUEST_METHOD'];

if ($method === 'GET') {
    // JOIN ke users untuk menampilkan username & status aktif dalam satu baris.
    // status_aktif ada di users (menyangkut login), bukan di karyawan.
    $stmt = $pdo->query(
        "SELECT k.id, k.user_id, k.nama, k.nomor_induk, k.jabatan, k.kontak,
                u.username, u.status_aktif
         FROM karyawan k
         JOIN users u ON u.id = k.user_id
         ORDER BY k.nama ASC"
    );
    $rows = array_map(function ($r) {
        return [
            'id'          => (int) $r['id'],
            'user_id'     => (int) $r['user_id'],
            'nama'        => $r['nama'],
            'nomor_induk' => $r['nomor_induk'],
            'jabatan'     => $r['jabatan'],
            'kontak'      => $r['kontak'],
            'username'    => $r['username'],
            // Ubah 1/0 dari DB menjadi boolean agar rapi di sisi Android.
            'is_active'   => ((int) $r['status_aktif']) === 1,
        ];
    }, $stmt->fetchAll());

    sendSuccess($rows, 'Daftar karyawan.');
}

// --- Method PUT: perbarui data & status akun karyawan. ---
$input = getJsonInput();
$id    = $input['id'] ?? null;

if (!is_numeric($id)) {
    sendError('id karyawan wajib diisi.', 422);
}
$id = (int) $id;

// Pastikan karyawan ada sekaligus dapatkan user_id-nya (untuk update status).
$find = $pdo->prepare("SELECT user_id FROM karyawan WHERE id = ? LIMIT 1");
$find->execute([$id]);
$kar = $find->fetch();
if (!$kar) {
    sendError('Karyawan tidak ditemukan.', 404);
}
$userId = (int) $kar['user_id'];

// Bangun UPDATE tabel karyawan secara dinamis: hanya field yang dikirim
// yang diubah. Ini membuat PUT bersifat "partial update" — admin bisa
// mengubah satu field tanpa mengosongkan field lain.
$fields = [];
$params = [];
foreach (['nama', 'jabatan', 'kontak', 'nomor_induk'] as $f) {
    if (array_key_exists($f, $input)) {
        $fields[] = "$f = ?";
        $val = trim((string) $input[$f]);
        // Simpan NULL bila dikosongkan (kecuali nama yang wajib ada isinya).
        if ($f === 'nama') {
            if ($val === '') {
                sendError('Nama tidak boleh kosong.', 422);
            }
            $params[] = $val;
        } else {
            $params[] = $val !== '' ? $val : null;
        }
    }
}

if ($fields) {
    $params[] = $id;
    $sql = "UPDATE karyawan SET " . implode(', ', $fields) . " WHERE id = ?";
    $pdo->prepare($sql)->execute($params);
}

// Update status akun (aktif/nonaktif) di tabel users bila is_active dikirim.
// Menonaktifkan akun mencegah karyawan login (dicek di login & middleware).
if (array_key_exists('is_active', $input)) {
    $active = filter_var($input['is_active'], FILTER_VALIDATE_BOOLEAN) ? 1 : 0;
    $updU = $pdo->prepare("UPDATE users SET status_aktif = ? WHERE id = ?");
    $updU->execute([$active, $userId]);

    // Bila dinonaktifkan, hapus token aktifnya agar sesi yang sedang
    // berjalan langsung terputus (tidak menunggu token kedaluwarsa).
    if ($active === 0) {
        $pdo->prepare("DELETE FROM auth_tokens WHERE user_id = ?")->execute([$userId]);
    }
}

// Ambil kembali baris final untuk dikembalikan ke UI admin.
$get = $pdo->prepare(
    "SELECT k.id, k.user_id, k.nama, k.nomor_induk, k.jabatan, k.kontak,
            u.username, u.status_aktif
     FROM karyawan k JOIN users u ON u.id = k.user_id
     WHERE k.id = ?"
);
$get->execute([$id]);
$r = $get->fetch();

sendSuccess([
    'id'          => (int) $r['id'],
    'user_id'     => (int) $r['user_id'],
    'nama'        => $r['nama'],
    'nomor_induk' => $r['nomor_induk'],
    'jabatan'     => $r['jabatan'],
    'kontak'      => $r['kontak'],
    'username'    => $r['username'],
    'is_active'   => ((int) $r['status_aktif']) === 1,
], 'Data karyawan diperbarui.');
