package com.julian.absenpro.ui.screen.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Dashboard Admin: menu utama menuju fitur-fitur admin (F-09..F-14).
 * Sengaja dibuat sebagai daftar tombol sederhana agar navigasi jelas dan
 * mudah dipertanggungjawabkan. Tiap tombol memicu callback navigasi yang
 * ditangani NavGraph.
 *
 * @param nama nama admin untuk sapaan.
 * Parameter onXxx = aksi navigasi ke tiap layar admin.
 */
@Composable
fun AdminHomeScreen(
    nama: String,
    onKelolaKaryawan: () -> Unit,
    onAturLokasi: () -> Unit,
    onKontrolSesi: () -> Unit,
    onApprovalIzin: () -> Unit,
    onRekap: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            "Dashboard Admin",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text("Halo, $nama", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))

        // Tiap menu = satu tombol lebar. Urutannya mengikuti alur kerja admin.
        MenuButton("Kelola Karyawan", onKelolaKaryawan)
        MenuButton("Atur Lokasi Kantor", onAturLokasi)
        MenuButton("Kontrol Sesi Absen", onKontrolSesi)
        MenuButton("Approval Izin", onApprovalIzin)
        MenuButton("Rekap & Export CSV", onRekap)

        Spacer(Modifier.height(24.dp))

        // Logout dipisah dengan gaya outlined agar tidak tertukar dengan menu.
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Logout")
        }
    }
}

/** Tombol menu lebar yang seragam untuk dashboard. */
@Composable
private fun MenuButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(text)
    }
}
