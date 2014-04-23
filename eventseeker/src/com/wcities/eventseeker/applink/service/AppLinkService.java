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
import android.widget.Toast;

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
import com.ford.syncV4.proxy.rpc.DialNumberResponse;
import com.ford.syncV4.proxy.rpc.EncodedSyncPDataResponse;
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
import com.ford.syncV4.proxy.rpc.OnEncodedSyncPData;
import com.ford.syncV4.proxy.rpc.OnHMIStatus;
import com.ford.syncV4.proxy.rpc.OnLanguageChange;
import com.ford.syncV4.proxy.rpc.OnPermissionsChange;
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
import com.ford.syncV4.proxy.rpc.UnsubscribeButtonResponse;
import com.ford.syncV4.proxy.rpc.UnsubscribeVehicleDataResponse;
import com.ford.syncV4.proxy.rpc.enums.ButtonName;
import com.ford.syncV4.proxy.rpc.enums.DriverDistractionState;
import com.ford.syncV4.proxy.rpc.enums.Language;
import com.ford.syncV4.transport.TCPTransportConfig;
import com.wcities.eventseeker.LanguageFragment.Locales;
import com.wcities.eventseeker.LockScreenActivity;
import com.wcities.eventseeker.MainActivity;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.applink.handler.DiscoverAL;
import com.wcities.eventseeker.applink.handler.ESIProxyALM;
import com.wcities.eventseeker.applink.handler.MainAL;
import com.wcities.eventseeker.applink.handler.MyEventsAL;
import com.wcities.eventseeker.applink.handler.SearchAL;
import com.wcities.eventseeker.applink.util.CommandsUtil.Command;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.util.FragmentUtil;

public class AppLinkService extends Service implements IProxyListenerALM {

	private static final String TAG = AppLinkService.class.getSimpleName();
	
	public static final int CMD_ID_AL = 1;
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
	private ESIProxyALM esIProxyALM;
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
	
	public ESIProxyALM getESIProxyListener() {
		return esIProxyALM;
	}

