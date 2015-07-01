package com.wcities.eventseeker;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.Toast;

import com.ford.syncV4.proxy.SyncProxyALM;
import com.ford.syncV4.transport.TransportType;
import com.wcities.eventseeker.GeneralDialogFragment.DialogBtnClickListener;
import com.wcities.eventseeker.analytics.GoogleAnalyticsTracker;
import com.wcities.eventseeker.analytics.GoogleAnalyticsTracker.Type;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.applink.service.AppLinkService;
import com.wcities.eventseeker.bosch.BoschMainActivity;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.gcm.GcmBroadcastReceiver.NotificationType;
import com.wcities.eventseeker.interfaces.ConnectionFailureListener;
import com.wcities.eventseeker.interfaces.DrawerListFragmentListener;
import com.wcities.eventseeker.util.DeviceUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Set;

public abstract class BaseActivity extends ActionBarActivity implements ConnectionFailureListener, DialogBtnClickListener,
        DrawerListFragmentListener {

    private static final String TAG = BaseActivity.class.getSimpleName();

    private boolean activityOnTop;

    private boolean onSaveInstanceStateCalled;

    private static boolean isConnectedToFord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Log.d(TAG, "base onCreate() - " + getClass().getSimpleName() + ", taskId = " + getTaskId());
        //Toast.makeText(getApplicationContext(), "base onCreate() - " + getClass().getSimpleName() + ", taskId = " + getTaskId(), Toast.LENGTH_SHORT).show();
        BoschMainActivity.appTaskId = getTaskId();
        /**
         * Locale changes are Activity specific i.e. after the Activity gets destroyed, the Locale changes
         * associated with that activity will also get destroyed. So, if Activity was destroyed due to
         * configuration changes(like orientation change) then the Newer Activity will initialize itself with
         * the Device specific Locale. So, each and every time when activity gets initialized it should
         * also initialize its Locale from SharedPref.
         */
        ((EventSeekr) getApplication()).setDefaultLocale();

        /*if (((EventSeekr) getApplication()).getWcitiesId() == null) {
            return;
        }*/

        if (AppConstants.FORD_SYNC_APP && !isConnectedToFord) {
            startSyncProxyService();
            isConnectedToFord = true;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Log.d(TAG, "BaseActivity onStart()");
        //Toast.makeText(getApplicationContext(), "BaseActivity onStart()", Toast.LENGTH_SHORT).show();
        EventSeekr.setConnectionFailureListener(this);
        DeviceUtil.registerLocationListener(this);
        /*if (((EventSeekr) getApplication()).getWcitiesId() == null) {
            return;
        }*/
        // when user comes back from bosch, onStart() is called where we need to reset language as per mobile/tablet app
        ((EventSeekr) getApplication()).setDefaultLocale();
        return;
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Log.d(TAG, "BaseActivity onResume()");
        //Toast.makeText(getApplicationContext(), "BaseActivity onResume()", Toast.LENGTH_SHORT).show();
        /*if (((EventSeekr) getApplication()).getWcitiesId() == null) {
            return;
        }*/

        boolean isLockscreenVisible = false;
        if (AppConstants.FORD_SYNC_APP) {
            EventSeekr.setCurrentBaseActivity(this);
            setCurrentActivity();

            activityOnTop = true;
            // check if lockscreen should be up
            AppLinkService serviceInstance = AppLinkService.getInstance();
            if (serviceInstance != null) {
                if (serviceInstance.getLockScreenStatus() == true) {
                    if (LockScreenActivity.getInstance() == null) {
                        serviceInstance.startLockScreen();
                    }
                    isLockscreenVisible = true;
                }
            }
        }
        if (!isLockscreenVisible) {
            /**
             * This is required because if user is connected to ford & then goes to change language
             * from sync tdk, then lock screen is destroyed showing actual app screen on device.
             * In this case locale should be set for the device (not what is there on TDK).
             */
            //Log.d(TAG, "onResume()");
            ((EventSeekr) getApplication()).setDefaultLocale();
            //Log.d(TAG, "onResume()");
        }
        onSaveInstanceStateCalled = false;
    }

    @Override
    protected void onPause() {
        //Log.d(TAG, "onPause()");
        super.onPause();
        /*if (((EventSeekr) getApplication()).getWcitiesId() == null) {
            return;
        }*/

        if (AppConstants.FORD_SYNC_APP) {
            activityOnTop = false;
            /**
             * 30-06-2015:
             * Below code determines if DriverDistraction is Off then the user must be using Eventseeker
             * Mobile app if at this moment if he leaves app (moves to some other app) then in Low memory
             * Scenarios the Eventseeker app may get killed(seen this issue in Sony Xperia L) resulting into
             * disconnection with Applink. So, This is just to put the Service in Foreground Process.
             * Here, if the Service is already running then new instance would not be created, instead
             * the onStartCommand() would be called with this new intent.
             *
             * NOTE: The below code is strictly for the DDOff scenario, if this check isn't added then this
             * block will get executed even while switching from BaseActivity to LockScreenActivity.
             * And we haven't added code for 'ACTION_APPLINK_SERVICE_STOP_FOREGROUND' in onStop() as this
             * will get executed in LockScreenActivity as soon as we resume the app the LockScreenActivity
             * would get launch.
             */
            AppLinkService serviceInstance = AppLinkService.getInstance();
            if (serviceInstance != null && serviceInstance.isDDOff()) {
                //Logger.d(TAG, "making service in Foreground for the DDOff mode...");
                Intent intent = new Intent(getApplicationContext(), AppLinkService.class);
                intent.setAction(AppConstants.ACTION_APPLINK_SERVICE_START_FOREGROUND);
                startService(intent);

            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        //Log.d(TAG, "onStop()");

        EventSeekr.resetConnectionFailureListener(this);
        DeviceUtil.unregisterLocationListener((EventSeekr) getApplication());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //Log.d(TAG, "onSaveInstanceState()");
        onSaveInstanceStateCalled = true;
    }

    /**
     * Added on: 2015-04-15
     * onDestroy was removed earlier because of addition of multiple activities in new 'Material Design implementation'.
     * Hence code of onDestroy was added in the onStop() of this BaseActivity.
     * Because of this every time when 'LockScreenActivity' comes in front on the top of BaseActivity, then
     * onStop() of 'BaseActIvity' gets called, which ends the syncProxyInstance & because of this connection gets
     * lost whenever APP connects to 'SYNC'. So to avoid this we again added this onDestroy().
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Toast.makeText(getApplicationContext(), "BaseActivity onDestroy()", Toast.LENGTH_SHORT).show();
        if (AppConstants.FORD_SYNC_APP && EventSeekr.getCurrentBaseActivity() == this) {
            //Log.v(TAG, "onDestroy main");
            endSyncProxyInstance();
            EventSeekr.resetCurrentBaseActivityFor(this);
            AppLinkService serviceInstance = AppLinkService.getInstance();
            if (serviceInstance != null) {
                serviceInstance.resetCurrentActivityFor(this);
            }
        }
    }

    public boolean isOnSaveInstanceStateCalled() {
        return onSaveInstanceStateCalled;
    }

    public void startSyncProxyService() {
        // Log.i(TAG, "startSyncProxyService()");
        if (AppConstants.DEBUG) {
            if (AppLinkService.getInstance() == null) {
                // Log.i(TAG, "getInstance() == null");
                Intent startIntent = new Intent(this, AppLinkService.class);
                startService(startIntent);

            } else {
                // if the service is already running and proxy is up,
                // set this as current UI activity
                AppLinkService.getInstance().setCurrentActivity(this);
                // Log.i(TAG, " proxyAlive == true success");
            }

        } else {
            boolean isSYNCpaired = false;
            // Get the local Bluetooth adapter
            BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();

            // BT Adapter exists, is enabled, and there are paired devices with
            // the
            // name SYNC
            // Ideally start service and start proxy if already connected to
            // sync
            // but, there is no way to tell if a device is currently connected
            // (pre
            // OS 4.0)

            if (mBtAdapter != null) {
                // Log.i(TAG, "mBtAdapter is not null");
                if ((mBtAdapter.isEnabled() && mBtAdapter.getBondedDevices().isEmpty() == false)) {
                    Log.i(TAG, "pairedDevices");
                    // Get a set of currently paired devices
                    Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

                    // Check if there is a paired device with the name "SYNC"
                    if (pairedDevices.size() > 0) {
                        // Log.i(TAG, "pairedDevices > 0");
                        for (BluetoothDevice device : pairedDevices) {
                            // Log.i(TAG, "device.getName() = " +
                            // device.getName());
                            if (device.getName().toString().contains("SYNC")) {
                                // Log.i(TAG, "found SYNC");
                                isSYNCpaired = true;
                                break;
                            }
                        }

                    } else {
                        Log.i(TAG, "A No Paired devices with the name sync");
                    }

                    if (isSYNCpaired == true) {
                        if (AppLinkService.getInstance() == null) {
                            // Log.i(TAG, "start service");
                            Intent startIntent = new Intent(this, AppLinkService.class);
                            startService(startIntent);

                        } else {
                            //if the service is already running and proxy is up, set this as current UI activity
                            AppLinkService serviceInstance = AppLinkService.getInstance();
                            serviceInstance.setCurrentActivity(this);
                            SyncProxyALM proxyInstance = serviceInstance.getProxy();
                            if (proxyInstance != null) {
                                serviceInstance.reset();

                            } else {
                                Log.i(TAG, "proxy is null");
                                serviceInstance.startProxy();
                            }
                            Log.i(TAG, " proxyAlive == true success");
                        }
                    }
                }
            }
        }
    }

    public void setCurrentActivity() {
        // Log.i(TAG, "startSyncProxyService()");
        if (AppConstants.DEBUG) {
            if (AppLinkService.getInstance() != null) {
                // if the service is already running and proxy is up,
                // set this as current UI activity
                AppLinkService.getInstance().setCurrentActivity(this);
                // Log.i(TAG, " proxyAlive == true success");
            }

        } else {
            boolean isSYNCpaired = false;
            // Get the local Bluetooth adapter
            BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();

            // BT Adapter exists, is enabled, and there are paired devices with
            // the
            // name SYNC
            // Ideally start service and start proxy if already connected to
            // sync
            // but, there is no way to tell if a device is currently connected
            // (pre
            // OS 4.0)

            if (mBtAdapter != null) {
                // Log.i(TAG, "mBtAdapter is not null");
                if ((mBtAdapter.isEnabled() && mBtAdapter.getBondedDevices().isEmpty() == false)) {
                    Log.i(TAG, "pairedDevices");
                    // Get a set of currently paired devices
                    Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

                    // Check if there is a paired device with the name "SYNC"
                    if (pairedDevices.size() > 0) {
                        // Log.i(TAG, "pairedDevices > 0");
                        for (BluetoothDevice device : pairedDevices) {
                            // Log.i(TAG, "device.getName() = " +
                            // device.getName());
                            if (device.getName().toString().contains("SYNC")) {
                                // Log.i(TAG, "found SYNC");
                                isSYNCpaired = true;
                                break;
                            }
                        }

                    } else {
                        Log.i(TAG, "A No Paired devices with the name sync");
                    }

                    if (isSYNCpaired == true) {
                        if (AppLinkService.getInstance() != null) {
                            //if the service is already running and proxy is up, set this as current UI activity
                            AppLinkService serviceInstance = AppLinkService.getInstance();
                            serviceInstance.setCurrentActivity(this);
                        }
                    }
                }
            }
        }
    }

    // upon onDestroy(), dispose current proxy and create a new one to enable
    // auto-start
    // call resetProxy() to do so
    public void endSyncProxyInstance() {
        AppLinkService serviceInstance = AppLinkService.getInstance();
        if (serviceInstance != null) {
            SyncProxyALM proxyInstance = serviceInstance.getProxy();
            // if proxy exists, reset it
            if (proxyInstance != null) {
                if (proxyInstance.getCurrentTransportType() == TransportType.BLUETOOTH) {
                    serviceInstance.reset();

                } else {
                    Log.e(TAG, "endSyncProxyInstance. No reset required if transport is TCP");
                }
                // if proxy == null create proxy
            } else {
                serviceInstance.startProxy();
            }
        }
    }

    public boolean isActivityonTop() {
        return activityOnTop;
    }

    @Override
    public void onConnectionFailure() {
        GeneralDialogFragment generalDialogFragment = GeneralDialogFragment.newInstance(this,
                getResources().getString(R.string.no_internet_connectivity),
                getResources().getString(R.string.connection_lost), "Ok", null, false);
        generalDialogFragment.show(getSupportFragmentManager(), AppConstants.DIALOG_FRAGMENT_TAG_CONNECTION_LOST);
    }

    @Override
    public void doPositiveClick(String dialogTag) {
        if (dialogTag.equals(AppConstants.DIALOG_FRAGMENT_TAG_CONNECTION_LOST)) {
            DialogFragment dialogFragment = (DialogFragment) getSupportFragmentManager()
                    .findFragmentByTag(AppConstants.DIALOG_FRAGMENT_TAG_CONNECTION_LOST);
            if (dialogFragment != null) {
                dialogFragment.dismiss();
            }
        }
    }

    @Override
    public void doNegativeClick(String dialogTag) {
        // TODO Auto-generated method stub
    }

    /**
     * Handles navigation to drawerList Items only
     * @param notificationType
     */
    public void onNotificationClicked(NotificationType notificationType) {
        if (notificationType.getNavDrawerIndex() != AppConstants.INVALID_INDEX) {
            Bundle args = null;
            if (notificationType == NotificationType.RECOMMENDED_EVENTS) {
                args = new Bundle();
                args.putBoolean(BundleKeys.SELECT_RECOMMENDED_EVENTS, true);
            }
            onDrawerItemSelected(notificationType.getNavDrawerIndex(), args);
        }
    }

    protected void inviteFriends() {
        //Log.d(TAG, "inviteFriends()");
        try {
            if (getIntent().hasExtra(BundleKeys.IS_FROM_NOTIFICATION)) {
                EventSeekr eventseekr = (EventSeekr) getApplication();
                GoogleAnalyticsTracker.getInstance().sendApiCall(eventseekr, GoogleAnalyticsTracker.getInstance()
                        .getExtraInfoApiUrl(Type.Notification.name(), URLEncoder.encode(GoogleAnalyticsTracker.OPEN_INVITE_FRIENDS_NOTIFICATION,
                                AppConstants.CHARSET_NAME), null, AppConstants.INVALID_ID, eventseekr.getWcitiesId()), null);

                getIntent().removeExtra(BundleKeys.IS_FROM_NOTIFICATION);
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        String url = "https://play.google.com/store/apps/details?id=" + getPackageName();
        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        intent.putExtra(Intent.EXTRA_TEXT, "Checkout eventseeker" + " " + url);
        try {
            startActivityForResult(intent, AppConstants.REQ_CODE_INVITE_FRIENDS);

        } catch (ActivityNotFoundException e) {
            Toast.makeText(getApplicationContext(), "Error, this action cannot be completed at this time.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    protected void rateApp() {
        Uri uri = Uri.parse("market://details?id=" + getPackageName());
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        try {
            startActivityForResult(goToMarket, AppConstants.REQ_CODE_RATE_APP);

        } catch (ActivityNotFoundException e) {
            Toast.makeText(getApplicationContext(), R.string.error_this_action_couldnt_be_completed_at_this_time,
                    Toast.LENGTH_SHORT).show();
        }
    }

    public Fragment getFragmentByTag(String fragmentTag) {
        return getSupportFragmentManager().findFragmentByTag(fragmentTag);
    }
}