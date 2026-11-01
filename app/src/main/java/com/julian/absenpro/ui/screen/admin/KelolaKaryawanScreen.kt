package com.julian.absenpro.ui.screen.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.julian.absenpro.data.model.Karyawan
import com.julian.absenpro.ui.components.EmptyView
import com.julian.absenpro.ui.components.ErrorView
import com.julian.absenpro.ui.components.LoadingIndicator
import com.julian.absenpro.ui.viewmodel.KaryawanViewModel
import com.julian.absenpro.ui.viewmodel.ViewModelFactory
import com.julian.absenpro.util.UiState

/**
 * Layar Kelola Karyawan (PRD F-09). Menampilkan daftar karyawan yang
 * mendaftar mandiri, dengan tombol Aktifkan/Nonaktifkan per baris.
 * (Admin tidak membuat akun — hanya mengelola yang sudah ada.)
 *
 * Mengamati listState untuk daftar; setiap aksi setActive() memicu reload
 * daftar dari ViewModel sehingga badge status langsung sinkron.
 */
@Composable
fun KelolaKaryawanScreen(onBack: () -> Unit) {
    val vm: KaryawanViewModel = viewModel(factory = ViewModelFactory)

    LaunchedEffect(Unit) { vm.loadList() }
    val listState by vm.listState.collectAsState()

    AdminScaffold(title = "Kelola Karyawan", onBack = onBack) { modifier ->
        when (val s = listState) {
            is UiState.Loading -> LoadingIndicator(modifier.fillMaxSize())
            is UiState.Error -> ErrorView(s.message, modifier.fillMaxSize(), onRetry = { vm.loadList() })
            is UiState.Success -> {
                if (s.data.isEmpty()) {
                    EmptyView("Belum ada karyawan terdaftar.", modifier.fillMaxSize())
                } else {
                    LazyColumn(modifier.fillMaxSize().padding(16.dp)) {
                        items(s.data) { k ->
                            KaryawanRow(
                                karyawan = k,
                                onToggleActive = { vm.setActive(k.id, !k.isActive) }
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
 * Satu baris karyawan: identitas + tombol ubah status aktif.
 * Warna teks status memberi sinyal cepat aktif (primary) / nonaktif (error).
 */
@Composable
private fun KaryawanRow(
    karyawan: Karyawan,
    onToggleActive: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(karyawan.nama, fontWeight = FontWeight.Bold)
                Text("@${karyawan.username}", style = MaterialTheme.typography.bodySmall)
                if (!karyawan.jabatan.isNullOrBlank()) Text("Jabatan: ${karyawan.jabatan}")
                if (!karyawan.kontak.isNullOrBlank()) Text("Kontak: ${karyawan.kontak}")
                Text(
                    text = if (karyawan.isActive) "Aktif" else "Nonaktif",
                    color = if (karyawan.isActive)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(8.dp))
            // Tombol mengubah status ke kebalikannya.
            Button(onClick = onToggleActive) {
                Text(if (karyawan.isActive) "Nonaktifkan" else "Aktifkan")
            }
        }
    }
}
