package com.wcities.eventseeker.interfaces;

/**
 * 'SyncArtistListenerTab' is used for Tablet app and 'SyncArtistListener' is used for Mobile app. The need of these 
 * different interfaces is that in Mobile app development using 'SyncArtistListener' listener we were passing entire 
 * fragment as 'Serializable' object but later we faced few issues where we were getting errors saying that few of the 
 * inner data members of the child fragment were not 'Serializable'. So, one after another we have to make them 
 * 'transient' to resolve this issue. So, in tablet app, we are just passing the String tag of the fragment with this 
 * interface and with the help of activity the receiver fragment can get the instance of the child fragment of this 
 * interface
 * @author win2
 *
 */
public interface SyncArtistListenerTab {
	public void onArtistSyncStarted(boolean doBackPress);
}
