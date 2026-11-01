package com.julian.absenpro.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.julian.absenpro.data.model.ApprovalIzinRequest
import com.julian.absenpro.data.model.Izin
import com.julian.absenpro.data.model.IzinResult
import com.julian.absenpro.data.remote.ApiService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

/**
 * Repository izin. Bagian tersulit adalah submit() yang mengirim MULTIPART
 * (field teks + file). File dipilih pengguna sebagai content Uri (dari
 * galeri/penyimpanan), yang tidak bisa langsung dibaca sebagai path biasa,
 * sehingga isinya disalin dulu ke file sementara lalu diunggah.
 *
 * Butuh Context untuk membaca stream Uri -> disuntikkan lewat konstruktor.
 */
class IzinRepository(
    private val api: ApiService,
    private val context: Context
) : BaseRepository() {

    /**
     * Ambil daftar izin. Server otomatis memfilter berdasarkan peran token
     * (admin: semua, karyawan: miliknya), jadi tidak ada parameter di sini.
     */
    suspend fun getList(): ApiResult<List<Izin>> {
        return safeCall { api.getIzinList() }
    }

    /**
     * Ajukan izin. Field teks dibungkus RequestBody, file (opsional)
     * dibungkus MultipartBody.Part. Bila fileUri null, part file dikirim
     * null -> server memperlakukannya sebagai izin tanpa lampiran.
     *
     * @param fileUri Uri file bukti dari picker; boleh null.
     */
    suspend fun submit(
        jenisIzin: String,
        tanggalMulai: String,
        tanggalSelesai: String,
        keterangan: String,
        fileUri: Uri?
    ): ApiResult<IzinResult> {
        // Bungkus tiap field teks. "text/plain" adalah tipe wajar untuk
        // nilai form biasa; server membacanya via $_POST.
        val textType = "text/plain".toMediaTypeOrNull()
        val jenisPart = jenisIzin.toRequestBody(textType)
        val mulaiPart = tanggalMulai.toRequestBody(textType)
        val selesaiPart = tanggalSelesai.toRequestBody(textType)
        val ketPart = keterangan.toRequestBody(textType)

        // Siapkan part file bila ada. Jika gagal menyiapkan file, kembalikan
        // Failure yang jelas alih-alih mengirim data rusak.
        val filePart: MultipartBody.Part? = if (fileUri != null) {
            try {
                buildFilePart(fileUri)
            } catch (e: Exception) {
                return ApiResult.Failure("Gagal membaca file bukti: ${e.message}")
            }
        } else null

        return safeCall {
            api.submitIzin(jenisPart, mulaiPart, selesaiPart, ketPart, filePart)
        }
    }

    /** Approve/tolak izin (admin), meneruskan catatan bila diisi. */
    suspend fun approval(id: Int, status: String, catatan: String?): ApiResult<Izin> {
        return safeCall { api.approvalIzin(ApprovalIzinRequest(id, status, catatan)) }
    }

    /**
     * Salin isi content Uri ke file sementara lalu bungkus jadi part
     * bernama "file_bukti" (harus sama dengan yang dibaca server di
     * $_FILES['file_bukti']). Menyalin dulu diperlukan karena OkHttp butuh
     * File/stream nyata, sedangkan Uri galeri belum tentu berupa file lokal.
     */
    private fun buildFilePart(uri: Uri): MultipartBody.Part {
        val resolver = context.contentResolver

        // Tentukan nama & tipe file dari metadata Uri (untuk header multipart).
        val fileName = queryFileName(uri) ?: "bukti_${System.currentTimeMillis()}"
        val mime = resolver.getType(uri) ?: "application/octet-stream"

        // Salin stream Uri -> file cache aplikasi.
        val tempFile = File(context.cacheDir, fileName)
        resolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Tidak bisa membuka file yang dipilih.")

        // Bungkus file sebagai body dengan tipe media aslinya.
        val requestFile: RequestBody = tempFile.asRequestBody(mime.toMediaTypeOrNull())

        // "file_bukti" = nama field yang diharapkan server.
        return MultipartBody.Part.createFormData("file_bukti", fileName, requestFile)
    }

    /**
     * Ambil nama asli file dari kolom DISPLAY_NAME milik content resolver,
     * agar file di server memakai ekstensi yang benar (mis. .jpg/.pdf).
     */
    private fun queryFileName(uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return it.getString(idx)
            }
        }
        return null
    }
}
