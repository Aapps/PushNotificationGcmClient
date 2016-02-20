*******************************************
GCM Client app implementation for Android.
*******************************************
For a complete tutorial on how you can make use of this project visit [this link](https://neurobin.org/docs/android/push-notification-gcm-client-server/).
#Features:

1. Client app will register with GCM using an intent service.
2. It will provide users the ability to retry/refresh the registration/un-registration with GCM server.
3. It will implement registration/un-registration to your own server using an exponential back-off (a retry mechanism).
4. It will use shared preferences to store some necessary information.
5. By default, if a previous registration id is available, it will use that id from shared prefs instead of a fresh retrieval.
6. The unregister button will unregister the id from both GCM server and your own notification server.
7. The refresh button will retrieve a fresh id from GCM server and register it to your own notification server.
8. It will be able to handle multiple types of notifications and trigger different events accordingly.

<span id="java-classes"></span>
#Java Classes at a glance:

1. **GCMCommonUtils:** This class includes some commonly used variables and methods.
2. **GCMSharedPreferences:** This class holds the registration id in shared preferences and also some other values used throughout the project.
3. **MyGcmListenerService:** It's an intent service which runs in the background (always) for the project and responsible for getting the notification and triggering different events accordingly.
4. **MyGCMRegistrationIntentService:** It's another intent service which is responsible for registering/un-registering the device with both GCM and the notification server. It detects whether to register or un-register by evaluating the Extra value put for the intent service with `putExtra()` method.
5. **MyInstanceIDListenerService:** An extra class which is not used in the sample project but provided for further digging. You can make use of it if you want. It can be used to handle token refresh (I am using a different but easier method to refresh tokens).
6. **PushNotificationMainActivity:** This is the main activity. Includes a `BroadcastReceiver` to handle messages from the intent service `MyGCMRegistrationIntentService`.
7. **ScrollingActivity:** A dummy activity to show an example of handling multiple types of notifications.

<span id="java-methods"></span>
#Java methods at a glance:

1. `checkPlayServices(Activity activity)`: Check play services API availability and install/update if necessary. Defined in *GCMCommonUtils* class.
2. `onMessageReceived(String from, Bundle data)`: Override method in *MyGcmListenerService*. Handles messages received through push notification.
3. `sendNotification(String message, String type)`: Generates notification on the device with the message and triggers an event if clicked from the notification area in device according to the type specified.
4. `onHandleIntent(Intent intent)`: Override method in *MyGCMRegistrationIntentService*, handles registration intent, registers or unregisters with GCM and server.
5. `registerWithServer(String token, InstanceID instanceID)`: Defined in *MyGCMRegistrationIntentService*. Sends the token and the instanceID (string) to server in order to register with the server. It employs an exponential backoff mechanism to retry in case of failure.
6. `unregisterFromServer(final String regId)`: Defined in *MyGCMRegistrationIntentService*. Sends the token/regid to server and unregisters the device for the server. It employs an exponential backoff mechanism to retry in case of failure.
7. `post(String endpoint, Map<String, String> params)`: Defined in *MyGCMRegistrationIntentService*. It triggers an HTTP post request.
8. `startRegistrationService(boolean reg, boolean tkr)`: Defined in *PushNotificationMainActivity*. If `reg` is `true`, it tries to register, if `false` it tries to un-register. If `tkr` is `true`, a fresh token is retrieved, otherwise old token from shared preferences is used.
