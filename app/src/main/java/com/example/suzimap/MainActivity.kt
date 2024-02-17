package com.example.suzimap

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.google.maps.android.heatmaps.WeightedLatLng
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    private lateinit var mMap: GoogleMap
    private lateinit var heatmapTileProvider: HeatmapTileProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        with(mMap.uiSettings) {
            isZoomControlsEnabled = true
            isCompassEnabled = true
            isMyLocationButtonEnabled = true
            isMapToolbarEnabled = true
            isScrollGesturesEnabled = true
            isZoomGesturesEnabled = true
            isTiltGesturesEnabled = true
            isRotateGesturesEnabled = true
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            != android.content.pm.PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }

        mMap.isMyLocationEnabled = true
        // Example of adding a marker with an info window
        mMap.addMarker(
            MarkerOptions()
            .position(LatLng(19.0, 73.0))
            .title("Hot Women")
            .snippet("Test Category for SUZI development"))


        val defaultLocation = LatLng(19.0, 73.0)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 8f))

        addHeatmap()
    }

    private fun addHeatmap() {
        CoroutineScope(ioDispatcher).launch {
            val geoJsonData = fetchGeoJsonData("https://suzi-backend.onrender.com/gheatmap/test")
            val list = parseGeoJsonData(geoJsonData)

            withContext(Dispatchers.Main) {
                heatmapTileProvider = HeatmapTileProvider.Builder()
                    .weightedData(list)
                    .radius(50)
                    .opacity(1.0) // opacity of heatmap overlay
                    .build()


                mMap.addTileOverlay(com.google.android.gms.maps.model.TileOverlayOptions().tileProvider(heatmapTileProvider))
            }
        }
    }


    private suspend fun fetchGeoJsonData(url: String): String = withContext(ioDispatcher) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .build()
        val response = client.newCall(request).execute()
        return@withContext response.body?.string() ?: ""
    }

    private fun parseGeoJsonData(data: String): List<WeightedLatLng> {
        val list = ArrayList<WeightedLatLng>()
        val jsonArray = JSONArray(data)
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val lat = jsonObject.getDouble("latitude")
            val lng = jsonObject.getDouble("longitude")
            val rating = jsonObject.getDouble("rating")
            list.add(WeightedLatLng(LatLng(lat, lng), rating))
        }
        return list
    }
}

