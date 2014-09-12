package com.wcities.eventseeker.applink.util;

import java.util.List;
import java.util.Vector;

import android.util.Log;

import com.ford.syncV4.exception.SyncException;
import com.ford.syncV4.proxy.TTSChunkFactory;
import com.ford.syncV4.proxy.rpc.AddCommand;
import com.ford.syncV4.proxy.rpc.Alert;
import com.ford.syncV4.proxy.rpc.Choice;
import com.ford.syncV4.proxy.rpc.CreateInteractionChoiceSet;
import com.ford.syncV4.proxy.rpc.DeleteCommand;
import com.ford.syncV4.proxy.rpc.DeleteInteractionChoiceSet;
import com.ford.syncV4.proxy.rpc.MenuParams;
import com.ford.syncV4.proxy.rpc.PerformInteraction;
import com.ford.syncV4.proxy.rpc.SetGlobalProperties;
import com.ford.syncV4.proxy.rpc.SoftButton;
import com.ford.syncV4.proxy.rpc.Speak;
import com.ford.syncV4.proxy.rpc.SubscribeVehicleData;
import com.ford.syncV4.proxy.rpc.TTSChunk;
import com.ford.syncV4.proxy.rpc.enums.InteractionMode;
import com.ford.syncV4.proxy.rpc.enums.TextAlignment;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.applink.service.AppLinkService;
import com.wcities.eventseeker.applink.util.CommandsUtil.Command;
import com.wcities.eventseeker.constants.AppConstants;

public class ALUtil {

	private static final String TAG = ALUtil.class.getSimpleName();
	private static final int ALERT_DURATION = 3000;
	
	public static void addCommand(Vector<String> vrCommands, int cmdID) {
		//Log.d(TAG, "Add Command Id is : " + cmdID);
		AppLinkService appLinkService = AppLinkService.getInstance();
		
		AddCommand msg = new AddCommand();
		msg.setCorrelationID(appLinkService.autoIncCorrId++);
		msg.setVrCommands(vrCommands);
		msg.setCmdID(cmdID);
		
		MenuParams menuParams = new MenuParams();
		menuParams.setMenuName(vrCommands.get(0));
		menuParams.setPosition(0);
		msg.setMenuParams(menuParams);
		
		try {
			appLinkService.getProxy().sendRPCRequest(msg);
			
		} catch (SyncException e) {
			e.printStackTrace();
		}
	}

	public static void addCommand(Vector<String> vrCommands, int cmdID, int position) {
		//Log.d(TAG, "Add Command Id is : " + cmdID);
		AppLinkService appLinkService = AppLinkService.getInstance();
		
		AddCommand msg = new AddCommand();
		msg.setCorrelationID(appLinkService.autoIncCorrId++);
		msg.setVrCommands(vrCommands);
		msg.setCmdID(cmdID);
		
		MenuParams menuParams = new MenuParams();
		menuParams.setMenuName(vrCommands.get(0));
		menuParams.setPosition(position);
		msg.setMenuParams(menuParams);
		
		try {
			appLinkService.getProxy().sendRPCRequest(msg);
			
		} catch (SyncException e) {
			e.printStackTrace();
		}
	}
	
	public static void deleteCommand(int cmdID) {
		//Log.d(TAG, "Delete Command Id is : " + cmdID);
		AppLinkService appLinkService = AppLinkService.getInstance();
		
		DeleteCommand msg = new DeleteCommand();
		msg.setCorrelationID(appLinkService.autoIncCorrId++);
		msg.setCmdID(cmdID);
		
		try {
			appLinkService.getProxy().sendRPCRequest(msg);
			
		} catch (SyncException e) {
			e.printStackTrace();
		}
	}

	public static Choice createChoice(int choiceID, String menuName, Vector<String> vrCommands) {
		Choice choice = new Choice();
		choice.setChoiceID(choiceID);
		if (menuName != null) {
			choice.setMenuName(menuName);
		}
		if (vrCommands != null) {
			choice.setVrCommands(vrCommands);
		}
		return choice;
	}

