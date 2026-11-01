package com.julian.absenpro.data.repository

import com.julian.absenpro.data.model.LokasiKantor
import com.julian.absenpro.data.model.UpdateLokasiRequest
import com.julian.absenpro.data.remote.ApiService

/**
 * Repository lokasi kantor. Dipakai oleh DUA peran:
 *  - Karyawan: getLokasi() untuk tahu titik & radius kantor (menampilkan
 *    jarak sebelum absen).
 *  - Admin: updateLokasi() untuk mengubah koordinat/radius (F-10).
 * Endpoint sama (lokasi_kantor/index.php), dibedakan method GET vs PUT.
 */
class LokasiRepository(private val api: ApiService) : BaseRepository() {

    /** Ambil lokasi kantor aktif dari server. */
    suspend fun getLokasi(): ApiResult<LokasiKantor> {
        return safeCall { api.getLokasiKantor() }
    }

    /** Ubah koordinat & radius kantor (server memvalidasi rentang nilainya). */
    suspend fun updateLokasi(
        latitude: Double,
        longitude: Double,
        radiusMeter: Int,
        namaKantor: String
    ): ApiResult<LokasiKantor> {
        return safeCall {
            api.updateLokasiKantor(
                UpdateLokasiRequest(latitude, longitude, radiusMeter, namaKantor)
            )
        }
    }
}
