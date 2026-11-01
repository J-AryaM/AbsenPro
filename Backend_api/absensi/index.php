<?php
// =====================================================================
// Backend_api/absensi/index.php
// Inti fitur geofencing (PRD F-02/F-03/F-04).
//   POST : karyawan submit absen. Server memvalidasi ULANG jarak & sesi.
//   GET  : karyawan melihat riwayat absensinya sendiri.
//
// Kontrak:
//   POST body { tipe, latitude, longitude }   (tipe: 'masuk'|'pulang')
//        -> data: { id, tipe, jarak_ke_kantor, status, waktu_absen, radius_meter }
//   GET  ?limit=  -> data: [ { id, tipe, tanggal, jam, status, jarak_ke_kantor } ]
// =====================================================================

require_once __DIR__ . '/../helpers/cors.php';
require_once __DIR__ . '/../helpers/response.php';
require_once __DIR__ . '/../helpers/auth_middleware.php';
require_once __DIR__ . '/../helpers/geo.php';
require_once __DIR__ . '/../config/db.php';

requireMethod(['GET', 'POST']);

$pdo    = getConnection();
$method = $_SERVER['REQUEST_METHOD'];

// Absensi selalu milik karyawan yang sedang login.
$user       = requireAuth($pdo);
$karyawanId = getKaryawanId($pdo, (int) $user['id']);

// ---------------------------------------------------------------------
// GET: riwayat absensi pribadi. Join ke sesi untuk mendapat tipe & tanggal.
// ---------------------------------------------------------------------
if ($method === 'GET') {
    // Batasi jumlah baris agar payload tidak membengkak; default 60 terakhir.
    $limit = isset($_GET['limit']) && is_numeric($_GET['limit']) ? (int) $_GET['limit'] : 60;
    if ($limit <= 0 || $limit > 500) {
        $limit = 60;
    }

    // LIMIT tidak bisa di-bind sebagai parameter di sebagian versi MySQL,
    // maka nilainya sudah dipastikan integer di atas -> aman disisipkan.
    $stmt = $pdo->prepare(
        "SELECT a.id, s.tipe, s.tanggal, a.waktu_absen, a.status, a.jarak_ke_kantor
         FROM absensi a
         JOIN sesi_absen s ON s.id = a.sesi_id
         WHERE a.karyawan_id = ?
         ORDER BY a.waktu_absen DESC
         LIMIT $limit"
    );
    $stmt->execute([$karyawanId]);

    $rows = array_map(function ($r) {
        return [
            'id'              => (int) $r['id'],
            'tipe'            => $r['tipe'],
            'tanggal'         => $r['tanggal'],
            // Pisahkan jam dari datetime agar UI gampang menampilkannya.
            'jam'             => substr($r['waktu_absen'], 11, 5), // 'HH:MM'
            'status'          => $r['status'],
            'jarak_ke_kantor' => round((float) $r['jarak_ke_kantor'], 1),
        ];
    }, $stmt->fetchAll());

    sendSuccess($rows, 'Riwayat absensi.');
}

// ---------------------------------------------------------------------
// POST: submit absen. Ini bagian paling kritikal keamanannya.
// ---------------------------------------------------------------------
$input     = getJsonInput();
$tipe      = $input['tipe'] ?? '';
$latitude  = $input['latitude'] ?? null;
$longitude = $input['longitude'] ?? null;

if (!in_array($tipe, ['masuk', 'pulang'], true)) {
    sendError("Tipe absen harus 'masuk' atau 'pulang'.", 422);
}
if (!is_numeric($latitude) || !is_numeric($longitude)) {
    sendError('Koordinat lokasi tidak valid.', 422);
}
$latitude  = (float) $latitude;
$longitude = (float) $longitude;

