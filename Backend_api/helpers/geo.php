<?php
// =====================================================================
// Backend_api/helpers/geo.php
// Perhitungan jarak dua titik koordinat memakai formula Haversine.
// Dipakai server saat memvalidasi absen.
//
// Kenapa Haversine? Bumi berbentuk bola, sehingga jarak dua titik lat/long
// bukan garis lurus di bidang datar melainkan busur di permukaan bola.
// Haversine menghitung jarak "great-circle" itu dengan akurasi yang
// lebih dari cukup untuk skala geofencing puluhan-ratusan meter.
// =====================================================================

/**
 * Hitung jarak antara dua titik GPS dalam METER.
 *
 * @param float $lat1 latitude titik 1 (mis. posisi karyawan)
 * @param float $lon1 longitude titik 1
 * @param float $lat2 latitude titik 2 (mis. kantor)
 * @param float $lon2 longitude titik 2
 * @return float jarak dalam meter
 */
function haversineMeters(float $lat1, float $lon1, float $lat2, float $lon2): float
{
    // Radius rata-rata Bumi dalam meter (nilai standar WGS-84 approx).
    $earthRadius = 6371000.0;

    // Trigonometri butuh radian, sedangkan koordinat GPS dalam derajat.
    $dLat = deg2rad($lat2 - $lat1);
    $dLon = deg2rad($lon2 - $lon1);

    // Inti formula Haversine: 'a' adalah kuadrat setengah panjang tali busur.
    $a = sin($dLat / 2) * sin($dLat / 2) +
        cos(deg2rad($lat1)) * cos(deg2rad($lat2)) *
        sin($dLon / 2) * sin($dLon / 2);

    // 'c' sudut pusat busur; atan2 lebih stabil secara numerik daripada asin.
    $c = 2 * atan2(sqrt($a), sqrt(1 - $a));

    // Jarak = radius Bumi x sudut pusat.
    return $earthRadius * $c;
}
