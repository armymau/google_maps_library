package google_maps_library.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import armymau.it.google_maps_library.R;
import core.utils.CoreConstants;
import google_maps_library.utils.GoogleMapsConstants;

public abstract class GoogleMapsFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private GoogleApiClient locationClient;
    private LocationRequest mLocationRequest;

    @Override
    public void onPause() {
        super.onPause();
        // Stop location updates to save battery, but don't disconnect the GoogleApiClient object.
        if (locationClient != null && locationClient.isConnected())
            stopLocationUpdates();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(locationClient != null && locationClient.isConnected())
            locationClient.disconnect();
    }

    @Override
    public void onStart() {
        super.onStart();
        if(locationClient != null && !locationClient.isConnected())
            locationClient.connect();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initGoogleApi();
    }

    private void initGoogleApi() {
        locationClient = createLocationClient(getActivity());
        createLocationRequest();
    }

    private synchronized GoogleApiClient createLocationClient(Context context) {
        return new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
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

    public void startLocationUpdates() throws SecurityException {
        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        if(locationClient != null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(locationClient, mLocationRequest, this);
        }
    }

    private void stopLocationUpdates() {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        if(locationClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(locationClient, this);
        }
    }

    private void handleGmsConnectionResult(int resultCode) {
        if (resultCode == Activity.RESULT_OK) {
            if(locationClient != null) {
                locationClient.connect();
            }
        }
    }

    public void checkLocationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            GoogleMapsFragment.super.requestPermissions(new String[] {
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, GoogleMapsConstants.GOOGLE_MAPS_PERMISSION_REQUEST_LOCATION);
        } else {
            onPermissionsGranted(GoogleMapsConstants.GOOGLE_MAPS_PERMISSION_REQUEST_LOCATION);
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        int permissionCheck = PackageManager.PERMISSION_GRANTED;
        for (int permission : grantResults) {
            permissionCheck = permissionCheck + permission;
        }
        if ((grantResults.length > 0) && permissionCheck == PackageManager.PERMISSION_GRANTED) {
            onPermissionsGranted(requestCode);
        } else {
            Snackbar.make(getActivity().findViewById(android.R.id.content), this.getResources().getString(armymau.it.core_library.R.string.core_runtime_permissions_settings_txt), Snackbar.LENGTH_INDEFINITE).setAction(armymau.it.core_library.R.string.core_application_settings, this.onSnackbarPermissionsResult).show();
        }
    }

    private View.OnClickListener onSnackbarPermissionsResult = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setData(Uri.parse("package:" + getActivity().getPackageName()));
            startActivityForResult(intent, CoreConstants.CHECK_PERMISSIONS_REQUEST_CODE);
        }
    };

    private void onPermissionsGranted(int requestCode) {
        if(requestCode == GoogleMapsConstants.GOOGLE_MAPS_PERMISSION_REQUEST_LOCATION) {
            checkLocationManager();
        }
    }

    public GoogleApiClient getLocationClient() {
        return locationClient;
    }

    public void checkLocationManager() {
        LocationManager lm = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false; boolean network_enabled = false;
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {}

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception ex) {}

        if (!gps_enabled && !network_enabled) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
            dialog.setMessage(getActivity().getResources().getString(R.string.gps_network_not_enabled));
            dialog.setPositiveButton(getActivity().getResources().getString(R.string.button_ok_label), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    getActivity().startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), GoogleMapsConstants.OPEN_LOCATION_SETTINGS_REQUEST_CODE);
                }
            });
            dialog.setNegativeButton(getActivity().getResources().getString(R.string.button_cancel_label), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                }
            });
            dialog.show();
        } else {
            startLocationUpdates();
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


    /* GoogleApiClient.ConnectionCallbacks */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(GoogleMapsConstants.TAG, "onConnected");
        checkLocationPermissions();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(GoogleMapsConstants.TAG, "onConnectionSuspended: " + cause);
    }
    //***************************************************


    /* GoogleApiClient.OnConnectionFailedListener */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(GoogleMapsConstants.TAG, "onConnectionFailed: " + connectionResult);

        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(getActivity(), GoogleMapsConstants.RC_CONNECT);
            } catch (IntentSender.SendIntentException e) {
                Log.e(GoogleMapsConstants.TAG, "Unable to resolve connection issue", e);
                Snackbar.make(getActivity().findViewById(android.R.id.content), "GMS result resolution failed, see log", Snackbar.LENGTH_LONG).show();
            }
        } else {
            Snackbar.make((getActivity().findViewById(android.R.id.content)), "GMS connection failed: " + connectionResult.getErrorCode(), Snackbar.LENGTH_LONG).show();
        }
    }
    //***************************************************


    /* LocationListener */
    @Override
    public void onLocationChanged(Location location) {
        Log.d(GoogleMapsConstants.TAG, "Location >>> latitude : " + location.getLatitude() + " longitude : " + location.getLongitude());
        onLocationRetrieved(location);
    }
    //***************************************************


    public abstract void onLocationRetrieved(Location location);
}
