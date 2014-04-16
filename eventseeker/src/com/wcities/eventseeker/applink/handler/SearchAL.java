package com.wcities.eventseeker.applink.handler;

import java.util.Arrays;
import java.util.Vector;

import android.util.Log;

import com.ford.syncV4.proxy.TTSChunkFactory;
import com.ford.syncV4.proxy.rpc.ChangeRegistrationResponse;
import com.ford.syncV4.proxy.rpc.Choice;
import com.ford.syncV4.proxy.rpc.DeleteFileResponse;
import com.ford.syncV4.proxy.rpc.OnButtonPress;
import com.ford.syncV4.proxy.rpc.OnCommand;
import com.ford.syncV4.proxy.rpc.PerformInteractionResponse;
import com.ford.syncV4.proxy.rpc.TTSChunk;
import com.ford.syncV4.proxy.rpc.enums.ButtonName;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.applink.datastructure.EventList;
import com.wcities.eventseeker.applink.service.AppLinkService;
import com.wcities.eventseeker.applink.util.ALUtil;
import com.wcities.eventseeker.applink.util.CommandsUtil;
import com.wcities.eventseeker.applink.util.CommandsUtil.Commands;
import com.wcities.eventseeker.applink.util.EventALUtil;

public class SearchAL extends ESIProxyALM {

	private static final String TAG = SearchAL.class.getName();
	private static final int CHOICE_SET_ID_SEARCH = 3;
	private static final int EVENTS_LIMIT = 10;
	
	public static enum SearchCategories {
		SEARCH_EVENTS(R.string.search_event),
		SEARCH_ARTISTS(R.string.search_artist);

		private int nameResId;
		private SearchCategories(int nameResId) {
			this.nameResId = nameResId;
		}
		
		public String getNameResId() {
			return AppLinkService.getInstance().getString(nameResId);
		}
		
		public static SearchCategories getSearchChoiceId(int choiceId) {
			SearchCategories[] searchCtgrs = SearchCategories.values();
			for (SearchCategories searchCtgry : searchCtgrs) {
				if (searchCtgry.ordinal() == choiceId) {
					return searchCtgry;
				}
			}
			return null;
		}
	}
	private static SearchAL instance;
	
	private EventSeekr context;
	private EventList eventList;
	private int selectedCategoryId;
	
	public SearchAL(EventSeekr context) {
		this.context = context;
		eventList = new EventList();
		eventList.setEventsLimit(EVENTS_LIMIT);
		/**
		 * No need to add the LoadListener as for Ford there will be at the max 10 Searched events.
		 * eventList.setLoadEventsListener(this);
		 */
	}

	public static ESIProxyALM getInstance(EventSeekr context) {
		if (instance == null) {
			instance = new SearchAL(context);
		}
		return instance;
	}
	
	@Override
	public void onStartInstance() {
		//search_events_or_artists
		Log.d(TAG, "onStartInstance()");
		initializeInteractionChoiceSets();
		performInteraction();		
		addCommands();
	}
	
	private void initializeInteractionChoiceSets() {
		Log.d(TAG, "initializeInteractionChoiceSets()");

		Vector<Choice> choices = new Vector<Choice>();
		
		SearchCategories[] categories = SearchCategories.values();
		for (int i = 0; i < categories.length; i++) {
			SearchCategories category = categories[i];
			
			Log.d(TAG, "Category id : " + category.ordinal());
			Log.d(TAG, "Category nameResId : " + category.getNameResId());
			
			Choice choice = ALUtil.createChoice(category.ordinal(), category.getNameResId(), 
					new Vector<String>(Arrays.asList(new String[] {category.getNameResId()})));
			choices.add(choice);
		}

		ALUtil.createInteractionChoiceSet(choices, CHOICE_SET_ID_SEARCH);
	}
	
	private void performInteraction() {
		Log.d(TAG, "performInteraction()");
		Log.d(TAG, "Searchordinal : " + CHOICE_SET_ID_SEARCH);
		
		Vector<Integer> interactionChoiceSetIDList = new Vector<Integer>();
		interactionChoiceSetIDList.add(CHOICE_SET_ID_SEARCH);
		
		String simple = AppLinkService.getInstance().getString(R.string.search_events_or_artists);
		String initialText = context.getResources().getString(R.string.search_for);		
		
		Vector<TTSChunk> initChunks = TTSChunkFactory.createSimpleTTSChunks(simple);
		Vector<TTSChunk> timeoutChunks = TTSChunkFactory.createSimpleTTSChunks(
				context.getResources().getString(R.string.time_out));
		
		ALUtil.performInteractionChoiceSet(initChunks, initialText, interactionChoiceSetIDList, timeoutChunks);
	}
	
