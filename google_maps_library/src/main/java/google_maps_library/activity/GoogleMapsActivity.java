package google_maps_library.activity;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.Builder;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import armymau.it.google_maps_library.R;
import core.activity.RuntimePermissionsActivity;
import google_maps_library.callback.LocationResultCallback;
import google_maps_library.listener.GoogleApiClientConnectionListener;
import google_maps_library.utils.GoogleMapsConstants;

public abstract class GoogleMapsActivity extends RuntimePermissionsActivity {

    private GoogleApiClient locationClient;
    private LocationRequest mLocationRequest;
    private LocationResultCallback callback;

    @Override
    protected void onPause() {
        super.onPause();
        // Stop location updates to save battery, but don't disconnect the GoogleApiClient object.
        if (locationClient != null && callback != null && locationClient.isConnected())
            stopLocationUpdates();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(locationClient != null && locationClient.isConnected())
            locationClient.disconnect();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(locationClient != null && !locationClient.isConnected())
            locationClient.connect();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initGoogleApi();
    }

    private void initGoogleApi() {
        GoogleApiClientConnectionListener googleApiClientConnectionListener = new GoogleApiClientConnectionListener(this);
        locationClient = createLocationClient(this, googleApiClientConnectionListener);
        createLocationRequest();
    }

    private synchronized GoogleApiClient createLocationClient(Context context, GoogleApiClientConnectionListener listener) {
        return new Builder(context)
                .addConnectionCallbacks(listener)
                .addOnConnectionFailedListener(listener)
                .addApi(LocationServices.API)
                .build();
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(GoogleMapsConstants.UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(GoogleMapsConstants.FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    public void checkLocationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            GoogleMapsActivity.super.onRequestAppPermissions(new String[] {
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, R.string.core_runtime_permissions_txt, GoogleMapsConstants.GOOGLE_MAPS_PERMISSION_REQUEST_LOCATION);
        } else {
            onPermissionsGranted(GoogleMapsConstants.GOOGLE_MAPS_PERMISSION_REQUEST_LOCATION);
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode) {
        if(requestCode == GoogleMapsConstants.GOOGLE_MAPS_PERMISSION_REQUEST_LOCATION) {
            callback = new LocationResultCallback(this);
            checkLocationManager();
        }
    }

    public void checkLocationManager() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false; boolean network_enabled = false;
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {}

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception ex) {}

        if (!gps_enabled && !network_enabled) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setMessage(getResources().getString(R.string.gps_network_not_enabled));
            dialog.setPositiveButton(getResources().getString(R.string.button_ok_label), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), GoogleMapsConstants.OPEN_LOCATION_SETTINGS_REQUEST_CODE);
                }
            });
            dialog.setNegativeButton(getResources().getString(R.string.button_cancel_label), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                }
            });
            dialog.show();
        } else {
            startLocationUpdates();
        }
    }

    public void startLocationUpdates() throws SecurityException {
        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        if(locationClient != null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(locationClient, mLocationRequest, callback);
        }
    }

    private void stopLocationUpdates() {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        if(locationClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(locationClient, callback);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(GoogleMapsConstants.TAG, "onActivityResult:" + requestCode + ":" + resultCode + ":" + data);

        if (requestCode == GoogleMapsConstants.RC_CONNECT) {
            handleGmsConnectionResult(resultCode);
        }
    }

    private void handleGmsConnectionResult(int resultCode) {
        if (resultCode == RESULT_OK) {
            if(locationClient != null) {
                locationClient.connect();
            }
        }
    }

    public GoogleApiClient getLocationClient() {
        return locationClient;
    }

    public abstract void onLocationRetrieved(Location location);
}
