package com.wcities.eventseeker.applink.util;

import java.util.Arrays;
import java.util.Vector;

import com.ford.syncV4.proxy.rpc.SoftButton;
import com.ford.syncV4.proxy.rpc.enums.ButtonName;
import com.ford.syncV4.proxy.rpc.enums.SoftButtonType;
import com.ford.syncV4.proxy.rpc.enums.SystemAction;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.applink.service.AppLinkService;

public class CommandsUtil {

	private static final String TAG = CommandsUtil.class.getSimpleName();
	
	public static enum Command {
		DISCOVER(AppLinkService.CMD_ID_AL),
		MY_EVENTS(AppLinkService.CMD_ID_AL + 1),
		/**
		 * The Search screen has been removed from the Ford app.
		 */
		SEARCH(AppLinkService.CMD_ID_AL + 2),
		NEXT(AppLinkService.CMD_ID_AL + 3),
		BACK(AppLinkService.CMD_ID_AL + 4),
		DETAILS(AppLinkService.CMD_ID_AL + 5),
		//PLAY(AppLinkService.CMD_ID_AL + 6),
		CALL_VENUE(AppLinkService.CMD_ID_AL + 6),
		FOLLOW(AppLinkService.CMD_ID_AL + 7);

		private int cmdId;
		private boolean isAdded;
		
		private Command(int cId) {
			this.cmdId = cId;
		}
		
		public static Command getCommandById(int cmdId) {
			Command[] cmds = Command.values();
			for (Command cmd : cmds) {
				if (cmd.getCmdId() == cmdId) {
					return cmd;
				}
			}
			return null;
		}
		
		public static Command getCommandByButtonName(ButtonName btnName) {
			switch (btnName) {
			case SEEKRIGHT : 
				return NEXT;
			case SEEKLEFT : 
				return BACK;
			case OK : 
				return DETAILS;
			default :
				return null;
			}
		}
		
		public int getCmdId() {
			return cmdId;
		}
		
		public boolean isAdded() {
			return isAdded;
		}

		public void setAdded(boolean isAdded) {
			this.isAdded = isAdded;
		}

		@Override
		public String toString() {
			String str = null;
			switch (this) {
			case DISCOVER:
				str = AppLinkService.getInstance().getResources().getString(R.string.al_command_discover);
				break;
			case MY_EVENTS:
				str = AppLinkService.getInstance().getResources().getString(R.string.al_command_my_events);
				break;
			case SEARCH:
				str = AppLinkService.getInstance().getResources().getString(R.string.al_command_search);
				break;
			case NEXT:
				str = AppLinkService.getInstance().getResources().getString(R.string.al_command_next);
				break;
			case BACK:
				str = AppLinkService.getInstance().getResources().getString(R.string.al_command_back);
				break;
			case DETAILS:
				str = AppLinkService.getInstance().getResources().getString(R.string.al_command_details);
				break;
			/*case PLAY:
				str = AppLinkService.getInstance().getResources().getString(R.string.al_command_play);
				break;*/
			case CALL_VENUE:
				str = AppLinkService.getInstance().getResources().getString(R.string.al_command_call_venue);
				break;
			case FOLLOW:
				str = AppLinkService.getInstance().getResources().getString(R.string.al_command_follow);
				break;
			default:
				break;
			}
			return str;
		}
		
		private String getSoftBtnText() {
			switch (this) {
			
			case DISCOVER:
				return AppLinkService.getInstance().getResources().getString(R.string.soft_btn_discover);

			default:
				return toString();
			}
		}
		
		public SoftButton buildSoftBtn() {
			SoftButton softBtn = new SoftButton();
			softBtn.setSoftButtonID(cmdId);
			softBtn.setText(getSoftBtnText());
			softBtn.setIsHighlighted(false);
			softBtn.setSystemAction(SystemAction.DEFAULT_ACTION);
			softBtn.setType(SoftButtonType.SBT_TEXT);
			return softBtn;
		}

		public static void reset() {
			for (Command cmd : Command.values()) {
				cmd.isAdded = false;
			}
		}
	}

	public static void addCommands(Vector<Command> reqCommands) {
		/**
		 * To run it on simulator we have to comment this for loop, 
		 * otherwise simulator crashes on pressing any command.
		 */
		//if (!AppConstants.DEBUG) {
		for (int i = 0; i < Command.values().length; i++) {
			Command command = Command.values()[i];
			if (command.isAdded() && !reqCommands.contains(command)) {
				//Log.d(TAG, "Delete command : " + command.toString());
				ALUtil.deleteCommand(command.getCmdId());
				command.setAdded(false);
			}
		}
		//}
		
		for (int index = 0; index < reqCommands.size(); index++) {
			Command cmd = reqCommands.get(index);
			if (cmd.isAdded()) {
				continue;
			}
			//Log.d(TAG, "Add command : " + cmd.toString());
			ALUtil.addCommand(new Vector<String>(Arrays.asList(new String[] {cmd.toString()})), cmd.getCmdId(), index);
			cmd.setAdded(true);
		}
		
		/**
		 * 10-07-2014: commented below functionality as in IOS-Eventseeker_Ford app. The system on its own
		 * speaks the available help commands.
		 */
		//ALUtil.setGlobalProperties(helpCommands);
	}
	
	/*public static void performOperationForCommand(Commands cmd) {
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
	}*/
	
}
