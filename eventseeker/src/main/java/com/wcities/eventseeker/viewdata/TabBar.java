package com.wcities.eventseeker.viewdata;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.widget.Button;

public class TabBar {
	
	public static final int TAB_NOT_FOUND = -1;

	private static final String TAG = TabBar.class.getName();

	private List<Tab> tabs;
	private Tab selectedTab;
	private FragmentManager fm;
	
	public TabBar(FragmentManager fm) {
		tabs = new ArrayList<TabBar.Tab>();
		this.fm = fm;
	}
	
	public void addTab(Tab tab) {
		tabs.add(tab);
		if (selectedTab == null) {
			select(tab);
		}
	}
	
	public void addTab(Tab tab, TabBar oldTabBarToRetainSelection) {
		tabs.add(tab);
		if (selectedTab == null) {
			String oldSelectedTabTag = oldTabBarToRetainSelection.selectedTab.getTag();
			if (tab.getTag().equals(oldSelectedTabTag)) {
				select(tab);
			}
		}
	}
	
	public void select(Tab tab) {
		FragmentTransaction ft = fm.beginTransaction();
		if (selectedTab != null) {
			selectedTab.setSelected(false, ft);
		}
		selectedTab = tab;
		selectedTab.setSelected(true, ft);
		ft.commit();
	}
	
	public void select(String tag) {
		// tab is already selected
		if (tag.equals(selectedTab.getTag())) {
			return;
		}
		
		FragmentTransaction ft = fm.beginTransaction();
		Tab tabToSelect = getTabByTag(tag);
		if (selectedTab != null) {
			selectedTab.setSelected(false, ft);
		}
		selectedTab = tabToSelect;
		selectedTab.setSelected(true, ft);
		ft.commit();
	}
	
	public void select(int pos) {
		// tab is already selected
		if (selectedTab != null && pos == getPos(selectedTab)) {
			return;
		}
		
		FragmentTransaction ft = fm.beginTransaction();
		Tab tabToSelect = getTab(pos);
		if (selectedTab != null) {
			selectedTab.setSelected(false, ft);
		}
		selectedTab = tabToSelect;
		selectedTab.setSelected(true, ft);
		ft.commit();
	}
	
	public Tab getTabByTag(String tag) {
		for (int i = 0; i < tabs.size(); i++) {
			Tab tab = tabs.get(i);
			if (tab.getTag().equals(tag)) {
				return tab;
			}
		}
		return null;
	}
	
	public Tab getTab(int pos) {
		if (tabs.size() > pos) {
			return tabs.get(pos);
		}
		return null;
	}
	
	public Tab getSelectedTab() {
		return selectedTab;
	}

	public String getSelectedTabTag() {
		return selectedTab.getTag();
	}
	
	public int getNumberOfTabs() {
		return tabs.size();
	}
	
	public int getPos(Tab tab) {
		for (int i = 0; i < tabs.size(); i++) {
			if (tab.getTag().equals(tabs.get(i).getTag())) {
				return i;
			}
		}
		return TAB_NOT_FOUND;
	}
	
	private int getPos(String tag) {
		for (int i = 0; i < tabs.size(); i++) {
			if (tag.equals(tabs.get(i).getTag())) {
				return i;
			}
		}
		return TAB_NOT_FOUND;
	}
	
	public void setArgs(Bundle args, int pos) {
		tabs.get(pos).setArgs(args);
	}
	
	public static class Tab {
		
		private Button button;
		private TabListener listener;
		private String tag;
		private Class<?> clss;
	    private Bundle args;
		
		public interface TabListener {
			public void onTabSelected(Tab tab, FragmentTransaction ft);
			public void onTabUnselected(Tab tab, FragmentTransaction ft);
		}

		public Tab(Button button, String tag, Class<?> clss, Bundle args) {
			this.button = button;
			this.tag = tag;
			this.clss = clss;
			this.args = args;
		}
		
		private void setSelected(boolean selected, FragmentTransaction ft) {
			button.setSelected(selected);
			if (selected) {
				listener.onTabSelected(this, ft);
				
			} else {
				listener.onTabUnselected(this, ft);
			}
		}
		
		public String getTag() {
			return tag;
		}

		public void setListener(TabListener listener) {
			this.listener = listener;
		}

		public Class<?> getClss() {
			return clss;
		}

		public Bundle getArgs() {
			return args;
		}

		public void setArgs(Bundle args) {
			this.args = args;
		}
		
		public void setButtonBg(int resId) {
			button.setBackgroundResource(resId);
		}
	}
}
