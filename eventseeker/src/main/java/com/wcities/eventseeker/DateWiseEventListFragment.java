package com.wcities.eventseeker;

import android.os.Bundle;

import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.wcities.eventseeker.adapter.DateWiseEventListAdapter;
import com.wcities.eventseeker.interfaces.DateWiseEventParentAdapterListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public class DateWiseEventListFragment extends DateWiseEventListParentFragment {

	private static final String TAG = DateWiseEventListFragment.class.getName();

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
		((DateWiseEventListAdapter) eventListAdapter).setLoadDateWiseEvents(loadEvents);
		AsyncTaskUtil.executeAsyncTask(loadEvents, true);
	}

	@Override
	protected DateWiseEventParentAdapterListener getAdapterInstance() {
		return new DateWiseEventListAdapter( FragmentUtil.getActivity(this), dateWiseEvtList, null, this);
	}

	/*@Override
	public void call(Session session, SessionState state, Exception exception) {
		// TODO Auto-generated method stub
		
	}*/

	@Override
	public void onPublishPermissionGranted() {
		// TODO Auto-generated method stub
		
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
