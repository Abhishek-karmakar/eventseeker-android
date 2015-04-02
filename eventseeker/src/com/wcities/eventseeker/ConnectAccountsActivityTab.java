package com.wcities.eventseeker;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.wcities.eventseeker.ConnectAccountsFragmentTab.ConnectAccountsFragmentListener;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.constants.Enums.Service;
import com.wcities.eventseeker.interfaces.FragmentLoadedFromBackstackListener;
import com.wcities.eventseeker.interfaces.ReplaceFragmentListener;
import com.wcities.eventseeker.util.FragmentUtil;

public class ConnectAccountsActivityTab extends BaseActivityTab implements ConnectAccountsFragmentListener,
		FragmentLoadedFromBackstackListener, ReplaceFragmentListener {
	
	private static final String TAG = ConnectAccountsActivityTab.class.getSimpleName();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Log.d(TAG, "onCreate()");
		setContentView(R.layout.activity_base_tab);
		
		setCommonUI();
		
		/**
		 * if user moves away quickly to any other screen resulting in fragment
		 * replacement & if we are adding this fragment into backstack, then
		 * orientation change made now will result in getActivity() returning
		 * null for all fragments existing in back stack. To resolve this we
		 * don't use getActivity() or getParentFragment().getActivity() call
		 * directly from fragment; rather we keep activity as instance variable
		 * of all such fragments & we keep this reference updated in all the
		 * back stack fragments by below call.
		 */
		FragmentUtil.updateActivityReferenceInAllFragments(getSupportFragmentManager(), this);
		
		if (isOnCreateCalledFirstTime) {
			//Log.d(TAG, "add settings fragment tab");
			ConnectAccountsFragmentTab connectAccountsFragmentTab = new ConnectAccountsFragmentTab();
			addFragment(R.id.content_frame, connectAccountsFragmentTab, 
					FragmentUtil.getTag(connectAccountsFragmentTab), false);
		}
		
		if (savedInstanceState != null) {
			currentContentFragmentTag = savedInstanceState.getString(BundleKeys.CURRENT_CONTENT_FRAGMENT_TAG);
		}
	}

	@Override
	public String getScreenName() {
		/**
		 * The ScreenName for the Google Analytics Tracker are handled in the child 
		 * Fragments of this Activity.
		 */
		return null;
	}

	@Override
	protected String getScrnTitle() {
		return "";
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			
		case AppConstants.REQ_CODE_SPOTIFY:
			Fragment fragment = getSupportFragmentManager().findFragmentByTag(currentContentFragmentTag);
			if (fragment instanceof ConnectAccountsFragment) {
				fragment.onActivityResult(requestCode, resultCode, data);
			}
			break;
			
		default:
			super.onActivityResult(requestCode, resultCode, data);
		}
	}
	
	@Override
	public void onFragmentResumed(Fragment fragment, int drawerPosition, String actionBarTitle) {
		updateTitle(actionBarTitle);
		// Log.d(TAG, "got the current tag as : " + fragmentTag);
	}
	
	@Override
	public void onServiceSelected(Service service, Bundle args, boolean addToBackStack) {
		//Log.d(TAG, "onServiceSelected()");
		switch (service) {
		
		case GooglePlay:
			GooglePlayMusicFragmentTab googlePlayMusicFragmentTab = new GooglePlayMusicFragmentTab();
			googlePlayMusicFragmentTab.setArguments(args);
			replaceFragment(R.id.content_frame, googlePlayMusicFragmentTab, 
					FragmentUtil.getTag(googlePlayMusicFragmentTab), addToBackStack);
			break;

		case DeviceLibrary:
			DeviceLibraryFragmentTab deviceLibraryFragmentTab = new DeviceLibraryFragmentTab();
			deviceLibraryFragmentTab.setArguments(args);
			replaceFragment(R.id.content_frame, deviceLibraryFragmentTab, 
					FragmentUtil.getTag(deviceLibraryFragmentTab), addToBackStack);
			break;
			
		case Twitter:
			TwitterFragmentTab twitterFragmentTab = new TwitterFragmentTab();
			twitterFragmentTab.setArguments(args);
					replaceFragment(R.id.content_frame, twitterFragmentTab, 
							FragmentUtil.getTag(twitterFragmentTab), addToBackStack);
			break;
			
		case Spotify:
			Intent intent = new Intent(getApplicationContext(), SpotifyActivity.class);
			intent.putExtras(args);
			startActivityForResult(intent, AppConstants.REQ_CODE_SPOTIFY);
			break;

		case Rdio:
			RdioFragmentTab rdioFragmentTab = new RdioFragmentTab();
			rdioFragmentTab.setArguments(args);
			replaceFragment(R.id.content_frame, rdioFragmentTab, 
					FragmentUtil.getTag(rdioFragmentTab), addToBackStack);
			break;

		case Lastfm:
			LastfmFragmentTab lastfmFragmentTab = new LastfmFragmentTab();
			lastfmFragmentTab.setArguments(args);
			replaceFragment(R.id.content_frame, lastfmFragmentTab, 
					FragmentUtil.getTag(lastfmFragmentTab), addToBackStack);
			break;

		case Pandora:
			PandoraFragment pandoraFragment = new PandoraFragment();
			pandoraFragment.setArguments(args);
			replaceFragment(R.id.content_frame, pandoraFragment, 
					FragmentUtil.getTag(pandoraFragment), addToBackStack);
			break;

		default:
			break;
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString(BundleKeys.CURRENT_CONTENT_FRAGMENT_TAG, currentContentFragmentTag);
		super.onSaveInstanceState(outState);
	}
	
	@Override
	public void onFragmentResumed(Fragment fragment) {}

	@Override
	public void replaceByFragment(String fragmentTag, Bundle args) {
		if (fragmentTag.equals(FragmentUtil.getTag(TwitterSyncingFragment.class))) {
			if (currentContentFragmentTag.equals(FragmentUtil.getTag(TwitterFragmentTab.class))) {
				try {
					/**
					 * added this try catch as the app was crashing when user presses the twitter button to sync and
					 * after that if he immediately presses home then after around 2-3 sec, app was crashing with
					 * following error : java.lang.IllegalStateException: Can not perform this action after 
					 * onSaveInstanceState
					 */
					onBackPressed();
					
				} catch (IllegalStateException e) {
					e.printStackTrace();
					/**
					 * return from here otherwise app will again crash at below lines.
					 */
					return;
				}
			}
			TwitterSyncingFragment twitterSyncingFragment = new TwitterSyncingFragment();
			twitterSyncingFragment.setArguments(args);
			super.replaceFragment(R.id.content_frame, twitterSyncingFragment, fragmentTag, true);
		}
	}
	
	@Override
	public void onBackPressed() throws IllegalStateException {
		try {
			super.onBackPressed();
			
		} catch (IllegalStateException e) {
			if (currentContentFragmentTag.equals(FragmentUtil.getTag(TwitterFragmentTab.class))) {
				throw e;
				
			} else {
				e.printStackTrace();					
			}
		}
	}
}
