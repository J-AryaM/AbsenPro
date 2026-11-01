package com.julian.absenpro.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.julian.absenpro.util.Constants

/**
 * Penyimpan sesi login (token + identitas) secara AMAN di perangkat.
 *
 * Kenapa EncryptedSharedPreferences, bukan SharedPreferences biasa?
 *  Token adalah "kunci" akses ke akun. Bila disimpan sebagai teks biasa,
 *  aplikasi lain yang punya akses root / backup bisa membacanya. PRD
 *  bagian 10 mewajibkan data sensitif dienkripsi. Di sini isi file
 *  preferensi dienkripsi dengan kunci yang dikelola Android Keystore,
 *  sehingga yang tersimpan di disk berupa ciphertext, bukan token asli.
 *
 * Dibuat sebagai kelas biasa (bukan object) agar butuh Context untuk
 * membuat file terenkripsi; instansinya dibuat sekali lalu dibagikan.
 */
class SessionManager(context: Context) {

    // Kunci utama (master key) untuk mengenkripsi/dekripsi isi preferensi.
    // AES256_GCM adalah skema standar yang direkomendasikan library ini.
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    // Buat/instansiasi file preferensi terenkripsi. Nama key & value pun
    // ikut dienkripsi (AES256_SIV untuk key, AES256_GCM untuk value).
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        Constants.SECURE_PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * Simpan seluruh data sesi setelah login sukses. Dipanggil oleh
     * AuthRepository/ViewModel begitu server mengembalikan token.
     */
    fun saveSession(token: String, role: String, userId: Int, karyawanId: Int?, nama: String) {
        prefs.edit().apply {
            putString(KEY_TOKEN, token)
            putString(KEY_ROLE, role)
            putInt(KEY_USER_ID, userId)
            // karyawanId bisa null (admin) -> simpan -1 sebagai penanda "tidak ada".
            putInt(KEY_KARYAWAN_ID, karyawanId ?: -1)
            putString(KEY_NAMA, nama)
            apply() // tulis secara asynchronous
        }
    }

    /** Token mentah untuk header Authorization; null bila belum login. */
    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    /** Peran user aktif ("admin"/"karyawan") untuk menentukan navigasi. */
    fun getRole(): String? = prefs.getString(KEY_ROLE, null)

    /** id karyawan aktif; null bila admin atau belum login. */
    fun getKaryawanId(): Int? {
        val id = prefs.getInt(KEY_KARYAWAN_ID, -1)
        return if (id == -1) null else id
    }

    /** Nama yang ditampilkan di layar (mis. sapaan di beranda). */
    fun getNama(): String? = prefs.getString(KEY_NAMA, null)

    /**
     * Cek apakah ada sesi tersimpan. Dipakai saat app dibuka untuk
     * memutuskan langsung ke beranda atau ke layar login.
     */
    fun isLoggedIn(): Boolean = getToken() != null

    /**
     * Hapus seluruh sesi (saat logout atau token ditolak server).
     * Setelah ini isLoggedIn() kembali false.
     */
    fun clear() {
        prefs.edit().clear().apply()
    }

    // Nama-nama key disimpan sebagai konstanta agar konsisten & bebas typo.
    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_ROLE = "role"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_KARYAWAN_ID = "karyawan_id"
        private const val KEY_NAMA = "nama"
    }
}