// 1) Ambil lokasi kantor sebagai titik acuan. Tanpa ini tidak ada
//    pembanding jarak, jadi absen ditolak bila admin belum mengaturnya.
$lok = $pdo->query("SELECT latitude, longitude, radius_meter
                    FROM lokasi_kantor ORDER BY id ASC LIMIT 1")->fetch();
if (!$lok) {
    sendError('Lokasi kantor belum diatur admin.', 409);
}
$radius = (int) $lok['radius_meter'];

// 2) Ambil sesi hari ini untuk tipe tsb yang berstatus 'aktif'.
//    Absen hanya boleh saat admin membuka sesinya (PRD).
$today = date('Y-m-d');
$sesiStmt = $pdo->prepare(
    "SELECT id, jam_buka, jam_tutup, status
     FROM sesi_absen
     WHERE tipe = ? AND tanggal = ?
     LIMIT 1"
);
$sesiStmt->execute([$tipe, $today]);
$sesi = $sesiStmt->fetch();

if (!$sesi) {
    sendError("Belum ada sesi absen $tipe untuk hari ini.", 409);
}
if ($sesi['status'] !== 'aktif') {
    sendError("Sesi absen $tipe sedang ditutup admin.", 409);
}

// 3) Cek jendela waktu: jam sekarang harus di antara jam_buka & jam_tutup.
//    Diperiksa di server supaya tidak bisa diakali dengan mengubah jam HP.
$now = date('H:i:s');
if ($now < $sesi['jam_buka'] || $now > $sesi['jam_tutup']) {
    sendError(
        'Di luar jam absen (' . substr($sesi['jam_buka'], 0, 5) .
            ' - ' . substr($sesi['jam_tutup'], 0, 5) . ').',
        409
    );
}

// 4) VALIDASI RADIUS DI SERVER. Meski Android sudah menghitung jarak, cek
//    diulang di sini karena klien tidak bisa dipercaya (bisa dimodifikasi
//    atau mengirim koordinat palsu). Server adalah otoritas final.
$jarak = haversineMeters($latitude, $longitude, (float) $lok['latitude'], (float) $lok['longitude']);
if ($jarak > $radius) {
    sendError(
        'Anda di luar radius kantor (' . round($jarak) . ' m dari ' . $radius . ' m).',
        403
    );
}

// 5) Tentukan status hadir/telat: telat bila absen 'masuk' dilakukan
//    setelah jam_buka. Untuk 'pulang' selalu 'hadir' (tidak ada konsep telat pulang).
$status = 'hadir';
if ($tipe === 'masuk' && $now > $sesi['jam_buka']) {
    $status = 'telat';
}

// 6) Simpan. UNIQUE(karyawan_id, sesi_id) mencegah dobel absen; bila
//    terjadi, PDO melempar error yang kita terjemahkan jadi pesan ramah.
try {
    $ins = $pdo->prepare(
        "INSERT INTO absensi
            (karyawan_id, sesi_id, latitude_absen, longitude_absen, jarak_ke_kantor, waktu_absen, status)
         VALUES (?, ?, ?, ?, ?, NOW(), ?)"
    );
    $ins->execute([
        $karyawanId,
        (int) $sesi['id'],
        $latitude,
        $longitude,
        $jarak,
        $status,
    ]);
} catch (PDOException $e) {
    // Kode 23000 = pelanggaran integritas (di sini: UNIQUE dobel absen).
    if ($e->getCode() === '23000') {
        sendError("Anda sudah absen $tipe untuk sesi ini.", 409);
    }
    sendError('Gagal menyimpan absen: ' . $e->getMessage(), 500);
}

// Balikan ringkasan hasil agar UI bisa menampilkan konfirmasi lengkap.
sendSuccess([
    'id'              => (int) $pdo->lastInsertId(),
    'tipe'            => $tipe,
    'jarak_ke_kantor' => round($jarak, 1),
    'radius_meter'    => $radius,
    'status'          => $status,
    'waktu_absen'     => date('Y-m-d H:i:s'),
], "Absen $tipe berhasil ($status).", 201);
