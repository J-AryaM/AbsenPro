<?php
// =====================================================================
// Backend_api/auth/logout.php
// Mengakhiri sesi dengan menghapus token yang sedang dipakai dari
// auth_tokens. Setelah ini, token yang sama tidak lagi valid di
// requireAuth(). Android memanggilnya lalu menghapus token lokalnya.
//
// Kontrak:
//   Method : POST
//   Header : Authorization: Bearer <token>
//   Sukses : 200 { success:true, data:null }
// =====================================================================

require_once __DIR__ . '/../helpers/cors.php';
require_once __DIR__ . '/../helpers/response.php';
require_once __DIR__ . '/../helpers/auth_middleware.php';
require_once __DIR__ . '/../config/db.php';

requireMethod('POST');

$pdo = getConnection();

// Validasi token dulu (memastikan yang logout memang sesi valid),
// lalu hapus baris token tersebut.
requireAuth($pdo);
$token = getBearerToken();

$del = $pdo->prepare("DELETE FROM auth_tokens WHERE token = ?");
$del->execute([$token]);

sendSuccess(null, 'Logout berhasil.');
