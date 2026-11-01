package com.julian.absenpro.util

/**
 * Pembungkus status UI generik untuk pola MVVM. Setiap layar yang memuat
 * data dari jaringan biasanya melewati tiga kondisi ini, dan Composable
 * cukup melakukan 'when' terhadap tipe untuk menentukan apa yang digambar.
 *
 * Memakai sealed class agar 'when' bisa exhaustive (compiler memastikan
 * semua kondisi ditangani), sehingga tidak ada state yang lupa di-handle.
 *
 * @param T tipe data yang dibawa saat sukses (mis. List<Karyawan>).
 */
sealed class UiState<out T> {

    /** Kondisi awal sebelum aksi apa pun dilakukan (form kosong, dsb). */
    data object Idle : UiState<Nothing>()

    /** Sedang menunggu respons jaringan -> UI menampilkan indikator loading. */
    data object Loading : UiState<Nothing>()

    /** Berhasil; membawa data hasil -> UI menampilkan kontennya. */
    data class Success<T>(val data: T) : UiState<T>()

    /** Gagal; membawa pesan error -> UI menampilkan pesan tsb ke pengguna. */
    data class Error(val message: String) : UiState<Nothing>()
}
