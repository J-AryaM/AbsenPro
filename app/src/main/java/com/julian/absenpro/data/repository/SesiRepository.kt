package com.julian.absenpro.data.repository

import com.julian.absenpro.data.model.CreateSesiRequest
import com.julian.absenpro.data.model.SesiAbsen
import com.julian.absenpro.data.model.SesiAbsenStatus
import com.julian.absenpro.data.model.UpdateSesiStatusRequest
import com.julian.absenpro.data.remote.ApiService

/**
 * Repository sesi absen. Karyawan memakai getSesi() untuk tahu sesi apa
 * yang sedang aktif hari ini; admin memakai create/updateStatus untuk
 * membuka-menutup jendela absen masuk & pulang (F-11).
 */
class SesiRepository(private val api: ApiService) : BaseRepository() {

    /**
     * Ambil daftar sesi pada tanggal tertentu. Bila tanggal null, server
     * memakai hari ini. Mengembalikan list (bisa berisi 'masuk' & 'pulang').
     */
    suspend fun getSesi(tanggal: String? = null): ApiResult<List<SesiAbsen>> {
        return safeCall { api.getSesiAbsen(tanggal) }
    }

    /** Buat atau timpa sesi (admin). tipe+tanggal jadi kunci upsert server. */
    suspend fun createSesi(request: CreateSesiRequest): ApiResult<SesiAbsen> {
        return safeCall { api.createSesiAbsen(request) }
    }

    /** Buka/tutup sesi tertentu dengan mengubah statusnya (admin). */
    suspend fun updateStatus(id: Int, status: String): ApiResult<SesiAbsenStatus> {
        return safeCall { api.updateSesiStatus(UpdateSesiStatusRequest(id, status)) }
    }
}
