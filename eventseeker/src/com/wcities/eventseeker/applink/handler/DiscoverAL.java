package com.wcities.eventseeker.applink.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.ford.syncV4.proxy.TTSChunkFactory;
import com.ford.syncV4.proxy.rpc.AddCommandResponse;
import com.ford.syncV4.proxy.rpc.AddSubMenuResponse;
import com.ford.syncV4.proxy.rpc.AlertResponse;
import com.ford.syncV4.proxy.rpc.Choice;
import com.ford.syncV4.proxy.rpc.CreateInteractionChoiceSetResponse;
import com.ford.syncV4.proxy.rpc.DeleteCommandResponse;
import com.ford.syncV4.proxy.rpc.DeleteInteractionChoiceSetResponse;
import com.ford.syncV4.proxy.rpc.DeleteSubMenuResponse;
import com.ford.syncV4.proxy.rpc.EncodedSyncPDataResponse;
import com.ford.syncV4.proxy.rpc.GenericResponse;
import com.ford.syncV4.proxy.rpc.OnButtonEvent;
import com.ford.syncV4.proxy.rpc.OnButtonPress;
import com.ford.syncV4.proxy.rpc.OnCommand;
import com.ford.syncV4.proxy.rpc.OnDriverDistraction;
import com.ford.syncV4.proxy.rpc.OnEncodedSyncPData;
import com.ford.syncV4.proxy.rpc.OnHMIStatus;
import com.ford.syncV4.proxy.rpc.OnPermissionsChange;
import com.ford.syncV4.proxy.rpc.OnTBTClientState;
import com.ford.syncV4.proxy.rpc.PerformInteractionResponse;
import com.ford.syncV4.proxy.rpc.ResetGlobalPropertiesResponse;
import com.ford.syncV4.proxy.rpc.SetGlobalPropertiesResponse;
import com.ford.syncV4.proxy.rpc.SetMediaClockTimerResponse;
import com.ford.syncV4.proxy.rpc.ShowResponse;
import com.ford.syncV4.proxy.rpc.SpeakResponse;
import com.ford.syncV4.proxy.rpc.SubscribeButtonResponse;
import com.ford.syncV4.proxy.rpc.TTSChunk;
import com.ford.syncV4.proxy.rpc.UnsubscribeButtonResponse;
import com.ford.syncV4.proxy.rpc.enums.ButtonName;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.EventApi;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.applink.interfaces.ESIProxyListener;
import com.wcities.eventseeker.applink.service.AppLinkService;
import com.wcities.eventseeker.applink.util.ALUtil;
import com.wcities.eventseeker.applink.util.CommandsUtil;
import com.wcities.eventseeker.applink.util.CommandsUtil.Commands;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Date;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.jsonparser.EventApiJSONParser;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.DeviceUtil;

public class DiscoverAL implements ESIProxyListener {

	private static final String TAG = DiscoverAL.class.getName();
	private static final int EVENTS_LIMIT = 10;

	private static DiscoverAL instance;

	private EventSeekr context;
	private List<Event> discoverByCategoryEvtList;	
	private double lat, lon;
	private int currentEvtPos;
	private int eventsAlreadyRequested;
	private int selectedCategoryId;
	private boolean isMoreDataAvailable = true;
	
	private static enum ChoiceSetId {
		Discover(R.string.discover_al_discover);
		
		private int name;
		
		private ChoiceSetId(int name) {
			this.name = name;
		}
		
		public String getName() {
			return AppLinkService.getStringFromRes(name);
		}
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

	public static ESIProxyListener getInstance(EventSeekr context) {
		if (instance == null) {
			Log.d(TAG, "instance is null");
			instance = new DiscoverAL(context);
		}
		Log.d(TAG, "return instance");
		return instance;
	}

	@Override
	public void initiateInterAction() {
		Log.d(TAG, "initiateInterAction()");
		initializeInteractionChoiceSets();
		performInteraction();
		addCommands();
	}

