package com.wcities.eventseeker;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView.RecyclerListener;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.Session;
import com.facebook.SessionState;
import com.wcities.eventseeker.GeneralDialogFragment.DialogBtnClickListener;
import com.wcities.eventseeker.ShareOnFBDialogFragment.OnFacebookShareClickedListener;
import com.wcities.eventseeker.adapter.ArtistListAdapterWithoutIndexerTab;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingItemType;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.LoadArtistsByCategory;
import com.wcities.eventseeker.asynctask.UserTracker;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.Artist.Attending;
import com.wcities.eventseeker.core.Artist.Genre;
import com.wcities.eventseeker.custom.fragment.PublishArtistFragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.ArtistTrackingListener;
import com.wcities.eventseeker.interfaces.AsyncTaskListener;
import com.wcities.eventseeker.interfaces.FullScrnProgressListener;
import com.wcities.eventseeker.interfaces.LoadArtistsListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.FbUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public class SelectedArtistCategoryFragmentTab extends PublishArtistFragmentLoadableFromBackStack implements ArtistTrackingListener, 
		LoadArtistsListener, LoadItemsInBackgroundListener, DialogBtnClickListener, OnFacebookShareClickedListener,
		FullScrnProgressListener, AsyncTaskListener<Void>, OnClickListener {

	private static final String TAG = SelectedArtistCategoryFragmentTab.class.getName();

	private static final String DIALOG_FOLLOW_ALL = "dialogFollowAll";
	private static final String DIALOG_ARTIST_SAVED = "dialogArtistSaved";

	private static final int NUM_COLUMNS_PORTRAIT = 2;
	private static final int NUM_COLUMNS_LANDSCAPE = 3;
	
	private String wcitiesId;

	private Genre genre;

	private LoadArtistsByCategory loadCategorialArtists;
	private ArtistListAdapterWithoutIndexerTab artistListAdapter;

	private List<Artist> artistList;

	private TextView txtNoItemsFound;
	private ImageView imgPrgOverlay;
	//private Button btnFollowAll;	
	private GridView grdvArtists;

	//private RelativeLayout rltFollowAll;
	private RelativeLayout rltLytPrgsBar;

	private int fbCallCountForSameArtist = 0;

	private Artist artistToBeSaved;

	private View rltLayoutRoot;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);

		genre = (Genre) getArguments().getSerializable(BundleKeys.GENRE);
		
		if (wcitiesId == null) {
			wcitiesId = ((EventSeekr) FragmentUtil.getActivity(this).getApplication()).getWcitiesId();
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_following_tab, null);
		//rltFollowAll = (RelativeLayout) v.findViewById(R.id.rltFollowMoreArtist);
		
		txtNoItemsFound = (TextView) v.findViewById(R.id.txtNoItemsFound);
		
		grdvArtists = (GridView) v.findViewById(R.id.grdvFollowing);
		grdvArtists.setNumColumns(FragmentUtil.getResources(this).getConfiguration()
			.orientation == Configuration.ORIENTATION_PORTRAIT ? NUM_COLUMNS_PORTRAIT : NUM_COLUMNS_LANDSCAPE);
						
		/*btnFollowAll = (Button) v.findViewById(R.id.btnFollowMoreArtists);
		btnFollowAll.setText(R.string.btn_follow_all);
		btnFollowAll.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (artistList.isEmpty()) {
					return;
				}
				Resources res = FragmentUtil.getResources(SelectedArtistCategoryFragmentTab.this);
				GeneralDialogFragment generalDialogFragment = GeneralDialogFragment.newInstance(
					SelectedArtistCategoryFragmentTab.this,
					res.getString(R.string.dialog_title_follow_all),  
					*//**
					 * 'artistList.size() - 1' is being passed as number of Artist as 1 value is 
					 * null to show progress dialog.
					 *//*
					String.format(res.getString(R.string.dialog_msg_follow_all), artistList.size() - 1),
					res.getString(R.string.my_events_al_no),
					res.getString(R.string.yes), false);
				generalDialogFragment.show(
					((ActionBarActivity) FragmentUtil.getActivity(SelectedArtistCategoryFragmentTab.this))
					.getSupportFragmentManager(), DIALOG_FOLLOW_ALL);
			}
		});*/
		
		rltLayoutRoot = v.findViewById(R.id.rltLayoutRoot);
		rltLytPrgsBar = (RelativeLayout) v.findViewById(R.id.rltLytPrgsBar);
		imgPrgOverlay = (ImageView) rltLytPrgsBar.findViewById(R.id.imgPrgOverlay);
		
		v.findViewById(R.id.btnSyncAccounts).setOnClickListener(this);
		v.findViewById(R.id.btnRecommended).setOnClickListener(this);
		v.findViewById(R.id.btnSearch).setOnClickListener(this);
		
		CheckBox btnPopularArtists = (CheckBox) v.findViewById(R.id.btnPopularArtists);
		btnPopularArtists.setOnClickListener(this);
		btnPopularArtists.setChecked(true);
		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		//Log.d(TAG, "onActivityCreated()");
		
		if (artistList == null) {
			artistList = new ArrayList<Artist>();
			artistList.add(null);
			
			artistListAdapter = new ArtistListAdapterWithoutIndexerTab(this, artistList, null, this, this, this);

			loadItemsInBackground();

		} else {
			if (artistList.isEmpty()) {
				showNoArtistFound();				
			}
		}
		
		grdvArtists.setRecyclerListener(new RecyclerListener() {
			
			@Override
			public void onMovedToScrapHeap(View view) {
				freeUpBitmapMemory(view);
			}
		});
		
		grdvArtists.setAdapter(artistListAdapter);
		grdvArtists.setScrollingCacheEnabled(false);
		grdvArtists.setFastScrollEnabled(true);
	}
	
	@Override
	public void onResume() {
		super.onResume(AppConstants.INVALID_INDEX, 
			FragmentUtil.getResources(this).getString(getArguments().getInt(BundleKeys.SCREEN_TITLE)));
	}

	@Override
	public void loadItemsInBackground() {
		loadCategorialArtists = new LoadArtistsByCategory(Api.OAUTH_TOKEN, wcitiesId, artistList, artistListAdapter, this, genre);
		artistListAdapter.setLoadArtists(loadCategorialArtists);
		AsyncTaskUtil.executeAsyncTask(loadCategorialArtists, true);
	}
	
	protected void freeUpBitmapMemory(View view) {
		if (view.getTag().equals(AppConstants.TAG_CONTENT)) {
			((ImageView) view.findViewById(R.id.imgItem)).setImageBitmap(null);
		}
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		for (int i = grdvArtists.getFirstVisiblePosition(), j = 0; 
				i <= grdvArtists.getLastVisiblePosition(); 
				i++, j++) {
			freeUpBitmapMemory(grdvArtists.getChildAt(j));
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		//Log.d(TAG, "onDestroy()");
		if (loadCategorialArtists != null && loadCategorialArtists.getStatus() != Status.FINISHED) {
			loadCategorialArtists.cancel(true);
		}
	}
	
	@Override
	public void showNoArtistFound() {
		/**
		 * try-catch is used to handle case where even before we get call back to this function, user leaves 
		 * this screen.
		 */
		try {
			grdvArtists.setVisibility(View.GONE);
			/*if (rltFollowAll != null) {
				rltFollowAll.setVisibility(View.GONE);
			}*/
			
		} catch (IllegalStateException e) {
			Log.e(TAG, "" + e.getMessage());
			e.printStackTrace();
		}
		
		rltLayoutRoot.setBackgroundResource(R.drawable.ic_no_content_background_overlay);
		txtNoItemsFound.setText(R.string.no_artist_found);
		txtNoItemsFound.setVisibility(View.VISIBLE);		
	}

	@Override
	public void doPositiveClick(String dialogTag) {
		if (dialogTag.equals(DIALOG_FOLLOW_ALL)) {
			EventSeekr eventSeekr = FragmentUtil.getApplication(SelectedArtistCategoryFragmentTab.this);
			List<Long> ids = new ArrayList<Long>();
			for (Artist artist : artistList) {
				if (artist != null && artist.getAttending() == Attending.NotTracked) {
					ids.add((long) artist.getId());
					artist.updateAttending(Attending.Tracked, eventSeekr);
				}
			}
			if (ids.size() > 1) {
				new UserTracker(Api.OAUTH_TOKEN, eventSeekr, UserTrackingItemType.artist, ids).execute();
				artistListAdapter.notifyDataSetChanged();
			
			} else if (ids.size() == 1) {
				new UserTracker(Api.OAUTH_TOKEN, eventSeekr, UserTrackingItemType.artist, ids.get(0)).execute();				
				artistListAdapter.notifyDataSetChanged();
			}
			
		} else {
			//This is for Remove Artist Dialog
			artistListAdapter.unTrackArtistAt(Integer.parseInt(dialogTag));
			artistListAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public void doNegativeClick(String dialogTag) {
		/*if (FieldValidationUtil.isNumber(dialogTag)) {
			//This is for Remove Artist Dialog
			artistListAdapter.notifyDataSetChanged();
		}*/
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
	public void onArtistTracking(Artist artist, int position) {
		EventSeekr eventseekr = FragmentUtil.getApplication(this);
		if (artist.getAttending() == Attending.NotTracked) {
			artist.updateAttending(Attending.Tracked, eventseekr);
			new UserTracker(Api.OAUTH_TOKEN, eventseekr, UserTrackingItemType.artist, artist.getId()).execute();
			//The below notifyDataSetChange will change the status of following CheckBox for current Artist
			artistListAdapter.notifyDataSetChanged();

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
	public void onPublishPermissionGranted() {
		
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
	public void onTaskCompleted(Void... params) {
		// free up memory
		rltLytPrgsBar.setBackgroundResource(0);
		rltLytPrgsBar.setVisibility(View.INVISIBLE);
	}

	@Override
	public void displayFullScrnProgress() {
		rltLytPrgsBar.setVisibility(View.VISIBLE);
		imgPrgOverlay.setVisibility(View.VISIBLE);
	}

	@Override
	public void onClick(View v) {
		Intent intent = null;
		switch (v.getId()) {
		case R.id.btnSyncAccounts:
			((CheckBox) v).setChecked(false);
			intent = new Intent(FragmentUtil.getApplication(this), ConnectAccountsActivityTab.class);
			break;

		case R.id.btnPopularArtists:
			((CheckBox) v).setChecked(true);
			intent = new Intent(FragmentUtil.getApplication(this), PopularArtistsActivityTab.class);
			break;

		case R.id.btnRecommended:
			((CheckBox) v).setChecked(false);
			intent = new Intent(FragmentUtil.getApplication(this), RecommendedArtistsActivityTab.class);
			break;

		case R.id.btnSearch:
			((CheckBox) v).setChecked(false);
			((BaseActivityTab) FragmentUtil.getActivity(this)).expandSearchView();
			break;
		}
		if (intent != null) {
			startActivity(intent);
			FragmentUtil.getActivity(this).finish();
		}
	}

	@Override
	public String getScreenName() {
		return ScreenNames.POPULAR_ARTISTS_CATEGORIES_SCREEN + FragmentUtil.getResources(this)
				.getString(getArguments().getInt(BundleKeys.SCREEN_TITLE));
	}
}
