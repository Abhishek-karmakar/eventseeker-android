package com.wcities.eventseeker.applink.handler;

import android.os.Bundle;

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
import com.ford.syncV4.proxy.rpc.SystemRequestResponse;
import com.ford.syncV4.proxy.rpc.UnsubscribeButtonResponse;
import com.ford.syncV4.proxy.rpc.UnsubscribeVehicleDataResponse;
import com.ford.syncV4.proxy.rpc.enums.SyncDisconnectedReason;
import com.wcities.eventseeker.applink.util.CommandsUtil.Command;
import com.wcities.eventseeker.applink.util.EventALUtil;

public abstract class ESIProxyALM implements IProxyListenerALM {

    private static final String TAG = ESIProxyALM.class.getSimpleName();
    private Bundle args;

    public abstract void onStartInstance();
    public abstract void performOperationForCommand(Command cmd, boolean isTriggerSrcMenu);

    public Bundle getArguments() {
        return args;
    }

    public void setArguments(Bundle args) {
        this.args = args;
    }

    @Override
    public void onOnButtonPress(OnButtonPress response) {}

    @Override
    public void onOnCommand(final OnCommand response) {
		/*AppLinkService.getInstance().getCurrentActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try {
					Toast.makeText(AppLinkService.getInstance().getCurrentActivity(), "onOnCommand : " + response.getCmdID() 
						+ " Response : " + response.serializeJSON().toString(), Toast.LENGTH_SHORT).show();
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		});*/
        //Logger.d(TAG, "onOnCommand : " + response.getCmdID());
    }

    @Override
    public void onPerformInteractionResponse(final PerformInteractionResponse response) {
		/*AppLinkService.getInstance().getCurrentActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(AppLinkService.getInstance().getCurrentActivity(), "PerformInteractionCS : " + 
					response.getInfo() + " Response : " + response.getResultCode(), Toast.LENGTH_SHORT).show();
			}
		});*/
    }

    @Override
    public void onAddCommandResponse(final AddCommandResponse response) {
		/*AppLinkService.getInstance().getCurrentActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(AppLinkService.getInstance().getCurrentActivity(), "AddCommand : " 
					+ response.getInfo() + " Response : " + response.getResultCode(), Toast.LENGTH_SHORT).show();
			}
		});*/
        //Logger.d(TAG, "onAddCommandResponse : " + response.getResultCode());
    }

    @Override
    public void onAddSubMenuResponse(AddSubMenuResponse response) {}

    /**
     * If overridden in sub classes then don't forget to call the super implementation
     */
    @Override
    public void onAlertResponse(final AlertResponse response) {
        EventALUtil.isAlertForNoEventsAvailable = false;
		/*AppLinkService.getInstance().getCurrentActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(AppLinkService.getInstance().getCurrentActivity(), "Alert : " 
					+ response.getInfo() + " Response : " + response.getResultCode(), Toast.LENGTH_SHORT).show();
			}
		});*/
    }

    @Override
    public void onCreateInteractionChoiceSetResponse(final CreateInteractionChoiceSetResponse response) {
		/*AppLinkService.getInstance().getCurrentActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(AppLinkService.getInstance().getCurrentActivity(), "CreateInteractionCS : " 
					+ response.getInfo() + " Response : " + response.getResultCode(), Toast.LENGTH_SHORT).show();
			}
		});*/
    }

    @Override
    public void onDeleteCommandResponse(final DeleteCommandResponse response) {
		/*AppLinkService.getInstance().getCurrentActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(AppLinkService.getInstance().getCurrentActivity(), "DeleteCommand : " 
					+ response.getInfo() + " Response : " + response.getResultCode(), Toast.LENGTH_SHORT).show();
			}
		});*/
        //Logger.d(TAG, "onDeleteCommandResponse : " + response.getResultCode());
    }

    @Override
    public void onDeleteInteractionChoiceSetResponse(final DeleteInteractionChoiceSetResponse response) {
		/*AppLinkService.getInstance().getCurrentActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(AppLinkService.getInstance().getCurrentActivity(), "onDeleteInteractionCS : " 
					+ response.getInfo() + " Response : " + response.getResultCode(), Toast.LENGTH_SHORT).show();
			}
		});*/
    }

    @Override
    public void onDeleteSubMenuResponse(DeleteSubMenuResponse response) {}

