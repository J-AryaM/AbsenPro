<?php
// =====================================================================
// Backend_api/absensi/rekap_query.php
// Pembangun query rekap yang dipakai bersama oleh rekap.php (JSON) dan
// export_csv.php (CSV). Disatukan agar filter yang tampil di layar admin
// identik dengan yang terunduh — mencegah "data di CSV beda dengan di app".
// =====================================================================

/**
 * Susun SQL + parameter untuk rekap absensi berdasarkan filter querystring.
 *
 * @param array $q biasanya $_GET: boleh berisi tanggal_mulai, tanggal_selesai,
 *                 karyawan_id, status, tipe
 * @return array [ string $sql, array $params ] siap dieksekusi prepared stmt
 */
function buildRekapQuery(array $q): array
{
    // Query dasar: gabung absensi + sesi (untuk tipe/tanggal) + karyawan (nama).
    $sql = "SELECT a.id, a.karyawan_id, k.nama, s.tipe, s.tanggal,
                   a.waktu_absen, a.status, a.jarak_ke_kantor
            FROM absensi a
            JOIN sesi_absen s ON s.id = a.sesi_id
            JOIN karyawan k   ON k.id = a.karyawan_id
            WHERE 1=1";   // '1=1' memudahkan menambah 'AND ...' secara kondisional

    $params = [];

    // Filter rentang tanggal (berdasarkan tanggal sesi). Keduanya opsional,
    // tapi umumnya admin mengisi keduanya untuk periode tertentu.
    if (!empty($q['tanggal_mulai']) && preg_match('/^\d{4}-\d{2}-\d{2}$/', $q['tanggal_mulai'])) {
        $sql       .= " AND s.tanggal >= ?";
        $params[]   = $q['tanggal_mulai'];
    }
    if (!empty($q['tanggal_selesai']) && preg_match('/^\d{4}-\d{2}-\d{2}$/', $q['tanggal_selesai'])) {
        $sql       .= " AND s.tanggal <= ?";
        $params[]   = $q['tanggal_selesai'];
    }

    // Filter karyawan tertentu (mis. melihat kehadiran satu orang).
    if (!empty($q['karyawan_id']) && is_numeric($q['karyawan_id'])) {
        $sql       .= " AND a.karyawan_id = ?";
        $params[]   = (int) $q['karyawan_id'];
    }

    // Filter status kehadiran; hanya nilai yang dikenal ENUM yang diterima.
    if (!empty($q['status']) && in_array($q['status'], ['hadir', 'telat'], true)) {
        $sql       .= " AND a.status = ?";
        $params[]   = $q['status'];
    }

    // Filter tipe (masuk/pulang).
    if (!empty($q['tipe']) && in_array($q['tipe'], ['masuk', 'pulang'], true)) {
        $sql       .= " AND s.tipe = ?";
        $params[]   = $q['tipe'];
    }

    // Urutkan terbaru dulu supaya rekap paling relevan tampil di atas.
    $sql .= " ORDER BY s.tanggal DESC, a.waktu_absen DESC";

    return [$sql, $params];
}
