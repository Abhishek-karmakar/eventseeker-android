package com.wcities.eventseeker.widget;

import java.io.IOException;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.EventApi;
import com.wcities.eventseeker.api.UserInfoApi;
import com.wcities.eventseeker.api.UserInfoApi.Type;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.jsonparser.EventApiJSONParser;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser.MyItemsList;
import com.wcities.eventseeker.util.DeviceUtil;

public class EventseekerWidgetService extends Service {
	
	private static final String TAG = EventseekerWidgetService.class.getName();
	
	public static enum LoadType {
		LOAD_EVENTS,
		LOAD_IMAGE;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		//Log.d(TAG, "onStartCommand()");
		LoadType loadType = (LoadType) intent.getSerializableExtra(BundleKeys.LOAD_TYPE);
		if (loadType == LoadType.LOAD_EVENTS) {
			new LoadEvents().execute();  
			
		} else if (loadType == LoadType.LOAD_IMAGE) {
			Event event = (Event) intent.getSerializableExtra(BundleKeys.EVENT);
			AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
	        asyncLoadImg.loadImg(getApplication(), ImgResolution.LOW, event, intent.getIntExtra(BundleKeys.WIDGET_ID, 0));
		}

        stopSelf();  
  
        return super.onStartCommand(intent, flags, startId); 
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	private class LoadEvents extends AsyncTask<Void, Void, List<Event>> {
		
		private static final int EVENTS_LIMIT = 10;
		
		@Override
		protected List<Event> doInBackground(Void... params) {
			Log.d(TAG, "doInBackground");
			List<Event> tmpEvents = null;
			
			UserInfoApi userInfoApi = new UserInfoApi(Api.OAUTH_TOKEN);
			userInfoApi.setLimit(EVENTS_LIMIT);
			userInfoApi.setUserId(((EventSeekr)getApplication()).getWcitiesId());
			
			try {
				// load my events
				JSONObject jsonObject = userInfoApi.getMyProfileInfoFor(Type.myevents);
				UserInfoApiJSONParser jsonParser = new UserInfoApiJSONParser();
				
				MyItemsList<Event> myEventsList = jsonParser.getEventList(jsonObject);
				tmpEvents = myEventsList.getItems();
				
				if (tmpEvents.isEmpty()) {
					// load recommended events
					jsonObject = userInfoApi.getMyProfileInfoFor(Type.recommendedevent);
					jsonParser = new UserInfoApiJSONParser();
					
					tmpEvents = jsonParser.getRecommendedEventList(jsonObject);
				}
				
				if (tmpEvents.isEmpty()) {
					// load featured events
					double[] latLon = DeviceUtil.getLatLon(EventseekerWidgetService.this.getApplicationContext());
					EventApi eventApi = new EventApi(Api.OAUTH_TOKEN, latLon[0], latLon[1]);
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

			return tmpEvents;
		}
		
		@Override
		protected void onPostExecute(List<Event> result) {
			Log.d(TAG, "onPostExecute");
			if (result != null && !result.isEmpty()) {
				EventseekerWidgetList.getInstance().setEvents(result);
			}
			
			Intent intent = new Intent();
			intent.setAction(EventseekerWidget.WIDGET_UPDATE)
			.putExtra(BundleKeys.WIDGET_UPDATE_TYPE, EventseekerWidget.UpdateType.REFRESH_WIDGET);
			
			getApplication().sendBroadcast(intent);
		}    	
	}
}
