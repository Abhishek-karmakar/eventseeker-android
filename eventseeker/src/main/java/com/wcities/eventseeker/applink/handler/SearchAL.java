package com.wcities.eventseeker.applink.handler;

import android.content.res.Resources;
import android.util.Log;

import com.ford.syncV4.exception.SyncException;
import com.ford.syncV4.proxy.TTSChunkFactory;
import com.ford.syncV4.proxy.rpc.OnAudioPassThru;
import com.ford.syncV4.proxy.rpc.PerformAudioPassThruResponse;
import com.ford.syncV4.proxy.rpc.PerformInteractionResponse;
import com.ford.syncV4.proxy.rpc.SoftButton;
import com.ford.syncV4.proxy.rpc.TTSChunk;
import com.ford.syncV4.proxy.rpc.enums.AudioType;
import com.ford.syncV4.proxy.rpc.enums.BitsPerSample;
import com.ford.syncV4.proxy.rpc.enums.SamplingRate;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.ArtistApi;
import com.wcities.eventseeker.api.ArtistApi.Method;
import com.wcities.eventseeker.api.EventApi;
import com.wcities.eventseeker.api.EventApi.MoreInfo;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingItemType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.applink.api.NuanceApi;
import com.wcities.eventseeker.applink.core.ArtistList;
import com.wcities.eventseeker.applink.core.EventList;
import com.wcities.eventseeker.applink.core.EventList.GetEventsFrom;
import com.wcities.eventseeker.applink.service.AppLinkService;
import com.wcities.eventseeker.applink.util.ALUtil;
import com.wcities.eventseeker.applink.util.CommandsUtil;
import com.wcities.eventseeker.applink.util.CommandsUtil.Command;
import com.wcities.eventseeker.applink.util.EventALUtil;
import com.wcities.eventseeker.applink.util.InteractionChoiceSetUtil.ChoiceSet;
import com.wcities.eventseeker.asynctask.UserTracker;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.Artist.Attending;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.ItemsList;
import com.wcities.eventseeker.jsonparser.ArtistApiJSONParser;
import com.wcities.eventseeker.jsonparser.EventApiJSONParser;
import com.wcities.eventseeker.util.ConversionUtil;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.List;
import java.util.Vector;

public class SearchAL extends ESIProxyALM {

	private static final String TAG = SearchAL.class.getSimpleName();
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
	private ByteArrayOutputStream audioDataOutputStream;
	private String query;
	
	public SearchAL(EventSeekr context) {
		this.context = context;
		eventList = new EventList();
		eventList.setEventsLimit(EVENTS_LIMIT);
		artistList = new ArtistList();
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
		//Log.d(TAG, "onStartInstance()");
		reset();
		performInteraction();		
	}
	
	private void performInteraction() {
		//Log.d(TAG, "performInteraction()");
		//Log.d(TAG, "Searchordinal : " + CHOICE_SET_ID_SEARCH);
		
		Vector<Integer> interactionChoiceSetIDList = new Vector<Integer>();
		interactionChoiceSetIDList.add(ChoiceSet.SEARCH.ordinal());
		
		String simple = AppLinkService.getInstance().getString(R.string.search_events_or_artists);
		String initialText = context.getResources().getString(R.string.search_for);		
		
		Vector<TTSChunk> initChunks = TTSChunkFactory.createSimpleTTSChunks(simple);
		Vector<TTSChunk> timeoutChunks = TTSChunkFactory.createSimpleTTSChunks(
				context.getResources().getString(R.string.time_out));
		
		ALUtil.performInteractionChoiceSet(initChunks, initialText, interactionChoiceSetIDList, timeoutChunks,
				getArguments().getBoolean(BundleKeys.MANUAL_IO_ONLY));
	}
	
	private void addCommands() {
		Vector<Command> requiredCmds = new Vector<Command>();
		requiredCmds.add(Command.DISCOVER);
		requiredCmds.add(Command.MY_EVENTS);
		requiredCmds.add(Command.SEARCH);
		requiredCmds.add(Command.NEXT);
		requiredCmds.add(Command.BACK);
		requiredCmds.add(Command.DETAILS);
		if (selectedCategoryId == SearchCategories.SEARCH_EVENT.ordinal()) {
			requiredCmds.add(Command.CALL_VENUE);
		} else {
			requiredCmds.add(Command.FOLLOW);			
		}
		//requiredCmds.add(Commands.PLAY);
		requiredCmds.add(Command.CALL_VENUE);
		
		CommandsUtil.addCommands(requiredCmds);
	}
	
