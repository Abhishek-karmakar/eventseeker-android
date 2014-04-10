package com.wcities.eventseeker.applink.handler;

import java.util.Vector;

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
import com.ford.syncV4.proxy.rpc.enums.ButtonName;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.applink.interfaces.ESIProxyListener;
import com.wcities.eventseeker.applink.service.AppLinkService;
import com.wcities.eventseeker.applink.util.CommandsUtil;
import com.wcities.eventseeker.applink.util.CommandsUtil.Commands;

public class MainAL implements ESIProxyListener {
	
	private static final String TAG = MainAL.class.getName();
	
	private EventSeekr context;
	private static MainAL instance;
	
	public static MainAL getInstance(EventSeekr context) {
		if (instance == null) {
			instance = new MainAL(context);
		}
		return instance;
	}
	
	private MainAL(EventSeekr context) {
		this.context = context;
	}

	@Override
	public void onOnButtonPress(OnButtonPress notification) {	
		ButtonName btnName = notification.getButtonName();
		Commands cmd = Commands.getCommandByButtonName(btnName);
		CommandsUtil.performOperationForCommand(cmd );
	}


	@Override
	public void onOnCommand(OnCommand notification) {
		Commands cmd = Commands.getCommandById(notification.getCmdID());
		CommandsUtil.performOperationForCommand(cmd);
	}
	
	@Override
	public void onOnHMIStatus(OnHMIStatus notification) {
		/*switch (notification.getHmiLevel()) {
		
		case HMI_FULL:
			
			AppLinkService.getInstance().showWelcomeMsg();
			addCommands();
			break;

		default:
			return;
		}*/
	}
	
	@Override
	public void initiateInterAction() {
		AppLinkService.getInstance().showWelcomeMsg();
		addCommands();
	}
	
	private void addCommands() {
		Vector<Commands> reqCmds = new Vector<Commands>();
		reqCmds.add(Commands.DISCOVER);
		reqCmds.add(Commands.MY_EVENTS);
		reqCmds.add(Commands.SEARCH);
		CommandsUtil.addCommands(reqCmds);
	}
	
	@Override
	public void onAddCommandResponse(AddCommandResponse arg0) {}

	@Override
	public void onAddSubMenuResponse(AddSubMenuResponse arg0) {}

	@Override
	public void onAlertResponse(AlertResponse arg0) {}

	@Override
	public void onCreateInteractionChoiceSetResponse(CreateInteractionChoiceSetResponse arg0) {}

	@Override
	public void onDeleteCommandResponse(DeleteCommandResponse arg0) {}

	@Override
	public void onDeleteInteractionChoiceSetResponse(DeleteInteractionChoiceSetResponse arg0) {}

	@Override
	public void onDeleteSubMenuResponse(DeleteSubMenuResponse arg0) {}

	@Override
	public void onEncodedSyncPDataResponse(EncodedSyncPDataResponse arg0) {}

	@Override
	public void onError(String arg0, Exception arg1) {}

	@Override
	public void onGenericResponse(GenericResponse arg0) {}

	@Override
	public void onOnButtonEvent(OnButtonEvent arg0) {}

	@Override
	public void onOnPermissionsChange(OnPermissionsChange arg0) {}

	@Override
	public void onPerformInteractionResponse(PerformInteractionResponse arg0) {}

	@Override
	public void onProxyClosed(String arg0, Exception arg1) {}

	@Override
	public void onResetGlobalPropertiesResponse(ResetGlobalPropertiesResponse arg0) {}

	@Override
	public void onSetGlobalPropertiesResponse(SetGlobalPropertiesResponse arg0) {}

	@Override
	public void onSetMediaClockTimerResponse(SetMediaClockTimerResponse arg0) {}

	@Override
	public void onShowResponse(ShowResponse arg0) {}

	@Override
	public void onSpeakResponse(SpeakResponse arg0) {}

	@Override
	public void onSubscribeButtonResponse(SubscribeButtonResponse arg0) {}

	@Override
	public void onUnsubscribeButtonResponse(UnsubscribeButtonResponse arg0) {}

	@Override
	public void onOnDriverDistraction(OnDriverDistraction arg0) {}

	@Override
	public void onOnEncodedSyncPData(OnEncodedSyncPData arg0) {}

	@Override
	public void onOnTBTClientState(OnTBTClientState arg0) {}

}
