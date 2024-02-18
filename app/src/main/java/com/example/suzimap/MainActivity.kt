package com.example.suzimap

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.google.maps.android.heatmaps.WeightedLatLng
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private lateinit var mMap: GoogleMap
    private lateinit var heatmapTileProvider: HeatmapTileProvider
    private lateinit var mapView: com.google.android.gms.maps.MapView // Initialize mapView here


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        mapView = findViewById(R.id.mapView) // Correctly initialize mapView
        mapView.onCreate(savedInstanceState) // Important to call this
        mapView.getMapAsync(this)

        val changeMapType: ImageButton = findViewById(R.id.btnChangeMapType)
        var currentMapType = GoogleMap.MAP_TYPE_NORMAL

        changeMapType.setOnClickListener {
            currentMapType = when (currentMapType) {
                GoogleMap.MAP_TYPE_NORMAL -> GoogleMap.MAP_TYPE_SATELLITE
                GoogleMap.MAP_TYPE_SATELLITE -> GoogleMap.MAP_TYPE_TERRAIN
                GoogleMap.MAP_TYPE_TERRAIN -> GoogleMap.MAP_TYPE_HYBRID
                GoogleMap.MAP_TYPE_HYBRID -> GoogleMap.MAP_TYPE_NORMAL
                else -> GoogleMap.MAP_TYPE_NORMAL
            }
            mMap.mapType = currentMapType
        }
    }

    // Implement lifecycle methods
    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    override fun onStop() {
        mapView.onStop()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        mapView.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap=googleMap
        mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE

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
        // example of adding a marker with an info window
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
            try {
                val geoJsonData = fetchGeoJsonData("https://suzi-backend.onrender.com/gheatmap/test")
                val list = parseGeoJsonData(geoJsonData)

                withContext(Dispatchers.Main) {
                    heatmapTileProvider = HeatmapTileProvider.Builder()
                        .weightedData(list)
                        .radius(50)
                        .opacity(1.0)
                        .build()

                    mMap.addTileOverlay(com.google.android.gms.maps.model.TileOverlayOptions().tileProvider(heatmapTileProvider))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Network timeout, please try again.", Toast.LENGTH_LONG).show()
                }
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

