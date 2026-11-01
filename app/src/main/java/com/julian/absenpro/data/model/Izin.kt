package com.julian.absenpro.data.model

import com.google.gson.annotations.SerializedName

/**
 * Item pengajuan izin (GET izin/index.php). fileBuktiUrl adalah URL siap
 * pakai (atau null bila tanpa lampiran) sehingga UI bisa langsung membuka
 * gambar/PDF-nya. catatanAdmin berisi alasan approve/tolak.
 */
data class Izin(
    @SerializedName("id") val id: Int,
    @SerializedName("karyawan_id") val karyawanId: Int,
    @SerializedName("nama") val nama: String,
    @SerializedName("jenis_izin") val jenisIzin: String,
    @SerializedName("tanggal_mulai") val tanggalMulai: String,
    @SerializedName("tanggal_selesai") val tanggalSelesai: String,
    @SerializedName("keterangan") val keterangan: String?,
    @SerializedName("file_bukti_url") val fileBuktiUrl: String?,
    @SerializedName("status_approval") val statusApproval: String, // menunggu|disetujui|ditolak
    @SerializedName("catatan_admin") val catatanAdmin: String?,
    @SerializedName("created_at") val createdAt: String
)

/**
 * Body request PUT izin/approval.php (admin). status hanya boleh
 * 'disetujui' atau 'ditolak'; catatan opsional (berguna saat menolak).
 */
data class ApprovalIzinRequest(
    @SerializedName("id") val id: Int,
    @SerializedName("status") val status: String,
    @SerializedName("catatan_admin") val catatanAdmin: String? = null
)

/**
 * Payload 'data' hasil submit izin sukses. Dipakai untuk konfirmasi cepat.
 */
data class IzinResult(
    @SerializedName("id") val id: Int,
    @SerializedName("file_bukti_url") val fileBuktiUrl: String?,
    @SerializedName("status_approval") val statusApproval: String
)
