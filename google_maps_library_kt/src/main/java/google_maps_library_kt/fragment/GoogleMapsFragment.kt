package google_maps_library_kt.fragment

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View
import armymau.it.google_maps_library.R
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*
import core_kt.utils.CHECK_PERMISSIONS_REQUEST_CODE
import google_maps_library_kt.utils.*

abstract class GoogleMapsFragment : Fragment(), GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    lateinit var googleApiClient: GoogleApiClient
    lateinit var locationRequest: LocationRequest
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            onLocationChanged(locationResult.lastLocation)
        }
    }

    override fun onPause() {
        super.onPause()
        // Stop location updates to save battery, but don't disconnect the GoogleApiClient object.
        if (googleApiClient.isConnected)
            stopLocationUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (googleApiClient.isConnected)
            googleApiClient.disconnect()
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
        googleApiClient = createLocationClient(activity)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity!!)
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

    private fun handleGmsConnectionResult(resultCode: Int) {
        if (resultCode == Activity.RESULT_OK) {
            googleApiClient.connect()
        }
    }

    fun checkLocationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            super@GoogleMapsFragment.requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), GOOGLE_MAPS_PERMISSION_REQUEST_LOCATION)
        } else {
            onPermissionsGranted(GOOGLE_MAPS_PERMISSION_REQUEST_LOCATION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        var permissionCheck = PackageManager.PERMISSION_GRANTED
        for (permission in grantResults) {
            permissionCheck += permission
        }
        if (grantResults.isNotEmpty() && permissionCheck == PackageManager.PERMISSION_GRANTED) {
            onPermissionsGranted(requestCode)
        } else {
            Snackbar.make(activity!!.findViewById(android.R.id.content), this.resources.getString(armymau.it.core_library.R.string.core_runtime_permissions_settings_txt), Snackbar.LENGTH_INDEFINITE).setAction(armymau.it.core_library.R.string.core_application_settings, this.onSnackbarPermissionsResult).show()
        }
    }

    private val onSnackbarPermissionsResult = View.OnClickListener {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        intent.data = Uri.parse("package:" + activity!!.packageName)
        startActivityForResult(intent, CHECK_PERMISSIONS_REQUEST_CODE)
    }

    private fun onPermissionsGranted(requestCode: Int) {
        if (requestCode == GOOGLE_MAPS_PERMISSION_REQUEST_LOCATION) {
            checkLocationManager()
        }
    }

    fun checkLocationManager() {
        val locationManager = activity!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager
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
            val dialog = AlertDialog.Builder(activity!!)
            dialog.setMessage(activity!!.resources.getString(R.string.gps_network_not_enabled))
            dialog.setPositiveButton(activity!!.resources.getString(R.string.button_ok_label)) { _, _ -> activity!!.startActivityForResult(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), OPEN_LOCATION_SETTINGS_REQUEST_CODE) }
            dialog.setNegativeButton(activity!!.resources.getString(R.string.button_cancel_label)) { _, _ -> }
            dialog.show()
        } else {
            startLocationUpdates()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult:$requestCode:$resultCode:$data")

        if (requestCode == RC_CONNECT) {
            handleGmsConnectionResult(resultCode)
        }
    }


    /* GoogleApiClient.ConnectionCallbacks */
    override fun onConnected(bundle: Bundle?) {
        Log.d(TAG, "onConnected")
        checkLocationPermissions()
    }

    override fun onConnectionSuspended(cause: Int) {
        Log.d(TAG, "onConnectionSuspended: $cause")
    }
    //***************************************************


    /* GoogleApiClient.OnConnectionFailedListener */
    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        Log.e(TAG, "onConnectionFailed: $connectionResult")

        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(activity, RC_CONNECT)
            } catch (e: IntentSender.SendIntentException) {
                Log.e(TAG, "Unable to resolve connection issue", e)
                Snackbar.make(activity!!.findViewById(android.R.id.content), "GMS result resolution failed, see log", Snackbar.LENGTH_LONG).show()
            }

        } else {
            Snackbar.make(activity!!.findViewById(android.R.id.content), "GMS connection failed: " + connectionResult.errorCode, Snackbar.LENGTH_LONG).show()
        }
    }
    //***************************************************


    fun getLocationClient(): GoogleApiClient? {
        return googleApiClient
    }

    @Synchronized
    private fun createLocationClient(context: Context?): GoogleApiClient {
        return GoogleApiClient.Builder(context!!)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build()
    }



    private fun onLocationChanged(location: Location) {
        Log.d(TAG, "Location >>> latitude : " + location.latitude + " longitude : " + location.longitude)
        onLocationRetrieved(location)
    }

    abstract fun onLocationRetrieved(location: Location)
}