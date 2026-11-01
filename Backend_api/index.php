<?php
// =====================================================================
// Backend_api/index.php
// Halaman "health check" sederhana. Berguna untuk memastikan API sudah
// terpasang benar di Laragon: buka http://localhost/absenpro_api/ di
// browser dan pastikan muncul JSON status di bawah.
// =====================================================================

require_once __DIR__ . '/helpers/cors.php';
require_once __DIR__ . '/helpers/response.php';

// Kembalikan info ringkas tentang API + daftar endpoint utama, agar
// mudah dicek tanpa membuka dokumentasi terpisah.
sendSuccess([
    'app'       => 'AbsenPro API',
    'version'   => '1.0',
    'endpoints' => [
        'POST /auth/register.php',
        'POST /auth/login.php',
        'POST /auth/logout.php',
        'GET|PUT /karyawan/index.php',
        'GET|PUT /lokasi_kantor/index.php',
        'GET|POST|PUT /sesi_absen/index.php',
        'GET|POST /absensi/index.php',
        'GET /absensi/rekap.php',
        'GET /absensi/export_csv.php',
        'GET|POST /izin/index.php',
        'PUT /izin/approval.php',
    ],
], 'AbsenPro API aktif.');
