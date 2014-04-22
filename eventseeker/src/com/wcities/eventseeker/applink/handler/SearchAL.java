package com.wcities.eventseeker.applink.handler;

import java.util.Arrays;
import java.util.Vector;

import android.util.Log;

import com.ford.syncV4.proxy.TTSChunkFactory;
import com.ford.syncV4.proxy.rpc.Choice;
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
import com.wcities.eventseeker.applink.util.CommandsUtil.Command;
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
		Vector<Command> requiredCmds = new Vector<Command>();
		requiredCmds.add(Command.CALL_VENUE);
		//requiredCmds.add(Commands.PLAY);
		requiredCmds.add(Command.DETAILS);
		requiredCmds.add(Command.BACK);
		requiredCmds.add(Command.NEXT);
		requiredCmds.add(Command.SEARCH);
		requiredCmds.add(Command.MY_EVENTS);
		requiredCmds.add(Command.DISCOVER);
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
		//EventALUtil.onNextCommand(eventList, context);
	}

	@Override
	public void onOnCommand(OnCommand notification) {
		/************************************************
		 * NOTE:notification.getCmdID() is not working. *
		 * So, we have used the alternative for the same*
		 ************************************************/
		int cmdId = Integer.parseInt(notification.getParameters("cmdID").toString());
		//Log.d(TAG, "onOnCommand, cmdId = " + cmdId);
		Command cmd = Command.getCommandById(cmdId);
		performOperationForCommand(cmd);
	}
	
	public void performOperationForCommand(Command cmd) {
		if (cmd == null) {
			return;
		}
		Log.d(TAG, "performOperationForCommand : " + cmd.name());
		reset(cmd);
		
		switch (cmd) {
			case DISCOVER:
			case MY_EVENTS:
			case SEARCH:
				AppLinkService.getInstance().initiateESIProxyListener(cmd);
				break;
			case NEXT:
				EventALUtil.onNextCommand(eventList, context);
				break;
			case BACK:
				EventALUtil.onBackCommand(eventList, context);
				break;
			case DETAILS:
				EventALUtil.speakDetailsOfEvent(eventList.getCurrentEvent(), context);
				break;
			/*case PLAY:
				break;*/
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
	private void reset(Command cmd) {
		eventList.resetEventList();
		selectedCategoryId = 0;
	}

}
