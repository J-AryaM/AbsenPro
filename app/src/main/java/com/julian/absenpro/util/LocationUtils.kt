package com.julian.absenpro.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Utilitas lokasi: (1) menghitung jarak dua titik GPS dengan Haversine,
 * dan (2) membungkus FusedLocationProviderClient agar bisa dipanggil
 * secara suspend (cocok dengan coroutine di ViewModel).
 */
object LocationUtils {

    /**
     * Jarak dua koordinat dalam METER memakai formula Haversine.
     *
     * Kenapa Haversine (bukan sekadar selisih lat/long)? Permukaan Bumi
     * melengkung, sehingga jarak sebenarnya adalah panjang busur di bola,
     * bukan garis lurus pada bidang datar. Untuk geofencing skala ratusan
     * meter, Haversine memberi hasil yang akurat dan murah dihitung.
     *
     * Perhitungan ini dipakai UI untuk PRATINJAU jarak sebelum absen;
     * keputusan lolos/tidak tetap ditentukan server (yang menghitung ulang).
     */
    fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // radius Bumi (meter)

        // Trigonometri butuh radian; koordinat GPS dalam derajat.
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        // 'a' = kuadrat setengah panjang tali busur antara dua titik.
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        // 'c' = sudut pusat busur; atan2 lebih stabil daripada asin.
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

    /**
     * Ambil lokasi GPS terkini sebagai fungsi suspend. Mengembalikan
     * Location, atau null bila lokasi tidak bisa didapat (mis. GPS mati).
     *
     * @SuppressLint("MissingPermission"): pemanggil WAJIB memastikan izin
     * ACCESS_FINE_LOCATION sudah diberikan sebelum memanggil ini (dicek di
     * layar/ViewModel). Anotasi ini menekan peringatan compiler karena
     * pengecekan izin dilakukan di lapisan UI.
     *
     * Memakai getCurrentLocation (bukan lastLocation) supaya mendapat
     * pembacaan SEGAR — penting untuk absen agar tidak memakai lokasi lama
     * yang bisa saja masih menunjukkan posisi sebelumnya.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Location? =
        suspendCancellableCoroutine { cont ->
            val client = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()

            // PRIORITY_HIGH_ACCURACY meminta GPS presisi tinggi (bukan hanya
            // jaringan), sesuai kebutuhan absensi berbasis lokasi.
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { location ->
                    // location bisa null bila perangkat gagal fix GPS.
                    if (cont.isActive) cont.resume(location)
                }
                .addOnFailureListener {
                    // Kegagalan (mis. layanan lokasi mati) -> kembalikan null,
                    // biarkan lapisan atas menampilkan pesan yang sesuai.
                    if (cont.isActive) cont.resume(null)
                }

            // Bila coroutine dibatalkan (mis. layar ditutup), batalkan pula
            // permintaan lokasi agar tidak bocor resource.
            cont.invokeOnCancellation { cts.cancel() }
        }
}
