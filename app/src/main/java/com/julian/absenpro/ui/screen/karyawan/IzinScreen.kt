package com.julian.absenpro.ui.screen.karyawan

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.julian.absenpro.data.model.Izin
import com.julian.absenpro.ui.components.EmptyView
import com.julian.absenpro.ui.components.ErrorView
import com.julian.absenpro.ui.components.LoadingIndicator
import com.julian.absenpro.ui.viewmodel.IzinViewModel
import com.julian.absenpro.util.UiState

/**
 * Layar Izin karyawan (PRD F-05/F-06). Bagian atas = form pengajuan
 * (dengan pemilih file bukti), bagian bawah = daftar izin miliknya beserta
 * status approval.
 *
 * Alur:
 *  - submitState Loading -> tombol jadi spinner.
 *  - submitState Success -> form dikosongkan, daftar otomatis refresh.
 *  - listState menampilkan izin milik karyawan (server yang memfilter).
 */
@Composable
fun IzinScreen(viewModel: IzinViewModel) {
    LaunchedEffect(Unit) { viewModel.loadList() }

    val listState by viewModel.listState.collectAsState()
    val submitState by viewModel.submitState.collectAsState()

    // State form pengajuan.
    var jenis by remember { mutableStateOf("") }
    var tglMulai by remember { mutableStateOf("") }
    var tglSelesai by remember { mutableStateOf("") }
    var keterangan by remember { mutableStateOf("") }
    var fileUri by remember { mutableStateOf<Uri?>(null) }

    // Picker file bukti. GetContent mengembalikan content Uri yang kemudian
    // dibaca Repository saat submit (disalin ke file lalu diunggah).
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> fileUri = uri }

    // Setelah submit sukses, bersihkan form agar siap untuk pengajuan baru.
    LaunchedEffect(submitState) {
        if (submitState is UiState.Success) {
            jenis = ""; tglMulai = ""; tglSelesai = ""; keterangan = ""; fileUri = null
            viewModel.resetSubmitState()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // --- Bagian form pengajuan (item pertama LazyColumn). ---
        item {
            Text(
                "Ajukan Izin",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = jenis,
                onValueChange = { jenis = it },
                label = { Text("Jenis Izin (Sakit/Cuti/Dinas)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            // Tanggal diketik manual dengan format YYYY-MM-DD (server memvalidasi).
            OutlinedTextField(
                value = tglMulai,
                onValueChange = { tglMulai = it },
                label = { Text("Tanggal Mulai (YYYY-MM-DD)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = tglSelesai,
                onValueChange = { tglSelesai = it },
                label = { Text("Tanggal Selesai (YYYY-MM-DD)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = keterangan,
                onValueChange = { keterangan = it },
                label = { Text("Keterangan (opsional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            // Tombol pilih file. Menampilkan status terpilih/belum.
            OutlinedButton(
                onClick = { filePicker.launch("*/*") }, // izinkan gambar/PDF
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (fileUri != null) "File bukti dipilih ✓" else "Pilih File Bukti (opsional)")
            }
            Spacer(Modifier.height(12.dp))

            if (submitState is UiState.Loading) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Button(
                    onClick = {
                        viewModel.submit(jenis, tglMulai, tglSelesai, keterangan, fileUri)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Kirim Pengajuan")
                }
            }

            if (submitState is UiState.Error) {
                Text(
                    (submitState as UiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "Riwayat Pengajuan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
        }

        // --- Bagian daftar izin. ---
        when (val s = listState) {
            is UiState.Loading -> item { LoadingIndicator(Modifier.height(120.dp)) }
            is UiState.Error -> item { ErrorView(s.message, Modifier.height(160.dp), onRetry = { viewModel.loadList() }) }
            is UiState.Success -> {
                if (s.data.isEmpty()) {
                    item { EmptyView("Belum ada pengajuan izin.") }
                } else {
                    items(s.data) { izin -> IzinKaryawanItem(izin) }
                }
            }
            else -> {}
        }
    }
}

/**
 * Satu kartu izin dari sisi karyawan: menampilkan jenis, rentang tanggal,
 * status approval, dan catatan admin bila ada (mis. alasan penolakan).
 */
@Composable
private fun IzinKaryawanItem(izin: Izin) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("${izin.jenisIzin}", fontWeight = FontWeight.Bold)
            Text("${izin.tanggalMulai} s/d ${izin.tanggalSelesai}")
            if (!izin.keterangan.isNullOrBlank()) Text("Ket: ${izin.keterangan}")
            // Warnai status: hijau disetujui, merah ditolak, default menunggu.
            Text(
                "Status: ${izin.statusApproval}",
                color = when (izin.statusApproval) {
                    "disetujui" -> MaterialTheme.colorScheme.primary
                    "ditolak" -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            if (!izin.catatanAdmin.isNullOrBlank()) {
                Text("Catatan admin: ${izin.catatanAdmin}")
            }
            if (izin.fileBuktiUrl != null) {
                Text(
                    "Ada file bukti terlampir",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
