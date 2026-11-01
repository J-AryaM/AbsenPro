package com.julian.absenpro.data.model

import com.google.gson.annotations.SerializedName

/**
 * Titik acuan geofencing (satu baris di server). Karyawan memakainya
 * untuk menghitung jarak lokal (sebelum server memvalidasi ulang), dan
 * admin memakainya sebagai form nilai saat mengubah koordinat/radius.
 */
data class LokasiKantor(
    @SerializedName("id") val id: Int,
    @SerializedName("nama_kantor") val namaKantor: String,
    // Double dipakai (bukan Float) karena presisi koordinat GPS butuh
    // banyak digit desimal; Float bisa membulatkan sampai puluhan meter.
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("radius_meter") val radiusMeter: Int
)

/**
 * Body request untuk PUT lokasi_kantor/index.php (khusus admin).
 * Semua field wajib karena server memvalidasi rentangnya.
 */
data class UpdateLokasiRequest(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("radius_meter") val radiusMeter: Int,
    @SerializedName("nama_kantor") val namaKantor: String
)
