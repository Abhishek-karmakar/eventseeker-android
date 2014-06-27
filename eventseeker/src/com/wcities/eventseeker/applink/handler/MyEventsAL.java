package com.wcities.eventseeker.applink.handler;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.ford.syncV4.proxy.TTSChunkFactory;
import com.ford.syncV4.proxy.rpc.Choice;
import com.ford.syncV4.proxy.rpc.PerformInteractionResponse;
import com.ford.syncV4.proxy.rpc.SoftButton;
import com.ford.syncV4.proxy.rpc.TTSChunk;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi;
import com.wcities.eventseeker.api.UserInfoApi.Type;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.applink.core.EventList;
import com.wcities.eventseeker.applink.core.EventList.LoadEventsListener;
import com.wcities.eventseeker.applink.service.AppLinkService;
import com.wcities.eventseeker.applink.util.ALUtil;
import com.wcities.eventseeker.applink.util.CommandsUtil;
import com.wcities.eventseeker.applink.util.CommandsUtil.Command;
import com.wcities.eventseeker.applink.util.EventALUtil;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.ItemsList;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser;

public class MyEventsAL extends ESIProxyALM implements LoadEventsListener {

	private static final String TAG = MyEventsAL.class.getName();
	private static final int START_CHOICE_ID_SUGGESTION_REPLY = 1;
	private static final int CHOICE_SET_ID_SUGGESTION_REPLY = 1;
	
	private static MyEventsAL instance;

	private EventSeekr mEventSeekr;
	private EventList eventList;
	private double lat, lon;
	private Type type;
	
	private static enum SuggestionReply {
		Yes(START_CHOICE_ID_SUGGESTION_REPLY, R.string.my_events_al_yes),
		No(START_CHOICE_ID_SUGGESTION_REPLY + 1, R.string.my_events_al_no);
		
		private int id, nameId;
		
		private SuggestionReply(int value, int nameId) {
			this.id = value;
			this.nameId = nameId;
		}

		public String getName() {
			return AppLinkService.getInstance().getResources().getString(nameId);
		}
		
		public int getId() {
			return id;
		}
	}
	
	public MyEventsAL(EventSeekr mEventSeekr) {
		this.mEventSeekr = mEventSeekr;
		eventList = new EventList();
		eventList.setLoadEventsListener(this);
	}

	public static ESIProxyALM getInstance(EventSeekr context) {
		if (instance == null) {
			instance = new MyEventsAL(context);
		}
		return instance;
	}
	
