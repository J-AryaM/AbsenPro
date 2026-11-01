package com.julian.absenpro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.julian.absenpro.di.ServiceLocator

/**
 * Pabrik ViewModel manual. Karena ViewModel di aplikasi ini punya
 * parameter konstruktor (Repository), sistem tidak bisa membuatnya
 * otomatis; factory inilah yang menyediakan dependency dari ServiceLocator.
 *
 * Satu factory menangani semua ViewModel agar pemakaian di Composable
 * ringkas: cukup viewModel(factory = ViewModelFactory).
 */
object ViewModelFactory : ViewModelProvider.Factory {

    // create() dipanggil framework saat sebuah ViewModel pertama diminta.
    // Kita cocokkan kelas yang diminta lalu rakit dengan repository terkait.
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val sl = ServiceLocator
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) ->
                AuthViewModel(sl.authRepository, sl.sessionManager) as T

            modelClass.isAssignableFrom(AbsensiViewModel::class.java) ->
                AbsensiViewModel(sl.absensiRepository, sl.lokasiRepository, sl.sesiRepository) as T

            modelClass.isAssignableFrom(IzinViewModel::class.java) ->
                IzinViewModel(sl.izinRepository) as T

            modelClass.isAssignableFrom(KaryawanViewModel::class.java) ->
                KaryawanViewModel(sl.karyawanRepository) as T

            modelClass.isAssignableFrom(LokasiViewModel::class.java) ->
                LokasiViewModel(sl.lokasiRepository) as T

            modelClass.isAssignableFrom(SesiViewModel::class.java) ->
                SesiViewModel(sl.sesiRepository) as T

            modelClass.isAssignableFrom(RekapViewModel::class.java) ->
                RekapViewModel(sl.absensiRepository, sl.karyawanRepository) as T

            // Bila ada ViewModel yang belum terdaftar, gagal cepat dengan
            // pesan jelas daripada menghasilkan bug diam-diam.
            else -> throw IllegalArgumentException("ViewModel tidak dikenal: ${modelClass.name}")
        }
    }
}
