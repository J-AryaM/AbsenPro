<?php
// =====================================================================
// Backend_api/absensi/rekap.php
// Rekap absensi seluruh karyawan untuk admin (PRD F-13), dengan filter
// rentang tanggal, per karyawan, dan per status.
//
// Kontrak:
//   Method : GET (khusus admin)
//   Query  : tanggal_mulai=YYYY-MM-DD, tanggal_selesai=YYYY-MM-DD,
//            karyawan_id (opsional), status ('hadir'|'telat', opsional),
//            tipe ('masuk'|'pulang', opsional)
//   Sukses : data: [ { id, karyawan_id, nama, tipe, tanggal, jam,
//                       status, jarak_ke_kantor } ]
// =====================================================================

require_once __DIR__ . '/../helpers/cors.php';
require_once __DIR__ . '/../helpers/response.php';
require_once __DIR__ . '/../helpers/auth_middleware.php';
require_once __DIR__ . '/../config/db.php';

requireMethod('GET');

$pdo = getConnection();
requireAdmin($pdo);

// Fungsi filter dipisah ke helper agar export_csv.php memakai logika yang
// SAMA PERSIS -> rekap yang dilihat admin == rekap yang diekspor.
require_once __DIR__ . '/rekap_query.php';

// Bangun query + parameter dari querystring lalu jalankan.
[$sql, $params] = buildRekapQuery($_GET);
$stmt = $pdo->prepare($sql);
$stmt->execute($params);

$rows = array_map(function ($r) {
    return [
        'id'              => (int) $r['id'],
        'karyawan_id'     => (int) $r['karyawan_id'],
        'nama'            => $r['nama'],
        'tipe'            => $r['tipe'],
        'tanggal'         => $r['tanggal'],
        'jam'             => substr($r['waktu_absen'], 11, 5),
        'status'          => $r['status'],
        'jarak_ke_kantor' => round((float) $r['jarak_ke_kantor'], 1),
    ];
}, $stmt->fetchAll());

sendSuccess($rows, 'Rekap absensi.');
