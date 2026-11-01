package com.julian.absenpro.ui.navigation

/**
 * Kumpulan nama rute navigasi sebagai konstanta. Memakai konstanta (bukan
 * string mentah tersebar) agar tidak ada typo saat navigate() dan mudah
 * mengganti nama rute di satu tempat.
 *
 * Rute dikelompokkan: auth (login/register), area karyawan, dan area admin.
 */
object Routes {
    // --- Auth ---
    const val LOGIN = "login"
    const val REGISTER = "register"

    // --- Karyawan ---
    const val KARYAWAN_HOME = "karyawan_home"     // beranda + tombol absen
    const val KARYAWAN_RIWAYAT = "karyawan_riwayat"
    const val KARYAWAN_IZIN = "karyawan_izin"     // daftar + ajukan izin
    const val KARYAWAN_PROFIL = "karyawan_profil"

    // --- Admin ---
    const val ADMIN_HOME = "admin_home"           // dashboard menu admin
    const val ADMIN_KARYAWAN = "admin_karyawan"
    const val ADMIN_LOKASI = "admin_lokasi"
    const val ADMIN_SESI = "admin_sesi"
    const val ADMIN_IZIN = "admin_izin"           // approval izin
    const val ADMIN_REKAP = "admin_rekap"         // rekap + export CSV
}
