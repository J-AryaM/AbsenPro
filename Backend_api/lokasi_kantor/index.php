<?php
// =====================================================================
// Backend_api/lokasi_kantor/index.php
// Titik acuan geofencing (PRD F-10).
//   GET : semua user login boleh membaca (karyawan butuh koordinat kantor
//         untuk menghitung/menampilkan jarak sebelum absen).
//   PUT : hanya admin yang boleh mengubah koordinat & radius.
//
// Kontrak:
//   GET  -> data: { id, nama_kantor, latitude, longitude, radius_meter }
//   PUT  body JSON { latitude, longitude, radius_meter, nama_kantor? }
// =====================================================================

require_once __DIR__ . '/../helpers/cors.php';
require_once __DIR__ . '/../helpers/response.php';
require_once __DIR__ . '/../helpers/auth_middleware.php';
require_once __DIR__ . '/../config/db.php';

requireMethod(['GET', 'PUT']);

$pdo    = getConnection();
$method = $_SERVER['REQUEST_METHOD'];

// Karena PRD hanya mengizinkan SATU lokasi kantor, kita selalu bekerja
// dengan baris pertama (id terkecil). Helper kecil ini mengambilnya.
$getRow = function () use ($pdo) {
    $stmt = $pdo->query("SELECT id, nama_kantor, latitude, longitude, radius_meter
                         FROM lokasi_kantor ORDER BY id ASC LIMIT 1");
    return $stmt->fetch();
};

if ($method === 'GET') {
    // Semua user login boleh baca -> requireAuth (bukan requireAdmin).
    requireAuth($pdo);

    $row = $getRow();
    if (!$row) {
        sendError('Lokasi kantor belum diatur admin.', 404);
    }
    // Cast eksplisit agar Android menerima tipe angka, bukan string.
    sendSuccess([
        'id'           => (int) $row['id'],
        'nama_kantor'  => $row['nama_kantor'],
        'latitude'     => (float) $row['latitude'],
        'longitude'    => (float) $row['longitude'],
        'radius_meter' => (int) $row['radius_meter'],
    ], 'Data lokasi kantor.');
}

// --- Mulai sini method PUT: ubah lokasi, khusus admin. ---
requireAdmin($pdo);

$input     = getJsonInput();
$latitude  = $input['latitude']  ?? null;
$longitude = $input['longitude'] ?? null;
$radius    = $input['radius_meter'] ?? null;
$nama      = trim($input['nama_kantor'] ?? 'Kantor Pusat');

// Validasi rentang koordinat: lat [-90,90], long [-180,180]. Angka di luar
// itu pasti salah input dan akan merusak perhitungan jarak.
if (!is_numeric($latitude) || $latitude < -90 || $latitude > 90) {
    sendError('Latitude tidak valid (harus antara -90 dan 90).', 422);
}
if (!is_numeric($longitude) || $longitude < -180 || $longitude > 180) {
    sendError('Longitude tidak valid (harus antara -180 dan 180).', 422);
}
// Radius harus positif; nol/negatif membuat tidak ada tempat yang lolos.
if (!is_numeric($radius) || $radius <= 0) {
    sendError('Radius harus lebih dari 0 meter.', 422);
}

$existing = $getRow();
if ($existing) {
    // Sudah ada baris -> update saja (mempertahankan id yang sama).
    $upd = $pdo->prepare(
        "UPDATE lokasi_kantor
         SET nama_kantor = ?, latitude = ?, longitude = ?, radius_meter = ?
         WHERE id = ?"
    );
    $upd->execute([$nama, $latitude, $longitude, (int) $radius, (int) $existing['id']]);
} else {
    // Belum ada (mis. DB baru tanpa seed) -> buat baris pertama.
    $ins = $pdo->prepare(
        "INSERT INTO lokasi_kantor (nama_kantor, latitude, longitude, radius_meter)
         VALUES (?, ?, ?, ?)"
    );
    $ins->execute([$nama, $latitude, $longitude, (int) $radius]);
}

// Kembalikan data terbaru supaya UI admin langsung sinkron tanpa GET ulang.
$row = $getRow();
sendSuccess([
    'id'           => (int) $row['id'],
    'nama_kantor'  => $row['nama_kantor'],
    'latitude'     => (float) $row['latitude'],
    'longitude'    => (float) $row['longitude'],
    'radius_meter' => (int) $row['radius_meter'],
], 'Lokasi kantor diperbarui.');
