package com.wcities.eventseeker.applink.util;

import java.util.Arrays;
import java.util.Vector;

import com.ford.syncV4.proxy.rpc.Choice;
import com.wcities.eventseeker.applink.handler.DiscoverAL.Discover;
import com.wcities.eventseeker.applink.handler.MyEventsAL.SuggestionReply;
import com.wcities.eventseeker.applink.handler.SearchAL.SearchCategories;

public class InteractionChoiceSetUtil {
	
	public static enum ChoiceSet {
		DISCOVER,
		SUGGESTION_REPLY,
		SEARCH;
	}

	public static void createInteractionChoiceSets() {
		Vector<Choice> choices = new Vector<Choice>();
		
		Discover[] categories = Discover.values();
		for (int i = 0; i < categories.length; i++) {
			Discover category = categories[i];
			//Log.d(TAG, "Category id : " + category.getId());
			//Log.d(TAG, "Category nameResId : " + category.getName());
			Choice choice = ALUtil.createChoice(category.getId(), category.getName(), 
					new Vector<String>(Arrays.asList(new String[] {category.getName()})));
			choices.add(choice);
		}
		ALUtil.createInteractionChoiceSet(choices, ChoiceSet.DISCOVER.ordinal());
		choices.clear();
		
		SuggestionReply[] replies = SuggestionReply.values();
		for (int i = 0; i < replies.length; i++) {
			SuggestionReply reply = replies[i];
			Choice choice = ALUtil.createChoice(reply.getId(), reply.getName(), 
					new Vector<String>(Arrays.asList(new String[] {reply.getName()})));
			choices.add(choice);
		}
		ALUtil.createInteractionChoiceSet(choices, ChoiceSet.SUGGESTION_REPLY.ordinal());
		choices.clear();
		
		SearchCategories[] searchCategories = SearchCategories.values();
		for (int i = 0; i < searchCategories.length; i++) {
			SearchCategories category = searchCategories[i];
			//Log.d(TAG, "Category id : " + category.ordinal());
			//Log.d(TAG, "Category nameResId : " + category.getNameResId());
			Choice choice = ALUtil.createChoice(category.ordinal(), category.getNameResId(), 
					new Vector<String>(Arrays.asList(new String[] {category.getNameResId()})));
			choices.add(choice);
		}
		ALUtil.createInteractionChoiceSet(choices, ChoiceSet.SEARCH.ordinal());
	}
}