	public void setESIProxyListener(ESIProxyALM esIProxyListener) {
		this.esIProxyALM = esIProxyListener;
	}

	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "onCreate()");
		instance = this;
	}
	
	public int onStartCommand(Intent intent, int flags, int startId) {
		//Log.d(TAG, "onStartCommand()");
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
		
        if (MainActivity.getInstance() != null) {
        	setCurrentActivity(MainActivity.getInstance());
        }
			
        return START_STICKY;
	}
	
	public void startProxy() {
		Log.d(TAG, "startProxy()");
		if (proxy == null) {
			try {
				if (AppConstants.DEBUG) {
					//Log.d(TAG, "startProxy with TCP");
					proxy = new SyncProxyALM(this, getResources().getString(R.string.app_name), true, 
							AppConstants.FORD_APP_ID, new TCPTransportConfig(AppConstants.TCP_PORT, 
									AppConstants.TCP_IP_ADDRESS, true));
					
				} else {
					Language language = ((EventSeekr)getApplication()).getFordLocale().getFordLanguage();
					SyncMsgVersion syncMsgVersion = new SyncMsgVersion();
					syncMsgVersion.setMajorVersion(1);
					syncMsgVersion.setMinorVersion(1);
					proxy = new SyncProxyALM(this, getResources().getString(R.string.app_name), null, 
							new Vector<String>(Arrays.asList(new String[] {getResources().getString(
							R.string.app_name)})), true, syncMsgVersion, language, language, 
							AppConstants.FORD_APP_ID, null);
					Log.d(TAG, "startProxy() registration done");
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
			clearLockScreen();
		}
	}
	
	public void onProxyClosed(String info, Exception e) {
		Log.d(TAG, "onProxyClosed()");
		clearLockScreen();

		if ((((SyncException) e).getSyncExceptionCause() != SyncExceptionCause.SYNC_PROXY_CYCLED)) {
			if (((SyncException) e).getSyncExceptionCause() != SyncExceptionCause.BLUETOOTH_DISABLED) {
				Log.v(TAG, "reset proxy in onproxy closed");
				reset();
			}
		}
	}

   public void reset() {
	   Log.d(TAG, "reset()");
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
   
   /*public void show(String mainText1, String mainText2, TextAlignment alignment) {
		try {
			proxy.show(mainText1, mainText2, alignment, autoIncCorrId++);

		} catch (SyncException e) {
			DebugTool.logError("Failed to send Show", e);
		}
   }*/
   
   /*public void showWelcomeMsg() {
		//String welcomeMsg1 = "Inside", welcomeMsg2 = "eventseekr!";
	   	String welcomeMsg1 = AppLinkService.getStringFromRes(R.string.msg_welcome_to), 
	   	welcomeMsg2 = AppLinkService.getStringFromRes(R.string.msg_eventseeker);
		show(welcomeMsg1, welcomeMsg2, TextAlignment.CENTERED);
	}*/
   
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
		  
		Log.d(TAG, "onOnHMIStatus(), " + notification.getSystemContext().name());
		switch (notification.getHmiLevel()) {
		
		case HMI_FULL:			
			Log.d(TAG, "onOnHMIStatus(), HMI_FULL, driverDistractionNotif = " + driverDistractionNotif);
			/**
			 * Commented driverDistractionNotif check because it doesn't open lock screen when user 
			 * goes to change language in between from sync tdk & then returns back to app
			 */
			//if (driverDistractionNotif == false) {
			showLockScreen();
			//}
			
			if (notification.getFirstRun()) {
				
				if (MainActivity.getInstance() != null) {
					setCurrentActivity(MainActivity.getInstance());
				}
				
				/**
				 * Subscribe for the buttons
				 */
				subscribeButtons();
				// setup app on SYNC
				// send welcome message if applicable
				esIProxyALM = MainAL.getInstance((EventSeekr) getApplication());
				//esIProxyALM.onOnHMIStatus(notification);
				//esIProxyALM.onCreateInstance();
				esIProxyALM.onStartInstance();
				
			} else if (isHMIStatusNone) {
				// In case if user had exited app & revisits the app, display welcome msg. No need to add commands again.
				isHMIStatusNone = false;
				//showWelcomeMsg();
				/**
				 * showing welcome message from here is commented as in between of the app also when this event
				 * gets fired it removes the current text from screen and prints welcome message. This issue was
				 * happening on Discover screen when first 10 events gets loaded and system shows the info of
				 * first event on screen and currently speaking for the first event and then suddenly welcome 
				 * message gets appear.
				 */
				//ALUtil.displayMessage(R.string.msg_welcome_to, R.string.msg_eventseeker);
			}
			break;
			
		case HMI_LIMITED:
			Log.d(TAG, "onOnHMIStatus(), HMI_LIMITED, driverDistractionNotif = " + driverDistractionNotif);
			if (driverDistractionNotif == false) {
				showLockScreen();
			}
			break;
			
		case HMI_BACKGROUND:
			Log.d(TAG, "onOnHMIStatus(), HMI_BACKGROUND, driverDistractionNotif = " + driverDistractionNotif);
			if (driverDistractionNotif == false) {
				showLockScreen();
			}
			break;
			
		case HMI_NONE:
			Log.d(TAG, "onOnHMIStatus(), HMI_NONE");
			driverDistractionNotif = false;
			isHMIStatusNone = true;
			clearLockScreen();
			/**
			 * This is called in 2 cases:
			 * 1) just after registration of app with SYNC - As soon as user selects our app, we will get notification
			 * for HMI_FULL after which only we can initiate making calls. This will be fulfilled by corresponding 
			 * HMI_FULL case under if block with condition notification.getFirstRun(). So esIProxyALM initialization
			 * here is redundant.
			 * 2) on exit - The application will be returned to NONE when the user selects "Exit <app_name>" 
			 * via the menu or PTT VR command. The user has opted out of using the application at this point 
			 * and the application may not send requests. The application’s registration, button subscriptions, 
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
			esIProxyALM = MainAL.getInstance((EventSeekr) getApplication());
			esIProxyALM.onStartInstance();
			break;
			
		default:
			return;
		}
	}
   
	private void showLockScreen() {
		Log.d(TAG, "showLockScreen()");
		// only throw up lockscreen if main activity is currently on top
		// else, wait until onResume() to throw lockscreen so it doesn't
		// pop-up while a user is using another app on the phone
		if (currentUIActivity != null) {
			if (currentUIActivity.isActivityonTop()) {
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
		Log.d(TAG, "clearlockscreen()");
		if (LockScreenActivity.getInstance() != null) {
			LockScreenActivity.getInstance().exit();
		}
		lockscreenUP = false;
	}

	public boolean getLockScreenStatus() {return lockscreenUP;}
	
	public void subscribeButtons() {
		try {
	        proxy.subscribeButton(ButtonName.OK, autoIncCorrId++);
	        proxy.subscribeButton(ButtonName.SEEKLEFT, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.SEEKRIGHT, autoIncCorrId++);
			// TODO: Remove following subscriptions
			proxy.subscribeButton(ButtonName.PRESET_0, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.PRESET_1, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.PRESET_2, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.PRESET_3, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.PRESET_4, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.PRESET_5, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.PRESET_6, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.PRESET_7, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.PRESET_8, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.PRESET_9, autoIncCorrId++);
		} catch (SyncException e) {}
	}
	
	/*public void unSubscribeButtons() {
		try {
			proxy.unsubscribeButton(ButtonName.OK, autoIncCorrId++);
			proxy.unsubscribeButton(ButtonName.SEEKLEFT, autoIncCorrId++);
			proxy.unsubscribeButton(ButtonName.SEEKRIGHT, autoIncCorrId++);
			proxy.unsubscribeButton(ButtonName.PRESET_0, autoIncCorrId++);
			proxy.unsubscribeButton(ButtonName.PRESET_1, autoIncCorrId++);
			proxy.unsubscribeButton(ButtonName.PRESET_2, autoIncCorrId++);
			proxy.unsubscribeButton(ButtonName.PRESET_3, autoIncCorrId++);
			proxy.unsubscribeButton(ButtonName.PRESET_4, autoIncCorrId++);
			proxy.unsubscribeButton(ButtonName.PRESET_5, autoIncCorrId++);
			proxy.unsubscribeButton(ButtonName.PRESET_6, autoIncCorrId++);
			proxy.unsubscribeButton(ButtonName.PRESET_7, autoIncCorrId++);
			proxy.unsubscribeButton(ButtonName.PRESET_8, autoIncCorrId++);
			proxy.unsubscribeButton(ButtonName.PRESET_9, autoIncCorrId++);
		} catch (SyncException e) {}
	}*/

	public void onOnDriverDistraction(OnDriverDistraction notification) {
		driverDistractionNotif = true;
		// Log.d(TAG, "dd: " + notification.getStringState());
		if (notification.getState() == DriverDistractionState.DD_OFF) {
			Log.d(TAG, "clear lock, DD_OFF");
			clearLockScreen();

		} else {
			Log.d(TAG, "show lockscreen, DD_ON");
			showLockScreen();
		}
	}

	public void onOnCommand(OnCommand notification) {
		Log.d(TAG, "onOnCommand");
		/*
		 * notification obj structure :
		 * {"notification": {"name": "OnCommand", "parameters": {"cmdID": "3", "triggerSource": "MENU"}}}
		 */
		//int cmdId = Integer.parseInt(notification.getParameters("cmdID").toString());
		esIProxyALM.onOnCommand(notification);
	}

	public void onCreateInteractionChoiceSetResponse(CreateInteractionChoiceSetResponse response) {
		Log.d(TAG, "onCreateInteractionChoiceSetResponse(), response: " + response.getInfo() + ", " 
				+ response.getMessageType() + ", " + response.getResultCode());
	}

	public void onPerformInteractionResponse(PerformInteractionResponse response) {
		Log.d(TAG, "onPerformInteractionResponse()");
		esIProxyALM.onPerformInteractionResponse(response);
	}

	public void onSpeakResponse(SpeakResponse response) {
		esIProxyALM.onSpeakResponse(response);
	}
	
	public void onOnButtonPress(OnButtonPress notification) {
		ButtonName btnName = notification.getButtonName();
		Command cmd = null;
		if (btnName == ButtonName.CUSTOM_BUTTON) {
			cmd = Command.getCommandById(notification.getCustomButtonName());
			
		} else {
			cmd = Command.getCommandByButtonName(btnName);
		}
		esIProxyALM.performOperationForCommand(cmd);
	}
	
	/**
	 * must be called when Discver, My Events and search commands are invoked
	 * cmd - non null value
	 * @param cmd
	 */
	public void initiateESIProxyListener(Command cmd) {
		switch (cmd) {
			case DISCOVER:
				Log.d(TAG, "DISCOVER");
				esIProxyALM =  DiscoverAL.getInstance((EventSeekr) getApplication());
				break;
			case MY_EVENTS:
				Log.d(TAG, "MY EVENTS");
				esIProxyALM =  MyEventsAL.getInstance((EventSeekr) getApplication());
				break;
			case SEARCH:
				Log.d(TAG, "SEARCH");
				esIProxyALM =  SearchAL.getInstance((EventSeekr) getApplication());
				break;
		}
		if (esIProxyALM == null) {
			return;
		}
		esIProxyALM.onStartInstance();
	}

	public void onError(String info, Exception e) {}
	
	public void onGenericResponse(GenericResponse response) {}

	public void onAddCommandResponse(AddCommandResponse response) {}
	
	public void onAddSubMenuResponse(AddSubMenuResponse response) {}

	public void onAlertResponse(AlertResponse response) {}
	
	public void onDeleteCommandResponse(DeleteCommandResponse response) {}
	
	public void onDeleteInteractionChoiceSetResponse(DeleteInteractionChoiceSetResponse response) {}
	
	public void onDeleteSubMenuResponse(DeleteSubMenuResponse response) {}
	
	public void onEncodedSyncPDataResponse(EncodedSyncPDataResponse response) {}

	public void onResetGlobalPropertiesResponse(ResetGlobalPropertiesResponse response) {}
	
	public void onSetGlobalPropertiesResponse(SetGlobalPropertiesResponse response) {}
	
	public void onSetMediaClockTimerResponse(SetMediaClockTimerResponse response) {}
	
	public void onShowResponse(ShowResponse response) {}
	
	public void onOnButtonEvent(OnButtonEvent notification) {}
	
	public void onSubscribeButtonResponse(SubscribeButtonResponse response) {}
	
	public void onUnsubscribeButtonResponse(UnsubscribeButtonResponse response) {}
	
	public void onOnPermissionsChange(OnPermissionsChange notification) {}
	
	public void onOnEncodedSyncPData(OnEncodedSyncPData notification) {}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onChangeRegistrationResponse(ChangeRegistrationResponse arg0) {
		
	}

	@Override
	public void onDeleteFileResponse(DeleteFileResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDialNumberResponse(DialNumberResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onEndAudioPassThruResponse(EndAudioPassThruResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onGetDTCsResponse(GetDTCsResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onGetVehicleDataResponse(GetVehicleDataResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onListFilesResponse(ListFilesResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onOnAudioPassThru(OnAudioPassThru arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onOnLanguageChange(final OnLanguageChange arg0) {
		Log.d(TAG, "onOnLanguageChange to " + arg0.getLanguage().name() + ", " + 
				arg0.getHmiDisplayLanguage().name());
		/*currentUIActivity.runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				Toast.makeText(currentUIActivity, "onOnLanguageChange to " + arg0.getLanguage().name() + ", " + 
						arg0.getHmiDisplayLanguage().name(), Toast.LENGTH_LONG).show();
			}
		});*/
		
		Language language = arg0.getLanguage();
		((EventSeekr) getApplication()).updateFordLocale(Locales.getFordLocaleByLanguage(language));
	}

	@Override
	public void onOnVehicleData(OnVehicleData arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPerformAudioPassThruResponse(PerformAudioPassThruResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPutFileResponse(PutFileResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onReadDIDResponse(ReadDIDResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onScrollableMessageResponse(ScrollableMessageResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSetAppIconResponse(SetAppIconResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSetDisplayLayoutResponse(SetDisplayLayoutResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSliderResponse(SliderResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSubscribeVehicleDataResponse(SubscribeVehicleDataResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onUnsubscribeVehicleDataResponse(
			UnsubscribeVehicleDataResponse arg0) {
		// TODO Auto-generated method stub
		
	}
}