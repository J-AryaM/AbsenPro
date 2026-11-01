<?php
// =====================================================================
// Backend_api/izin/index.php
// Pengajuan & daftar izin (PRD F-05/F-06/F-12).
//   POST : karyawan mengajukan izin + upload file bukti (multipart).
//   GET  : - karyawan  -> daftar izin miliknya sendiri.
//          - admin     -> daftar SEMUA izin (untuk approval).
//
// Kontrak:
//   POST  multipart/form-data:
//         fields: jenis_izin, tanggal_mulai, tanggal_selesai, keterangan
//         file  : file_bukti (opsional; gambar atau PDF)
//   GET   -> data: [ { id, karyawan_id, nama, jenis_izin, tanggal_mulai,
//                      tanggal_selesai, keterangan, file_bukti_url,
//                      status_approval, catatan_admin, created_at } ]
// =====================================================================

require_once __DIR__ . '/../helpers/cors.php';
require_once __DIR__ . '/../helpers/response.php';
require_once __DIR__ . '/../helpers/auth_middleware.php';
require_once __DIR__ . '/../config/db.php';

requireMethod(['GET', 'POST']);

$pdo  = getConnection();
$user = requireAuth($pdo);

// Base URL folder uploads agar klien bisa membentuk URL lengkap file bukti.
// Dihitung dari lokasi request supaya ikut host/port yang dipakai.
$scheme   = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') ? 'https' : 'http';
$host     = $_SERVER['HTTP_HOST'] ?? 'localhost';
// SCRIPT_NAME contohnya '/absenpro_api/izin/index.php'. Kita butuh path
// dasar API '/absenpro_api'. Sengaja TIDAK memakai dirname() karena di
// Windows dirname() mengembalikan pemisah backslash '\' yang merusak URL;
// gantinya kita normalkan ke '/' lalu buang sufiks '/izin/index.php'.
$scriptPath = str_replace('\\', '/', $_SERVER['SCRIPT_NAME'] ?? '');
$baseDir    = preg_replace('#/izin/index\.php$#', '', $scriptPath); // -> '/absenpro_api' atau ''
$uploadUrlBase = $scheme . '://' . $host . $baseDir . '/uploads/izin/';

// ---------------------------------------------------------------------
// GET: daftar izin. Admin lihat semua, karyawan hanya miliknya.
// ---------------------------------------------------------------------
if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    if ($user['role'] === 'admin') {
        // Admin: tanpa filter karyawan -> semua pengajuan.
        $stmt = $pdo->query(
            "SELECT i.*, k.nama
             FROM izin i JOIN karyawan k ON k.id = i.karyawan_id
             ORDER BY i.created_at DESC"
        );
    } else {
        // Karyawan: batasi ke izin miliknya sendiri (privasi).
        $karyawanId = getKaryawanId($pdo, (int) $user['id']);
        $stmt = $pdo->prepare(
            "SELECT i.*, k.nama
             FROM izin i JOIN karyawan k ON k.id = i.karyawan_id
             WHERE i.karyawan_id = ?
             ORDER BY i.created_at DESC"
        );
        $stmt->execute([$karyawanId]);
    }

    $rows = array_map(function ($r) use ($uploadUrlBase) {
        return [
            'id'              => (int) $r['id'],
            'karyawan_id'     => (int) $r['karyawan_id'],
            'nama'            => $r['nama'],
            'jenis_izin'      => $r['jenis_izin'],
            'tanggal_mulai'   => $r['tanggal_mulai'],
            'tanggal_selesai' => $r['tanggal_selesai'],
            'keterangan'      => $r['keterangan'],
            // Kembalikan URL siap-pakai; null bila tidak ada file bukti.
            'file_bukti_url'  => $r['file_bukti'] ? $uploadUrlBase . $r['file_bukti'] : null,
            'status_approval' => $r['status_approval'],
            'catatan_admin'   => $r['catatan_admin'],
            'created_at'      => $r['created_at'],
        ];
    }, $stmt->fetchAll());

    sendSuccess($rows, 'Daftar izin.');
}

// ---------------------------------------------------------------------
// POST: pengajuan izin baru. Hanya karyawan (admin tak mengajukan izin).
// Data dikirim sebagai multipart karena ada kemungkinan file terlampir,
// jadi field dibaca dari $_POST (bukan JSON body).
// ---------------------------------------------------------------------
if ($user['role'] !== 'karyawan') {
    sendError('Hanya karyawan yang dapat mengajukan izin.', 403);
}
$karyawanId = getKaryawanId($pdo, (int) $user['id']);

