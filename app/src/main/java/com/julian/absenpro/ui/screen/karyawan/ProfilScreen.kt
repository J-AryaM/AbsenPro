package com.julian.absenpro.ui.screen.karyawan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Layar Profil karyawan (PRD F-07). Menampilkan data dasar yang tersimpan
 * di sesi (nama & role) dan menyediakan tombol Logout.
 *
 * Data profil di sini diambil dari sesi lokal (bukan endpoint terpisah)
 * karena PRD hanya meminta "melihat data profil dasar", dan nama sudah
 * tersimpan saat login. Ini menghindari panggilan jaringan tak perlu.
 */
@Composable
fun ProfilScreen(
    nama: String,
    role: String,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Profil",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Nama", fontWeight = FontWeight.Bold)
                Text(nama)
                Spacer(Modifier.height(8.dp))
                Text("Peran", fontWeight = FontWeight.Bold)
                Text(role)
            }
        }

        Spacer(Modifier.height(24.dp))

        // Logout: memicu penghapusan sesi (di host) lalu kembali ke login.
        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Logout")
        }
    }
}
