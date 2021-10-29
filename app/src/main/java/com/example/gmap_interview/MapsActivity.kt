package com.example.gmap_interview

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.*
import android.location.LocationListener
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.gmap_interview.databinding.ActivityMapsBinding
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.*


const val REQUEST_CODE = 1
//const val MIN_TIME = 1000L
//const val MIN_DISTANCE = 1.0

class MapsActivity : AppCompatActivity(),
    OnMapReadyCallback, LocationListener,
    ActivityCompat.OnRequestPermissionsResultCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var layout: View
    private lateinit var fusedLocProvider: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locManager: LocationManager
    private lateinit var binding: ActivityMapsBinding
    private val database = Firebase.database
    private val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION
        ,Manifest.permission.ACCESS_COARSE_LOCATION)


    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        layout = findViewById(R.id.layout)
//        database.getReference("interview app")

        if (checkSelfPermissionCompat(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED) {
            layout.showSnackbar("hi there", Snackbar.LENGTH_SHORT)
        } else {
            reqPermission()
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        locationStuff()
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.P)
    private fun locationStuff() {
        fusedLocProvider = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 2000
        locationRequest.fastestInterval = 1000
        val locationCallback: LocationCallback = object: LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult ?: return
                if (locationResult.locations.isNotEmpty()) {
                    val location = locationResult.lastLocation
                    Log.e("location", location.toString())
                    val addresses: List<Address>?
                    val geoCoder = Geocoder(applicationContext, Locale.getDefault())
                    addresses = geoCoder.getFromLocation(
                        locationResult.lastLocation.latitude,
                        locationResult.lastLocation.longitude,
                        1
                    )
                    if (addresses != null && addresses.isNotEmpty()) {
                        val address: String = addresses[0].getAddressLine(0)
                        val city: String = addresses[0].locality
                        val state: String = addresses[0].adminArea
                        val country: String = addresses[0].countryName
                        val postalCode: String = addresses[0].postalCode
                        val knownName: String = addresses[0].featureName
                        Log.e("location", "$address $city $state $postalCode $country $knownName")
                    }
                }
            }
        }
        fusedLocProvider.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper() /* Looper */
        )

        try {
            if (!locManager.isLocationEnabled) {
               Toast.makeText(this, "enable providers",Toast.LENGTH_SHORT).show()
            }
        } catch(e: Exception) {
            Toast.makeText(this, "err: $e", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                layout.showSnackbar("granted",Snackbar.LENGTH_SHORT)
            } else {
                layout.showSnackbar("R required",
                    Snackbar.LENGTH_INDEFINITE, "ok") {
                    requestPermissionsCompat(
                        this.permissions,
                        REQUEST_CODE)
                }
            }
        }
    }

    private fun reqPermission() {
        // Permission has not been granted and must be requested.
        if (shouldShowRequestPermissionRationaleCompat(Manifest.permission.ACCESS_FINE_LOCATION)) {
            layout.showSnackbar("R.string.camera_access_required",
                Snackbar.LENGTH_INDEFINITE, "ok") {
                requestPermissionsCompat(permissions,
                    REQUEST_CODE)
            }

        } else {
            layout.showSnackbar("not_available", Snackbar.LENGTH_SHORT)
            // Request the permission. The result will be received in onRequestPermissionResult().
            requestPermissionsCompat(permissions, REQUEST_CODE)
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions().position(sydney).title("for interview"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
    }

    override fun onLocationChanged(location: Location) {
        try {
            saveLocation(location)
        } catch (e: Exception) {
            Toast.makeText(this,"err: $e", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveLocation(location: Location) {
        val ref = database.getReference("test usr")
        ref.setValue(location)
    }
}