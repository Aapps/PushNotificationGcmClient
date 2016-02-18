package org.neurobin.aapps.pushnotificationgcmclient;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

/**
 * Created by jahid on 16/02/16.
 */
public class GCMCommonUtils {

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    // put your server registration url here, must end with a /
    public static final String SERVER_URL = "https://neurobin.org/api/android/gcm/gcm-server-demo/";

    public static String notificationType[] = {"default", "type1"};

    /**
     * Tag used on log messages.
     */
    static final String TAG = "androidpushnotification";

    public static final String DISPLAY_MESSAGE_ACTION =
            "org.neurobin.aapps.pushnotificationclient";

    static final String EXTRA_MESSAGE = "message";

    static void displayMessage(Context context, String message) {
        Intent intent = new Intent(DISPLAY_MESSAGE_ACTION);
        intent.putExtra(EXTRA_MESSAGE, message);
        context.sendBroadcast(intent);
    }


    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    public static boolean checkPlayServices(Activity activity) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(activity);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(activity, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported.");
                activity.finish();
            }
            return false;
        }
        return true;
    }

}
