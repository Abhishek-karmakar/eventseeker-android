package com.wcities.eventseeker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView.RecyclerListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wcities.eventseeker.ArtistDetailsFragment.ArtistDetailsFragmentListener;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingItemType;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.LoadArtistEvents;
import com.wcities.eventseeker.asynctask.UserTracker;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.Artist.Attending;
import com.wcities.eventseeker.custom.fragment.PublishEventListFragment;
import com.wcities.eventseeker.interfaces.DateWiseEventParentAdapterListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.viewdata.DateWiseEventList;
import com.wcities.eventseeker.viewdata.DateWiseEventList.LIST_ITEM_TYPE;

public abstract class ArtistEventsParentFragment extends PublishEventListFragment implements OnClickListener, 
		ArtistDetailsFragmentListener, LoadItemsInBackgroundListener {

	private static final String TAG = ArtistEventsParentFragment.class.getName();

	protected Artist artist;

	protected RelativeLayout fragmentArtistDetailsFooter;
	protected ImageView imgFollow;
	protected TextView txtFollow;

	protected boolean isTablet;

	protected LoadArtistEvents loadArtistEvents;

	protected DateWiseEventParentAdapterListener adapter;

	protected DateWiseEventList dateWiseEventList;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		artist = (Artist) getArguments().getSerializable(BundleKeys.ARTIST);
		isTablet = ((MainActivity) FragmentUtil.getActivity(this)).isTablet();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View v = inflater.inflate(R.layout.fragment_artist_events, null);

		imgFollow = (ImageView) v.findViewById(R.id.imgFollow);
		txtFollow = (TextView) v.findViewById(R.id.txtFollow);
		fragmentArtistDetailsFooter = (RelativeLayout) v
				.findViewById(R.id.fragmentArtistDetailsFooter);
		fragmentArtistDetailsFooter.setOnClickListener(this);

		updateFollowingFooter();

		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (dateWiseEventList == null) {

			dateWiseEventList = new DateWiseEventList();
			dateWiseEventList.addDummyItem();

			adapter = getAdapterInstance();

			loadItemsInBackground();

		} else {
			adapter.updateContext(FragmentUtil.getActivity(this));
		}

		
		setListAdapter(((BaseAdapter) adapter));

		getListView().setDivider(null);
		getListView().setBackgroundResource(R.drawable.story_space);
		getListView().setScrollingCacheEnabled(false);
		getListView().setRecyclerListener(new RecyclerListener() {
			
			@Override
			public void onMovedToScrapHeap(View view) {
				freeUpBitmapMemory(view);
			}

		});

	}

	private void freeUpBitmapMemory(View view) {
		if (view != null && view.getTag() == LIST_ITEM_TYPE.CONTENT) {
			((ImageView) view.findViewById(R.id.imgEvent)).setImageBitmap(null);
		}
	}

	@Override
	public void onClick(View v) {

		switch (v.getId()) {

		case R.id.fragmentArtistDetailsFooter:
			EventSeekr eventSeekr = (EventSeekr) FragmentUtil.getActivity(this).getApplication();
			if (/*txtFollow.getText().equals(FooterTxt.Follow.getStringForm(this))*/
					artist.getAttending() == Attending.NotTracked) {
				artist.updateAttending(Attending.Tracked, eventSeekr);
				updateFollowingFooter();
				new UserTracker(eventSeekr, UserTrackingItemType.artist, artist.getId()).execute();

			} else {
				artist.updateAttending(Attending.NotTracked, eventSeekr);
				updateFollowingFooter();
				new UserTracker(eventSeekr, UserTrackingItemType.artist, artist.getId(), 
						Attending.NotTracked.getValue(), UserTrackingType.Edit).execute();
			}

			((ArtistDetailsFragment) getParentFragment()).onArtistFollowingUpdated();
			break;

		default:
			break;
		}
	}

	@Override
	public void onArtistUpdatedByArtistDetailsFragment() {
		updateFollowingFooter();
		((BaseAdapter) adapter).notifyDataSetChanged();
	}

	@Override
	public void onArtistFollowingUpdated() {
		updateFollowingFooter();
	}

	@Override
	public void loadItemsInBackground() {
		loadArtistEvents = new LoadArtistEvents(dateWiseEventList, adapter, artist.getId(),
				((EventSeekr)FragmentUtil.getActivity(this).getApplicationContext()).getWcitiesId());

		adapter.setLoadDateWiseEvents(loadArtistEvents);
		AsyncTaskUtil.executeAsyncTask(loadArtistEvents, true);
		
	}
	
	@Override
	public void onDestroyView() {
		
		for (int i = getListView().getFirstVisiblePosition(), j = 0; 
				i <= getListView().getLastVisiblePosition(); i++, j++) {
			freeUpBitmapMemory(getListView().getChildAt(j));
		}
		super.onDestroyView();
	}
	
	
	protected abstract void updateFollowingFooter();
	protected abstract DateWiseEventParentAdapterListener getAdapterInstance();

}
