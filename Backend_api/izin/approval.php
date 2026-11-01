<?php
// =====================================================================
// Backend_api/izin/approval.php
// Admin menyetujui / menolak pengajuan izin (PRD F-12).
//
// Kontrak:
//   Method : PUT (khusus admin)
//   Body   : JSON { id, status, catatan_admin? }
//            status: 'disetujui' | 'ditolak'
//   Sukses : data: { id, status_approval, catatan_admin }
// =====================================================================

require_once __DIR__ . '/../helpers/cors.php';
require_once __DIR__ . '/../helpers/response.php';
require_once __DIR__ . '/../helpers/auth_middleware.php';
require_once __DIR__ . '/../config/db.php';

requireMethod('PUT');

$pdo = getConnection();
requireAdmin($pdo);

$input   = getJsonInput();
$id      = $input['id'] ?? null;
$status  = $input['status'] ?? '';
$catatan = trim($input['catatan_admin'] ?? '');

if (!is_numeric($id)) {
    sendError('id izin wajib diisi.', 422);
}
// Hanya dua keputusan akhir yang valid dari admin.
if (!in_array($status, ['disetujui', 'ditolak'], true)) {
    sendError("Status harus 'disetujui' atau 'ditolak'.", 422);
}

// Pastikan izin ada sebelum di-update, agar bisa memberi 404 yang jelas.
$find = $pdo->prepare("SELECT id FROM izin WHERE id = ? LIMIT 1");
$find->execute([(int) $id]);
if (!$find->fetch()) {
    sendError('Pengajuan izin tidak ditemukan.', 404);
}

// Perbarui status + catatan admin. Catatan berguna terutama saat menolak,
// agar karyawan tahu alasannya (ditampilkan di layar status izin).
$upd = $pdo->prepare(
    "UPDATE izin SET status_approval = ?, catatan_admin = ? WHERE id = ?"
);
$upd->execute([
    $status,
    $catatan !== '' ? $catatan : null,
    (int) $id,
]);

sendSuccess([
    'id'              => (int) $id,
    'status_approval' => $status,
    'catatan_admin'   => $catatan !== '' ? $catatan : null,
], 'Keputusan izin tersimpan.');
