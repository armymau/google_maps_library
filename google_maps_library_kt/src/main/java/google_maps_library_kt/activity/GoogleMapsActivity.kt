package google_maps_library_kt.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import armymau.it.google_maps_library.R
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import core_kt.activity.RuntimePermissionsActivity
import google_maps_library_kt.utils.*

open abstract class GoogleMapsActivity : RuntimePermissionsActivity() {

    private lateinit var locationRequest: LocationRequest
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            onLocationChanged(locationResult.lastLocation)
        }
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    @SuppressLint("MissingSuperCall")
    override fun onCreate(savedInstanceState: Bundle?) {
        initGoogleApi()
    }

    private fun initGoogleApi() {
        val builder = LocationSettingsRequest.Builder()
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        createLocationRequest()

        task.addOnSuccessListener { locationSettingsResponse ->
            // All location settings are satisfied. The client can initialize
            // location requests here.
            startLocationUpdates()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(this@GoogleMapsActivity, REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.create().apply {
            // Sets the desired interval for active location updates. This interval is
            // inexact. You may not receive updates at all if no location sources are available, or
            // you may receive them slower than requested. You may also receive updates faster than
            // requested if other applications are requesting location at a faster interval.
            interval = UPDATE_INTERVAL_IN_MILLISECONDS
            // Sets the fastest rate for active location updates. This interval is exact, and your
            // application will never receive updates faster than this value.
            fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS

            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
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

    private fun checkLocationManager() {
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