	public static void createInteractionChoiceSet(Vector<Choice> choices, int choiceSetId) {
		Log.d(TAG, "ChoiceSetid : " + choiceSetId);
		if (!choices.isEmpty()) {
			CreateInteractionChoiceSet msg = new CreateInteractionChoiceSet();
			msg.setCorrelationID(AppLinkService.getInstance().autoIncCorrId++);
			msg.setInteractionChoiceSetID(choiceSetId);
			msg.setChoiceSet(choices);
			
			try {
				AppLinkService.getInstance().getProxy().sendRPCRequest(msg);
				
			} catch (SyncException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void deleteInteractionChoiceSet(int choiceSetId) {
		Log.d(TAG, "ChoiceSetid : " + choiceSetId);
		DeleteInteractionChoiceSet msg = new DeleteInteractionChoiceSet();
		msg.setCorrelationID(AppLinkService.getInstance().autoIncCorrId++);
		msg.setInteractionChoiceSetID(choiceSetId);
		
		try {
			AppLinkService.getInstance().getProxy().sendRPCRequest(msg);
			
		} catch (SyncException e) {
			e.printStackTrace();
		}
	}

	public static void performInteractionChoiceSet(Vector<TTSChunk> initialPrompt, String initialText,
			Vector<Integer> interactionChoiceSetIDList, Vector<TTSChunk> timeoutPrompt) {
		PerformInteraction msg = new PerformInteraction();
		msg.setCorrelationID(AppLinkService.getInstance().autoIncCorrId++);		
		msg.setInitialPrompt(initialPrompt);
		msg.setInitialText(initialText);
		msg.setInteractionChoiceSetIDList(interactionChoiceSetIDList);
		msg.setInteractionMode(InteractionMode.BOTH);
		msg.setTimeout(AppConstants.INTERACTION_TIME_OUT_AL);
		msg.setTimeoutPrompt(timeoutPrompt);
		msg.setHelpPrompt(timeoutPrompt);
		
		try {
			AppLinkService.getInstance().getProxy().sendRPCRequest(msg);
			//Log.d(TAG, "PerformInteraction sendRPCRequest() done");
			
		} catch (SyncException e) {
			e.printStackTrace();
		}
	}

	public static void speak(int strResId) {
		String simple = AppLinkService.getInstance().getResources().getString(strResId);
		Vector<TTSChunk> ttsChunks = TTSChunkFactory.createSimpleTTSChunks(simple);
		ALUtil.speakText(ttsChunks);		
	}
	
	public static void speak(String str) {
		Vector<TTSChunk> ttsChunks = TTSChunkFactory.createSimpleTTSChunks(str);			
		ALUtil.speakText(ttsChunks);
	}

	public static void speakText(Vector<TTSChunk> ttsChunks) {
		Speak msg = new Speak();
		msg.setCorrelationID(AppLinkService.getInstance().autoIncCorrId++);
		msg.setTtsChunks(ttsChunks);
		
		try {
			AppLinkService.getInstance().getProxy().sendRPCRequest(msg);
			
		} catch (SyncException e) {
			e.printStackTrace();
		}
	}
	
	public static void displayMessage(int resIdText1, int resIdText2) {
		String text1 = (resIdText1 == AppConstants.INVALID_RES_ID) ? "" : AppLinkService.getInstance().getResources().getString(resIdText1);
		String text2 = (resIdText2 == AppConstants.INVALID_RES_ID) ? "" : AppLinkService.getInstance().getResources().getString(resIdText2);
		displayMessage(text1, text2, null);
	}
	
	public static void displayMessage(int resIdText1, int resIdText2, Vector<SoftButton> softButtons) {
		String text1 = (resIdText1 == AppConstants.INVALID_RES_ID) ? "" : AppLinkService.getInstance().getResources().getString(resIdText1);
		String text2 = (resIdText2 == AppConstants.INVALID_RES_ID) ? "" : AppLinkService.getInstance().getResources().getString(resIdText2);
		displayMessage(text1, text2, softButtons);
	}
	
	public static void displayMessage(String text1, String text2) {
		displayMessage(text1, text2, null);
    }
	
	public static void displayMessage(String text1, String text2, Vector<SoftButton> softButtons) {
		//Log.d(TAG, "text1 : " + text1 + " text2 : " + text2);
		try {
			if (text1 == null) {
				text1 = "";
			}
			if (text2 == null) {
				text2 = "";
			}
			AppLinkService.getInstance().getProxy().show(text1, text2, "", "", null, softButtons, null, 
					TextAlignment.LEFT_ALIGNED, AppLinkService.getInstance().autoIncCorrId++);

		} catch (SyncException e) {
			e.printStackTrace();
			Log.d(TAG, "Failed to send Show");
		}
	}
	
	public static void alert(String alertText1, String speakText) {
		try {
			Alert msg = new Alert();
			msg.setCorrelationID(AppLinkService.getInstance().autoIncCorrId++);
			msg.setAlertText1(alertText1);
			msg.setDuration(ALERT_DURATION);
			msg.setPlayTone(true);
			Vector<TTSChunk> ttsChunks = TTSChunkFactory.createSimpleTTSChunks(speakText);
			msg.setTtsChunks(ttsChunks);
			AppLinkService.getInstance().getProxy().sendRPCRequest(msg);
			
		} catch (SyncException e) {
			e.printStackTrace();
			Log.d(TAG, "Failed to show alert");
		}
	}
	
	public static void alertText(String alertText1, String alertText2) {
		try {
			Alert msg = new Alert();
			msg.setCorrelationID(AppLinkService.getInstance().autoIncCorrId++);
			msg.setAlertText1(alertText1);
			msg.setAlertText2(alertText2);
			msg.setDuration(ALERT_DURATION);
			msg.setPlayTone(true);
			AppLinkService.getInstance().getProxy().sendRPCRequest(msg);
			
		} catch (SyncException e) {
			e.printStackTrace();
			Log.d(TAG, "Failed to show alert");
		}
	}
	
	public static void alert(String alertText1, String alertText2, String alertText3, String speakText) {
		try {
			Alert msg = new Alert();
			msg.setCorrelationID(AppLinkService.getInstance().autoIncCorrId++);
			msg.setAlertText1(alertText1);
			msg.setAlertText2(alertText2);
			msg.setAlertText3(alertText3);
			msg.setDuration(ALERT_DURATION);
			msg.setPlayTone(true);
			Vector<TTSChunk> ttsChunks = TTSChunkFactory.createSimpleTTSChunks(speakText);
			msg.setTtsChunks(ttsChunks);
			AppLinkService.getInstance().getProxy().sendRPCRequest(msg);
			
		} catch (SyncException e) {
			e.printStackTrace();
			Log.d(TAG, "Failed to show alert");
		}
	}
	
	public static void subscribeForGps() {
		SubscribeVehicleData msg = new SubscribeVehicleData();
		msg.setGps(true);
		msg.setCorrelationID(AppLinkService.getInstance().autoIncCorrId++);
		
		try {
			AppLinkService.getInstance().getProxy().sendRPCRequest(msg);
			
		} catch (SyncException e) {
			e.printStackTrace();
		}
	}
	
	/*public static void unsubscribeForGps() {
		UnsubscribeVehicleData msg = new UnsubscribeVehicleData();
		msg.setGps(true);
		msg.setCorrelationID(AppLinkService.getInstance().autoIncCorrId++);
		
		try {
			AppLinkService.getInstance().getProxy().sendRPCRequest(msg);
			
		} catch (SyncException e) {
			e.printStackTrace();
		}
	}*/
	
	public static void setGlobalProperties(List<Command> commands) {
		String helpPrompt = "";
		for (int i = 0; i < commands.size() - 1; i++) {
			Command command = (Command) commands.get(i);
			helpPrompt += command.toString() + ", ";
		}
		helpPrompt += "and, " + commands.get(commands.size() - 1).toString();
		Log.d(TAG, "help = " + helpPrompt);
		
		SetGlobalProperties msg = new SetGlobalProperties();
		msg.setCorrelationID(AppLinkService.getInstance().autoIncCorrId++);
		msg.setHelpPrompt(TTSChunkFactory.createSimpleTTSChunks(helpPrompt));
		msg.setTimeoutPrompt(TTSChunkFactory.createSimpleTTSChunks(AppLinkService.getInstance()
				.getResources().getString(R.string.time_out)));
		
		try {
			AppLinkService.getInstance().getProxy().sendRPCRequest(msg);
			
		} catch (SyncException e) {
			e.printStackTrace();
		}
	}
}
