package com.julian.absenpro.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.julian.absenpro.data.model.Izin
import com.julian.absenpro.data.model.IzinResult
import com.julian.absenpro.data.repository.ApiResult
import com.julian.absenpro.data.repository.IzinRepository
import com.julian.absenpro.util.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel izin, dipakai oleh DUA peran:
 *  - Karyawan: mengajukan izin (submit) + melihat daftar/status miliknya.
 *  - Admin   : melihat semua izin + menyetujui/menolak (approval).
 * Server yang memfilter daftar sesuai peran token, jadi ViewModel-nya sama.
 *
 * Alur state:
 *  - listState   : daftar izin (Loading saat memuat).
 *  - submitState : hasil pengajuan (Loading saat upload; Success/Error).
 *  - approvalState: hasil keputusan admin.
 */
class IzinViewModel(
    private val izinRepository: IzinRepository
) : ViewModel() {

    private val _listState = MutableStateFlow<UiState<List<Izin>>>(UiState.Idle)
    val listState: StateFlow<UiState<List<Izin>>> = _listState.asStateFlow()

    private val _submitState = MutableStateFlow<UiState<IzinResult>>(UiState.Idle)
    val submitState: StateFlow<UiState<IzinResult>> = _submitState.asStateFlow()

    private val _approvalState = MutableStateFlow<UiState<Izin>>(UiState.Idle)
    val approvalState: StateFlow<UiState<Izin>> = _approvalState.asStateFlow()

    /** Muat daftar izin (miliknya untuk karyawan, semua untuk admin). */
    fun loadList() {
        viewModelScope.launch {
            _listState.value = UiState.Loading
            when (val r = izinRepository.getList()) {
                is ApiResult.Success -> _listState.value = UiState.Success(r.data)
                is ApiResult.Failure -> _listState.value = UiState.Error(r.message)
            }
        }
    }

    /**
     * Ajukan izin baru. fileUri opsional (boleh null bila tanpa lampiran).
     * Setelah sukses, daftar dimuat ulang agar pengajuan baru langsung tampil.
     */
    fun submit(
        jenisIzin: String,
        tanggalMulai: String,
        tanggalSelesai: String,
        keterangan: String,
        fileUri: Uri?
    ) {
        // Validasi ringan sebelum menembak server.
        if (jenisIzin.isBlank() || tanggalMulai.isBlank() || tanggalSelesai.isBlank()) {
            _submitState.value = UiState.Error("Jenis izin dan tanggal wajib diisi.")
            return
        }

        viewModelScope.launch {
            _submitState.value = UiState.Loading
            val r = izinRepository.submit(jenisIzin, tanggalMulai, tanggalSelesai, keterangan, fileUri)
            when (r) {
                is ApiResult.Success -> {
                    _submitState.value = UiState.Success(r.data)
                    loadList() // segarkan daftar
                }
                is ApiResult.Failure -> _submitState.value = UiState.Error(r.message)
            }
        }
    }

    /**
     * Keputusan admin atas sebuah izin. Setelah tersimpan, daftar dimuat
     * ulang supaya status baru (disetujui/ditolak) tercermin di layar.
     */
    fun approval(id: Int, status: String, catatan: String?) {
        viewModelScope.launch {
            _approvalState.value = UiState.Loading
            when (val r = izinRepository.approval(id, status, catatan)) {
                is ApiResult.Success -> {
                    _approvalState.value = UiState.Success(r.data)
                    loadList()
                }
                is ApiResult.Failure -> _approvalState.value = UiState.Error(r.message)
            }
        }
    }

    /** Reset hasil submit agar dialog/pesan tidak muncul berulang. */
    fun resetSubmitState() {
        _submitState.value = UiState.Idle
    }

    /** Reset hasil approval setelah UI menanggapinya. */
    fun resetApprovalState() {
        _approvalState.value = UiState.Idle
    }
}
