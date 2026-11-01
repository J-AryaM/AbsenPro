package com.julian.absenpro.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.julian.absenpro.di.ServiceLocator
import com.julian.absenpro.ui.screen.admin.AdminHomeScreen
import com.julian.absenpro.ui.screen.admin.ApprovalIzinScreen
import com.julian.absenpro.ui.screen.admin.AturLokasiScreen
import com.julian.absenpro.ui.screen.admin.KelolaKaryawanScreen
import com.julian.absenpro.ui.screen.admin.KontrolSesiScreen
import com.julian.absenpro.ui.screen.admin.RekapScreen
import com.julian.absenpro.ui.screen.auth.LoginScreen
import com.julian.absenpro.ui.screen.auth.RegisterScreen
import com.julian.absenpro.ui.screen.karyawan.KaryawanHomeScreen
import com.julian.absenpro.ui.viewmodel.AuthViewModel
import com.julian.absenpro.ui.viewmodel.ViewModelFactory

/**
 * Pusat navigasi aplikasi. Menentukan layar awal berdasarkan status sesi
 * (sudah login -> ke area sesuai role; belum -> login) lalu mendefinisikan
 * semua rute dan aksi perpindahannya.
 *
 * Prinsip: hanya NavGraph yang tahu cara berpindah antar layar. Tiap layar
 * cukup memanggil callback (mis. onLoginSuccess) tanpa tahu detail rute,
 * sehingga layar tetap dapat diuji/dipakai ulang.
 */
@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    // AuthViewModel dibagikan untuk cek sesi & aksi login/logout.
    val authViewModel: AuthViewModel = viewModel(factory = ViewModelFactory)
    val session = ServiceLocator.sessionManager

    // Tentukan rute awal: bila sudah login, langsung ke home sesuai peran.
    val startDestination = when {
        session.isLoggedIn() && session.getRole() == "admin" -> Routes.ADMIN_HOME
        session.isLoggedIn() && session.getRole() == "karyawan" -> Routes.KARYAWAN_HOME
        else -> Routes.LOGIN
    }

    NavHost(navController = navController, startDestination = startDestination) {

        // ------------------------- AUTH -------------------------
        composable(Routes.LOGIN) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = { role ->
                    // Arahkan ke area sesuai role, hapus login dari back stack
                    // agar tombol back tidak kembali ke halaman login.
                    val dest = if (role == "admin") Routes.ADMIN_HOME else Routes.KARYAWAN_HOME
                    navController.navigate(dest) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                viewModel = authViewModel,
                onRegisterSuccess = {
                    // Setelah daftar, kembali ke login untuk masuk.
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        // ----------------------- KARYAWAN -----------------------
        composable(Routes.KARYAWAN_HOME) {
            // Ambil identitas dari sesi untuk sapaan & profil.
            KaryawanHomeScreen(
                nama = session.getNama() ?: "Karyawan",
                role = session.getRole() ?: "karyawan",
                onLogout = {
                    authViewModel.logout {
                        // Setelah sesi dibersihkan, kembali ke login & reset stack.
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true } // bersihkan seluruh back stack
                        }
                    }
                }
            )
        }

        // ------------------------- ADMIN ------------------------
        composable(Routes.ADMIN_HOME) {
            AdminHomeScreen(
                nama = session.getNama() ?: "Admin",
                onKelolaKaryawan = { navController.navigate(Routes.ADMIN_KARYAWAN) },
                onAturLokasi = { navController.navigate(Routes.ADMIN_LOKASI) },
                onKontrolSesi = { navController.navigate(Routes.ADMIN_SESI) },
                onApprovalIzin = { navController.navigate(Routes.ADMIN_IZIN) },
                onRekap = { navController.navigate(Routes.ADMIN_REKAP) },
                onLogout = {
                    authViewModel.logout {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }

        // Sub-layar admin: masing-masing hanya perlu aksi kembali.
        composable(Routes.ADMIN_KARYAWAN) {
            KelolaKaryawanScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.ADMIN_LOKASI) {
            AturLokasiScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.ADMIN_SESI) {
            KontrolSesiScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.ADMIN_IZIN) {
            ApprovalIzinScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.ADMIN_REKAP) {
            RekapScreen(onBack = { navController.popBackStack() })
        }
    }
}
