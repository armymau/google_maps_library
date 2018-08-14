package google_maps_library_kt.listener

import android.content.Context
import android.content.IntentSender
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.util.Log
import android.view.View
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import google_maps_library_kt.activity.GoogleMapsActivity
import google_maps_library_kt.utils.RC_CONNECT
import google_maps_library_kt.utils.TAG

class GoogleApiClientConnectionListener(var context: Context) : GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    override fun onConnected(bundle: Bundle?) {
        Log.d(TAG, "onConnected")
        (context as GoogleMapsActivity).checkLocationPermissions()
    }

    override fun onConnectionSuspended(cause: Int) {
        Log.d(TAG, "onConnectionSuspended: $cause")
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        Log.e(TAG, "onConnectionFailed: $connectionResult")

        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(context as GoogleMapsActivity, RC_CONNECT)
            } catch (e: IntentSender.SendIntentException) {
                Log.e(TAG, "Unable to resolve connection issue", e)
                Snackbar.make((context as GoogleMapsActivity).findViewById<View>(android.R.id.content), "GMS result resolution failed, see log", Snackbar.LENGTH_LONG).show()
            }

        } else {
            Snackbar.make((context as GoogleMapsActivity).findViewById<View>(android.R.id.content), "GMS connection failed: " + connectionResult.errorCode, Snackbar.LENGTH_LONG).show()
        }
    }
}