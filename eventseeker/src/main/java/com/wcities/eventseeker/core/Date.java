package com.wcities.eventseeker.core;

import java.io.Serializable;

public class Date implements Serializable {
	
	private java.util.Date startDate;
    private java.util.Date endDate;
	private boolean startTimeAvailable;
	
	public Date(boolean startTimeAvailable) {
		this.startTimeAvailable = startTimeAvailable;
	}

	public java.util.Date getStartDate() {
		return startDate;
	}

	public void setStartDate(java.util.Date startDate) {
		this.startDate = startDate;
	}

    public java.util.Date getEndDate() {
        return endDate;
    }

    public void setEndDate(java.util.Date endDate) {
        this.endDate = endDate;
    }

    public boolean isStartTimeAvailable() {
		return startTimeAvailable;
	}

	public void setStartTimeAvailable(boolean startTimeAvailable) {
		this.startTimeAvailable = startTimeAvailable;
	}
}
