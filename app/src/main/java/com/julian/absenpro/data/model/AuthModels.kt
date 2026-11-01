package com.julian.absenpro.data.model

import com.google.gson.annotations.SerializedName

/**
 * Body request untuk POST auth/login.php.
 * Nama field harus persis 'username' & 'password' (server memakainya).
 */
data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

/**
 * Body request untuk POST auth/register.php. Field opsional (jabatan,
 * kontak) dibuat nullable; Gson akan mengabaikan yang bernilai null saat
 * meng-encode, sehingga server menerima field yang benar-benar diisi saja.
 */
data class RegisterRequest(
    @SerializedName("nama") val nama: String,
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String,
    @SerializedName("jabatan") val jabatan: String? = null,
    @SerializedName("kontak") val kontak: String? = null
)

/**
 * Payload 'data' dari login sukses. karyawan_id bernilai null bila akun
 * yang login adalah admin (admin tidak memiliki baris karyawan).
 */
data class LoginData(
    @SerializedName("token") val token: String,
    @SerializedName("role") val role: String,           // "admin" atau "karyawan"
    @SerializedName("user_id") val userId: Int,
    @SerializedName("karyawan_id") val karyawanId: Int?, // null untuk admin
    @SerializedName("nama") val nama: String
)

/**
 * Payload 'data' dari register sukses. Hanya id yang baru dibuat;
 * karyawan langsung diarahkan ke halaman login setelah ini.
 */
data class RegisterData(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("karyawan_id") val karyawanId: Int
)
