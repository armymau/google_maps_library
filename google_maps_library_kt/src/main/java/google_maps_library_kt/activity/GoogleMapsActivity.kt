package google_maps_library_kt.activity

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.support.v7.app.AlertDialog
import android.util.Log
import armymau.it.google_maps_library.R
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*
import core_kt.activity.RuntimePermissionsActivity
import google_maps_library_kt.listener.GoogleApiClientConnectionListener
import google_maps_library_kt.utils.*

open abstract class GoogleMapsActivity : RuntimePermissionsActivity() {

    private lateinit var locationRequest: LocationRequest
    private lateinit var googleApiClient: GoogleApiClient
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            onLocationChanged(locationResult.lastLocation)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (googleApiClient.isConnected)
            googleApiClient.disconnect()
    }

    override fun onPause() {
        super.onPause()
        // Stop location updates to save battery, but don't disconnect the GoogleApiClient object.
        if (googleApiClient.isConnected)
            stopLocationUpdates()
    }

    override fun onStart() {
        super.onStart()
        if (!googleApiClient.isConnected)
            googleApiClient.connect()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initGoogleApi()
    }

    private fun initGoogleApi() {
        val googleApiClientConnectionListener = GoogleApiClientConnectionListener(this)
        googleApiClient = createLocationClient(this, googleApiClientConnectionListener)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        createLocationRequest()
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.create()

        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        locationRequest.interval = UPDATE_INTERVAL_IN_MILLISECONDS

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        locationRequest.fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS
    }

    @Synchronized
    private fun createLocationClient(context: Context, listener: GoogleApiClientConnectionListener): GoogleApiClient {
        return GoogleApiClient.Builder(context)
                .addConnectionCallbacks(listener)
                .addOnConnectionFailedListener(listener)
                .addApi(LocationServices.API)
                .build()
    }

    fun checkLocationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            super@GoogleMapsActivity.onRequestAppPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), R.string.core_runtime_permissions_txt, GOOGLE_MAPS_PERMISSION_REQUEST_LOCATION)
        } else {
            onPermissionsGranted(GOOGLE_MAPS_PERMISSION_REQUEST_LOCATION)
        }
    }

    override fun onPermissionsGranted(requestCode: Int) {
        if (requestCode == GOOGLE_MAPS_PERMISSION_REQUEST_LOCATION) {
            checkLocationManager()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult:$requestCode:$resultCode:$data")

        if (requestCode == RC_CONNECT) {
            handleGmsConnectionResult(resultCode)
        }
    }

    private fun handleGmsConnectionResult(resultCode: Int) {
        if (resultCode == Activity.RESULT_OK) {
            googleApiClient.connect()
        }
    }

    fun getLocationClient(): GoogleApiClient {
        return googleApiClient
    }

    fun checkLocationManager() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var gps_enabled = false
        var network_enabled = false

        try {
            gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        try {
            network_enabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        if (!gps_enabled && !network_enabled) {
            val dialog = AlertDialog.Builder(this)
            dialog.setMessage(resources.getString(R.string.gps_network_not_enabled))
            dialog.setPositiveButton(resources.getString(R.string.button_ok_label)) { _, _ -> startActivityForResult(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), OPEN_LOCATION_SETTINGS_REQUEST_CODE) }
            dialog.setNegativeButton(resources.getString(R.string.button_cancel_label)) { _, _ -> }
            dialog.show()
        } else {
            startLocationUpdates()
        }
    }

    @Throws(SecurityException::class)
    fun startLocationUpdates() {
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
    }

    private fun stopLocationUpdates() {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    private fun onLocationChanged(location: Location) {
        Log.e(TAG, "Location >>> latitude : " + location.latitude + " longitude : " + location.longitude)
        onLocationRetrieved(location)
    }

    abstract fun onLocationRetrieved(location: Location)
}