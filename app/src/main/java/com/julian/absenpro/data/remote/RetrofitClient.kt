package com.julian.absenpro.data.remote

import com.julian.absenpro.data.local.SessionManager
import com.julian.absenpro.util.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Pabrik tunggal untuk ApiService. Menyiapkan OkHttp (dengan interceptor
 * token + logging) dan Retrofit (dengan konverter Gson), lalu men-cache
 * instance-nya supaya tidak dibuat berulang tiap panggilan API.
 *
 * SessionManager dibutuhkan agar interceptor bisa membaca token terbaru
 * untuk disisipkan ke tiap request.
 */
object RetrofitClient {

    // Disimpan agar init() cukup dipanggil sekali (di Application/MainActivity),
    // lalu getApiService() memakai instance yang sama.
    @Volatile
    private var apiService: ApiService? = null

    /**
     * Bangun (atau kembalikan yang sudah ada) ApiService.
     * @param sessionManager sumber token untuk header Authorization.
     */
    fun getApiService(sessionManager: SessionManager): ApiService {
        // Double-checked locking: aman dari race condition bila dipanggil
        // dari beberapa coroutine sekaligus, tanpa mengunci setiap akses.
        return apiService ?: synchronized(this) {
            apiService ?: buildRetrofit(sessionManager).create(ApiService::class.java).also {
                apiService = it
            }
        }
    }

    /** Rakit Retrofit lengkap dengan OkHttp yang sudah dikonfigurasi. */
    private fun buildRetrofit(sessionManager: SessionManager): Retrofit {
        // Interceptor logging: mencetak isi request/response ke Logcat saat
        // debug, memudahkan menelusuri masalah kontrak API tanpa debugger.
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // Interceptor token: menyisipkan "Authorization: Bearer <token>"
        // ke SETIAP request bila token ada. Dengan begini tiap fungsi API
        // tidak perlu menuliskan header-nya sendiri (lebih konsisten).
        val authInterceptor = okhttp3.Interceptor { chain ->
            val original = chain.request()
            val token = sessionManager.getToken()
            val request = if (token != null) {
                original.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                original // belum login (mis. saat login/register) -> kirim apa adanya
            }
            chain.proceed(request)
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            // Timeout wajar agar tidak menggantung bila server lokal mati.
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)   // sudah diakhiri '/'
            .client(client)
            .addConverterFactory(GsonConverterFactory.create()) // JSON <-> data class
            .build()
    }
}
