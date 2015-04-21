package com.wcities.eventseeker.core;

import java.util.List;

public class ItemsList<T> {
	
	private int totalCount;
	private List<T> items;
	
	public void setTotalCount(int totalCount) {
		this.totalCount = totalCount;
	}

	public void setItems(List<T> items) {
		this.items = items;
	}

	public int getTotalCount() {
		return totalCount;
	}
	
	public List<T> getItems() {
		return items;
	}
}
