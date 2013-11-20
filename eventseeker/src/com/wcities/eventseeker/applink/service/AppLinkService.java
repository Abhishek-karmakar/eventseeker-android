/**Ford Motor Company
 * September 2012
 * Elizabeth Halash
 */

package com.wcities.eventseeker.applink.service;

import java.util.Arrays;
import java.util.Vector;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.ford.syncV4.exception.SyncException;
import com.ford.syncV4.exception.SyncExceptionCause;
import com.ford.syncV4.proxy.SyncProxyALM;
import com.ford.syncV4.proxy.interfaces.IProxyListenerALM;
import com.ford.syncV4.proxy.rpc.AddCommandResponse;
import com.ford.syncV4.proxy.rpc.AddSubMenuResponse;
import com.ford.syncV4.proxy.rpc.AlertResponse;
import com.ford.syncV4.proxy.rpc.CreateInteractionChoiceSetResponse;
import com.ford.syncV4.proxy.rpc.DeleteCommandResponse;
import com.ford.syncV4.proxy.rpc.DeleteInteractionChoiceSetResponse;
import com.ford.syncV4.proxy.rpc.DeleteSubMenuResponse;
import com.ford.syncV4.proxy.rpc.EncodedSyncPDataResponse;
import com.ford.syncV4.proxy.rpc.GenericResponse;
import com.ford.syncV4.proxy.rpc.OnButtonEvent;
import com.ford.syncV4.proxy.rpc.OnButtonPress;
import com.ford.syncV4.proxy.rpc.OnCommand;
import com.ford.syncV4.proxy.rpc.OnDriverDistraction;
import com.ford.syncV4.proxy.rpc.OnEncodedSyncPData;
import com.ford.syncV4.proxy.rpc.OnHMIStatus;
import com.ford.syncV4.proxy.rpc.OnPermissionsChange;
import com.ford.syncV4.proxy.rpc.OnTBTClientState;
import com.ford.syncV4.proxy.rpc.PerformInteractionResponse;
import com.ford.syncV4.proxy.rpc.ResetGlobalPropertiesResponse;
import com.ford.syncV4.proxy.rpc.SetGlobalPropertiesResponse;
import com.ford.syncV4.proxy.rpc.SetMediaClockTimerResponse;
import com.ford.syncV4.proxy.rpc.ShowResponse;
import com.ford.syncV4.proxy.rpc.SpeakResponse;
import com.ford.syncV4.proxy.rpc.SubscribeButtonResponse;
import com.ford.syncV4.proxy.rpc.SyncMsgVersion;
import com.ford.syncV4.proxy.rpc.UnsubscribeButtonResponse;
import com.ford.syncV4.proxy.rpc.enums.DriverDistractionState;
import com.ford.syncV4.proxy.rpc.enums.Language;
import com.ford.syncV4.proxy.rpc.enums.TextAlignment;
import com.ford.syncV4.transport.TCPTransportConfig;
import com.ford.syncV4.util.DebugTool;
import com.wcities.eventseeker.LockScreenActivity;
import com.wcities.eventseeker.MainActivity;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.applink.handler.MainActivityAL;
import com.wcities.eventseeker.constants.AppConstants;

public class AppLinkService extends Service implements IProxyListenerALM {

	private static final String TAG = "AppLinkService";
	
	public static final int START_CMD_ID_MAIN_ACTIVITY_AL = 1;
	public static final int SPEAK_CHAR_LIMIT = 499;
	
	//variable used to increment correlation ID for every request sent to SYNC
	public int autoIncCorrId = 0;
	
	//variable to contain the current state of the service
	private static AppLinkService instance = null;
	//variable to contain the current state of the main UI ACtivity
	private MainActivity currentUIActivity;
	//variable to access the BluetoothAdapter
	private BluetoothAdapter mBtAdapter;
	//variable to create and call functions of the SyncProxy
	private SyncProxyALM proxy = null;
	//variable that keeps track of whether SYNC is sending driver distractions
	//(older versions of SYNC will not send this notification)
	private boolean driverDistractionNotif = false;
	//variable to contain the current state of the lockscreen
	private boolean lockscreenUP = false;
	private IProxyListenerALM iProxyListenerALM;
	private boolean isHMIStatusNone;
	
	public static AppLinkService getInstance() {
		return instance;
	}
	
	public SyncProxyALM getProxy() {
		return proxy;
	}
	
	public MainActivity getCurrentActivity() {
		return currentUIActivity;
	}
	
	public void setCurrentActivity(MainActivity currentActivity) {
		this.currentUIActivity = currentActivity;
	}
	
