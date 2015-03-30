package com.wcities.eventseeker.adapter;

import android.content.res.Resources;
import android.graphics.Color;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.text.Html;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wcities.eventseeker.ArtistDetailsFragmentTab;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.util.FragmentUtil;

public class RVArtistDetailsAdapterTab extends Adapter<RVArtistDetailsAdapterTab.ViewHolder> {
	
	private static final String TAG = RVArtistDetailsAdapterTab.class.getSimpleName();
	
	private static final int EXTRA_TOP_DUMMY_ITEM_COUNT = 2;
	private static final int MAX_LINES_ARTIST_DESC = 3;
	private static final int EXTRA_TOP_DUMMY_ITEM_COUNT_AFTER_DETAILS_LOADED = 4;
	
	private RecyclerView recyclerView;
	
	private ArtistDetailsFragmentTab artistDetailsFragmentTab;
	private Artist artist;
	private boolean isArtistDescExpanded;
	
	private VideoPagerAdapter videoPagerAdapter;
	private FriendsRVAdapter friendsRVAdapter;
	
	private static enum ViewType {
		IMG, DESC, VIDEOS, FRIENDS, UPCOMING_EVENTS_TITLE, PROGRESS, EVENT;
		
		private static ViewType getViewType(int type) {
			ViewType[] viewTypes = ViewType.values();
			for (int i = 0; i < viewTypes.length; i++) {
				if (viewTypes[i].ordinal() == type) {
					return viewTypes[i];
				}
			}
			return null;
		}
	};

	static class ViewHolder extends RecyclerView.ViewHolder {
		
		private RelativeLayout rltLytPrgsBar, rltRootDesc;
		private TextView txtDesc;
		private ImageView imgDown;
		private View vHorLine;
		
		private ViewPager vPagerVideos;
		
		private RecyclerView recyclerVFriends;

		public ViewHolder(View itemView) {
			super(itemView);
			
			rltRootDesc = (RelativeLayout) itemView.findViewById(R.id.rltRootDesc);
			rltLytPrgsBar = (RelativeLayout) itemView.findViewById(R.id.rltLytPrgsBar);
			txtDesc = (TextView) itemView.findViewById(R.id.txtDesc);
			imgDown = (ImageView) itemView.findViewById(R.id.imgDown);
			vHorLine = itemView.findViewById(R.id.vHorLine);
			
			vPagerVideos = (ViewPager) itemView.findViewById(R.id.vPagerVideos);
			
			recyclerVFriends = (RecyclerView) itemView.findViewById(R.id.recyclerVFriends);
		}
	}
	
	public RVArtistDetailsAdapterTab(ArtistDetailsFragmentTab artistDetailsFragmentTab) {
		this.artistDetailsFragmentTab = artistDetailsFragmentTab;
		artist = artistDetailsFragmentTab.getArtist();
	}

	@Override
	public int getItemViewType(int position) {
		if (position == ViewType.IMG.ordinal()) {
			return ViewType.IMG.ordinal();
			
		} else if (position == ViewType.DESC.ordinal()) {
			return ViewType.DESC.ordinal();
			
		} else if (position == ViewType.VIDEOS.ordinal()) {
			return ViewType.VIDEOS.ordinal();
			
		} else if (position == ViewType.FRIENDS.ordinal()) {
			return ViewType.FRIENDS.ordinal();
			
		} /*else if (position == ViewType.UPCOMING_EVENTS_TITLE.ordinal()) {
			return ViewType.UPCOMING_EVENTS_TITLE.ordinal();
			
		} else if (eventList.get(position - EXTRA_TOP_DUMMY_ITEM_COUNT_AFTER_DETAILS_LOADED) == null) {
			return ViewType.PROGRESS.ordinal();
			
		}*/ else {
			return ViewType.EVENT.ordinal();
		}
	}

