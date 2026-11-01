package com.julian.absenpro.ui.screen.karyawan

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.julian.absenpro.data.model.SesiAbsen
import com.julian.absenpro.ui.viewmodel.AbsensiViewModel
import com.julian.absenpro.util.UiState

/**
 * Layar Absen (beranda karyawan). Menampilkan:
 *  - sapaan nama,
 *  - info lokasi kantor & radius,
 *  - status sesi masuk/pulang hari ini,
 *  - tombol Absen Masuk / Absen Pulang (aktif hanya bila sesinya 'aktif'),
 *  - hasil absen (hadir/telat) atau alasan penolakan dari server.
 *
 * Izin lokasi diminta di sini karena absen butuh GPS. Bila belum diberikan,
 * tombol absen memicu permintaan izin lebih dulu.
 */
@Composable
fun AbsenScreen(
    nama: String,
    viewModel: AbsensiViewModel
) {
    val context = LocalContext.current

    // Muat lokasi kantor + sesi hari ini sekali saat layar tampil.
    LaunchedEffect(Unit) {
        viewModel.loadAbsenScreen()
    }

    val lokasiState by viewModel.lokasiState.collectAsState()
    val sesiState by viewModel.sesiState.collectAsState()
    val absenState by viewModel.absenState.collectAsState()

    // Menyimpan tipe absen yang diminta agar dieksekusi setelah izin lokasi
    // diberikan (permintaan izin bersifat asynchronous lewat launcher).
    var pendingTipe by remember { mutableStateOf<String?>(null) }

    // Launcher untuk meminta izin lokasi runtime. Callback dijalankan setelah
    // pengguna menjawab dialog izin sistem.
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Bila diizinkan dan ada tipe absen yang menunggu, jalankan absennya.
        val tipe = pendingTipe
        if (granted && tipe != null) {
            viewModel.absen(context, tipe)
        }
        pendingTipe = null
    }

    // Fungsi bantu: cek izin -> langsung absen, atau minta izin dulu.
    fun requestAbsen(tipe: String) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.absen(context, tipe)
        } else {
            // Simpan niat absen lalu minta izin; absen dilanjutkan di callback.
            pendingTipe = tipe
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Halo, $nama",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))

        // --- Kartu info lokasi kantor. ---
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Lokasi Kantor", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                when (val s = lokasiState) {
                    is UiState.Loading -> Text("Memuat lokasi...")
                    is UiState.Success -> {
                        val l = s.data
                        Text(l.namaKantor)
                        Text("Koordinat: ${l.latitude}, ${l.longitude}")
                        Text("Radius toleransi: ${l.radiusMeter} m")
                    }
                    is UiState.Error -> Text(s.message, color = MaterialTheme.colorScheme.error)
                    else -> {}
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // --- Tombol absen berdasarkan status sesi. ---
        when (val s = sesiState) {
            is UiState.Loading -> {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) { CircularProgressIndicator() }
            }
            is UiState.Success -> {
                // Cari sesi masuk & pulang dari daftar hari ini.
                val sesiMasuk = s.data.firstOrNull { it.tipe == "masuk" }
                val sesiPulang = s.data.firstOrNull { it.tipe == "pulang" }
                AbsenButtons(
                    sesiMasuk = sesiMasuk,
                    sesiPulang = sesiPulang,
                    isProcessing = absenState is UiState.Loading,
                    onAbsen = { tipe -> requestAbsen(tipe) }
                )
            }
            is UiState.Error -> Text(s.message, color = MaterialTheme.colorScheme.error)
            else -> {}
        }

        Spacer(Modifier.height(16.dp))

        // --- Hasil absen (konfirmasi atau error dari server). ---
        when (val a = absenState) {
            is UiState.Loading -> {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(8.dp))
                    Text("  Mengambil lokasi & memproses absen...")
                }
            }
            is UiState.Success -> {
                val r = a.data
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Absen ${r.tipe} berhasil!",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("Status: ${r.status}")
                        Text("Jarak ke kantor: ${r.jarakKeKantor} m (radius ${r.radiusMeter} m)")
                        Text("Waktu: ${r.waktuAbsen}")
                    }
                }
            }
            is UiState.Error -> {
                Text(
                    a.message,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            else -> {}
        }
    }
}

/**
 * Dua tombol absen. Masing-masing aktif HANYA jika sesinya ada & berstatus
 * 'aktif' (server tetap memvalidasi ulang, tapi ini mencegah klik sia-sia).
 * isProcessing menonaktifkan tombol saat proses absen berjalan.
 */
@Composable
private fun AbsenButtons(
    sesiMasuk: SesiAbsen?,
    sesiPulang: SesiAbsen?,
    isProcessing: Boolean,
    onAbsen: (String) -> Unit
) {
    val masukAktif = sesiMasuk?.status == "aktif"
    val pulangAktif = sesiPulang?.status == "aktif"

    Column(Modifier.fillMaxWidth()) {
        // Absen Masuk.
        Button(
            onClick = { onAbsen("masuk") },
            enabled = masukAktif && !isProcessing,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (masukAktif) "Absen Masuk (${sesiMasuk?.jamBuka}-${sesiMasuk?.jamTutup})"
                else "Sesi Masuk Ditutup"
            )
        }
        Spacer(Modifier.height(8.dp))

        // Absen Pulang.
        Button(
            onClick = { onAbsen("pulang") },
            enabled = pulangAktif && !isProcessing,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (pulangAktif) "Absen Pulang (${sesiPulang?.jamBuka}-${sesiPulang?.jamTutup})"
                else "Sesi Pulang Ditutup"
            )
        }
    }
}