	public IProxyListenerALM getIProxyListenerALM() {
		return iProxyListenerALM;
	}

	public void setIProxyListenerALM(IProxyListenerALM iProxyListenerALM) {
		this.iProxyListenerALM = iProxyListenerALM;
	}

	public void onCreate() {
		super.onCreate();
		//Log.i(TAG, "onCreate()");
		instance = this;
	}
	
	public int onStartCommand(Intent intent, int flags, int startId) {
		//Log.i(TAG, "onStartCommand()");
		if (AppConstants.DEBUG) {
			startProxy();
			
		} else {
			//Log.i(TAG, "onStartCommand(), Non-debug mode");
	        if (intent != null) {
	        	//Log.i(TAG, "intent != null");
	        	mBtAdapter = BluetoothAdapter.getDefaultAdapter();
	    		if (mBtAdapter != null) {
	    			//Log.i(TAG, "mBtAdapter != null");
	    			if (mBtAdapter.isEnabled()) {
	    				//Log.i(TAG, "mBtAdapter is Enabled");
	    				startProxy();
	    			}
	    		}
			}
		}
		
        if (MainActivity.getInstance() != null) {
        	setCurrentActivity(MainActivity.getInstance());
        }
			
        return START_STICKY;
	}
	
	public void startProxy() {
		Log.i(TAG, "startProxy()");
		if (proxy == null) {
			try {
				if (AppConstants.DEBUG) {
					//Log.i(TAG, "startProxy with TCP");
					proxy = new SyncProxyALM(this, getResources().getString(R.string.app_name), true,
							new TCPTransportConfig(AppConstants.TCP_PORT, AppConstants.TCP_IP_ADDRESS, true));
					
				} else {
					SyncMsgVersion syncMsgVersion = new SyncMsgVersion();
					syncMsgVersion.setMajorVersion(1);
					syncMsgVersion.setMinorVersion(1);
					proxy = new SyncProxyALM(this, getResources().getString(R.string.app_name), null, 
							new Vector<String>(Arrays.asList(new String[] {"eventseeker"})), true, 
							syncMsgVersion, Language.EN_US, null);
					Log.i(TAG, "startProxy() registration done");
				}
				
			} catch (SyncException e) {
				e.printStackTrace();
				//error creating proxy, returned proxy = null
				if (proxy == null) {
					stopSelf();
				}
			}
		}
	}
	
	public void onDestroy() {
		Log.i(TAG, "onDestroy()");
		disposeSyncProxy();
		clearLockScreen();
		instance = null;
		super.onDestroy();
	}
	
	public void disposeSyncProxy() {
		Log.i(TAG, "disposeSyncProxy()");
		if (proxy != null) {
			try {
				proxy.dispose();
			} catch (SyncException e) {
				e.printStackTrace();
			}
			proxy = null;
			clearLockScreen();
		}
	}
	
	public void onProxyClosed(String info, Exception e) {
		Log.i(TAG, "onProxyClosed()");
		clearLockScreen();

		if ((((SyncException) e).getSyncExceptionCause() != SyncExceptionCause.SYNC_PROXY_CYCLED)) {
			if (((SyncException) e).getSyncExceptionCause() != SyncExceptionCause.BLUETOOTH_DISABLED) {
				Log.v(TAG, "reset proxy in onproxy closed");
				reset();
			}
		}
	}

