package com.julian.absenpro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.julian.absenpro.data.model.Karyawan
import com.julian.absenpro.data.model.RekapAbsensi
import com.julian.absenpro.data.repository.AbsensiRepository
import com.julian.absenpro.data.repository.ApiResult
import com.julian.absenpro.data.repository.KaryawanRepository
import com.julian.absenpro.util.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel rekap absensi (admin). Menampilkan rekap terfilter dan
 * menyediakan daftar karyawan untuk dropdown filter. Juga menyimpan filter
 * aktif agar bisa dipakai membangun URL export CSV yang konsisten.
 *
 * Alur state:
 *  - rekapState   : hasil rekap terfilter.
 *  - karyawanState: daftar karyawan untuk pilihan filter "per karyawan".
 */
class RekapViewModel(
    private val absensiRepository: AbsensiRepository,
    private val karyawanRepository: KaryawanRepository
) : ViewModel() {

    private val _rekapState = MutableStateFlow<UiState<List<RekapAbsensi>>>(UiState.Idle)
    val rekapState: StateFlow<UiState<List<RekapAbsensi>>> = _rekapState.asStateFlow()

    private val _karyawanState = MutableStateFlow<UiState<List<Karyawan>>>(UiState.Idle)
    val karyawanState: StateFlow<UiState<List<Karyawan>>> = _karyawanState.asStateFlow()

    // Simpan filter terakhir yang dipakai supaya tombol "Export CSV" bisa
    // membangun query yang sama persis dengan rekap yang sedang tampil.
    var lastFilter: RekapFilter = RekapFilter()
        private set

    /** Muat daftar karyawan untuk mengisi dropdown filter. */
    fun loadKaryawan() {
        viewModelScope.launch {
            _karyawanState.value = UiState.Loading
            when (val r = karyawanRepository.getList()) {
                is ApiResult.Success -> _karyawanState.value = UiState.Success(r.data)
                is ApiResult.Failure -> _karyawanState.value = UiState.Error(r.message)
            }
        }
    }

    /**
     * Muat rekap sesuai filter. Nilai kosong ("") diperlakukan sebagai
     * "tidak difilter" -> dikirim null ke repository.
     */
    fun loadRekap(filter: RekapFilter) {
        lastFilter = filter
        viewModelScope.launch {
            _rekapState.value = UiState.Loading
            val r = absensiRepository.getRekap(
                tanggalMulai = filter.tanggalMulai.ifBlank { null },
                tanggalSelesai = filter.tanggalSelesai.ifBlank { null },
                karyawanId = filter.karyawanId,
                status = filter.status.ifBlank { null },
                tipe = filter.tipe.ifBlank { null }
            )
            when (r) {
                is ApiResult.Success -> _rekapState.value = UiState.Success(r.data)
                is ApiResult.Failure -> _rekapState.value = UiState.Error(r.message)
            }
        }
    }
}

/**
 * Wadah nilai filter rekap. Dipakai bersama antara UI (form filter) dan
 * pembentukan URL export CSV agar keduanya memakai kriteria yang sama.
 * String kosong = filter tidak aktif; karyawanId null = semua karyawan.
 */
data class RekapFilter(
    val tanggalMulai: String = "",
    val tanggalSelesai: String = "",
    val karyawanId: Int? = null,
    val status: String = "",
    val tipe: String = ""
)
