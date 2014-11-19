package com.wcities.eventseeker.core.registration;

import com.wcities.eventseeker.app.EventSeekr;

public abstract class Registration implements IRegistration {

	protected EventSeekr eventSeekr;

	public Registration(EventSeekr eventSeekr) {
		this.eventSeekr = eventSeekr;
	}
}
