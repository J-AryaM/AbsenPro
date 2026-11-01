package com.julian.absenpro.data.repository

import com.julian.absenpro.data.local.SessionManager
import com.julian.absenpro.data.model.LoginData
import com.julian.absenpro.data.model.LoginRequest
import com.julian.absenpro.data.model.RegisterData
import com.julian.absenpro.data.model.RegisterRequest
import com.julian.absenpro.data.remote.ApiService

/**
 * Repository autentikasi: menjembatani ViewModel dengan endpoint auth.
 * Juga bertanggung jawab MENYIMPAN sesi ke SessionManager saat login
 * sukses, sehingga ViewModel tidak perlu tahu detail penyimpanan aman.
 *
 * Alasan pemetaan endpoint:
 *  - login  -> auth/login.php   (menukar kredensial dengan token)
 *  - register-> auth/register.php (membuat akun karyawan baru)
 *  - logout -> auth/logout.php  (menghapus token di server)
 */
class AuthRepository(
    private val api: ApiService,
    private val session: SessionManager
) : BaseRepository() {

    /**
     * Login. Bila sukses, token & identitas langsung disimpan terenkripsi
     * agar request berikutnya otomatis membawa Authorization header.
     *
     * @return ApiResult<LoginData> -> ViewModel memakai data.role untuk
     *         memutuskan tujuan navigasi (admin vs karyawan).
     */
    suspend fun login(username: String, password: String): ApiResult<LoginData> {
        val result = safeCall { api.login(LoginRequest(username, password)) }
        // Simpan sesi HANYA jika benar-benar sukses; kegagalan tidak
        // menyentuh penyimpanan sehingga sesi lama tidak ikut rusak.
        if (result is ApiResult.Success) {
            val d = result.data
            session.saveSession(d.token, d.role, d.userId, d.karyawanId, d.nama)
        }
        return result
    }

    /**
     * Registrasi mandiri karyawan. Tidak menyentuh sesi karena setelah
     * register pengguna diminta login (server tidak mengembalikan token).
     */
    suspend fun register(
        nama: String,
        username: String,
        password: String,
        jabatan: String?,
        kontak: String?
    ): ApiResult<RegisterData> {
        return safeCall {
            api.register(RegisterRequest(nama, username, password, jabatan, kontak))
        }
    }

    /**
     * Logout. Panggil server untuk menghapus token, LALU bersihkan sesi
     * lokal apa pun hasilnya — supaya walau server tak terjangkau,
     * pengguna tetap keluar dari sisi aplikasi.
     */
    suspend fun logout(): ApiResult<Unit> {
        val result = safeCallNoData { api.logout() }
        session.clear()
        return result
    }
}
