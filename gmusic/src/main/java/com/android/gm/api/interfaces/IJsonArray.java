package com.android.gm.api.interfaces;

import org.json.JSONArray;

import java.util.ArrayList;

public interface IJsonArray<T>
{
	ArrayList<T> fromJsonArray(JSONArray jsonArray);
}
