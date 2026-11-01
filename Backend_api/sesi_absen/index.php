<?php
// =====================================================================
// Backend_api/sesi_absen/index.php
// Kelola jendela waktu absen (PRD F-11). Sesi 'masuk' dan 'pulang'
// dikontrol independen, per tanggal.
//   GET  : lihat sesi (default hari ini). Semua user login boleh.
//   POST : admin membuat/menimpa sesi (tipe+tanggal+jam+status).
//   PUT  : admin mengubah status aktif/nonaktif sebuah sesi by id.
//
// Kontrak:
//   GET  ?tanggal=YYYY-MM-DD (opsional) -> data: [ {sesi...}, ... ]
//   POST body { tipe, tanggal, jam_buka, jam_tutup, status }
//   PUT  body { id, status }  (status: 'aktif'|'nonaktif')
// =====================================================================

require_once __DIR__ . '/../helpers/cors.php';
require_once __DIR__ . '/../helpers/response.php';
require_once __DIR__ . '/../helpers/auth_middleware.php';
require_once __DIR__ . '/../config/db.php';

requireMethod(['GET', 'POST', 'PUT']);

$pdo    = getConnection();
$method = $_SERVER['REQUEST_METHOD'];

// Bentuk baris sesi menjadi array dengan tipe yang rapi untuk JSON.
$mapSesi = function (array $r): array {
    return [
        'id'        => (int) $r['id'],
        'tipe'      => $r['tipe'],          // 'masuk' | 'pulang'
        'tanggal'   => $r['tanggal'],       // 'YYYY-MM-DD'
        'jam_buka'  => substr($r['jam_buka'], 0, 5),   // potong detik -> 'HH:MM'
        'jam_tutup' => substr($r['jam_tutup'], 0, 5),
        'status'    => $r['status'],        // 'aktif' | 'nonaktif'
    ];
};

if ($method === 'GET') {
    // Karyawan butuh tahu sesi apa yang sedang aktif -> requireAuth cukup.
    requireAuth($pdo);

    // Default melihat sesi hari ini bila ?tanggal tidak diberikan.
    $tanggal = $_GET['tanggal'] ?? date('Y-m-d');

    $stmt = $pdo->prepare(
        "SELECT id, tipe, tanggal, jam_buka, jam_tutup, status
         FROM sesi_absen WHERE tanggal = ? ORDER BY tipe ASC"
    );
    $stmt->execute([$tanggal]);
    $rows = array_map($mapSesi, $stmt->fetchAll());

    sendSuccess($rows, 'Daftar sesi absen.');
}

if ($method === 'POST') {
    // Membuat/menyetel sesi adalah wewenang admin.
    requireAdmin($pdo);

    $input     = getJsonInput();
    $tipe      = $input['tipe'] ?? '';
    $tanggal   = $input['tanggal'] ?? '';
    $jamBuka   = $input['jam_buka'] ?? '';
    $jamTutup  = $input['jam_tutup'] ?? '';
    $status    = $input['status'] ?? 'aktif';

    // tipe hanya boleh dua nilai yang dikenal DB (ENUM).
    if (!in_array($tipe, ['masuk', 'pulang'], true)) {
        sendError("Tipe sesi harus 'masuk' atau 'pulang'.", 422);
    }
    if (!in_array($status, ['aktif', 'nonaktif'], true)) {
        sendError("Status harus 'aktif' atau 'nonaktif'.", 422);
    }
    // Validasi format tanggal & jam supaya tidak menyimpan data sampah.
    if (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $tanggal)) {
        sendError('Format tanggal harus YYYY-MM-DD.', 422);
    }
    if (!preg_match('/^\d{2}:\d{2}$/', $jamBuka) || !preg_match('/^\d{2}:\d{2}$/', $jamTutup)) {
        sendError('Format jam harus HH:MM.', 422);
    }
    // Jendela waktu harus maju: jam tutup lebih besar dari jam buka.
    if ($jamTutup <= $jamBuka) {
        sendError('Jam tutup harus lebih besar dari jam buka.', 422);
    }

    // UNIQUE(tipe, tanggal) di DB berarti sesi bisa "upsert": kalau sudah
    // ada sesi tipe+tanggal itu, kita perbarui; kalau belum, kita buat.
    $find = $pdo->prepare("SELECT id FROM sesi_absen WHERE tipe = ? AND tanggal = ? LIMIT 1");
    $find->execute([$tipe, $tanggal]);
    $exist = $find->fetch();

    if ($exist) {
        $upd = $pdo->prepare(
            "UPDATE sesi_absen SET jam_buka = ?, jam_tutup = ?, status = ? WHERE id = ?"
        );
        $upd->execute([$jamBuka, $jamTutup, $status, (int) $exist['id']]);
        $id = (int) $exist['id'];
    } else {
        $ins = $pdo->prepare(
            "INSERT INTO sesi_absen (tipe, tanggal, jam_buka, jam_tutup, status)
             VALUES (?, ?, ?, ?, ?)"
        );
        $ins->execute([$tipe, $tanggal, $jamBuka, $jamTutup, $status]);
        $id = (int) $pdo->lastInsertId();
    }

    // Balikan baris final agar UI admin sinkron.
    $get = $pdo->prepare("SELECT id, tipe, tanggal, jam_buka, jam_tutup, status
                          FROM sesi_absen WHERE id = ?");
    $get->execute([$id]);
    sendSuccess($mapSesi($get->fetch()), 'Sesi absen disimpan.', 201);
}

// --- Method PUT: ubah status (buka/tutup) sesi tertentu, khusus admin. ---
requireAdmin($pdo);

$input  = getJsonInput();
$id     = $input['id'] ?? null;
$status = $input['status'] ?? '';

if (!is_numeric($id)) {
    sendError('id sesi wajib diisi.', 422);
}
if (!in_array($status, ['aktif', 'nonaktif'], true)) {
    sendError("Status harus 'aktif' atau 'nonaktif'.", 422);
}

$upd = $pdo->prepare("UPDATE sesi_absen SET status = ? WHERE id = ?");
$upd->execute([$status, (int) $id]);

// rowCount 0 berarti id tidak ada -> beri tahu klien.
if ($upd->rowCount() === 0) {
    // Bisa juga karena status sama dengan sebelumnya; cek keberadaannya.
    $chk = $pdo->prepare("SELECT id FROM sesi_absen WHERE id = ?");
    $chk->execute([(int) $id]);
    if (!$chk->fetch()) {
        sendError('Sesi tidak ditemukan.', 404);
    }
}

sendSuccess(['id' => (int) $id, 'status' => $status], 'Status sesi diperbarui.');
