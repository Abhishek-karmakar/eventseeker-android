package com.wcities.eventseeker.jsonparser;

import com.wcities.eventseeker.core.Category;
import com.wcities.eventseeker.core.Subcategory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class InfoApiJSONParser {
	
	private static final String KEY_EVENT_CATEGORIES = "eventCategories";
	private static final String KEY_CATEGORY = "category";
	private static final String KEY_ID = "id";
	private static final String KEY_NAME = "name";
	private static final String KEY_SUBCATEGORY = "subcategory";

	public List<Category> getCategoryList(JSONObject jsonObject) {
		List<Category> categories = new ArrayList<Category>();
		
		try {
			JSONObject jObjEvtCategories = jsonObject.getJSONObject(KEY_EVENT_CATEGORIES);
			JSONArray jArrCategories = jObjEvtCategories.getJSONArray(KEY_CATEGORY);
			
			for (int i = 0; i < jArrCategories.length(); i++) {
				Category category = getCategory(jArrCategories.getJSONObject(i));
				categories.add(category);
			}
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return categories;
	}
	
	private Category getCategory(JSONObject jObjCategory) throws JSONException {
		JSONArray jArrSubcategories = jObjCategory.getJSONArray(KEY_SUBCATEGORY);
		
		List<Subcategory> subcategories = new ArrayList<Subcategory>();
		for (int i = 0; i < jArrSubcategories.length(); i++) {
			Subcategory subcategory = getSubcategory(jArrSubcategories.getJSONObject(i));
			subcategories.add(subcategory);
		}
		Category category = new Category(jObjCategory.getInt(KEY_ID), jObjCategory.getString(KEY_NAME), subcategories);
		return category;
	}
	
	private Subcategory getSubcategory(JSONObject jObjSubcategory) throws JSONException {
		Subcategory subcategory = new Subcategory(jObjSubcategory.getInt(KEY_ID), jObjSubcategory.getString(KEY_NAME));
		return subcategory;
	}
}
