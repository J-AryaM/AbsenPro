package com.julian.absenpro.ui.screen.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.julian.absenpro.ui.viewmodel.AuthViewModel
import com.julian.absenpro.util.UiState

/**
 * Layar Login. Mengamati loginState dari AuthViewModel:
 *  - Loading  -> tombol berubah jadi indikator (mencegah klik ganda).
 *  - Success  -> memanggil onLoginSuccess(role) agar host (NavGraph)
 *    mengarahkan ke area admin/karyawan sesuai peran.
 *  - Error    -> menampilkan pesan di bawah form.
 *
 * View ini TIDAK memanggil API langsung; semua lewat viewModel.login().
 */
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: (role: String) -> Unit,
    onNavigateToRegister: () -> Unit
) {
    // State input lokal layar (bukan urusan ViewModel) -> remember biasa.
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Amati state login. collectAsState mengubah StateFlow jadi State
    // Compose sehingga layar otomatis rekomposisi saat state berubah.
    val loginState by viewModel.loginState.collectAsState()

    // Saat state menjadi Success, jalankan navigasi SEKALI sebagai efek
    // samping (bukan langsung di badan komposisi, yang bisa terpanggil
    // berkali-kali). LaunchedEffect di-key pada loginState sehingga blok
    // ini hanya berjalan ketika state benar-benar berubah.
    LaunchedEffect(loginState) {
        if (loginState is UiState.Success) {
            val data = (loginState as UiState.Success).data
            viewModel.resetLoginState() // cegah trigger ulang saat kembali
            onLoginSuccess(data.role)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "AbsenPro",
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            text = "Masuk untuk melanjutkan",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            // Sembunyikan karakter password demi keamanan tampilan.
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))

        // Tombol login berubah menjadi spinner saat Loading.
        if (loginState is UiState.Loading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = { viewModel.login(username, password) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Login")
            }
        }

        // Tampilkan pesan error di bawah tombol bila ada.
        if (loginState is UiState.Error) {
            Text(
                text = (loginState as UiState.Error).message,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )
        }

        // Tautan ke registrasi mandiri (self-register).
        TextButton(
            onClick = onNavigateToRegister,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Belum punya akun? Daftar di sini")
        }
    }
}
