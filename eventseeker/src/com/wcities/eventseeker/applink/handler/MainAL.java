package com.wcities.eventseeker.applink.handler;

import java.util.Vector;

import android.util.Log;

import com.ford.syncV4.proxy.rpc.OnButtonPress;
import com.ford.syncV4.proxy.rpc.OnCommand;
import com.ford.syncV4.proxy.rpc.enums.ButtonName;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.applink.interfaces.ESIProxyALM;
import com.wcities.eventseeker.applink.service.AppLinkService;
import com.wcities.eventseeker.applink.util.ALUtil;
import com.wcities.eventseeker.applink.util.CommandsUtil;
import com.wcities.eventseeker.applink.util.CommandsUtil.Commands;

public class MainAL extends ESIProxyALM {
	
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
		performOperationForCommand(cmd );
	}


	@Override
	public void onOnCommand(OnCommand notification) {
		/************************************************
		 * NOTE:notification.getCmdID() is not working. *
		 * So, we have used the alternative for the same*
		 ************************************************/
		int cmdId = Integer.parseInt(notification.getParameters("cmdID").toString());
		//Log.d(TAG, "onOnCommand, cmdId = " + cmdId);
		Commands cmd = Commands.getCommandById(/*notification.getCmdID()*/cmdId);
		performOperationForCommand(cmd);
	}
	
	@Override
	public void onStartInstance() {
		ALUtil.displayMessage(R.string.main_al_welcome_to, R.string.main_al_eventseeker);
		addCommands();
	}
	
	@Override
	public void onStopInstance() {
		Vector<Commands> delCmds = new Vector<Commands>();
		delCmds.add(Commands.DISCOVER);
		delCmds.add(Commands.MY_EVENTS);
		delCmds.add(Commands.SEARCH);
		CommandsUtil.deleteCommands(delCmds);
	}
	
	private void addCommands() {
		Vector<Commands> reqCmds = new Vector<Commands>();
		reqCmds.add(Commands.DISCOVER);
		reqCmds.add(Commands.MY_EVENTS);
		reqCmds.add(Commands.SEARCH);
		CommandsUtil.addCommands(reqCmds);
	}
	
	public static void performOperationForCommand(Commands cmd) {
		if (cmd == null) {
			return;
		}

		switch (cmd) {
			case DISCOVER:
			case MY_EVENTS:
			case SEARCH:
				AppLinkService.getInstance().initiateESIProxyListener(cmd);
				break;
			default:
				Log.d(TAG, "Command : " + cmd + " is an invalid command.");
				break;
		}
	}

}
