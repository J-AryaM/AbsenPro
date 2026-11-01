package com.julian.absenpro.ui.screen.admin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Kerangka umum untuk layar-layar admin: TopAppBar dengan judul + tombol
 * kembali, lalu slot konten. Dibuat sekali agar semua layar admin
 * konsisten dan tidak menulis ulang TopAppBar.
 *
 * @param title judul layar.
 * @param onBack aksi tombol panah kembali.
 * @param content isi layar (diberi padding scaffold otomatis).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                }
            )
        }
    ) { padding ->
        // Teruskan Modifier padding ke konten agar tidak tertutup app bar.
        Box(Modifier.padding(padding)) {
            content(Modifier)
        }
    }
}
