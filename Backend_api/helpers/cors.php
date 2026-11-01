<?php
// =====================================================================
// Backend_api/helpers/cors.php
// Header CORS + penanganan preflight. Di-include paling awal pada tiap
// endpoint. Meski Android tidak menegakkan CORS seketat browser, header
// ini membuat API tetap bisa diuji dari browser/Postman tanpa error, dan
// preflight OPTIONS ditangani agar tidak menghasilkan 404/HTML.
// =====================================================================

// Izinkan akses dari mana saja (aman untuk lingkungan pengujian lokal).
header('Access-Control-Allow-Origin: *');
// Method yang didukung API secara keseluruhan.
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
// Izinkan header Authorization (untuk token) dan Content-Type (JSON/multipart).
header('Access-Control-Allow-Headers: Content-Type, Authorization');

// Browser mengirim OPTIONS lebih dulu (preflight) sebelum request asli.
// Balas kosong dengan 200 lalu berhenti — tidak perlu proses lebih lanjut.
if (($_SERVER['REQUEST_METHOD'] ?? '') === 'OPTIONS') {
    http_response_code(200);
    exit;
}
