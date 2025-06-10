# SmartMeet 📍🤝

**SmartMeet** adalah aplikasi Android yang membantu sekelompok pengguna menemukan lokasi pertemuan (meet-up) yang optimal. Aplikasi ini menghitung titik tengah geografis dari beberapa lokasi pengguna dan merekomendasikan tempat-tempat menarik (seperti kafe atau restoran) di sekitar titik tengah tersebut. Proyek ini sepenuhnya mengandalkan API gratis dan *open-source* untuk menyediakan fungsionalitas peta dan pencarian lokasi.

---

## ✨ Fitur Utama

-   **Input Multi-Lokasi:** Memasukkan 2 hingga 5 alamat pengguna yang berbeda.
-   **Perhitungan Titik Tengah (Midpoint):** Secara otomatis menghitung titik tengah geografis dari semua alamat yang dimasukkan.
-   **Pencarian Amenitas:** Mencari dan merekomendasikan tempat (venue) berdasarkan kategori yang dipilih (misalnya, Kafe, Restoran, Taman) di sekitar titik tengah.
-   **Tampilan Peta & Daftar:** Menampilkan hasil pencarian di peta interaktif beserta daftar yang terurut berdasarkan relevansi (jarak).
-   **Detail Rute:** Menampilkan rute dari titik tengah ke venue yang dipilih di dalam aplikasi, dan menyediakan opsi untuk membuka rute di aplikasi peta eksternal.
-   **Riwayat Pencarian:** Menyimpan pencarian sebelumnya secara lokal untuk referensi di masa mendatang.
-   **Pengaturan Tema:** Opsi untuk beralih antara mode Terang (Light) dan Gelap (Dark).

---

## 🚀 Cara Penggunaan

1.  **Masukkan Alamat:** Pada halaman utama, masukkan alamat untuk setiap peserta di kolom yang tersedia.
2.  **Pilih Tipe Tempat:** Pilih jenis tempat pertemuan yang Anda inginkan dari menu dropdown (Spinner), misalnya "Kafe".
3.  **Cari Lokasi:** Tekan tombol **"Cari Lokasi"**. Aplikasi akan mengubah alamat menjadi koordinat, menghitung titik tengah, dan mencari tempat di sekitarnya.
4.  **Lihat Hasil:** Anda akan dibawa ke halaman Hasil. Di sini Anda dapat melihat:
    -   Peta dengan penanda lokasi semua peserta, titik tengah, dan rekomendasi tempat.
    -   Daftar 5 tempat teratas yang paling direkomendasikan di bawah peta.
5.  **Lihat Detail:** Klik pada salah satu tempat di daftar untuk membuka halaman Detail. Halaman ini akan menampilkan:
    -   Peta dengan rute dari titik tengah ke lokasi tempat tersebut.
    -   Tombol untuk membuka rute di aplikasi peta eksternal (misalnya Google Maps) untuk navigasi belokan demi belokan.
6.  **Akses Riwayat:** Dari menu di pojok kanan atas (Options Menu), pilih **"Riwayat Pencarian"** untuk melihat daftar pencarian yang pernah Anda lakukan.

---

## 🛠️ Implementasi Teknis

Aplikasi ini dibangun menggunakan komponen Android modern dengan fokus pada penggunaan layanan gratis dan open-source.

### Arsitektur
Aplikasi ini mengikuti arsitektur Android standar dengan pemisahan tanggung jawab yang jelas, diorganisir ke dalam beberapa package:
-   `ui`: Berisi semua Activity dan Fragment (komponen UI).
-   `data`: Berisi model data dan sumber data (network & local).
-   `util`: Berisi kelas-kelas utilitas (misalnya, kalkulasi jarak, decoder polyline).
-   `navigation`: Berisi `nav_graph.xml` untuk Navigation Component.

### Komponen & Library Utama
-   **UI & Navigasi:**
    -   `AndroidX AppCompat & Material Components`: Untuk komponen UI modern dan tema.
    -   `AndroidX Navigation Component`: Untuk mengelola alur navigasi antar fragment.
-   **Networking:**
    -   `Retrofit`: Sebagai HTTP client untuk berinteraksi dengan API secara deklaratif.
    -   `Gson`: Untuk serialisasi dan deserialisasi objek Java ke/dari JSON.
-   **Peta:**
    -   `OSMDroid`: Alternatif open-source untuk Google Maps API, digunakan untuk menampilkan peta, penanda (marker), dan rute (polyline).
-   **Penyimpanan Lokal:**
    -   `SQLite Murni (SQLiteOpenHelper)`: Digunakan untuk mengelola database lokal yang menyimpan riwayat pencarian pengguna.

### API yang Digunakan
Aplikasi ini tidak menggunakan API berbayar. Semua fungsionalitas didukung oleh layanan berikut:

1.  **[Nominatim API (OpenStreetMap)](https://nominatim.openstreetmap.org/)**
    -   **Tujuan:** *Geocoding*. Mengubah input alamat teks dari pengguna menjadi koordinat Lintang (Latitude) dan Bujur (Longitude).

2.  **[Overpass API (OpenStreetMap)](https://overpass-api.de/)**
    -   **Tujuan:** *Pencarian Tempat (Places)*. Mengajukan query kompleks untuk mencari data mentah dari OpenStreetMap, seperti "temukan semua kafe dalam radius 2km dari titik X".

3.  **[OpenRouteService API](https://openrouteservice.org/)**
    -   **Tujuan:** *Routing/Directions*. Menghitung dan menyediakan data rute (dalam bentuk polyline) antara dua titik koordinat. **Membutuhkan API key gratis**.

---

## ⚙️ Setup & Instalasi

Untuk menjalankan proyek ini di Android Studio:

1.  **Clone Repositori**
    ```bash
    git clone [https://github.com/USERNAME_ANDA/NAMA_REPOSITORI_ANDA.git](https://github.com/USERNAME_ANDA/NAMA_REPOSITORI_ANDA.git)
    ```

2.  **Buka di Android Studio**
    -   Buka Android Studio, pilih "Open" dan arahkan ke folder proyek yang baru saja Anda clone.

3.  **Tambahkan API Key OpenRouteService (PENTING!)**
    -   Daftar dan dapatkan API key gratis dari [situs OpenRouteService](https://openrouteservice.org/dev-hub/).
    -   Buka file `ApiClient.java` di `com.smartmeet.data.network`.
    -   Ganti placeholder `YOUR_ORS_API_KEY` dengan API key Anda.
        ```java
        // Di dalam ApiClient.java
        public static final String ORS_API_KEY = "GANTI_DENGAN_API_KEY_ANDA";
        ```
    -   **(Cara yang Lebih Baik)** Untuk keamanan, letakkan API key Anda di file `gradle.properties` (tingkat proyek):
        ```properties
        ORS_API_KEY="GANTI_DENGAN_API_KEY_ANDA"
        ```
        Lalu akses di `build.gradle (Module: app)` dan `ApiClient.java`.

4.  **Sync Gradle & Run**
    -   Biarkan Android Studio melakukan sinkronisasi Gradle.
    -   Jalankan aplikasi pada emulator atau perangkat fisik.

---

## 📜 Lisensi

Proyek ini dilisensikan di bawah **MIT License**. Lihat file `LICENSE` untuk detail lebih lanjut.
