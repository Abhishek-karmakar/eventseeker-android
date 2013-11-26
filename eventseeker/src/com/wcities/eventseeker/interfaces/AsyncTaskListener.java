package com.wcities.eventseeker.interfaces;

public interface AsyncTaskListener<T> {
	public void onTaskCompleted(T... params);
}
