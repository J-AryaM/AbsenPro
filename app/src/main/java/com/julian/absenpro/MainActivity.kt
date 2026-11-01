package com.julian.absenpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.julian.absenpro.di.ServiceLocator
import com.julian.absenpro.ui.navigation.AppNavGraph
import com.julian.absenpro.ui.theme.AbsenProTheme

/**
 * Activity tunggal aplikasi (single-activity + Compose Navigation).
 * Tugasnya minimal: menyiapkan dependency global lalu menampilkan NavGraph.
 * Seluruh logika layar berada di Composable + ViewModel, sesuai pola MVVM.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inisialisasi Service Locator SEKALI dengan applicationContext.
        // Harus sebelum ada ViewModel/Repository yang dibuat, karena mereka
        // mengambil dependency dari sini (SessionManager, Retrofit, dll).
        ServiceLocator.init(applicationContext)

        enableEdgeToEdge()
        setContent {
            AbsenProTheme {
                // Surface memberi latar sesuai tema Material di seluruh layar.
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Titik masuk seluruh navigasi aplikasi.
                    AppNavGraph()
                }
            }
        }
    }
}
