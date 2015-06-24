/**Ford Motor Company
 * September 2012
 * Elizabeth Halash
 */

package com.wcities.eventseeker.applink.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.ford.syncV4.exception.SyncException;
import com.ford.syncV4.exception.SyncExceptionCause;
import com.ford.syncV4.proxy.SyncProxyALM;
import com.ford.syncV4.proxy.interfaces.IProxyListenerALM;
import com.ford.syncV4.proxy.rpc.AddCommandResponse;
import com.ford.syncV4.proxy.rpc.AddSubMenuResponse;
import com.ford.syncV4.proxy.rpc.AlertResponse;
import com.ford.syncV4.proxy.rpc.ChangeRegistrationResponse;
import com.ford.syncV4.proxy.rpc.CreateInteractionChoiceSetResponse;
import com.ford.syncV4.proxy.rpc.DeleteCommandResponse;
import com.ford.syncV4.proxy.rpc.DeleteFileResponse;
import com.ford.syncV4.proxy.rpc.DeleteInteractionChoiceSetResponse;
import com.ford.syncV4.proxy.rpc.DeleteSubMenuResponse;
import com.ford.syncV4.proxy.rpc.DiagnosticMessageResponse;
import com.ford.syncV4.proxy.rpc.EndAudioPassThruResponse;
import com.ford.syncV4.proxy.rpc.GenericResponse;
import com.ford.syncV4.proxy.rpc.GetDTCsResponse;
import com.ford.syncV4.proxy.rpc.GetVehicleDataResponse;
import com.ford.syncV4.proxy.rpc.ListFilesResponse;
import com.ford.syncV4.proxy.rpc.OnAudioPassThru;
import com.ford.syncV4.proxy.rpc.OnButtonEvent;
import com.ford.syncV4.proxy.rpc.OnButtonPress;
import com.ford.syncV4.proxy.rpc.OnCommand;
import com.ford.syncV4.proxy.rpc.OnDriverDistraction;
import com.ford.syncV4.proxy.rpc.OnHMIStatus;
import com.ford.syncV4.proxy.rpc.OnHashChange;
import com.ford.syncV4.proxy.rpc.OnKeyboardInput;
import com.ford.syncV4.proxy.rpc.OnLanguageChange;
import com.ford.syncV4.proxy.rpc.OnLockScreenStatus;
import com.ford.syncV4.proxy.rpc.OnPermissionsChange;
import com.ford.syncV4.proxy.rpc.OnSystemRequest;
import com.ford.syncV4.proxy.rpc.OnTBTClientState;
import com.ford.syncV4.proxy.rpc.OnTouchEvent;
import com.ford.syncV4.proxy.rpc.OnVehicleData;
import com.ford.syncV4.proxy.rpc.PerformAudioPassThruResponse;
import com.ford.syncV4.proxy.rpc.PerformInteractionResponse;
import com.ford.syncV4.proxy.rpc.PutFileResponse;
import com.ford.syncV4.proxy.rpc.ReadDIDResponse;
import com.ford.syncV4.proxy.rpc.ResetGlobalPropertiesResponse;
import com.ford.syncV4.proxy.rpc.ScrollableMessageResponse;
import com.ford.syncV4.proxy.rpc.SetAppIconResponse;
import com.ford.syncV4.proxy.rpc.SetDisplayLayoutResponse;
import com.ford.syncV4.proxy.rpc.SetGlobalPropertiesResponse;
import com.ford.syncV4.proxy.rpc.SetMediaClockTimerResponse;
import com.ford.syncV4.proxy.rpc.ShowResponse;
import com.ford.syncV4.proxy.rpc.SliderResponse;
import com.ford.syncV4.proxy.rpc.SpeakResponse;
import com.ford.syncV4.proxy.rpc.SubscribeButtonResponse;
import com.ford.syncV4.proxy.rpc.SubscribeVehicleDataResponse;
import com.ford.syncV4.proxy.rpc.SyncMsgVersion;
import com.ford.syncV4.proxy.rpc.SystemRequestResponse;
import com.ford.syncV4.proxy.rpc.UnsubscribeButtonResponse;
import com.ford.syncV4.proxy.rpc.UnsubscribeVehicleDataResponse;
import com.ford.syncV4.proxy.rpc.enums.ButtonName;
import com.ford.syncV4.proxy.rpc.enums.DriverDistractionState;
import com.ford.syncV4.proxy.rpc.enums.Language;
import com.ford.syncV4.proxy.rpc.enums.LockScreenStatus;
import com.ford.syncV4.proxy.rpc.enums.Result;
import com.ford.syncV4.proxy.rpc.enums.SyncDisconnectedReason;
import com.ford.syncV4.proxy.rpc.enums.TriggerSource;
import com.ford.syncV4.proxy.rpc.enums.VehicleDataResultCode;
import com.ford.syncV4.transport.TCPTransportConfig;
import com.wcities.eventseeker.BaseActivity;
import com.wcities.eventseeker.LockScreenActivity;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.applink.handler.DiscoverAL;
import com.wcities.eventseeker.applink.handler.ESIProxyALM;
import com.wcities.eventseeker.applink.handler.MainAL;
import com.wcities.eventseeker.applink.handler.MyEventsAL;
import com.wcities.eventseeker.applink.handler.SearchAL;
import com.wcities.eventseeker.applink.util.ALUtil;
import com.wcities.eventseeker.applink.util.CommandsUtil.Command;
import com.wcities.eventseeker.applink.util.InteractionChoiceSetUtil;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.constants.Enums;
import com.wcities.eventseeker.logger.Logger;
import com.wcities.eventseeker.util.DeviceUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

