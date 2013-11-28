package com.wcities.eventseeker.interfaces;

public interface DateWiseEventListener {

	public int getEventsAlreadyRequested();

	public void setMoreDataAvailable(boolean isMoreDataAvailable);

	public void setEventsAlreadyRequested(int eventsAlreadyRequested);

}
