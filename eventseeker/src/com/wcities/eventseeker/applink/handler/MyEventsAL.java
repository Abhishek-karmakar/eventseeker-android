package com.wcities.eventseeker.applink.handler;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.ford.syncV4.proxy.TTSChunkFactory;
import com.ford.syncV4.proxy.rpc.ChangeRegistrationResponse;
import com.ford.syncV4.proxy.rpc.Choice;
import com.ford.syncV4.proxy.rpc.DeleteFileResponse;
import com.ford.syncV4.proxy.rpc.DialNumberResponse;
import com.ford.syncV4.proxy.rpc.EndAudioPassThruResponse;
import com.ford.syncV4.proxy.rpc.GetDTCsResponse;
import com.ford.syncV4.proxy.rpc.GetVehicleDataResponse;
import com.ford.syncV4.proxy.rpc.ListFilesResponse;
import com.ford.syncV4.proxy.rpc.OnAudioPassThru;
import com.ford.syncV4.proxy.rpc.OnCommand;
import com.ford.syncV4.proxy.rpc.OnLanguageChange;
import com.ford.syncV4.proxy.rpc.OnVehicleData;
import com.ford.syncV4.proxy.rpc.PerformAudioPassThruResponse;
import com.ford.syncV4.proxy.rpc.PerformInteractionResponse;
import com.ford.syncV4.proxy.rpc.PutFileResponse;
import com.ford.syncV4.proxy.rpc.ReadDIDResponse;
import com.ford.syncV4.proxy.rpc.ScrollableMessageResponse;
import com.ford.syncV4.proxy.rpc.SetAppIconResponse;
import com.ford.syncV4.proxy.rpc.SetDisplayLayoutResponse;
import com.ford.syncV4.proxy.rpc.SliderResponse;
import com.ford.syncV4.proxy.rpc.SoftButton;
import com.ford.syncV4.proxy.rpc.SubscribeVehicleDataResponse;
import com.ford.syncV4.proxy.rpc.TTSChunk;
import com.ford.syncV4.proxy.rpc.UnsubscribeVehicleDataResponse;
import com.ford.syncV4.proxy.rpc.enums.SystemAction;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi;
import com.wcities.eventseeker.api.UserInfoApi.Type;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.applink.interfaces.ESIProxyALM;
import com.wcities.eventseeker.applink.service.AppLinkService;
import com.wcities.eventseeker.applink.util.ALUtil;
import com.wcities.eventseeker.applink.util.CommandsUtil;
import com.wcities.eventseeker.applink.util.CommandsUtil.Commands;
import com.wcities.eventseeker.core.Date;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.ItemsList;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser;
import com.wcities.eventseeker.util.DeviceUtil;

public class MyEventsAL extends ESIProxyALM {

	private static final String TAG = MyEventsAL.class.getName();
	private static final int EVENTS_LIMIT = 10;
	private static final String COUNTRY_NAME = "United States";
	private static final int START_CHOICE_ID_SUGGESTION_REPLY = 1;
	private static final int CHOICE_SET_ID_SUGGESTION_REPLY = 1;
	
	private static MyEventsAL instance;

	private EventSeekr mEventSeekr;
	private List<Event> currentEvtList;
	private int currentEvtPos = -1;
	private int eventsAlreadyRequested, totalNoOfEvents;
	private double lat, lon;
	private boolean isMoreDataAvailable = true;
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
		currentEvtList = new ArrayList<Event>();
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
		addCommands();
		addSoftButtons();
		
		generateLatLon();
		loadEvents(Type.myevents);
		
		if (currentEvtList.isEmpty()) {
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
				AppLinkService.getStringFromRes(R.string.time_out));
		
