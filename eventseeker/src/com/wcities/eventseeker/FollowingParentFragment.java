package com.wcities.eventseeker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.RecyclerListener;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.wcities.eventseeker.DrawerListFragment.DrawerListFragmentListener;
import com.wcities.eventseeker.GeneralDialogFragment.DialogBtnClickListener;
import com.wcities.eventseeker.SettingsFragment.OnSettingsItemClickedListener;
import com.wcities.eventseeker.SettingsFragment.SettingsItem;
import com.wcities.eventseeker.adapter.MyArtistListAdapter;
import com.wcities.eventseeker.adapter.MyArtistListAdapter.AdapterFor;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingItemType;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.LoadMyArtists;
import com.wcities.eventseeker.asynctask.UserTracker;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.Artist.Attending;
import com.wcities.eventseeker.core.FollowingList;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.ArtistTrackingListener;
import com.wcities.eventseeker.interfaces.CustomSharedElementTransitionSource;
import com.wcities.eventseeker.interfaces.LoadArtistsListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.VersionUtil;
import com.wcities.eventseeker.util.ViewUtil;

public abstract class FollowingParentFragment extends FragmentLoadableFromBackStack implements ArtistTrackingListener,
		OnClickListener, LoadArtistsListener, LoadItemsInBackgroundListener, DialogBtnClickListener, CustomSharedElementTransitionSource {

	private static final String TAG = FollowingParentFragment.class.getName();

	private String wcitiesId;
	private FollowingList cachedFollowingList;

	private LoadMyArtists loadMyArtists;
	protected MyArtistListAdapter myArtistListAdapter;

	private List<Artist> artistList;
	private SortedSet<Integer> artistIds;
	
	private Map<Character, Integer> alphaNumIndexer;
	private List<Character> indices;

	private AbsListView absListView;

	private View rltDummyLyt;
	private ScrollView scrlVRootNoItemsFoundWithAction;
	
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
		rltDummyLyt = v.findViewById(R.id.rltDummyLyt);
		scrlVRootNoItemsFoundWithAction = (ScrollView) v.findViewById(R.id.scrlVRootNoItemsFoundWithAction);
		v.findViewById(R.id.btnAction).setOnClickListener(this);
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
					alphaNumIndexer, indices, this, this, this, AdapterFor.following, this);

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
		// So, that 'absListView.setAdapter(myArtistListAdapter);' was throwing java.lang.NoSuchMethodError
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
			if (btnFollowMoreArtists != null) {
				btnFollowMoreArtists.setVisibility(View.GONE);
			}
			
		} catch (IllegalStateException e) {
			Log.e(TAG, "" + e.getMessage());
			e.printStackTrace();
		}

		if (wcitiesId == null) {
			rltDummyLyt.setVisibility(View.VISIBLE);
			TextView txtNoItemsFound = (TextView)rltDummyLyt.findViewById(R.id.txtNoItemsFound);
			txtNoItemsFound.setText(res.getString(R.string.no_items_found_pls_login) + " the list of artists you are following.");
			
		} else {
			scrlVRootNoItemsFoundWithAction.setVisibility(View.VISIBLE);
			((TextView)scrlVRootNoItemsFoundWithAction.findViewById(R.id.txtNoItemsHeading)).setText(
					res.getString(R.string.personalize_your_experience));
			((TextView)scrlVRootNoItemsFoundWithAction.findViewById(R.id.txtNoItemsMsg)).setText(
					res.getString(R.string.sync_accounts_or_search_for_artists));
			((Button)scrlVRootNoItemsFoundWithAction.findViewById(R.id.btnAction)).setText(
					res.getString(R.string.navigation_drawer_item_sync_accounts));
			((ImageView)scrlVRootNoItemsFoundWithAction.findViewById(R.id.imgNoItems)).setImageDrawable(
					res.getDrawable(R.drawable.no_artists_following));
		}
	}

	/**
	 * TODO: if it is required
	 */
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.btnAction:
			// set firstTimeLaunch=false so as to keep facebook & google sign in rows visible.
			((EventSeekr)FragmentUtil.getActivity(this).getApplication()).updateFirstTimeLaunch(false);
			/*((DrawerListFragmentListener)FragmentUtil.getActivity(this)).onDrawerItemSelected(
					MainActivity.INDEX_NAV_ITEM_CONNECT_ACCOUNTS, null);*/
			((OnSettingsItemClickedListener) FragmentUtil.getActivity(this)).onSettingsItemClicked(SettingsItem.SYNC_ACCOUNTS, null);
			break;

		default:
			break;
		}
	}
	
	@Override
	public String getScreenName() {
		return "Following Screen";
	}
	
	@Override
	public void onArtistTracking(Context context, Artist artist) {
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
		/*myArtistListAdapter.notifyDataSetChanged();*/
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
	
	protected abstract AbsListView getScrollableView();
}
