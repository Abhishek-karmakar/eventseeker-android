package com.wcities.eventseeker.applink.handler;

import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
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
import com.wcities.eventseeker.api.EventApi.MoreInfo;
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
import com.wcities.eventseeker.jsonparser.EventApiJSONParser;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.DeviceUtil;

public class DiscoverAL extends ESIProxyALM implements LoadEventsListener {

	private static final String TAG = DiscoverAL.class.getName();
	private static final int CHOICE_CATEGORIES_DISCOVER_AL = 1000;
	private static final int EVENTS_LIMIT = 10;
	private static final int MILES_LIMIT = 25;

	private static DiscoverAL instance;

	private EventSeekr context;
	private EventList eventList;
	private double lat, lon;
	private int selectedCategoryId;
	private static final int CHOICE_SET_ID_DISCOVER = 0;
	
	public static enum GetEventsFrom {
		EVENTS,
		FEATURED_EVENTS;
	}
	
	private static enum Discover {
		Concerts(CHOICE_CATEGORIES_DISCOVER_AL, 900, R.string.discover_al_concerts),
		Clubs(CHOICE_CATEGORIES_DISCOVER_AL + 1, 905, R.string.discover_al_clubs),
		Sports(CHOICE_CATEGORIES_DISCOVER_AL + 2, 902, R.string.discover_al_sports),
		Theater(CHOICE_CATEGORIES_DISCOVER_AL + 3, 901, R.string.discover_al_theater),
		Festivals(CHOICE_CATEGORIES_DISCOVER_AL + 4, 907, R.string.discover_al_festivals);
		
		private int id, categoryId, nameResId;
		
