package com.julian.absenpro.data.repository

import com.julian.absenpro.data.model.AbsensiRequest
import com.julian.absenpro.data.model.AbsensiResult
import com.julian.absenpro.data.model.RekapAbsensi
import com.julian.absenpro.data.model.RiwayatAbsensi
import com.julian.absenpro.data.remote.ApiService

/**
 * Repository absensi. Menghubungkan ke:
 *  - POST absensi/index.php  : submit absen (karyawan). Server memvalidasi
 *    ulang radius & jendela waktu; error apa pun (di luar radius, sesi
 *    ditutup, dobel absen) diteruskan sebagai Failure(pesan server).
 *  - GET  absensi/index.php  : riwayat pribadi (karyawan).
 *  - GET  absensi/rekap.php  : rekap semua karyawan dengan filter (admin).
 */
class AbsensiRepository(private val api: ApiService) : BaseRepository() {

    /**
     * Kirim absen. Koordinat diperoleh dari GPS lewat LocationUtils, lalu
     * diserahkan ke server untuk keputusan akhir hadir/telat/ditolak.
     */
    suspend fun submit(tipe: String, latitude: Double, longitude: Double): ApiResult<AbsensiResult> {
        return safeCall { api.submitAbsensi(AbsensiRequest(tipe, latitude, longitude)) }
    }

    /** Riwayat absensi milik karyawan yang sedang login. */
    suspend fun getRiwayat(limit: Int? = null): ApiResult<List<RiwayatAbsensi>> {
        return safeCall { api.getRiwayatAbsensi(limit) }
    }

    /**
     * Rekap absensi untuk admin. Semua filter opsional; yang null tidak
     * dikirim sebagai query sehingga server mengabaikannya.
     */
    suspend fun getRekap(
        tanggalMulai: String? = null,
        tanggalSelesai: String? = null,
        karyawanId: Int? = null,
        status: String? = null,
        tipe: String? = null
    ): ApiResult<List<RekapAbsensi>> {
        return safeCall {
            api.getRekapAbsensi(tanggalMulai, tanggalSelesai, karyawanId, status, tipe)
        }
    }
}
