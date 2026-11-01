package com.julian.absenpro.data.model

import com.google.gson.annotations.SerializedName

/**
 * Sesi jendela waktu absen (masuk/pulang) per tanggal.
 * jamBuka/jamTutup berformat "HH:MM" (tanpa detik), langsung ditampilkan.
 */
data class SesiAbsen(
    @SerializedName("id") val id: Int,
    @SerializedName("tipe") val tipe: String,        // "masuk" | "pulang"
    @SerializedName("tanggal") val tanggal: String,  // "YYYY-MM-DD"
    @SerializedName("jam_buka") val jamBuka: String,
    @SerializedName("jam_tutup") val jamTutup: String,
    @SerializedName("status") val status: String     // "aktif" | "nonaktif"
)

/**
 * Body request POST sesi_absen/index.php: admin membuat / menimpa sesi
 * (tipe+tanggal dijadikan kunci upsert oleh server).
 */
data class CreateSesiRequest(
    @SerializedName("tipe") val tipe: String,
    @SerializedName("tanggal") val tanggal: String,
    @SerializedName("jam_buka") val jamBuka: String,
    @SerializedName("jam_tutup") val jamTutup: String,
    @SerializedName("status") val status: String
)

/**
 * Body request PUT sesi_absen/index.php: admin cukup mengubah status
 * (buka/tutup sesi tanpa mengubah jam).
 */
data class UpdateSesiStatusRequest(
    @SerializedName("id") val id: Int,
    @SerializedName("status") val status: String
)

/**
 * Payload 'data' balasan PUT status sesi. Server hanya mengembalikan id
 * dan status baru (bukan objek sesi penuh), jadi modelnya ringkas.
 */
data class SesiAbsenStatus(
    @SerializedName("id") val id: Int,
    @SerializedName("status") val status: String
)
