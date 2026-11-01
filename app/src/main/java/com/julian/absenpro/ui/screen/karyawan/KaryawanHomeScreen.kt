package com.julian.absenpro.ui.screen.karyawan

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.julian.absenpro.ui.viewmodel.AbsensiViewModel
import com.julian.absenpro.ui.viewmodel.IzinViewModel
import com.julian.absenpro.ui.viewmodel.ViewModelFactory

/**
 * Shell (kerangka) area karyawan dengan bottom navigation 4 tab:
 * Absen, Riwayat, Izin, Profil. Tab dikelola dengan indeks lokal
 * (bukan NavController terpisah) agar sederhana dan state tab tidak hilang.
 *
 * ViewModel dibuat di level ini dengan ViewModelFactory lalu diteruskan
 * ke tiap sub-layar, sehingga sub-layar berbagi ViewModel yang sama
 * (mis. AbsenScreen & RiwayatScreen memakai AbsensiViewModel yang sama).
 *
 * @param nama     nama karyawan (dari sesi) untuk sapaan & profil.
 * @param role     peran (untuk ditampilkan di profil).
 * @param onLogout dipanggil saat menekan logout di layar profil.
 */
@Composable
fun KaryawanHomeScreen(
    nama: String,
    role: String,
    onLogout: () -> Unit
) {
    // Indeks tab aktif. mutableIntStateOf lebih efisien untuk Int.
    var selectedTab by remember { mutableIntStateOf(0) }

    // ViewModel dibagi ke sub-layar. Dibuat sekali & bertahan selama shell ini.
    val absensiViewModel: AbsensiViewModel = viewModel(factory = ViewModelFactory)
    val izinViewModel: IzinViewModel = viewModel(factory = ViewModelFactory)

    // Definisi tab: label + ikon. Urutannya menentukan indeks.
    val tabs = listOf("Absen", "Riwayat", "Izin", "Profil")

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            // Ikon dipilih sesuai fungsi tiap tab.
                            val icon = when (index) {
                                0 -> Icons.Filled.LocationOn // Absen (berbasis lokasi)
                                1 -> Icons.Filled.DateRange  // Riwayat
                                2 -> Icons.Filled.Edit       // Izin (mengajukan)
                                else -> Icons.Filled.Person  // Profil
                            }
                            Icon(icon, contentDescription = label)
                        },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        // Isi layar sesuai tab terpilih. Modifier padding menghindari konten
        // tertutup bottom bar.
        val contentModifier = Modifier.padding(padding)
        when (selectedTab) {
            0 -> androidx.compose.foundation.layout.Box(contentModifier) {
                AbsenScreen(nama = nama, viewModel = absensiViewModel)
            }
            1 -> androidx.compose.foundation.layout.Box(contentModifier) {
                RiwayatScreen(viewModel = absensiViewModel)
            }
            2 -> androidx.compose.foundation.layout.Box(contentModifier) {
                IzinScreen(viewModel = izinViewModel)
            }
            else -> androidx.compose.foundation.layout.Box(contentModifier) {
                ProfilScreen(nama = nama, role = role, onLogout = onLogout)
            }
        }
    }
}
