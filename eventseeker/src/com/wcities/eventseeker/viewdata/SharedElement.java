package com.wcities.eventseeker.viewdata;

import java.io.Serializable;

import android.support.v4.view.ViewCompat;
import android.view.View;

public class SharedElement implements Serializable {

	private SharedElementPosition sharedElementPosition;
	private String transitionName;
	private transient View view;
	
	public SharedElement(SharedElementPosition sharedElementPosition, View view) {
		this.sharedElementPosition = sharedElementPosition;
		this.view = view;
		this.transitionName = ViewCompat.getTransitionName(view);
	}

	public SharedElementPosition getSharedElementPosition() {
		return sharedElementPosition;
	}

	public View getView() {
		return view;
	}

	public String getTransitionName() {
		return transitionName;
	}
}
