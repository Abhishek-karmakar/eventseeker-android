package com.wcities.eventseeker.applink.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.ford.syncV4.exception.SyncException;
import com.ford.syncV4.proxy.IProxyListener;
import com.ford.syncV4.proxy.TTSChunkFactory;
import com.ford.syncV4.proxy.rpc.AddCommandResponse;
import com.ford.syncV4.proxy.rpc.AddSubMenuResponse;
import com.ford.syncV4.proxy.rpc.AlertResponse;
import com.ford.syncV4.proxy.rpc.ChangeRegistrationResponse;
import com.ford.syncV4.proxy.rpc.Choice;
import com.ford.syncV4.proxy.rpc.CreateInteractionChoiceSet;
import com.ford.syncV4.proxy.rpc.CreateInteractionChoiceSetResponse;
import com.ford.syncV4.proxy.rpc.DeleteCommandResponse;
import com.ford.syncV4.proxy.rpc.DeleteFileResponse;
import com.ford.syncV4.proxy.rpc.DeleteInteractionChoiceSetResponse;
import com.ford.syncV4.proxy.rpc.DeleteSubMenuResponse;
import com.ford.syncV4.proxy.rpc.DialNumberResponse;
import com.ford.syncV4.proxy.rpc.EncodedSyncPDataResponse;
import com.ford.syncV4.proxy.rpc.EndAudioPassThruResponse;
import com.ford.syncV4.proxy.rpc.GenericResponse;
import com.ford.syncV4.proxy.rpc.GetDTCsResponse;
import com.ford.syncV4.proxy.rpc.GetVehicleDataResponse;
import com.ford.syncV4.proxy.rpc.ListFilesResponse;
import com.ford.syncV4.proxy.rpc.OnAppInterfaceUnregistered;
import com.ford.syncV4.proxy.rpc.OnAudioPassThru;
import com.ford.syncV4.proxy.rpc.OnButtonEvent;
import com.ford.syncV4.proxy.rpc.OnButtonPress;
import com.ford.syncV4.proxy.rpc.OnCommand;
import com.ford.syncV4.proxy.rpc.OnDriverDistraction;
import com.ford.syncV4.proxy.rpc.OnEncodedSyncPData;
import com.ford.syncV4.proxy.rpc.OnHMIStatus;
import com.ford.syncV4.proxy.rpc.OnLanguageChange;
import com.ford.syncV4.proxy.rpc.OnPermissionsChange;
import com.ford.syncV4.proxy.rpc.OnVehicleData;
import com.ford.syncV4.proxy.rpc.PerformAudioPassThruResponse;
import com.ford.syncV4.proxy.rpc.PerformInteraction;
import com.ford.syncV4.proxy.rpc.PerformInteractionResponse;
import com.ford.syncV4.proxy.rpc.PutFileResponse;
import com.ford.syncV4.proxy.rpc.ReadDIDResponse;
import com.ford.syncV4.proxy.rpc.RegisterAppInterfaceResponse;
import com.ford.syncV4.proxy.rpc.ResetGlobalPropertiesResponse;
import com.ford.syncV4.proxy.rpc.ScrollableMessageResponse;
import com.ford.syncV4.proxy.rpc.SetAppIconResponse;
import com.ford.syncV4.proxy.rpc.SetDisplayLayoutResponse;
import com.ford.syncV4.proxy.rpc.SetGlobalPropertiesResponse;
import com.ford.syncV4.proxy.rpc.SetMediaClockTimerResponse;
import com.ford.syncV4.proxy.rpc.ShowResponse;
import com.ford.syncV4.proxy.rpc.SliderResponse;
import com.ford.syncV4.proxy.rpc.Speak;
import com.ford.syncV4.proxy.rpc.SpeakResponse;
import com.ford.syncV4.proxy.rpc.SubscribeButtonResponse;
import com.ford.syncV4.proxy.rpc.SubscribeVehicleDataResponse;
import com.ford.syncV4.proxy.rpc.TTSChunk;
import com.ford.syncV4.proxy.rpc.UnregisterAppInterfaceResponse;
import com.ford.syncV4.proxy.rpc.UnsubscribeButtonResponse;
import com.ford.syncV4.proxy.rpc.UnsubscribeVehicleDataResponse;
import com.ford.syncV4.proxy.rpc.enums.InteractionMode;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi;
import com.wcities.eventseeker.api.UserInfoApi.Type;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.applink.handler.MainActivityAL.CmdId;
import com.wcities.eventseeker.applink.service.AppLinkService;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.ItemsList;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.DeviceUtil;

