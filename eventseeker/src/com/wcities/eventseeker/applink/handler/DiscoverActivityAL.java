package com.wcities.eventseeker.applink.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.ford.syncV4.exception.SyncException;
import com.ford.syncV4.proxy.TTSChunkFactory;
import com.ford.syncV4.proxy.interfaces.IProxyListenerALM;
import com.ford.syncV4.proxy.rpc.AddCommandResponse;
import com.ford.syncV4.proxy.rpc.AddSubMenuResponse;
import com.ford.syncV4.proxy.rpc.AlertResponse;
import com.ford.syncV4.proxy.rpc.Choice;
import com.ford.syncV4.proxy.rpc.CreateInteractionChoiceSet;
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
import com.ford.syncV4.proxy.rpc.PerformInteraction;
import com.ford.syncV4.proxy.rpc.PerformInteractionResponse;
import com.ford.syncV4.proxy.rpc.ResetGlobalPropertiesResponse;
import com.ford.syncV4.proxy.rpc.SetGlobalPropertiesResponse;
import com.ford.syncV4.proxy.rpc.SetMediaClockTimerResponse;
import com.ford.syncV4.proxy.rpc.ShowResponse;
import com.ford.syncV4.proxy.rpc.Speak;
import com.ford.syncV4.proxy.rpc.SpeakResponse;
import com.ford.syncV4.proxy.rpc.SubscribeButtonResponse;
import com.ford.syncV4.proxy.rpc.TTSChunk;
import com.ford.syncV4.proxy.rpc.UnsubscribeButtonResponse;
import com.ford.syncV4.proxy.rpc.enums.InteractionMode;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.EventApi;
import com.wcities.eventseeker.api.EventApi.MoreInfo;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.applink.service.AppLinkService;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.jsonparser.EventApiJSONParser;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.DeviceUtil;

public class DiscoverActivityAL implements IProxyListenerALM {

	private static final String TAG = DiscoverActivityAL.class.getName();

	/*public static final String FEATURED_EVENTS = "Featured Events";
	public static final String EVENT_CATEGORIES = "Event Categories";*/
	
	private static final int START_CHOICE_ID_DISCOVER = 1;
	private static final int START_CHOICE_ID_EVENT_ACTION = 51;
	private static final int START_CHOICE_ID_NEXT_BACK = 61;
	private static final int EVENTS_LIMIT = 10;
	
	private EventSeekr context;
	private static DiscoverActivityAL instance;
	
	private List<Event> discoverByCategoryEvtList;
	private int currentEvtPos;
	
	private double lat, lon;
	private int eventsAlreadyRequested;
	private boolean isMoreDataAvailable = true;
	private int selectedCategoryId;
	
	private CurrentSpeakStage speakStage;
	
	private static enum CurrentSpeakStage {
		Basic,
		More;
	}
	
	/*private List<Category> evtCategories;
	private List<Event> featuredEvts;*/
	
	/*private int currentPage;;
	private CurrentScreenStage currentScreenStage;
	
	public static enum CurrentScreenStage {
		TOP_LEVEL,
		LOADING,
		EVENT_LIST,
		CATEGORY_LIST,
		EVENT_DETAILS
	}*/
	
	private enum ChoiceSetId {
		Discover(1),
		EventAction(2),
		NextBack(3);
		
		private int value;
		
		private ChoiceSetId(int value) {
			this.value = value;
		}
		
		/*public static ChoiceSetId getChoiceSetId(int value) {
			ChoiceSetId[] ids = ChoiceSetId.values();
			for (int i = 0; i < ids.length; i++) {
				ChoiceSetId choiceSetId = ids[i];
				if (choiceSetId.value == value) {
					return choiceSetId;
				}
			}
			return null;
		}*/
	}
	
	private enum DiscoverChoiceId {
		Concerts(START_CHOICE_ID_DISCOVER, 900),
		Clubs(START_CHOICE_ID_DISCOVER + 1, 905),
		Sports(START_CHOICE_ID_DISCOVER + 2, 902),
		Theater(START_CHOICE_ID_DISCOVER + 3, 901),
		Festivals(START_CHOICE_ID_DISCOVER + 4, 907);
		
		private int value, categoryId;
		
		private DiscoverChoiceId(int value, int categoryId) {
			this.value = value;
			this.categoryId = categoryId;
		}
		
