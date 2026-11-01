package com.julian.absenpro.di

import android.content.Context
import com.julian.absenpro.data.local.SessionManager
import com.julian.absenpro.data.remote.ApiService
import com.julian.absenpro.data.remote.RetrofitClient
import com.julian.absenpro.data.repository.AbsensiRepository
import com.julian.absenpro.data.repository.AuthRepository
import com.julian.absenpro.data.repository.IzinRepository
import com.julian.absenpro.data.repository.KaryawanRepository
import com.julian.absenpro.data.repository.LokasiRepository
import com.julian.absenpro.data.repository.SesiRepository

/**
 * "Service Locator" sederhana sebagai pengganti library DI (Hilt/Koin).
 * Untuk proyek berskala UAS ini, satu wadah manual sudah cukup: ia membuat
 * dependency (SessionManager, ApiService, Repository) SEKALI lalu
 * membagikannya, sehingga tidak ada objek jaringan/sesi yang dibuat ganda.
 *
 * Harus di-init() sekali di Application/MainActivity dengan applicationContext
 * agar tidak menahan referensi Activity (mencegah memory leak).
 */
object ServiceLocator {

    // Ditandai lateinit karena diisi saat init(); dipakai setelah itu.
    private lateinit var appContext: Context

    // by lazy: instance baru dibuat saat pertama diakses, lalu dipakai ulang.
    // Urutannya penting: session dulu, karena RetrofitClient butuh session
    // untuk interceptor token.
    val sessionManager: SessionManager by lazy { SessionManager(appContext) }

    private val apiService: ApiService by lazy {
        RetrofitClient.getApiService(sessionManager)
    }

    // Repository dibagikan sebagai singleton ringan. Semua ViewModel
    // mengambil dari sini sehingga konsisten memakai ApiService yang sama.
    val authRepository: AuthRepository by lazy { AuthRepository(apiService, sessionManager) }
    val karyawanRepository: KaryawanRepository by lazy { KaryawanRepository(apiService) }
    val lokasiRepository: LokasiRepository by lazy { LokasiRepository(apiService) }
    val sesiRepository: SesiRepository by lazy { SesiRepository(apiService) }
    val absensiRepository: AbsensiRepository by lazy { AbsensiRepository(apiService) }
    val izinRepository: IzinRepository by lazy { IzinRepository(apiService, appContext) }

    /** Panggil sekali sebelum memakai dependency apa pun. */
    fun init(context: Context) {
        // Selalu simpan applicationContext, bukan Activity, agar aman dari leak.
        appContext = context.applicationContext
    }
}
