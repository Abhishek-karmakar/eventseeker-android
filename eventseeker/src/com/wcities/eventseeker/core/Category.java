package com.wcities.eventseeker.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Category implements Serializable {

	private int id;
	private String name;
	private List<Subcategory> subcategories;
	
	public Category(int id, String name) {
		this.id = id;
		this.name = name;
		this.subcategories = new ArrayList<Subcategory>();
	}
	
	public Category(int id, String name, List<Subcategory> subcategories) {
		this.id = id;
		this.name = name;
		this.subcategories = subcategories;
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

	public List<Subcategory> getSubcategories() {
		return subcategories;
	}

	public void setSubcategories(List<Subcategory> subcategories) {
		this.subcategories = subcategories;
	}
}