	@Override
	public void onStartInstance() {
		//Log.d(TAG, "onStartInstance()");
		if (eventList != null) {
			reset();
		}
		addCommands();
		Vector<SoftButton> softBtns = buildSoftButtons();
		ALUtil.displayMessage(R.string.loading, AppConstants.INVALID_RES_ID, softBtns);

		generateLatLon();
		try {
			loadEvents(Type.myevents);
			if (eventList.isEmpty()) {
				onNoMyEventsFound();
				
			} else {
				handleNext();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			AppLinkService.getInstance().handleNoNetConnectivity();
		}
	}
	
	private void reset() {
		eventList.resetEventList();
		eventList.setLoadEventsListener(this);	
	}
	
	private void onNoMyEventsFound() {
		initializeInteractionChoiceSet();
		performInteraction();
	}
	
	private void initializeInteractionChoiceSet() {
		Vector<Choice> choices = new Vector<Choice>();
		SuggestionReply[] categories = SuggestionReply.values();
		for (int i = 0; i < categories.length; i++) {
			SuggestionReply category = categories[i];
			Choice choice = ALUtil.createChoice(category.getId(), category.getName(), 
					new Vector<String>(Arrays.asList(new String[] {category.getName()})));
			choices.add(choice);
		}

		ALUtil.createInteractionChoiceSet(choices, CHOICE_SET_ID_SUGGESTION_REPLY);
	}
	
	private void performInteraction() {
		Vector<Integer> interactionChoiceSetIDList = new Vector<Integer>();
		interactionChoiceSetIDList.add(CHOICE_SET_ID_SUGGESTION_REPLY);
		
		String simple = AppLinkService.getInstance().getString(R.string.my_events_al_no_evts_avail_make_suggestions);
		String initialText = mEventSeekr.getResources().getString(R.string.my_events_al_confirmation);	
		
		Vector<TTSChunk> initChunks = TTSChunkFactory.createSimpleTTSChunks(simple);
		Vector<TTSChunk> timeoutChunks = TTSChunkFactory.createSimpleTTSChunks(
				mEventSeekr.getResources().getString(R.string.time_out));
		
		ALUtil.performInteractionChoiceSet(initChunks, initialText, interactionChoiceSetIDList, timeoutChunks);
	}
	
	private void handleNext() throws IOException {
		EventALUtil.onNextCommand(eventList, mEventSeekr);
	}
	
	private void handleBack() {
		EventALUtil.onBackCommand(eventList, mEventSeekr);
	}
	
	private void generateLatLon() {
    	double[] latLon = AppLinkService.getInstance().getLatLng();
    	lat = latLon[0];
    	lon = latLon[1];
    }
	
	private void addCommands() {
		//Log.d(TAG, "addCommands");
		Vector<Command> reqCmds = new Vector<Command>();
		reqCmds.add(Command.CALL_VENUE);
		//reqCmds.add(Commands.PLAY);
		reqCmds.add(Command.DETAILS);
		reqCmds.add(Command.BACK);
		reqCmds.add(Command.NEXT);
		
		Vector<Command> helpCommands = new Vector<CommandsUtil.Command>(reqCmds);
		Collections.reverse(helpCommands);
		
		//reqCmds.add(Command.SEARCH);
		reqCmds.add(Command.MY_EVENTS);
		reqCmds.add(Command.DISCOVER);
		CommandsUtil.addCommands(reqCmds, helpCommands);	
	}
	
	private Vector<SoftButton> buildSoftButtons() {
		Vector<SoftButton> softBtns = new Vector<SoftButton>();
		//softBtns.add(Commands.PLAY.buildSoftBtn());		
		softBtns.add(Command.CALL_VENUE.buildSoftBtn());
		return softBtns;
	}

	private void loadEvents(Type type) throws IOException {
		List<Event> tmpEvents = null;

		UserInfoApi userInfoApi = buildUserInfoApi();
		ItemsList<Event> myEventsList = null;
		try {
			this.type = type;
			JSONObject jsonObject = userInfoApi.getMyProfileInfoFor(type);
			UserInfoApiJSONParser jsonParser = new UserInfoApiJSONParser();
			
			if (type == Type.myevents) {
				myEventsList = jsonParser.getEventList(jsonObject);
				
			} else {
				myEventsList = jsonParser.getRecommendedEventList(jsonObject);
			}
			tmpEvents = myEventsList.getItems();
			//Log.d(TAG, "load count = " + tmpEvents.size());
			
			eventList.setRequestCode(type);
			eventList.addAll(tmpEvents);
			eventList.setTotalNoOfEvents(myEventsList.getTotalCount());
			
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	private UserInfoApi buildUserInfoApi() {
		UserInfoApi userInfoApi = new UserInfoApi(Api.OAUTH_TOKEN_CAR_APPS);
		userInfoApi.setLimit(eventList.updateAndGetEventsLimit());
		userInfoApi.setAlreadyRequested(eventList.getEventsAlreadyRequested());
		userInfoApi.setUserId(mEventSeekr.getWcitiesId());
		userInfoApi.setLat(lat);
		userInfoApi.setLon(lon);
		userInfoApi.setAddFordLangParam(true);
		return userInfoApi;
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
			
		case NEXT:
			try {
				handleNext();
				
			} catch (IOException e) {
				e.printStackTrace();
				AppLinkService.getInstance().handleNoNetConnectivity();
			}
			break;
			
		case BACK:
			handleBack();
			break;
			
		case DETAILS:
			EventALUtil.speakDetailsOfEvent(eventList.getCurrentEvent(), mEventSeekr);
			break;
			
		/*case PLAY:
			break;*/
			
		case CALL_VENUE:
			EventALUtil.callVenue(eventList);
			break;
			
		default:
			Log.d(TAG, cmd + " is an Invalid Command");
			break;
		}
	}
	
	@Override
	public void onPerformInteractionResponse(PerformInteractionResponse response) {
		super.onPerformInteractionResponse(response);
		
		if (response == null || response.getChoiceID() == null) {
			// This will happen when on Choice menu user selects cancel button
			AppLinkService.getInstance().initiateMainAL();
			return;
		}
		
		if (response.getChoiceID() == SuggestionReply.Yes.id) {
			try {
				loadEvents(Type.recommendedevent);
				
				if (eventList.isEmpty()) {
					AppLinkService.getInstance().initiateMainAL();
				} 
				
				handleNext();
				
			} catch (IOException e) {
				e.printStackTrace();
				AppLinkService.getInstance().handleNoNetConnectivity();
			}

		} else if (response.getChoiceID() == SuggestionReply.No.id) {
			AppLinkService.getInstance().initiateMainAL();
			ALUtil.speak(R.string.my_events_how_can_i_help);
		}
	}

	@Override
	public void loadEvents() throws IOException {
		loadEvents(type);
	}
}
