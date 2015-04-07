package com.wcities.eventseeker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import android.app.Activity;
import android.content.res.Resources;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.RecyclerListener;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wcities.eventseeker.GeneralDialogFragment.DialogBtnClickListener;
import com.wcities.eventseeker.adapter.MyArtistListAdapter;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingItemType;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.LoadMyArtists;
import com.wcities.eventseeker.asynctask.UserTracker;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.Artist.Attending;
import com.wcities.eventseeker.core.FollowingList;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.ArtistTrackingListener;
import com.wcities.eventseeker.interfaces.AsyncTaskListener;
import com.wcities.eventseeker.interfaces.CustomSharedElementTransitionSource;
import com.wcities.eventseeker.interfaces.DrawerListFragmentListener;
import com.wcities.eventseeker.interfaces.FullScrnProgressListener;
import com.wcities.eventseeker.interfaces.LoadArtistsListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.VersionUtil;
import com.wcities.eventseeker.util.ViewUtil;

public abstract class FollowingParentFragment extends FragmentLoadableFromBackStack implements ArtistTrackingListener,
		LoadArtistsListener, LoadItemsInBackgroundListener, DialogBtnClickListener, 
		CustomSharedElementTransitionSource, FullScrnProgressListener, AsyncTaskListener<Void> {


	private static final String TAG = FollowingParentFragment.class.getName();

	private String wcitiesId;
	private FollowingList cachedFollowingList;

	private LoadMyArtists loadMyArtists;
	protected MyArtistListAdapter myArtistListAdapter;

	protected List<Artist> artistList;
	private SortedSet<Integer> artistIds;
	
	private Map<Character, Integer> alphaNumIndexer;
	private List<Character> indices;

	private AbsListView absListView;

	private View rltRootNoContentFound;
	private RelativeLayout rltLytPrgsBar;
	
	/**
	 * Using its instance variable since otherwise calling getResources() directly from fragment from 
	 * callback methods is dangerous in a sense that it may throw java.lang.IllegalStateException: 
	 * Fragment not attached to Activity, if user has already left this fragment & 
	 * then changed the orientation.
	 */
	private Resources res;

	protected Button btnFollowMoreArtists;
	
	private List<View> hiddenViews;
	private boolean isOnPushedToBackStackCalled;

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

		if (wcitiesId == null) {
			wcitiesId = ((EventSeekr) FragmentUtil.getActivity(this).getApplication()).getWcitiesId();
			cachedFollowingList = ((EventSeekr) FragmentUtil.getActivity(this).getApplication()).getCachedFollowingList();
		}
		res = getResources();
		hiddenViews = new ArrayList<View>();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_following, null);
		/**
		 * add extra top margin (equal to statusbar height) since we are removing vStatusBar from onStart() 
		 * even though we want search screen to have this statusbar. We had to mark VStatusBar as GONE from 
		 * onStart() so that on transition to details screen doesn't cause jumping effect on this screen, as we remove vStatusBar 
		 * on detail screen when this screen is visible in the background
		 */
		if (VersionUtil.isApiLevelAbove18()) {
			Resources res = FragmentUtil.getResources(this);
			RelativeLayout rltFollowMoreArtist = (RelativeLayout) v.findViewById(R.id.rltFollowMoreArtist);
			RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) rltFollowMoreArtist.getLayoutParams();
			lp.topMargin = res.getDimensionPixelSize(R.dimen.common_t_mar_pad_for_all_layout) 
					+ ViewUtil.getStatusBarHeight(res);
			rltFollowMoreArtist.setLayoutParams(lp);
		}

		rltRootNoContentFound = v.findViewById(R.id.rltRootNoContentFound);
		rltLytPrgsBar = (RelativeLayout) v.findViewById(R.id.rltLytPrgsBar);
		rltLytPrgsBar.setBackgroundResource(R.drawable.bg_no_content_overlay);
		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		// Log.d(TAG, "onActivityCreated()");
		
		absListView = getScrollableView();

		if (artistList == null) {
			artistList = new ArrayList<Artist>();
			artistList.add(null);
			artistIds = new TreeSet<Integer>();
			
			alphaNumIndexer = new HashMap<Character, Integer>();
			indices = new ArrayList<Character>();

			myArtistListAdapter = new MyArtistListAdapter(FragmentUtil.getActivity(this), artistList, null, 
					alphaNumIndexer, indices, this, this, this, this);

			loadItemsInBackground();

		} else {
			if (artistList.isEmpty()) {
				showNoArtistFound();				
			}
			myArtistListAdapter.updateContext(FragmentUtil.getActivity(this));
		}
		
		absListView.setRecyclerListener(new RecyclerListener() {
			
			@Override
			public void onMovedToScrapHeap(View view) {
				freeUpBitmapMemory(view);
			}
		});
		
		// this is because setAdapter method on 'absListView' is added from api level 11. 
		// So, that 'absListView.setAdapter(artistListAdapter);' was throwing java.lang.NoSuchMethodError
		if (absListView instanceof ListView) {
			((ListView) absListView).setAdapter(myArtistListAdapter);
			
		} else {			
			((GridView) absListView).setAdapter(myArtistListAdapter);
		}
		
		absListView.setScrollingCacheEnabled(false);
		absListView.setFastScrollEnabled(true);
		/*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			absListView.setFastScrollAlwaysVisible(false);
		}*/
	}
	
	@Override
	public void onStart() {
		super.onStart();

		if (!isOnTop()) {
			return;
		}
		
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
	public void loadItemsInBackground() {
		loadMyArtists = new LoadMyArtists(Api.OAUTH_TOKEN, wcitiesId, artistList, myArtistListAdapter, 
				cachedFollowingList, artistIds, indices, alphaNumIndexer, this);
		myArtistListAdapter.setLoadArtists(loadMyArtists);
		AsyncTaskUtil.executeAsyncTask(loadMyArtists, true);
	}
	
	protected void freeUpBitmapMemory(View view) {
		if (view.getTag().equals(AppConstants.TAG_CONTENT)) {
			((ImageView) view.findViewById(R.id.imgItem)).setImageBitmap(null);
		}
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		for (int i = absListView.getFirstVisiblePosition(), j = 0; 
				i <= absListView.getLastVisiblePosition(); 
				i++, j++) {
			freeUpBitmapMemory(absListView.getChildAt(j));
		}
	}
	
	@Override
	public void showNoArtistFound() {
		/**
		 * try-catch is used to handle case where even before we get call back to this function, user leaves 
		 * this screen.
		 */
		try {
			absListView.setVisibility(View.GONE);
			
		} catch (IllegalStateException e) {
			Log.e(TAG, "" + e.getMessage());
			e.printStackTrace();
		}
		
		TextView txtNoContentMsg = (TextView) rltRootNoContentFound.findViewById(R.id.txtNoItemsMsg);
		txtNoContentMsg.setText(R.string.following_no_content);
		txtNoContentMsg.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_list_follow, 0, 0);
		
		((ImageView) rltRootNoContentFound.findViewById(R.id.imgPhone))
			.setImageDrawable(res.getDrawable(R.drawable.ic_following_no_content));
		
		rltRootNoContentFound.setVisibility(View.VISIBLE);		
	}
	
	@Override
	public String getScreenName() {
		return ScreenNames.FOLLOWING_SCREEN;
	}
	
	@Override
	public void onArtistTracking(Artist artist, int position) {
		EventSeekr eventseekr = FragmentUtil.getApplication(this);
		artist.updateAttending(Attending.NotTracked, eventseekr );
		new UserTracker(Api.OAUTH_TOKEN, eventseekr, UserTrackingItemType.artist, artist.getId(), 
				Attending.NotTracked.getValue(), UserTrackingType.Edit).execute();
	}

	@Override
	public void doPositiveClick(String dialogTag) {
		/**
		 * Here in this case we would have passed the position of the Artist as its tag.
		 */
		myArtistListAdapter.unTrackArtistAt(Integer.parseInt(dialogTag));
	}

	@Override
	public void doNegativeClick(String dialogTag) {
		/*artistListAdapter.notifyDataSetChanged();*/
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
		//onStop();
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
		if (loadMyArtists != null && loadMyArtists.getStatus() != Status.FINISHED) {
			loadMyArtists.cancel(true);
		}
	}
	
	protected abstract AbsListView getScrollableView();
}
