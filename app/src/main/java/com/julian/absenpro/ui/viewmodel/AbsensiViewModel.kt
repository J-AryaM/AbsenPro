package com.julian.absenpro.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.julian.absenpro.data.model.AbsensiResult
import com.julian.absenpro.data.model.LokasiKantor
import com.julian.absenpro.data.model.RiwayatAbsensi
import com.julian.absenpro.data.model.SesiAbsen
import com.julian.absenpro.data.repository.AbsensiRepository
import com.julian.absenpro.data.repository.ApiResult
import com.julian.absenpro.data.repository.LokasiRepository
import com.julian.absenpro.data.repository.SesiRepository
import com.julian.absenpro.util.LocationUtils
import com.julian.absenpro.util.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel untuk fitur absensi karyawan (absen masuk/pulang + riwayat).
 *
 * Menggabungkan tiga sumber data karena satu layar absen membutuhkan
 * semuanya: lokasi kantor (titik acuan), sesi hari ini (boleh absen atau
 * tidak), dan aksi submit itu sendiri.
 *
 * Ringkasan alur state:
 *  - lokasiState / sesiState : dimuat saat layar dibuka (loadAbsenScreen).
 *  - absenState  : Idle -> Loading (saat menekan absen; termasuk menunggu
 *    GPS) -> Success(AbsensiResult) atau Error. Layar menampilkan hasil
 *    hadir/telat atau alasan penolakan dari server.
 *  - riwayatState: daftar riwayat untuk layar histori.
 */
class AbsensiViewModel(
    private val absensiRepository: AbsensiRepository,
    private val lokasiRepository: LokasiRepository,
    private val sesiRepository: SesiRepository
) : ViewModel() {

    // Lokasi kantor acuan (untuk menampilkan jarak/pratinjau di UI).
    private val _lokasiState = MutableStateFlow<UiState<LokasiKantor>>(UiState.Idle)
    val lokasiState: StateFlow<UiState<LokasiKantor>> = _lokasiState.asStateFlow()

    // Daftar sesi hari ini (berisi 'masuk' dan/atau 'pulang').
    private val _sesiState = MutableStateFlow<UiState<List<SesiAbsen>>>(UiState.Idle)
    val sesiState: StateFlow<UiState<List<SesiAbsen>>> = _sesiState.asStateFlow()

    // Hasil aksi absen (dipakai untuk menampilkan konfirmasi/ error).
    private val _absenState = MutableStateFlow<UiState<AbsensiResult>>(UiState.Idle)
    val absenState: StateFlow<UiState<AbsensiResult>> = _absenState.asStateFlow()

    // Riwayat absensi pribadi.
    private val _riwayatState = MutableStateFlow<UiState<List<RiwayatAbsensi>>>(UiState.Idle)
    val riwayatState: StateFlow<UiState<List<RiwayatAbsensi>>> = _riwayatState.asStateFlow()

    /**
     * Muat data yang dibutuhkan layar absen: lokasi kantor + sesi hari ini.
     * Dipanggil saat layar pertama tampil (LaunchedEffect).
     */
    fun loadAbsenScreen() {
        loadLokasi()
        loadSesiHariIni()
    }

    /** Ambil lokasi kantor acuan. */
    fun loadLokasi() {
        viewModelScope.launch {
            _lokasiState.value = UiState.Loading
            when (val r = lokasiRepository.getLokasi()) {
                is ApiResult.Success -> _lokasiState.value = UiState.Success(r.data)
                is ApiResult.Failure -> _lokasiState.value = UiState.Error(r.message)
            }
        }
    }

    /** Ambil sesi absen untuk hari ini (server default = hari ini). */
    fun loadSesiHariIni() {
        viewModelScope.launch {
            _sesiState.value = UiState.Loading
            when (val r = sesiRepository.getSesi()) {
                is ApiResult.Success -> _sesiState.value = UiState.Success(r.data)
                is ApiResult.Failure -> _sesiState.value = UiState.Error(r.message)
            }
        }
    }

    /**
     * Lakukan absen. Urutan:
     *  1) set Loading (UI menampilkan "mengambil lokasi...").
     *  2) ambil GPS lewat LocationUtils (suspend).
     *  3) bila lokasi null -> Error (GPS mati/izin belum siap).
     *  4) kirim koordinat ke server; server yang memutuskan lolos/tidak.
     *
     * Context diminta sebagai parameter (bukan disimpan) agar ViewModel
     * tidak menahan referensi Context -> tetap patuh aturan MVVM.
     *
     * @param context dipakai sekejap untuk FusedLocationProvider.
     * @param tipe    "masuk" atau "pulang".
     */
    fun absen(context: Context, tipe: String) {
        viewModelScope.launch {
            _absenState.value = UiState.Loading

            // Ambil lokasi terkini. Ini bisa memakan waktu -> sudah dalam Loading.
            val location = LocationUtils.getCurrentLocation(context)
            if (location == null) {
                _absenState.value = UiState.Error(
                    "Gagal mendapatkan lokasi. Pastikan GPS aktif & izin lokasi diberikan."
                )
                return@launch
            }

            // Kirim ke server. Validasi radius/jendela waktu terjadi di sana.
            when (val r = absensiRepository.submit(tipe, location.latitude, location.longitude)) {
                is ApiResult.Success -> {
                    _absenState.value = UiState.Success(r.data)
                    // Segarkan riwayat & sesi agar UI mencerminkan absen baru.
                    loadRiwayat()
                }
                is ApiResult.Failure -> _absenState.value = UiState.Error(r.message)
            }
        }
    }

    /** Muat riwayat absensi pribadi untuk layar histori. */
    fun loadRiwayat() {
        viewModelScope.launch {
            _riwayatState.value = UiState.Loading
            when (val r = absensiRepository.getRiwayat()) {
                is ApiResult.Success -> _riwayatState.value = UiState.Success(r.data)
                is ApiResult.Failure -> _riwayatState.value = UiState.Error(r.message)
            }
        }
    }

    /** Reset hasil absen setelah UI menampilkannya (agar tidak muncul ulang). */
    fun resetAbsenState() {
        _absenState.value = UiState.Idle
    }
}
