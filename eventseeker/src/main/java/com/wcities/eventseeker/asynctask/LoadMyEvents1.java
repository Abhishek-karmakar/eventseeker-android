package com.wcities.eventseeker.asynctask;

import android.os.AsyncTask;
import android.widget.BaseAdapter;

import com.wcities.eventseeker.api.UserInfoApi;
import com.wcities.eventseeker.api.UserInfoApi.Type;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.ItemsList;
import com.wcities.eventseeker.interfaces.DateWiseEventParentAdapterListener;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser;
import com.wcities.eventseeker.viewdata.DateWiseEventList;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LoadMyEvents1 extends AsyncTask<Void, Void, List<Event>> {
	
	private static final int EVENTS_LIMIT = 10;

	private static final String TAG = LoadMyEvents1.class.getSimpleName();
	
	private String wcitiesId, oauthToken;
	private Type loadType;
	private double lat, lon;
	
	private DateWiseEventList dateWiseEventList;
	private List<Event> eventList;
	
	private DateWiseEventParentAdapterListener dateWiseEventParentAdapterListener;
	private MyEventsLoadedListener myEventsLoadedListener;
	
	public interface MyEventsLoadedListener {
		public abstract void onEventsLoaded();
	}
	
	public LoadMyEvents1(String oauthToken, DateWiseEventList dateWiseEventList, DateWiseEventParentAdapterListener 
			dateWiseEventParentAdapterListener, String wcitiesId, 
			Type loadType, double lat, double lon, MyEventsLoadedListener myEventsLoadedListener) {
		this.oauthToken = oauthToken;
		this.dateWiseEventList = dateWiseEventList;
		this.dateWiseEventParentAdapterListener = dateWiseEventParentAdapterListener;
		this.wcitiesId = wcitiesId;
		this.loadType = loadType;
		this.lat = lat;
		this.lon = lon;
		this.myEventsLoadedListener = myEventsLoadedListener;
	}
	
	public LoadMyEvents1(String oauthToken, List<Event> eventList, DateWiseEventParentAdapterListener dateWiseEventParentAdapterListener, 
			String wcitiesId, Type loadType, double lat, double lon) {
		this.oauthToken = oauthToken;
		this.eventList = eventList;
		this.dateWiseEventParentAdapterListener = dateWiseEventParentAdapterListener;
		this.wcitiesId = wcitiesId;
		this.loadType = loadType;
		this.lat = lat;
		this.lon = lon;
	}
	
	@Override
	protected List<Event> doInBackground(Void... params) {
		List<Event> tmpEvents = new ArrayList<Event>();
		int eventsAlreadyRequested = dateWiseEventParentAdapterListener.getEventsAlreadyRequested();
		
		UserInfoApi userInfoApi = new UserInfoApi(oauthToken);
		userInfoApi.setLimit(EVENTS_LIMIT);
		userInfoApi.setAlreadyRequested(eventsAlreadyRequested);
		userInfoApi.setUserId(wcitiesId);
		userInfoApi.setLat(lat);
		userInfoApi.setLon(lon);
		
		try {
			JSONObject jsonObject = userInfoApi.getMyProfileInfoFor(loadType);
			UserInfoApiJSONParser jsonParser = new UserInfoApiJSONParser();
			
			ItemsList<Event> myEventsList;
			if (loadType == Type.myevents) {
				myEventsList = jsonParser.getEventList(jsonObject);
				
			} else {
				// loadType = Type.recommendedevent
				myEventsList = jsonParser.getRecommendedEventList(jsonObject);
			}

			tmpEvents = myEventsList.getItems();

		} catch (ClientProtocolException e) {
			e.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
			
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return tmpEvents;
	}
	
	@Override
	protected void onPostExecute(List<Event> tmpEvents) {
		if (dateWiseEventList != null) {
			if (tmpEvents.size() > 0) {
				dateWiseEventList.addEventListItems(tmpEvents, this);
				dateWiseEventParentAdapterListener.setEventsAlreadyRequested(dateWiseEventParentAdapterListener.getEventsAlreadyRequested() + tmpEvents.size());
				
				if (tmpEvents.size() < EVENTS_LIMIT) {
					dateWiseEventParentAdapterListener.setMoreDataAvailable(false);
					dateWiseEventList.removeProgressBarIndicator(this);
				}
				
			} else {
				dateWiseEventParentAdapterListener.setMoreDataAvailable(false);
				dateWiseEventList.removeProgressBarIndicator(this);
				// here 1 item is indicating no events message.
				if (dateWiseEventList.getCount() == 1) {
					myEventsLoadedListener.onEventsLoaded();
				}
			}
			
		} else {
			// used for bosch
			if (tmpEvents.size() > 0) {
				eventList.addAll(eventList.size() - 1, tmpEvents);
				dateWiseEventParentAdapterListener.setEventsAlreadyRequested(
						dateWiseEventParentAdapterListener.getEventsAlreadyRequested() + tmpEvents.size());
				
				if (tmpEvents.size() < EVENTS_LIMIT) {
					dateWiseEventParentAdapterListener.setMoreDataAvailable(false);
					eventList.remove(eventList.size() - 1);
				}
				
			} else {
				dateWiseEventParentAdapterListener.setMoreDataAvailable(false);
				eventList.remove(eventList.size() - 1);
				// here 1 item is indicating no events message.
				if (eventList.isEmpty()) {
					eventList.add(new Event(AppConstants.INVALID_ID, null));
				}
			}
		}
		((BaseAdapter)dateWiseEventParentAdapterListener).notifyDataSetChanged();
	}    	
}
