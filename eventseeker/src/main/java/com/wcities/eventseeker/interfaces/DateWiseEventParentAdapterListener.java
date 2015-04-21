package com.wcities.eventseeker.interfaces;

import java.util.List;

import android.content.Context;
import android.os.AsyncTask;

import com.wcities.eventseeker.core.Event;

public interface DateWiseEventParentAdapterListener {

	public int getEventsAlreadyRequested();

	public void setMoreDataAvailable(boolean isMoreDataAvailable);

	public void setEventsAlreadyRequested(int eventsAlreadyRequested);

	public void updateContext(Context context);

	public void setLoadDateWiseEvents(AsyncTask<Void, Void, List<Event>> loadDateWiseEvents);
}