		public static DiscoverChoiceId getDiscoverChoiceId(int value) {
			DiscoverChoiceId[] ids = DiscoverChoiceId.values();
			for (int i = 0; i < ids.length; i++) {
				DiscoverChoiceId discoverChoiceId = ids[i];
				if (discoverChoiceId.value == value) {
					return discoverChoiceId;
				}
			}
			return null;
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
	
	public static DiscoverActivityAL getInstance(EventSeekr context) {
		if (instance == null) {
			Log.i(TAG, "instance is null");
			instance = new DiscoverActivityAL(context);
		}
		Log.i(TAG, "return instance");
		return instance;
	}
	
	private DiscoverActivityAL(EventSeekr context) {
		this.context = context;
		discoverByCategoryEvtList = new ArrayList<Event>();
		//displayTopLevelScreen();
	}
	
	private void initializeInteractionChoiceSets() {
		ChoiceSetId[] choiceSetIds = ChoiceSetId.values();
		for (int i = 0; i < choiceSetIds.length; i++) {
			ChoiceSetId choiceSetId = choiceSetIds[i];
			createInteractionChoiceSet(choiceSetId);
		}
	}
	
	public void initiateInterAction() {
		Log.i(TAG, "initiateInterAction()");
		initializeInteractionChoiceSets();
		performInteraction(ChoiceSetId.Discover);
	}
	
	private void createInteractionChoiceSet(ChoiceSetId choiceSetId) {
		Vector<Choice> commands = new Vector<Choice>();
		
		switch (choiceSetId) {
		
		case Discover:
			DiscoverChoiceId[] discoverChoiceIds = DiscoverChoiceId.values();
			for (int i = 0; i < discoverChoiceIds.length; i++) {
				DiscoverChoiceId discoverChoiceId = discoverChoiceIds[i];
				Choice choice = new Choice();
				choice.setChoiceID(discoverChoiceId.value);
				choice.setMenuName(discoverChoiceId.name());
				choice.setVrCommands(new Vector<String>(Arrays.asList(new String[] {discoverChoiceId.name()})));
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
		Log.i(TAG, "performInteraction()");
		PerformInteraction msg = new PerformInteraction();
		msg.setCorrelationID(AppLinkService.getInstance().autoIncCorrId++);
		
		Vector<Integer> interactionChoiceSetIDs = new Vector<Integer>();
		interactionChoiceSetIDs.add(choiceSetId.value);
		
		String initialText = null, simple = null;
		switch (choiceSetId) {
		
		case Discover:
			simple = "Okay, would you like concerts, clubs, sports, theater or, festivals";
			initialText = choiceSetId.name();
			break;
			
		case EventAction:
			simple = "Share or More";
			initialText = discoverByCategoryEvtList.get(currentEvtPos).getName();
			break;
			
		case NextBack:
			simple = "Next or Back";
			initialText = discoverByCategoryEvtList.get(currentEvtPos).getName();
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
	
	/*private void displayTopLevelScreen() {
		AppLinkService.getInstance().show(FEATURED_EVENTS, EVENT_CATEGORIES, TextAlignment.LEFT_ALIGNED);
		currentScreenStage = CurrentScreenStage.TOP_LEVEL;
	}
	
	private int getCurrentPageFirstIndex() {
		return currentPage * 2;
	}
	
	private int getCurrentPageSecondIndex() {
		return getCurrentPageFirstIndex() + 1;
	}
	
	private boolean hasPrevItems() {
		return currentPage > 0;
	}*/
	
	/*private boolean hasNextItems() {
		if (currentScreenStage == CurrentScreenStage.CATEGORY_LIST) {
			return (currentPage + 1) * 2 < evtCategories.size();
			
		} else {
			return (currentPage + 1) * 2 < featuredEvts.size();
		}
	}*/
	
	/*private class LoadEvtCategories extends AsyncTask<Void, Void, Void> {
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			currentScreenStage = CurrentScreenStage.LOADING;
			currentPage = 0;
			AppLinkService.getInstance().show(FEATURED_EVENTS, "Loading Categories...", TextAlignment.LEFT_ALIGNED);
		}

		@Override
		protected Void doInBackground(Void... params) {
			InfoApi infoApi = new InfoApi(Api.OAUTH_TOKEN);
			try {
				JSONObject jsonObject = infoApi.getCategories();
				InfoApiJSONParser jsonParser = new InfoApiJSONParser();
				evtCategories = jsonParser.getCategoryList(jsonObject);
				
			} catch (ClientProtocolException e) {
				e.printStackTrace();
				
			} catch (IOException e) {
				e.printStackTrace();
				
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			// Continue updating UI only if user has not tried to revert the operation
			if (currentScreenStage == CurrentScreenStage.LOADING) {
				displayCategoryList();
			}
		}    	
    }*/
	
	/*private class LoadFeaturedEvts extends AsyncTask<Void, Void, Void> {
		
		private static final int FEATURED_EVTS_LIMIT = 5;
		
		private double lat, lon;
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			currentScreenStage = CurrentScreenStage.LOADING;
			currentPage = 0;
			AppLinkService.getInstance().show("Loading Events...", EVENT_CATEGORIES, TextAlignment.LEFT_ALIGNED);
		}

		@Override
		protected Void doInBackground(Void... params) {
			generateLatLon();
			EventApi eventApi = new EventApi(Api.OAUTH_TOKEN, lat, lon);
			eventApi.setLimit(FEATURED_EVTS_LIMIT);
			try {
				JSONObject jsonObject = eventApi.getFeaturedEvents();
				EventApiJSONParser jsonParser = new EventApiJSONParser();
				featuredEvts = jsonParser.getFeaturedEventList(jsonObject);
				
			} catch (ClientProtocolException e) {
				e.printStackTrace();
				
			} catch (IOException e) {
				e.printStackTrace();
				
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			// Continue updating UI only if user has not tried to revert the operation
			if (currentScreenStage == CurrentScreenStage.LOADING) {
				displayEventList();
			}
		}    	
		
		private void generateLatLon() {
	    	double[] latLon = DeviceUtil.getLatLon(context);
	    	lat = latLon[0];
	    	lon = latLon[1];
	    }
    }*/
	
	/*private void displayEventList() {
		String msg1 = featuredEvts.get(getCurrentPageFirstIndex()).getName();
		String msg2 = (getCurrentPageSecondIndex() < featuredEvts.size() - 1) ? 
				featuredEvts.get(getCurrentPageSecondIndex()).getName() : "";
		AppLinkService.getInstance().show(msg1, msg2, TextAlignment.LEFT_ALIGNED);
		currentScreenStage = CurrentScreenStage.EVENT_LIST;
	}
	
	private void displayCategoryList() {
		String msg1 = evtCategories.get(getCurrentPageFirstIndex()).getName();
		String msg2 = (getCurrentPageSecondIndex() < evtCategories.size() - 1) ? 
				evtCategories.get(getCurrentPageSecondIndex()).getName() : "";
		AppLinkService.getInstance().show(msg1, msg2, TextAlignment.LEFT_ALIGNED);
		currentScreenStage = CurrentScreenStage.CATEGORY_LIST;
	}*/
	
	/*private void displayEventDetails(Event event) {
		currentScreenStage = CurrentScreenStage.EVENT_DETAILS;
		
		Schedule schedule = event.getSchedule();
		String msg1 = deAccent(schedule.getVenue().getName());
		String msg2 = "";
		if (schedule.getDates().size() > 0) {
			com.wcities.eventseeker.core.Date date = schedule.getDates().get(0);
			
			DateFormat dateFormat = date.isStartTimeAvailable() ? new SimpleDateFormat("EEEE MMMM dd @h:mm a") :
				new SimpleDateFormat("EEEE MMMM dd");
			msg2 = dateFormat.format(date.getStartDate());
		}
		
		AppLinkService.getInstance().show(msg1, msg2, TextAlignment.LEFT_ALIGNED);
	}*/
	
	/*private String deAccent(String str) {
	    String nfdNormalizedString = Normalizer.normalize(str, Normalizer.Form.NFD); 
	    Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
	    return pattern.matcher(nfdNormalizedString).replaceAll("");
	}*/
	
	private void loadEvents(int categoryId) {
		List<Event> tmpEvents = null;

		EventApi eventApi = new EventApi(Api.OAUTH_TOKEN, lat, lon);
		eventApi.setLimit(EVENTS_LIMIT);
		eventApi.setAlreadyRequested(eventsAlreadyRequested);
		eventApi.setCategory(categoryId);
		eventApi.addMoreInfo(MoreInfo.artistdesc);

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
		//double[] latLon = new double[] {49.28766, -123.12361};
    	lat = latLon[0];
    	lon = latLon[1];
    	Log.i(TAG, "lat = " + lat + ", lon = " + lon);
    }
	
	private void speakCurrentEvt() {
		speakStage = CurrentSpeakStage.Basic;
		Event event = discoverByCategoryEvtList.get(currentEvtPos);
		String simple = "Okay, " + event.getName();
		
		if (event.getSchedule() != null) {
			simple += " at " + event.getSchedule().getVenue().getName() + " on " 
					+ ConversionUtil.getDateTime(event.getSchedule().getDates().get(0));
		}
		
		Log.i(TAG, "simple = " + simple);
		
		Vector<TTSChunk> chunks = TTSChunkFactory.createSimpleTTSChunks(simple);
		Speak msg = new Speak();
		msg.setCorrelationID(AppLinkService.getInstance().autoIncCorrId++);
		msg.setTtsChunks(chunks);
		
		try {
			AppLinkService.getInstance().getProxy().sendRPCRequest(msg);
			Log.i(TAG, "speakCurrentEvt() sendRPCRequest() done");
			
		} catch (SyncException e) {
			Log.i(TAG, "SyncException: ");
			e.printStackTrace();
		}
	}
	
	private void speakNoEventsAvailable() {
		String simple = "No events available for this category";
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
	
	private boolean areMoreEventDetailsAvailable() {
		Event event = discoverByCategoryEvtList.get(currentEvtPos);
		if ((event.getArtists() != null && !event.getArtists().isEmpty()) 
				|| (event.getSchedule() != null && !event.getSchedule().getPriceRange().isEmpty())) {
			return true;
		}
		return false;
	}
	
	private void speakMoreOnCurrentEvt() {
		speakStage = CurrentSpeakStage.More;
		Event event = discoverByCategoryEvtList.get(currentEvtPos);
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
		Log.i(TAG, "simple = " + simple);
		
		Vector<TTSChunk> chunks = TTSChunkFactory.createSimpleTTSChunks(simple);
		Speak msg = new Speak();
		msg.setCorrelationID(AppLinkService.getInstance().autoIncCorrId++);
		msg.setTtsChunks(chunks);
		
		try {
			AppLinkService.getInstance().getProxy().sendRPCRequest(msg);
			Log.i(TAG, "sendRPCRequest() done");
			
		} catch (SyncException e) {
			Log.i(TAG, "SyncException");
			e.printStackTrace();
		}
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
	
	private void resetLoadingParams() {
		currentEvtPos = 0;
		isMoreDataAvailable = true;
		eventsAlreadyRequested = 0;
		discoverByCategoryEvtList.clear();
		generateLatLon();
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
	public void onEncodedSyncPDataResponse(EncodedSyncPDataResponse arg0) {
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
	public void onOnButtonPress(OnButtonPress notification) {
		/*switch (notification.getButtonName()) {
		
		case PRESET_1:
			if (currentScreenStage == CurrentScreenStage.TOP_LEVEL) {
				new LoadFeaturedEvts().execute();
				
			} else if (currentScreenStage == CurrentScreenStage.EVENT_LIST) {
				displayEventDetails(featuredEvts.get(getCurrentPageFirstIndex()));
			}
			break;
			
		case PRESET_2:
			if (currentScreenStage == CurrentScreenStage.TOP_LEVEL) {
				new LoadEvtCategories().execute();
				
			} else if (currentScreenStage == CurrentScreenStage.EVENT_LIST) {
				if (getCurrentPageSecondIndex() <= featuredEvts.size() - 1) {
					displayEventDetails(featuredEvts.get(getCurrentPageSecondIndex()));
				}
			}
			break;
		
		case TUNEUP:
			if (currentScreenStage == CurrentScreenStage.EVENT_LIST) {
				if (!featuredEvts.isEmpty() && hasPrevItems()) {
					currentPage--;
					displayEventList();
				}
				
			} else if (currentScreenStage == CurrentScreenStage.CATEGORY_LIST) {
				if (!evtCategories.isEmpty() && hasPrevItems()) {
					currentPage--;
					displayCategoryList();
				}
			}
			break;
			
		case TUNEDOWN:
			if (currentScreenStage == CurrentScreenStage.EVENT_LIST) {
				if (!featuredEvts.isEmpty() && hasNextItems()) {
					currentPage++;
					displayEventList();
				}
				
			} else if (currentScreenStage == CurrentScreenStage.CATEGORY_LIST) {
				if (!evtCategories.isEmpty() && hasNextItems()) {
					currentPage++;
					displayCategoryList();
				}
			}
			break;
			
		case SEEKLEFT:
			if (currentScreenStage == CurrentScreenStage.EVENT_DETAILS) {
				displayEventList();
				
			} else if (currentScreenStage == CurrentScreenStage.LOADING 
					|| currentScreenStage == CurrentScreenStage.EVENT_LIST 
					|| currentScreenStage == CurrentScreenStage.CATEGORY_LIST) {
				displayTopLevelScreen();
			} 
			break;
			
		default:
			break;
		}*/
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
		
		if (response.getChoiceID() < START_CHOICE_ID_EVENT_ACTION) {
			selectedCategoryId = DiscoverChoiceId.getDiscoverChoiceId(response.getChoiceID()).categoryId;
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
		}
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
		switch (speakStage) {
		
		case Basic:
			ChoiceSetId choiceSetId;
			if (areMoreEventDetailsAvailable()) {
				choiceSetId = ChoiceSetId.EventAction;
				
			} else {
				choiceSetId = ChoiceSetId.NextBack;
			}
			performInteraction(choiceSetId);
			break;
			
		case More:
			performInteraction(ChoiceSetId.NextBack);
			break;

		default:
			break;
		}
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
	public void onOnEncodedSyncPData(OnEncodedSyncPData arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onOnTBTClientState(OnTBTClientState arg0) {
		// TODO Auto-generated method stub
		
	}
}
