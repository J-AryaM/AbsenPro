package com.julian.absenpro.data.remote

import com.julian.absenpro.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Kontrak jaringan aplikasi: satu fungsi per endpoint backend. Retrofit
 * membuat implementasinya secara otomatis dari anotasi di sini.
 *
 * Catatan pola:
 *  - Semua fungsi 'suspend' agar dipanggil dari coroutine (di Repository),
 *    sehingga jaringan tidak memblokir main thread.
 *  - Pengembaliannya dibungkus Response<...> supaya Repository bisa
 *    membaca kode HTTP (mis. membedakan 401 vs 409) sekaligus body-nya.
 *  - Path relatif (tanpa '/' di depan) agar digabung dengan BASE_URL yang
 *    sudah mengandung folder 'absenpro_api/'.
 */
interface ApiService {

    // ----------------------------- AUTH -----------------------------

    // Register mengirim JSON -> @Body. Tidak butuh token.
    @POST("auth/register.php")
    suspend fun register(@Body body: RegisterRequest): Response<ApiResponse<RegisterData>>

    // Login mengirim JSON -> @Body. Mengembalikan token + role.
    @POST("auth/login.php")
    suspend fun login(@Body body: LoginRequest): Response<ApiResponse<LoginData>>

    // Logout butuh token; header di-inject otomatis oleh AuthInterceptor,
    // jadi tidak perlu parameter @Header di sini.
    @POST("auth/logout.php")
    suspend fun logout(): Response<ApiResponse<Unit>>

    // --------------------------- KARYAWAN ---------------------------

    // Daftar karyawan (admin). Data berupa list.
    @GET("karyawan/index.php")
    suspend fun getKaryawanList(): Response<ApiResponse<List<Karyawan>>>

    // Update/nonaktifkan karyawan (admin).
    @PUT("karyawan/index.php")
    suspend fun updateKaryawan(@Body body: UpdateKaryawanRequest): Response<ApiResponse<Karyawan>>

    // ------------------------- LOKASI KANTOR -------------------------

    // Ambil lokasi kantor (semua user login).
    @GET("lokasi_kantor/index.php")
    suspend fun getLokasiKantor(): Response<ApiResponse<LokasiKantor>>

    // Ubah lokasi kantor (admin).
    @PUT("lokasi_kantor/index.php")
    suspend fun updateLokasiKantor(@Body body: UpdateLokasiRequest): Response<ApiResponse<LokasiKantor>>

    // -------------------------- SESI ABSEN --------------------------

    // Lihat sesi pada tanggal tertentu (default server = hari ini).
    // tanggal dikirim sebagai query opsional.
    @GET("sesi_absen/index.php")
    suspend fun getSesiAbsen(@Query("tanggal") tanggal: String? = null): Response<ApiResponse<List<SesiAbsen>>>

    // Buat/timpa sesi (admin).
    @POST("sesi_absen/index.php")
    suspend fun createSesiAbsen(@Body body: CreateSesiRequest): Response<ApiResponse<SesiAbsen>>

    // Ubah status buka/tutup sesi (admin).
    @PUT("sesi_absen/index.php")
    suspend fun updateSesiStatus(@Body body: UpdateSesiStatusRequest): Response<ApiResponse<SesiAbsenStatus>>

    // ---------------------------- ABSENSI ----------------------------

    // Submit absen (karyawan). Server validasi radius + jendela waktu.
    @POST("absensi/index.php")
    suspend fun submitAbsensi(@Body body: AbsensiRequest): Response<ApiResponse<AbsensiResult>>

    // Riwayat absensi pribadi (karyawan).
    @GET("absensi/index.php")
    suspend fun getRiwayatAbsensi(@Query("limit") limit: Int? = null): Response<ApiResponse<List<RiwayatAbsensi>>>

    // Rekap semua karyawan dengan filter (admin). Semua query opsional.
    @GET("absensi/rekap.php")
    suspend fun getRekapAbsensi(
        @Query("tanggal_mulai") tanggalMulai: String? = null,
        @Query("tanggal_selesai") tanggalSelesai: String? = null,
        @Query("karyawan_id") karyawanId: Int? = null,
        @Query("status") status: String? = null,
        @Query("tipe") tipe: String? = null
    ): Response<ApiResponse<List<RekapAbsensi>>>

    // ------------------------------ IZIN ------------------------------

    // Daftar izin (admin: semua, karyawan: miliknya) -> server yang memfilter.
    @GET("izin/index.php")
    suspend fun getIzinList(): Response<ApiResponse<List<Izin>>>

    // Ajukan izin (karyawan) dengan file bukti opsional. Memakai multipart
    // karena membawa berkas. Tiap field teks jadi @Part terpisah, dan
    // file sebagai @Part MultipartBody.Part (boleh null bila tanpa lampiran).
    @Multipart
    @POST("izin/index.php")
    suspend fun submitIzin(
        @Part("jenis_izin") jenisIzin: RequestBody,
        @Part("tanggal_mulai") tanggalMulai: RequestBody,
        @Part("tanggal_selesai") tanggalSelesai: RequestBody,
        @Part("keterangan") keterangan: RequestBody,
        @Part fileBukti: MultipartBody.Part?
    ): Response<ApiResponse<IzinResult>>

    // Approve/tolak izin (admin).
    @PUT("izin/approval.php")
    suspend fun approvalIzin(@Body body: ApprovalIzinRequest): Response<ApiResponse<Izin>>
}
