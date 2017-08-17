package google_maps_library.listener;

import android.content.Context;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import google_maps_library.activity.GoogleMapsActivity;
import google_maps_library.utils.GoogleMapsConstants;

public class GoogleApiClientConnectionListener implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private Context context;

    public GoogleApiClientConnectionListener(Context context) {
        this.context = context;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(GoogleMapsConstants.TAG, "onConnected");

        ((GoogleMapsActivity) context).checkLocationPermissions();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(GoogleMapsConstants.TAG, "onConnectionSuspended: " + cause);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(GoogleMapsConstants.TAG, "onConnectionFailed: " + connectionResult);

        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult((GoogleMapsActivity) context, GoogleMapsConstants.RC_CONNECT);
            } catch (IntentSender.SendIntentException e) {
                Log.e(GoogleMapsConstants.TAG, "Unable to resolve connection issue", e);
                Snackbar.make(((GoogleMapsActivity) context).findViewById(android.R.id.content), "GMS result resolution failed, see log", Snackbar.LENGTH_LONG).show();
            }
        } else {
            Snackbar.make(((GoogleMapsActivity) context).findViewById(android.R.id.content), "GMS connection failed: " + connectionResult.getErrorCode(), Snackbar.LENGTH_LONG).show();
        }
    }
}
