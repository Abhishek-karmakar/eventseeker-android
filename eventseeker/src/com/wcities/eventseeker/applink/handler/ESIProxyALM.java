package com.wcities.eventseeker.applink.handler;

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
import com.ford.syncV4.proxy.rpc.UnsubscribeButtonResponse;
import com.ford.syncV4.proxy.rpc.UnsubscribeVehicleDataResponse;
import com.wcities.eventseeker.applink.util.CommandsUtil.Command;

public abstract class ESIProxyALM implements IProxyListenerALM {
	
	public abstract void onStartInstance();
	public abstract void performOperationForCommand(Command cmd);
	
	@Override
	public void onOnButtonPress(OnButtonPress arg0) {}

	@Override
	public void onOnCommand(OnCommand arg0) {}

	@Override
	public void onPerformInteractionResponse(PerformInteractionResponse arg0) {}
	
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
	public void onError(String arg0, Exception arg1) {}

	@Override
	public void onGenericResponse(GenericResponse arg0) {}

	@Override
	public void onOnButtonEvent(OnButtonEvent arg0) {}

	@Override
	public void onOnHMIStatus(OnHMIStatus arg0) {}
	
	@Override
	public void onOnPermissionsChange(OnPermissionsChange arg0) {}
	
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
	public void onChangeRegistrationResponse(ChangeRegistrationResponse arg0) {}

	@Override
	public void onDeleteFileResponse(DeleteFileResponse arg0) {}

	@Override
	public void onDialNumberResponse(DialNumberResponse arg0) {}

	@Override
	public void onEndAudioPassThruResponse(EndAudioPassThruResponse arg0) {}

	@Override
	public void onGetDTCsResponse(GetDTCsResponse arg0) {}

	@Override
	public void onGetVehicleDataResponse(GetVehicleDataResponse arg0) {}

	@Override
	public void onListFilesResponse(ListFilesResponse arg0) {}

	@Override
	public void onOnAudioPassThru(OnAudioPassThru arg0) {}

	@Override
	public void onOnLanguageChange(OnLanguageChange arg0) {}

	@Override
	public void onOnVehicleData(OnVehicleData arg0) {}

	@Override
	public void onPerformAudioPassThruResponse(PerformAudioPassThruResponse arg0) {}

	@Override
	public void onPutFileResponse(PutFileResponse arg0) {}

	@Override
	public void onReadDIDResponse(ReadDIDResponse arg0) {}

	@Override
	public void onScrollableMessageResponse(ScrollableMessageResponse arg0) {}

	@Override
	public void onSetAppIconResponse(SetAppIconResponse arg0) {}

	@Override
	public void onSetDisplayLayoutResponse(SetDisplayLayoutResponse arg0) {}

	@Override
	public void onSliderResponse(SliderResponse arg0) {}

	@Override
	public void onSubscribeVehicleDataResponse(SubscribeVehicleDataResponse arg0) {}

	@Override
	public void onUnsubscribeVehicleDataResponse(UnsubscribeVehicleDataResponse arg0) {}
}