public class AppLinkService extends Service implements IProxyListenerALM {

    private static final String TAG = AppLinkService.class.getSimpleName();

    public static final int CMD_ID_AL = 1;
    public static final int SPEAK_CHAR_LIMIT = 499;

    //variable used to increment correlation ID for every request sent to SYNC
    public int autoIncCorrId = 0;

    /**
     * In this list we have added 2nd level commands which user can continue with after location changed.
     */
    private List<Command> secLevelCmd;
    //variable to contain the current state of the service
    private static AppLinkService instance = null;
    //variable to contain the current state of the main UI ACtivity
    private BaseActivity currentUIActivity;
    //variable to access the BluetoothAdapter
    private BluetoothAdapter mBtAdapter;
    //variable to create and call functions of the SyncProxy
    private SyncProxyALM proxy = null;
    //variable that keeps track of whether SYNC is sending driver distractions
    //(older versions of SYNC will not send this notification)
    private boolean driverDistractionNotif = false;
    //variable to contain the current state of the lockscreen
    private boolean lockscreenUP = false;
    private ESIProxyALM esIProxyALM;
    private boolean isHMIStatusNone, isFordGPSAvailable, isAlertCurrentlyVisible;

    /**
     * This flag is used to notify if DD mode is OFF or ON. In similar way it also notifies if Lockscreen is ON or OFF.
     */
    private boolean isDDOff;
    /**
     * This flag is introduced to notify whether lat-lon changed.
     * Issues & fixes:
     * 1) When user in DD_OFF mode & changes location. After location changed if user continue
     * with any 2nd level commands, then the execution goes in 'isDDOFF section check' of HMI_FULL
     * where it updates lat-lon & set this flag to true & bcoz of this it will update lat-lon in onCommandPress() method.
     * 2) When user in DD_OFF mode & Changes location. After location changed, if user presses DD button again(To make DD_ON)
     * then lat-lon wasn't updating in ApplinkService. So to notify, we set this flag to true in DD_ON mode
     * of onOnDriverDistraction().
     */
    private boolean isLatLngUpdatedInDdOffMode;

    private double lat = AppConstants.NOT_ALLOWED_LAT, lng = AppConstants.NOT_ALLOWED_LON;

    public static AppLinkService getInstance() {
        return instance;
    }

    public SyncProxyALM getProxy() {
        return proxy;
    }

    public BaseActivity getCurrentActivity() {
        return currentUIActivity;
    }

    public void setCurrentActivity(BaseActivity currentActivity) {
        this.currentUIActivity = currentActivity;
    }

    public void resetCurrentActivityFor(BaseActivity currentActivity) {
        /**
         * reset to null only if it's called by same activity which had called setCurrentBaseActivity() last
         * time, because otherwise it's possible that activity A starts activity B where onStart() of B gets
         * called up first followed by onStop() of A, resulting in first currentBaseActivity set to B & then
         * to null by A, which we don't want, since B is right value in this case; whereas if last activity's
         * onStop() is getting called up then in that case this method will rightly set currentBaseActivity
         * to null.
         */
        if (currentActivity == this.currentUIActivity) {
            this.currentUIActivity = null;
        }
    }

    public ESIProxyALM getESIProxyListener() {
        return esIProxyALM;
    }

    public void setESIProxyListener(ESIProxyALM esIProxyListener) {
        this.esIProxyALM = esIProxyListener;
    }