	private void addCommands() {
		Vector<Commands> reqCommands = new Vector<Commands>();
		reqCommands.add(Commands.NEXT);
		reqCommands.add(Commands.BACK);
		reqCommands.add(Commands.DETAILS);
		reqCommands.add(Commands.PLAY);
		reqCommands.add(Commands.CALL_VENUE);
		CommandsUtil.addCommands(reqCommands);
	}

	private void initializeInteractionChoiceSets() {
		Log.d(TAG, "initializeInteractionChoiceSets()");

		Vector<Choice> commands = new Vector<Choice>();
		
		Discover[] categories = Discover.values();
		for (int i = 0; i < categories.length; i++) {
			Discover category = categories[i];
			
			Log.d(TAG, "Category id : " + category.getId());
			Log.d(TAG, "Category name : " + category.getName());
			
			Choice choice = ALUtil.createChoice(category.getId(), category.getName(), 
					new Vector<String>(Arrays.asList(new String[] {category.getName()})));
			commands.add(choice);
		}

		ALUtil.createInteractionChoiceSet(commands, ChoiceSetId.Discover.ordinal());
	}
	
	private void performInteraction() {
		Log.d(TAG, "performInteraction()");
		Log.d(TAG, "Discover ordinal : " + ChoiceSetId.Discover.ordinal());
		
		Vector<Integer> interactionChoiceSetIDList = new Vector<Integer>();
		interactionChoiceSetIDList.add(ChoiceSetId.Discover.ordinal());
		
		String simple = AppLinkService.getInstance().getString(R.string.discover_al_discover_categories);
		String initialText = ChoiceSetId.Discover.getName();		
		
		Vector<TTSChunk> initChunks = TTSChunkFactory.createSimpleTTSChunks(simple);
		Vector<TTSChunk> timeoutChunks = TTSChunkFactory.createSimpleTTSChunks(
				AppLinkService.getStringFromRes(R.string.discover_al_time_out));
		
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
		loadEvents(selectedCategoryId);

		if (discoverByCategoryEvtList.isEmpty()) {
			speakNoEventsAvailable();
			
		} else {
			speakCurrentEvent();
		}
		
		/*if (response.getChoiceID() < START_CHOICE_ID_EVENT_ACTION) {
			selectedCategoryId = Discover.getDiscoverChoiceId(response.getChoiceID()).categoryId;
			resetLoadingParams();
			loadEvents(selectedCategoryId);
			if (discoverByCategoryEvtList.isEmpty()) {
				speakNoEventsAvailable();
				
			} else {
				speakCurrentEvt();
			}
			
		} else if (response.getChoiceID() < START_CHOICE_ID_NEXT_BACK) {
			if (response.getChoiceID() == EventActionChoiceId.More.value) {
				speakMoreOnCurrentEvt();
				
			} else {
				Log.i(TAG, "onPerformInteractionResponse() - share");
			}
			
		} else {
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
					currentEvtPos = discoverByCategoryEvtList.size() - 1;
				}
			}
			Log.i(TAG, "currentEvtPos = " + currentEvtPos);
			speakCurrentEvt();
		}*/
	}
	
	private void speakNoEventsAvailable() {
		String simple = AppLinkService.getStringFromRes(R.string.event_no_evts_avail);
		Vector<TTSChunk> ttsChunks = TTSChunkFactory.createSimpleTTSChunks(simple);
		ALUtil.speakText(ttsChunks);		
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
			simple += ", at " + event.getSchedule().getVenue().getName();
			List<Date> dates = event.getSchedule().getDates();
			if (dates != null && !dates.isEmpty()) {
				simple += ", on " + ConversionUtil.getDateTime(dates.get(0));
			}
		}
		Log.i(TAG, "simple = " + simple);
		Vector<TTSChunk> ttsChunks = TTSChunkFactory.createSimpleTTSChunks(simple);
		ALUtil.speakText(ttsChunks);				
	}

