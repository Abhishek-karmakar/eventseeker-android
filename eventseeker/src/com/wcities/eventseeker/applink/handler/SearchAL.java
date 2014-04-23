package com.wcities.eventseeker.applink.handler;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Vector;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.ford.syncV4.proxy.TTSChunkFactory;
import com.ford.syncV4.proxy.rpc.Choice;
import com.ford.syncV4.proxy.rpc.OnButtonPress;
import com.ford.syncV4.proxy.rpc.OnCommand;
import com.ford.syncV4.proxy.rpc.PerformInteractionResponse;
import com.ford.syncV4.proxy.rpc.TTSChunk;
import com.ford.syncV4.proxy.rpc.enums.ButtonName;
import com.wcities.eventseeker.MainActivity;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.ArtistApi;
import com.wcities.eventseeker.api.ArtistApi.Method;
import com.wcities.eventseeker.api.EventApi;
import com.wcities.eventseeker.api.EventApi.MoreInfo;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingItemType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.applink.datastructure.ArtistList;
import com.wcities.eventseeker.applink.datastructure.EventList;
import com.wcities.eventseeker.applink.datastructure.EventList.GetEventsFrom;
import com.wcities.eventseeker.applink.service.AppLinkService;
import com.wcities.eventseeker.applink.util.ALUtil;
import com.wcities.eventseeker.applink.util.CommandsUtil;
import com.wcities.eventseeker.applink.util.CommandsUtil.Commands;
import com.wcities.eventseeker.applink.util.EventALUtil;
import com.wcities.eventseeker.asynctask.UserTracker;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.BookingInfo;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.ItemsList;
import com.wcities.eventseeker.core.Artist.Attending;
import com.wcities.eventseeker.jsonparser.ArtistApiJSONParser;
import com.wcities.eventseeker.jsonparser.EventApiJSONParser;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public class SearchAL extends ESIProxyALM {

	private static final String TAG = SearchAL.class.getName();
	private static final int CHOICE_SET_ID_SEARCH = 3;
	private static final int EVENTS_LIMIT = 10;
	private static final int MILES_LIMIT = 10000;
	private static final int ARTISTS_LIMIT = 10;
	
	public static enum SearchCategories {
		SEARCH_EVENT(R.string.search_event),
		SEARCH_ARTIST(R.string.search_artist);

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
	private ArtistList artistList;
	private int selectedCategoryId;
	private String query;
	
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
		//requiredCmds.add(Commands.PLAY);
		if (selectedCategoryId == SearchCategories.SEARCH_EVENT.ordinal()) {
			requiredCmds.add(Commands.CALL_VENUE);
		} else {
			requiredCmds.add(Commands.FOLLOW);			
		}
		CommandsUtil.addCommands(requiredCmds);
	}
	
	private void loadSearchedEvent() {
		double[] latLon = DeviceUtil.getLatLon((EventSeekr) MainActivity.getInstance().getApplication());
		
		Calendar c = Calendar.getInstance();
		String startDate = ConversionUtil.getDay(c);
		c.add(Calendar.YEAR, 1);
		String endDate = ConversionUtil.getDay(c);
		
		List<Event> tmpEvents = null;
		int eventsAlreadyRequested = eventList.getEventsAlreadyRequested();
		int totalNoOfEvents = 0;
		
		EventApi eventApi = new EventApi(Api.OAUTH_TOKEN, latLon[0], latLon[1]);
		eventApi.setLimit(EVENTS_LIMIT);
		eventApi.setAlreadyRequested(eventsAlreadyRequested);
		eventApi.setUserId(((EventSeekr) MainActivity.getInstance().getApplication()).getWcitiesId());//it can be null also
		eventApi.setStart(startDate);
		eventApi.setEnd(endDate);
		eventApi.setMiles(MILES_LIMIT);
		eventApi.addMoreInfo(MoreInfo.booking);
		eventApi.addMoreInfo(MoreInfo.multiplebooking);
		
		try {
			if (query != null) {
				eventApi.setSearchFor(URLEncoder.encode(query, AppConstants.CHARSET_NAME));
			}
			
			JSONObject jsonObject = eventApi.getEvents();
			EventApiJSONParser jsonParser = new EventApiJSONParser();
			
			ItemsList<Event> eventsList = jsonParser.getEventItemList(jsonObject, GetEventsFrom.SEARCH_EVENTS);
			tmpEvents = eventsList.getItems();
			totalNoOfEvents = eventsList.getTotalCount();

		} catch (ClientProtocolException e) {
			e.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
			
		} catch (JSONException e) {
			e.printStackTrace();
		}

		eventList.setRequestCode(GetEventsFrom.SEARCH_EVENTS);
		eventList.addAll(tmpEvents);
		/*****************************************************************************************
		 *TODO:verify if only 1st 10 result need to be shown or it should lazily load new events.*
		 *****************************************************************************************/
		eventList.setTotalNoOfEvents((totalNoOfEvents < 11) ? totalNoOfEvents : 10);
	}
	
	private void loadSearchedArtist() {
		ArtistApi artistApi = new ArtistApi(Api.OAUTH_TOKEN);
		artistApi.setLimit(ARTISTS_LIMIT);
		artistApi.setMethod(Method.artistSearch);

		try {
			artistApi.setArtist(URLEncoder.encode(query, AppConstants.CHARSET_NAME));

			JSONObject jsonObject = artistApi.getArtists();
			ArtistApiJSONParser jsonParser = new ArtistApiJSONParser();
			
			ItemsList<Artist> artistItemList = jsonParser.getArtistItemList(jsonObject);
			
			if (artistList == null) {
				artistList = new ArtistList();
			}
			artistList.addAll(artistItemList.getItems());
			artistList.setTotalNoOfArtists(artistItemList.getTotalCount());

		} catch (ClientProtocolException e) {
			e.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
			
		} catch (JSONException e) {
			e.printStackTrace();
		}

	}
	
	@Override
	public void onPerformInteractionResponse(PerformInteractionResponse response) {
		Log.d(TAG, "onPerformInteractionResponse(), response.getChoiceID() = " + response.getChoiceID());
		
		if (SearchCategories.getSearchChoiceId(response.getChoiceID()) == null) {
			//TODO: when Choice Id is invalid that is null
			Log.d(TAG, "SearchCategories.getSearchChoiceId(response.getChoiceID()) == null");
		}
		
		selectedCategoryId = SearchCategories.getSearchChoiceId(response.getChoiceID()).ordinal();
		Log.d(TAG, "Category selected : " + selectedCategoryId);
		
		addCommands();
		
		//show Welcome message when no events are available
		if (eventList.isEmpty()) {
			ALUtil.displayMessage(R.string.msg_welcome_to, R.string.msg_eventseeker);
		}
		
		int msgResId = R.string.say_event_name;
		if (selectedCategoryId == SearchCategories.SEARCH_ARTIST.ordinal()) {
			msgResId = R.string.say_artist_name;
		}

		recordUserInput();
		query = getTextFromNuanceApi();
		
		//After getting the text from nuance api and place a search call.
		
		//TODO:remove below line after the above implementation is completed
		query = "a";
		if (selectedCategoryId == SearchCategories.SEARCH_EVENT.ordinal()) {
			loadSearchedEvent();

			EventALUtil.onNextCommand(eventList, context);
			
			//show Welcome message when no events are available
			if (eventList.isEmpty()) {
				ALUtil.displayMessage(R.string.msg_welcome_to, R.string.msg_eventseeker);
			}
		} else {
			loadSearchedArtist();

			onNextArtistCommand(artistList, context);
			
			//show Welcome message when no events are available
			if (artistList.isEmpty()) {
				ALUtil.displayMessage(R.string.msg_welcome_to, R.string.msg_eventseeker);
			}
		}
	}

	private String getTextFromNuanceApi() {
		/**
		 * TODO:convert the user's input speech to the text form using Nuance api
		 */				
		return null;
	}

	private void recordUserInput() {
		/**
		 * TODO:ask for the user i/p and then process the User's speech
		 */
	}

	@Override
	public void onOnButtonPress(OnButtonPress notification) {
		Log.d(TAG, "onOnButtonPress");
		ButtonName btnName = notification.getButtonName();
		Commands cmd = Commands.getCommandByButtonName(btnName);
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
		performOperationForCommand(cmd);
	}
	
	@SuppressWarnings("unused")
	public void performOperationForCommand(Commands cmd) {
		if (cmd == null) {
			return;
		}
		Log.d(TAG, "performOperationForCommand : " + cmd.name());
		
		switch (cmd) {
			case DISCOVER:
			case MY_EVENTS:
			case SEARCH:
				reset();
				AppLinkService.getInstance().initiateESIProxyListener(cmd);
				break;
			case NEXT:
				if (selectedCategoryId == SearchCategories.SEARCH_EVENT.ordinal()) {
					EventALUtil.onNextCommand(eventList, context);
				} else {
					onNextArtistCommand(artistList, context);
				}
				break;
			case BACK:
				if (selectedCategoryId == SearchCategories.SEARCH_EVENT.ordinal()) {
					EventALUtil.onBackCommand(eventList, context);
				} else {
					onBackArtistCommand(artistList, context);					
				}
				break;
			case DETAILS:
				if (selectedCategoryId == SearchCategories.SEARCH_EVENT.ordinal()) {
					EventALUtil.speakDetailsOfEvent(eventList.getCurrentEvent(), context);
				} else {
				}
				break;
			/*case PLAY:
				break;*/
			case CALL_VENUE:
				EventALUtil.callVenue(eventList);
				break;
			case FOLLOW:
				/**********************************************************************************
				 * Here if User has already tracked the artist with other devices(mobile/Tab etc.)*
				 * Then also for the first time it will track the artist and next time onwards it * 
				 * will show the message artist already Tracked. This is because while Search     *
				 * artist call the Artist Tracked info isn't available. Hence, in Artist object   *
				 * the initial attending value will be 'Not Tracked', this can be resolved by     *
				 * parsing the response of Artist Tracking call each time and from that we can get*
				 * info whether this artist was already tracked or not.							  *
				 **********************************************************************************/
				Artist artist = artistList.getCurrentArtist();
				if (artist.getAttending() == Attending.NotTracked) {
					artist.updateAttending(Attending.Tracked, context);
					new UserTracker(context, UserTrackingItemType.artist, artist.getId()).execute();
				} else {
					//TODO:display & speak artist Already followed
					//display : text1 : artist.getName(), text2 : already_followed
					//speak : artist.getName() + " " + already_followed
				} 
				break;
			default:
				Log.d(TAG, cmd + " is an Invalid Command");
				break;
			
		}
	}
	
	public void onNextArtistCommand(ArtistList artistList, EventSeekr context) {
		if (artistList.moveToNextEvent()) {
			ALUtil.displayMessage(artistList.getCurrentArtist().getName(), 
					(artistList.getCurrentArtistPosition() + 1) + "/" + artistList.getTotalNoOfArtists());
			speakArtistTitle(artistList.getCurrentArtist(), context);
			
		} else {
			ALUtil.speak(R.string.no_artists_avail);
		}
	}

	public void onBackArtistCommand(ArtistList artistList,EventSeekr context) {
		if (artistList.moveToPreviousEvent()) {
			ALUtil.displayMessage(artistList.getCurrentArtist().getName(), 
					(artistList.getCurrentArtistPosition() + 1) + "/" + artistList.getTotalNoOfArtists());
			speakArtistTitle(artistList.getCurrentArtist(), context);
			
		} else {
			ALUtil.speak(R.string.no_artists_avail);
		}		
	}
	
	public static void speakArtistTitle(Artist artist, EventSeekr app) {
		String simple = "Okay, " + artist.getName();
		
		if (app.isFirstArtistTitleForFord()) {
			simple += app.getResources().getString(R.string.plz_press_nxt_or_bck);
			app.setFirstArtistTitleForFord(false);
		}
		
		Log.d(TAG, "simple = " + simple);
		Vector<TTSChunk> ttsChunks = TTSChunkFactory.createSimpleTTSChunks(simple);
		ALUtil.speakText(ttsChunks);				
	}
	
	public static void speakDetailsOfArtist(Artist artist, EventSeekr app) {}
	
	private void reset() {
		eventList.resetEventList();
		selectedCategoryId = 0;
		query = null;
	}

}