public class MyEventsActivityAL implements IProxyListener {
	
	private static final String TAG = MyEventsActivityAL.class.getName();

	private static final int START_CHOICE_ID_WHEN = 1;
	private static final int START_CHOICE_ID_EVENT_ACTION = 11;
	private static final int START_CHOICE_ID_NEXT_BACK = 21;
	private static final int START_CHOICE_ID_RECOMMEND = 31;

	private static final int EVENTS_LIMIT = 10;
	
	private static MyEventsActivityAL instance;
	private EventSeekr mEventSeekr;
	
	private List<Event> currentEvtList;
	private int currentEvtPos;
	private WhenChoiceId whenChoiceId;
	private int eventsAlreadyRequested;
	private double lat, lon;
	private boolean isMoreDataAvailable = true;
	private SpeakStage speakStage;
	
	private enum ChoiceSetId {
		When(1),
		EventAction(2),
		NextBack(3),
		Recommend(4);
		
		private int value;
		
		private ChoiceSetId(int value) {
			this.value = value;
		}
		
		public static ChoiceSetId getChoiceSetId(int value) {
			ChoiceSetId[] ids = ChoiceSetId.values();
			for (int i = 0; i < ids.length; i++) {
				ChoiceSetId choiceSetId = ids[i];
				if (choiceSetId.value == value) {
					return choiceSetId;
				}
			}
			return null;
		}
	}
	
	private enum WhenChoiceId {
		Today(START_CHOICE_ID_WHEN),
		Tomorrow(START_CHOICE_ID_WHEN + 1),
		ThisWeekend(START_CHOICE_ID_WHEN + 2);
		
		private int value;
		
		private WhenChoiceId(int value) {
			this.value = value;
		}
		
		public static WhenChoiceId getWhenChoiceId(int value) {
			WhenChoiceId[] ids = WhenChoiceId.values();
			for (int i = 0; i < ids.length; i++) {
				WhenChoiceId whenChoiceId = ids[i];
				if (whenChoiceId.value == value) {
					return whenChoiceId;
				}
			}
			return null;
		}
		
		@Override
		public String toString() {
			if (this == ThisWeekend) {
				return "this weekend";
			}
			return super.toString();
		}
		
		public Date getStartDate() {
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			
			switch (this) {
			
			case Today:
				break;
				
			case Tomorrow:
				cal.add(Calendar.DATE, 1);
				break;
				
			case ThisWeekend:
				cal.setFirstDayOfWeek(Calendar.MONDAY);
				cal.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
				break;

			default:
				break;
			}
			
			return cal.getTime();
		}
		
		public Date getEndDate() {
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.HOUR_OF_DAY, 23);
			cal.set(Calendar.MINUTE, 59);
			cal.set(Calendar.SECOND, 59);
			cal.set(Calendar.MILLISECOND, 999);
			
			switch (this) {
			
			case Today:
				break;
				
			case Tomorrow:
				cal.add(Calendar.DATE, 1);
				break;
				
			case ThisWeekend:
				cal.setFirstDayOfWeek(Calendar.MONDAY);
				cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
				break;

			default:
				break;
			}
			
