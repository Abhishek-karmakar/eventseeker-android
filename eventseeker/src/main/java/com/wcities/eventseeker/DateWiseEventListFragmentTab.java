package com.wcities.eventseeker;

import android.os.Bundle;

import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.wcities.eventseeker.adapter.DateWiseMyEventListAdapter;
import com.wcities.eventseeker.interfaces.DateWiseEventParentAdapterListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public class DateWiseEventListFragmentTab extends DateWiseEventListParentFragment {

	private static final String TAG = DateWiseEventListFragmentTab.class.getName();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}
	
	@Override
	public void loadItemsInBackground() {
		loadEvents = getLoadDateWiseEvents();
		((DateWiseMyEventListAdapter) eventListAdapter).setLoadDateWiseEvents(loadEvents);
		AsyncTaskUtil.executeAsyncTask(loadEvents, true);
	}

	@Override
	protected DateWiseEventParentAdapterListener getAdapterInstance() {
		return new DateWiseMyEventListAdapter(FragmentUtil.getActivity(this), dateWiseEvtList, null, 
				this, this, FragmentUtil.getScreenName(this));
	}

	/*@Override
	public void call(Session session, SessionState state, Exception exception) {
		((DateWiseMyEventListAdapter)eventListAdapter).call(session, state, exception);
	}*/

	@Override
	public void onPublishPermissionGranted() {
		((DateWiseMyEventListAdapter)eventListAdapter).onPublishPermissionGranted();
	}

	@Override
	public void onSuccess(LoginResult loginResult) {

	}

	@Override
	public void onCancel() {

	}

	@Override
	public void onError(FacebookException e) {

	}
}
