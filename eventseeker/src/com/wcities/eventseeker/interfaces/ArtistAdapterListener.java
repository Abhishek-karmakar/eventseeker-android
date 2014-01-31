package com.wcities.eventseeker.interfaces;

import java.util.List;

import android.content.Context;
import android.os.AsyncTask;

import com.wcities.eventseeker.core.Artist;

public interface ArtistAdapterListener<T> {
	public void setMoreDataAvailable(boolean isMoreDataAvailable);
	public void setArtistsAlreadyRequested(int artistsAlreadyRequested);
	public int getArtistsAlreadyRequested();
	public void updateContext(Context context);
	public void setLoadArtists(AsyncTask<T, Void, List<Artist>> loadArtists);
}
