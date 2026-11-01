<?php
// =====================================================================
// AbsenPro - seed_admin.php
// Skrip cadangan untuk MEMBUAT / ME-RESET akun admin dengan hash yang
// PASTI valid pada versi PHP yang sedang dipakai.
//
// Kenapa perlu ada, padahal seed.sql sudah menaruh hash?
//   Hash bcrypt di seed.sql dibuat di satu mesin. Meski bcrypt bersifat
//   portable, cara paling aman menghindari "login admin gagal" adalah
//   membiarkan PHP di mesin target sendiri yang menghitung hash-nya
//   memakai password_hash(). Skrip ini melakukan itu.
//
// Cara pakai (via Laragon):
//   Buka di browser: http://localhost/absenpro_api/../Database/seed_admin.php
//   atau taruh folder Database di document root lalu akses langsung.
//   Lebih praktis: jalankan lewat CLI ->  php seed_admin.php
// =====================================================================

// Koneksi langsung (skrip ini berdiri sendiri, tidak lewat config/db.php
// karena letaknya di folder Database, bukan Backend_api).
$host = '127.0.0.1';
$db   = 'absenpro';
$user = 'root';
$pass = '';         // Laragon default: user root tanpa password

// Kredensial admin default yang akan dibuat.
$adminUsername = 'admin';
$adminPassword = 'admin123';

header('Content-Type: text/plain; charset=utf-8');

try {
    // Sambung ke MySQL memakai PDO + mode exception agar error langsung terlihat.
    $pdo = new PDO(
        "mysql:host=$host;dbname=$db;charset=utf8mb4",
        $user,
        $pass,
        [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION]
    );

    // Hitung hash di mesin target -> dijamin cocok dengan password_verify()
    // pada PHP yang sama saat login nanti.
    $hash = password_hash($adminPassword, PASSWORD_BCRYPT);

    // Cek apakah admin sudah ada. Bila ada -> update hash-nya (reset),
    // bila belum -> insert baru. Pola "upsert" manual ini membuat skrip
    // aman dijalankan berkali-kali tanpa menghasilkan admin ganda.
    $stmt = $pdo->prepare("SELECT id FROM users WHERE username = ?");
    $stmt->execute([$adminUsername]);
    $existing = $stmt->fetch(PDO::FETCH_ASSOC);

    if ($existing) {
        $upd = $pdo->prepare(
            "UPDATE users SET password = ?, role = 'admin', status_aktif = 1 WHERE id = ?"
        );
        $upd->execute([$hash, $existing['id']]);
        echo "OK: password admin di-reset.\n";
    } else {
        $ins = $pdo->prepare(
            "INSERT INTO users (username, password, role, status_aktif) VALUES (?, ?, 'admin', 1)"
        );
        $ins->execute([$adminUsername, $hash]);
        echo "OK: akun admin baru dibuat.\n";
    }

    echo "Login  -> username: $adminUsername  password: $adminPassword\n";
} catch (PDOException $e) {
    // Tampilkan pesan error koneksi/SQL agar mudah didiagnosa saat setup.
    http_response_code(500);
    echo "GAGAL: " . $e->getMessage() . "\n";
}
