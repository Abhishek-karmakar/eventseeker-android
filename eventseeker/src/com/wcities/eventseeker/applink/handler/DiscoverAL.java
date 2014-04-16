package com.wcities.eventseeker.applink.handler;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
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
import com.ford.syncV4.proxy.rpc.OnButtonPress;
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
import com.ford.syncV4.proxy.rpc.SubscribeVehicleDataResponse;
import com.ford.syncV4.proxy.rpc.TTSChunk;
import com.ford.syncV4.proxy.rpc.UnsubscribeVehicleDataResponse;
import com.ford.syncV4.proxy.rpc.enums.ButtonName;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.EventApi;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.applink.interfaces.ESIProxyALM;
import com.wcities.eventseeker.applink.service.AppLinkService;
import com.wcities.eventseeker.applink.util.ALUtil;
import com.wcities.eventseeker.applink.util.CommandsUtil;
import com.wcities.eventseeker.applink.util.CommandsUtil.Commands;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Date;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.ItemsList;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.jsonparser.EventApiJSONParser;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.DeviceUtil;

public class DiscoverAL extends ESIProxyALM {

	private static final String TAG = DiscoverAL.class.getName();
	private static final int EVENTS_LIMIT = 10;
	private static final int MILES_LIMIT = 25;
	private static final String COUNTRY_NAME = "United States";

	private static DiscoverAL instance;

	private EventSeekr context;
	private List<Event> discoverByCategoryEvtList;	
	private double lat, lon;
	private int currentEvtPos = -1;
	private int eventsAlreadyRequested;
	private int selectedCategoryId;
	private int totalNoOfEvents;
	private boolean isMoreDataAvailable = true;
	private static int CHOICE_SET_ID_DISCOVER = 0;
	private GetEventsFrom whichCall;
	
	public static enum GetEventsFrom {
		EVENTS,
		FEATURED_EVENTS;
	}
	
	private static enum Discover {
		Concerts(AppConstants.CHOICE_CATEGORIES_DISCOVER_AL, AppConstants.CATEGORY_ID_START, 
				R.string.discover_al_concerts),
		Clubs(AppConstants.CHOICE_CATEGORIES_DISCOVER_AL + 1, AppConstants.CATEGORY_ID_START + 5, 
				R.string.discover_al_clubs),
		Sports(AppConstants.CHOICE_CATEGORIES_DISCOVER_AL + 2, AppConstants.CATEGORY_ID_START + 2, 
				R.string.discover_al_sports),
		Theater(AppConstants.CHOICE_CATEGORIES_DISCOVER_AL + 3, AppConstants.CATEGORY_ID_START + 1,
				R.string.discover_al_theater),
		Festivals(AppConstants.CHOICE_CATEGORIES_DISCOVER_AL + 4, AppConstants.CATEGORY_ID_START + 7, 
				R.string.discover_al_festivals);
		
		private int id, categoryId, name;
		
		private Discover(int value, int categoryId, int name) {
			this.id = value;
			this.categoryId = categoryId;
			this.name = name;
		}

		public static Discover getDiscoverChoiceId(int discoverId) {
			Discover[] discovers = Discover.values();
			for (Discover discover : discovers) {
				if (discover.id == discoverId) {
					return discover;
				}
			}
			return null;
		}
		
		public String getName() {
			return AppLinkService.getStringFromRes(name);
		}
		
		public int getId() {
			return id;
		}
		
		public int getCategoryId() {
			return categoryId;
		}
		
	}
	
	public DiscoverAL(EventSeekr context) {
		this.context = context;
		discoverByCategoryEvtList = new ArrayList<Event>();
	}

	public static ESIProxyALM getInstance(EventSeekr context) {
		if (instance == null) {
			instance = new DiscoverAL(context);
		}
		return instance;
	}
	