		ALUtil.performInteractionChoiceSet(initChunks, initialText, interactionChoiceSetIDList, timeoutChunks);
	}
	
	private void handleNext() {
		currentEvtPos++;
		displayCurrentEvent();
		speakCurrentEvent();
	}
	
	private void handleBack() {
		currentEvtPos--;
		displayCurrentEvent();
		speakCurrentEvent();
	}
	
	private void displayCurrentEvent() {
		Event event = currentEvtList.get(currentEvtPos);
		/**
		 * TODO: actually total count will be the one from api response. currently using the total events
		 * in the list. So, parse the value for total events from response and show it over here.
		 */
		ALUtil.displayMessage(event.getName(), (currentEvtPos + 1) + "/" + totalNoOfEvents);
	}

	private void speakCurrentEvent() {
		/**
		 * TODO: after launching the app, each and every time system should speak about the 
		 * first event and then append the 'plz press next or back' and then throughout the
		 * current session it shouldn't append the second line.
		 */
		Event event = currentEvtList.get(currentEvtPos);

		String simple = "Okay, " + event.getName();
		
		if (event.getSchedule() != null) {
			Venue venue = event.getSchedule().getVenue();
			if (venue != null) {
				String venueName = venue.getName();
				if (venueName != null) {
					simple += ", at " + venueName;			
				}
				
				List<Date> dates = event.getSchedule().getDates();
				if (dates != null && !dates.isEmpty()) {
					//simple += ", on " + ConversionUtil.getDateTime(dates.get(0));
					simple += ", on " + getFormattedDateTime(dates.get(0), venue);
				}
			}
		}
		
		Log.i(TAG, "simple = " + simple);
		
		Vector<TTSChunk> ttsChunks = TTSChunkFactory.createSimpleTTSChunks(simple);
		ALUtil.speakText(ttsChunks);				
	}
	
	public String getFormattedDateTime(Date date, Venue venue) {
		if (date == null) {
			return null;			
		}
		
		String dateTime = "";
		
		java.util.Date dt = date.getStartDate();
		SimpleDateFormat format = new SimpleDateFormat("dd MMM");
		dateTime += format.format(dt);
		
		if (date.isStartTimeAvailable()) {
			format = new SimpleDateFormat("HH");
			
			dateTime += ", " + format.format(dt);
			
			format = new SimpleDateFormat("m");
			
			String min = format.format(dt);
			if (min.length() > 1) {
				dateTime += " " + min;		
			} else {
				if (min.equals("0")) {
					dateTime += " hundred";	
				} else {
					if (venue.getAddress().getCountry().getName().equals(COUNTRY_NAME)) {
						//if country is USA then " 0 " must be spelled as " Oh "
						dateTime += " Oh " + min;						
						
					} else {
						dateTime += " 0 " + min;						
						
					}
				}
			}
			
			dateTime += " hours";
			Log.d(TAG, "dateTime : " + dateTime);
		}
		return dateTime;
	}
	
	private void generateLatLon() {
    	double[] latLon = DeviceUtil.getLatLon(mEventSeekr);
    	lat = latLon[0];
    	lon = latLon[1];
    }
	
	private void addCommands() {
		Vector<Commands> reqCmds = new Vector<Commands>();
		reqCmds.add(Commands.DISCOVER);
		reqCmds.add(Commands.MY_EVENTS);
		reqCmds.add(Commands.SEARCH);
		reqCmds.add(Commands.NEXT);
		reqCmds.add(Commands.BACK);
		reqCmds.add(Commands.DETAILS);
		reqCmds.add(Commands.PLAY);
		reqCmds.add(Commands.CALL_VENUE);
		CommandsUtil.addCommands(reqCmds);	
	}
	
	private void addSoftButtons() {
		Vector<SoftButton> softBtns = new Vector<SoftButton>();
		
		SoftButton softBtnPlay = new SoftButton();
		softBtnPlay.setSoftButtonID(Commands.PLAY.getCmdId());
		softBtnPlay.setText(Commands.PLAY.toString());
		softBtns.add(softBtnPlay);
		
		SoftButton softBtnCallVenue = new SoftButton();
		softBtnCallVenue.setSoftButtonID(Commands.CALL_VENUE.getCmdId());
		softBtnCallVenue.setText(Commands.CALL_VENUE.toString());
		softBtns.add(softBtnCallVenue);
		
		ALUtil.displayMessage("Loading...", "", softBtns);
	}

	private void loadEvents(Type type) {
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
			Log.d(TAG, "load count = " + tmpEvents.size());
			
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		if (tmpEvents != null && !tmpEvents.isEmpty()) {
			totalNoOfEvents = myEventsList.getTotalCount();
			currentEvtList.addAll(tmpEvents);
			eventsAlreadyRequested += tmpEvents.size();
			
			if (tmpEvents.size() < EVENTS_LIMIT) {
				isMoreDataAvailable = false;
			}
			
		} else {
			isMoreDataAvailable = false;
		}
	}
	
	private UserInfoApi buildUserInfoApi() {
		UserInfoApi userInfoApi = new UserInfoApi(Api.FORD_OAUTH_TOKEN);
		userInfoApi.setLimit(EVENTS_LIMIT);
		userInfoApi.setAlreadyRequested(eventsAlreadyRequested);
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
			onNextCommand();
			break;
			
		case BACK:
			onBackCommand();
			break;
			
		case DETAILS:
			//TODO:
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
	 * reset the fields to default if my events screen is being launched from the
	 * this my events screen itself.
	 * @param cmd
	 */
	private void resetIfNeeded(Commands cmd) {
		if (cmd == Commands.MY_EVENTS) {
			currentEvtList.clear();	
			currentEvtPos = -1;
			eventsAlreadyRequested = 0;
			isMoreDataAvailable = true;	
		}
	}
	
	private void speak(int strResId) {
		String simple = mEventSeekr.getResources().getString(strResId);
		Vector<TTSChunk> ttsChunks = TTSChunkFactory.createSimpleTTSChunks(simple);
		ALUtil.speakText(ttsChunks);		
	}
	
	private boolean hasMoreEvents() {
		if (currentEvtPos + 1 < currentEvtList.size()) {
			return true;
			
		} else if (isMoreDataAvailable) {
			
			loadEvents(type);
			
			if (currentEvtPos + 1 < currentEvtList.size()) {
				return true;
				
			} else {
				return false;
			}
			
		} else {
			return false;
		}
	}
	
	private boolean hasPreviousEvents() {
		if (currentEvtPos - 1 >= 0) {
			return true;
			
		} else {
			return false;
		}
	}
	
	private void onNextCommand() {
		if (hasMoreEvents()) {
			handleNext();
			
		} else {
			speak(R.string.event_no_evts_avail);
		}		
	}

	private void onBackCommand() {
		if (hasPreviousEvents()) {
			handleBack();
			
		} else {
			speak(R.string.event_no_evts_avail);
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
	public void onPerformInteractionResponse(PerformInteractionResponse response) {
		super.onPerformInteractionResponse(response);
		
		if (response.getChoiceID() == SuggestionReply.Yes.id) {
			loadEvents(Type.recommendedevent);

			if (currentEvtList.isEmpty()) {
				ALUtil.displayMessage(R.string.main_al_welcome_to, R.string.main_al_eventseeker);
				speak(R.string.event_no_evts_avail);
				
			} else {
				handleNext();
			}
			
		} else if (response.getChoiceID() == SuggestionReply.No.id) {
			speak(R.string.my_events_how_can_i_help);
		}
	}

	@Override
	public void onChangeRegistrationResponse(ChangeRegistrationResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDeleteFileResponse(DeleteFileResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDialNumberResponse(DialNumberResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onEndAudioPassThruResponse(EndAudioPassThruResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onGetDTCsResponse(GetDTCsResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onGetVehicleDataResponse(GetVehicleDataResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onListFilesResponse(ListFilesResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onOnAudioPassThru(OnAudioPassThru arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onOnLanguageChange(OnLanguageChange arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onOnVehicleData(OnVehicleData arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPerformAudioPassThruResponse(PerformAudioPassThruResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPutFileResponse(PutFileResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onReadDIDResponse(ReadDIDResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onScrollableMessageResponse(ScrollableMessageResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSetAppIconResponse(SetAppIconResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSetDisplayLayoutResponse(SetDisplayLayoutResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSliderResponse(SliderResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSubscribeVehicleDataResponse(SubscribeVehicleDataResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onUnsubscribeVehicleDataResponse(
			UnsubscribeVehicleDataResponse arg0) {
		// TODO Auto-generated method stub
		
	}
}
