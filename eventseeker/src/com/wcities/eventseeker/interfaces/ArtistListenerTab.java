package com.wcities.eventseeker.interfaces;

import android.widget.ImageView;
import android.widget.TextView;

import com.wcities.eventseeker.core.Artist;

public interface ArtistListenerTab {
	public void onArtistSelected(Artist artist, ImageView imageView, TextView textView);
}