			return cal.getTime();
		}
	}
	
	private enum EventActionChoiceId {
		Share(START_CHOICE_ID_EVENT_ACTION),
		More(START_CHOICE_ID_EVENT_ACTION + 1);
		
		private int value;

		private EventActionChoiceId(int value) {
			this.value = value;
		}
	}
	
	private enum NextBackChoiceId {
		Next(START_CHOICE_ID_NEXT_BACK),
		Back(START_CHOICE_ID_NEXT_BACK + 1);
		
		private int value;

		private NextBackChoiceId(int value) {
			this.value = value;
		}
	}
	
	private enum RecommendChoiceId {
		Yes(START_CHOICE_ID_RECOMMEND),
		No(START_CHOICE_ID_RECOMMEND + 1);
		
		private int value;

		private RecommendChoiceId(int value) {
			this.value = value;
		}
	}
	
	public static MyEventsActivityAL getInstance(EventSeekr eventSeekr) {
		if (instance == null) {
			Log.i(TAG, "instance is null");
			instance = new MyEventsActivityAL(eventSeekr);
		}
		Log.i(TAG, "return instance");
		return instance;
	}
	
	private MyEventsActivityAL(EventSeekr eventSeekr) {
		mEventSeekr = eventSeekr;
		currentEvtList = new ArrayList<Event>();
	}
	
	public void onCreateInstance() {
		Log.i(TAG, "initiateInterAction()");
		initializeInteractionChoiceSets();
		speakStage = new Init();
	}
	
	private void initializeInteractionChoiceSets() {
		ChoiceSetId[] choiceSetIds = ChoiceSetId.values();
		for (int i = 0; i < choiceSetIds.length; i++) {
			ChoiceSetId choiceSetId = choiceSetIds[i];
			createInteractionChoiceSet(choiceSetId);
		}
	}
	
	private void createInteractionChoiceSet(ChoiceSetId choiceSetId) {
		Vector<Choice> commands = new Vector<Choice>();
		
		switch (choiceSetId) {
		
		case When:
			WhenChoiceId[] whenChoiceIds = WhenChoiceId.values();
			for (int i = 0; i < whenChoiceIds.length; i++) {
				WhenChoiceId whenChoiceId = whenChoiceIds[i];
				Choice choice = new Choice();
				choice.setChoiceID(whenChoiceId.value);
				choice.setMenuName(whenChoiceId.name());
				choice.setVrCommands(new Vector<String>(Arrays.asList(new String[] {whenChoiceId.toString()})));
				commands.add(choice);
			}
			break;
			
		case EventAction:
			EventActionChoiceId[] eventActionChoiceIds = EventActionChoiceId.values();
			for (int i = 0; i < eventActionChoiceIds.length; i++) {
				EventActionChoiceId eventActionChoiceId = eventActionChoiceIds[i];
				Choice choice = new Choice();
				choice.setChoiceID(eventActionChoiceId.value);
				choice.setMenuName(eventActionChoiceId.name());
				choice.setVrCommands(new Vector<String>(Arrays.asList(new String[] {eventActionChoiceId.name()})));
				commands.add(choice);
			}
			break;
			
		case NextBack:
			NextBackChoiceId[] nextBackChoiceIds = NextBackChoiceId.values();
			for (int i = 0; i < nextBackChoiceIds.length; i++) {
				NextBackChoiceId nextBackChoiceId = nextBackChoiceIds[i];
				Choice choice = new Choice();
				choice.setChoiceID(nextBackChoiceId.value);
				choice.setMenuName(nextBackChoiceId.name());
				choice.setVrCommands(new Vector<String>(Arrays.asList(new String[] {nextBackChoiceId.name()})));
				commands.add(choice);
			}
			break;
			
		case Recommend:
			RecommendChoiceId[] recommendChoiceIds = RecommendChoiceId.values();
			for (int i = 0; i < recommendChoiceIds.length; i++) {
				RecommendChoiceId recommendChoiceId = recommendChoiceIds[i];
				Choice choice = new Choice();
				choice.setChoiceID(recommendChoiceId.value);
				choice.setMenuName(recommendChoiceId.name());
				choice.setVrCommands(new Vector<String>(Arrays.asList(new String[] {recommendChoiceId.name()})));
				commands.add(choice);
			}
			break;
			
		default:
			break;
		}
		
		if (!commands.isEmpty()) {
			CreateInteractionChoiceSet msg = new CreateInteractionChoiceSet();
			msg.setCorrelationID(AppLinkService.getInstance().autoIncCorrId++);
			msg.setInteractionChoiceSetID(choiceSetId.value);
			msg.setChoiceSet(commands);
			
			try {
				AppLinkService.getInstance().getProxy().sendRPCRequest(msg);
				
			} catch (SyncException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void performInteraction(ChoiceSetId choiceSetId) {
		Log.i(TAG, "performInteraction(), choiceSetId = " + choiceSetId);
		PerformInteraction msg = new PerformInteraction();
		msg.setCorrelationID(AppLinkService.getInstance().autoIncCorrId++);
		
		Vector<Integer> interactionChoiceSetIDs = new Vector<Integer>();
		interactionChoiceSetIDs.add(choiceSetId.value);
		
		String initialText = null, simple = null;
		switch (choiceSetId) {
		
		case When:
			simple = "Okay, today, tomorrow or this weekend?";
			initialText = "My Events";
			break;
			
		case EventAction:
			simple = "Share or More";
			initialText = currentEvtList.get(currentEvtPos).getName();
			break;
			
		case NextBack:
			simple = "Next or Back";
			initialText = currentEvtList.get(currentEvtPos).getName();
			break;
			
		case Recommend:
			simple = "Yes or No";
			initialText = "Recommend event?";
			break;
			
		default:
			break;
		}
		
		Vector<TTSChunk> initChunks = TTSChunkFactory.createSimpleTTSChunks(simple);
		Vector<TTSChunk> timeoutChunks = TTSChunkFactory.createSimpleTTSChunks("time out");
		msg.setInitialPrompt(initChunks);
		msg.setInitialText(initialText);
		msg.setInteractionChoiceSetIDList(interactionChoiceSetIDs);
		msg.setInteractionMode(InteractionMode.VR_ONLY);
		msg.setTimeout(5000);
		msg.setTimeoutPrompt(timeoutChunks);
		
		try {
			AppLinkService.getInstance().getProxy().sendRPCRequest(msg);
			Log.i(TAG, "PerformInteraction sendRPCRequest() done");
			
		} catch (SyncException e) {
			e.printStackTrace();
		}
	}
	
	private UserInfoApi buildUserInfoApi() {
		UserInfoApi userInfoApi = new UserInfoApi(Api.OAUTH_TOKEN);
		userInfoApi.setLimit(EVENTS_LIMIT);
		userInfoApi.setAlreadyRequested(eventsAlreadyRequested);
		userInfoApi.setUserId(mEventSeekr.getWcitiesId());
		userInfoApi.setLat(lat);
		userInfoApi.setLon(lon);
		return userInfoApi;
	}
	
	private void loadMyEvents() {
		Log.i(TAG, "loadMyEvents(), eventsAlreadyRequested = " + eventsAlreadyRequested);
		List<Event> tmpEvents = null;

		UserInfoApi userInfoApi = buildUserInfoApi();

		try {
			// Here getMyProfileInfoFor() returns time sorted event list in ascending order.
			JSONObject jsonObject = userInfoApi.getMyProfileInfoFor(Type.myevents);
			UserInfoApiJSONParser jsonParser = new UserInfoApiJSONParser();
			
			ItemsList<Event> myEventsList = jsonParser.getEventList(jsonObject);
			tmpEvents = myEventsList.getItems();
			
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		if (tmpEvents != null && !tmpEvents.isEmpty()) {
			Date startDate = whenChoiceId.getStartDate();
			Date endDate = whenChoiceId.getEndDate();
			int noOfEventsAdded = filterAndAddMyEvents(tmpEvents, startDate, endDate);
			Log.i(TAG, "noOfEventsAdded = " + noOfEventsAdded);
			eventsAlreadyRequested += tmpEvents.size();
			
			/**
			 * since getMyProfileInfoFor() returns time sorted event list in ascending order, we can use second condition
			 * here to prevent redundant api calls based on last fetched event time.
			 */
			if (tmpEvents.size() < EVENTS_LIMIT || 
					tmpEvents.get(tmpEvents.size() - 1).getSchedule().getDates().get(0).getStartDate().compareTo(endDate) > 0) {
				Log.i(TAG, "set isMoreDataAvailable = false, tmpEvents.size() = " + tmpEvents.size());
				isMoreDataAvailable = false;
				
			} else if (noOfEventsAdded == 0) {
				Log.i(TAG, "no event found on filtering the result, so recursively load More Events");
				loadMyEvents();
			}
			
		} else {
			isMoreDataAvailable = false;
		}
	}
	
	private void loadRecommendedEvents() {
		Log.i(TAG, "loadRecommendedEvents(), eventsAlreadyRequested = " + eventsAlreadyRequested);
		List<Event> tmpEvents = null;
		/*lat = 37.7771199960262;
		lon = -122.419640002772;*/
		UserInfoApi userInfoApi = buildUserInfoApi();

		try {
			JSONObject jsonObject = userInfoApi.getMyProfileInfoFor(Type.recommendedevent);
			UserInfoApiJSONParser jsonParser = new UserInfoApiJSONParser();
			
			ItemsList<Event> recommendedEventsList = jsonParser.getRecommendedEventList(jsonObject);
			tmpEvents = recommendedEventsList.getItems();
			
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		if (tmpEvents != null && !tmpEvents.isEmpty()) {
			currentEvtList.addAll(tmpEvents);
			eventsAlreadyRequested += tmpEvents.size();
			
			if (tmpEvents.size() < EVENTS_LIMIT) {
				Log.i(TAG, "set isMoreDataAvailable = false, tmpEvents.size() = " + tmpEvents.size());
				isMoreDataAvailable = false;
			} 
			
		} else {
			isMoreDataAvailable = false;
		}
	}
	
	private int filterAndAddMyEvents(List<Event> tmpEvents, Date startDate, Date endDate) {
		int noOfEventsAdded = 0;
		
		for (Iterator<Event> iterator = tmpEvents.iterator(); iterator.hasNext();) {
			Event event = iterator.next();
			Date evtDate = event.getSchedule().getDates().get(0).getStartDate();
			Log.i(TAG, "evtDate = " + evtDate.toString());
			if (evtDate.compareTo(startDate) < 0 || evtDate.compareTo(endDate) > 0) {
				Log.i(TAG, "filter out");
				continue;
			}
			currentEvtList.add(event);
			noOfEventsAdded++;
		}
		return noOfEventsAdded;
	}
	
	private void resetLoadingParams() {
		currentEvtPos = 0;
		isMoreDataAvailable = true;
		eventsAlreadyRequested = 0;
		currentEvtList.clear();
		generateLatLon();
	}
	
	private void generateLatLon() {
    	double[] latLon = DeviceUtil.getLatLon(mEventSeekr);
		//double[] latLon = new double[] {49.28766, -123.12361};
    	lat = latLon[0];
    	lon = latLon[1];
    	Log.i(TAG, "lat = " + lat + ", lon = " + lon);
    }
	
	private boolean areMoreEventDetailsAvailable() {
		Event event = currentEvtList.get(currentEvtPos);
		if ((event.getArtists() != null && !event.getArtists().isEmpty()) 
				|| (event.getSchedule() != null && !event.getSchedule().getPriceRange().isEmpty())) {
			return true;
		}
		return false;
	}
	
	private boolean hasMoreEvents() {
		if (currentEvtPos + 1 < currentEvtList.size()) {
			return true;
			
		} else if (isMoreDataAvailable) {
			speakStage.loadEvents();
			
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
	
	private void handleNextBack(PerformInteractionResponse response) {
		if (response.getChoiceID() == NextBackChoiceId.Next.value) {
			Log.i(TAG, "next");
			if (hasMoreEvents()) {
				currentEvtPos++;
				
			} else {
				currentEvtPos = 0;
			}
			
		} else {
			Log.i(TAG, "prev");
			if (hasPreviousEvents()) {
				currentEvtPos--;
				
			} else {
				currentEvtPos = currentEvtList.size() - 1;
			}
		}
		Log.i(TAG, "currentEvtPos = " + currentEvtPos);
		
		/**
		 * speakStage here can be BasicEventInfo, MoreEventInfo or RecommendedEventInfo. 
		 * To speak for next or previous event, its state must be BasicEventInfo or RecommendedEventInfo.
		 */
		if (speakStage instanceof MoreEventInfo) {
			speakStage = new BasicEventInfo();
		}
		speakStage.speak();
	}
	
	private interface ISpeak {
		public void speak();
		public void onSpeakResponse(SpeakResponse response);
	}
	
	private interface IPerformInteraction {
		public void performInteraction();
		public void onPerformInteractionResponse(PerformInteractionResponse response);
	}
	
	private abstract class SpeakStage implements ISpeak, IPerformInteraction {
		
		protected static final String PERFORM_INTERACTION_PROMPT_SHARE_MORE = "Share or More";
		protected static final String PERFORM_INTERACTION_PROMPT_NEXT_BACK = "Next or Back";
		protected static final String PERFORM_INTERACTION_PROMPT_YES_NO = "Yes or No";
		
		@Override
		public void onSpeakResponse(SpeakResponse response) {
			Log.d(TAG, "SpeakStage onSpeakResponse()");
			performInteraction();
		}
		
		protected void sendSpeakMsg(String simple) {
			Log.d(TAG, "SpeakStage sendSpeakMsg()");
			Vector<TTSChunk> chunks = TTSChunkFactory.createSimpleTTSChunks(simple);
			Speak msg = new Speak();
			msg.setCorrelationID(AppLinkService.getInstance().autoIncCorrId++);
			msg.setTtsChunks(chunks);
			
			try {
				AppLinkService.getInstance().getProxy().sendRPCRequest(msg);
				
			} catch (SyncException e) {
				e.printStackTrace();
			}
		}
		
		protected boolean loadEvents() {
			Log.d(TAG, "SpeakStage loadEvents()");
			return false; 
		}
		
		protected void sendPerformInteractionMsg(int interactionChoiceSetId, String simple, String initialText) {
			Log.d(TAG, "sendPerformInteractionMsg(), choiceSetId = " + interactionChoiceSetId);
			PerformInteraction msg = new PerformInteraction();
			msg.setCorrelationID(AppLinkService.getInstance().autoIncCorrId++);
			
			Vector<Integer> interactionChoiceSetIDs = new Vector<Integer>();
			interactionChoiceSetIDs.add(interactionChoiceSetId);
			
			Vector<TTSChunk> initChunks = TTSChunkFactory.createSimpleTTSChunks(simple);
			Vector<TTSChunk> timeoutChunks = TTSChunkFactory.createSimpleTTSChunks("time out");
			msg.setInitialPrompt(initChunks);
			msg.setInitialText(initialText);
			msg.setInteractionChoiceSetIDList(interactionChoiceSetIDs);
			msg.setInteractionMode(InteractionMode.VR_ONLY);
			msg.setTimeout(5000);
			msg.setTimeoutPrompt(timeoutChunks);
			
			try {
				AppLinkService.getInstance().getProxy().sendRPCRequest(msg);
				Log.d(TAG, "PerformInteraction sendRPCRequest() done");
				
			} catch (SyncException e) {
				e.printStackTrace();
			}
		}
	}
	
	private class Init extends SpeakStage {
		
		public Init() {
			super();
			Log.d(TAG, "Init()");
			performInteraction();
		}

		@Override
		public void speak() {
			Log.d(TAG, "Init speak()");
		}

		@Override
		public void performInteraction() {
			Log.d(TAG, "Init performInteraction()");
			sendPerformInteractionMsg(ChoiceSetId.When.value, "Okay, today, tomorrow or this weekend?", 
					"My Events");
		}

		@Override
		public void onPerformInteractionResponse(PerformInteractionResponse response) {
			Log.d(TAG, "Init onPerformInteractionResponse()");
			resetLoadingParams();
			whenChoiceId = WhenChoiceId.getWhenChoiceId(response.getChoiceID());
			loadMyEvents();
			
			if (currentEvtList.isEmpty()) {
				speakStage = new NoMyEvents();
				
			} else {
				speakStage = new BasicEventInfo();
			}
			speakStage.speak();
		}
	}
	
	private class BasicEventInfo extends SpeakStage {

		@Override
		public void speak() {
			Log.d(TAG, "BasicEventInfo speak()");
			Event event = currentEvtList.get(currentEvtPos);
			String simple = "Okay, " + whenChoiceId.toString() + " " + event.getName();
			
			if (event.getSchedule() != null) {
				/*simple += " is at " + event.getSchedule().getVenue().getName() + " on " 
						+ ConversionUtil.getDateTime(event.getSchedule().getDates().get(0));*/
			}
			
			Log.d(TAG, "simple = " + simple);
			
			sendSpeakMsg(simple);
		}

		@Override
		protected boolean loadEvents() {
			Log.d(TAG, "BasicEventInfo loadEvents()");
			loadMyEvents();
			return true;
		}

		@Override
		public void performInteraction() {
			Log.d(TAG, "BasicEventInfo performInteraction()");
			int interactionChoiceSetId;
			String simple, initialText;
			if (areMoreEventDetailsAvailable()) {
				interactionChoiceSetId = ChoiceSetId.EventAction.value;
				simple = PERFORM_INTERACTION_PROMPT_SHARE_MORE;
				initialText = currentEvtList.get(currentEvtPos).getName();
				
			} else {
				interactionChoiceSetId = ChoiceSetId.NextBack.value;
				simple = PERFORM_INTERACTION_PROMPT_NEXT_BACK;
				initialText = currentEvtList.get(currentEvtPos).getName();
			}
			
			sendPerformInteractionMsg(interactionChoiceSetId, simple, initialText);
		}

		@Override
		public void onPerformInteractionResponse(PerformInteractionResponse response) {
			Log.d(TAG, "BasicEventInfo onPerformInteractionResponse()");
			if (response.getChoiceID() == EventActionChoiceId.More.value) {
				speakStage = new MoreEventInfo();
				speakStage.speak();
				
			} else if (response.getChoiceID() == EventActionChoiceId.Share.value) {
				Log.d(TAG, "onPerformInteractionResponse() - share");
				
			} else {
				handleNextBack(response);
			}
		}
	}
	
	private class MoreEventInfo extends SpeakStage {

		@Override
		public void speak() {
			Log.d(TAG, "MoreEventInfo speak()");
			Event event = currentEvtList.get(currentEvtPos);
			String simple = "", price = "";
			
			if (event.getSchedule() != null) {
				List<Float> priceRange = event.getSchedule().getPriceRange();
				if (!priceRange.isEmpty()) {
					price += "Price would be " + priceRange.get(0);

					if (priceRange.size() == 2) {
						price += "-" + priceRange.get(1);
					}
					
					price += " " + event.getSchedule().getBookingInfos().get(0).getFullCurrencyString();
				}
			}
			
			if (event.getArtists() != null && !event.getArtists().isEmpty()) {
				String appendForLimitedArtists = " & few more";
				String postfix = "";
				if (event.getArtists().size() > 1) {
					postfix += " are ";
					
				} else {
					postfix += " is ";
				}
				postfix += "performing for this event, ";
				int reservedLength = appendForLimitedArtists.length() + postfix.length();
				
				for (Iterator<Artist> iterator = event.getArtists().iterator(); iterator.hasNext();) {
					Artist artist = iterator.next();
					if (simple.length() + artist.getName().length() + reservedLength > AppLinkService.SPEAK_CHAR_LIMIT) {
						simple += appendForLimitedArtists;
						break;
					}
					simple += artist.getName() + ",";
				}
				simple = simple.substring(0, simple.length() - 1);
				simple += postfix;
			}
			
			simple += price;
			Log.d(TAG, "simple = " + simple);
			
			sendSpeakMsg(simple);
		}

		@Override
		protected boolean loadEvents() {
			Log.d(TAG, "MoreEventInfo loadEvents()");
			loadMyEvents();
			return true;
		}
		
		@Override
		public void performInteraction() {
			Log.d(TAG, "MoreEventInfo performInteraction()");
			sendPerformInteractionMsg(ChoiceSetId.NextBack.value, PERFORM_INTERACTION_PROMPT_NEXT_BACK, 
					currentEvtList.get(currentEvtPos).getName());
		}

		@Override
		public void onPerformInteractionResponse(PerformInteractionResponse response) {
			Log.d(TAG, "MoreEventInfo onPerformInteractionResponse()");
			handleNextBack(response);
		}
	}
	
	private class NoMyEvents extends SpeakStage {

		@Override
		public void speak() {
			Log.d(TAG, "NoMyEvents speak()");
			String simple = "You have no events " + whenChoiceId.toString() + " - Can we recommend an event?";
			super.sendSpeakMsg(simple);
		}

		@Override
		public void performInteraction() {
			Log.d(TAG, "NoMyEvents performInteraction()");
			sendPerformInteractionMsg(ChoiceSetId.Recommend.value, PERFORM_INTERACTION_PROMPT_YES_NO, 
					"Recommend event?");
		}

		@Override
		public void onPerformInteractionResponse(PerformInteractionResponse response) {
			Log.d(TAG, "NoMyEvents onPerformInteractionResponse()");
			if (response.getChoiceID() == RecommendChoiceId.Yes.value) {
				resetLoadingParams();
				loadRecommendedEvents();
				
				if (currentEvtList.isEmpty()) {
					speakStage = new NoRecommendedEvents();
					
				} else {
					speakStage = new RecommendedEventInfo();
				}
				
			} else {
				speakStage = new HowCanIHelp();
			}
			speakStage.speak();
		}
	}
	
	private class RecommendedEventInfo extends SpeakStage {

		@Override
		public void speak() {
			Log.d(TAG, "RecommendedEventInfo speak()");
			Event event = currentEvtList.get(currentEvtPos);
			
			String simple = "Okay, " + event.getName();
			String postfix = "", appendForLimitedArtists = " & few more";
			if (event.getSchedule() != null) {
				/*postfix = " at " + event.getSchedule().getVenue().getName() + " on " 
						+ ConversionUtil.getDateTime(event.getSchedule().getDates().get(0));*/
			}
			
			int reservedLength = postfix.length() + appendForLimitedArtists.length();
			if (event.getArtists() != null && !event.getArtists().isEmpty()) {
				simple += " with ";
				for (Iterator<Artist> iterator = event.getArtists().iterator(); iterator.hasNext();) {
					Artist artist = iterator.next();
					if (simple.length() + artist.getName().length() + reservedLength > AppLinkService.SPEAK_CHAR_LIMIT) {
						simple += appendForLimitedArtists;
						break;
					}
					simple += artist.getName() + ",";
				}
				simple = simple.substring(0, simple.length() - 1);
			}
			simple += postfix;
			
			Log.d(TAG, "simple = " + simple);
			sendSpeakMsg(simple);
		}

		@Override
		protected boolean loadEvents() {
			Log.d(TAG, "RecommendedEventInfo loadEvents()");
			loadRecommendedEvents();
			return true;
		}
		
		@Override
		public void performInteraction() {
			Log.d(TAG, "RecommendedEventInfo performInteraction()");
			sendPerformInteractionMsg(ChoiceSetId.NextBack.value, PERFORM_INTERACTION_PROMPT_NEXT_BACK, 
					currentEvtList.get(currentEvtPos).getName());
		}

		@Override
		public void onPerformInteractionResponse(PerformInteractionResponse response) {
			Log.d(TAG, "RecommendedEventInfo onPerformInteractionResponse()");
			handleNextBack(response);
		}
	}
	
	private class NoRecommendedEvents extends SpeakStage {

		@Override
		public void speak() {
			Log.d(TAG, "NoRecommendedEvents speak()");
			String simple = "No events found to recommend";
			sendSpeakMsg(simple);
		}

		@Override
		public void onSpeakResponse(SpeakResponse response) {
			Log.d(TAG, "NoRecommendedEvents onSpeakResponse()");
		}
		
		@Override
		public void performInteraction() {
			Log.d(TAG, "NoRecommendedEvents performInteraction()");
		}

		@Override
		public void onPerformInteractionResponse(PerformInteractionResponse response) {
			Log.d(TAG, "NoRecommendedEvents onPerformInteractionResponse()");
		}
	}
	
	private class HowCanIHelp extends SpeakStage {

		@Override
		public void speak() {
			Log.d(TAG, "HowCanIHelp speak()");
			String simple = "How can I help? ";
			CmdId[] cmdIds = CmdId.values();
			for (int i = 0; i < cmdIds.length; i++) {
				simple += cmdIds[i].toString() + ", ";
			}
			simple = simple.substring(0, simple.length() - 2);
			sendSpeakMsg(simple);
		}

		@Override
		public void onSpeakResponse(SpeakResponse response) {
			Log.d(TAG, "HowCanIHelp onSpeakResponse()");
		}
		
		@Override
		public void performInteraction() {
			Log.d(TAG, "HowCanIHelp performInteraction()");
		}

		@Override
		public void onPerformInteractionResponse(PerformInteractionResponse response) {
			Log.d(TAG, "HowCanIHelp onPerformInteractionResponse()");
		}
	}
	
	@Override
	public void onAddCommandResponse(AddCommandResponse arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onAddSubMenuResponse(AddSubMenuResponse arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onAlertResponse(AlertResponse arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onCreateInteractionChoiceSetResponse(
			CreateInteractionChoiceSetResponse arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDeleteCommandResponse(DeleteCommandResponse arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDeleteInteractionChoiceSetResponse(
			DeleteInteractionChoiceSetResponse arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDeleteSubMenuResponse(DeleteSubMenuResponse arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onError(String arg0, Exception arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onGenericResponse(GenericResponse arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onOnButtonEvent(OnButtonEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onOnButtonPress(OnButtonPress arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onOnCommand(OnCommand arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onOnHMIStatus(OnHMIStatus arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onOnPermissionsChange(OnPermissionsChange arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPerformInteractionResponse(PerformInteractionResponse response) {
		Log.i(TAG, "onPerformInteractionResponse(), response.getChoiceID() = " + response.getChoiceID());
		speakStage.onPerformInteractionResponse(response);
	}

	@Override
	public void onProxyClosed(String arg0, Exception arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onResetGlobalPropertiesResponse(
			ResetGlobalPropertiesResponse arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSetGlobalPropertiesResponse(SetGlobalPropertiesResponse arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSetMediaClockTimerResponse(SetMediaClockTimerResponse arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onShowResponse(ShowResponse arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSpeakResponse(SpeakResponse response) {
		speakStage.onSpeakResponse(response);
	}

	@Override
	public void onSubscribeButtonResponse(SubscribeButtonResponse arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onUnsubscribeButtonResponse(UnsubscribeButtonResponse arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onOnDriverDistraction(OnDriverDistraction arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onOnAppInterfaceUnregistered(OnAppInterfaceUnregistered arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProxyOpened() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRegisterAppInterfaceResponse(RegisterAppInterfaceResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onUnregisterAppInterfaceResponse(
			UnregisterAppInterfaceResponse arg0) {
		// TODO Auto-generated method stub
		
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
