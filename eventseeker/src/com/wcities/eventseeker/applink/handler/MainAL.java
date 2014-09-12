package com.wcities.eventseeker.applink.handler;

import java.util.Collections;
import java.util.Vector;

import android.util.Log;

import com.ford.syncV4.proxy.rpc.OnCommand;
import com.ford.syncV4.proxy.rpc.SoftButton;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.applink.service.AppLinkService;
import com.wcities.eventseeker.applink.util.ALUtil;
import com.wcities.eventseeker.applink.util.CommandsUtil;
import com.wcities.eventseeker.applink.util.CommandsUtil.Command;

public class MainAL extends ESIProxyALM {
	
	private static final String TAG = MainAL.class.getSimpleName();
	
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
	public void onOnCommand(OnCommand notification) {
		/************************************************
		 * NOTE:notification.getCmdID() is not working. *
		 * So, we have used the alternative for the same*
		 ************************************************/
		int cmdId = Integer.parseInt(notification.getParameters("cmdID").toString());
		//Log.d(TAG, "onOnCommand, cmdId = " + cmdId);
		Command cmd = Command.getCommandById(/*notification.getCmdID()*/cmdId);
		performOperationForCommand(cmd);
	}
	
	@Override
	public void onStartInstance() {
		addCommands();
		Vector<SoftButton> softBtns = buildSoftButtons();
		ALUtil.displayMessage(R.string.msg_welcome_to, R.string.msg_eventseeker, softBtns);
	}
	
	private void addCommands() {
		Vector<Command> reqCmds = new Vector<Command>();
		reqCmds.add(Command.DISCOVER);
		reqCmds.add(Command.MY_EVENTS);
		//reqCmds.add(Command.SEARCH);
		CommandsUtil.addCommands(reqCmds);
	}
	
	private Vector<SoftButton> buildSoftButtons() {
		Vector<SoftButton> softBtns = new Vector<SoftButton>();
		softBtns.add(Command.DISCOVER.buildSoftBtn());
		softBtns.add(Command.MY_EVENTS.buildSoftBtn());
		//softBtns.add(Command.SEARCH.buildSoftBtn());
		return softBtns;
	}
	
	public void performOperationForCommand(Command cmd) {
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