$jenis     = trim($_POST['jenis_izin'] ?? '');
$tglMulai  = trim($_POST['tanggal_mulai'] ?? '');
$tglSelesai = trim($_POST['tanggal_selesai'] ?? '');
$keterangan = trim($_POST['keterangan'] ?? '');

// Validasi field wajib.
if ($jenis === '' || $tglMulai === '' || $tglSelesai === '') {
    sendError('Jenis izin, tanggal mulai, dan tanggal selesai wajib diisi.', 422);
}
if (
    !preg_match('/^\d{4}-\d{2}-\d{2}$/', $tglMulai) ||
    !preg_match('/^\d{4}-\d{2}-\d{2}$/', $tglSelesai)
) {
    sendError('Format tanggal harus YYYY-MM-DD.', 422);
}
// Rentang tanggal harus logis: selesai tidak boleh sebelum mulai.
if ($tglSelesai < $tglMulai) {
    sendError('Tanggal selesai tidak boleh sebelum tanggal mulai.', 422);
}

// --- Tangani file bukti bila ada (opsional). ---
$savedFileName = null;
if (isset($_FILES['file_bukti']) && $_FILES['file_bukti']['error'] !== UPLOAD_ERR_NO_FILE) {
    $file = $_FILES['file_bukti'];

    // Pastikan tidak ada error saat upload (mis. ukuran melebihi batas server).
    if ($file['error'] !== UPLOAD_ERR_OK) {
        sendError('Gagal mengunggah file (kode: ' . $file['error'] . ').', 422);
    }

    // Batasi ukuran maksimum 5 MB agar penyimpanan tidak membengkak.
    $maxBytes = 5 * 1024 * 1024;
    if ($file['size'] > $maxBytes) {
        sendError('Ukuran file maksimal 5 MB.', 422);
    }

    // Batasi tipe file ke gambar & PDF saja (bukti izin). Deteksi MIME dari
    // isi file (finfo), bukan sekadar ekstensi, agar tidak mudah dipalsukan.
    $finfo = finfo_open(FILEINFO_MIME_TYPE);
    $mime  = finfo_file($finfo, $file['tmp_name']);
    finfo_close($finfo);

    $allowed = [
        'image/jpeg' => 'jpg',
        'image/png'  => 'png',
        'application/pdf' => 'pdf',
    ];
    if (!isset($allowed[$mime])) {
        sendError('File harus berupa gambar (JPG/PNG) atau PDF.', 422);
    }

    // Siapkan folder tujuan; buat bila belum ada.
    $targetDir = __DIR__ . '/../uploads/izin/';
    if (!is_dir($targetDir)) {
        mkdir($targetDir, 0775, true);
    }

    // Nama file dibuat unik (karyawan + waktu + acak) agar tidak saling
    // menimpa dan tidak bisa ditebak. Ekstensi diambil dari MIME tepercaya.
    $ext = $allowed[$mime];
    $savedFileName = 'izin_' . $karyawanId . '_' . time() . '_' . bin2hex(random_bytes(4)) . '.' . $ext;

    // Pindahkan dari lokasi sementara ke folder uploads permanen.
    if (!move_uploaded_file($file['tmp_name'], $targetDir . $savedFileName)) {
        sendError('Gagal menyimpan file bukti di server.', 500);
    }
}

// Simpan pengajuan. status_approval default 'menunggu' (dari skema).
$ins = $pdo->prepare(
    "INSERT INTO izin
        (karyawan_id, jenis_izin, tanggal_mulai, tanggal_selesai, keterangan, file_bukti)
     VALUES (?, ?, ?, ?, ?, ?)"
);
$ins->execute([
    $karyawanId,
    $jenis,
    $tglMulai,
    $tglSelesai,
    $keterangan !== '' ? $keterangan : null,
    $savedFileName, // null bila tanpa lampiran
]);

sendSuccess([
    'id'             => (int) $pdo->lastInsertId(),
    'file_bukti_url' => $savedFileName ? $uploadUrlBase . $savedFileName : null,
    'status_approval' => 'menunggu',
], 'Pengajuan izin terkirim.', 201);
