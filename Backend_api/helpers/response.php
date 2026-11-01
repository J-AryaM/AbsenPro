<?php
// =====================================================================
// Backend_api/helpers/response.php
// Pembungkus respons JSON yang SERAGAM untuk semua endpoint. Android
// (Retrofit + Gson) mengandalkan bentuk yang selalu sama:
//   { "success": bool, "message": string, "data": object|array|null }
// Kalau tiap endpoint bikin format sendiri, parsing di Android akan pecah.
// =====================================================================

/**
 * Kirim respons SUKSES lalu hentikan skrip.
 *
 * @param mixed  $data    payload yang dikirim ke klien (objek/array/null)
 * @param string $message pesan singkat untuk ditampilkan di UI
 * @param int    $code    HTTP status (200 default, 201 untuk "created")
 */
function sendSuccess($data = null, string $message = 'OK', int $code = 200): void
{
    http_response_code($code);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode([
        'success' => true,
        'message' => $message,
        'data'    => $data,
    ]);
    // exit penting: mencegah kode setelah pemanggilan ikut ter-output
    // dan merusak JSON (mis. warning/echo tak sengaja).
    exit;
}

/**
 * Kirim respons ERROR lalu hentikan skrip.
 * data selalu null pada error agar Android cukup memeriksa "success".
 *
 * @param string $message alasan error yang aman ditampilkan ke user
 * @param int    $code    HTTP status yang sesuai (400/401/403/404/500)
 */
function sendError(string $message = 'Terjadi kesalahan', int $code = 400): void
{
    http_response_code($code);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode([
        'success' => false,
        'message' => $message,
        'data'    => null,
    ]);
    exit;
}

/**
 * Ambil body JSON dari request POST/PUT dan decode jadi array assoc.
 * Android mengirim body sebagai application/json untuk endpoint non-file,
 * sehingga tidak muncul di $_POST — harus dibaca dari php://input.
 *
 * @return array hasil decode; array kosong bila body bukan JSON valid
 */
function getJsonInput(): array
{
    $raw = file_get_contents('php://input');
    if ($raw === '' || $raw === false) {
        return [];
    }
    $decoded = json_decode($raw, true);
    // Bila JSON rusak, kembalikan array kosong supaya pemanggil bisa
    // memvalidasi "field wajib kosong" alih-alih fatal error.
    return is_array($decoded) ? $decoded : [];
}

/**
 * Pastikan method HTTP request sesuai yang diizinkan endpoint.
 * Dipanggil di awal tiap endpoint; kalau tidak cocok -> 405.
 *
 * @param string|array $allowed method yang boleh, mis. 'POST' atau ['GET','PUT']
 */
function requireMethod($allowed): void
{
    $allowedList = is_array($allowed) ? $allowed : [$allowed];
    $method = $_SERVER['REQUEST_METHOD'] ?? 'GET';
    if (!in_array($method, $allowedList, true)) {
        sendError('Method tidak diizinkan untuk endpoint ini.', 405);
    }
}
