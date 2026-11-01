package com.julian.absenpro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.julian.absenpro.data.model.CreateSesiRequest
import com.julian.absenpro.data.model.SesiAbsen
import com.julian.absenpro.data.repository.ApiResult
import com.julian.absenpro.data.repository.SesiRepository
import com.julian.absenpro.util.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel kontrol sesi absen (admin). Admin dapat melihat sesi hari ini,
 * membuat/menyetel sesi masuk & pulang, serta membuka/menutup masing-masing.
 *
 * Alur state:
 *  - sesiState  : daftar sesi pada tanggal terpilih.
 *  - actionState: hasil aksi terakhir (buat/ubah status), lalu daftar
 *    dimuat ulang agar UI mencerminkan perubahan.
 */
class SesiViewModel(
    private val sesiRepository: SesiRepository
) : ViewModel() {

    private val _sesiState = MutableStateFlow<UiState<List<SesiAbsen>>>(UiState.Idle)
    val sesiState: StateFlow<UiState<List<SesiAbsen>>> = _sesiState.asStateFlow()

    private val _actionState = MutableStateFlow<UiState<SesiAbsen>>(UiState.Idle)
    val actionState: StateFlow<UiState<SesiAbsen>> = _actionState.asStateFlow()

    // Tanggal yang sedang ditampilkan; disimpan agar reload memakai tanggal
    // yang sama setelah aksi. Diisi saat loadSesi dipanggil layar.
    private var currentTanggal: String? = null

    /** Muat sesi untuk tanggal tertentu (null = hari ini di server). */
    fun loadSesi(tanggal: String?) {
        currentTanggal = tanggal
        viewModelScope.launch {
            _sesiState.value = UiState.Loading
            when (val r = sesiRepository.getSesi(tanggal)) {
                is ApiResult.Success -> _sesiState.value = UiState.Success(r.data)
                is ApiResult.Failure -> _sesiState.value = UiState.Error(r.message)
            }
        }
    }

    /**
     * Buat/timpa sesi. Server memakai (tipe, tanggal) sebagai kunci upsert,
     * jadi memanggil ini untuk tipe+tanggal yang sudah ada = memperbaruinya.
     */
    fun simpanSesi(
        tipe: String,
        tanggal: String,
        jamBuka: String,
        jamTutup: String,
        status: String
    ) {
        viewModelScope.launch {
            _actionState.value = UiState.Loading
            val req = CreateSesiRequest(tipe, tanggal, jamBuka, jamTutup, status)
            when (val r = sesiRepository.createSesi(req)) {
                is ApiResult.Success -> {
                    _actionState.value = UiState.Success(r.data)
                    loadSesi(tanggal) // segarkan daftar untuk tanggal itu
                }
                is ApiResult.Failure -> _actionState.value = UiState.Error(r.message)
            }
        }
    }

    /**
     * Buka/tutup sesi tertentu. Setelah berhasil, muat ulang daftar pada
     * tanggal yang sedang ditampilkan agar status tombol ikut berubah.
     */
    fun ubahStatus(id: Int, status: String) {
        viewModelScope.launch {
            when (val r = sesiRepository.updateStatus(id, status)) {
                is ApiResult.Success -> loadSesi(currentTanggal)
                is ApiResult.Failure -> _sesiState.value = UiState.Error(r.message)
            }
        }
    }

    /** Reset status aksi setelah UI menanggapinya. */
    fun resetActionState() {
        _actionState.value = UiState.Idle
    }
}