    public void onCreate() {
        super.onCreate();
        //Log.d(TAG, "onCreate()");
        secLevelCmd = new ArrayList<Command>();
        for (Command cmd : Command.values()) {
            if (cmd.isSecLevelCmd()) {
                secLevelCmd.add(cmd);
            }
        }

        instance = this;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        /**
         * if 'intent is null or intent.getAction() is null' is added to avoid NullPointerException,
         * as once found crash over here.
         */
        if (intent == null || (intent != null && intent.getAction() == null)) {
            if (AppConstants.DEBUG) {
                startProxy();

            } else {
                //Log.d(TAG, "onStartCommand(), Non-debug mode");
                if (intent != null) {
                    //Log.d(TAG, "intent != null");
                    mBtAdapter = BluetoothAdapter.getDefaultAdapter();
                    if (mBtAdapter != null) {
                        //Log.d(TAG, "mBtAdapter != null");
                        if (mBtAdapter.isEnabled()) {
                            //Log.d(TAG, "mBtAdapter is Enabled");
                            startProxy();
                        }
                    }
                }
            }

            if (EventSeekr.getCurrentBaseActivity() != null) {
                setCurrentActivity(EventSeekr.getCurrentBaseActivity());
            }

        } else if (intent.getAction().equals(AppConstants.ACTION_APPLINK_SERVICE_START_FOREGROUND)) {
            Intent activityIntent = new Intent(this, LockScreenActivity.class);
            activityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, activityIntent, 0);

            /**
             * 21-04-2015:
             * Issue: In Sony Xperia L, Before adding the below code to make this Service as Foreground
             * when the service gets started and if we leave the app in background and start using other apps
             * (mostly happens with chrome) then because of Low Memory the application gets killed and thus
             * the Service gets killed and the Connection with Applink get disconnected. So, to avoid this issue
             * we are making this Service to run as Foreground process.
             * This code will make this service to run in foreground and it is added
             */
            Notification notification = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_notification_white)
                    .setColor(getResources().getColor(R.color.colorPrimary))
                    .setContentTitle("Eventseeker")
                    .setContentText("connected to Ford SYNC")
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .build();

