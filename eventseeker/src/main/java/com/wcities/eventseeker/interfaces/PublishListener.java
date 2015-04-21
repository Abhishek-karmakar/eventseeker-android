package com.wcities.eventseeker.interfaces;

import com.facebook.Session.StatusCallback;

public interface PublishListener extends StatusCallback {
	public void setPendingAnnounce(boolean pendingAnnounce);
	public boolean isPendingAnnounce();
	public void onPublishPermissionGranted();
	/**
	 * The PublishPermissionDisplayed related callback's were added as the user was continuously 
	 * getting the permission dialog when user was trying to choose 'want to' or 'going to' options
	 * and if he is trying to cancel the permission dialog
	 * @return
	 */
	public boolean isPermissionDisplayed();
	public void setPermissionDisplayed(boolean isPermissionDisplayed);
}
