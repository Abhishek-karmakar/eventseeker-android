package com.wcities.eventseeker.adapter;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask.Status;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.asynctask.LoadPopularArtists;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.core.FeaturedListArtistCategory;
import com.wcities.eventseeker.core.PopularArtistCategory;
import com.wcities.eventseeker.interfaces.FullScrnProgressListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.interfaces.OnPopularArtistsCategoryClickListener;
import com.wcities.eventseeker.util.FragmentUtil;

import java.lang.ref.WeakReference;
import java.util.List;

public class RVPopularArtistsAdapter extends RVAdapterBase<RVPopularArtistsAdapter.ViewHolder> {

	private static final String TAG = RVPopularArtistsAdapter.class.getSimpleName();
	private Fragment fragment;
	private RecyclerView rvPopularArtists;
	private WeakReference<RecyclerView> weakRecyclerView;
	
	private List<PopularArtistCategory> popularArtistCategories;

	private LoadItemsInBackgroundListener loadItemsInBackgroundListener;
	private FullScrnProgressListener fullScrnProgressListener;
	private LoadPopularArtists loadPopularArtists;
	private BitmapCache bitmapCache;
	
	private int artistsAlreadyRequested;
	private boolean isMoreDataAvailable = true;
	private OnPopularArtistsCategoryClickListener onPopularArtistsCategoryClickListener;

	private int imgWidth, imgHeight;

	private enum ViewType {
		ROW, PROGRESS
	}
	
	public RVPopularArtistsAdapter(List<PopularArtistCategory> popularArtistCategories, Fragment fragment,
			FullScrnProgressListener fullScrnProgressListener, LoadItemsInBackgroundListener loadItemsInBackgroundListener,
			OnPopularArtistsCategoryClickListener onPopularArtistsCategoryClickListener) {
		this.popularArtistCategories = popularArtistCategories;
		this.fragment = fragment;
		this.fullScrnProgressListener = fullScrnProgressListener;
		this.loadItemsInBackgroundListener = loadItemsInBackgroundListener;
		this.onPopularArtistsCategoryClickListener = onPopularArtistsCategoryClickListener;

		bitmapCache = BitmapCache.getInstance();

		updateViewParams();
	}

	public void updateViewParams() {
		Resources res = FragmentUtil.getResources(fragment);

		DisplayMetrics displaymetrics = new DisplayMetrics();
		FragmentUtil.getActivity(fragment).getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);

		imgWidth = displaymetrics.widthPixels
				- 2 * res.getDimensionPixelSize(R.dimen.lnr_root_padding_fragment_follow_more_artists);

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeResource(res, R.drawable.ic_featured, options);

		imgHeight = (int) (imgWidth / (options.outWidth / (float) options.outHeight));
	}

	@Override
	public int getItemCount() {
		return popularArtistCategories.size();
	}

	@Override
	public int getItemViewType(int position) {
		return popularArtistCategories.get(position) == null ? ViewType.PROGRESS.ordinal() : ViewType.ROW.ordinal();
	}
	
	@Override
	public void onBindViewHolder(ViewHolder viewHolder, int position) {
		final PopularArtistCategory popularArtistCategory = popularArtistCategories.get(position);
		if (popularArtistCategory != null) {
			viewHolder.vSeparator.setVisibility((position == 0) ? View.GONE : View.VISIBLE);

			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) viewHolder.imgAritstCategory.getLayoutParams();
			params.width = imgWidth;
			params.height = imgHeight;
			viewHolder.imgAritstCategory.setLayoutParams(params);

			if (popularArtistCategory instanceof FeaturedListArtistCategory) {
				FeaturedListArtistCategory featuredListArtistCategory = (FeaturedListArtistCategory) popularArtistCategory;
				
				String key = featuredListArtistCategory.getKey(ImgResolution.LOW);
				Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);

				if (bitmap != null) {
					//Log.d(TAG, "bitmap != null");
					viewHolder.imgAritstCategory.setImageBitmap(bitmap);
					viewHolder.prgbrAritstCategory.setVisibility(View.GONE);

				} else {
					//Log.d(TAG, "bitmap = null");
					viewHolder.imgAritstCategory.setImageBitmap(null);
					viewHolder.imgAritstCategory.setBackgroundResource(R.color.bg_featured_list_img);
					viewHolder.prgbrAritstCategory.setVisibility(View.VISIBLE);

					AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
					asyncLoadImg.loadImg(viewHolder.imgAritstCategory, ImgResolution.LOW, weakRecyclerView, position,
							featuredListArtistCategory, true);
				}
				viewHolder.txtAritstCategory.setText(featuredListArtistCategory.getName());

			} else {
				viewHolder.imgAritstCategory.setImageResource(popularArtistCategory.getDrwResIdCategory());
				viewHolder.txtAritstCategory.setText(popularArtistCategory.getStrResIdCategory());
			}
			viewHolder.itemView.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (onPopularArtistsCategoryClickListener != null) {
						onPopularArtistsCategoryClickListener.onPopularArtistsCategoryClick(popularArtistCategory);
					}
				}
			});
			
		} else {
			if (position == 0) {
				fullScrnProgressListener.displayFullScrnProgress();
			}
			
			if (loadItemsInBackgroundListener != null) {
				loadItemsInBackgroundListener.loadItemsInBackground();
			}

			if ((loadPopularArtists == null || loadPopularArtists.getStatus() == Status.FINISHED) && isMoreDataAvailable) {
				loadItemsInBackgroundListener.loadItemsInBackground();
			}
		}
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		if (parent != rvPopularArtists) {
			rvPopularArtists = (RecyclerView) parent;
			weakRecyclerView = new WeakReference<RecyclerView>(rvPopularArtists);
		}
		
		ViewHolder viewHolder;
		LayoutInflater inflater = LayoutInflater.from(FragmentUtil.getActivity(fragment));
		if (viewType == ViewType.ROW.ordinal()) {
			viewHolder = new ViewHolder(inflater.inflate(R.layout.rv_item_popular_artists, null));
			
		} else {
			viewHolder = new ViewHolder(inflater.inflate(R.layout.progress_bar_eventseeker_fixed_ht, null));
		}
		return viewHolder;
	}

	public void setMoreDataAvailable(boolean isMoreDataAvailable) {
		this.isMoreDataAvailable = isMoreDataAvailable;
	}

	public void setArtistsAlreadyRequested(int artistsAlreadyRequested) {
		this.artistsAlreadyRequested = artistsAlreadyRequested;
	}

	public int getArtistsAlreadyRequested() {
		return artistsAlreadyRequested;
	}

	public void setLoadArtists(LoadPopularArtists loadPopularArtists) {
		this.loadPopularArtists = loadPopularArtists;
	}

	public static class ViewHolder extends RecyclerView.ViewHolder {

		private TextView txtAritstCategory;
		private ImageView imgAritstCategory;
		private View vSeparator;
		private View prgbrAritstCategory;

		public ViewHolder(View itemView) {
			super(itemView);
			vSeparator = itemView.findViewById(R.id.vSeparator);
			txtAritstCategory = (TextView) itemView.findViewById(R.id.txtAritstCategory);
			imgAritstCategory = (ImageView) itemView.findViewById(R.id.imgAritstCategory);
			prgbrAritstCategory = itemView.findViewById(R.id.prgbrAritstCategory);
		}
	}
}