            startForeground(AppConstants.FOREGROUND_SERVICE_NOTIFICATION_ID, notification);

        } else if (intent.getAction().equals(AppConstants.ACTION_APPLINK_SERVICE_STOP_FOREGROUND)) {
            stopForeground(true);
        }
        return START_STICKY;
    }

    public void startProxy() {
        Log.d(TAG, "startProxy() proxy: " + proxy);
        if (proxy == null) {
            try {
                if (AppConstants.DEBUG) {
                    //Log.d(TAG, "startProxy with TCP");
                    proxy = new SyncProxyALM(this, getResources().getString(R.string.ford_app_name), true,
                            AppConstants.FORD_APP_ID, new TCPTransportConfig(AppConstants.TCP_PORT,
                            AppConstants.TCP_IP_ADDRESS, true));

                } else {
                    Language language = ((EventSeekr)getApplication()).getFordLocale().getFordLanguage();
                    SyncMsgVersion syncMsgVersion = new SyncMsgVersion();
                    syncMsgVersion.setMajorVersion(1);
                    syncMsgVersion.setMinorVersion(1);
                    proxy = new SyncProxyALM(this, getResources().getString(R.string.ford_app_name), null,
                            new Vector<String>(Arrays.asList(new String[] {getResources().getString(
                                    R.string.app_name)})), true, syncMsgVersion, language, language,
                            AppConstants.FORD_APP_ID, null);
                    //Log.d(TAG, "startProxy() registration done");
                }

            } catch (SyncException e) {
                e.printStackTrace();
                //error creating proxy, returned proxy = null
                if (proxy == null) {
                    Log.d(TAG, "startProxy() stopSelf()");
                    stopSelf();
                }
            }
        }
    }

    public boolean isAlertCurrentlyVisible() {
        return isAlertCurrentlyVisible;
    }

    public void setIsAlertCurrentlyVisible(boolean isAlertCurrentlyVisible) {
        this.isAlertCurrentlyVisible = isAlertCurrentlyVisible;
    }

    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        //unSubscribeButtons();
        disposeSyncProxy();
        clearLockScreen();
        instance = null;
        super.onDestroy();
    }

    public void disposeSyncProxy() {
        Log.d(TAG, "disposeSyncProxy()");
        if (proxy != null) {
            try {
                proxy.dispose();
            } catch (SyncException e) {
                e.printStackTrace();
            }
            proxy = null;
            //clearLockScreen();
        }
    }

    public void onProxyClosed(String info, Exception e) {
        Log.d(TAG, "onProxyClosed() String info, Exception e");
        clearLockScreen();

        if ((((SyncException) e).getSyncExceptionCause() != SyncExceptionCause.SYNC_PROXY_CYCLED)) {
            if (((SyncException) e).getSyncExceptionCause() != SyncExceptionCause.BLUETOOTH_DISABLED) {
                Log.v(TAG, "reset proxy in onproxy closed");
                reset();
            }
        }
    }

    public void reset() {
        //Log.d(TAG, "reset() proxy: " + proxy);
        if (proxy != null) {
            try {
                proxy.resetProxy();
            } catch (SyncException e1) {
                e1.printStackTrace();
                //something goes wrong, & the proxy returns as null, stop the service.
                //do not want a running service with a null proxy
                if (proxy == null) {
                    stopSelf();
                }
            }

        } else {
            startProxy();
        }
    }

    public double[] getLatLng() {
        return new double[] {lat, lng};
    }

    public void onOnHMIStatus(final OnHMIStatus notification) {
        if (esIProxyALM != null) {
            esIProxyALM.onOnHMIStatus(notification);
        }

        switch (notification.getSystemContext()) {
            case SYSCTXT_MAIN:
                break;
            case SYSCTXT_VRSESSION:
                break;
            case SYSCTXT_MENU:
                break;
            default:
                return;
        }

        switch (notification.getAudioStreamingState()) {
            case AUDIBLE:
                // play audio if applicable
                break;
            case NOT_AUDIBLE:
                // pause/stop/mute audio if applicable
                break;
            default:
                return;
        }

        //Log.d(TAG, "onOnHMIStatus(), " + notification.getSystemContext().name());

        switch (notification.getHmiLevel()) {

            case HMI_FULL:
                /**
                 * Commented driverDistractionNotif check because it doesn't open lock screen when user
                 * goes to change language in between from sync tdk & then returns back to app
                 */
                //if (driverDistractionNotif == false) {
                //showLockScreen();
                //}

                /**
                 * for first release we want it to be in 'English language only'. Thats why the below code is commented
                 */
                 /*try {
                     Language tdkLanguage = proxy.getHmiDisplayLanguage();
                     Locales fordLocale = Locales.getFordLocaleByLanguage(tdkLanguage);
                     Locales appLocale = Locales.getFordLocaleByAppLocale(Locale.getDefault());

                 if (fordLocale != appLocale) {
                    ((EventSeekr) getApplication()).updateFordLocale(Locales.getFordLocaleByLanguage(tdkLanguage));
                 }

                 } catch (SyncException e) {
                    e.printStackTrace();
                 }*/

                /**
                 * 08-09-2014: This line is added for the first release only, as English Language is needed to be set.
                 **/
                ((EventSeekr) getApplication()).updateFordLocale(Enums.Locales.ENGLISH_UNITED_KINGDOM);

                if (notification.getFirstRun()) {

                    /**
                     * 11-09-2014:
                     * This is added as, When app is running on handheld device connected with ford machine and user
                     * disconnects the handheld device's blue-tooth (but app is not killed on handheld device) and then
                     * reconnects it, then app carries the old state of added commands. So, now app will try to delete
                     * all the previously added commands but as the blue-tooth was disconnected this state is not
                     * maintained by Ford machine and hence when delete command request gets invoked it will give
                     * 'INVALID_ID' error and app commands gets unstable.
                     */
                    Command.reset();
                    InteractionChoiceSetUtil.createInteractionChoiceSets();

                    if (EventSeekr.getCurrentBaseActivity() != null) {
                        setCurrentActivity(EventSeekr.getCurrentBaseActivity());
                    }

                    /**
                     * Subscribe for the buttons
                     */
                    subscribeButtons();
                    // setup app on SYNC
                    // send welcome message if applicable
                    initiateMainAL();
                    // initialize with location from phone
                    final double[] latLng = DeviceUtil.getLatLon((EventSeekr) getApplication());
                    lat = latLng[0];
                    lng = latLng[1];

                } else if (isHMIStatusNone) {
                    // In case if user had exited app & revisits the app, display welcome msg. No need to add commands again.
                    isHMIStatusNone = false;
                    //showWelcomeMsg();
                    initiateMainAL();
                    // initialize with location from phone
                    final double[] latLng = DeviceUtil.getLatLon((EventSeekr) getApplication());
                    lat = latLng[0];
                    lng = latLng[1];
                }
                /**
                 * Issue: To change location user makes DD OFF. After changing location, user continue with any 2nd
                 * level commands which was shifting to HMI_FULL section where location wasn't updating.
                 * In this situation the location wasn't updating & hence it was showing event's of
                 * previously selected location.
                 *
                 * Solution: So to avoid this, We check the flag 'isDDOff' if its true then it updates location
                 * in the 'ApplinkService'.
                 *
                 * To know about 'isDDOff' flag see its description.
                 */
                else if (isDDOff && !isFordGPSAvailable) {
                    final double[] latLng = DeviceUtil.getLatLon((EventSeekr) getApplication());
                    if (lat != latLng[0] || lng != latLng[1]) {
                        lat = latLng[0];
                        lng = latLng[1];
                        isLatLngUpdatedInDdOffMode = true;
                    }
                }
                break;

            case HMI_LIMITED:
                if (driverDistractionNotif == false) {
                    Logger.d(TAG, "HMI_LIMITED.. showing LocakScreen..");
                    showLockScreen();
                }
                break;

            case HMI_BACKGROUND:
                if (driverDistractionNotif == false) {
                    Logger.d(TAG, "HMI_BACKGROUND.. showing LocakScreen..");
                    showLockScreen();
                }
                break;

            case HMI_NONE:
                driverDistractionNotif = false;
                isHMIStatusNone = true;
                clearLockScreen();
                resetFirstTimeLaunchParameters();

                /**
                 * This is called in 2 cases:
                 * 1) just after registration of app with SYNC - As soon as user selects our app, we will get notification
                 * for HMI_FULL after which only we can initiate making calls. This will be fulfilled by corresponding
                 * HMI_FULL case under if block with condition notification.getFirstRun(). So esIProxyALM initialization
                 * here is redundant.
                 * 2) on exit - The application will be returned to NONE when the user selects "Exit <app_name>"
                 * via the menu or PTT VR command. The user has opted out of using the application at this point
                 * and the application may not send requests. The application�s registration, button subscriptions,
                 * display state, custom prompts, interaction ChoiceSets, and commands will be persisted and
                 * available if the user again selects the application from the Mobile Applications menu.
                 *
                 * This initialization is especially required to handle 2nd case, because otherwise if user has moved
                 * in the flow after say discover command, then value of esIProxyALM = DiscoverAL. Now user exits
                 * the app followed by reselecting app after some time. As mentioned above under case 2), all
                 * components & registrations are persisted but app will start from first screen asking to choose
                 * one of the commands from Discover, My Events, etc. Its callback onOnCommand() will then call same
                 * method on esIProxyALM which should be initial AL, i.e., MainAL & not DiscoverAL.
                 * That's why this resetting is done here on exit.
                 */
                /**
                 * Following code shifted to HMI_FULL case since we cannot make calls in HMI Status "NONE"
                 */
			/*esIProxyALM = MainAL.getInstance((EventSeekr) getApplication());
			esIProxyALM.onStartInstance();*/
                break;

            default:
                return;
        }
    }

    private void resetFirstTimeLaunchParameters() {
        ((EventSeekr)getApplication()).setFirstTimeLaunchFordWelComeMsg(true);
        ((EventSeekr)getApplication()).setFirstEventTitleForFordEventAL(true);
    }

    private void showLockScreen() {
        //Log.d(TAG, "showLockScreen() currentUIActivity: " + currentUIActivity);
        // only throw up lockscreen if main activity is currently on top
        // else, wait until onResume() to throw lockscreen so it doesn't
        // pop-up while a user is using another app on the phone
        if (currentUIActivity != null) {
            if (currentUIActivity.isActivityonTop()) {
                if (LockScreenActivity.getInstance() == null) {
                    startLockScreen();
                }
            }
        }
        Logger.d(TAG, "showLockScreen lockscreenUP(before setting true): " + lockscreenUP);
        lockscreenUP = true;
    }

    public void startLockScreen() {
        //Log.d(TAG, "startLockScreen()");
        Intent i = new Intent(this, LockScreenActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        startActivity(i);
    }

    private void clearLockScreen() {
        if (LockScreenActivity.getInstance() != null) {
            LockScreenActivity.getInstance().exit();
        }
        Logger.d(TAG, "clearLockScreen lockscreenUP(before setting false): " + lockscreenUP);
        lockscreenUP = false;
        //ALUtil.unsubscribeForGps();
    }

    public boolean getLockScreenStatus() {return lockscreenUP;}

    public void subscribeButtons() {
        try {
            proxy.subscribeButton(ButtonName.OK, autoIncCorrId++);
            proxy.subscribeButton(ButtonName.SEEKLEFT, autoIncCorrId++);
            proxy.subscribeButton(ButtonName.SEEKRIGHT, autoIncCorrId++);

        } catch (SyncException e) {}
    }

    public void onOnDriverDistraction(final OnDriverDistraction notification) {
        driverDistractionNotif = true;
        if (notification.getState() == DriverDistractionState.DD_OFF) {
            Logger.d(TAG, "onOnDriverDistraction.. clearing LockScreen..");
            clearLockScreen();
            isDDOff = true;

        } else {
            if (!isFordGPSAvailable) {
                final double[] latLng = DeviceUtil.getLatLon((EventSeekr) getApplication());
                if (lat != latLng[0] || lng != latLng[1]) {
                    lat = latLng[0];
                    lng = latLng[1];
                    isLatLngUpdatedInDdOffMode = true;
                }
            }
            Logger.d(TAG, "onOnDriverDistraction.. showing LockScreen..");
            showLockScreen();
            isDDOff = false;
        }
    }

    public void onOnCommand(OnCommand notification) {
        //Log.d(TAG, "onOnCommand");
		/*
		 * notification obj structure :
		 * {"notification": {"name": "OnCommand", "parameters": {"cmdID": "3", "triggerSource": "MENU"}}}
		 */
        //int cmdId = Integer.parseInt(notification.getParameters("cmdID").toString());
        esIProxyALM.onOnCommand(notification);
        /************************************************
         * NOTE:notification.getCmdID() is not working. *
         * So, we have used the alternative for the same*
         ************************************************/
        int cmdId = Integer.parseInt(notification.getParameters("cmdID").toString());
        //Log.d(TAG, "onOnCommand, cmdId = " + cmdId);
        Command cmd = Command.getCommandById(cmdId);
        onCommandPress(cmd, notification.getTriggerSource() == TriggerSource.TS_MENU);
        //esIProxyALM.performOperationForCommand(cmd, notification.getTriggerSource() == TriggerSource.TS_MENU);
    }

    /**
     * Issue: Location was not changing if the city is changed from app.
     * Steps to produce the issue:
     * 1. When app connected to ford system, select any category ex. Nearby.
     * 2. Then it shows the venues of initially selected city ex. Mumbai, say total venues near by to Mumbai city are 25.
     * 3. Then I traversed through the venues till 5th venue. Then I changed the city to Sydney.
     *
     * So, Sir told solution on this is like 'Add reset here and query again.'
     * Hence we created this method, which we used in 'onOnCommand()' & 'onOnButtonPress()'
     *
     * In this method, if user changes location with help of DD-Mode & after changing location
     * if user continue with any 2nd level command irrespective of DD_ON mode then this method
     * reset venue list with its previous category.
     * @param cmd
     */
    private void onCommandPress(Command cmd, boolean isTriggerSrcMenu) {
        if (!isFordGPSAvailable && secLevelCmd.contains(cmd)) {
            double latlon[] = DeviceUtil.getLatLon((EventSeekr) getApplication());
            if (lat != latlon[0] || lng != latlon[1] || isLatLngUpdatedInDdOffMode) {
                lat = latlon[0];
                lng = latlon[1];

                /**
                 *  In initiateESIProxyListener we are passing false in third parameter so that it will use same category
                 *  for the next categoryAL call with different location.
                 */
                Bundle args = new Bundle();
                args.putBoolean(BundleKeys.HAS_LAT_LON_CHANGED_OUT_OF_FORD_APP_SCOPE, true);
                if (esIProxyALM instanceof DiscoverAL) {
                    cmd = Command.DISCOVER;

                } else if (esIProxyALM instanceof MyEventsAL) {
                    cmd = Command.MY_EVENTS;
                }
                AppLinkService.getInstance().initiateESIProxyListener(cmd, isTriggerSrcMenu, args);
                isLatLngUpdatedInDdOffMode = false;
                return;
            }
        }
        isLatLngUpdatedInDdOffMode = false;
        esIProxyALM.performOperationForCommand(cmd, isTriggerSrcMenu);
    }

    public void onCreateInteractionChoiceSetResponse(CreateInteractionChoiceSetResponse response) {
        esIProxyALM.onCreateInteractionChoiceSetResponse(response);
		/*Log.d(TAG, "onCreateInteractionChoiceSetResponse(), response: " + response.getInfo() + ", " 
				+ response.getMessageType() + ", " + response.getResultCode());*/
    }

    public void onPerformInteractionResponse(PerformInteractionResponse response) {
        //Log.d(TAG, "onPerformInteractionResponse()");
        esIProxyALM.onPerformInteractionResponse(response);
    }

    public void onSpeakResponse(SpeakResponse response) {
        esIProxyALM.onSpeakResponse(response);
    }

    public void onOnButtonPress(OnButtonPress notification) {
        ButtonName btnName = notification.getButtonName();
        Command cmd = null;
        if (btnName == ButtonName.CUSTOM_BUTTON) {//This case is for Soft Buttons
            cmd = Command.getCommandById(notification.getCustomButtonName());

        } else {//This case is for Seek-Buttons
            cmd = Command.getCommandByButtonName(btnName);
        }
        //esIProxyALM.performOperationForCommand(cmd, true);
        onCommandPress(cmd, true);
    }

    /**
     * must be called when Discover, My Events and search commands are invoked
     * cmd - non null value
     * @param cmd
     * @param isTriggerSrcMenu
     * @param args
     */
    public void initiateESIProxyListener(Command cmd, boolean isTriggerSrcMenu, Bundle args) {
        switch (cmd) {
            case DISCOVER:
                //Log.d(TAG, "DISCOVER");
                esIProxyALM =  DiscoverAL.getInstance((EventSeekr) getApplication());
                break;
            case MY_EVENTS:
                //Log.d(TAG, "MY EVENTS");
                esIProxyALM =  MyEventsAL.getInstance((EventSeekr) getApplication());
                break;
            case SEARCH:
                //Log.d(TAG, "SEARCH");
                esIProxyALM =  SearchAL.getInstance((EventSeekr) getApplication());
                break;
        }
        if (esIProxyALM == null) {
            return;
        }
        if (args == null) {
            args = new Bundle();
        }
        args.putBoolean(BundleKeys.MANUAL_IO_ONLY, isTriggerSrcMenu);
        esIProxyALM.setArguments(args);
        esIProxyALM.onStartInstance();
    }

    public void initiateMainAL() {
        esIProxyALM = MainAL.getInstance((EventSeekr) getApplication());
        esIProxyALM.onStartInstance();
    }

    public void handleNoNetConnectivity() {
        ALUtil.alert(getResources().getString(R.string.the_internet), getResources().getString(R.string.connection_appears),
                getResources().getString(R.string.to_be_offline), getResources().getString(R.string.connection_lost));
        initiateMainAL();
    }
	
	/*public void registerPhoneStateListener() {
		lastCallState = TelephonyManager.CALL_STATE_IDLE;
		isCallAborted = false;
        currentUIActivity.runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
		        listener = new ListenToPhoneState();
		        TelephonyManager tManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		        tManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);
			}
		});
	}
	
	public void unregisterPhoneStateListener() {
		TelephonyManager tManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		tManager.listen(listener, PhoneStateListener.LISTEN_NONE);
		listener = null;
	}
	
	private class ListenToPhoneState extends PhoneStateListener {

        public void onCallStateChanged(final int state, String incomingNumber) {
        	if (state == TelephonyManager.CALL_STATE_IDLE) {
				if (lastCallState != TelephonyManager.CALL_STATE_IDLE) {
					isCallAborted = true;
					unregisterPhoneStateListener();
				}
				
			} else {
				lastCallState = state;
			}
        }
    }*/

    public void onError(String info, Exception e) {}

    public void onGenericResponse(GenericResponse response) {}

    public void onAddCommandResponse(final AddCommandResponse response) {
        esIProxyALM.onAddCommandResponse(response);
    }

    public void onAddSubMenuResponse(AddSubMenuResponse response) {}

    public void onAlertResponse(AlertResponse response) {
        esIProxyALM.onAlertResponse(response);
        isAlertCurrentlyVisible = false;
    }

    public void onDeleteCommandResponse(final DeleteCommandResponse response) {
        esIProxyALM.onDeleteCommandResponse(response);
    }

    public void onDeleteInteractionChoiceSetResponse(DeleteInteractionChoiceSetResponse response) {
        esIProxyALM.onDeleteInteractionChoiceSetResponse(response);
    }

    public void onDeleteSubMenuResponse(DeleteSubMenuResponse response) {}

    public void onResetGlobalPropertiesResponse(ResetGlobalPropertiesResponse response) {}

    public void onSetGlobalPropertiesResponse(SetGlobalPropertiesResponse response) {}

    public void onSetMediaClockTimerResponse(SetMediaClockTimerResponse response) {}

    public void onShowResponse(ShowResponse response) {}

    public void onOnButtonEvent(OnButtonEvent notification) {}

    public void onSubscribeButtonResponse(SubscribeButtonResponse response) {}

    public void onUnsubscribeButtonResponse(UnsubscribeButtonResponse response) {}

    public void onOnPermissionsChange(OnPermissionsChange notification) {}

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onChangeRegistrationResponse(ChangeRegistrationResponse arg0) {

    }

    @Override
    public void onDeleteFileResponse(DeleteFileResponse arg0) {

    }

    @Override
    public void onEndAudioPassThruResponse(EndAudioPassThruResponse arg0) {

    }

    @Override
    public void onGetDTCsResponse(GetDTCsResponse arg0) {

    }

    @Override
    public void onGetVehicleDataResponse(final GetVehicleDataResponse arg0) {
        /**17-06-2015:
         * As per the email on 16-06-2015, we need to remove the usage of 'subscribeForGps()' instead we need to
         * use 'getVehicleData()'
         */
        final Result resultCode = arg0.getResultCode();
        if (resultCode == Result.SUCCESS) {
            isFordGPSAvailable = true;
            lat = arg0.getGps().getLatitudeDegrees();
            lng = arg0.getGps().getLongitudeDegrees();

        } else if (DeviceUtil.isDefaultLatLonUsed()) {
            ALUtil.alert(getResources().getString(R.string.unable_to_determine),
                getResources().getString(R.string.your_location), "",
                getResources().getString(R.string.using_san_francisco_as_default));
        }
        if (esIProxyALM != null) {
            esIProxyALM.onGetVehicleDataResponse(arg0);
        }
    }

    @Override
    public void onListFilesResponse(ListFilesResponse arg0) {

    }

    @Override
    public void onOnAudioPassThru(OnAudioPassThru arg0) {
        if (esIProxyALM instanceof SearchAL) {
            esIProxyALM.onOnAudioPassThru(arg0);
        }
    }

    @Override
    public void onOnLanguageChange(final OnLanguageChange arg0) {
        /**
         * 08-09-2014: This code is commented as for the first release only English Language is needed.
         Log.d(TAG, "onOnLanguageChange to " + arg0.getLanguage().name() + ", " +
         arg0.getHmiDisplayLanguage().name());
         currentUIActivity.runOnUiThread(new Runnable() {

        @Override
        public void run() {
        Toast.makeText(currentUIActivity, "onOnLanguageChange to " + arg0.getLanguage().name() + ", " +
        arg0.getHmiDisplayLanguage().name(), Toast.LENGTH_SHORT).show();
        }
        });

         Language language = arg0.getLanguage();
         ((EventSeekr) getApplication()).updateFordLocale(Locales.getFordLocaleByLanguage(language));*/
    }

    @Override
    public void onOnVehicleData(final OnVehicleData arg0) {
        lat = arg0.getGps().getLatitudeDegrees();
        lng = arg0.getGps().getLongitudeDegrees();
		
		/*currentUIActivity.runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				Toast.makeText(currentUIActivity, "onOnVehicleData lat = " + 
						arg0.getGps().getLatitudeDegrees(), Toast.LENGTH_SHORT).show();
			}
		});*/
    }

    @Override
    public void onPerformAudioPassThruResponse(PerformAudioPassThruResponse arg0) {
        if (esIProxyALM instanceof SearchAL) {
            esIProxyALM.onPerformAudioPassThruResponse(arg0);
        }
    }

    @Override
    public void onPutFileResponse(PutFileResponse arg0) {

    }

    @Override
    public void onReadDIDResponse(ReadDIDResponse arg0) {

    }

    @Override
    public void onScrollableMessageResponse(ScrollableMessageResponse arg0) {

    }

    @Override
    public void onSetAppIconResponse(SetAppIconResponse arg0) {

    }

    @Override
    public void onSetDisplayLayoutResponse(SetDisplayLayoutResponse arg0) {

    }

    @Override
    public void onSliderResponse(SliderResponse arg0) {

    }

    @Override
    public void onSubscribeVehicleDataResponse(final SubscribeVehicleDataResponse arg0) {
        /**17-06-2015:
         * As per the email on 16-06-2015, we need to remove the usage of 'subscribeForGps()' instead we need to
         * use 'getVehicleData()'
         *VehicleDataResultCode resultCode = arg0.getGps().getResultCode();
        if (resultCode == VehicleDataResultCode.SUCCESS || resultCode == VehicleDataResultCode.DATA_ALREADY_SUBSCRIBED) {
            isFordGPSAvailable = true;

        } else {
            isFordGPSAvailable = false;
        }*/
        if (esIProxyALM != null) {
            esIProxyALM.onSubscribeVehicleDataResponse(arg0);
        }
		/*currentUIActivity.runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				Toast.makeText(currentUIActivity, "onSubscribeVehicleDataResponse " + 
						arg0.getGps().getResultCode().name(), Toast.LENGTH_SHORT).show();
			}
		});*/
    }

    @Override
    public void onUnsubscribeVehicleDataResponse(final UnsubscribeVehicleDataResponse arg0) {
        //isFordGPSAvailable = false;
    }

    @Override
    public void onOnTBTClientState(OnTBTClientState arg0) {}

    @Override
    public void onDiagnosticMessageResponse(DiagnosticMessageResponse arg0) {}

    @Override
    public void onOnHashChange(OnHashChange arg0) {}

    @Override
    public void onOnKeyboardInput(OnKeyboardInput arg0) {}

    @Override
    public void onOnLockScreenNotification(OnLockScreenStatus notification) {
        //Log.d(TAG, "onOnLockScreenNotification");
        LockScreenStatus displayLockScreen = notification.getShowLockScreen();
        Logger.d(TAG, "onOnLockScreenNotification displayLockScreen: " + displayLockScreen);
        if (!isDDOff && displayLockScreen == LockScreenStatus.REQUIRED || displayLockScreen == LockScreenStatus.OPTIONAL) {
            showLockScreen();

        } else {
            clearLockScreen();
        }
    }

    @Override
    public void onOnSystemRequest(OnSystemRequest arg0) {}

    @Override
    public void onOnTouchEvent(OnTouchEvent arg0) {}

    @Override
    public void onProxyClosed(String info, Exception e, SyncDisconnectedReason reason) {
        Log.d(TAG, "onProxyClosed() String info, Exception e, SyncDisconnectedReason reason");
        clearLockScreen();
        if (((SyncException) e).getSyncExceptionCause() == SyncExceptionCause.BLUETOOTH_DISABLED) {
            resetFirstTimeLaunchParameters();
        }

        if ((((SyncException) e).getSyncExceptionCause() != SyncExceptionCause.SYNC_PROXY_CYCLED)) {
            if (((SyncException) e).getSyncExceptionCause() != SyncExceptionCause.BLUETOOTH_DISABLED) {
                Log.d(TAG, "onProxyClosed() reset()");
                reset();
            }
        }
    }

    @Override
    public void onSystemRequestResponse(SystemRequestResponse arg0) {}
}