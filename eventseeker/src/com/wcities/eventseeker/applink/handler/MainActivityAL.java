package com.wcities.eventseeker.applink.handler;

import java.util.Arrays;
import java.util.Vector;

import android.util.Log;

import com.ford.syncV4.exception.SyncException;
import com.ford.syncV4.proxy.interfaces.IProxyListenerALM;
import com.ford.syncV4.proxy.rpc.AddCommand;
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
import com.ford.syncV4.proxy.rpc.UnsubscribeButtonResponse;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.applink.service.AppLinkService;
import com.wcities.eventseeker.applink.util.ALUtil;

public class MainActivityAL implements IProxyListenerALM {
	
	public enum CmdId {
		MY_EVENTS(AppLinkService.CMD_ID_AL),
		DISCOVER(AppLinkService.CMD_ID_AL + 1),
		SEARCH(AppLinkService.CMD_ID_AL + 2);
		
		private int value;
		
		private CmdId(int value) {
			this.value = value;
		}
		
		public static CmdId getCmdId(int value) {
			CmdId[] ids = CmdId.values();
			for (int i = 0; i < ids.length; i++) {
				CmdId cmdId = ids[i];
				if (cmdId.value == value) {
					return cmdId;
				}
			}
			return null;
		}
		
		@Override
		public String toString() {
			String str = null;
			
			switch (this) {
			
			case MY_EVENTS:
				str = "My Events";
				break;
				
			case DISCOVER:
				str = "Discover";
				break;
				
			case SEARCH:
				str = "Search";
				break;

			default:
				break;
			}
			return str;
		}
	}

	private static final String TAG = MainActivityAL.class.getName();
	
	private EventSeekr context;
	private static MainActivityAL instance;
	
	public static MainActivityAL getInstance(EventSeekr context) {
		if (instance == null) {
			instance = new MainActivityAL(context);
		}
		return instance;
	}
	
	private MainActivityAL(EventSeekr context) {
		this.context = context;
	}
	
	@Override
	public void onAddCommandResponse(AddCommandResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAddSubMenuResponse(AddSubMenuResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAlertResponse(AlertResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onCreateInteractionChoiceSetResponse(
			CreateInteractionChoiceSetResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDeleteCommandResponse(DeleteCommandResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDeleteInteractionChoiceSetResponse(
			DeleteInteractionChoiceSetResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDeleteSubMenuResponse(DeleteSubMenuResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onEncodedSyncPDataResponse(EncodedSyncPDataResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onError(String arg0, Exception arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onGenericResponse(GenericResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onOnButtonEvent(OnButtonEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onOnButtonPress(OnButtonPress notification) {
		
		/*switch (notification.getButtonName()) {
		
		case OK:
			DiscoverActivityAL discoverActivityAL = new DiscoverActivityAL(eventSeekr);
			AppLinkService.getInstance().setIProxyListenerALM(discoverActivityAL);
			break;
			
		case SEEKLEFT:
			break;
			
		case SEEKRIGHT:
			break;
			
		case TUNEUP:
			break;
			
		case TUNEDOWN:
			break;
			
		default:
			break;
		}*/		
	}

	@Override
	public void onOnCommand(OnCommand notification) {
	/*	CmdId cmdId = CmdId.getCmdId(notification.getCmdID());
		Log.i(TAG, "onOnCommand(), cmdID = " + notification.getCmdID());
		switch (cmdId) {
		
		case MY_EVENTS:
			Log.i(TAG, "My Events");
			MyEventsActivityAL myEventsActivityAL = MyEventsActivityAL.getInstance(context);
			AppLinkService.getInstance().setESIProxyListener(myEventsActivityAL);
			myEventsActivityAL.onCreateInstance();
			break;
		
		case DISCOVER:
			Log.i(TAG, "DISCOVER");
			DiscoverActivityAL discoverActivityAL = DiscoverActivityAL.getInstance(context);
			AppLinkService.getInstance().setESIProxyListener(discoverActivityAL);
			discoverActivityAL.onCreateInstance();
			break;

		default:
			break;
		}*/
	}
	
	@Override
	public void onOnHMIStatus(OnHMIStatus notification) {
		switch (notification.getHmiLevel()) {
		
		case HMI_FULL:
			// send welcome message
			/*if (eventSeekr.getFbUserId() == null) {
				welcomeMsg1 = "You have not";
				welcomeMsg2 = "logged in";

			} else {
				welcomeMsg1 = "You have logged";
				welcomeMsg2 = "in using facebook";
			}*/
			
			//AppLinkService.getInstance().showWelcomeMsg();
			ALUtil.displayMessage(R.string.msg_welcome_to, R.string.msg_eventseeker);
			addCommands();
			break;

		default:
			return;
		}
	}
	
	private void addCommands() {
		AppLinkService appLinkService = AppLinkService.getInstance();
		
		CmdId[] cmdIds = CmdId.values();
		
		for (int i = 0; i < 3; i++) {
			AddCommand msg = new AddCommand();
			msg.setCorrelationID(appLinkService.autoIncCorrId++);
			msg.setVrCommands(new Vector<String>(Arrays.asList(new String[] {cmdIds[i].toString()})));
			msg.setCmdID(cmdIds[i].value);
			
			try {
				appLinkService.getProxy().sendRPCRequest(msg);
				
			} catch (SyncException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onOnPermissionsChange(OnPermissionsChange arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPerformInteractionResponse(PerformInteractionResponse arg0) {
		// TODO Auto-generated method stub
		Log.i(TAG, "onPerformInteractionResponse()");
	}

	@Override
	public void onProxyClosed(String arg0, Exception arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onResetGlobalPropertiesResponse(
			ResetGlobalPropertiesResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSetGlobalPropertiesResponse(SetGlobalPropertiesResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSetMediaClockTimerResponse(SetMediaClockTimerResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onShowResponse(ShowResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSpeakResponse(SpeakResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSubscribeButtonResponse(SubscribeButtonResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onUnsubscribeButtonResponse(UnsubscribeButtonResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onOnDriverDistraction(OnDriverDistraction arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onOnEncodedSyncPData(OnEncodedSyncPData arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onOnTBTClientState(OnTBTClientState arg0) {
		// TODO Auto-generated method stub
		
	}

}
