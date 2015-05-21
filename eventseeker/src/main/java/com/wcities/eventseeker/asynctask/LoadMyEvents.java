package com.wcities.eventseeker.asynctask;

import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.widget.BaseAdapter;

import com.wcities.eventseeker.api.UserInfoApi;
import com.wcities.eventseeker.api.UserInfoApi.Type;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.ItemsList;
import com.wcities.eventseeker.interfaces.AsyncTaskListener;
import com.wcities.eventseeker.interfaces.DateWiseEventParentAdapterListener;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LoadMyEvents extends AsyncTask<Void, Void, List<Event>> {
	
	private static final int EVENTS_LIMIT = 10;

	private static final String TAG = LoadMyEvents.class.getSimpleName();
	
	private String wcitiesId, oauthToken;
	private Type loadType;
	private double lat, lon;
	
	private List<Event> eventList;
	
	private DateWiseEventParentAdapterListener dateWiseEventParentAdapterListener;
	private AsyncTaskListener<Void> asyncTaskListener;

	private boolean addSrcFromNotification;
	
	public LoadMyEvents(String oauthToken, List<Event> eventList, DateWiseEventParentAdapterListener 
			dateWiseEventParentAdapterListener, String wcitiesId, Type loadType, double lat, double lon, 
			AsyncTaskListener<Void> asyncTaskListener) {
		this.oauthToken = oauthToken;
		this.eventList = eventList;
		this.dateWiseEventParentAdapterListener = dateWiseEventParentAdapterListener;
		this.wcitiesId = wcitiesId;
		this.loadType = loadType;
		this.lat = lat;
		this.lon = lon;
		this.asyncTaskListener = asyncTaskListener;
	}
	
	public void setAddSrcFromNotification(boolean addSrcFromNotification) {
		this.addSrcFromNotification = addSrcFromNotification;
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
		userInfoApi.setSrcFromNotification(addSrcFromNotification);
		userInfoApi.setAddTimestamp(true);
		
		try {
			JSONObject jsonObject = userInfoApi.getMyProfileInfoFor(loadType);
			UserInfoApiJSONParser jsonParser = new UserInfoApiJSONParser();
			
			ItemsList<Event> myEventsList;
			if (loadType == Type.recommendedevent) { 
				myEventsList = jsonParser.getRecommendedEventList(jsonObject);
				
			} else {
				/*loadType.myevents, loadType.mysavedevents*/
				myEventsList = jsonParser.getEventList(jsonObject);
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
		// used for bosch & also in new Mobile app ui
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
		if (dateWiseEventParentAdapterListener instanceof RecyclerView.Adapter) {
			((RecyclerView.Adapter) dateWiseEventParentAdapterListener).notifyDataSetChanged();
		
		} else {
			((BaseAdapter) dateWiseEventParentAdapterListener).notifyDataSetChanged();			
		}
		if (asyncTaskListener != null) {
			asyncTaskListener.onTaskCompleted();
		}
	}
}