	private void loadEvents(int categoryId) {
		List<Event> tmpEvents = null;

		EventApi eventApi = new EventApi(Api.OAUTH_TOKEN, lat, lon);
		eventApi.setLimit(EVENTS_LIMIT);
		eventApi.setAlreadyRequested(eventsAlreadyRequested);
		eventApi.setCategory(categoryId);

		try {
			JSONObject jsonObject = eventApi.getEvents();
			EventApiJSONParser jsonParser = new EventApiJSONParser();
			
			tmpEvents = jsonParser.getEventList(jsonObject);
			
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		//TODO:delete this line and below loop
		Log.d(TAG, "Size : " + tmpEvents.size());
		for (Event event : tmpEvents) {
			Log.d(TAG, "Event Name : " + event.getName());
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
	
	private void generateLatLon() {
    	double[] latLon = DeviceUtil.getLatLon(context);
    	lat = latLon[0];
    	lon = latLon[1];
    	Log.i(TAG, "lat = " + lat + ", lon = " + lon);
    }
	
	private boolean hasMoreEvents() {
		if (currentEvtPos + 1 < discoverByCategoryEvtList.size()) {
			return true;
			
		} else if (isMoreDataAvailable) {
			loadEvents(selectedCategoryId);
			if (currentEvtPos + 1 < discoverByCategoryEvtList.size()) {
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

	@Override
	public void onOnButtonPress(OnButtonPress notification) {
		ButtonName btnName = notification.getButtonName();
		Commands cmd = Commands.getCommandByButtonName(btnName);
		CommandsUtil.performOperationForCommand(cmd);
	}
	
	@Override
	public void onOnCommand(OnCommand notification) {
		Commands cmd = Commands.getCommandById(notification.getCmdID());
		CommandsUtil.performOperationForCommand(cmd);
	}
	
	@Override
	public void onAddCommandResponse(AddCommandResponse arg0) {}

	@Override
	public void onAddSubMenuResponse(AddSubMenuResponse arg0) {}
	
	@Override
	public void onAlertResponse(AlertResponse arg0) {}
	
	@Override
	public void onCreateInteractionChoiceSetResponse(CreateInteractionChoiceSetResponse arg0) {}
	
	@Override
	public void onDeleteCommandResponse(DeleteCommandResponse arg0) {}

	@Override
	public void onDeleteInteractionChoiceSetResponse(DeleteInteractionChoiceSetResponse arg0) {}

	@Override
	public void onDeleteSubMenuResponse(DeleteSubMenuResponse arg0) {}

	@Override
	public void onEncodedSyncPDataResponse(EncodedSyncPDataResponse arg0) {}
	
	@Override
	public void onError(String arg0, Exception arg1) {}

	@Override
	public void onGenericResponse(GenericResponse arg0) {}

	@Override
	public void onOnButtonEvent(OnButtonEvent arg0) {}

	@Override
	public void onOnHMIStatus(OnHMIStatus arg0) {}
	@Override
	public void onOnPermissionsChange(OnPermissionsChange arg0) {}
	
	@Override
	public void onProxyClosed(String arg0, Exception arg1) {}
	
	@Override
	public void onResetGlobalPropertiesResponse(ResetGlobalPropertiesResponse arg0) {}

	@Override
	public void onSetGlobalPropertiesResponse(SetGlobalPropertiesResponse arg0) {}
	
	@Override
	public void onSetMediaClockTimerResponse(SetMediaClockTimerResponse arg0) {}
	
	@Override
	public void onShowResponse(ShowResponse arg0) {}
	
	@Override
	public void onSpeakResponse(SpeakResponse arg0) {}
	
	@Override
	public void onSubscribeButtonResponse(SubscribeButtonResponse arg0) {}
	
	@Override
	public void onUnsubscribeButtonResponse(UnsubscribeButtonResponse arg0) {}
	
	@Override
	public void onOnDriverDistraction(OnDriverDistraction arg0) {}
	
	@Override
	public void onOnEncodedSyncPData(OnEncodedSyncPData arg0) {}
	
	@Override
	public void onOnTBTClientState(OnTBTClientState arg0) {}
	
}
