package com.wcities.eventseeker.applink.handler;

import android.util.Log;

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
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.applink.interfaces.ESIProxyListener;

public class SearchAL implements ESIProxyListener {

	private static final String TAG = SearchAL.class.getName();
	private static SearchAL instance;
	private EventSeekr context;

	public SearchAL(EventSeekr context) {
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
	public void onCreateInteractionChoiceSetResponse(CreateInteractionChoiceSetResponse arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onDeleteCommandResponse(DeleteCommandResponse arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onDeleteInteractionChoiceSetResponse(DeleteInteractionChoiceSetResponse arg0) {
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
	public void onOnButtonPress(OnButtonPress arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onOnCommand(OnCommand arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onOnHMIStatus(OnHMIStatus arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onOnPermissionsChange(OnPermissionsChange arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onPerformInteractionResponse(PerformInteractionResponse arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onProxyClosed(String arg0, Exception arg1) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onResetGlobalPropertiesResponse(ResetGlobalPropertiesResponse arg0) {
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

	@Override
	public void initiateInterAction() {
		// TODO Auto-generated method stub
	}

	public static ESIProxyListener getInstance(EventSeekr context) {
		if (instance == null) {
			Log.i(TAG, "instance is null");
			instance = new SearchAL(context);
		}
		Log.i(TAG, "return instance");
		return instance;
	}

}