	@Override
	public void onStartInstance() {
		Log.d(TAG, "onStartInstance()");
		initializeInteractionChoiceSets();
		performInteraction();		
		addCommands();
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

	private void initializeInteractionChoiceSets() {
		Log.d(TAG, "initializeInteractionChoiceSets()");

		Vector<Choice> choices = new Vector<Choice>();
		
		Discover[] categories = Discover.values();
		for (int i = 0; i < categories.length; i++) {
			Discover category = categories[i];
			
			Log.d(TAG, "Category id : " + category.getId());
			Log.d(TAG, "Category name : " + category.getName());
			
			Choice choice = ALUtil.createChoice(category.getId(), category.getName(), 
					new Vector<String>(Arrays.asList(new String[] {category.getName()})));
			choices.add(choice);
		}

		/**
		 * TODO:If needed, add ++ before CHOICE_SET_ID_DISCOVER for incrementing it
		 * CHOICE_SET_ID_DISCOVER must always be unique. So, each time when screen gets
		 * reloaded then it must be incremented.
		 */
		ALUtil.createInteractionChoiceSet(choices, CHOICE_SET_ID_DISCOVER/*ChoiceSetId.Discover.ordinal()*/);
	}
	
	private void performInteraction() {
		Log.d(TAG, "performInteraction()");
		Log.d(TAG, "Discover ordinal : " + CHOICE_SET_ID_DISCOVER/*ChoiceSetId.Discover.ordinal()*/);
		
		Vector<Integer> interactionChoiceSetIDList = new Vector<Integer>();
		interactionChoiceSetIDList.add(CHOICE_SET_ID_DISCOVER/*ChoiceSetId.Discover.ordinal()*/);
		
		String simple = AppLinkService.getInstance().getString(R.string.discover_al_discover_categories);
		/**
		 * TODO: After testing it on TDK, check if initialText is the Title of the ChoiceSet then
		 * change it to "Say Category" as in IOS.
		 */
		String initialText = AppLinkService.getStringFromRes(R.string.discover_al_discover);//ChoiceSetId.Discover.getName();		
		
		Vector<TTSChunk> initChunks = TTSChunkFactory.createSimpleTTSChunks(simple);
		Vector<TTSChunk> timeoutChunks = TTSChunkFactory.createSimpleTTSChunks(
				AppLinkService.getStringFromRes(R.string.time_out));
		
		ALUtil.performInteractionChoiceSet(initChunks, initialText, interactionChoiceSetIDList, timeoutChunks);
	}
	
	@Override
	public void onPerformInteractionResponse(PerformInteractionResponse response) {
		Log.i(TAG, "onPerformInteractionResponse(), response.getChoiceID() = " + response.getChoiceID());
		
		if (Discover.getDiscoverChoiceId(response.getChoiceID()) == null) {
			//TODO: when Choice Id is invalid that is null	
			Log.i(TAG, "Discover.getDiscoverChoiceId(response.getChoiceID()) == null");
		}
		
		generateLatLon();
		
		selectedCategoryId = Discover.getDiscoverChoiceId(response.getChoiceID()).getCategoryId();
		/**
		 * Accordig to current implementation, 1st make Featured events call and if events are available,
		 * then show these events to user and if not, only then load events from 'getEvents' API call.
		 */
		loadFeaturedEvents(selectedCategoryId);
		if (discoverByCategoryEvtList.isEmpty()) {
			loadEvents(selectedCategoryId);
		}

		onNextCommand();
	}
	
	private void speakNoEventsAvailable() {
		String simple = AppLinkService.getStringFromRes(R.string.event_no_evts_avail);
		Vector<TTSChunk> ttsChunks = TTSChunkFactory.createSimpleTTSChunks(simple);
		ALUtil.speakText(ttsChunks);		
	}
	
	private void displayCurrentEvent() {
		Event event = discoverByCategoryEvtList.get(currentEvtPos);
		ALUtil.displayMessage(event.getName(), (currentEvtPos + 1) + "/" + totalNoOfEvents);
	}

	private void speakCurrentEvent() {
		/**
		 * TODO: after launching the app, each and every time system should speak about the 
		 * first event and then append the 'plz press next or back' and then throughout the
		 * current session it shouldn't append the second line.
		 */
		Event event = discoverByCategoryEvtList.get(currentEvtPos);

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

	private void speakDetailsOfCurrentEvent() {
		/**
		 * TODO: Current speech text is the not being used in IOS. So, it needs to be removed 
		 * and integrate the new one.
		 */
		Event event = discoverByCategoryEvtList.get(currentEvtPos);
		
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

	
	private void loadEvents(int categoryId) {
		/**
		 * http://dev.wcities.com/V3/event_api/getEvents.php?oauth_token=5c63440e7db1ad33c3898cdac3405b1e
		 * &lat=37.783300&lon=-122.416700&start=2014-04-14&end=2014-04-21&cat=900&subcat=&response_type=json
		 * &limit=0,10&moreInfo=artistdesc&strip_html=name,description&lang=en&miles=25
		 */
		/**
		 * First show the loading message
		 */
		ALUtil.displayMessage(AppLinkService.getStringFromRes(R.string.loading), "");
		
		List<Event> tmpEvents = null;

		EventApi eventApi = new EventApi(Api.FORD_OAUTH_TOKEN, lat, lon);
		eventApi.setStart(getStartDate());
		eventApi.setEnd(getEndDate());
		eventApi.setMiles(MILES_LIMIT);
		eventApi.setLimit(EVENTS_LIMIT);
		eventApi.setCategory(categoryId);
		eventApi.setAlreadyRequested(eventsAlreadyRequested);

		whichCall = GetEventsFrom.EVENTS;
		
		try {
			JSONObject jsonObject = eventApi.getEvents();
			EventApiJSONParser jsonParser = new EventApiJSONParser();
			
			//tmpEvents = jsonParser.getEventList(jsonObject);
			ItemsList<Event> eventsList = jsonParser.getEventItemList(jsonObject, whichCall);
			tmpEvents = eventsList.getItems();
			totalNoOfEvents = eventsList.getTotalCount();
			
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		if (tmpEvents != null && !tmpEvents.isEmpty()) {
			discoverByCategoryEvtList.addAll(tmpEvents);
			eventsAlreadyRequested += tmpEvents.size();
			
			if (tmpEvents.size() < EVENTS_LIMIT) {
				isMoreDataAvailable = false;
			}
			
		} else {
			isMoreDataAvailable = false;
		}
	}
	
	private void loadFeaturedEvents(int categoryId) {
		/**
		 * http://dev.wcities.com/V3/featured_event/getFeaturedEvents.php?oauth_token=5c63440e7db1ad33c3898cdac3405b1e
		 * &type=featured&lat=37.332331&lon=-122.031219&cat=900&subcat=&start=2014-04-14&end=2014-04-21&miles=25
		 * &limit=0,10&moreInfo=booking&lang=en
		 */
		/**
		 * First show the loading message
		 */
		ALUtil.displayMessage(AppLinkService.getStringFromRes(R.string.loading), "");
		
		List<Event> tmpEvents = null;
		
		EventApi eventApi = new EventApi(Api.FORD_OAUTH_TOKEN, lat, lon);
		eventApi.setCategory(categoryId);
		eventApi.setStart(getStartDate());
		eventApi.setEnd(getEndDate());
		eventApi.setMiles(MILES_LIMIT);
		eventApi.setLimit(EVENTS_LIMIT);
		eventApi.setAlreadyRequested(eventsAlreadyRequested);
		
		whichCall = GetEventsFrom.FEATURED_EVENTS;
		
		try {
			JSONObject jsonObject = eventApi.getFeaturedEventsForFord();
			EventApiJSONParser jsonParser = new EventApiJSONParser();
			
			//tmpEvents = jsonParser.getEventList(jsonObject);
			ItemsList<Event> eventsList = jsonParser.getEventItemList(jsonObject, whichCall);
			tmpEvents = eventsList.getItems();
			totalNoOfEvents = eventsList.getTotalCount();
			
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		if (tmpEvents != null && !tmpEvents.isEmpty()) {
			discoverByCategoryEvtList.addAll(tmpEvents);
			eventsAlreadyRequested += tmpEvents.size();
			
			if (tmpEvents.size() < EVENTS_LIMIT) {
				isMoreDataAvailable = false;
			}
			
		} else {
			isMoreDataAvailable = false;
		}
	}
	
	private String getStartDate() {
		Calendar c = Calendar.getInstance();
		return ConversionUtil.getDay(c);
	}

	private String getEndDate() {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DATE, 7);
		return ConversionUtil.getDay(c);
	}

	private void generateLatLon() {
    	double[] latLon = DeviceUtil.getLatLon(context);
    	lat = latLon[0];
    	lon = latLon[1];
    	Log.i(TAG, "lat = " + lat + ", lon = " + lon);
    }
	
	private boolean hasNextEvents() {
		if (currentEvtPos + 1 < discoverByCategoryEvtList.size()) {
			++currentEvtPos;
			return true;
			
		} else if (isMoreDataAvailable) {
			switch (whichCall) {
			case FEATURED_EVENTS:
				loadFeaturedEvents(selectedCategoryId);
				break;
			case EVENTS:
				loadEvents(selectedCategoryId);
				break;
			}
			
			if (currentEvtPos + 1 < discoverByCategoryEvtList.size()) {
				++currentEvtPos;
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
			--currentEvtPos;
			return true;
			
		} else {
			return false;
		}
	}

	@Override
	public void onOnButtonPress(OnButtonPress notification) {
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
	
	/**
	 * reset the fields to default if Discover screen is being launched from the
	 * Discover screen only.
	 * @param cmd
	 */
	private void resetIfNeeded(Commands cmd) {
		if (cmd != Commands.DISCOVER) {
			return;
		}
		discoverByCategoryEvtList.clear();	
		lat = 0;
		lon = 0;
		currentEvtPos = -1;
		eventsAlreadyRequested = 0;
		selectedCategoryId = 0;
		isMoreDataAvailable = true;		
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
	
	private void onNextCommand() {
		if (hasNextEvents()) {
			displayCurrentEvent();
			speakCurrentEvent();
			
		} else {
			//TODO:show some message when no events are available
			//TODO:ALUtil.displayMessage(R.string.main_al_welcome_to, R.string.main_al_eventseeker);
			if (discoverByCategoryEvtList.size() == 0) {
				ALUtil.displayMessage(R.string.main_al_welcome_to, R.string.main_al_eventseeker);
			}
			speakNoEventsAvailable();
		}		
	}

	private void onBackCommand() {
		if (hasPreviousEvents()) {
			displayCurrentEvent();
			speakCurrentEvent();
			
		} else {
			speakNoEventsAvailable();
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
