package com.wcities.eventseeker.interfaces;

/**
 *16-03-2015:
 *Removed the 'extends Serializable' as the app was crashing in TwitterFragment, when user selects the TwitterFragment
 *and Presses home button of phone before the page gets loaded.
 *java.lang.RuntimeException: Parcelable encountered IOException writing serializable object 
 *(name = com.wcities.eventseeker.ConnectAccountsFragment)
 */
public interface SyncArtistListener {
	public void onArtistSyncStarted(boolean doBackPress);
}