	private void addCommands() {
		Vector<Commands> requiredCmds = new Vector<Commands>();
		requiredCmds.add(Commands.DISCOVER);
		requiredCmds.add(Commands.MY_EVENTS);
		requiredCmds.add(Commands.SEARCH);
		requiredCmds.add(Commands.NEXT);
		requiredCmds.add(Commands.BACK);
		requiredCmds.add(Commands.DETAILS);
		requiredCmds.add(Commands.PLAY);
		requiredCmds.add(Commands.CALL_VENUE);
		CommandsUtil.addCommands(requiredCmds);
	}
	
	private void loadSearchEvents(int selectedCategoryId) {
		
	}
	
	@Override
	public void onPerformInteractionResponse(PerformInteractionResponse response) {
		Log.i(TAG, "onPerformInteractionResponse(), response.getChoiceID() = " + response.getChoiceID());
		
		if (SearchCategories.getSearchChoiceId(response.getChoiceID()) == null) {
			//TODO: when Choice Id is invalid that is null	
			Log.i(TAG, "SearchCategories.getSearchChoiceId(response.getChoiceID()) == null");
		}
		
		selectedCategoryId = SearchCategories.getSearchChoiceId(response.getChoiceID()).ordinal();

		loadSearchEvents(selectedCategoryId);

		//show Welcome message when no events are available
		if (eventList.isEmpty()) {
			ALUtil.displayMessage(R.string.msg_welcome_to, R.string.msg_eventseeker);
		}
		//onNextCommand();
	}

	@Override
	public void onOnButtonPress(OnButtonPress notification) {
		Log.d(TAG, "onOnButtonPress");
		ButtonName btnName = notification.getButtonName();
		Commands cmd = Commands.getCommandByButtonName(btnName);
		resetIfNeeded(cmd);
		performOperationForCommand(cmd);
	}
	
	@Override
	public void onOnCommand(OnCommand notification) {
		/************************************************
		 * NOTE:notification.getCmdID() is not working. *
		 * So, we have used the alternative for the same*
		 ************************************************/
		int cmdId = Integer.parseInt(notification.getParameters("cmdID").toString());
		//Log.d(TAG, "onOnCommand, cmdId = " + cmdId);
		Commands cmd = Commands.getCommandById(cmdId);
		resetIfNeeded(cmd);
		performOperationForCommand(cmd);
	}

	@Override
	public void onChangeRegistrationResponse(ChangeRegistrationResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDeleteFileResponse(DeleteFileResponse arg0) {
		// TODO Auto-generated method stub
	}
	
	@SuppressWarnings("unused")
	public void performOperationForCommand(Commands cmd) {
		Log.d(TAG, "performOperationForCommand : " + cmd.name());
		if (cmd == null) {
			return;
		}

		switch (cmd) {
			case DISCOVER:
			case MY_EVENTS:
			case SEARCH:
				AppLinkService.getInstance().initiateESIProxyListener(cmd);
				break;
			case NEXT:
				onNextCommand();
				break;
			case BACK:
				onBackCommand();
				break;
			case DETAILS:
				EventALUtil.speakDetailsOfEvent(eventList.getCurrentEvent(), context);
				break;
			case PLAY:
				break;
			case CALL_VENUE:
				break;
			case FOLLOW:
				break;
			default:
				Log.d(TAG, cmd + " is an Invalid Command");
				break;
			
		}
	}
	
	/**
	 * reset the fields to default if Search screen is being launched from the
	 * Discover screen only.
	 * @param cmd
	 */
	private void resetIfNeeded(Commands cmd) {
		if (cmd != Commands.SEARCH) {
			return;
		}
		eventList.resetEventList();
		selectedCategoryId = 0;
	}

	private void onNextCommand() {
		if (eventList.moveToNextEvent()) {
			EventALUtil.displayCurrentEvent(eventList);
			EventALUtil.speakEventTitle(eventList.getCurrentEvent(), context);
			
		} else {
			EventALUtil.speakNoEventsAvailable();
		}		
	}

	private void onBackCommand() {
		if (eventList.moveToPreviousEvent()) {
			EventALUtil.displayCurrentEvent(eventList);
			EventALUtil.speakEventTitle(eventList.getCurrentEvent(), context);
			
		} else {
			EventALUtil.speakNoEventsAvailable();
		}		
	}

}
