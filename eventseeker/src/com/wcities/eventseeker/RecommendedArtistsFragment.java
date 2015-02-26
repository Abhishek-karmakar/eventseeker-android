package com.wcities.eventseeker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.AsyncTask.Status;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.RecyclerListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.Session;
import com.facebook.SessionState;
import com.wcities.eventseeker.DrawerListFragment.DrawerListFragmentListener;
import com.wcities.eventseeker.GeneralDialogFragment.DialogBtnClickListener;
import com.wcities.eventseeker.RadioGroupDialogFragment.OnValueSelectedListener;
import com.wcities.eventseeker.ShareOnFBDialogFragment.OnFacebookShareClickedListener;
import com.wcities.eventseeker.adapter.MyArtistListAdapter;
import com.wcities.eventseeker.adapter.MyArtistListAdapter.AdapterFor;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingItemType;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.LoadRecommendedArtists;
import com.wcities.eventseeker.asynctask.UserTracker;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.Artist.Attending;
import com.wcities.eventseeker.custom.fragment.PublishArtistFragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.ArtistTrackingListener;
import com.wcities.eventseeker.interfaces.AsyncTaskListener;
import com.wcities.eventseeker.interfaces.CustomSharedElementTransitionSource;
import com.wcities.eventseeker.interfaces.FullScrnProgressListener;
import com.wcities.eventseeker.interfaces.LoadArtistsListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.FbUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.VersionUtil;
import com.wcities.eventseeker.util.ViewUtil;

