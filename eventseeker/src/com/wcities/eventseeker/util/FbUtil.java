package com.wcities.eventseeker.util;

import java.text.SimpleDateFormat;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Request.GraphUserCallback;
import com.facebook.RequestAsyncTask;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.Session.StatusCallback;
import com.facebook.SessionState;
import com.facebook.model.GraphObject;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingItemType;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.UserTracker;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.SharedPrefKeys;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.Event.Attending;
import com.wcities.eventseeker.core.FriendNewsItem;
import com.wcities.eventseeker.interfaces.PublishListener;

public class FbUtil {

	private static final String TAG = FbUtil.class.getName();

	public static boolean hasUserLoggedInBefore(Context context) {
		//Log.d(TAG, "hasUserLoggedInBefore()");
		SharedPreferences pref = context.getSharedPreferences(
                AppConstants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
		return pref.contains(SharedPrefKeys.FACEBOOK_USER_ID);
	}
	
	public static void callFacebookLogout(EventSeekr eventSeekr) {
	    Session session = Session.getActiveSession();
	    if (session != null) {
	        //if (!session.isClosed()) {
	            session.closeAndClearTokenInformation();
	            //clear your preferences if saved
	            eventSeekr.removeFbUserId();
	            eventSeekr.removeFbUserName();
	        //}
	        
	    } else {
	        session = new Session(eventSeekr);
	        Session.setActiveSession(session);

	        session.closeAndClearTokenInformation();
	        
	        //clear your preferences if saved
	        eventSeekr.removeFbUserId();
	        eventSeekr.removeFbUserName();
	    }
	}
	
	public static void onClickLogin(Fragment fragment, StatusCallback statusCallback) {
		//Log.d(TAG, "onClickLogin()");
        Session session = Session.getActiveSession();
        if (session == null) {
        	//Log.d(TAG, "session == null");
            session = new Session(FragmentUtil.getActivity(fragment));
            Session.setActiveSession(session);
        }
        if (!session.isOpened() && !session.isClosed()) {
        	//Log.d(TAG, "openForRead");
            session.openForRead(new Session.OpenRequest(fragment).setCallback(statusCallback));
            
        } else {
        	//Log.d(TAG, "openActiveSession");
            Session.openActiveSession(FragmentUtil.getActivity(fragment), fragment, true, statusCallback);
        }
    }
	
	public static void makeMeRequest(final Session session, GraphUserCallback graphUserCallback) {
	    // Make an API call to get user data and define a 
	    // new callback to handle the response.
	    Request request = Request.newMeRequest(session, graphUserCallback);
	    request.executeAsync();
	}
	
	public static void handlePublishFriendNewsItem(PublishListener fbPublishListener, Fragment fragment, 
			List<String> permissions, int requestCode, FriendNewsItem friendNewsItem) {
		Log.d(TAG, "handlePublish()");
		if (canPublishNow(fbPublishListener, fragment, permissions, requestCode)) {
			friendNewsItem.updateUserAttendingToNewUserAttending();
			fbPublishListener.onPublishPermissionGranted();
			publishFriendNewsItem(friendNewsItem, fragment);
		}
	}
	
	public static void handlePublishEvent(PublishListener fbPublishListener, Fragment fragment, 
			List<String> permissions, int requestCode, Event event) {
		Log.d(TAG, "handlePublish()");
		if (canPublishNow(fbPublishListener, fragment, permissions, requestCode)) {
			event.updateAttendingToNewAttending();
			fbPublishListener.onPublishPermissionGranted();
			publishEvent(event, fragment);
		}
	}
	
	private static boolean canPublishNow(PublishListener fbPublishListener, Fragment fragment, 
			List<String> permissions, int requestCode) {
		fbPublishListener.setPendingAnnounce(false);
	    Session session = Session.getActiveSession();

	    if (session == null) {
	    	Log.d(TAG, "session=null");
	    	session = new Session(FragmentUtil.getActivity(fragment));
			Session.setActiveSession(session);
	    }
	    
	    //Log.d(TAG, "active session, state=" + session.getState().name());
	    if (!session.isOpened()) {
	    	//Log.d(TAG, "session is not opened");
	    	fbPublishListener.setPendingAnnounce(true); // Mark that we are currently waiting for opening of session
	    	Fragment fragmentToHandleActivityResult = FragmentUtil.getTopLevelParentFragment(fragment);
    		Session.openActiveSession(FragmentUtil.getActivity(fragment), fragmentToHandleActivityResult, true, fbPublishListener);
    		return false;
	    }

	    if (!hasPublishPermission(permissions)) {
	    	//Log.d(TAG, "publish permission is not there");
	    	fbPublishListener.setPendingAnnounce(true); // Mark that we are currently waiting for confirmation of publish permissions
	        session.addCallback(fbPublishListener); 
	        // we get top level parent fragment here since onActivityResult() is not called in nested fragments.
	        Fragment fragmentToHandleActivityResult = FragmentUtil.getTopLevelParentFragment(fragment);
	        requestPublishPermissions(session, permissions, requestCode, fragmentToHandleActivityResult, 
	        		fbPublishListener);
	        return false;
	    }
	    
	    return true;
	}
	
	public static boolean hasPublishPermission(List<String> permissions) {
		Log.d(TAG, "hasPublishPermission()");
        Session session = Session.getActiveSession();
        return session != null && session.getPermissions().containsAll(permissions);
    }
	
	private static void requestPublishPermissions(Session session, List<String> permissions, int requestCode, 
			Fragment fragmentToHandleActivityResult, PublishListener fbPublishListener) {
		Log.d(TAG, "requestPublishPermissions()");
        Session.NewPermissionsRequest reauthRequest = new Session.NewPermissionsRequest(fragmentToHandleActivityResult, 
        		permissions).setRequestCode(requestCode);
        try {
        	session.requestNewPublishPermissions(reauthRequest);
        	
        } catch (UnsupportedOperationException e) {
        	/**
        	 * To handle,
        	 * java.lang.UnsupportedOperationException: Session: an attempt was made to request new 
        	 * permissions for a session that has a pending request.
        	 */
        	Log.e(TAG, "UnsupportedOperationException");
        	e.printStackTrace();
        	fbPublishListener.setPendingAnnounce(false);
        }
	}
	
	public static void call(Session session, SessionState state, Exception exception, 
			PublishListener fbPublishListener, Fragment fragment, 
			List<String> permissions, int requestCode, Event event) {
		Log.d(TAG, "call(): state = " + state);
		if (session != null && session.isOpened()) {
	    	if (state.equals(SessionState.OPENED)) {
	    		//Log.d(TAG, "OPENED");
	    		// Session opened 
	            // so try publishing once more.
	    		sessionOpened(fbPublishListener, fragment, permissions, 
		    			requestCode, event);
	    		
	    	} else if (state.equals(SessionState.OPENED_TOKEN_UPDATED)) {
	    		//Log.d(TAG, "OPENED_TOKEN_UPDATED");
	            // Session updated with new permissions
	            // so try publishing once more.
	            tokenUpdated(fbPublishListener, fragment, permissions, 
		    			requestCode, event);
	        }
	    }
	}
	
	public static void call(Session session, SessionState state, Exception exception, 
			PublishListener fbPublishListener, Fragment fragment, 
			List<String> permissions, int requestCode, FriendNewsItem friendNewsItem) {
		Log.d(TAG, "call(): state = " + state);
		if (session != null && session.isOpened()) {
	    	if (state.equals(SessionState.OPENED)) {
	    		//Log.d(TAG, "OPENED");
	    		// Session opened 
	            // so try publishing once more.
	    		sessionOpened(fbPublishListener, fragment, permissions, 
		    			requestCode, friendNewsItem);
	    		
	    	} else if (state.equals(SessionState.OPENED_TOKEN_UPDATED)) {
	    		//Log.d(TAG, "OPENED_TOKEN_UPDATED");
	            // Session updated with new permissions
	            // so try publishing once more.
	            tokenUpdated(fbPublishListener, fragment, permissions, 
		    			requestCode, friendNewsItem);
	        }
	    }
	}
	
	private static void sessionOpened(PublishListener fbPublishListener, Fragment fragment, 
			List<String> permissions, int requestCode, Event event) {
		Log.d(TAG, "sessionOpened()");
		// Check if a publish action is in progress
	    // awaiting a successful reauthorization
	    if (fbPublishListener.isPendingAnnounce()) {
	        // Publish the action
	    	handlePublishEvent(fbPublishListener, fragment, permissions, requestCode, event);
	    }
	}
	
	private static void sessionOpened(PublishListener fbPublishListener, Fragment fragment, 
			List<String> permissions, int requestCode, FriendNewsItem friendNewsItem) {
		Log.d(TAG, "sessionOpened()");
		// Check if a publish action is in progress
	    // awaiting a successful reauthorization
	    if (fbPublishListener.isPendingAnnounce()) {
	        // Publish the action
	    	handlePublishFriendNewsItem(fbPublishListener, fragment, permissions, requestCode, friendNewsItem);
	    }
	}
	
	/**
	 * Called when additional permission request is completed successfully.
	 */
	private static void tokenUpdated(PublishListener fbPublishListener, Fragment fragment, 
			List<String> permissions, int requestCode, Event event) {
		Log.d(TAG, "tokenUpdated()");
	    // Check if a publish action is in progress
	    // awaiting a successful reauthorization
	    if (fbPublishListener.isPendingAnnounce()) {
	        
	    	if (hasPublishPermission(permissions)) {
	    		// Publish the action
	    		handlePublishEvent(fbPublishListener, fragment, permissions, requestCode, 
		    			event);
	    		
	    	} else {
	    		// user has denied the permission
	    		fbPublishListener.setPendingAnnounce(false);
	    	}
	    }
	}
	
	private static void tokenUpdated(PublishListener fbPublishListener, Fragment fragment, 
			List<String> permissions, int requestCode, FriendNewsItem friendNewsItem) {
		Log.d(TAG, "tokenUpdated()");
	    // Check if a publish action is in progress
	    // awaiting a successful reauthorization
	    if (fbPublishListener.isPendingAnnounce()) {
	        
	    	if (hasPublishPermission(permissions)) {
	    		// Publish the action
	    		handlePublishFriendNewsItem(fbPublishListener, fragment, permissions, requestCode, 
		    			friendNewsItem);
	    		
	    	} else {
	    		// user has denied the permission
	    		fbPublishListener.setPendingAnnounce(false);
	    	}
	    }
	}
	
	private static void publishEvent(final Event event, final Fragment fragment) {
	    Session session = Session.getActiveSession();

        Bundle postParams = new Bundle();
        String name = "I am going to an event ";
        if (event.getAttending() == Attending.WANTS_TO_GO) {
        	name = "I want to go to an event ";
        }
        name += "'" + event.getName() + "' on eventseeker";
        postParams.putString("name", name);
        String caption = "";
        if (event.getSchedule() != null) {
        	if (event.getSchedule().getVenue().getAddress() != null) {
        		caption += "at " + event.getSchedule().getVenue().getAddress().getCity();
        	}
        	if (event.getSchedule().getDates().size() > 0) {
        		caption += " on " + new SimpleDateFormat("EEEE, MMM d").format(event.getSchedule().getDates().get(0).getStartDate());
        	}
            postParams.putString("caption", caption);
        }
        String description = event.getDescription() == null ? " " : event.getDescription();
        postParams.putString("description", description);
        
        String link = event.getEventUrl();
        if (link == null) {
        	link = "http://eventseeker.com/event/" + event.getId();
	    }
        postParams.putString("link", link);

        if (event.doesValidImgUrlExist()) {
        	String imgUrl = event.getHighResImgUrl();
        	if (imgUrl == null) {
        		imgUrl = event.getLowResImgUrl();
        	}
        	if (imgUrl == null) {
        		imgUrl = event.getMobiResImgUrl();
        	}
            postParams.putString("picture", imgUrl);
        }

        Request.Callback callback = new Request.Callback() {
        	
            public void onCompleted(Response response) {
            	Log.d(TAG, "response = " + response.toString());
            	String postId = null;

                GraphObject graphObject = response.getGraphObject();
                if (graphObject != null) {
                	JSONObject graphResponse = graphObject.getInnerJSONObject();
                    try {
                        postId = graphResponse.getString("id");
                        postId = postId.split("_")[1];
                        
                    } catch (JSONException e) {
                        Log.i(TAG, "JSON error "+ e.getMessage());
                    }
                    
                } else {
                	Log.d(TAG, "graphObj = null");
                }
                
                new UserTracker((EventSeekr) FragmentUtil.getActivity(fragment).getApplication(), 
                		UserTrackingItemType.event, event.getId(), event.getAttending().getValue(), postId, 
                		UserTrackingType.Add).execute();
            }
        };

        Request request = new Request(session, "me/feed", postParams, HttpMethod.POST, callback);
        RequestAsyncTask task = new RequestAsyncTask(request);
        task.execute();
	}
	
	private static void publishFriendNewsItem(final FriendNewsItem item, final Fragment fragment) {
	    Session session = Session.getActiveSession();

        Bundle postParams = new Bundle();
        String name = "I am going to an event ";
        if (item.getUserAttending() == Attending.WANTS_TO_GO) {
        	name = "I want to go to an event ";
        }
        name += "'" + item.getTrackName() + "' on eventseeker";
        postParams.putString("name", name);
        String caption = "";
    	if (item.getVenueName() != null) {
    		caption += "at " + item.getVenueName();
    	}
    	if (item.getStartTime() != null) {
    		caption += " on " + new SimpleDateFormat("EEEE, MMM d").format(item.getStartTime().getStartDate());
    	}
        postParams.putString("caption", caption);
        String description = " ";
        postParams.putString("description", description);
        
        String link = "http://eventseeker.com/event/" + item.getTrackId();
        postParams.putString("link", link);

        if (item.doesValidImgUrlExist()) {
        	String imgUrl = item.getHighResImgUrl();
        	if (imgUrl == null) {
        		imgUrl = item.getLowResImgUrl();
        	}
        	if (imgUrl == null) {
        		imgUrl = item.getMobiResImgUrl();
        	}
            postParams.putString("picture", imgUrl);
        }

        Request.Callback callback = new Request.Callback() {
        	
            public void onCompleted(Response response) {
            	Log.d(TAG, "response = " + response.toString());
            	String postId = null;

                GraphObject graphObject = response.getGraphObject();
                if (graphObject != null) {
                	JSONObject graphResponse = graphObject.getInnerJSONObject();
                    try {
                        postId = graphResponse.getString("id");
                        postId = postId.split("_")[1];
                        
                    } catch (JSONException e) {
                        Log.i(TAG, "JSON error "+ e.getMessage());
                    }
                    
                } else {
                	Log.d(TAG, "graphObj = null");
                }
                
                new UserTracker((EventSeekr) FragmentUtil.getActivity(fragment).getApplication(), 
                		UserTrackingItemType.event, item.getTrackId(), item.getUserAttending().getValue(), 
                		postId, UserTrackingType.Add).execute();
            }
        };

        Request request = new Request(session, "me/feed", postParams, HttpMethod.POST, callback);
        RequestAsyncTask task = new RequestAsyncTask(request);
        task.execute();
	}
}
