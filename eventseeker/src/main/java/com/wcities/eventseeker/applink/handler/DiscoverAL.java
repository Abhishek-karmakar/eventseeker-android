package com.wcities.eventseeker.applink.handler;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Vector;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.ford.syncV4.proxy.TTSChunkFactory;
import com.ford.syncV4.proxy.rpc.PerformInteractionResponse;
import com.ford.syncV4.proxy.rpc.SoftButton;
import com.ford.syncV4.proxy.rpc.TTSChunk;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.EventApi;
import com.wcities.eventseeker.api.EventApi.MoreInfo;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.applink.core.EventList;
import com.wcities.eventseeker.applink.core.EventList.GetEventsFrom;
import com.wcities.eventseeker.applink.core.EventList.LoadEventsListener;
import com.wcities.eventseeker.applink.service.AppLinkService;
import com.wcities.eventseeker.applink.util.ALUtil;
import com.wcities.eventseeker.applink.util.CommandsUtil;
import com.wcities.eventseeker.applink.util.CommandsUtil.Command;
import com.wcities.eventseeker.applink.util.EventALUtil;
import com.wcities.eventseeker.applink.util.InteractionChoiceSetUtil.ChoiceSet;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.ItemsList;
import com.wcities.eventseeker.jsonparser.EventApiJSONParser;
import com.wcities.eventseeker.util.ConversionUtil;

public class DiscoverAL extends ESIProxyALM implements LoadEventsListener {

	private static final String TAG = DiscoverAL.class.getName();
	
	private static final int CHOICE_CATEGORIES_DISCOVER_AL = 1000;
	
	private static final int MIN_MILES = 25;
	private static final int MAX_MILES = 100;
	
	public static enum Discover {
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

	private static DiscoverAL instance;

	private EventSeekr context;
	private EventList eventList;
	private double lat, lon;
	private int selectedCategoryId, miles = MIN_MILES;
	
	public DiscoverAL(EventSeekr context) {
		this.context = context;
		eventList = new EventList();
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
		//Log.d(TAG, "onStartInstance()");
		reset();
		performInteraction();		
		addCommands();
		Vector<SoftButton> softBtns = buildSoftButtons();
		ALUtil.displayMessage(R.string.loading, AppConstants.INVALID_RES_ID, softBtns);
	}
	
	private void addCommands() {
		Vector<Command> requiredCmds = new Vector<Command>();
		requiredCmds.add(Command.DISCOVER);
		requiredCmds.add(Command.MY_EVENTS);
		//requiredCmds.add(Command.SEARCH);
		requiredCmds.add(Command.NEXT);
		requiredCmds.add(Command.BACK);
		//requiredCmds.add(Commands.PLAY);
		requiredCmds.add(Command.DETAILS);
		requiredCmds.add(Command.CALL_VENUE);
		
		CommandsUtil.addCommands(requiredCmds);
	}
	
	private Vector<SoftButton> buildSoftButtons() {
		Vector<SoftButton> softBtns = new Vector<SoftButton>();
		//softBtns.add(Commands.PLAY.buildSoftBtn());
		softBtns.add(Command.CALL_VENUE.buildSoftBtn());
		return softBtns;
	}

	private void performInteraction() {
		//Log.d(TAG, "performInteraction()");
		//Log.d(TAG, "Discover ordinal : " + CHOICE_SET_ID_DISCOVER);
		
		Vector<Integer> interactionChoiceSetIDList = new Vector<Integer>();
		interactionChoiceSetIDList.add(ChoiceSet.DISCOVER.ordinal());
		
		String simple = context.getResources().getString(R.string.discover_al_discover_categories);
		String initialText = context.getResources().getString(R.string.discover_al_discover);		
		
		Vector<TTSChunk> initChunks = TTSChunkFactory.createSimpleTTSChunks(simple);
		Vector<TTSChunk> timeoutChunks = TTSChunkFactory.createSimpleTTSChunks(
				//context.getResources().getString(R.string.time_out));
				context.getResources().getString(R.string.discover_al_time_out_help_text)) ;  
		
		ALUtil.performInteractionChoiceSet(initChunks, initialText, interactionChoiceSetIDList, timeoutChunks);
	}
	
	@Override
	public void onPerformInteractionResponse(final PerformInteractionResponse response) {
		super.onPerformInteractionResponse(response);
		//Log.i(TAG, "onPerformInteractionResponse(), response.getChoiceID() = " + response.getChoiceID());
		if (response == null || response.getChoiceID() == null) {
			/**
			* This will happen when on Choice menu user selects cancel button
			*/
			//Log.i(TAG, "ChoiceID == null");
			AppLinkService.getInstance().initiateMainAL();
			return;
		}
		
		generateLatLon();
		
		selectedCategoryId = Discover.getDiscoverChoiceId(response.getChoiceID()).getCategoryId();
		/**
		 * According to current implementation, 1st make Featured events call and if events are available,
		 * then show these events to user and if not, only then load events from 'getEvents' API call.
		 */
		try {
			while (eventList.isEmpty() && miles <= MAX_MILES) {
				loadFeaturedEvents(selectedCategoryId);
				if (eventList.isEmpty()) {
					loadEvents(selectedCategoryId);
				}
				miles *= 2;
			}
			// divide by 2 to compensate for last one extra multiplication
			miles /= 2;

			//show Welcome message when no events are available
			if (eventList.isEmpty()) {
				AppLinkService.getInstance().initiateMainAL();
			}
			EventALUtil.onNextCommand(eventList, context);
			
		} catch (IOException e) {
			e.printStackTrace();
			AppLinkService.getInstance().handleNoNetConnectivity();
		}
	}
	
