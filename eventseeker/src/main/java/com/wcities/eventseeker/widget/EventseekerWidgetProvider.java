package com.wcities.eventseeker.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

import com.wcities.eventseeker.EventDetailsActivityTab;
import com.wcities.eventseeker.MainActivity;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.widget.EventseekerWidget.UpdateType;
import com.wcities.eventseeker.widget.EventseekerWidgetService.LoadType;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;

public class EventseekerWidgetProvider extends AppWidgetProvider {
	
	private static final String TAG = EventseekerWidgetProvider.class.getName();
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		//Log.d(TAG, "onUpdate()");
		EventseekerWidgetList eventseekerWidgetList = EventseekerWidgetList.getInstance();
		for (int widgetId : appWidgetIds) {
			//Log.d(TAG, "widgetId = " + widgetId);
			if (!eventseekerWidgetList.contains(widgetId)) {
				//Log.d(TAG, "add widget into eventseekerWidgetList = " + eventseekerWidgetList);
				eventseekerWidgetList.addWidget(new EventseekerWidget(widgetId));
			}
		}
		
		/*for (int i = 0; i < appWidgetIds.length; i++) {
			Log.d(TAG, "widgetId = " + appWidgetIds[i]);
		}*/
		loadEvents(context);
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		/**
		 * WIDGET_INIT and WIDGET_UPDATE actions are handle below so no need to call super.onReceive().
		 */
		String action = intent.getAction();
		Bundle bundle = intent.getExtras();
		//Log.d(TAG, "onReceive() action = " + action);
		
		if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
			//Log.d(TAG, "CONNECTIVITY_ACTION");
			ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		    NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
		    
