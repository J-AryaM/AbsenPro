package com.julian.absenpro.data.model

import com.google.gson.annotations.SerializedName

/**
 * Item daftar karyawan yang dilihat admin. Nilai-nilai nullable
 * mencerminkan kolom di DB yang boleh kosong (nomor_induk, jabatan, kontak).
 * isActive dipetakan ke tipe Boolean karena server mengirimnya sebagai
 * true/false (bukan 1/0) -> lihat karyawan/index.php.
 */
data class Karyawan(
    @SerializedName("id") val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("nama") val nama: String,
    @SerializedName("nomor_induk") val nomorInduk: String?,
    @SerializedName("jabatan") val jabatan: String?,
    @SerializedName("kontak") val kontak: String?,
    @SerializedName("username") val username: String,
    @SerializedName("is_active") val isActive: Boolean
)

/**
 * Body request PUT karyawan/index.php. Bersifat partial update: field
 * yang tidak ingin diubah cukup dibiarkan null -> tidak dikirim.
 * Karena itu semua field opsional (kecuali id, tapi id dikirim di URL
 * body -> tetap perlu diisi pemanggilnya).
 */
data class UpdateKaryawanRequest(
    @SerializedName("id") val id: Int,
    @SerializedName("nama") val nama: String? = null,
    @SerializedName("jabatan") val jabatan: String? = null,
    @SerializedName("kontak") val kontak: String? = null,
    @SerializedName("nomor_induk") val nomorInduk: String? = null,
    @SerializedName("is_active") val isActive: Boolean? = null
)
