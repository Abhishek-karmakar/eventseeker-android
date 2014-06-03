package com.wcities.eventseeker.gear.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi;
import com.wcities.eventseeker.api.UserInfoApi.Type;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.ItemsList;
import com.wcities.eventseeker.gear.interfaces.SG2Api;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser;
import com.wcities.eventseeker.util.DeviceUtil;

public class SG2LoadMyEvents implements SG2Api {

	private static final String TAG = SG2LoadMyEvents.class.getSimpleName();
	private static final int EVENTS_LIMIT = 10;
	
	private EventSeekr eventSeekr;
	private int eventsAlreadyRequested;
	private boolean isMoreDataAvailable = true;
	private double[] latLon;
	
	private List<Event> events;
	
	public SG2LoadMyEvents(EventSeekr eventSeekr) {
		this.eventSeekr = eventSeekr;
		events = new ArrayList<Event>();
	}

	@Override
	public byte[] execute(Integer... params) {
		Log.d(TAG, "execute");
		byte[] bytes = null;
		Event reqEvent;
		int index = (params.length == 0 || params[0] == SG2ApiFactory.NOT_SPECIFIED) ? 0 : params[0];
		if (events.size() <= index && isMoreDataAvailable) {
			if (latLon == null) {
				latLon = DeviceUtil.getLatLon(eventSeekr);
			}
			
			UserInfoApi userInfoApi = new UserInfoApi(Api.OAUTH_TOKEN);
			userInfoApi.setLimit(EVENTS_LIMIT);
			userInfoApi.setAlreadyRequested(eventsAlreadyRequested);
			userInfoApi.setUserId(eventSeekr.getWcitiesId());
			userInfoApi.setLat(latLon[0]);
			userInfoApi.setLon(latLon[1]);
			
			try {
				JSONObject jsonObject = userInfoApi.getMyProfileInfoFor(Type.myevents);
				UserInfoApiJSONParser jsonParser = new UserInfoApiJSONParser();
				ItemsList<Event> myEventsList = jsonParser.getEventList(jsonObject);
				List<Event> myEvents = myEventsList.getItems();
				events.addAll(myEvents);
				eventsAlreadyRequested += myEvents.size();
				Log.d(TAG, "myEvents length = " + myEvents.size());
				
				if (myEvents.size() < EVENTS_LIMIT) {
					isMoreDataAvailable = false;
				}
				
				/*if (events.size() > index) {
					reqEvent = events.get(index);
					JSONObject jObjMyEvents = new JSONObject();
					JSONArray jsonArray = new JSONArray();
					jObjMyEvents.put("myevents", jsonArray);
					for (Iterator<Event> iterator = myEvents.iterator(); iterator.hasNext();) {
						Event event = iterator.next();
						jsonArray.put(event.getJSONObjForSG());
					}
					bytes = jObjMyEvents.toString().getBytes();
				}*/
	
			} catch (ClientProtocolException e) {
				e.printStackTrace();
				
			} catch (IOException e) {
				e.printStackTrace();
				
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		if (events.size() > index) {
			reqEvent = events.get(index);
			JSONObject jObjMyEvent = new JSONObject();
			try {
				jObjMyEvent.put("myevent", reqEvent.getJSONObjForSG());
				bytes = jObjMyEvent.toString().getBytes();

			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		Log.d(TAG, "execute return");
		return bytes;
	}
}
