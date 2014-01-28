package com.wcities.eventseeker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.RecyclerListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.wcities.eventseeker.DrawerListFragment.DrawerListFragmentListener;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi;
import com.wcities.eventseeker.api.UserInfoApi.Type;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.FollowingList;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.ArtistListener;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser.MyItemsList;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public abstract class FollowingParentFragment extends FragmentLoadableFromBackStack implements OnClickListener {

	private static final String TAG = FollowingParentFragment.class.getName();

	private static final int ARTISTS_LIMIT = 10;

	private String wcitiesId;
	private FollowingList cachedFollowingList;

	private LoadArtists loadArtists;
	protected ArtistListAdapter artistListAdapter;

	private int artistsAlreadyRequested;
	private boolean isMoreDataAvailable = true;

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

			artistListAdapter = new ArtistListAdapter(FragmentUtil.getActivity(this));

			loadArtistsInBackground();

		} else {
			if (artistList.isEmpty()) {
				showNoArtistFound();				
			}
			artistListAdapter.setmInflater(FragmentUtil.getActivity(this));
		}
		
		absListView.setRecyclerListener(new RecyclerListener() {
			
			@Override
			public void onMovedToScrapHeap(View view) {
				freeUpBitmapMemory(view);
			}
		});
		
		absListView.setAdapter(artistListAdapter);
		absListView.setScrollingCacheEnabled(false);
		absListView.setFastScrollEnabled(true);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			absListView.setFastScrollAlwaysVisible(true);
		}
	}

	private void loadArtistsInBackground() {
		loadArtists = new LoadArtists();
		AsyncTaskUtil.executeAsyncTask(loadArtists, true);
	}

	private class LoadArtists extends AsyncTask<Void, Void, List<Artist>> {

		@Override
		protected List<Artist> doInBackground(Void... params) {
			List<Artist> tmpArtists = new ArrayList<Artist>();
			UserInfoApi userInfoApi = new UserInfoApi(Api.OAUTH_TOKEN);
			userInfoApi.setLimit(ARTISTS_LIMIT);
			userInfoApi.setAlreadyRequested(artistsAlreadyRequested);
			userInfoApi.setUserId(wcitiesId);

			try {
				JSONObject jsonObject = userInfoApi.getMyProfileInfoFor(Type.myartists);
				UserInfoApiJSONParser jsonParser = new UserInfoApiJSONParser();
				MyItemsList<Artist> myArtistsList = jsonParser.getArtistList(jsonObject);
				tmpArtists = myArtistsList.getItems();

			} catch (ClientProtocolException e) {
				e.printStackTrace();

			} catch (IOException e) {
				e.printStackTrace();

			} catch (JSONException e) {
				e.printStackTrace();
			}

			return tmpArtists;
		}

		@Override
		protected void onPostExecute(List<Artist> tmpArtists) {
			if (!tmpArtists.isEmpty()) {
				handleLoadedArtists(tmpArtists);

				if (tmpArtists.size() < ARTISTS_LIMIT) {
					isMoreDataAvailable = false;
					artistList.remove(artistList.size() - 1);
				}
				
			} else {
				handleLoadedArtists(tmpArtists);
				
				isMoreDataAvailable = false;
				artistList.remove(artistList.size() - 1);
				if (artistList.isEmpty()) {
					showNoArtistFound();
				}
			}
			
			artistListAdapter.notifyDataSetChanged();
		}
		
		private void handleLoadedArtists(List<Artist> tmpArtists) {
			int prevArtistListSize = artistList.size();

			// Add cached followed artists if any
			String fromInclusive = (artistList.get(0) == null) ? 
					null : artistList.get(artistList.size() - 2).getName();
			String toExclusive = (tmpArtists.size() < ARTISTS_LIMIT) ? 
					null : tmpArtists.get(tmpArtists.size() - 1).getName();
			Collection<Artist> mergedArtists = cachedFollowingList.addFollowedArtistsIfAny(tmpArtists, 
					fromInclusive, toExclusive);
			
			artistList.addAll(artistList.size() - 1, mergedArtists);
			artistsAlreadyRequested += tmpArtists.size();
			
			int i = 0;
			for (Iterator<Artist> iterator = mergedArtists.iterator(); iterator.hasNext();) {
				Artist artist = iterator.next();
				if (!artistIds.contains(artist.getId())) {
					artistIds.add(artist.getId());
					
				} else {
					artistList.remove(artist);
					Log.d(TAG, "remove artist - " + artist.getName());
					continue;
				}
				char key = artist.getName().charAt(0);
				if (!indices.contains(key)) {
					indices.add(key);
					/**
					 * subtract 1 from prevArtistListSize to compensate for progressbar null item 
					 * counted in prevArtistListSize
					 */
					alphaNumIndexer.put(key, prevArtistListSize - 1 + i);
				}
				i++;
			}
		}
	}

	/**
	 * SectionIndexer is only required when child class of this fragment has set this adapter on listview 
	 * (not on gridview)
	 */
	protected class ArtistListAdapter extends BaseAdapter implements SectionIndexer {

		private static final String TAG_PROGRESS_INDICATOR = "progressIndicator";
		public static final String TAG_CONTENT = "content";

		private LayoutInflater mInflater;
		private BitmapCache bitmapCache;
		
		public ArtistListAdapter(Context context) {
			mInflater = LayoutInflater.from(context);
			bitmapCache = BitmapCache.getInstance();
		}

		public void setmInflater(Context context) {
			mInflater = LayoutInflater.from(context);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final Artist artist = getItem(position);
			if (artist == null) {
				if (convertView == null || !convertView.getTag().equals(TAG_PROGRESS_INDICATOR)) {
					if(((EventSeekr)FragmentUtil.getActivity(FollowingParentFragment.this)
							.getApplicationContext()).isTablet()) {
						convertView = mInflater.inflate(R.layout.grd_progress_bar, null);
					} else {
						convertView = mInflater.inflate(R.layout.list_progress_bar, null);
					}
					convertView.setTag(TAG_PROGRESS_INDICATOR);
				}

				if ((loadArtists == null || loadArtists.getStatus() == Status.FINISHED) && isMoreDataAvailable) {
					loadArtistsInBackground();
				}

			} else {
				
				if (convertView == null || !convertView.getTag().equals(TAG_CONTENT)) {
					if (((EventSeekr)FragmentUtil.getActivity(FollowingParentFragment.this)
							.getApplicationContext()).isTablet()) {
						convertView = mInflater.inflate(R.layout.fragment_following_artists_list_item_tab, null);
					} else {
						convertView = mInflater.inflate(R.layout.fragment_search_artists_list_item, null);
					}
					convertView.setTag(TAG_CONTENT);
				}

				((TextView) convertView.findViewById(R.id.txtArtistName)).setText(artist.getName());

				if (artist.isOntour()) {
					convertView.findViewById(R.id.txtOnTour).setVisibility(View.VISIBLE);

				} else {
					convertView.findViewById(R.id.txtOnTour).setVisibility(View.INVISIBLE);
				}

				String key = artist.getKey(ImgResolution.LOW);
				Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
				if (bitmap != null) {
					((ImageView) convertView.findViewById(R.id.imgItem)).setImageBitmap(bitmap);

				} else {
					ImageView imgArtist = (ImageView) convertView.findViewById(R.id.imgItem);
					imgArtist.setImageBitmap(null);

					AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
					asyncLoadImg.loadImg(imgArtist, ImgResolution.LOW, (AdapterView) parent, position, artist);
				}

				convertView.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						((ArtistListener) FragmentUtil.getActivity(FollowingParentFragment.this))
								.onArtistSelected(artist);
					}
				});
			}

			return convertView;
		}

		@Override
		public Artist getItem(int position) {
			return artistList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public int getCount() {
			return artistList.size();
		}

		@Override
		public int getPositionForSection(int sectionIndex) {
			//Log.d(TAG, "index = " + alphaNumIndexer.get(indices.get(sectionIndex)));
			if (indices.size() > sectionIndex) {
				/**
				 * for some devices it has wrong sectionIndex (out of bounds). To prevent this we 
				 * have this if clause
				 * e.g. For Galaxy S3 4.3.1 (with custom OS) it throws ArrayIndexOutOfBoundsException, 
				 * because though indices size is 1, this function is called with sectionIndex=1.
				 */
				return alphaNumIndexer.get(indices.get(sectionIndex));
				
			} else {
				return -1;
			}
		}

		@Override
		public int getSectionForPosition(int position) {
			return 0;
		}

		@Override
		public Object[] getSections() {
			return indices.toArray();
		}
	}
	
	protected void freeUpBitmapMemory(View view) {
		if (view.getTag().equals(ArtistListAdapter.TAG_CONTENT)) {
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
	
	private void showNoArtistFound() {
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
					"Personalize Your Experience");
			((TextView)scrlVRootNoItemsFoundWithAction.findViewById(R.id.txtNoItemsMsg)).setText(
					"Sync accounts or search for artists to start your personalized eventseeker experience.");
			((Button)scrlVRootNoItemsFoundWithAction.findViewById(R.id.btnAction)).setText(
					"Sync Accounts");
			((ImageView)scrlVRootNoItemsFoundWithAction.findViewById(R.id.imgNoItems)).setImageDrawable(
					res.getDrawable(R.drawable.no_artists_following));
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.btnAction:
			((DrawerListFragmentListener)FragmentUtil.getActivity(this)).onDrawerItemSelected(
					MainActivity.INDEX_NAV_ITEM_CONNECT_ACCOUNTS);
			break;

		default:
			break;
		}
	}
	
	protected abstract AbsListView getScrollableView();
}