	private void loadEvents(int categoryId) throws IOException {
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
		/*int eventsAlreadyRequested = eventList.getEventsAlreadyRequested();
		evtsLimit = ((MAX_EVENTS - eventsAlreadyRequested) > EVENTS_LIMIT_10) ? EVENTS_LIMIT_10 : 
			(MAX_EVENTS - eventsAlreadyRequested);
		eventList.setEventsLimit(evtsLimit);
		int totalNoOfEvents = 0;*/

		EventApi eventApi = new EventApi(Api.OAUTH_TOKEN_FORD_APP, lat, lon);
		eventApi.setStart(getStartDate());
		eventApi.setEnd(getEndDate());
		eventApi.setMiles(miles);
		eventApi.setLimit(eventList.updateAndGetEventsLimit());
		eventApi.setCategory(categoryId);
		eventApi.setAlreadyRequested(eventList.getEventsAlreadyRequested());
		eventApi.addMoreInfo(MoreInfo.booking);
		eventApi.addMoreInfo(MoreInfo.multiplebooking);
		eventApi.setAddFordLangParam(true);
		
		try {
			JSONObject jsonObject = eventApi.getEvents();
			EventApiJSONParser jsonParser = new EventApiJSONParser();
			
			ItemsList<Event> eventsList = jsonParser.getEventItemList(jsonObject, GetEventsFrom.EVENTS);
			tmpEvents = eventsList.getItems();
			//totalNoOfEvents = (eventsList.getTotalCount() > MAX_EVENTS) ? MAX_EVENTS : eventsList.getTotalCount();
			
			eventList.setRequestCode(GetEventsFrom.EVENTS);
			eventList.addAll(tmpEvents);
			eventList.setTotalNoOfEvents(eventsList.getTotalCount());
			/*if (eventList.size() >= MAX_EVENTS) {
				eventList.setMoreDataAvailable(false);
			}*/
			
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	private void loadFeaturedEvents(int categoryId) throws IOException {
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
		/*int eventsAlreadyRequested = eventList.getEventsAlreadyRequested();
		evtsLimit = ((MAX_EVENTS - eventsAlreadyRequested) > EVENTS_LIMIT_10) ? EVENTS_LIMIT_10 : 
			(MAX_EVENTS - eventsAlreadyRequested);
		eventList.setEventsLimit(evtsLimit);
		int totalNoOfEvents = 0;*/
		
		EventApi eventApi = new EventApi(Api.OAUTH_TOKEN_FORD_APP, lat, lon);
		eventApi.setCategory(categoryId);
		eventApi.setStart(getStartDate());
		eventApi.setEnd(getEndDate());
		eventApi.setMiles(miles);
		eventApi.setLimit(eventList.updateAndGetEventsLimit());
		/**
		 * 12-06-2014 : added wcitiesId in Featured event call as per Rohit/Sameer's mail
		 * 10-07-2014 : Commenting below line(setting the userId) in featured events call, as
		 * after setting it. The total events get restricted to '8'.
		 */
		//eventApi.setUserId(((EventSeekr)MainActivity.getInstance().getApplication()).getWcitiesId());
		eventApi.setAlreadyRequested(eventList.getEventsAlreadyRequested());
		eventApi.setAddFordLangParam(true);
		
		try {
			JSONObject jsonObject = eventApi.getFeaturedEventsForFord();
			EventApiJSONParser jsonParser = new EventApiJSONParser();
			
			ItemsList<Event> eventsList = jsonParser.getEventItemList(jsonObject, GetEventsFrom.FEATURED_EVENTS);
			tmpEvents = eventsList.getItems();
			//totalNoOfEvents = (eventsList.getTotalCount() > MAX_EVENTS) ? MAX_EVENTS : eventsList.getTotalCount();
			
			eventList.setRequestCode(GetEventsFrom.FEATURED_EVENTS);
			eventList.addAll(tmpEvents);
			eventList.setTotalNoOfEvents(eventsList.getTotalCount());
			/*if (eventList.size() >= MAX_EVENTS) {
				eventList.setMoreDataAvailable(false);
			}*/
			
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
			
		} catch (JSONException e) {
			e.printStackTrace();
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
    	double[] latLon = AppLinkService.getInstance().getLatLng();
    	lat = latLon[0];
    	lon = latLon[1];
    }
	
	/**
	 * reset the fields to default if Discover screen is being launched from the
	 * Discover screen only.
	 * @param cmd
	 */
	private void reset() {
		eventList.resetEventList();
		selectedCategoryId = 0;
		miles = MIN_MILES;
		//evtsLimit = EVENTS_LIMIT_10;
		eventList.setLoadEventsListener(this);	
	}

	public void performOperationForCommand(Command cmd) {
		if (cmd == null) {
			return;
		}
		//Log.d(TAG, "performOperationForCommand : " + cmd.name());
		
		switch (cmd) {
			case DISCOVER:
			case MY_EVENTS:
			case SEARCH:
				reset();
				AppLinkService.getInstance().initiateESIProxyListener(cmd);
				break;
				
			case NEXT:
				try {
					EventALUtil.onNextCommand(eventList, context);
					
				} catch (IOException e) {
					e.printStackTrace();
					AppLinkService.getInstance().handleNoNetConnectivity();
				}
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
				EventALUtil.callVenue(eventList);
				break;
			default:
				Log.d(TAG, cmd + " is an Invalid Command");
				break;
			
		}
	}

	@Override
	public void loadEvents() throws IOException {
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