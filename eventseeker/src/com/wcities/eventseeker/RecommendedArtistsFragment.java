package com.wcities.eventseeker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.wcities.eventseeker.DrawerListFragment.DrawerListFragmentListener;
import com.wcities.eventseeker.GeneralDialogFragment.DialogBtnClickListener;
import com.wcities.eventseeker.RadioGroupDialogFragment.OnValueSelectedListener;
import com.wcities.eventseeker.SettingsFragment.OnSettingsItemClickedListener;
import com.wcities.eventseeker.SettingsFragment.SettingsItem;
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
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.ArtistTrackingListener;
import com.wcities.eventseeker.interfaces.LoadArtistsListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public class RecommendedArtistsFragment extends FragmentLoadableFromBackStack implements OnClickListener, 
		LoadArtistsListener, LoadItemsInBackgroundListener, DialogBtnClickListener, ArtistTrackingListener {

	private static final String TAG = FollowingParentFragment.class.getName();

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

	private View rltDummyLyt;
	private ScrollView scrlVRootNoItemsFoundWithAction;

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
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		((MainActivity) FragmentUtil.getActivity(this)).setVStatusBarColor(R.color.colorPrimaryDark);
		((MainActivity) FragmentUtil.getActivity(this)).setVStatusBarVisibility(View.VISIBLE);

		View v = inflater.inflate(R.layout.fragment_following, null);
		lstView = (ListView) v.findViewById(android.R.id.list);
		
		rltDummyLyt = v.findViewById(R.id.rltDummyLyt);
		scrlVRootNoItemsFoundWithAction = (ScrollView) v.findViewById(R.id.scrlVRootNoItemsFoundWithAction);
		
		btnFollowAll = (Button) v.findViewById(R.id.btnFollowMoreArtists);
		btnFollowAll.setText(R.string.btn_follow_all);
		btnFollowAll.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				GeneralDialogFragment generalDialogFragment = GeneralDialogFragment.newInstance(
					RecommendedArtistsFragment.this,
					res.getString(R.string.dialog_title_follow_all),  
					/**
					 * 'artistList.size() - 1' is being passed as number of Artist as 1 value is 
					 * null to show progress dialog.
					 */
					String.format(res.getString(R.string.dialog_msg_follow_all), artistList.size() - 1),
					res.getString(R.string.dialog_btn_no),
					res.getString(R.string.dialog_btn_yes));
				generalDialogFragment.show(
					((ActionBarActivity) FragmentUtil.getActivity(RecommendedArtistsFragment.this))
					.getSupportFragmentManager(), DIALOG_FOLLOW_ALL);				
			}
		});

		v.findViewById(R.id.btnAction).setOnClickListener(this);
		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		// Log.d(TAG, "onActivityCreated()");
		initListView();
	}

	private void initListView() {
		if (artistList == null) {
			artistList = new ArrayList<Artist>();
			artistList.add(null);

			alphaNumIndexer = new HashMap<Character, Integer>();
			indices = new ArrayList<Character>();

			myArtistListAdapter = new MyArtistListAdapter(FragmentUtil.getActivity(this), artistList, null,
					alphaNumIndexer, indices, this, this, this, AdapterFor.recommended);

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
		/**
		 * 05-01-2015:
		 * NOTE:
		 * The Caching Logic is commented for the future, if it is required in future. Although, it needs
		 * some modification as per the Recommended Artist screen, as in this screen the artist list could 
		 * be sorted as per the artist similarity score(default) or as per artist name. So, the logic crashes.
		 * The logic has been taken from Following screen.
		 */
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
		 * try-catch is used to handle case where even before we get call back
		 * to this function, user leaves this screen.
		 */
		try {
			lstView.setVisibility(View.GONE);
			if (btnFollowAll != null) {
				btnFollowAll.setVisibility(View.GONE);
			}

		} catch (IllegalStateException e) {
			Log.e(TAG, "" + e.getMessage());
			e.printStackTrace();
		}

		if (wcitiesId == null) {
			rltDummyLyt.setVisibility(View.VISIBLE);
			TextView txtNoItemsFound = (TextView) rltDummyLyt.findViewById(R.id.txtNoItemsFound);
			txtNoItemsFound.setText(res.getString(R.string.no_items_found_pls_login) 
					+ " the list of artists you are following.");

		} else {
			scrlVRootNoItemsFoundWithAction.setVisibility(View.VISIBLE);
			((TextView) scrlVRootNoItemsFoundWithAction.findViewById(R.id.txtNoItemsHeading))
					.setText(res.getString(R.string.personalize_your_experience));
			((TextView) scrlVRootNoItemsFoundWithAction.findViewById(R.id.txtNoItemsMsg))
					.setText(res.getString(R.string.sync_accounts_or_search_for_artists));
			((Button) scrlVRootNoItemsFoundWithAction.findViewById(R.id.btnAction))
					.setText(res.getString(R.string.navigation_drawer_item_sync_accounts));
			((ImageView) scrlVRootNoItemsFoundWithAction.findViewById(R.id.imgNoItems))
					.setImageDrawable(res.getDrawable(R.drawable.no_artists_following));
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
			((EventSeekr) FragmentUtil.getActivity(this).getApplication()).updateFirstTimeLaunch(false);
			/*
			 * ((DrawerListFragmentListener)FragmentUtil.getActivity(this)).
			 * onDrawerItemSelected(
			 * MainActivity.INDEX_NAV_ITEM_CONNECT_ACCOUNTS, null);
			 */
			((OnSettingsItemClickedListener) FragmentUtil.getActivity(this))
					.onSettingsItemClicked(SettingsItem.SYNC_ACCOUNTS, null);
			break;

		default:
			break;
		}
	}

	@Override
	public String getScreenName() {
		return "";
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
	}

	@Override
	public void onArtistTracking(Context context, Artist artist) {
		EventSeekr eventseekr = FragmentUtil.getApplication(this);
		if (artist.getAttending() == Attending.NotTracked) {
			artist.updateAttending(Attending.Tracked, eventseekr);
			new UserTracker(Api.OAUTH_TOKEN, eventseekr, UserTrackingItemType.artist, artist.getId()).execute();
			//The below notifyDataSetChange will change the status of following Checkbox for current Artist
			myArtistListAdapter.notifyDataSetChanged();
			
			GeneralDialogFragment generalDialogFragment = GeneralDialogFragment.newInstance(
					this,
					res.getString(R.string.follow_artist),  
					res.getString(R.string.artist_saved),  
					res.getString(R.string.btn_Ok),
					null);
			generalDialogFragment.show(
					((ActionBarActivity) FragmentUtil.getActivity(this)).getSupportFragmentManager(), DIALOG_ARTIST_SAVED);
			
		} else {			
			artist.updateAttending(Attending.NotTracked, eventseekr);
			new UserTracker(Api.OAUTH_TOKEN, eventseekr, UserTrackingItemType.artist, artist.getId(), 
					Attending.NotTracked.getValue(), UserTrackingType.Edit).execute();
		}
	}

}