	@Override
	public int getItemCount() {
		return artistDetailsFragmentTab.isAllDetailsLoaded() ? (EXTRA_TOP_DUMMY_ITEM_COUNT_AFTER_DETAILS_LOADED /*+ 
				eventList.size()*/) : EXTRA_TOP_DUMMY_ITEM_COUNT;
	}
	
	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		//Log.d(TAG, "onCreateViewHolder(), viewType = " + viewType);
		View v;
		
		recyclerView = (RecyclerView) parent;
		
		switch (ViewType.getViewType(viewType)) {
		
		case IMG:
			v = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_item_img_artist_artist_details, 
					parent, false);
			break;
			
		case DESC:
			v = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_item_desc_tab, parent, false);
			break;
			
		case VIDEOS:
			v = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_item_videos_artist_details, 
					parent, false);
			break;
			
		case FRIENDS:
			v = LayoutInflater.from(parent.getContext()).inflate(R.layout.include_friends, parent, false);
			break;
			
		/*case UPCOMING_EVENTS_TITLE:
			v = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_item_upcoming_events_title, 
					parent, false);
			break;
			
		case PROGRESS:
			v = LayoutInflater.from(parent.getContext()).inflate(R.layout.progress_bar_eventseeker_fixed_ht, parent, 
					false);
			break;
			
		case EVENT:
			v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_discover, parent, false);
			break;*/
			
		default:
			v = null;
			break;
		}
		
		ViewHolder vh = new ViewHolder(v);
        return vh;
	}

	@Override
	public void onBindViewHolder(ViewHolder holder, int position) {
		if (position == ViewType.IMG.ordinal()) {
			// nothing to do
			
		} else if (position == ViewType.DESC.ordinal()) {
			updateDescVisibility(holder);
			
		} else if (position == ViewType.VIDEOS.ordinal()) {
			Log.d(TAG, "onBindViewHolder(), pos = " + position);
			if (videoPagerAdapter == null) {
				videoPagerAdapter = new VideoPagerAdapter(artistDetailsFragmentTab.childFragmentManager(), 
						artist.getVideos(), artist.getId());
				
			} else {
				if (videoPagerAdapter.areFragmentsDetached()) {
					videoPagerAdapter.attachFragments();
				}
			}
			
			holder.vPagerVideos.setAdapter(videoPagerAdapter);
			holder.vPagerVideos.setOnPageChangeListener(videoPagerAdapter);
			
			// Necessary or the pager will only have one extra page to show make this at least however many pages you can see
			holder.vPagerVideos.setOffscreenPageLimit(7);
			
			// Set margin for pages as a negative number, so a part of next and previous pages will be showed
			Resources res = FragmentUtil.getResources(artistDetailsFragmentTab);
			holder.vPagerVideos.setPageMargin(res.getDimensionPixelSize(R.dimen.rlt_lyt_root_w_video) - res
					.getDimensionPixelSize(R.dimen.floating_window_w) + res.getDimensionPixelSize(
					R.dimen.v_pager_videos_margin_l_rv_item_videos_artist_details) + res.getDimensionPixelSize(
							R.dimen.v_pager_videos_margin_r_rv_item_videos_artist_details));
			
			// Set current item to the middle page so we can fling to both directions left and right
			holder.vPagerVideos.setCurrentItem(videoPagerAdapter.getCurrentPosition());
			
			updateVideosVisibility(holder);
			
		} else if (position == ViewType.FRIENDS.ordinal()) {
			if (friendsRVAdapter == null) {
				friendsRVAdapter = new FriendsRVAdapter(artist.getFriends());
			} 
			
			// use a linear layout manager
			RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(FragmentUtil.getActivity(artistDetailsFragmentTab), 
					LinearLayoutManager.HORIZONTAL, false);
			holder.recyclerVFriends.setLayoutManager(layoutManager);
			
			holder.recyclerVFriends.setAdapter(friendsRVAdapter);
			
			updateFriendsVisibility(holder);
			
		}
	}
	
	private void updateDescVisibility(ViewHolder holder) {
		if (artistDetailsFragmentTab.isAllDetailsLoaded()) {
			artistDetailsFragmentTab.setVNoContentBgVisibility(View.INVISIBLE);
			if (artist.getDescription() != null) {
				holder.rltRootDesc.setBackgroundColor(Color.WHITE);
				holder.rltLytPrgsBar.setVisibility(View.GONE);
				holder.txtDesc.setVisibility(View.VISIBLE);
				holder.imgDown.setVisibility(View.VISIBLE);
				holder.vHorLine.setVisibility(View.VISIBLE);
				
				makeDescVisible(holder);
				
			} else {
				setViewGone(holder);
			}
			
		} else {
			artistDetailsFragmentTab.setVNoContentBgVisibility(View.VISIBLE);
			holder.rltRootDesc.setBackgroundColor(Color.TRANSPARENT);
			holder.rltLytPrgsBar.setVisibility(View.VISIBLE);
			holder.txtDesc.setVisibility(View.GONE);
			holder.imgDown.setVisibility(View.GONE);
			holder.vHorLine.setVisibility(View.GONE);
		}
	}
	
	private void makeDescVisible(final ViewHolder holder) {
		holder.txtDesc.setText(Html.fromHtml(artist.getDescription()));
		holder.imgDown.setVisibility(View.VISIBLE);
		holder.imgDown.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//Log.d(TAG, "totalScrolled  = " + holder.itemView.getTop());
				if (isArtistDescExpanded) {
					collapseArtistDesc(holder);
					
					/**
					 * update scrolled distance after collapse, because sometimes it can happen that view becamse scrollable only
					 * due to expanded description after which if user collapses it, then based on recyclerview
					 * height it automatically resettles itself such that recyclerview again becomes unscrollable.
					 * Accordingly we need to reset scrolled amount, artist img & title
					 */
					artistDetailsFragmentTab.getHandler().post(new Runnable() {
						
						@Override
						public void run() {
							artistDetailsFragmentTab.onScrolled(0, true, false);
						}
					});
					
				} else {
					expandArtistDesc(holder);
				}
				//Log.d(TAG, "totalScrolled after  = " + holder.itemView.getTop());
			}
		});
		
		if (isArtistDescExpanded) {
			expandArtistDesc(holder);
			
		} else {
			collapseArtistDesc(holder);
		}
	}
	
	private void collapseArtistDesc(ViewHolder holder) {
		holder.txtDesc.setMaxLines(MAX_LINES_ARTIST_DESC);
		holder.txtDesc.setEllipsize(TruncateAt.END);
		holder.imgDown.setImageDrawable(FragmentUtil.getResources(artistDetailsFragmentTab).getDrawable(
				R.drawable.ic_description_expand));
		isArtistDescExpanded = false;
	}
	
	private void expandArtistDesc(ViewHolder holder) {
		holder.txtDesc.setMaxLines(Integer.MAX_VALUE);
		holder.txtDesc.setEllipsize(null);
		holder.imgDown.setImageDrawable(FragmentUtil.getResources(artistDetailsFragmentTab).getDrawable(
				R.drawable.ic_description_collapse));
		isArtistDescExpanded = true;
	}
	
	private void setViewGone(ViewHolder holder) {
		RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) holder.itemView.getLayoutParams();
		lp.height = 0;
		holder.itemView.setLayoutParams(lp);
	}
	
	private void updateVideosVisibility(ViewHolder holder) {
		if (!artist.getVideos().isEmpty()) {
			videoPagerAdapter.notifyDataSetChanged();
			
		} else {
			setViewGone(holder);
		}
	}
	
	private void updateFriendsVisibility(ViewHolder holder) {
		if (!artist.getFriends().isEmpty()) {
			friendsRVAdapter.notifyDataSetChanged();
			
		} else {
			setViewGone(holder);
		}
	}
	
	public void detachFragments() {
		videoPagerAdapter.detachFragments();
	}
}
