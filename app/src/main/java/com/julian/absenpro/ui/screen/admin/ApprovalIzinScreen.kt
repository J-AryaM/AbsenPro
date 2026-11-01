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
import com.julian.absenpro.data.model.Izin
import com.julian.absenpro.ui.components.EmptyView
import com.julian.absenpro.ui.components.ErrorView
import com.julian.absenpro.ui.components.LoadingIndicator
import com.julian.absenpro.ui.viewmodel.IzinViewModel
import com.julian.absenpro.ui.viewmodel.ViewModelFactory
import com.julian.absenpro.util.UiState

/**
 * Layar Approval Izin (PRD F-12). Admin melihat SEMUA pengajuan (server
 * mengembalikan semua karena token admin), dapat membuka file bukti, lalu
 * menyetujui/menolak dengan catatan.
 *
 * Setiap keputusan memicu reload daftar (di ViewModel) agar status baru
 * langsung tampak.
 */
@Composable
fun ApprovalIzinScreen(onBack: () -> Unit) {
    val vm: IzinViewModel = viewModel(factory = ViewModelFactory)

    LaunchedEffect(Unit) { vm.loadList() }
    val listState by vm.listState.collectAsState()

    AdminScaffold(title = "Approval Izin", onBack = onBack) { modifier ->
        when (val s = listState) {
            is UiState.Loading -> LoadingIndicator(modifier.fillMaxSize())
            is UiState.Error -> ErrorView(s.message, modifier.fillMaxSize(), onRetry = { vm.loadList() })
            is UiState.Success -> {
                if (s.data.isEmpty()) {
                    EmptyView("Belum ada pengajuan izin.", modifier.fillMaxSize())
                } else {
                    LazyColumn(modifier.fillMaxSize().padding(16.dp)) {
                        items(s.data) { izin ->
                            IzinAdminItem(
                                izin = izin,
                                onApprove = { catatan -> vm.approval(izin.id, "disetujui", catatan) },
                                onReject = { catatan -> vm.approval(izin.id, "ditolak", catatan) }
                            )
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

/**
 * Kartu izin dari sisi admin: identitas pengaju, detail, tombol lihat bukti
 * (membuka URL file di browser/aplikasi lain), input catatan, serta tombol
 * Setujui / Tolak. Tombol dinonaktifkan bila sudah diproses sebelumnya.
 */
@Composable
private fun IzinAdminItem(
    izin: Izin,
    onApprove: (catatan: String?) -> Unit,
    onReject: (catatan: String?) -> Unit
) {
    val context = LocalContext.current
    var catatan by remember { mutableStateOf(izin.catatanAdmin ?: "") }

    // Sudah diputuskan bila statusnya bukan "menunggu".
    val sudahDiproses = izin.statusApproval != "menunggu"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(izin.nama, fontWeight = FontWeight.Bold)
            Text("${izin.jenisIzin} • ${izin.tanggalMulai} s/d ${izin.tanggalSelesai}")
            if (!izin.keterangan.isNullOrBlank()) Text("Ket: ${izin.keterangan}")
            Text(
                "Status: ${izin.statusApproval}",
                color = when (izin.statusApproval) {
                    "disetujui" -> MaterialTheme.colorScheme.primary
                    "ditolak" -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            // Tombol buka file bukti bila ada. Membuka URL lewat Intent VIEW
            // (browser/penampil gambar/PDF) karena file di-serve backend.
            if (izin.fileBuktiUrl != null) {
                Spacer(Modifier.height(4.dp))
                OutlinedButton(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, izin.fileBuktiUrl.toUri())
                    context.startActivity(intent)
                }) {
                    Text("Lihat File Bukti")
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = catatan,
                onValueChange = { catatan = it },
                label = { Text("Catatan (opsional)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !sudahDiproses
            )
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onApprove(catatan.ifBlank { null }) },
                    enabled = !sudahDiproses,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Setujui")
                }
                OutlinedButton(
                    onClick = { onReject(catatan.ifBlank { null }) },
                    enabled = !sudahDiproses,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Tolak")
                }
            }
        }
    }
}
