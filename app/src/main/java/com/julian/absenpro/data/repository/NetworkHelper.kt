package com.julian.absenpro.data.repository

import com.julian.absenpro.data.model.ApiResponse
import retrofit2.Response

/**
 * Hasil pemanggilan API dalam bentuk sederhana untuk Repository/ViewModel.
 * Membungkus dua kemungkinan akhir: berhasil membawa data, atau gagal
 * membawa pesan. Ini memisahkan "detail HTTP/JSON" dari lapisan atas.
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Failure(val message: String) : ApiResult<Nothing>()
}

/**
 * Menerjemahkan Response<ApiResponse<T>> menjadi ApiResult untuk endpoint
 * yang MEMBAWA data (login, list, dsb). Semua Repository memakai ini agar
 * penanganan error seragam.
 *
 * Alur keputusan:
 *  1. HTTP gagal (401/500/..) -> ambil 'message' dari errorBody -> Failure.
 *  2. HTTP ok tapi success=false -> Failure(pesan server).
 *  3. success=true tapi data null -> Failure (data yang diharapkan hilang).
 *  4. Selain itu -> Success(data).
 */
fun <T> handleResponse(response: Response<ApiResponse<T>>): ApiResult<T> {
    if (!response.isSuccessful) {
        val serverMessage = parseErrorMessage(response)
        return ApiResult.Failure(serverMessage ?: "Kesalahan server (kode ${response.code()}).")
    }

    val body = response.body()
        ?: return ApiResult.Failure("Respons kosong dari server.")

    if (!body.success) {
        return ApiResult.Failure(body.message)
    }

    val data = body.data
        ?: return ApiResult.Failure("Data kosong dari server.")

    return ApiResult.Success(data)
}

/**
 * Varian untuk endpoint TANPA payload (mis. logout). Cukup memeriksa
 * status HTTP & flag success; data memang diabaikan. Mengembalikan
 * ApiResult<Unit> agar ViewModel tetap bisa membedakan sukses/gagal.
 */
fun handleResponseNoData(response: Response<ApiResponse<Unit>>): ApiResult<Unit> {
    if (!response.isSuccessful) {
        val serverMessage = parseErrorMessage(response)
        return ApiResult.Failure(serverMessage ?: "Kesalahan server (kode ${response.code()}).")
    }
    val body = response.body()
        ?: return ApiResult.Failure("Respons kosong dari server.")
    if (!body.success) {
        return ApiResult.Failure(body.message)
    }
    return ApiResult.Success(Unit)
}

/**
 * Ambil field 'message' dari errorBody JSON bila memungkinkan, supaya
 * pesan error dari server (mis. "Username atau password salah.") tetap
 * sampai ke pengguna meski status HTTP-nya bukan 2xx.
 */
private fun parseErrorMessage(response: Response<*>): String? {
    return try {
        val raw = response.errorBody()?.string() ?: return null
        // Parsing ringan tanpa model: cari "message":"..." di JSON error.
        val regex = "\"message\"\\s*:\\s*\"(.*?)\"".toRegex()
        regex.find(raw)?.groupValues?.get(1)
    } catch (e: Exception) {
        null // bila gagal parse, biarkan pemanggil memakai pesan default
    }
}
