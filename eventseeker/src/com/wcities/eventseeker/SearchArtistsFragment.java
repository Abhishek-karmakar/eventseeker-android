package com.wcities.eventseeker;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.facebook.Session;
import com.facebook.SessionState;
import com.wcities.eventseeker.GeneralDialogFragment.DialogBtnClickListener;
import com.wcities.eventseeker.SearchFragment.SearchFragmentChildListener;
import com.wcities.eventseeker.ShareOnFBDialogFragment.OnFacebookShareClickedListener;
import com.wcities.eventseeker.adapter.ArtistListAdapter;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingItemType;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.LoadArtists;
import com.wcities.eventseeker.asynctask.UserTracker;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.Artist.Attending;
import com.wcities.eventseeker.custom.fragment.PublishArtistListFragment;
import com.wcities.eventseeker.interfaces.ArtistListener;
import com.wcities.eventseeker.interfaces.ArtistTrackingListener;
import com.wcities.eventseeker.interfaces.AsyncTaskListener;
import com.wcities.eventseeker.interfaces.CustomSharedElementTransitionSource;
import com.wcities.eventseeker.interfaces.FullScrnProgressListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.interfaces.PublishListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.FbUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public class SearchArtistsFragment extends PublishArtistListFragment implements SearchFragmentChildListener, 
		PublishListener, LoadItemsInBackgroundListener, DialogBtnClickListener, ArtistTrackingListener, 
		CustomSharedElementTransitionSource, OnFacebookShareClickedListener, AsyncTaskListener<Void>,
		FullScrnProgressListener {

	private static final String TAG = SearchArtistsFragment.class.getSimpleName();

	private static final String DIALOG_ARTIST_SAVED = "dialogArtistSaved";

	private String query;
	private LoadArtists loadArtists;
	private ArtistListAdapter<String> artistListAdapter;
	
	private RelativeLayout rltLytPrgsBar;
	
	private List<Artist> artistList;

	private int fbCallCountForSameArtist;

	private Artist artistToBeSaved;
	
	private List<View> hiddenViews;
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (!(activity instanceof ArtistListener)) {
            throw new ClassCastException(activity.toString() + " must implement ArtistListener");
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		hiddenViews = new ArrayList<View>();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.list_with_centered_progress, null);

		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			int pad = getResources().getDimensionPixelSize(R.dimen.tab_bar_margin_fragment_custom_tabs);
			v.findViewById(android.R.id.list).setPadding(pad, 0, pad, 0);
		}
		
		rltLytPrgsBar = (RelativeLayout) v.findViewById(R.id.rltLytPrgsBar);
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		//Log.d(TAG, "onActivityCreated()");
		
		if (artistList == null) {
			artistList = new ArrayList<Artist>();
			//artistListAdapter = new ArtistListAdapter<String>(FragmentUtil.getActivity(this), artistList, null, this);
			artistListAdapter = new ArtistListAdapter<String>(FragmentUtil.getActivity(this), artistList, null, this, 
					this, this, this);
	        Bundle args = getArguments();
			if (args != null && args.containsKey(BundleKeys.QUERY)) {
				artistList.add(null);
				query = args.getString(BundleKeys.QUERY);
				loadItemsInBackground();
			}
			
		} else {
			artistListAdapter.updateContext(FragmentUtil.getActivity(this));
		}
		artistListAdapter.setAddPadding(true);
		setListAdapter(artistListAdapter);
        getListView().setDivider(null);
        //getListView().setBackgroundResource(R.drawable.story_space);
	}
	
	@Override
	public void loadItemsInBackground() {
		loadArtists = new LoadArtists(Api.OAUTH_TOKEN, artistList, artistListAdapter, 
				FragmentUtil.getApplication(this).getWcitiesId(), this);
		artistListAdapter.setLoadArtists(loadArtists);
		AsyncTaskUtil.executeAsyncTask(loadArtists, true, query);
	}
	
	private void refresh(String newQuery) {
		Log.d(TAG, "refresh()");
		// if user selection has changed then only reset the list
		if (query == null || !query.equals(newQuery)) {
			//Log.d(TAG, "query == null || !query.equals(newQuery)");

			query = newQuery;
			artistListAdapter.setArtistsAlreadyRequested(0);
			artistListAdapter.setMoreDataAvailable(true);
			
			if (loadArtists != null && loadArtists.getStatus() != Status.FINISHED) {
				loadArtists.cancel(true);
			}
			
			artistList.clear();
			artistList.add(null);
			artistListAdapter.notifyDataSetChanged();
			
			loadItemsInBackground();
		}
	}
	
	@Override
	public void onQueryTextSubmit(String query) {
		Log.d(TAG, "onQueryTextSubmit(), query = " + query);
		refresh(query);
	}

	@Override
	public void onArtistTracking(Context context, Artist artist) {
		EventSeekr eventseekr = FragmentUtil.getApplication(this);
		if (artist.getAttending() == Attending.NotTracked) {
			artist.updateAttending(Attending.Tracked, eventseekr);
			new UserTracker(Api.OAUTH_TOKEN, eventseekr, UserTrackingItemType.artist, artist.getId()).execute();
			//The below notifyDataSetChange will change the status of following CheckBox for current Artist
			artistListAdapter.notifyDataSetChanged();

			Resources res = FragmentUtil.getResources(this);
			ShareOnFBDialogFragment dialogFragment = ShareOnFBDialogFragment.newInstance(this);
			dialogFragment.show(((ActionBarActivity) FragmentUtil.getActivity(this)).getSupportFragmentManager(), 
						DIALOG_ARTIST_SAVED + ":" + artist.getId());
			
		} else {			
			artist.updateAttending(Attending.NotTracked, eventseekr);
			new UserTracker(Api.OAUTH_TOKEN, eventseekr, UserTrackingItemType.artist, artist.getId(), 
					Attending.NotTracked.getValue(), UserTrackingType.Edit).execute();
		}
	}

	@Override
	public void doPositiveClick(String dialogTag) {
		//This is for Remove Artist Dialog
		artistListAdapter.unTrackArtistAt(Integer.parseInt(dialogTag));
	}

	@Override
	public void doNegativeClick(String dialogTag) {
		/*if (dialogTag.contains(DIALOG_ARTIST_SAVED)) {
			String strId = dialogTag.substring(dialogTag.indexOf(":") + 1);
			//Log.d(TAG, "strId : " + strId);
			for (Artist artist : artistList) {
				if (artist != null && artist.getId() == Integer.parseInt(strId)) {					
					fbCallCountForSameArtist = 0;
					artistToBeSaved = artist;
					FbUtil.handlePublishArtist(this, this, AppConstants.PERMISSIONS_FB_PUBLISH_EVT_OR_ART, 
							AppConstants.REQ_CODE_FB_PUBLISH_EVT_OR_ART, artist);
					break;
				}
			}
		}*/
	}

	@Override
	public void call(Session session, SessionState state, Exception exception) {
		if (artistToBeSaved == null) {
			return;
		}
		fbCallCountForSameArtist++;
		/**
		 * To prevent infinite loop when network is off & we are calling requestPublishPermissions() of FbUtil.
		 */
		if (fbCallCountForSameArtist < AppConstants.MAX_FB_CALL_COUNT_FOR_SAME_EVT_OR_ART) {
			FbUtil.call(session, state, exception, this, this, AppConstants.PERMISSIONS_FB_PUBLISH_EVT_OR_ART, 
					AppConstants.REQ_CODE_FB_PUBLISH_EVT_OR_ART, artistToBeSaved);
			
		} else {
			fbCallCountForSameArtist = 0;
			setPendingAnnounce(false);
		}
	}

	@Override
	public void onPublishPermissionGranted() {
		
	}

	@Override
	public void addViewsToBeHidden(View... views) {
		for (int i = 0; i < views.length; i++) {
			hiddenViews.add(views[i]);
		}
	}

	@Override
	public void hideSharedElements() {
		for (Iterator<View> iterator = hiddenViews.iterator(); iterator.hasNext();) {
			View view = iterator.next();
			view.setVisibility(View.INVISIBLE);
		}
	}

	@Override
	public void onPushedToBackStack() {
		/**
		 * to remove facebook callback.
		 */
		onStop();
		
		((CustomSharedElementTransitionSource) getParentFragment()).onPushedToBackStack();
	}

	@Override
	public void onPoppedFromBackStack() {
		/**
		 * to add facebook callback.
		 */
		onStart();
		
		for (Iterator<View> iterator = hiddenViews.iterator(); iterator.hasNext();) {
			View view = iterator.next();
			view.setVisibility(View.VISIBLE);
		}
		hiddenViews.clear();
	}

	@Override
	public void onFacebookShareClicked(String dialogTag) {
		if (dialogTag.contains(DIALOG_ARTIST_SAVED)) {
			String strId = dialogTag.substring(dialogTag.indexOf(":") + 1);
			//Log.d(TAG, "strId : " + strId);
			for (Artist artist : artistList) {
				if (artist != null && artist.getId() == Integer.parseInt(strId)) {					
					fbCallCountForSameArtist = 0;
					artistToBeSaved = artist;
					FbUtil.handlePublishArtist(this, this, AppConstants.PERMISSIONS_FB_PUBLISH_EVT_OR_ART, 
							AppConstants.REQ_CODE_FB_PUBLISH_EVT_OR_ART, artist);
					break;
				}
			}
		}
	}

	@Override
	public boolean isOnTop() {
		return false;
	}

	@Override
	public void onTaskCompleted(Void... params) {
		// remove full screen progressbar
		rltLytPrgsBar.setVisibility(View.INVISIBLE);
	}

	@Override
	public void displayFullScrnProgress() {
		rltLytPrgsBar.setVisibility(View.VISIBLE);
	}
}
