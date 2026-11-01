package com.julian.absenpro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.julian.absenpro.data.local.SessionManager
import com.julian.absenpro.data.model.LoginData
import com.julian.absenpro.data.model.RegisterData
import com.julian.absenpro.data.repository.ApiResult
import com.julian.absenpro.data.repository.AuthRepository
import com.julian.absenpro.util.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel untuk autentikasi (login, register, logout).
 *
 * Alur state (pola MVVM):
 *  - loginState : Idle -> Loading (saat tombol login ditekan) -> Success
 *    (membawa LoginData berisi role) atau Error (pesan dari server).
 *    Layar LoginScreen mengamati state ini: menampilkan spinner saat
 *    Loading, dan bernavigasi berdasarkan role saat Success.
 *  - registerState : pola serupa untuk RegisterScreen.
 *
 * ViewModel TIDAK menyentuh Composable/Context Activity (sesuai batasan
 * MVVM); ia hanya memanggil Repository dan mempublikasikan StateFlow.
 */
class AuthViewModel(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    // MutableStateFlow privat = sumber kebenaran yang hanya boleh diubah
    // ViewModel. Versi publik (asStateFlow) read-only untuk UI.
    private val _loginState = MutableStateFlow<UiState<LoginData>>(UiState.Idle)
    val loginState: StateFlow<UiState<LoginData>> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow<UiState<RegisterData>>(UiState.Idle)
    val registerState: StateFlow<UiState<RegisterData>> = _registerState.asStateFlow()

    /**
     * Proses login. Memancarkan Loading dulu agar UI menampilkan indikator,
     * lalu Success/Error sesuai hasil Repository (yang juga menyimpan sesi
     * bila sukses).
     */
    fun login(username: String, password: String) {
        // Validasi ringan di klien agar tidak menembak server dengan input kosong.
        if (username.isBlank() || password.isBlank()) {
            _loginState.value = UiState.Error("Username dan password wajib diisi.")
            return
        }

        viewModelScope.launch {
            _loginState.value = UiState.Loading
            when (val result = authRepository.login(username.trim(), password)) {
                is ApiResult.Success -> _loginState.value = UiState.Success(result.data)
                is ApiResult.Failure -> _loginState.value = UiState.Error(result.message)
            }
        }
    }

    /**
     * Proses registrasi mandiri karyawan. jabatan & kontak opsional -> bila
     * kosong dikirim null supaya server menyimpannya sebagai NULL.
     */
    fun register(
        nama: String,
        username: String,
        password: String,
        jabatan: String,
        kontak: String
    ) {
        if (nama.isBlank() || username.isBlank() || password.isBlank()) {
            _registerState.value = UiState.Error("Nama, username, dan password wajib diisi.")
            return
        }
        if (password.length < 6) {
            _registerState.value = UiState.Error("Password minimal 6 karakter.")
            return
        }

        viewModelScope.launch {
            _registerState.value = UiState.Loading
            val result = authRepository.register(
                nama.trim(),
                username.trim(),
                password,
                jabatan.trim().ifBlank { null },
                kontak.trim().ifBlank { null }
            )
            when (result) {
                is ApiResult.Success -> _registerState.value = UiState.Success(result.data)
                is ApiResult.Failure -> _registerState.value = UiState.Error(result.message)
            }
        }
    }

    /** Logout: hapus sesi (server + lokal). UI lalu kembali ke login. */
    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onDone()
        }
    }

    /** Dipakai UI untuk menentukan layar awal (sudah login atau belum). */
    fun isLoggedIn(): Boolean = sessionManager.isLoggedIn()

    /** Peran user aktif untuk memutuskan grafik navigasi awal. */
    fun currentRole(): String? = sessionManager.getRole()

    /** Reset state login ke Idle setelah UI selesai menanggapi (mis. navigasi). */
    fun resetLoginState() {
        _loginState.value = UiState.Idle
    }

    /** Reset state register ke Idle (mis. setelah pindah ke layar login). */
    fun resetRegisterState() {
        _registerState.value = UiState.Idle
    }
}
