package com.wcities.eventseeker.interfaces;

public interface FragmentHavingFragmentInRecyclerView {
	public void onPushedToBackStackFHFIR();
	public void onPoppedFromBackStackFHFIR();
	
	/**
	 * This is used to check if this fragment is on top or not. If not then it means it's on back stack
	 * & hence we skip function calls like onStart() (except super call which is necessary) so that
	 * it presenets toolbar & statusbar as per top fragment. Main reason for this requirement is because
	 * when coming back from some other activity to MainActivity, onStart() of all fragments in back stack
	 * is called which we don't want.
	 * <p>e.g. - 
	 * a) discover -> event details -> venue details -> navigation -> google maps -> back results in call to 
	 * onStart() of DiscoverFragment, then venue details & navigation screen. Now, navigation screen 
	 * is ok with default toolbar & statusbar UI hence it doesn't do anything in onStart(). Due to this
	 * if Venue details screen applies its toolbar & statusbar color changes from its onStart() Navigation screen,
	 * won't do anything afterwards resulting in wrong toolbar-statusbar colors.
	 * b) discover -> event details -> venue details -> search -> lock-unlock/home-back to eventseeker.
	 * This also results in transparent toolbar.
	 * @return
	 */
	public boolean isOnTopFHFIR();
}
