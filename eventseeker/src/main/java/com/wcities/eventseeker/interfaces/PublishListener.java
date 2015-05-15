package com.wcities.eventseeker.interfaces;

import com.facebook.FacebookCallback;
import com.facebook.login.LoginResult;

public interface PublishListener extends FacebookCallback<LoginResult> {
	public void setPendingAnnounce(boolean pendingAnnounce);
	public boolean isPendingAnnounce();
	public void onPublishPermissionGranted();
}
