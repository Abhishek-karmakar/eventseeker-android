package com.wcities.eventseeker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.wcities.eventseeker.adapter.DateWiseMyEventListAdapter;
import com.wcities.eventseeker.interfaces.DateWiseEventParentAdapterListener;
import com.wcities.eventseeker.interfaces.PublishListener;
import com.wcities.eventseeker.util.FragmentUtil;

public class VenueEventsListFragmentTab extends VenueEventsParentListFragment implements PublishListener {

	private static final String TAG = VenueEventsListFragment.class.getName();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return super.onCreateView(inflater, container, savedInstanceState);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}
	
	@Override
	protected DateWiseEventParentAdapterListener getAdapterInstance() {
		return new DateWiseMyEventListAdapter(FragmentUtil.getActivity(this), dateWiseEvtList, null, 
				this, this, FragmentUtil.getScreenName(this));
	}
	
	/*@Override
	public void call(Session session, SessionState state, Exception exception) {
		((DateWiseMyEventListAdapter)adapter).call(session, state, exception);
	}*/

	@Override
	public void onPublishPermissionGranted() {
		((DateWiseMyEventListAdapter)adapter).onPublishPermissionGranted();
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
