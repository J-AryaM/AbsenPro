<?php
// =====================================================================
// Backend_api/absensi/export_csv.php
// Export rekap absensi ke CSV sederhana (PRD F-14). Memakai filter yang
// sama dengan rekap.php lewat rekap_query.php. Output-nya BUKAN JSON,
// melainkan file CSV yang bisa diunduh/dibuka admin.
//
// Kontrak:
//   Method : GET (khusus admin)
//   Query  : sama dengan rekap.php (tanggal_mulai, tanggal_selesai, dst)
//   Auth   : token bisa lewat header Authorization ATAU query ?token=
//            (karena unduhan lewat browser/DownloadManager kadang sulit
//            menyisipkan header) -> lihat penanganannya di bawah.
//   Output : text/csv sebagai attachment (Content-Disposition).
// =====================================================================

require_once __DIR__ . '/../helpers/cors.php';
require_once __DIR__ . '/../helpers/response.php';
require_once __DIR__ . '/../helpers/auth_middleware.php';
require_once __DIR__ . '/../config/db.php';
require_once __DIR__ . '/rekap_query.php';

requireMethod('GET');

$pdo = getConnection();

// Untuk unduhan file, token kadang lebih praktis dikirim via query string.
// Jika ?token= ada tapi header Authorization tidak, kita "pasang" token itu
// ke $_SERVER agar getBearerToken()/requireAdmin tetap berfungsi seragam.
if (empty($_SERVER['HTTP_AUTHORIZATION']) && !empty($_GET['token'])) {
    $_SERVER['HTTP_AUTHORIZATION'] = 'Bearer ' . $_GET['token'];
}

// Tetap wajib admin. Bila token tak valid, requireAdmin membalas JSON error.
requireAdmin($pdo);

// Ambil data memakai builder yang sama seperti rekap JSON.
[$sql, $params] = buildRekapQuery($_GET);
$stmt = $pdo->prepare($sql);
$stmt->execute($params);
$rows = $stmt->fetchAll();

// --- Kirim sebagai file CSV, bukan JSON. ---
// Nama file menyertakan tanggal generate agar unik saat diunduh berkali-kali.
$filename = 'rekap_absensi_' . date('Ymd_His') . '.csv';
header('Content-Type: text/csv; charset=utf-8');
header('Content-Disposition: attachment; filename="' . $filename . '"');

// php://output = menulis langsung ke body respons. fputcsv menangani
// escaping koma/petik otomatis sehingga CSV tetap valid.
$out = fopen('php://output', 'w');

// BOM UTF-8 supaya Excel membaca karakter non-ASCII (nama) dengan benar.
fwrite($out, "\xEF\xBB\xBF");

// Baris header kolom.
fputcsv($out, ['No', 'Nama', 'Tipe', 'Tanggal', 'Jam', 'Status', 'Jarak (m)']);

// Tulis tiap baris data. Nomor urut dibuat manual mulai dari 1.
$no = 1;
foreach ($rows as $r) {
    fputcsv($out, [
        $no++,
        $r['nama'],
        $r['tipe'],
        $r['tanggal'],
        substr($r['waktu_absen'], 11, 5),
        $r['status'],
        round((float) $r['jarak_ke_kantor'], 1),
    ]);
}

fclose($out);
exit; // pastikan tidak ada output lain yang menempel setelah CSV
