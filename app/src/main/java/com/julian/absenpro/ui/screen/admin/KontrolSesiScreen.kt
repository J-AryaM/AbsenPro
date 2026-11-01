package com.julian.absenpro.ui.screen.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.julian.absenpro.data.model.SesiAbsen
import com.julian.absenpro.ui.viewmodel.SesiViewModel
import com.julian.absenpro.ui.viewmodel.ViewModelFactory
import com.julian.absenpro.util.UiState

/**
 * Layar Kontrol Sesi Absen (PRD F-11). Admin mengatur jendela waktu absen
 * MASUK dan PULANG secara independen untuk tanggal tertentu, serta
 * membuka/menutup masing-masing.
 *
 * Untuk kesederhanaan, tanggal default diisi hari ini (diketik admin bila
 * ingin tanggal lain). Setiap simpan/ubah memicu reload daftar sesi.
 */
@Composable
fun KontrolSesiScreen(onBack: () -> Unit) {
    val vm: SesiViewModel = viewModel(factory = ViewModelFactory)

    // Tanggal target; diketik admin. Format YYYY-MM-DD (divalidasi server).
    var tanggal by remember { mutableStateOf(hariIni()) }

    // Muat sesi untuk tanggal awal saat layar tampil.
    LaunchedEffect(Unit) { vm.loadSesi(tanggal) }

    val sesiState by vm.sesiState.collectAsState()

    AdminScaffold(title = "Kontrol Sesi Absen", onBack = onBack) { modifier ->
        Column(
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Input tanggal + tombol muat.
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                OutlinedTextField(
                    value = tanggal,
                    onValueChange = { tanggal = it },
                    label = { Text("Tanggal (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = { vm.loadSesi(tanggal) }) { Text("Muat") }
            }
            Spacer(Modifier.height(16.dp))

            // Ambil sesi masuk & pulang dari state (bila sudah dimuat).
            val list = (sesiState as? UiState.Success)?.data ?: emptyList()
            val sesiMasuk = list.firstOrNull { it.tipe == "masuk" }
            val sesiPulang = list.firstOrNull { it.tipe == "pulang" }

            // Editor sesi MASUK.
            SesiEditor(
                judul = "Sesi Masuk",
                tipe = "masuk",
                tanggal = tanggal,
                existing = sesiMasuk,
                onSimpan = { jb, jt, st -> vm.simpanSesi("masuk", tanggal, jb, jt, st) },
                onToggle = { id, status -> vm.ubahStatus(id, status) }
            )
            Spacer(Modifier.height(16.dp))

            // Editor sesi PULANG.
            SesiEditor(
                judul = "Sesi Pulang",
                tipe = "pulang",
                tanggal = tanggal,
                existing = sesiPulang,
                onSimpan = { jb, jt, st -> vm.simpanSesi("pulang", tanggal, jb, jt, st) },
                onToggle = { id, status -> vm.ubahStatus(id, status) }
            )

            // Tampilkan error muat bila ada.
            (sesiState as? UiState.Error)?.let {
                Text(
                    it.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}

/**
 * Kartu editor untuk satu tipe sesi. Bila sesi sudah ada, jam-nya diisi
 * awal dari data existing dan tombol buka/tutup ditampilkan. Bila belum
 * ada, admin mengisi jam lalu menekan "Simpan Sesi" untuk membuatnya.
 */
@Composable
private fun SesiEditor(
    judul: String,
    tipe: String,
    tanggal: String,
    existing: SesiAbsen?,
    onSimpan: (jamBuka: String, jamTutup: String, status: String) -> Unit,
    onToggle: (id: Int, status: String) -> Unit
) {
    // Jam diisi dari sesi yang ada, atau default kosong untuk sesi baru.
    var jamBuka by remember(existing?.id) { mutableStateOf(existing?.jamBuka ?: "") }
    var jamTutup by remember(existing?.id) { mutableStateOf(existing?.jamTutup ?: "") }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(judul, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))

            // Status sekarang (bila sesi sudah ada).
            if (existing != null) {
                Text(
                    "Status: ${existing.status}",
                    color = if (existing.status == "aktif")
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
            } else {
                Text("Belum dibuat untuk tanggal ini.")
            }
            Spacer(Modifier.height(8.dp))

            Row {
                OutlinedTextField(
                    value = jamBuka,
                    onValueChange = { jamBuka = it },
                    label = { Text("Jam Buka (HH:MM)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = jamTutup,
                    onValueChange = { jamTutup = it },
                    label = { Text("Jam Tutup (HH:MM)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Simpan/buat sesi dengan status 'aktif' langsung.
                Button(
                    onClick = { onSimpan(jamBuka, jamTutup, "aktif") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (existing == null) "Buat & Buka" else "Simpan")
                }

                // Tombol buka/tutup hanya muncul bila sesi sudah ada.
                if (existing != null) {
                    OutlinedButton(
                        onClick = {
                            // Toggle status ke kebalikannya.
                            val next = if (existing.status == "aktif") "nonaktif" else "aktif"
                            onToggle(existing.id, next)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (existing.status == "aktif") "Tutup" else "Buka")
                    }
                }
            }
        }
    }
}

/**
 * Tanggal hari ini format YYYY-MM-DD memakai API waktu Java (tersedia di
 * minSdk 26). Dipakai sebagai nilai awal input tanggal.
 */
private fun hariIni(): String {
    return java.time.LocalDate.now().toString() // LocalDate.toString() -> ISO yyyy-MM-dd
}
