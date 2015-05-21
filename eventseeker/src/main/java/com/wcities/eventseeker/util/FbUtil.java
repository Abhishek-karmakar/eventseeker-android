package com.wcities.eventseeker.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.util.Log;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.FacebookRequestError;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.UserTracker;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.SharedPrefKeys;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.FriendNewsItem;
import com.wcities.eventseeker.interfaces.PublishListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class FbUtil {

	private static final String TAG = FbUtil.class.getSimpleName();

	/**
	 * error not occured
	 */
	private static final int FB_ERROR_CODE_NO_ERROR = -1;
	
	/**
	 * TODO:This issue is occurring more frequently for the Recommended Artist & Selected Artist Category
	 * need to test it thoroughly and resolve it.
	 */
	/**
	 * Your message couldn't be sent because it includes content 
	 * that other people on Facebook have reported as abusive.
	 */
	private static final int FB_ERROR_CODE_368 = 368;

	/**
	 * Feed action request limit reached.
	 */
	private static final int FB_ERROR_CODE_341 = 341;

	/**
	 * Invalid Parameter.
	 */
	private static final int FB_ERROR_CODE_100 = 100;

	public static boolean hasUserLoggedInBefore(Context context) {
		//Log.d(TAG, "hasUserLoggedInBefore()");
		SharedPreferences pref = context.getSharedPreferences(
                AppConstants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
		return pref.contains(SharedPrefKeys.FACEBOOK_USER_ID);
	}
	
	public static void logout(EventSeekr eventSeekr) {
		LoginManager.getInstance().logOut();
		//clear your preferences if saved
		eventSeekr.removeFbUserInfo();
	}
	
	public static void login(Fragment fragment) {
		//Log.d(TAG, "login()");
		LoginManager.getInstance().logInWithReadPermissions(fragment, AppConstants.PERMISSIONS_FB_LOGIN);
    }
	
	/**
	 * gets friend's image url
	 * @param friendId
	 * @return
	 */
	public static String getFriendImgUrl(String friendId) {
		return "http://graph.facebook.com/v2.0/" + friendId + "/picture?type=square";
		//return "http://graph.facebook.com/" + friendId + "/picture?type=square&height=100&width=100";
	}
	
	public static void makeMeRequest(AccessToken accessToken, GraphRequest.GraphJSONObjectCallback graphJSONObjectCallback) {
		GraphRequest request = GraphRequest.newMeRequest(accessToken, graphJSONObjectCallback);
		request.executeAsync();
	}
	
	/**
	 * NOTE: THIS CAN BE TESTED WITH PODUCTION BUILD ONLY
	 * @param fbPublishListener
	 * @param fragment
	 * @param permissions
	 * @param friendNewsItem
	 */
	public static void handlePublishFriendNewsItem(PublishListener fbPublishListener, Fragment fragment,
												   List<String> permissions, FriendNewsItem friendNewsItem) {
		//Log.d(TAG, "handlePublish()");
		if (canPublishNow(fragment, permissions)) {
			friendNewsItem.updateUserAttendingToNewUserAttending();
			Toast.makeText(FragmentUtil.getActivity(fragment), R.string.saving_event, Toast.LENGTH_SHORT).show();
			publishFriendNewsItem(friendNewsItem, fragment, fbPublishListener);
		}
	}
	
	/**
	 * NOTE: THIS CAN BE TESTED WITH PODUCTION BUILD ONLY
	 * @param fbPublishListener
	 * @param fragment
	 * @param permissions
	 * @param event
	 */
	public static void handlePublishEvent(PublishListener fbPublishListener, Fragment fragment,
										  List<String> permissions, Event event) {
		//Log.d(TAG, "handlePublishEvent()");
		if (canPublishNow(fragment, permissions)) {
			event.updateAttendingToNewAttending();
			/**
			 * Call onPublishPermissionGranted() afterwards, because for my events screen if user is saving
			 * event from following/recommended tab then we want to refresh saved events tab which we are doing from
			 * onPublishPermissionGranted() call sequence. If we call onPublishPermissionGranted() before actually
			 * sending updated usertracker value to eventseeker server, refreshed saved events call might not generate
			 * newly saved event since usertracker call might not have finished yet.
			 */
			Toast.makeText(FragmentUtil.getActivity(fragment), R.string.saving_event, Toast.LENGTH_SHORT).show();
			publishEvent(event, fragment, fbPublishListener);
		}
	}
	
	public static void handlePublishArtist(Fragment fragment, List<String> permissions, Artist artist) {
		//Log.d(TAG, "handlePublishArtist()");
		if (canPublishNow(fragment, permissions)) {
			publishArtist(artist, fragment, FB_ERROR_CODE_NO_ERROR);
		}
	}

	private static boolean canPublishNow(Fragment fragment, List<String> permissions) {
		if (!hasPermission(permissions)) {
			Log.d(TAG, "no permission");
			// we get top level parent fragment here since onActivityResult() is not called in nested fragments.
			Fragment fragmentToHandleActivityResult = FragmentUtil.getTopLevelParentFragment(fragment);
			LoginManager.getInstance().logInWithPublishPermissions(fragmentToHandleActivityResult, permissions);
			return false;
		}
		return true;
	}
	
	public static boolean hasPermission(List<String> permissions) {
		//Log.d(TAG, "hasPermission()");
		AccessToken accessToken = AccessToken.getCurrentAccessToken();
		/*Set<String> tmpPermissions = accessToken.getPermissions();
		for (Iterator<String> iterator = tmpPermissions.iterator(); iterator.hasNext(); ) {
			Log.d(TAG, "" + iterator.next());
		}*/
		return accessToken != null && accessToken.getPermissions().containsAll(permissions);
    }
	
	/**
	 * NOTE: THIS CAN BE TESTED WITH PODUCTION BUILD ONLY
	 * @param event
	 * @param fragment
	 */
	private static void publishEvent(final Event event, final Fragment fragment, final PublishListener fbPublishListener) {
		//Log.d(TAG, "publishEvent()");
		String attendingAction = AppConstants.ACTION_ADD;
		GraphRequest request = GraphRequest.newPostRequest(AccessToken.getCurrentAccessToken(), "me/" + attendingAction, null, new GraphRequest.Callback() {
			@Override
			public void onCompleted(GraphResponse graphResponse) {
				Log.d(TAG, "response = " + graphResponse.toString());
				FacebookRequestError error = graphResponse.getError();
				if (error != null) {
					Log.i(TAG, error.getErrorMessage());
				}
				String postId = null;
				JSONObject jsonObject = graphResponse.getJSONObject();
				if (jsonObject != null) {
					try {
						postId = jsonObject.getString("id");

					} catch (JSONException e) {
						Log.i(TAG, "JSON error "+ e.getMessage());
					}

				} else {
					Log.d(TAG, "jsonObject = null");
				}

				//Log.d(TAG, "track event");
				new UserTracker(Api.OAUTH_TOKEN, (EventSeekr) FragmentUtil.getActivity(fragment).getApplication(),
						UserInfoApi.UserTrackingItemType.event, event.getId(), event.getAttending().getValue(), postId,
						UserInfoApi.UserTrackingType.Add) {

					protected void onPostExecute(Void result) {
						fbPublishListener.onPublishPermissionGranted();
					}

				}.execute();
			}
		});
		Bundle postParams = request.getParameters();
		String link = event.getEventUrl();
		if (link == null) {
			link = "http://eventseeker.com/event/" + event.getId();
		}
		postParams.putString("event", link);
		request.setParameters(postParams);
		request.executeAsync();
	}

	/**
	 * NOTE: THIS CAN BE TESTED WITH PODUCTION BUILD ONLY
	 * @param item
	 * @param fragment
	 */
	private static void publishFriendNewsItem(final FriendNewsItem item, final Fragment fragment, final PublishListener fbPublishListener) {
		String attendingAction = AppConstants.ACTION_ADD;
		GraphRequest request = GraphRequest.newPostRequest(AccessToken.getCurrentAccessToken(), "me/" + attendingAction, null, new GraphRequest.Callback() {
			@Override
			public void onCompleted(GraphResponse graphResponse) {
				Log.d(TAG, "response = " + graphResponse.toString());
				String postId = null;
				JSONObject jsonObject = graphResponse.getJSONObject();
				if (jsonObject != null) {
					try {
						postId = jsonObject.getString("id");

					} catch (JSONException e) {
						Log.i(TAG, "JSON error "+ e.getMessage());
					}

				} else {
					Log.d(TAG, "jsonObject = null");
				}

				new UserTracker(Api.OAUTH_TOKEN, (EventSeekr) FragmentUtil.getActivity(fragment).getApplication(),
						UserInfoApi.UserTrackingItemType.event, item.getTrackId(), item.getUserAttending().getValue(),
						postId, UserInfoApi.UserTrackingType.Add) {

					protected void onPostExecute(Void result) {
						fbPublishListener.onPublishPermissionGranted();
					}

				}.execute();
			}
		});
		Bundle postParams = request.getParameters();
		String link = "http://eventseeker.com/event/" + item.getTrackId();
		postParams.putString("event", link);
		request.setParameters(postParams);
		request.executeAsync();
	}

	/**
	 * 19-01-2015:
	 * NOTE:
	 * Added addLink parameter. Because in some Artist while posting to FB. It was giving error code : 100
	 * saying 'Invalid Parameter' on debugging found without 'Link' those artists were getting posted without
	 * link. So, added this parameter and passing it 'false' from callback So, that atleast other data gets posted.
	 * @param artist
	 * @param fragment
	 * @param errorCode
	 */
	public static void publishArtist(final Artist artist, final Fragment fragment, final int errorCode) {
		//Log.d(TAG, "publishArtist");
		GraphRequest request = GraphRequest.newPostRequest(AccessToken.getCurrentAccessToken(), "me/feed", null, new GraphRequest.Callback() {
			@Override
			public void onCompleted(GraphResponse graphResponse) {
				Log.d(TAG, "response = " + graphResponse.toString());
				String postId = null;
				JSONObject jsonObject = graphResponse.getJSONObject();

				if (jsonObject == null) {
					Log.d(TAG, "jsonObject = null");
					if (errorCode != FB_ERROR_CODE_NO_ERROR) {
						return;
					}
					int eC = graphResponse.getError().getErrorCode();
					publishArtist(artist, fragment, eC);
				}
			}
		});
		Bundle postParams = request.getParameters();
		postParams.putString("caption", "EVENTSEEKER.COM");
		if (errorCode != FB_ERROR_CODE_368) {
			String description = artist.getDescription() == null ? " " : artist.getDescription();
			description = Html.fromHtml(description).toString();
			postParams.putString("description", description);
		}
		//Log.d(TAG, "description : " + description);
		String link = artist.getArtistUrl();
		if (link == null) {
			link = "http://eventseeker.com/artist/" + artist.getId();
			//Log.d(TAG, "link created : " + link);
		}
		postParams.putString("link", link);

		if (artist.doesValidImgUrlExist()) {
			String imgUrl = artist.getLowResImgUrl();
			if (imgUrl == null && errorCode == FB_ERROR_CODE_100) {
				imgUrl = artist.getMobiResImgUrl();
			}
			if (imgUrl == null) {
				imgUrl = artist.getHighResImgUrl();
			}
			//Log.d(TAG, "pic : " + imgUrl);
			postParams.putString("picture", imgUrl);
		}

		request.setParameters(postParams);
		request.executeAsync();
	}
}
