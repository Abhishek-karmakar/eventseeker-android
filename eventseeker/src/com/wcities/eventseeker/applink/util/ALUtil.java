package com.wcities.eventseeker.applink.util;

import java.util.Vector;

import android.util.Log;

import com.ford.syncV4.exception.SyncException;
import com.ford.syncV4.proxy.rpc.AddCommand;
import com.ford.syncV4.proxy.rpc.Choice;
import com.ford.syncV4.proxy.rpc.CreateInteractionChoiceSet;
import com.ford.syncV4.proxy.rpc.DeleteCommand;
import com.ford.syncV4.proxy.rpc.DeleteInteractionChoiceSet;
import com.ford.syncV4.proxy.rpc.PerformInteraction;
import com.ford.syncV4.proxy.rpc.Speak;
import com.ford.syncV4.proxy.rpc.TTSChunk;
import com.ford.syncV4.proxy.rpc.enums.InteractionMode;
import com.ford.syncV4.proxy.rpc.enums.TextAlignment;
import com.wcities.eventseeker.applink.service.AppLinkService;
import com.wcities.eventseeker.constants.AppConstants;

public class ALUtil {

	private static final String TAG = ALUtil.class.getName();
	
	public static void addCommand(Vector<String> vrCommands, int cmdID) {
		Log.d(TAG, "Command Id is : " + cmdID);
		AppLinkService appLinkService = AppLinkService.getInstance();
		
		AddCommand msg = new AddCommand();
		msg.setCorrelationID(appLinkService.autoIncCorrId++);
		msg.setVrCommands(vrCommands);
		msg.setCmdID(cmdID);
		try {
			appLinkService.getProxy().sendRPCRequest(msg);
			
		} catch (SyncException e) {
			e.printStackTrace();
		}
	}

	public static void deleteCommand(int cmdID) {
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
		msg.setInteractionMode(InteractionMode.VR_ONLY);
		msg.setTimeout(AppConstants.INTERACTION_TIME_OUT_AL);
		msg.setTimeoutPrompt(timeoutPrompt);
		
		try {
			AppLinkService.getInstance().getProxy().sendRPCRequest(msg);
			Log.d(TAG, "PerformInteraction sendRPCRequest() done");
			
		} catch (SyncException e) {
			e.printStackTrace();
		}
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
		displayMessage(AppLinkService.getInstance().getResources().getString(resIdText1), 
				AppLinkService.getInstance().getResources().getString(resIdText2));
	}
	
	public static void displayMessage(String text1, String text2) {
		Log.d(TAG,"text1 : " + text1 + " text2 : " + text2);
		try {
			if (text1 == null) {
				text1 = "";
			}
			if (text2 == null) {
				text2 = "";
			}
			AppLinkService.getInstance().getProxy()
				.show(text1, text2, TextAlignment.LEFT_ALIGNED, AppLinkService.getInstance().autoIncCorrId++);

		} catch (SyncException e) {
			e.printStackTrace();
			Log.d(TAG,"Failed to send Show");
		}
   }
	
}
