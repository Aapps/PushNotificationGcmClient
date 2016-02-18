package org.neurobin.aapps.pushnotificationgcmclient;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

/**
 * Created by jahid on 16/02/16.
 */
public class MyGCMRegistrationIntentService extends IntentService {

    private static final String TAG = "RegIntentService";
    private static final String[] TOPICS = {"global"};
    private static final int MAX_ATTEMPTS = 10;
    private static final int BACKOFF_MILLI_SECONDS = 500;
    private static final Random random = new Random();
    SharedPreferences sharedPreferences;

    public MyGCMRegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Intent registrationComplete = new Intent(GCMSharedPreferences.REGISTRATION_COMPLETE);
        registrationComplete.putExtra("prefix", "");
        String token = sharedPreferences.getString(GCMSharedPreferences.REG_ID, "");

        try {
            // [START register_for_gcm]
            // Initially this call goes out to the network to retrieve the token, subsequent calls
            // are local.
            // R.string.gcm_defaultSenderId (the Sender ID) is typically derived from google-services.json.
            // See https://developers.google.com/cloud-messaging/android/start for details on this file.
            // [START get_token]
            InstanceID instanceID = InstanceID.getInstance(this);

            if (!intent.getExtras().getBoolean("register")) {
                //client wants to un-register

                //instanceID.deleteInstanceID();  //This will delete the id i.e full un-register
                instanceID.deleteToken(getString(R.string.gcm_defaultSenderId),
                        GoogleCloudMessaging.INSTANCE_ID_SCOPE); //This will delete the token not the ID (partial)
                //delete from shared preferences
                //You would generally delete selectively
                //As this project is only about GCM notification, we can clear the sharedpreferences altogether
                sharedPreferences.edit().clear().apply();
                //un-register from server
                unregisterFromServer(token);

            } else {
                if (!intent.getExtras().getBoolean("tokenRefreshed") && !token.equals("")) {
                    //No need of retrieving token from GCM
                    registrationComplete.putExtra("prefix", "Old Token:\n");
                } else {
                    //retrieve a fresh token from GCM, it may or may not be the same as previous
                    token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                            GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
                    registrationComplete.putExtra("prefix", "Fresh Token:\n");
                }
                // [END get_token]
                Log.i(TAG, "GCM Registration Token: " + token);

                sharedPreferences.edit().putString(GCMSharedPreferences.REG_ID, token).apply();
                sharedPreferences.edit().putBoolean(GCMSharedPreferences.REG_SUCCESS, true).apply();

                // send registration request to server
                registerWithServer(token, instanceID);

                // Subscribe to topic channels
                //subscribeTopics(token);
                // [END register_for_gcm]
            }
        } catch (Exception e) {
            sharedPreferences.edit().putBoolean(GCMSharedPreferences.REG_SUCCESS, false).apply();
            Log.d(TAG, "Registration service failed.", e);
        }
        // Notify UI that task has completed, so the progress indicator can be hidden.
        if (intent.getExtras().getBoolean("register"))
            registrationComplete.putExtra("register", true);
        else
            registrationComplete.putExtra("register", false);
        LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
    }

    /**
     * Persist registration to third-party servers.
     * <p/>
     * Modify this method to associate the user's GCM registration token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void registerWithServer(String token, InstanceID instanceID) {

//        Log.i(TAG, "registering device (regId = " + regId + ")");
        String serverUrl = GCMCommonUtils.SERVER_URL + "register.php";
        Map<String, String> params = new HashMap<String, String>();
        params.put("regId", token);
        params.put("instanceId",instanceID.toString());
//        params.put("name", name);
//        params.put("email", email);

        long backoff = BACKOFF_MILLI_SECONDS + random.nextInt(1000);
        // Once GCM returns a registration id, we need to register on our server
        // As the server might be down, we will retry it a couple of
        // times.
        for (int i = 1; i <= MAX_ATTEMPTS; i++) {
//            Log.d(TAG, "Attempt #" + i + " to register");
            try {
                post(serverUrl, params);

                // You should store a boolean that indicates whether the generated token has been
                // sent to your server. If the boolean is false, send the token to your server,
                // otherwise your server should have already received the token.
                sharedPreferences.edit().putBoolean(GCMSharedPreferences.SENT_TOKEN_TO_SERVER, true).apply();
                return;
            } catch (IOException e) {
                Toast.makeText(this, "Failed to register...", Toast.LENGTH_SHORT).show();
                sharedPreferences.edit().putBoolean(GCMSharedPreferences.SENT_TOKEN_TO_SERVER, false).apply();
                // Here we are simplifying and retrying on any error; in a real
                // application, it should retry only on unrecoverable errors
                // (like HTTP error code 503).
//                Log.e(TAG, "Failed to register on attempt " + i + ":" + e);
                if (i == MAX_ATTEMPTS) {
                    break;
                }
                try {
//                    Log.d(TAG, "Sleeping for " + backoff + " ms before retry");
                    Thread.sleep(backoff);
                } catch (InterruptedException e1) {
                    // Activity finished before we complete - exit.
//                    Log.d(TAG, "Thread interrupted: abort remaining retries!");
                    Thread.currentThread().interrupt();
                    return;
                }
                // increase backoff exponentially
                backoff *= 2;
            }
        }
    }

    /**
     * Unregister this account/device pair within the server.
     */
    private void unregisterFromServer(final String regId) {
        // Log.i(TAG, "registering device (regId = " + regId + ")");
        String serverUrl = GCMCommonUtils.SERVER_URL + "unregister.php";
        Map<String, String> params = new HashMap<String, String>();
        params.put("regId", regId);

        long backoff = BACKOFF_MILLI_SECONDS + random.nextInt(1000);
        // Once GCM returns a registration id, we need to unregister on our server
        // As the server might be down, we will retry it a couple
        // times.
        for (int i = 1; i <= MAX_ATTEMPTS; i++) {
            try {
                post(serverUrl, params);
                sharedPreferences.edit().putBoolean(GCMSharedPreferences.SENT_TOKEN_TO_SERVER, true).apply();
                return;
            } catch (IOException e) {
                Toast.makeText(this, "Failed to un-register...", Toast.LENGTH_SHORT).show();
                sharedPreferences.edit().putBoolean(GCMSharedPreferences.SENT_TOKEN_TO_SERVER, false).apply();
                // Here we are simplifying and retrying on any error; in a real
                // application, it should retry only on unrecoverable errors
                // (like HTTP error code 503).
//                Log.e(TAG, "Failed to register on attempt " + i + ":" + e);
                if (i == MAX_ATTEMPTS) {
                    break;
                }
                try {
//                    Log.d(TAG, "Sleeping for " + backoff + " ms before retry");
                    Thread.sleep(backoff);
                } catch (InterruptedException e1) {
                    // Activity finished before we complete - exit.
//                    Log.d(TAG, "Thread interrupted: abort remaining retries!");
                    Thread.currentThread().interrupt();
                    return;
                }
                // increase backoff exponentially
                backoff *= 2;
            }
        }
//        String message = context.getString(R.string.server_register_error,
//                MAX_ATTEMPTS);
//        CommonUtilities.displayMessage(context, message);
    }

    /**
     * Issue a POST request to the server.
     *
     * @param endpoint POST address.
     * @param params   request parameters.
     * @throws IOException propagated from POST.
     */
    private static void post(String endpoint, Map<String, String> params)
            throws IOException {

        URL url;
        try {
            url = new URL(endpoint);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("invalid url: " + endpoint);
        }
        StringBuilder bodyBuilder = new StringBuilder();
        Iterator<Map.Entry<String, String>> iterator = params.entrySet().iterator();
        // constructs the POST body using the parameters
        while (iterator.hasNext()) {
            Map.Entry<String, String> param = iterator.next();
            bodyBuilder.append(param.getKey()).append('=')
                    .append(param.getValue());
            if (iterator.hasNext()) {
                bodyBuilder.append('&');
            }
        }
        String body = bodyBuilder.toString();
//        Log.v(TAG, "Posting '" + body + "' to " + url);
        byte[] bytes = body.getBytes();
        HttpURLConnection conn = null;
        try {
            Log.e("URL", "> " + url);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setFixedLengthStreamingMode(bytes.length);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded;charset=UTF-8");
            // post the request
            OutputStream out = conn.getOutputStream();
            out.write(bytes);
            out.close();
            // handle the response
            int status = conn.getResponseCode();
            if (status != 200) {
                throw new IOException("Post failed with error code " + status);
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * Subscribe to any GCM topics of interest, as defined by the TOPICS constant.
     *
     * @param token GCM token
     * @throws IOException if unable to reach the GCM PubSub service
     */
    // [START subscribe_topics]
    /*private void subscribeTopics(String token) throws IOException {
        GcmPubSub pubSub = GcmPubSub.getInstance(this);
        for (String topic : TOPICS) {
            pubSub.subscribe(token, "/topics/" + topic, null);
        }
    }*/
    // [END subscribe_topics]

}

