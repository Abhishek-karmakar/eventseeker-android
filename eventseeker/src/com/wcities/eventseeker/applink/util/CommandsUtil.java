package com.wcities.eventseeker.applink.util;

import java.util.Arrays;
import java.util.Vector;

import android.util.Log;

import com.ford.syncV4.proxy.rpc.enums.ButtonName;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.applink.handler.DiscoverAL;
import com.wcities.eventseeker.applink.handler.MyEventsAL;
import com.wcities.eventseeker.applink.handler.SearchAL;
import com.wcities.eventseeker.applink.interfaces.ESIProxyListener;
import com.wcities.eventseeker.applink.service.AppLinkService;
import com.wcities.eventseeker.applink.util.CommandsUtil.Commands;

public class CommandsUtil {

	private static final String TAG = CommandsUtil.class.getName();
	
	public static enum Commands {
		DISCOVER(AppLinkService.CMD_ID_AL),
		MY_EVENTS(AppLinkService.CMD_ID_AL + 1),
		SEARCH(AppLinkService.CMD_ID_AL + 2),
		NEXT(AppLinkService.CMD_ID_AL + 3),
		BACK(AppLinkService.CMD_ID_AL + 4),
		DETAILS(AppLinkService.CMD_ID_AL + 5),
		PLAY(AppLinkService.CMD_ID_AL + 6),
		CALL_VENUE(AppLinkService.CMD_ID_AL + 7),
		FOLLOW(AppLinkService.CMD_ID_AL + 8);

		private int cmdId;
		
		private Commands(int cId) {
			this.cmdId = cId;
		}
		
		public static Commands getCommandById(int cmdId) {
			Commands[] cmds = Commands.values();
			for (Commands cmd : cmds) {
				if (cmd.getCmdId() == cmdId) {
					return cmd;
				}
			}
			return null;
		}
		
		public static Commands getCommandByButtonName(ButtonName btnName) {
			switch (btnName) {
			case PRESET_0 : 
				return DISCOVER;
			case PRESET_1 : 
				return MY_EVENTS;
			case PRESET_2 : 
				return SEARCH;
			case PRESET_3 : 
				return NEXT;
			case PRESET_4 : 
				return BACK;
			case PRESET_5 : 
				return DETAILS;
			case PRESET_6 : 
				return PLAY;
			case PRESET_7 : 
				return CALL_VENUE;
			case PRESET_8 : 
				return FOLLOW;
			default :
				return null;
			}
		}
		
		public int getCmdId() {
			return cmdId;
		}
		
		@Override
		public String toString() {
			String str = null;
			switch (this) {
			case DISCOVER:
				str = AppLinkService.getStringFromRes(R.string.al_command_discover);
				break;
			case MY_EVENTS:
				str = AppLinkService.getStringFromRes(R.string.al_command_my_events);
				break;
			case SEARCH:
				str = AppLinkService.getStringFromRes(R.string.al_command_search);
				break;
			case NEXT:
				str = AppLinkService.getStringFromRes(R.string.al_command_next);
				break;
			case BACK:
				str = AppLinkService.getStringFromRes(R.string.al_command_back);
				break;
			case DETAILS:
				str = AppLinkService.getStringFromRes(R.string.al_command_details);
				break;
			case PLAY:
				str = AppLinkService.getStringFromRes(R.string.al_command_play);
				break;
			case CALL_VENUE:
				str = AppLinkService.getStringFromRes(R.string.al_command_call_venue);
				break;
			case FOLLOW:
				str = AppLinkService.getStringFromRes(R.string.al_command_follow);
				break;
			default:
				break;
			}
			return str;
		}
		
	}

	public static void addCommands(Vector<Commands> reqCommands) {
		Commands[] allCmds = Commands.values();
		for (Commands cmd : allCmds) {
			if (reqCommands.contains(cmd)) {
				ALUtil.addCommand(new Vector<String>(Arrays.asList(new String[] {cmd.toString()})), cmd.getCmdId());
			}
		}
	}
	
	public static void deleteCommands(Vector<Commands> delCommands) {
		//TODO: add functionality to delete the commands
	}
	
	public static void performOperationForCommand(Commands cmd) {
		if (cmd == null) {
			return;
		}

		switch (cmd) {
			
			case DISCOVER:
			case MY_EVENTS:
			case SEARCH:
				initiateESIProxyListener(cmd);
				break;
			case NEXT:
				break;
			case BACK:
				break;
			case DETAILS:
				break;
			case PLAY:
				break;
			case CALL_VENUE:
				break;
			case FOLLOW:
				break;
			default:
				Log.d(TAG, "Invalis command");
				break;
			
		}
	}
	
	private static void initiateESIProxyListener(Commands cmd) {
		ESIProxyListener esiProxyListener = null;
		AppLinkService proxyService = AppLinkService.getInstance();
		switch (cmd) {
		case DISCOVER:
			Log.d(TAG, "DISCOVER");
			esiProxyListener =  DiscoverAL.getInstance((EventSeekr) proxyService.getApplication());
			break;
		case MY_EVENTS:
			Log.d(TAG, "My Events");
			esiProxyListener =  MyEventsAL.getInstance((EventSeekr) proxyService.getApplication());
			break;
		case SEARCH:
			Log.d(TAG, "SEARCH");
			esiProxyListener =  SearchAL.getInstance((EventSeekr) proxyService.getApplication());
			break;
		}
		
		if (esiProxyListener == null) {
			return;
		}
		
		proxyService.setESIProxyListener(esiProxyListener);
		esiProxyListener.initiateInterAction();
	}
	
}