		private Discover(int value, int categoryId, int nameResId) {
			this.id = value;
			this.categoryId = categoryId;
			this.nameResId = nameResId;
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
			return AppLinkService.getInstance().getResources().getString(nameResId);
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
		eventList = new EventList();
		eventList.setEventsLimit(EVENTS_LIMIT);
		eventList.setLoadEventsListener(this);		
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

	private void initializeInteractionChoiceSets() {
		Log.d(TAG, "initializeInteractionChoiceSets()");

		Vector<Choice> choices = new Vector<Choice>();
		
		Discover[] categories = Discover.values();
		for (int i = 0; i < categories.length; i++) {
			Discover category = categories[i];
			
			Log.d(TAG, "Category id : " + category.getId());
			Log.d(TAG, "Category nameResId : " + category.getName());
			
			Choice choice = ALUtil.createChoice(category.getId(), category.getName(), 
					new Vector<String>(Arrays.asList(new String[] {category.getName()})));
			choices.add(choice);
		}

		ALUtil.createInteractionChoiceSet(choices, CHOICE_SET_ID_DISCOVER);
	}
	
	private void performInteraction() {
		Log.d(TAG, "performInteraction()");
		Log.d(TAG, "Discover ordinal : " + CHOICE_SET_ID_DISCOVER);
		
		Vector<Integer> interactionChoiceSetIDList = new Vector<Integer>();
		interactionChoiceSetIDList.add(CHOICE_SET_ID_DISCOVER);
		
		String simple = AppLinkService.getInstance().getString(R.string.discover_al_discover_categories);
		String initialText = context.getResources().getString(R.string.discover_al_discover);		
		
		Vector<TTSChunk> initChunks = TTSChunkFactory.createSimpleTTSChunks(simple);
		Vector<TTSChunk> timeoutChunks = TTSChunkFactory.createSimpleTTSChunks(
				context.getResources().getString(R.string.time_out));
		
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
		 * According to current implementation, 1st make Featured events call and if events are available,
		 * then show these events to user and if not, only then load events from 'getEvents' API call.
		 */
		loadFeaturedEvents(selectedCategoryId);
		if (eventList.isEmpty()) {
			loadEvents(selectedCategoryId);
		}

		//show Welcome message when no events are available
		if (eventList.isEmpty()) {
			ALUtil.displayMessage(R.string.msg_welcome_to, R.string.msg_eventseeker);
		}
		onNextCommand();
	}
	
	private void loadEvents(int categoryId) {
		/**
		 * http://dev.wcities.com/V3/event_api/getEvents.php?oauth_token=5c63440e7db1ad33c3898cdac3405b1e
		 * &lat=37.783300&lon=-122.416700&start=2014-04-14&end=2014-04-21&cat=900&subcat=&response_type=json
		 * &limit=0,10&moreInfo=artistdesc&strip_html=nameResId,description&lang=en&miles=25
		 */
		/**
		 * First show the loading message
		 */
		ALUtil.displayMessage(context.getResources().getString(R.string.loading), "");
		
		List<Event> tmpEvents = null;
		int eventsAlreadyRequested = eventList.getEventsAlreadyRequested();
		int totalNoOfEvents = 0;

		EventApi eventApi = new EventApi(Api.FORD_OAUTH_TOKEN, lat, lon);
		eventApi.setStart(getStartDate());
		eventApi.setEnd(getEndDate());
		eventApi.setMiles(MILES_LIMIT);
		eventApi.setLimit(EVENTS_LIMIT);
		eventApi.setCategory(categoryId);
		eventApi.setAlreadyRequested(eventsAlreadyRequested);
		eventApi.addMoreInfo(MoreInfo.booking);
		eventApi.addMoreInfo(MoreInfo.multiplebooking);
		
		try {
			JSONObject jsonObject = eventApi.getEvents();
			EventApiJSONParser jsonParser = new EventApiJSONParser();
			
			ItemsList<Event> eventsList = jsonParser.getEventItemList(jsonObject, GetEventsFrom.EVENTS);
			tmpEvents = eventsList.getItems();
			totalNoOfEvents = eventsList.getTotalCount();
			
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
			
		} catch (JSONException e) {
			e.printStackTrace();
		}

		eventList.setRequestCode(GetEventsFrom.EVENTS);
		eventList.addAll(tmpEvents);
		eventList.setTotalNoOfEvents(totalNoOfEvents);
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
		ALUtil.displayMessage(context.getResources().getString(R.string.loading), "");
		
		List<Event> tmpEvents = null;
		int eventsAlreadyRequested = eventList.getEventsAlreadyRequested();
		int totalNoOfEvents = 0;
		
		EventApi eventApi = new EventApi(Api.FORD_OAUTH_TOKEN, lat, lon);
		eventApi.setCategory(categoryId);
		eventApi.setStart(getStartDate());
		eventApi.setEnd(getEndDate());
		eventApi.setMiles(MILES_LIMIT);
		eventApi.setLimit(EVENTS_LIMIT);
		eventApi.setAlreadyRequested(eventsAlreadyRequested);
		
		try {
			JSONObject jsonObject = eventApi.getFeaturedEventsForFord();
			EventApiJSONParser jsonParser = new EventApiJSONParser();
			
			ItemsList<Event> eventsList = jsonParser.getEventItemList(jsonObject, GetEventsFrom.FEATURED_EVENTS);
			tmpEvents = eventsList.getItems();
			totalNoOfEvents = eventsList.getTotalCount();
			
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
			
		} catch (JSONException e) {
			e.printStackTrace();
		}

		eventList.setRequestCode(GetEventsFrom.FEATURED_EVENTS);
		eventList.addAll(tmpEvents);
		eventList.setTotalNoOfEvents(totalNoOfEvents);
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
    	//Log.d(TAG, "lat = " + lat + ", lon = " + lon);
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
	
	/**
	 * reset the fields to default if Discover screen is being launched from the
	 * Discover screen only.
	 * @param cmd
	 */
	private void resetIfNeeded(Commands cmd) {
		if (cmd != Commands.DISCOVER) {
			return;
		}
		selectedCategoryId = 0;
		eventList.resetEventList();
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
			default:
				Log.d(TAG, cmd + " is an Invalid Command");
				break;
			
		}
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

	@Override
	public void loadEvents() {
		GetEventsFrom which = (GetEventsFrom) eventList.getRequestCode();
		switch (which) {
		case EVENTS:
			loadEvents(selectedCategoryId);
			break;
		case FEATURED_EVENTS:
			loadFeaturedEvents(selectedCategoryId);
			break;
		}
	}

}
