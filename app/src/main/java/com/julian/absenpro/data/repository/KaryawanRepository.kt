package com.julian.absenpro.data.repository

import com.julian.absenpro.data.model.Karyawan
import com.julian.absenpro.data.model.UpdateKaryawanRequest
import com.julian.absenpro.data.remote.ApiService

/**
 * Repository pengelolaan karyawan (khusus fitur admin). Mengarahkan ke
 * endpoint karyawan/index.php:
 *  - GET untuk mengambil daftar semua karyawan (F-09).
 *  - PUT untuk mengubah data / mengaktif-nonaktifkan akun.
 * Error dari API diteruskan apa adanya sebagai ApiResult.Failure agar
 * ViewModel bisa menampilkannya ke admin.
 */
class KaryawanRepository(private val api: ApiService) : BaseRepository() {

    /** Ambil daftar karyawan untuk ditampilkan di layar kelola karyawan. */
    suspend fun getList(): ApiResult<List<Karyawan>> {
        return safeCall { api.getKaryawanList() }
    }

    /**
     * Perbarui data / status karyawan. Field yang null pada request tidak
     * dikirim (partial update), jadi pemanggil cukup mengisi yang berubah.
     */
    suspend fun update(request: UpdateKaryawanRequest): ApiResult<Karyawan> {
        return safeCall { api.updateKaryawan(request) }
    }
}
