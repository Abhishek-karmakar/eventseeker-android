package com.wcities.eventseeker.adapter;

import java.util.List;

import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.AdapterDataObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.core.Friend;

public class FriendsRVAdapter extends RecyclerView.Adapter<FriendsRVAdapter.ViewHolder> {
	
	static class ViewHolder extends RecyclerView.ViewHolder {
		
		private ImageView circleImgFriends;
		private TextView txtFriendName;

		public ViewHolder(View itemView) {
			super(itemView);
			this.circleImgFriends = (ImageView) itemView.findViewById(R.id.circleImgVFriend);
			this.txtFriendName = (TextView) itemView.findViewById(R.id.txtFriendName);
		}
	}
	
	private BitmapCache bitmapCache;
	private List<Friend> friends;
	
	private AdapterDataObserver adapterDataObserver;
	
	public FriendsRVAdapter(List<Friend> friends) {
		this.friends = friends;
		this.bitmapCache = BitmapCache.getInstance();
	}

	@Override
	public int getItemCount() {
		return friends.size();
	}

	@Override
	public void onBindViewHolder(ViewHolder holder, int position) {
		Friend friend = friends.get(position);
		holder.txtFriendName.setText(friend.getName());
		
		String key = friend.getKey(ImgResolution.LOW);
		Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
		if (bitmap != null) {
			holder.circleImgFriends.setImageBitmap(bitmap);
	        
	    } else {
	    	holder.circleImgFriends.setImageBitmap(null);
	        AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
	        asyncLoadImg.loadImg(holder.circleImgFriends, ImgResolution.LOW, friend);
	    }
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_item_friend, parent, false);
		ViewHolder vh = new ViewHolder(v);
		return vh;
	}
	
	/**
	 * Need to unregister manually because otherwise using same adapter on orientation change results in
	 * multiple time registrations w/o unregistration, due to which we need to manually 
	 * call unregisterAdapterDataObserver if it tries to register with new observer when already some older
	 * observer is registered. W/o having this results in multiple observers holding cardview & imgEvt memory.
	 */
	@Override
	public void registerAdapterDataObserver(AdapterDataObserver observer) {
		if (adapterDataObserver != null) {
			unregisterAdapterDataObserver(adapterDataObserver);
		}
        super.registerAdapterDataObserver(observer);
        adapterDataObserver = observer;
    }
}
