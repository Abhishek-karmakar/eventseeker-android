package com.wcities.eventseeker.interfaces;

import com.facebook.Session.StatusCallback;

public interface PublishListener extends StatusCallback {
	public void setPendingAnnounce(boolean pendingAnnounce);
	public boolean isPendingAnnounce();
	public void onPublishPermissionGranted();
}
