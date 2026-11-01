package com.julian.absenpro.ui.screen.karyawan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.julian.absenpro.data.model.RiwayatAbsensi
import com.julian.absenpro.ui.components.EmptyView
import com.julian.absenpro.ui.components.ErrorView
import com.julian.absenpro.ui.components.LoadingIndicator
import com.julian.absenpro.ui.viewmodel.AbsensiViewModel
import com.julian.absenpro.util.UiState

/**
 * Layar Riwayat Absensi pribadi (PRD F-04). Mengamati riwayatState:
 *  - Loading -> indikator.
 *  - Success -> daftar kartu (kosong -> EmptyView).
 *  - Error   -> pesan + tombol coba lagi.
 */
@Composable
fun RiwayatScreen(viewModel: AbsensiViewModel) {
    // Muat riwayat saat layar tampil.
    LaunchedEffect(Unit) { viewModel.loadRiwayat() }

    val riwayatState by viewModel.riwayatState.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Riwayat Absensi",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        when (val s = riwayatState) {
            is UiState.Loading -> LoadingIndicator()
            is UiState.Error -> ErrorView(s.message, onRetry = { viewModel.loadRiwayat() })
            is UiState.Success -> {
                if (s.data.isEmpty()) {
                    EmptyView("Belum ada riwayat absensi.")
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(s.data) { item -> RiwayatItem(item) }
                    }
                }
            }
            else -> {}
        }
    }
}

/**
 * Satu baris riwayat: tanggal, tipe (masuk/pulang), jam, status, jarak.
 */
@Composable
private fun RiwayatItem(item: RiwayatAbsensi) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "${item.tanggal} • ${item.tipe.uppercase()}",
                fontWeight = FontWeight.Bold
            )
            Text("Jam: ${item.jam}")
            // Warnai status untuk membedakan hadir vs telat secara cepat.
            Text(
                "Status: ${item.status}",
                color = if (item.status == "telat")
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary
            )
            Text("Jarak: ${item.jarakKeKantor} m")
        }
    }
}
