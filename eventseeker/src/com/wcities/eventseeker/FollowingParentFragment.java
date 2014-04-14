package com.wcities.eventseeker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Build;
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
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.wcities.eventseeker.DrawerListFragment.DrawerListFragmentListener;
import com.wcities.eventseeker.adapter.MyArtistListAdapter;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.LoadMyArtists;
import com.wcities.eventseeker.asynctask.LoadMyArtists.LoadMyArtistsListener;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.FollowingList;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public abstract class FollowingParentFragment extends FragmentLoadableFromBackStack implements 
		OnClickListener, LoadMyArtistsListener, LoadItemsInBackgroundListener {

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
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_following, null);
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
					alphaNumIndexer, indices, this);

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
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			absListView.setFastScrollAlwaysVisible(true);
		}
	}

	@Override
	public void loadItemsInBackground() {
		loadMyArtists = new LoadMyArtists(wcitiesId, artistList, myArtistListAdapter, 
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
		super.onDestroyView();
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

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.btnAction:
			// set firstTimeLaunch=false so as to keep facebook & google sign in rows visible.
			((EventSeekr)FragmentUtil.getActivity(this).getApplication()).updateFirstTimeLaunch(false);
			((DrawerListFragmentListener)FragmentUtil.getActivity(this)).onDrawerItemSelected(
					MainActivity.INDEX_NAV_ITEM_CONNECT_ACCOUNTS, null);
			break;

		default:
			break;
		}
	}
	
	@Override
	public String getScreenName() {
		return "Following Screen";
	}
	
	protected abstract AbsListView getScrollableView();
}