   public void reset() {
	   Log.i(TAG, "reset()");
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
   
   public void show(String mainText1, String mainText2, TextAlignment alignment) {
		try {
			proxy.show(mainText1, mainText2, alignment, autoIncCorrId++);

		} catch (SyncException e) {
			DebugTool.logError("Failed to send Show", e);
		}
   }
   
   public void showWelcomeMsg() {
		String welcomeMsg1 = "Inside", welcomeMsg2 = "eventseekr!";
		show(welcomeMsg1, welcomeMsg2, TextAlignment.CENTERED);
	}
   
   public void onOnHMIStatus(OnHMIStatus notification) {

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
		  
		Log.i(TAG, "onOnHMIStatus(), " + notification.getSystemContext().name());
		switch (notification.getHmiLevel()) {
		
		case HMI_FULL:			
			Log.i(TAG, "onOnHMIStatus(), HMI_FULL, driverDistractionNotif = " + driverDistractionNotif);
			if (driverDistractionNotif == false) {
				showLockScreen();
			}
			
			if (notification.getFirstRun()) {
				// setup app on SYNC
				// send welcome message if applicable
				iProxyListenerALM = MainActivityAL.getInstance((EventSeekr) getApplication());
				iProxyListenerALM.onOnHMIStatus(notification);

				if (MainActivity.getInstance() != null) {
					setCurrentActivity(MainActivity.getInstance());
				}
				
			} else if (isHMIStatusNone) {
				// In case if user had exited app & revisits the app, display welcome msg. No need to add commands again.
				isHMIStatusNone = false;
				showWelcomeMsg();
			}
			break;
			
		case HMI_LIMITED:
			Log.i(TAG, "onOnHMIStatus(), HMI_LIMITED, driverDistractionNotif = " + driverDistractionNotif);
			if (driverDistractionNotif == false) {
				showLockScreen();
			}
			break;
			
		case HMI_BACKGROUND:
			Log.i(TAG, "onOnHMIStatus(), HMI_BACKGROUND, driverDistractionNotif = " + driverDistractionNotif);
			if (driverDistractionNotif == false) {
				showLockScreen();
			}
			break;
			
		case HMI_NONE:
			Log.i(TAG, "onOnHMIStatus(), HMI_NONE");
			driverDistractionNotif = false;
			isHMIStatusNone = true;
			clearLockScreen();
			/**
			 * This is called in 2 cases:
			 * 1) just after registration of app with SYNC - As soon as user selects our app, we will get notification
			 * for HMI_FULL after which only we can initiate making calls. This will be fulfilled by corresponding 
			 * HMI_FULL case under if block with condition notification.getFirstRun(). So iProxyListenerALM initialization
			 * here is redundant.
			 * 2) on exit - The application will be returned to NONE when the user selects "Exit <app_name>" 
			 * via the menu or PTT VR command. The user has opted out of using the application at this point 
			 * and the application may not send requests. The application�s registration, button subscriptions, 
			 * display state, custom prompts, interaction ChoiceSets, and commands will be persisted and 
			 * available if the user again selects the application from the Mobile Applications menu.
			 * 
			 * This initialization is especially required to handle 2nd case, because otherwise if user has moved 
			 * in the flow after say discover command, then value of iProxyListenerALM = DiscoverActivityAL. Now user exits
			 * the app followed by reselecting app after some time. As mentioned above under case 2), all 
			 * components & registrations are persisted but app will start from first screen asking to choose 
			 * one of the commands from Discover, My Events, etc. Its callback onOnCommand() will then call same 
			 * method on iProxyListenerALM which should be initial AL, i.e., MainActivityAL & not DiscoverActivityAL.
			 * That's why this resetting is done here on exit.
			 */
			iProxyListenerALM = MainActivityAL.getInstance((EventSeekr) getApplication());
			break;
			
		default:
			return;
		}
	}
   
	private void showLockScreen() {
		Log.i(TAG, "showLockScreen()");
		// only throw up lockscreen if main activity is currently on top
		// else, wait until onResume() to throw lockscreen so it doesn't
		// pop-up while a user is using another app on the phone
		if (currentUIActivity != null) {
			if (currentUIActivity.isActivityonTop() == true) {
				if (LockScreenActivity.getInstance() == null) {
					Intent i = new Intent(this, LockScreenActivity.class);
					i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					i.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
					startActivity(i);
				}
			}
		}
		lockscreenUP = true;
	}

	private void clearLockScreen() {
		Log.i(TAG, "clearlockscreen()");
		if (LockScreenActivity.getInstance() != null) {
			LockScreenActivity.getInstance().exit();
		}
		lockscreenUP = false;
	}

	public boolean getLockScreenStatus() {return lockscreenUP;}

/*public void subButtons() {
	try {
        proxy.subscribeButton(ButtonName.OK, autoIncCorrId++);
        proxy.subscribeButton(ButtonName.SEEKLEFT, autoIncCorrId++);
		proxy.subscribeButton(ButtonName.SEEKRIGHT, autoIncCorrId++);
		proxy.subscribeButton(ButtonName.TUNEUP, autoIncCorrId++);
		proxy.subscribeButton(ButtonName.TUNEDOWN, autoIncCorrId++);
		proxy.subscribeButton(ButtonName.PRESET_1, autoIncCorrId++);
		proxy.subscribeButton(ButtonName.PRESET_2, autoIncCorrId++);
		proxy.subscribeButton(ButtonName.PRESET_3, autoIncCorrId++);
		proxy.subscribeButton(ButtonName.PRESET_4, autoIncCorrId++);
		proxy.subscribeButton(ButtonName.PRESET_5, autoIncCorrId++);
		proxy.subscribeButton(ButtonName.PRESET_6, autoIncCorrId++);
		proxy.subscribeButton(ButtonName.PRESET_7, autoIncCorrId++);
		proxy.subscribeButton(ButtonName.PRESET_8, autoIncCorrId++);
		proxy.subscribeButton(ButtonName.PRESET_9, autoIncCorrId++);
		proxy.subscribeButton(ButtonName.PRESET_0, autoIncCorrId++);
	} catch (SyncException e) {}
}*/

	public void onOnDriverDistraction(OnDriverDistraction notification) {
		driverDistractionNotif = true;
		// Log.i(TAG, "dd: " + notification.getStringState());
		if (notification.getState() == DriverDistractionState.DD_OFF) {
			Log.i(TAG, "clear lock, DD_OFF");
			clearLockScreen();

		} else {
			Log.i(TAG, "show lockscreen, DD_ON");
			showLockScreen();
		}
	}

public void onError(String info, Exception e) {
	// TODO Auto-generated method stub
}

public void onGenericResponse(GenericResponse response) {
	// TODO Auto-generated method stub
}

	public void onOnCommand(OnCommand notification) {
		Log.i(TAG, "onOnCommand()");
		if (notification.getCmdID() >= START_CMD_ID_MAIN_ACTIVITY_AL) {
			Log.i(TAG, "notification.getCmdID() >= START_CMD_ID_MAIN_ACTIVITY_AL");
			if (!(iProxyListenerALM instanceof MainActivityAL)) {
				Log.i(TAG, "!(iProxyListenerALM instanceof MainActivityAL)");
				iProxyListenerALM = MainActivityAL.getInstance((EventSeekr) getApplication());
			}
		}
		iProxyListenerALM.onOnCommand(notification);
	}

public void onAddCommandResponse(AddCommandResponse response) {
	// TODO Auto-generated method stub
}

public void onAddSubMenuResponse(AddSubMenuResponse response) {
	// TODO Auto-generated method stub
}

	public void onCreateInteractionChoiceSetResponse(CreateInteractionChoiceSetResponse response) {
		Log.i(TAG, "onCreateInteractionChoiceSetResponse(), response: " + response.getInfo() + ", " 
				+ response.getMessageType() + ", " + response.getResultCode());
	}

public void onAlertResponse(AlertResponse response) {
	// TODO Auto-generated method stub
}

public void onDeleteCommandResponse(DeleteCommandResponse response) {
	// TODO Auto-generated method stub
}

public void onDeleteInteractionChoiceSetResponse(
		DeleteInteractionChoiceSetResponse response) {
	// TODO Auto-generated method stub
}

public void onDeleteSubMenuResponse(DeleteSubMenuResponse response) {
	// TODO Auto-generated method stub
}

public void onEncodedSyncPDataResponse(EncodedSyncPDataResponse response) {
	// TODO Auto-generated method stub
}

	public void onPerformInteractionResponse(PerformInteractionResponse response) {
		Log.i(TAG, "onPerformInteractionResponse()");
		iProxyListenerALM.onPerformInteractionResponse(response);
	}

public void onResetGlobalPropertiesResponse(
		ResetGlobalPropertiesResponse response) {
	// TODO Auto-generated method stub
}

public void onSetGlobalPropertiesResponse(SetGlobalPropertiesResponse response) {
}

public void onSetMediaClockTimerResponse(SetMediaClockTimerResponse response) {
	// TODO Auto-generated method stub
}

public void onShowResponse(ShowResponse response) {
	// TODO Auto-generated method stub
}

	public void onSpeakResponse(SpeakResponse response) {
		iProxyListenerALM.onSpeakResponse(response);
	}

public void onOnButtonEvent(OnButtonEvent notification) {
	// TODO Auto-generated method stub
}

	public void onOnButtonPress(OnButtonPress notification) {
		iProxyListenerALM.onOnButtonPress(notification);
	}

public void onSubscribeButtonResponse(SubscribeButtonResponse response) {
	// TODO Auto-generated method stub
}

public void onUnsubscribeButtonResponse(UnsubscribeButtonResponse response) {
	// TODO Auto-generated method stub	
}

public void onOnPermissionsChange(OnPermissionsChange notification) {
	// TODO Auto-generated method stub	
}

public void onOnEncodedSyncPData(OnEncodedSyncPData notification) {
	// TODO Auto-generated method stub
}

public void onOnTBTClientState(OnTBTClientState notification) {
	// TODO Auto-generated method stub
}

@Override
public IBinder onBind(Intent intent) {
	// TODO Auto-generated method stub
	return null;
}
}