public class RecommendedArtistsFragment extends PublishArtistFragmentLoadableFromBackStack implements 
		LoadArtistsListener, LoadItemsInBackgroundListener, DialogBtnClickListener, ArtistTrackingListener,
		OnFacebookShareClickedListener, CustomSharedElementTransitionSource, FullScrnProgressListener,
		AsyncTaskListener<Void> {

	private static final String TAG = RecommendedArtistsFragment.class.getName();

	private static final String DIALOG_ARTIST_SAVED = "dialogArtistSaved";
	private static final String DIALOG_FOLLOW_ALL = "dialogFollowAll";
	private static final String DIALOG_SORT_BY = "dialogSortBy";

	public static enum SortRecommendedArtist {
		name(0),
		score(1);
		
		int value;
		private SortRecommendedArtist(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
		
		public static SortRecommendedArtist getSortTypeBy(int value) {
			for (SortRecommendedArtist sortBy : values()) {
				if (sortBy.getValue() == value) {
					return sortBy;
				}
			}
			return null;
		}
	}
	
	private String wcitiesId;

	private LoadRecommendedArtists loadRecommendedArtists;
	protected MyArtistListAdapter myArtistListAdapter;

	private List<Artist> artistList;

	private Map<Character, Integer> alphaNumIndexer;
	private List<Character> indices;

	private ListView lstView;

	private TextView txtNoItemsFound;
	private RelativeLayout rltLytPrgsBar;

	/**
	 * Using its instance variable since otherwise calling getResources()
	 * directly from fragment from callback methods is dangerous in a sense that
	 * it may throw java.lang.IllegalStateException: Fragment not attached to
	 * Activity, if user has already left this fragment & then changed the
	 * orientation.
	 */
	private Resources res;

	private Button btnFollowAll;

	private SortRecommendedArtist sortBy = SortRecommendedArtist.score;

	private int fbCallCountForSameArtist = 0;

	private Artist artistToBeSaved;
	
	private List<View> hiddenViews;
	private boolean isOnPushedToBackStackCalled;

	private View rltFollowAll, rltLayoutRoot;
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (!(activity instanceof DrawerListFragmentListener)) {
			throw new ClassCastException(activity.toString() + " must implement DrawerListFragmentListener");
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		setHasOptionsMenu(true);
		if (wcitiesId == null) {
			wcitiesId = ((EventSeekr) FragmentUtil.getActivity(this).getApplication()).getWcitiesId();
		}
		res = getResources();
		hiddenViews = new ArrayList<View>();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_following, null);
		rltFollowAll = (RelativeLayout) v.findViewById(R.id.rltFollowMoreArtist);
		/**
		 * add extra top margin (equal to statusbar height) since we are removing vStatusBar from onStart() 
		 * even though we want search screen to have this statusbar. We had to mark VStatusBar as GONE from 
		 * onStart() so that on transition to details screen doesn't cause jumping effect on this screen, as we remove vStatusBar 
		 * on detail screen when this screen is visible in the background
		 */
		if (VersionUtil.isApiLevelAbove18()) {
			Resources res = FragmentUtil.getResources(this);
			RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) rltFollowAll.getLayoutParams();
			lp.topMargin = res.getDimensionPixelSize(R.dimen.common_t_mar_pad_for_all_layout) 
					+ ViewUtil.getStatusBarHeight(res);
			rltFollowAll.setLayoutParams(lp);
		}
		lstView = (ListView) v.findViewById(android.R.id.list);
		
		txtNoItemsFound = (TextView) v.findViewById(R.id.txtNoItemsFound);
		
		btnFollowAll = (Button) v.findViewById(R.id.btnFollowMoreArtists);
		btnFollowAll.setText(R.string.btn_follow_all);
		btnFollowAll.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (artistList.isEmpty()) {
					return;
				}
				GeneralDialogFragment generalDialogFragment = GeneralDialogFragment.newInstance(
					RecommendedArtistsFragment.this,
					res.getString(R.string.dialog_title_follow_all),  
					/**
					 * 'artistList.size() - 1' is being passed as number of Artist as 1 value is 
					 * null to show progress dialog.
					 */
					String.format(res.getString(R.string.dialog_msg_follow_all), artistList.size() - 1),
					res.getString(R.string.my_events_al_no),
					res.getString(R.string.yes), false);
				generalDialogFragment.show(
					((ActionBarActivity) FragmentUtil.getActivity(RecommendedArtistsFragment.this))
					.getSupportFragmentManager(), DIALOG_FOLLOW_ALL);				
			}
		});

		rltLayoutRoot = v.findViewById(R.id.rltLayoutRoot);
		rltLytPrgsBar = (RelativeLayout) v.findViewById(R.id.rltLytPrgsBar);
		rltLytPrgsBar.setBackgroundResource(R.drawable.bg_no_content_overlay);
		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		// Log.d(TAG, "onActivityCreated()");
		initListView();
	}

	private void initListView() {
		if (lstView.getVisibility() != View.VISIBLE) {
			lstView.setVisibility(View.VISIBLE);
			rltLayoutRoot.setBackgroundResource(Color.WHITE);
			txtNoItemsFound.setVisibility(View.GONE);
		}
		if (artistList == null) {
			artistList = new ArrayList<Artist>();
			artistList.add(null);

			alphaNumIndexer = new HashMap<Character, Integer>();
			indices = new ArrayList<Character>();

			myArtistListAdapter = new MyArtistListAdapter(FragmentUtil.getActivity(this), artistList, null,
					alphaNumIndexer, indices, this, this, this, AdapterFor.recommended, this);

			loadItemsInBackground();

		} else {
			if (artistList.isEmpty()) {
				showNoArtistFound();
			}
			myArtistListAdapter.updateContext(FragmentUtil.getActivity(this));
		}

		lstView.setRecyclerListener(new RecyclerListener() {

			@Override
			public void onMovedToScrapHeap(View view) {
				freeUpBitmapMemory(view);
			}
		});

		lstView.setAdapter(myArtistListAdapter);
		lstView.setScrollingCacheEnabled(false);
		lstView.setFastScrollEnabled(true);
		/*
		 * if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
		 * lstView.setFastScrollAlwaysVisible(false); }
		 */		
	}
	
	@Override
	public void onStart() {
		if (!isOnTop()) {
			callOnlySuperOnStart = true;
			super.onStart();
			return;
		}
		
		super.onStart();
		MainActivity ma = (MainActivity) FragmentUtil.getActivity(this);
		ma.setToolbarElevation(0);
		/**
		 * Even though we want status bar in this case, mark it gone to have smoother transition to detail fragment
		 * & prevent jumping effect on search screen, caused due to removal of status bar on detail screen when this 
		 * search screen is visible in background.
		 */
		ma.setVStatusBarVisibility(View.GONE, AppConstants.INVALID_ID);
		ma.setVStatusBarLayeredVisibility(View.VISIBLE, R.color.colorPrimaryDark);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		MainActivity ma = (MainActivity) FragmentUtil.getActivity(this);
		ma.setToolbarElevation(ma.getResources().getDimensionPixelSize(R.dimen.action_bar_elevation));
		ma.setVStatusBarVisibility(View.VISIBLE, R.color.colorPrimaryDark);
		ma.setVStatusBarLayeredVisibility(View.GONE, AppConstants.INVALID_ID);
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
					if (RecommendedArtistsFragment.this.sortBy == sortBy) {
						return;
					}
					RecommendedArtistsFragment.this.sortBy = sortBy;
					artistList = null;
					
					initListView();
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
		loadRecommendedArtists = new LoadRecommendedArtists(Api.OAUTH_TOKEN, wcitiesId, artistList, myArtistListAdapter, 
				this, sortBy);
		myArtistListAdapter.setLoadArtists(loadRecommendedArtists);
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
		for (int i = lstView.getFirstVisiblePosition(), j = 0; i <= lstView
				.getLastVisiblePosition(); i++, j++) {
			freeUpBitmapMemory(lstView.getChildAt(j));
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
			lstView.setVisibility(View.GONE);
			if (rltFollowAll != null) {
				rltFollowAll.setVisibility(View.GONE);
			}
			
		} catch (IllegalStateException e) {
			Log.e(TAG, "" + e.getMessage());
			e.printStackTrace();
		}
		
		rltLayoutRoot.setBackgroundResource(R.drawable.bg_no_content_overlay);
		txtNoItemsFound.setText(R.string.no_artist_found);
		txtNoItemsFound.setVisibility(View.VISIBLE);		
	}

	@Override
	public String getScreenName() {
		return "Recommended Artists Screen";
	}

	@Override
	public void doPositiveClick(String dialogTag) {
		if (dialogTag.equals(DIALOG_FOLLOW_ALL)) {
			EventSeekr eventSeekr = FragmentUtil.getApplication(RecommendedArtistsFragment.this);
			List<Long> ids = new ArrayList<Long>();
			for (Artist artist : artistList) {
				if (artist != null && artist.getAttending() == Attending.NotTracked) {
					ids.add((long) artist.getId());
					artist.updateAttending(Attending.Tracked, eventSeekr);
				}
			}
			if (ids.size() > 1) {
				new UserTracker(Api.OAUTH_TOKEN, eventSeekr, UserTrackingItemType.artist, ids).execute();
				myArtistListAdapter.notifyDataSetChanged();
			
			} else if (ids.size() == 1) {
				new UserTracker(Api.OAUTH_TOKEN, eventSeekr, UserTrackingItemType.artist, ids.get(0)).execute();				
				myArtistListAdapter.notifyDataSetChanged();
			}
			
		} else {
			//This is for Remove Artist Dialog
			myArtistListAdapter.unTrackArtistAt(Integer.parseInt(dialogTag));
			myArtistListAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public void doNegativeClick(String dialogTag) {
		/*if (FieldValidationUtil.isNumber(dialogTag)) {
			//This is for Remove Artist Dialog
			myArtistListAdapter.notifyDataSetChanged();
		}*/
		//Log.d(TAG, "doNegativeClick");
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
	public void onArtistTracking(Context context, Artist artist) {
		EventSeekr eventseekr = FragmentUtil.getApplication(this);
		if (artist.getAttending() == Attending.NotTracked) {
			artist.updateAttending(Attending.Tracked, eventseekr);
			new UserTracker(Api.OAUTH_TOKEN, eventseekr, UserTrackingItemType.artist, artist.getId()).execute();
			//The below notifyDataSetChange will change the status of following CheckBox for current Artist
			myArtistListAdapter.notifyDataSetChanged();
			
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
		 * Not calling onStop() to prevent toolbar color changes occurring in between
		 * the transition
		 */
		super.onStop();
		
		setMenuVisibility(false);
		isOnPushedToBackStackCalled = true;
	}

	@Override
	public void onPoppedFromBackStack() {
		if (isOnPushedToBackStackCalled) {
			isOnPushedToBackStackCalled = false;
			
			// to update statusbar visibility
			onStart();
			// to call onFragmentResumed(Fragment) of MainActivity (to update title, current fragment tag, etc.)
			onResume();
			
			for (Iterator<View> iterator = hiddenViews.iterator(); iterator.hasNext();) {
				View view = iterator.next();
				view.setVisibility(View.VISIBLE);
			}
			hiddenViews.clear();
			
			setMenuVisibility(true);
		}
	}

	@Override
	public boolean isOnTop() {
		return !isOnPushedToBackStackCalled;
	}

	@Override
	public void displayFullScrnProgress() {
		rltLytPrgsBar.setVisibility(View.VISIBLE);
	}

	@Override
	public void onTaskCompleted(Void... params) {
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