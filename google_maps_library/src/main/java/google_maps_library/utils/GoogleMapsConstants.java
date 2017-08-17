package google_maps_library.utils;

public class GoogleMapsConstants {

    public static final String TAG = ">>> GOOGLE MAPS LIBRARY";
    public static final boolean isDebug = false;
    public static final boolean isSigned = true;

    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = /* 12000; */ 60000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    public static final int GOOGLE_MAPS_PERMISSION_REQUEST_LOCATION = 500;
    public static final int OPEN_LOCATION_SETTINGS_REQUEST_CODE = 44;
    public static final int RC_CONNECT = 300;
}
