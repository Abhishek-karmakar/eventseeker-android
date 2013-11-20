package com.wcities.eventseeker.core;

import java.io.Serializable;

public class Subcategory implements Serializable {

	private int id;
	private String name;
	
	public Subcategory(int id, String name) {
		this.id = id;
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
