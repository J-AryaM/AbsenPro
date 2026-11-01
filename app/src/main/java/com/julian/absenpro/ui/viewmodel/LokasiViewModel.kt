package com.julian.absenpro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.julian.absenpro.data.model.LokasiKantor
import com.julian.absenpro.data.repository.ApiResult
import com.julian.absenpro.data.repository.LokasiRepository
import com.julian.absenpro.util.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel atur lokasi kantor (admin). Memuat nilai saat ini untuk mengisi
 * form, lalu menyimpan perubahan koordinat/radius.
 *
 * Alur state:
 *  - lokasiState: nilai lokasi saat ini (untuk prefill form).
 *  - saveState  : hasil penyimpanan (Loading -> Success/Error). UI
 *    menampilkan konfirmasi lalu memuat ulang lokasiState.
 */
class LokasiViewModel(
    private val lokasiRepository: LokasiRepository
) : ViewModel() {

    private val _lokasiState = MutableStateFlow<UiState<LokasiKantor>>(UiState.Idle)
    val lokasiState: StateFlow<UiState<LokasiKantor>> = _lokasiState.asStateFlow()

    private val _saveState = MutableStateFlow<UiState<LokasiKantor>>(UiState.Idle)
    val saveState: StateFlow<UiState<LokasiKantor>> = _saveState.asStateFlow()

    /** Muat lokasi kantor untuk mengisi form awal. */
    fun loadLokasi() {
        viewModelScope.launch {
            _lokasiState.value = UiState.Loading
            when (val r = lokasiRepository.getLokasi()) {
                is ApiResult.Success -> _lokasiState.value = UiState.Success(r.data)
                is ApiResult.Failure -> _lokasiState.value = UiState.Error(r.message)
            }
        }
    }

    /**
     * Simpan lokasi baru. Menerima input sebagai String dari form lalu
     * mengonversinya ke angka; bila konversi gagal (input bukan angka),
     * langsung Error tanpa memanggil server.
     */
    fun simpan(latitude: String, longitude: String, radius: String, namaKantor: String) {
        val lat = latitude.trim().toDoubleOrNull()
        val lon = longitude.trim().toDoubleOrNull()
        val rad = radius.trim().toIntOrNull()

        if (lat == null || lon == null || rad == null) {
            _saveState.value = UiState.Error("Latitude, longitude, dan radius harus berupa angka.")
            return
        }

        viewModelScope.launch {
            _saveState.value = UiState.Loading
            val nama = namaKantor.trim().ifBlank { "Kantor Pusat" }
            when (val r = lokasiRepository.updateLokasi(lat, lon, rad, nama)) {
                is ApiResult.Success -> {
                    _saveState.value = UiState.Success(r.data)
                    // Perbarui juga lokasiState agar form menampilkan nilai terbaru.
                    _lokasiState.value = UiState.Success(r.data)
                }
                is ApiResult.Failure -> _saveState.value = UiState.Error(r.message)
            }
        }
    }

    /** Reset status simpan setelah UI menanggapinya. */
    fun resetSaveState() {
        _saveState.value = UiState.Idle
    }
}
