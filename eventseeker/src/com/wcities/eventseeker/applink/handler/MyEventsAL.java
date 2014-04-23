package com.wcities.eventseeker.applink.handler;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.ford.syncV4.proxy.TTSChunkFactory;
import com.ford.syncV4.proxy.rpc.Choice;
import com.ford.syncV4.proxy.rpc.OnButtonEvent;
import com.ford.syncV4.proxy.rpc.OnCommand;
import com.ford.syncV4.proxy.rpc.PerformInteractionResponse;
import com.ford.syncV4.proxy.rpc.SoftButton;
import com.ford.syncV4.proxy.rpc.TTSChunk;
import com.ford.syncV4.proxy.rpc.enums.ButtonEventMode;
import com.ford.syncV4.proxy.rpc.enums.ButtonName;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi;
import com.wcities.eventseeker.api.UserInfoApi.Type;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.applink.datastructure.EventList;
import com.wcities.eventseeker.applink.datastructure.EventList.LoadEventsListener;
import com.wcities.eventseeker.applink.service.AppLinkService;
import com.wcities.eventseeker.applink.util.ALUtil;
import com.wcities.eventseeker.applink.util.CommandsUtil;
import com.wcities.eventseeker.applink.util.CommandsUtil.Commands;
import com.wcities.eventseeker.applink.util.EventALUtil;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.ItemsList;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser;
import com.wcities.eventseeker.util.DeviceUtil;

public class MyEventsAL extends ESIProxyALM implements LoadEventsListener {

	private static final String TAG = MyEventsAL.class.getName();
	private static final int EVENTS_LIMIT = 10;
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
		eventList.setEventsLimit(EVENTS_LIMIT);
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
			eventList.resetEventList();
		}
		addCommands();
		addSoftButtons();
		
		generateLatLon();
		loadEvents(Type.myevents);
		
		if (eventList.isEmpty()) {
			onNoMyEventsFound();
			
		} else {
			handleNext();
		}
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
		String initialText = mEventSeekr.getResources().getString(R.string.my_events_al_suggestions);	
		
		Vector<TTSChunk> initChunks = TTSChunkFactory.createSimpleTTSChunks(simple);
		Vector<TTSChunk> timeoutChunks = TTSChunkFactory.createSimpleTTSChunks(
				mEventSeekr.getResources().getString(R.string.time_out));
		
		ALUtil.performInteractionChoiceSet(initChunks, initialText, interactionChoiceSetIDList, timeoutChunks);
	}
	
	private void handleNext() {
		EventALUtil.onNextCommand(eventList, mEventSeekr);
	}
	
	private void handleBack() {
		EventALUtil.onBackCommand(eventList, mEventSeekr);
	}
	
	private void generateLatLon() {
    	double[] latLon = DeviceUtil.getLatLon(mEventSeekr);
    	lat = latLon[0];
    	lon = latLon[1];
    }
	
	private void addCommands() {
		Vector<Commands> reqCmds = new Vector<Commands>();
		reqCmds.add(Commands.CALL_VENUE);
		//reqCmds.add(Commands.PLAY);
		reqCmds.add(Commands.DETAILS);
		reqCmds.add(Commands.BACK);
		reqCmds.add(Commands.NEXT);
		reqCmds.add(Commands.SEARCH);
		reqCmds.add(Commands.MY_EVENTS);
		reqCmds.add(Commands.DISCOVER);
		CommandsUtil.addCommands(reqCmds);	
	}
	
	private void addSoftButtons() {
		Vector<SoftButton> softBtns = new Vector<SoftButton>();
		
		/*SoftButton softBtnPlay = new SoftButton();
		softBtnPlay.setSoftButtonID(Commands.PLAY.getCmdId());
		softBtnPlay.setText(Commands.PLAY.toString());
		softBtns.add(softBtnPlay);*/
		
		SoftButton softBtnCallVenue = new SoftButton();
		softBtnCallVenue.setSoftButtonID(Commands.CALL_VENUE.getCmdId());
		softBtnCallVenue.setText(Commands.CALL_VENUE.toString());
		softBtns.add(softBtnCallVenue);
		
		ALUtil.displayMessage("Loading...", "", softBtns);
	}

	private void loadEvents(Type type) {
		List<Event> tmpEvents = null;
		int totalNoOfEvents = 0;

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
			totalNoOfEvents = myEventsList.getTotalCount();
			Log.d(TAG, "load count = " + tmpEvents.size());
			
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		eventList.setRequestCode(type);
		eventList.addAll(tmpEvents);
		eventList.setTotalNoOfEvents(totalNoOfEvents);
	}
	
	private UserInfoApi buildUserInfoApi() {
		UserInfoApi userInfoApi = new UserInfoApi(Api.FORD_OAUTH_TOKEN);
		userInfoApi.setLimit(EVENTS_LIMIT);
		userInfoApi.setAlreadyRequested(eventList.getEventsAlreadyRequested());
		userInfoApi.setUserId(mEventSeekr.getWcitiesId());
		userInfoApi.setLat(lat);
		userInfoApi.setLon(lon);
		return userInfoApi;
	}
	
	private void performOperationForCommand(Commands cmd) {
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
			handleNext();
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
	
	/**
	 * reset the fields to default if my events screen is being launched from the
	 * this my events screen itself.
	 * @param cmd
	 */
	private void resetIfNeeded(Commands cmd) {
		if (cmd == Commands.MY_EVENTS) {
			eventList.resetEventList();	
		}
	}
	
	@Override
	public void onOnCommand(OnCommand notification) {
		/************************************************
		 * NOTE:notification.getCmdID() is not working. *
		 * So, we have used the alternative for the same*
		 ************************************************/
		int cmdId = Integer.parseInt(notification.getParameters("cmdID").toString());
		Commands cmd = Commands.getCommandById(cmdId);
		resetIfNeeded(cmd);
		performOperationForCommand(cmd);
	}
	
	@Override
	public void onOnButtonEvent(OnButtonEvent notification) {
		if (notification.getButtonName() == ButtonName.CUSTOM_BUTTON && notification.getCustomButtonID() == 
				Commands.CALL_VENUE.getCmdId() && notification.getButtonEventMode() == ButtonEventMode.BUTTONUP) {
			EventALUtil.callVenue(eventList);
		}
	}
	
	@Override
	public void onPerformInteractionResponse(PerformInteractionResponse response) {
		super.onPerformInteractionResponse(response);
		
		if (response.getChoiceID() == SuggestionReply.Yes.id) {
			loadEvents(Type.recommendedevent);

			if (eventList.isEmpty()) {
				ALUtil.displayMessage(R.string.msg_welcome_to, R.string.msg_eventseeker);
				EventALUtil.speak(R.string.event_no_evts_avail);
				
			} else {
				handleNext();
			}
			
		} else if (response.getChoiceID() == SuggestionReply.No.id) {
			EventALUtil.speak(R.string.my_events_how_can_i_help);
		}
	}

	@Override
	public void loadEvents() {
		loadEvents(type);
	}
}
