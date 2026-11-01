package com.julian.absenpro.ui.screen.admin

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.julian.absenpro.ui.viewmodel.LokasiViewModel
import com.julian.absenpro.ui.viewmodel.ViewModelFactory
import com.julian.absenpro.util.UiState

/**
 * Layar Atur Lokasi Kantor (PRD F-10). Memuat nilai koordinat & radius saat
 * ini untuk mengisi form, lalu menyimpan perubahan lewat LokasiViewModel.
 *
 * Karena field lokasiState dan input form berbeda sumber, form diisi
 * SEKALI dari data server saat pertama dimuat (agar tidak menimpa ketikan
 * admin setiap kali state berubah).
 */
@Composable
fun AturLokasiScreen(onBack: () -> Unit) {
    val vm: LokasiViewModel = viewModel(factory = ViewModelFactory)

    LaunchedEffect(Unit) { vm.loadLokasi() }

    val lokasiState by vm.lokasiState.collectAsState()
    val saveState by vm.saveState.collectAsState()

    // Field form. Diisi dari data server saat pertama kali tersedia.
    var nama by remember { mutableStateOf("") }
    var lat by remember { mutableStateOf("") }
    var lon by remember { mutableStateOf("") }
    var radius by remember { mutableStateOf("") }
    // Penanda agar prefill hanya sekali (tidak menimpa input admin).
    var sudahPrefill by remember { mutableStateOf(false) }

    // Isi form ketika data lokasi berhasil dimuat pertama kali.
    LaunchedEffect(lokasiState) {
        val s = lokasiState
        if (!sudahPrefill && s is UiState.Success) {
            nama = s.data.namaKantor
            lat = s.data.latitude.toString()
            lon = s.data.longitude.toString()
            radius = s.data.radiusMeter.toString()
            sudahPrefill = true
        }
    }

    AdminScaffold(title = "Atur Lokasi Kantor", onBack = onBack) { modifier ->
        Column(
            modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = nama,
                onValueChange = { nama = it },
                label = { Text("Nama Kantor") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = lat,
                onValueChange = { lat = it },
                label = { Text("Latitude (mis. -6.1753924)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = lon,
                onValueChange = { lon = it },
                label = { Text("Longitude (mis. 106.8271528)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = radius,
                onValueChange = { radius = it },
                label = { Text("Radius Toleransi (meter)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            if (saveState is UiState.Loading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = { vm.simpan(lat, lon, radius, nama) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Simpan Lokasi")
                }
            }

            // Umpan balik hasil simpan.
            when (val s = saveState) {
                is UiState.Success -> Text(
                    "Lokasi berhasil disimpan.",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
                is UiState.Error -> Text(
                    s.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
                else -> {}
            }
        }
    }
}
