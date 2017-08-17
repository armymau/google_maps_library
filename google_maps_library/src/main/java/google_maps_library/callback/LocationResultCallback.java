package google_maps_library.callback;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.location.LocationListener;

import google_maps_library.activity.GoogleMapsActivity;
import google_maps_library.utils.GoogleMapsConstants;

public class LocationResultCallback implements LocationListener {

    private final Context context;

    public LocationResultCallback(Context context) {
        this.context = context;
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(GoogleMapsConstants.TAG, "Location >>> latitude : " + location.getLatitude() + " longitude : " + location.getLongitude());
        ((GoogleMapsActivity) context).onLocationRetrieved(location);
    }
}