    @Override
    public void onError(String response, Exception arg1) {}

    @Override
    public void onGenericResponse(GenericResponse response) {}

    @Override
    public void onOnButtonEvent(OnButtonEvent response) {}

    @Override
    public void onOnHMIStatus(final OnHMIStatus response) {
		/*AppLinkService.getInstance().getCurrentActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(AppLinkService.getInstance().getCurrentActivity(), "onOnHMIStatus : " +
					"Response level : " + response.getHmiLevel(), Toast.LENGTH_SHORT).show();
			}
		});*/
    }

    @Override
    public void onOnPermissionsChange(OnPermissionsChange response) {}

    @Override
    public void onResetGlobalPropertiesResponse(ResetGlobalPropertiesResponse response) {}

    @Override
    public void onSetGlobalPropertiesResponse(SetGlobalPropertiesResponse response) {}

    @Override
    public void onSetMediaClockTimerResponse(SetMediaClockTimerResponse response) {}

    @Override
    public void onShowResponse(ShowResponse response) {}

    @Override
    public void onSpeakResponse(final SpeakResponse response) {
		/*AppLinkService.getInstance().getCurrentActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(AppLinkService.getInstance().getCurrentActivity(), "onSpeak : " 
					+ response.getInfo() + " Response : " + response.getResultCode(), Toast.LENGTH_SHORT).show();
			}
		});*/
    }

    @Override
    public void onSubscribeButtonResponse(SubscribeButtonResponse response) {}

    @Override
    public void onUnsubscribeButtonResponse(UnsubscribeButtonResponse response) {}

    @Override
    public void onOnDriverDistraction(OnDriverDistraction response) {}

    @Override
    public void onChangeRegistrationResponse(ChangeRegistrationResponse response) {}

    @Override
    public void onDeleteFileResponse(DeleteFileResponse response) {}

    @Override
    public void onEndAudioPassThruResponse(EndAudioPassThruResponse response) {}

    @Override
    public void onGetDTCsResponse(GetDTCsResponse response) {}

    @Override
    public void onGetVehicleDataResponse(GetVehicleDataResponse response) {}

    @Override
    public void onListFilesResponse(ListFilesResponse response) {}

    @Override
    public void onOnAudioPassThru(OnAudioPassThru response) {}

    @Override
    public void onOnLanguageChange(OnLanguageChange response) {}

    @Override
    public void onOnVehicleData(OnVehicleData response) {}

    @Override
    public void onPerformAudioPassThruResponse(PerformAudioPassThruResponse response) {}

    @Override
    public void onPutFileResponse(PutFileResponse response) {}

    @Override
    public void onReadDIDResponse(ReadDIDResponse response) {}

    @Override
    public void onScrollableMessageResponse(ScrollableMessageResponse response) {}

    @Override
    public void onSetAppIconResponse(SetAppIconResponse response) {}

    @Override
    public void onSetDisplayLayoutResponse(SetDisplayLayoutResponse response) {}

    @Override
    public void onSliderResponse(SliderResponse response) {}

    @Override
    public void onSubscribeVehicleDataResponse(final SubscribeVehicleDataResponse response) {
		/*AppLinkService.getInstance().getCurrentActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(AppLinkService.getInstance().getCurrentActivity(), "SubscribeVehicleData : " 
					+ response.getInfo() + " Response : " + response.getResultCode(), Toast.LENGTH_SHORT).show();
			}
		});*/
    }

    @Override
    public void onUnsubscribeVehicleDataResponse(UnsubscribeVehicleDataResponse response) {}

    @Override
    public void onOnTBTClientState(OnTBTClientState response) {}

    @Override
    public void onDiagnosticMessageResponse(DiagnosticMessageResponse arg0) {}

    @Override
    public void onOnHashChange(OnHashChange arg0) {}

    @Override
    public void onOnKeyboardInput(OnKeyboardInput arg0) {}

    @Override
    public void onOnLockScreenNotification(OnLockScreenStatus arg0) {}

    @Override
    public void onOnSystemRequest(OnSystemRequest arg0) {}

    @Override
    public void onOnTouchEvent(OnTouchEvent arg0) {}

    @Override
    public void onProxyClosed(String arg0, Exception arg1, SyncDisconnectedReason arg2) {}

    @Override
    public void onSystemRequestResponse(SystemRequestResponse arg0) {}

}