	private void addCommandsMain() {
		Vector<Command> reqCmds = new Vector<Command>();
		reqCmds.add(Command.DISCOVER);
		reqCmds.add(Command.MY_EVENTS);
		reqCmds.add(Command.SEARCH);
		CommandsUtil.addCommands(reqCmds);
	}
	
	private void loadSearchedEvent() throws IOException {
		double[] latLon = AppLinkService.getInstance().getLatLng();
		
		Calendar c = Calendar.getInstance();
		String startDate = ConversionUtil.getDay(c);
		c.add(Calendar.YEAR, 1);
		String endDate = ConversionUtil.getDay(c);
		
		List<Event> tmpEvents = null;
		int eventsAlreadyRequested = eventList.getEventsAlreadyRequested();
		int totalNoOfEvents = 0;
		
		EventApi eventApi = new EventApi(Api.OAUTH_TOKEN_FORD_APP, latLon[0], latLon[1]);
		eventApi.setLimit(EVENTS_LIMIT);
		eventApi.setAlreadyRequested(eventsAlreadyRequested);
		eventApi.setUserId(context.getWcitiesId());//it can also be null
		eventApi.setStart(startDate);
		eventApi.setEnd(endDate);
		eventApi.setMiles(MILES_LIMIT);
		eventApi.addMoreInfo(MoreInfo.booking);
		eventApi.addMoreInfo(MoreInfo.multiplebooking);
		eventApi.setAddFordLangParam(true);
		
		try {
			if (query != null) {
				eventApi.setSearchFor(URLEncoder.encode(query, AppConstants.CHARSET_NAME));
			}
			
			JSONObject jsonObject = eventApi.getEvents();
			EventApiJSONParser jsonParser = new EventApiJSONParser();
			
			ItemsList<Event> eventsList = jsonParser.getEventItemList(jsonObject, GetEventsFrom.SEARCH_EVENTS);
			tmpEvents = eventsList.getItems();
			totalNoOfEvents = eventsList.getTotalCount();
			
			eventList.setRequestCode(GetEventsFrom.SEARCH_EVENTS);
			eventList.addAll(tmpEvents);
			eventList.setTotalNoOfEvents((totalNoOfEvents < 11) ? totalNoOfEvents : 10);

		} catch (ClientProtocolException e) {
			e.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	private void loadSearchedArtist() throws IOException {
		ArtistApi artistApi = new ArtistApi(Api.OAUTH_TOKEN_FORD_APP);
		artistApi.setLimit(ARTISTS_LIMIT);
		artistApi.setMethod(Method.artistSearch);
		artistApi.setAddFordLangParam(true);
		artistApi.setUserId(((EventSeekr) context).getWcitiesId());
		
		try {
			artistApi.setArtist(URLEncoder.encode(query, AppConstants.CHARSET_NAME));

			JSONObject jsonObject = artistApi.getArtists();
			ArtistApiJSONParser jsonParser = new ArtistApiJSONParser();
			
			ItemsList<Artist> artistItemList = jsonParser.getArtistItemList(jsonObject);

			artistList.addAll(artistItemList.getItems());
			artistList.setTotalNoOfArtists(artistItemList.getTotalCount());

		} catch (ClientProtocolException e) {
			e.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
			
		} catch (JSONException e) {
			e.printStackTrace();
		}

	}
	
	@Override
	public void onPerformInteractionResponse(PerformInteractionResponse response) {
		//Log.d(TAG, "onPerformInteractionResponse(), response.getChoiceID() = " + response.getChoiceID());
		super.onPerformInteractionResponse(response);
		
		if (response == null || response.getChoiceID() == null) {
			/**
			* This will happen when on Choice menu user selects cancel button
			*/
			Log.i(TAG, "ChoiceID == null");
			return;
		}
		
		selectedCategoryId = SearchCategories.getSearchChoiceId(response.getChoiceID()).ordinal();
		//Log.d(TAG, "Category selected : " + selectedCategoryId);
		
		addCommands();
		Vector<SoftButton> softBtns = buildSoftButtons();
		ALUtil.displayMessage("", "", softBtns);
		
		initiateSearchProcess();
	}
	
	private Vector<SoftButton> buildSoftButtons() {
		Vector<SoftButton> softBtns = new Vector<SoftButton>();
		if (selectedCategoryId == SearchCategories.SEARCH_EVENT.ordinal()) {
			softBtns.add(Command.CALL_VENUE.buildSoftBtn());
			
		} else {
			softBtns.add(Command.FOLLOW.buildSoftBtn());			
		}
		return softBtns;
	}
	
	private Vector<SoftButton> buildSoftButtonsMain() {
		Vector<SoftButton> softBtns = new Vector<SoftButton>();
		softBtns.add(Command.DISCOVER.buildSoftBtn());
		softBtns.add(Command.MY_EVENTS.buildSoftBtn());
		softBtns.add(Command.SEARCH.buildSoftBtn());
		return softBtns;
	}

	private void initiateSearchProcess() {
		/**
		 * ask for the user i/p and then process the User's speech
		 */
		audioDataOutputStream = new ByteArrayOutputStream();
		
		try {
			int msgResId = R.string.say_event_name;
			int search_cat = R.string.search_event_text;
			if (selectedCategoryId == SearchCategories.SEARCH_ARTIST.ordinal()) {
				msgResId = R.string.say_artist_name;
				search_cat = R.string.search_artist_text;
			}
		
			AppLinkService.getInstance().getProxy().performaudiopassthru(
				context.getResources().getString(msgResId), context.getResources().getString(search_cat),
				context.getResources().getString(R.string.listening), SamplingRate._8KHZ, 6000, BitsPerSample._16_BIT, 
				AudioType.PCM, true, AppLinkService.getInstance().autoIncCorrId++);
			
			ALUtil.displayMessage(context.getResources().getString(R.string.processing), "");
			
		} catch (SyncException e) {
			e.printStackTrace();
		}
	}

	public void performOperationForCommand(Command cmd, boolean isTriggerSrcMenu) {
		if (cmd == null) {
			return;
		}
		//Log.d(TAG, "performOperationForCommand : " + cmd.name());
		
		switch (cmd) {
		
			case DISCOVER:
			case MY_EVENTS:
			case SEARCH:
				reset();
				AppLinkService.getInstance().initiateESIProxyListener(cmd, isTriggerSrcMenu, null);
				break;
				
			case NEXT:
				if (selectedCategoryId == SearchCategories.SEARCH_EVENT.ordinal()) {
					try {
						EventALUtil.onNextCommand(eventList, context);
						
					} catch (IOException e) {
						e.printStackTrace();
						AppLinkService.getInstance().handleNoNetConnectivity();
					}
					
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
					speakDetailsOfArtist(artistList.getCurrentArtist(), context);
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
					new UserTracker(Api.OAUTH_TOKEN_FORD_APP, context, UserTrackingItemType.artist, artist.getId()).execute();
					ALUtil.alert(artist.getName(), context.getResources().getString(R.string.followed), "", 
							artist.getName() + " " + context.getResources().getString(R.string.followed));
					
				} else {
					//display : text1 : artist.getName(), text2 : already_followed
					//speak : artist.getName() + " " + already_followed
					ALUtil.alert(artist.getName(), context.getResources().getString(R.string.already_followed), "", 
							artist.getName() + " " + context.getResources().getString(R.string.already_followed));
				} 
				break;
				
			default:
				//Log.d(TAG, cmd + " is an Invalid Command");
				break;
			
		}
	}
	
	public void onNextArtistCommand(ArtistList artistList, EventSeekr context) {
		if (artistList.moveToNextArtist()) {
			int total = artistList.getTotalNoOfArtists();
			ALUtil.displayMessage(artistList.getCurrentArtist().getName(), 
					(artistList.getCurrentArtistPosition() + 1) + "/" + ((total < 11) ? total : 10));
			speakArtistTitle(artistList.getCurrentArtist(), context);
			
		} else {
			Resources res = context.getResources();
			ALUtil.alert(res.getString(R.string.alert_no_artists_available), res.getString(
					R.string.no_artists_avail));
		}
	}

	public void onBackArtistCommand(ArtistList artistList,EventSeekr context) {
		if (artistList.moveToPreviousArtist()) {
			int total = artistList.getTotalNoOfArtists();
			ALUtil.displayMessage(artistList.getCurrentArtist().getName(), 
					(artistList.getCurrentArtistPosition() + 1) + "/" + ((total < 11) ? total : 10));
			speakArtistTitle(artistList.getCurrentArtist(), context);
			
		} else {
			Resources res = context.getResources();
			ALUtil.alert(res.getString(R.string.alert_no_artists_available), res.getString(
					R.string.no_artists_avail));
		}		
	}
	
	public static void speakArtistTitle(Artist artist, EventSeekr app) {
		String simple = "Okay, " + artist.getName();
		
		if (app.isFirstArtistTitleForFord()) {
			simple += app.getResources().getString(R.string.plz_press_nxt_or_bck);
			app.setFirstArtistTitleForFord(false);
		}
		
		//Log.d(TAG, "simple = " + simple);
		Vector<TTSChunk> ttsChunks = TTSChunkFactory.createSimpleTTSChunks(simple);
		ALUtil.speakText(ttsChunks);				
	}
	
	public static void speakDetailsOfArtist(Artist artist, EventSeekr app) {
		String desc = artist.getDescription();
		//Log.d(TAG, "desc = " + desc);
		if (desc == null) {
			ALUtil.speak(R.string.detail_not_available);
			return;
		}
		ALUtil.speak(desc);				
	}
	
	private void reset() {
		eventList.resetEventList(true);
		artistList.resetArtistList();
		selectedCategoryId = 0;
		query = null;
	}
	
	@Override
	public void onOnAudioPassThru(OnAudioPassThru notification) {
		super.onOnAudioPassThru(notification);
		try {
			byte[] tempBytes = notification.getAPTData();
			//Log.d(TAG, "tempBytes l = " + tempBytes.length);
			audioDataOutputStream.write(tempBytes);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onPerformAudioPassThruResponse(PerformAudioPassThruResponse arg0) {
		super.onPerformAudioPassThruResponse(arg0);
		
		switch (arg0.getResultCode()) {
		
		case ABORTED:
			ALUtil.alert(context.getResources().getString(R.string.cancelling), 
					context.getResources().getString(R.string.cancelling));
			AppLinkService.getInstance().initiateMainAL();
			return;
			
		case RETRY:
			initiateSearchProcess();
			return;

		default:
			break;
		}
		
		try {
			query = (new NuanceApi()).execute(audioDataOutputStream);
			//Log.i(TAG, "Response Text from Nuance API: " + query);
			if (query == null) {
				addCommandsMain();
				Vector<SoftButton> softBtns = buildSoftButtonsMain();
				ALUtil.displayMessage(context.getResources().getString(R.string.error), "", softBtns);
				//ALUtil.speak(R.string.nuance_error);
				ALUtil.alert(context.getResources().getString(R.string.error), 
					context.getResources().getString(R.string.nuance_error));
				//AppLinkService.getInstance().initiateMainAL();
				return;
			}
			
			ALUtil.alertText(context.getResources().getString(R.string.searching_for), query);
			//Log.d(TAG, "alert displayed");
			
			try {
				Thread.sleep(2000);
				
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			if (selectedCategoryId == SearchCategories.SEARCH_EVENT.ordinal()) {
				loadSearchedEvent();

				if (eventList.isEmpty()) {
					AppLinkService.getInstance().initiateMainAL();
				} 
				EventALUtil.onNextCommand(eventList, context);
				
			} else {
				loadSearchedArtist();

				if (artistList.isEmpty()) {
					AppLinkService.getInstance().initiateMainAL();
				}
				onNextArtistCommand(artistList, context);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			AppLinkService.getInstance().handleNoNetConnectivity();
		}
	}
}
