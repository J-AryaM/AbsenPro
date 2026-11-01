package com.julian.absenpro.util

/**
 * Konstanta global aplikasi. Dikumpulkan di satu tempat agar nilai yang
 * sering berubah antar-lingkungan (terutama alamat server) cukup diubah
 * di sini, tidak tersebar di banyak file.
 */
object Constants {

    /**
     * Alamat dasar REST API PHP.
     *
     * PENTING saat testing di Android Studio:
     *  - EMULATOR: gunakan 10.0.2.2 -> ini alias khusus emulator yang
     *    menunjuk ke "localhost" laptop tempat Laragon berjalan. Jangan
     *    pakai 127.0.0.1 karena itu menunjuk ke emulator itu sendiri.
     *  - HP FISIK (satu WiFi): ganti dengan IP laptop, mis.
     *    "http://192.168.1.10/absenpro_api/".
     *
     * Wajib diakhiri '/' karena Retrofit menggabungkannya dengan path
     * relatif tiap endpoint (mis. "auth/login.php").
     *
     * Folder "absenpro_api" harus sama dengan nama folder Backend_api
     * saat diletakkan di www Laragon.
     */
    const val BASE_URL = "http://10.0.2.2/absenpro_api/"

    /**
     * Nama file penyimpanan aman (EncryptedSharedPreferences) untuk sesi.
     * Dipakai SessionManager sebagai identitas file preferensi.
     */
    const val SECURE_PREFS_NAME = "absenpro_secure_prefs"

    /**
     * Radius default (meter) yang dipakai UI hanya sebagai nilai tampilan
     * awal sebelum data lokasi kantor termuat dari server. Validasi radius
     * yang sebenarnya tetap memakai nilai dari server (lokasi_kantor).
     */
    const val DEFAULT_RADIUS_METER = 150
}
