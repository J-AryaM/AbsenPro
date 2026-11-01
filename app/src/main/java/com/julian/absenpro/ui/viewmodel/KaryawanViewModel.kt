package com.julian.absenpro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.julian.absenpro.data.model.Karyawan
import com.julian.absenpro.data.model.UpdateKaryawanRequest
import com.julian.absenpro.data.repository.ApiResult
import com.julian.absenpro.data.repository.KaryawanRepository
import com.julian.absenpro.util.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel kelola karyawan (admin). Menyediakan daftar karyawan dan aksi
 * update/nonaktifkan.
 *
 * Alur state:
 *  - listState  : daftar karyawan (Loading -> Success/Error).
 *  - actionState: hasil update terakhir; dipakai untuk menampilkan pesan
 *    sukses/gagal, lalu daftar dimuat ulang agar UI sinkron.
 */
class KaryawanViewModel(
    private val karyawanRepository: KaryawanRepository
) : ViewModel() {

    private val _listState = MutableStateFlow<UiState<List<Karyawan>>>(UiState.Idle)
    val listState: StateFlow<UiState<List<Karyawan>>> = _listState.asStateFlow()

    private val _actionState = MutableStateFlow<UiState<Karyawan>>(UiState.Idle)
    val actionState: StateFlow<UiState<Karyawan>> = _actionState.asStateFlow()

    /** Muat daftar semua karyawan untuk layar kelola karyawan. */
    fun loadList() {
        viewModelScope.launch {
            _listState.value = UiState.Loading
            when (val r = karyawanRepository.getList()) {
                is ApiResult.Success -> _listState.value = UiState.Success(r.data)
                is ApiResult.Failure -> _listState.value = UiState.Error(r.message)
            }
        }
    }

    /**
     * Aktifkan/nonaktifkan akun karyawan. Hanya mengirim field is_active
     * (partial update). Setelah sukses, daftar dimuat ulang agar badge
     * status berubah seketika.
     */
    fun setActive(karyawanId: Int, active: Boolean) {
        viewModelScope.launch {
            _actionState.value = UiState.Loading
            val req = UpdateKaryawanRequest(id = karyawanId, isActive = active)
            when (val r = karyawanRepository.update(req)) {
                is ApiResult.Success -> {
                    _actionState.value = UiState.Success(r.data)
                    loadList()
                }
                is ApiResult.Failure -> _actionState.value = UiState.Error(r.message)
            }
        }
    }

    /**
     * Ubah data profil karyawan (nama/jabatan/kontak/nomor induk).
     * Field null dibiarkan tidak berubah oleh server.
     */
    fun updateData(
        karyawanId: Int,
        nama: String?,
        jabatan: String?,
        kontak: String?,
        nomorInduk: String?
    ) {
        viewModelScope.launch {
            _actionState.value = UiState.Loading
            val req = UpdateKaryawanRequest(
                id = karyawanId,
                nama = nama,
                jabatan = jabatan,
                kontak = kontak,
                nomorInduk = nomorInduk
            )
            when (val r = karyawanRepository.update(req)) {
                is ApiResult.Success -> {
                    _actionState.value = UiState.Success(r.data)
                    loadList()
                }
                is ApiResult.Failure -> _actionState.value = UiState.Error(r.message)
            }
        }
    }

    /** Reset status aksi setelah UI menanggapinya. */
    fun resetActionState() {
        _actionState.value = UiState.Idle
    }
}