		    if (activeNetInfo != null & EventseekerWidgetList.getInstance().getEvents().isEmpty()) {
		    	//Log.d(TAG, "activeNetInfo != null");
		    	callOnUpdate(context);
		    }
			
		} /*else if (action.equals(EventseekerWidget.WIDGET_PREV_EVENT)) {
			//Log.d(TAG, "WIDGET_PREV_EVENT");
			int widgetId = bundle.getInt(BundleKeys.WIDGET_ID);
			Log.d(TAG, "widgetId = " + widgetId);
			EventseekerWidgetList eventseekerWidgetList = EventseekerWidgetList.getInstance();
			EventseekerWidget widget = eventseekerWidgetList.getWidget(widgetId);
			if (widget == null) {
				Log.d(TAG, "widget == null for eventseekerWidgetList = " + eventseekerWidgetList);
				callOnUpdate(context);
				
			} else {
				widget.setCurrentEvent(eventseekerWidgetList.getPrevious(widget.getCurrentEvent()));
				refreshWidget(context, eventseekerWidgetList, widget, eventseekerWidgetList.getEvents());
			}
			
		}*/ else if (action.equals(EventseekerWidget.WIDGET_NEXT_EVENT)) {
			//Log.d(TAG, "WIDGET_NEXT_EVENT");
			int widgetId = bundle.getInt(BundleKeys.WIDGET_ID);
			//Log.d(TAG, "widgetId = " + widgetId);
			EventseekerWidgetList eventseekerWidgetList = EventseekerWidgetList.getInstance();
			EventseekerWidget widget = eventseekerWidgetList.getWidget(widgetId);
			if (widget == null) {
				//Log.d(TAG, "widget == null for eventseekerWidgetList = " + eventseekerWidgetList);
				callOnUpdate(context);
				
			} else {
				widget.setCurrentEvent(eventseekerWidgetList.getNext(widget.getCurrentEvent()));
				refreshWidget(context, eventseekerWidgetList, widget, eventseekerWidgetList.getEvents());
			}
			
		} else if (action.equals(EventseekerWidget.WIDGET_UPDATE)) {
		
			UpdateType updateType = (UpdateType) bundle.getSerializable(BundleKeys.WIDGET_UPDATE_TYPE);
			
			if (updateType == UpdateType.REFRESH_WIDGET) {
				EventseekerWidgetList eventseekerWidgetList = EventseekerWidgetList.getInstance();
				List<EventseekerWidget> widgets = eventseekerWidgetList.getWidgetList();
				List<Event> events = eventseekerWidgetList.getEvents();
				
				if (events != null && !events.isEmpty()) {
					//Log.d(TAG, "!null");
					
					for (Iterator<EventseekerWidget> iterator = widgets.iterator(); iterator.hasNext();) {
						EventseekerWidget eventseekerWidget = iterator.next();
						refreshWidget(context, eventseekerWidgetList, eventseekerWidget, events);
					}
		            
				} else {
		            //Log.d(TAG, "null");
					/*if (retryLoadingCount < MAX_RETRY_LOADING_COUNT) {
						Log.d(TAG, "retryLoading - " + retryLoadingCount + "for eventseekerWidgetList = " + eventseekerWidgetList);
						++retryLoadingCount;
						callOnUpdate(context);
						
					} else {
						Log.d(TAG, "no events found.");
						retryLoadingCount = 0;*/
						
						RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_loading);  
						remoteViews.setTextViewText(R.id.btnLoading, "No events found.");  
						
						// get widget ids for widgets created  
						AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
						ComponentName widget = new ComponentName(context, EventseekerWidgetProvider.class);  
						int[] widgetIds = widgetManager.getAppWidgetIds(widget); 
						widgetManager.updateAppWidget(widgetIds, remoteViews);
					//}
				}
				
			} else if (updateType == UpdateType.REFRESH_IMAGE) {
				int widgetId = bundle.getInt(BundleKeys.WIDGET_ID);
				EventseekerWidgetList eventseekerWidgetList = EventseekerWidgetList.getInstance();
				EventseekerWidget widget = eventseekerWidgetList.getWidget(widgetId);
				if (widget != null && widget.getCurrentEvent().getId() == bundle.getLong(BundleKeys.EVENT_ID)) {
					refreshWidget(context, eventseekerWidgetList, widget, eventseekerWidgetList.getEvents());
				}
			}
			
		} else {
			super.onReceive(context, intent);
		}
	}
	
	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);
		EventseekerWidgetList.getInstance().removeWidget(appWidgetIds[0]);
	}
	
	private void callOnUpdate(Context context) {
		//Log.d(TAG, "callOnUpdate()");
		Intent updateIntent = buildUpdateIntent(context);
		context.sendBroadcast(updateIntent);
	}
	
	private Intent buildUpdateIntent(Context context) {
		AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
		ComponentName widgetComponent = new ComponentName(context, EventseekerWidgetProvider.class);
		int[] widgetIds = widgetManager.getAppWidgetIds(widgetComponent);
		
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_loading);
		remoteViews.setTextViewText(R.id.btnLoading, context.getResources().getString(R.string.loading_events));  
		widgetManager.updateAppWidget(widgetIds, remoteViews);
		
		Intent updateIntent = new Intent();
		updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
		updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
		
		return updateIntent;
	}
	
	private void loadEvents(Context context) {
		//Log.d(TAG, "loadEvents()");
		Intent intent = new Intent(context.getApplicationContext(), EventseekerWidgetService.class); 
		intent.putExtra(BundleKeys.LOAD_TYPE, LoadType.LOAD_EVENTS);
        context.startService(intent);
	}
	
	private void refreshWidget(Context context, EventseekerWidgetList eventseekerWidgetList, EventseekerWidget eventseekerWidget, List<Event> events) {
		Event event = eventseekerWidget.getCurrentEvent();
		if (event == null) {
			event = events.get(0);
			eventseekerWidget.setCurrentEvent(event);
		} 
				
	    RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);  
        remoteViews.setTextViewText(R.id.txtEvtTitle, event.getName());  
        BitmapCache bitmapCache = BitmapCache.getInstance();
        String key = event.getKey(ImgResolution.LOW);
		Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
		if (bitmap != null) {
	        remoteViews.setImageViewBitmap(R.id.imgEvt, bitmap);
	        
	        remoteViews.setViewVisibility(R.id.progressBar, View.INVISIBLE);
	        remoteViews.setViewVisibility(R.id.imgRefresh, View.VISIBLE);
	        
	    } else {
	        Intent serviceIntent = new Intent(context.getApplicationContext(), EventseekerWidgetService.class); 
	        serviceIntent.putExtra(BundleKeys.LOAD_TYPE, LoadType.LOAD_IMAGE);
	        serviceIntent.putExtra(BundleKeys.EVENT, event);
	        serviceIntent.putExtra(BundleKeys.WIDGET_ID, eventseekerWidget.getWidgetId());
	        context.startService(serviceIntent);
	        
	        remoteViews.setViewVisibility(R.id.progressBar, View.VISIBLE);
	        remoteViews.setViewVisibility(R.id.imgRefresh, View.INVISIBLE);
	    }
		
		if (event.getSchedule() != null && !event.getSchedule().getDates().isEmpty()) {
			com.wcities.eventseeker.core.Date date = event.getSchedule().getDates().get(0);
			
			DateFormat dateFormat = date.isStartTimeAvailable() ? new SimpleDateFormat("EEE dd MMM yyyy, h:mma") :
					new SimpleDateFormat("EEE dd MMM yyyy");
			remoteViews.setTextViewText(R.id.txtEvtTime, dateFormat.format(date.getStartDate()));

		}
		
		/*Event prevEvent = eventseekerWidgetList.getPrevious(event);
		if (prevEvent != null) {
			remoteViews.setBoolean(R.id.imgLeft, "setEnabled", true);
			
			//Log.d(TAG, "prevEvent != null for widget Id = " + eventseekerWidget.getWidgetId());
			Intent leftIntent = new Intent();
			leftIntent.setAction(EventseekerWidget.WIDGET_PREV_EVENT)
			.putExtra(BundleKeys.WIDGET_ID, eventseekerWidget.getWidgetId());
			PendingIntent pendingIntent = PendingIntent.getBroadcast(context, eventseekerWidget.getWidgetId(), leftIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			remoteViews.setOnClickPendingIntent(R.id.imgLeft, pendingIntent);
			
		} else {
			//Log.d(TAG, "prevEvent = null for widget Id = " + eventseekerWidget.getWidgetId());
			remoteViews.setBoolean(R.id.imgLeft, "setEnabled", false);
		}
		
		Event nextEvent = eventseekerWidgetList.getNext(event);
		if (nextEvent != null) {
			remoteViews.setBoolean(R.id.imgRight, "setEnabled", true);

			//Log.d(TAG, "nextEvent != null for widget Id = " + eventseekerWidget.getWidgetId());
			Intent rightIntent = new Intent();
			rightIntent.setAction(EventseekerWidget.WIDGET_NEXT_EVENT)
			.putExtra(BundleKeys.WIDGET_ID, eventseekerWidget.getWidgetId());
			PendingIntent pendingIntent = PendingIntent.getBroadcast(context, eventseekerWidget.getWidgetId(), rightIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			remoteViews.setOnClickPendingIntent(R.id.imgRight, pendingIntent);
			
		} else {
			//Log.d(TAG, "nextEvent = null for widget Id = " + eventseekerWidget.getWidgetId());
			remoteViews.setBoolean(R.id.imgRight, "setEnabled", false);
		}*/
		boolean isTablet = ((EventSeekr) context.getApplicationContext()).isTablet();
		Intent widgetIntent;
		if (isTablet) {
			widgetIntent = new Intent(context, EventDetailsActivityTab.class);			
			
		} else {
			widgetIntent = new Intent(context, MainActivity.class);			
		}
		widgetIntent.putExtra(BundleKeys.EVENT, event);
		/**
		 * 18-09-2014: See Commit: removed launchmode=singletask due to error in Bosch
		 * NOTE: added Action and Category of Launcher Activity for notification Intent so as to avoid
		 * creating 2 new task for notification and app launch event(when from app menu).
		 * For getting this issue, remove the below 2 lines and follow steps below:
		 * e.g.:- Launch app -> sync accounts -> get notification of NotificationType=FOLLOWING -> click on it.
		 *	      It redirects to 'following' screen -> press back to move out of app. Now from home screen click 
		 *	      app icon. It displays discover screen rather then last 'following' screen which was browsed. Same result
		 *	      can be observed now onwards for every launch. This happens due to onCreate() being called up on every 
		 *	      launch after this, even though one task is there in back stack for the app.
		 **/
		if (!isTablet) {
			widgetIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		}
		PendingIntent pendingIntent = PendingIntent.getActivity(context, eventseekerWidget.getWidgetId(), 
				widgetIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		remoteViews.setOnClickPendingIntent(R.id.rltLayoutRoot, pendingIntent);
		
		Intent nextEvtIntent = new Intent();
		nextEvtIntent.setAction(EventseekerWidget.WIDGET_NEXT_EVENT)
		.putExtra(BundleKeys.WIDGET_ID, eventseekerWidget.getWidgetId());
		pendingIntent = PendingIntent.getBroadcast(context, eventseekerWidget.getWidgetId(), nextEvtIntent, 
				PendingIntent.FLAG_UPDATE_CURRENT);
		remoteViews.setOnClickPendingIntent(R.id.rltLayoutRefresh, pendingIntent);
		
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        widgetManager.updateAppWidget(eventseekerWidget.getWidgetId(), remoteViews);
	}
}
