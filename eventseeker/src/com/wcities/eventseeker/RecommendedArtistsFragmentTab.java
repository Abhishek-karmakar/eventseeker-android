package com.wcities.eventseeker;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView.RecyclerListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.Session;
import com.facebook.SessionState;
import com.wcities.eventseeker.GeneralDialogFragment.DialogBtnClickListener;
import com.wcities.eventseeker.RadioGroupDialogFragment.OnValueSelectedListener;
import com.wcities.eventseeker.ShareOnFBDialogFragment.OnFacebookShareClickedListener;
import com.wcities.eventseeker.adapter.ArtistListAdapterWithoutIndexerTab;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingItemType;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.LoadRecommendedArtists;
import com.wcities.eventseeker.asynctask.UserTracker;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.Enums.SortRecommendedArtist;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.Artist.Attending;
import com.wcities.eventseeker.custom.fragment.PublishArtistFragment;
import com.wcities.eventseeker.interfaces.ArtistTrackingListener;
import com.wcities.eventseeker.interfaces.AsyncTaskListener;
import com.wcities.eventseeker.interfaces.FullScrnProgressListener;
import com.wcities.eventseeker.interfaces.LoadArtistsListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.FbUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public class RecommendedArtistsFragmentTab extends PublishArtistFragment implements OnClickListener,
		LoadArtistsListener, LoadItemsInBackgroundListener, DialogBtnClickListener, ArtistTrackingListener,
		OnFacebookShareClickedListener, FullScrnProgressListener, AsyncTaskListener<Void> {

	private static final String TAG = RecommendedArtistsFragmentTab.class.getName();
	private static final String DIALOG_ARTIST_SAVED = "dialogArtistSaved";
	private static final String DIALOG_FOLLOW_ALL = "dialogFollowAll";
	private static final String DIALOG_SORT_BY = "dialogSortBy";

	private static final int NUM_COLUMNS_PORTRAIT = 2;
	private static final int NUM_COLUMNS_LANDSCAPE = 3;

	private String wcitiesId;

	private LoadRecommendedArtists loadRecommendedArtists;
	protected ArtistListAdapterWithoutIndexerTab artistListAdapter;

	private List<Artist> artistList;

	private GridView grdvFollowing;

	private TextView txtNoItemsFound;
	private RelativeLayout rltLytPrgsBar;

	//private Button btnFollowAll;

	private SortRecommendedArtist sortBy = SortRecommendedArtist.score;

	private int fbCallCountForSameArtist = 0;

	private Artist artistToBeSaved;
	
	private View /*rltFollowAll,*/ rltLayoutRoot;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		setHasOptionsMenu(true);
		if (wcitiesId == null) {
			wcitiesId = ((EventSeekr) FragmentUtil.getActivity(this).getApplication()).getWcitiesId();
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_following_tab, null);
		//rltFollowAll = (RelativeLayout) v.findViewById(R.id.rltFollowMoreArtist);

		grdvFollowing = (GridView) v.findViewById(R.id.grdvFollowing);
		grdvFollowing.setNumColumns(FragmentUtil.getResources(this).getConfiguration()
				.orientation == Configuration.ORIENTATION_PORTRAIT ? NUM_COLUMNS_PORTRAIT : NUM_COLUMNS_LANDSCAPE);
						
		txtNoItemsFound = (TextView) v.findViewById(R.id.txtNoItemsFound);
		
		/*btnFollowAll = (Button) v.findViewById(R.id.btnFollowMoreArtists);
		btnFollowAll.setText(R.string.btn_follow_all);
		btnFollowAll.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (artistList.isEmpty()) {
					return;
				}
				GeneralDialogFragment generalDialogFragment = GeneralDialogFragment.newInstance(
					RecommendedArtistsFragmentTab.this,
					res.getString(R.string.dialog_title_follow_all),  
					*//**
					 * 'artistList.size() - 1' is being passed as number of Artist as 1 value is 
					 * null to show progress dialog.
					 *//*
					String.format(res.getString(R.string.dialog_msg_follow_all), artistList.size() - 1),
					res.getString(R.string.my_events_al_no),
					res.getString(R.string.yes), false);
				generalDialogFragment.show(
					((ActionBarActivity) FragmentUtil.getActivity(RecommendedArtistsFragmentTab.this))
					.getSupportFragmentManager(), DIALOG_FOLLOW_ALL);				
			}
		});*/

		rltLayoutRoot = v.findViewById(R.id.rltLayoutRoot);
		rltLytPrgsBar = (RelativeLayout) v.findViewById(R.id.rltLytPrgsBar);
		
		v.findViewById(R.id.btnSyncAccounts).setOnClickListener(this);
		v.findViewById(R.id.btnPopularArtists).setOnClickListener(this);
		v.findViewById(R.id.btnSearch).setOnClickListener(this);
		
		CheckBox btnRecommended = (CheckBox) v.findViewById(R.id.btnRecommended);
		btnRecommended.setOnClickListener(this);
		btnRecommended.setChecked(true);
		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		// Log.d(TAG, "onActivityCreated()");
		initGridView();
	}

	private void initGridView() {
		if (grdvFollowing.getVisibility() != View.VISIBLE) {
			grdvFollowing.setVisibility(View.VISIBLE);
			rltLayoutRoot.setBackgroundColor(Color.WHITE);
			txtNoItemsFound.setVisibility(View.GONE);
		}
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

		grdvFollowing.setRecyclerListener(new RecyclerListener() {

			@Override
			public void onMovedToScrapHeap(View view) {
				freeUpBitmapMemory(view);
			}
		});

		grdvFollowing.setAdapter(artistListAdapter);
		grdvFollowing.setScrollingCacheEnabled(false);
		grdvFollowing.setFastScrollEnabled(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_recommended, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_sort_by:
			Bundle args = new Bundle();
			args.putInt(RadioGroupDialogFragment.DEFAULT_VALUE, sortBy.getValue());
			args.putString(RadioGroupDialogFragment.DIALOG_TITLE, 
					FragmentUtil.getResources(this).getString(R.string.sort_by));
			args.putString(RadioGroupDialogFragment.DIALOG_RDB_ZEROTH_TEXT, 
					FragmentUtil.getResources(this).getString(R.string.a_z));
			args.putString(RadioGroupDialogFragment.DIALOG_RDB_FIRST_TEXT, 
					FragmentUtil.getResources(this).getString(R.string.recommendation_score));
			args.putSerializable(RadioGroupDialogFragment.ON_VALUE_SELECTED_LISETER, new OnValueSelectedListener() {

				@Override
				public void onValueSelected(int selectedValue) {
					SortRecommendedArtist sortBy = SortRecommendedArtist.getSortTypeBy(selectedValue);
					if (RecommendedArtistsFragmentTab.this.sortBy == sortBy) {
						return;
					}
					RecommendedArtistsFragmentTab.this.sortBy = sortBy;
					artistList = null;
					
					initGridView();
				}
			});
			
			RadioGroupDialogFragment dialogFragment = new RadioGroupDialogFragment();
			dialogFragment.setArguments(args);
			dialogFragment.show(((ActionBarActivity) FragmentUtil.getActivity(this))
					.getSupportFragmentManager(), DIALOG_SORT_BY);
			return true;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void loadItemsInBackground() {
		loadRecommendedArtists = new LoadRecommendedArtists(Api.OAUTH_TOKEN, wcitiesId, artistList, artistListAdapter, 
				this, sortBy);
		artistListAdapter.setLoadArtists(loadRecommendedArtists);
		AsyncTaskUtil.executeAsyncTask(loadRecommendedArtists, true);
	}

	protected void freeUpBitmapMemory(View view) {
		if (view.getTag().equals(AppConstants.TAG_CONTENT)) {
			((ImageView) view.findViewById(R.id.imgItem)).setImageBitmap(null);
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		for (int i = grdvFollowing.getFirstVisiblePosition(), j = 0; i <= grdvFollowing.getLastVisiblePosition(); i++, j++) {
			freeUpBitmapMemory(grdvFollowing.getChildAt(j));
		}
		super.onDestroyView();
	}

	@Override
	public void showNoArtistFound() {
		/**
		 * try-catch is used to handle case where even before we get call back to this function, user leaves 
		 * this screen.
		 */
		try {
			grdvFollowing.setVisibility(View.GONE);
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
			EventSeekr eventSeekr = FragmentUtil.getApplication(RecommendedArtistsFragmentTab.this);
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
	public void doNegativeClick(String dialogTag) {}

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
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.btnSyncAccounts:
				((CheckBox) v).setChecked(false);
				Intent intent = new Intent(FragmentUtil.getApplication(this), ConnectAccountsActivityTab.class);
				startActivity(intent);
				FragmentUtil.getActivity(this).finish();
				break;
				
			case R.id.btnPopularArtists:
				((CheckBox) v).setChecked(false);
				intent = new Intent(FragmentUtil.getApplication(this), PopularArtistsActivityTab.class);
				startActivity(intent);
				FragmentUtil.getActivity(this).finish();
				break;
				
			case R.id.btnRecommended:
				((CheckBox) v).setChecked(true);
				break;
				
			case R.id.btnSearch:
				((CheckBox) v).setChecked(false);
				((BaseActivityTab) FragmentUtil.getActivity(this)).expandSearchView();
				break;
		}
	}
	
	@Override
	public void onPublishPermissionGranted() {}

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
	public void displayFullScrnProgress() {
		rltLytPrgsBar.setBackgroundResource(R.drawable.ic_no_content_background_overlay);
		rltLytPrgsBar.setVisibility(View.VISIBLE);
	}

	@Override
	public void onTaskCompleted(Void... params) {
		Log.d(TAG, "onTaskCompleted");
		rltLytPrgsBar.setVisibility(View.INVISIBLE);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		//Log.d(TAG, "onDestroy()");
		if (loadRecommendedArtists != null && loadRecommendedArtists.getStatus() != Status.FINISHED) {
			loadRecommendedArtists.cancel(true);
		}
	}
}