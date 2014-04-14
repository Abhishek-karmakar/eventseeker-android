package com.wcities.eventseeker.applink.handler;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi;
import com.wcities.eventseeker.api.UserInfoApi.Type;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.applink.interfaces.ESIProxyALM;
import com.wcities.eventseeker.applink.util.CommandsUtil;
import com.wcities.eventseeker.applink.util.CommandsUtil.Commands;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser;

public class MyEventsAL extends ESIProxyALM {

	private static final String TAG = MyEventsAL.class.getName();
	private static final int EVENTS_LIMIT = 10;
	
	private static MyEventsAL instance;

	private EventSeekr context;
	private List<Event> currentEvtList;
	private int currentEvtPos;
	private int eventsAlreadyRequested;
	private double lat, lon;
	private boolean isMoreDataAvailable = true;
	
	public MyEventsAL(EventSeekr context) {
		this.context = context;
	}

	public static ESIProxyALM getInstance(EventSeekr context) {
		if (instance == null) {
			instance = new MyEventsAL(context);
			instance.onCreateInstance();
		}
		return instance;
	}

	@Override
	public void onCreateInstance() {
		Log.d(TAG, "onCreateInstance()");
	}
	
	@Override
	public void onStartInstance() {
		Log.d(TAG, "onStartInstance()");
		addCommands();
	}
	
	@Override
	public void onStopInstance() {
		Log.d(TAG, "onStopInstance()");	
		/*ALUtil.deleteInteractionChoiceSet(CHOICE_SET_ID_DISCOVER);
		Vector<Commands> delCmds = new Vector<Commands>();
		delCmds.add(Commands.DISCOVER);
		delCmds.add(Commands.MY_EVENTS);
		delCmds.add(Commands.SEARCH);
		delCmds.add(Commands.NEXT);
		delCmds.add(Commands.BACK);
		delCmds.add(Commands.DETAILS);
		delCmds.add(Commands.PLAY);
		delCmds.add(Commands.CALL_VENUE);
		CommandsUtil.deleteCommands(delCmds);*/
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

	/*private void loadMyEvents() {
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
			
			*//**
			 * since getMyProfileInfoFor() returns time sorted event list in ascending order, we can use second condition
			 * here to prevent redundant api calls based on last fetched event time.
			 *//*
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
	}*/
	
	private void loadRecommendedEvents() {
		Log.i(TAG, "loadRecommendedEvents(), eventsAlreadyRequested = " + eventsAlreadyRequested);
		List<Event> tmpEvents = null;
		/*lat = 37.7771199960262;
		lon = -122.419640002772;*/
		UserInfoApi userInfoApi = buildUserInfoApi();

		try {
			JSONObject jsonObject = userInfoApi.getMyProfileInfoFor(Type.recommendedevent);
			UserInfoApiJSONParser jsonParser = new UserInfoApiJSONParser();
			
			tmpEvents = jsonParser.getRecommendedEventList(jsonObject);
			
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
	
	private UserInfoApi buildUserInfoApi() {
		UserInfoApi userInfoApi = new UserInfoApi(Api.OAUTH_TOKEN);
		userInfoApi.setLimit(EVENTS_LIMIT);
		userInfoApi.setAlreadyRequested(eventsAlreadyRequested);
		userInfoApi.setUserId(context.getWcitiesId());
		userInfoApi.setLat(lat);
		userInfoApi.setLon(lon);
		return userInfoApi;
	}
	
}
