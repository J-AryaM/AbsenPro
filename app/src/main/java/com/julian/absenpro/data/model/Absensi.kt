package com.julian.absenpro.data.model

import com.google.gson.annotations.SerializedName

/**
 * Body request POST absensi/index.php. Server memvalidasi ulang koordinat
 * ini terhadap lokasi kantor (Haversine), jadi Android hanya perlu
 * mengirim posisi apa adanya + tipe absen.
 */
data class AbsensiRequest(
    @SerializedName("tipe") val tipe: String,        // "masuk" | "pulang"
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double
)

/**
 * Payload 'data' hasil absen sukses. Dipakai UI untuk menampilkan
 * konfirmasi: jarak terukur, status hadir/telat, dan waktu tercatat.
 */
data class AbsensiResult(
    @SerializedName("id") val id: Int,
    @SerializedName("tipe") val tipe: String,
    @SerializedName("jarak_ke_kantor") val jarakKeKantor: Double,
    @SerializedName("radius_meter") val radiusMeter: Int,
    @SerializedName("status") val status: String,    // "hadir" | "telat"
    @SerializedName("waktu_absen") val waktuAbsen: String
)

/**
 * Item riwayat absensi pribadi (GET absensi/index.php). 'jam' sudah
 * dipisah server dari waktu_absen agar UI tinggal menampilkan.
 */
data class RiwayatAbsensi(
    @SerializedName("id") val id: Int,
    @SerializedName("tipe") val tipe: String,
    @SerializedName("tanggal") val tanggal: String,
    @SerializedName("jam") val jam: String,
    @SerializedName("status") val status: String,
    @SerializedName("jarak_ke_kantor") val jarakKeKantor: Double
)

/**
 * Item rekap absensi yang dilihat admin (GET absensi/rekap.php). Sama
 * seperti RiwayatAbsensi tetapi menyertakan identitas karyawan karena
 * rekap mencakup semua orang.
 */
data class RekapAbsensi(
    @SerializedName("id") val id: Int,
    @SerializedName("karyawan_id") val karyawanId: Int,
    @SerializedName("nama") val nama: String,
    @SerializedName("tipe") val tipe: String,
    @SerializedName("tanggal") val tanggal: String,
    @SerializedName("jam") val jam: String,
    @SerializedName("status") val status: String,
    @SerializedName("jarak_ke_kantor") val jarakKeKantor: Double
)
