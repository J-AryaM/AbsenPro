package com.julian.absenpro.data.repository

import com.julian.absenpro.data.model.ApiResponse
import retrofit2.Response
import java.io.IOException

/**
 * Induk semua Repository. Menyediakan pembungkus panggilan jaringan yang
 * menangani DUA jenis kegagalan sekaligus:
 *  - Kegagalan protokol/aplikasi (HTTP non-2xx, success=false) -> lewat
 *    handleResponse().
 *  - Kegagalan koneksi (server mati, tidak ada internet) yang berupa
 *    Exception -> ditangkap di sini agar tidak meng-crash aplikasi dan
 *    diubah menjadi ApiResult.Failure dengan pesan ramah.
 *
 * Dengan begitu ViewModel cukup menghadapi ApiResult (Success/Failure),
 * tidak perlu tahu detail try/catch jaringan.
 */
abstract class BaseRepository {

    /**
     * Jalankan panggilan API yang membawa data, tangkap error jaringannya.
     * @param call lambda suspend yang memanggil satu fungsi ApiService.
     */
    protected suspend fun <T> safeCall(call: suspend () -> Response<ApiResponse<T>>): ApiResult<T> {
        return try {
            handleResponse(call())
        } catch (e: IOException) {
            // IOException = masalah jaringan (host tak terjangkau, timeout).
            ApiResult.Failure("Tidak dapat terhubung ke server. Cek koneksi & BASE_URL.")
        } catch (e: Exception) {
            // Jaring pengaman untuk error tak terduga (mis. parsing JSON).
            ApiResult.Failure("Terjadi kesalahan: ${e.message}")
        }
    }

    /**
     * Varian safeCall untuk endpoint tanpa payload (mis. logout).
     */
    protected suspend fun safeCallNoData(call: suspend () -> Response<ApiResponse<Unit>>): ApiResult<Unit> {
        return try {
            handleResponseNoData(call())
        } catch (e: IOException) {
            ApiResult.Failure("Tidak dapat terhubung ke server. Cek koneksi & BASE_URL.")
        } catch (e: Exception) {
            ApiResult.Failure("Terjadi kesalahan: ${e.message}")
        }
    }
}
