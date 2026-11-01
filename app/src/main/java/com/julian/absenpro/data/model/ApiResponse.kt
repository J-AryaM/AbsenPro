package com.julian.absenpro.data.model

import com.google.gson.annotations.SerializedName

/**
 * Cerminan Kotlin dari amplop JSON seragam yang dikirim backend:
 *   { "success": bool, "message": string, "data": <T> }
 *
 * Semua endpoint membungkus hasilnya seperti ini, jadi Retrofit selalu
 * mengembalikan ApiResponse<...> dan Repository cukup memeriksa 'success'
 * satu kali untuk memutuskan sukses/gagal.
 *
 * @param T tipe payload 'data'; berbeda tiap endpoint (mis. LoginData).
 */
data class ApiResponse<T>(
    // @SerializedName memetakan nama field JSON -> properti Kotlin. Nama
    // sudah sama, tapi ditulis eksplisit agar tahan terhadap obfuscation
    // ProGuard/R8 yang bisa mengganti nama properti.
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: T?
)
