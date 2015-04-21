package com.wcities.eventseeker.widget;

import java.io.IOException;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.IntentService;
import android.content.Intent;

import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.EventApi;
import com.wcities.eventseeker.api.UserInfoApi;
import com.wcities.eventseeker.api.UserInfoApi.Type;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.ItemsList;
import com.wcities.eventseeker.jsonparser.EventApiJSONParser;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser;
import com.wcities.eventseeker.util.DeviceUtil;

public class EventseekerWidgetService extends IntentService {
	
	private static final String TAG = EventseekerWidgetService.class.getName();
	private static final int EVENTS_LIMIT = 10;

	public EventseekerWidgetService() {
		super(TAG);
	}

	public static enum LoadType {
		LOAD_EVENTS,
		LOAD_IMAGE;
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		LoadType loadType = (LoadType) intent.getSerializableExtra(BundleKeys.LOAD_TYPE);
		//Log.d(TAG, "onHandleIntent(), loadType = " + loadType.name());

		if (loadType == LoadType.LOAD_EVENTS) {
			//Log.d(TAG, "execute loadEvents()");
			loadEvents();
			
		} else if (loadType == LoadType.LOAD_IMAGE) {
			Event event = (Event) intent.getSerializableExtra(BundleKeys.EVENT);
			AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
	        asyncLoadImg.loadImg(getApplication(), ImgResolution.LOW, event, intent.getIntExtra(BundleKeys.WIDGET_ID, 0));
		}
	}
	
	private void loadEvents() {
		List<Event> tmpEvents = null;
		
		UserInfoApi userInfoApi = new UserInfoApi(Api.OAUTH_TOKEN);
		userInfoApi.setLimit(EVENTS_LIMIT);
		userInfoApi.setUserId(((EventSeekr)getApplication()).getWcitiesId());
		double[] latLng = DeviceUtil.getLatLon((EventSeekr) EventseekerWidgetService.this.getApplicationContext());
		userInfoApi.setLat(latLng[0]);
		userInfoApi.setLon(latLng[1]);
		
		try {
			// load my events
			JSONObject jsonObject = userInfoApi.getMyProfileInfoFor(Type.myevents);
			UserInfoApiJSONParser jsonParser = new UserInfoApiJSONParser();
			
			ItemsList<Event> myEventsList = jsonParser.getEventList(jsonObject);
			tmpEvents = myEventsList.getItems();
			
			if (tmpEvents.isEmpty()) {
				//Log.d(TAG, "no my events found");
				// load recommended events
				jsonObject = userInfoApi.getMyProfileInfoFor(Type.recommendedevent);
				jsonParser = new UserInfoApiJSONParser();
				
				myEventsList = jsonParser.getRecommendedEventList(jsonObject);
				tmpEvents = myEventsList.getItems();
			}
			
			if (tmpEvents.isEmpty()) {
				//Log.d(TAG, "no recommended events found");
				// load featured events
				EventApi eventApi = new EventApi(Api.OAUTH_TOKEN, latLng[0], latLng[1]);
				eventApi.setLimit(EVENTS_LIMIT);
				jsonObject = eventApi.getFeaturedEvents();
				EventApiJSONParser eventApiJSONParser = new EventApiJSONParser();
				tmpEvents = eventApiJSONParser.getFeaturedEventList(jsonObject);
			}
				
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		if (tmpEvents != null && !tmpEvents.isEmpty()) {
			EventseekerWidgetList.getInstance().setEvents(tmpEvents);
			
		} else {
			//Log.d(TAG, "no featured events found");
		}
		
		Intent intent = new Intent();
		intent.setAction(EventseekerWidget.WIDGET_UPDATE)
		.putExtra(BundleKeys.WIDGET_UPDATE_TYPE, EventseekerWidget.UpdateType.REFRESH_WIDGET);
		
		getApplication().sendBroadcast(intent);
	}
}
