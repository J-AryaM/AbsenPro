package com.julian.absenpro.ui.screen.admin

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.julian.absenpro.data.model.RekapAbsensi
import com.julian.absenpro.di.ServiceLocator
import com.julian.absenpro.ui.components.EmptyView
import com.julian.absenpro.ui.components.ErrorView
import com.julian.absenpro.ui.components.LoadingIndicator
import com.julian.absenpro.ui.viewmodel.RekapFilter
import com.julian.absenpro.ui.viewmodel.RekapViewModel
import com.julian.absenpro.ui.viewmodel.ViewModelFactory
import com.julian.absenpro.util.Constants
import com.julian.absenpro.util.UiState

/**
 * Layar Rekap & Export CSV (PRD F-13/F-14). Admin memfilter rekap
 * (rentang tanggal, status, tipe) lalu melihat hasilnya. Tombol Export CSV
 * membuka URL export di browser/DownloadManager dengan filter & token yang
 * SAMA seperti rekap yang tampil, sehingga file unduhan konsisten.
 *
 * Token disertakan lewat query ?token= karena unduhan browser tidak
 * membawa header Authorization (server mendukung kedua cara).
 */
@Composable
fun RekapScreen(onBack: () -> Unit) {
    val vm: RekapViewModel = viewModel(factory = ViewModelFactory)
    val context = LocalContext.current

    // Muat rekap awal (tanpa filter = semua) saat layar tampil.
    LaunchedEffect(Unit) { vm.loadRekap(RekapFilter()) }

    val rekapState by vm.rekapState.collectAsState()

    // Field filter.
    var tglMulai by remember { mutableStateOf("") }
    var tglSelesai by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }   // "", "hadir", "telat"
    var tipe by remember { mutableStateOf("") }     // "", "masuk", "pulang"

    AdminScaffold(title = "Rekap Absensi", onBack = onBack) { modifier ->
        Column(modifier.fillMaxSize().padding(16.dp)) {
            // --- Baris filter tanggal. ---
            Row {
                OutlinedTextField(
                    value = tglMulai,
                    onValueChange = { tglMulai = it },
                    label = { Text("Dari (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = tglSelesai,
                    onValueChange = { tglSelesai = it },
                    label = { Text("Sampai (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))

            // --- Filter status & tipe sebagai tombol pilih sederhana. ---
            Text("Status:", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChipButton("Semua", status == "") { status = "" }
                FilterChipButton("Hadir", status == "hadir") { status = "hadir" }
                FilterChipButton("Telat", status == "telat") { status = "telat" }
            }
            Spacer(Modifier.height(4.dp))
            Text("Tipe:", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChipButton("Semua", tipe == "") { tipe = "" }
                FilterChipButton("Masuk", tipe == "masuk") { tipe = "masuk" }
                FilterChipButton("Pulang", tipe == "pulang") { tipe = "pulang" }
            }
            Spacer(Modifier.height(8.dp))

            // --- Tombol terapkan filter & export. ---
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        vm.loadRekap(
                            RekapFilter(
                                tanggalMulai = tglMulai,
                                tanggalSelesai = tglSelesai,
                                status = status,
                                tipe = tipe
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Terapkan") }

                OutlinedButton(
                    onClick = {
                        // Bangun URL export dengan filter aktif + token, lalu buka.
                        val url = buildExportUrl(tglMulai, tglSelesai, status, tipe)
                        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Export CSV") }
            }
            Spacer(Modifier.height(12.dp))

            // --- Hasil rekap. ---
            when (val s = rekapState) {
                is UiState.Loading -> LoadingIndicator(Modifier.height(200.dp))
                is UiState.Error -> ErrorView(s.message, Modifier.height(200.dp))
                is UiState.Success -> {
                    if (s.data.isEmpty()) {
                        EmptyView("Tidak ada data untuk filter ini.")
                    } else {
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(s.data) { r -> RekapItem(r) }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

/**
 * Bangun URL endpoint export_csv.php beserta query filter & token.
 * Token diambil dari SessionManager (via ServiceLocator). Nilai filter
 * kosong tidak disertakan agar sama dengan perilaku rekap.
 */
private fun buildExportUrl(
    tglMulai: String,
    tglSelesai: String,
    status: String,
    tipe: String
): String {
    val token = ServiceLocator.sessionManager.getToken() ?: ""
    // Mulai dari base + path endpoint.
    val sb = StringBuilder(Constants.BASE_URL + "absensi/export_csv.php?token=" + token)
    if (tglMulai.isNotBlank()) sb.append("&tanggal_mulai=").append(tglMulai)
    if (tglSelesai.isNotBlank()) sb.append("&tanggal_selesai=").append(tglSelesai)
    if (status.isNotBlank()) sb.append("&status=").append(status)
    if (tipe.isNotBlank()) sb.append("&tipe=").append(tipe)
    return sb.toString()
}

/** Tombol filter kecil yang menandai pilihan aktif dengan gaya berbeda. */
@Composable
private fun FilterChipButton(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick) { Text(label) }
    }
}

/** Satu baris rekap: nama, tipe, tanggal/jam, status, jarak. */
@Composable
private fun RekapItem(r: RekapAbsensi) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(r.nama, fontWeight = FontWeight.Bold)
            Text("${r.tipe.uppercase()} • ${r.tanggal} ${r.jam}")
            Text(
                "Status: ${r.status}",
                color = if (r.status == "telat")
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary
            )
            Text("Jarak: ${r.jarakKeKantor} m")
        }
    }
}
