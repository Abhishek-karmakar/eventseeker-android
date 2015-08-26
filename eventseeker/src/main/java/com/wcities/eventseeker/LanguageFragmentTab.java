package com.wcities.eventseeker;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AbsListView;
import android.widget.AbsListView.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.constants.Enums.Locales;
import com.wcities.eventseeker.interfaces.OnLocaleChangedListener;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.VersionUtil;
import com.wcities.eventseeker.util.ViewUtil;

import java.util.List;

public class LanguageFragmentTab extends ListFragment {

	private static final String TAG = LanguageFragmentTab.class.getName();

	private List<Locales> languages;

	private LanguageAdapter adapter;
	
	private int htForLangList;
	
	private OnGlobalLayoutListener onGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
		
		private int count = 0;
		
        @Override
        public void onGlobalLayout() {
        	if (count == 0) {
        		count++;
        		
        	} else {
        		count = 0;
        		if (VersionUtil.isApiLevelAbove15()) {
    				getListView().getViewTreeObserver().removeOnGlobalLayoutListener(this);

    			} else {
    				getListView().getViewTreeObserver().removeGlobalOnLayoutListener(this);
    			}
        	}
        	
    		Resources res = FragmentUtil.getResources(LanguageFragmentTab.this);
    		DisplayMetrics displaymetrics = new DisplayMetrics();
    		FragmentUtil.getActivity(LanguageFragmentTab.this).getWindowManager().getDefaultDisplay()
    			.getMetrics(displaymetrics);
    		
    		int lstHt = displaymetrics.heightPixels;
    		htForLangList = lstHt - res.getDimensionPixelSize(R.dimen.action_bar_ht)
    				/**
    				 * subtracting the top-bottom margins
    				 */
    				- 2 * res.getDimensionPixelSize(R.dimen.list_view_m_t_b_language_fragment_tab)
    				/**
    				 * subtracting the StatusBar height
    				 */
    				- ViewUtil.getStatusBarHeight(res);
    		
        	adapter.setHtForLanguageList(htForLangList);
        }
    };
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_language_tab, null);
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (languages == null) {
			loadLanguages();
			adapter = new LanguageAdapter(languages, this);
		}
		setListAdapter(adapter);

		getListView().setDivider(null);
        getListView().getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		
		Locales locale = (Locales) adapter.getItem(position);
		EventSeekr app = (EventSeekr) FragmentUtil.getActivity(this).getApplication();
		app.updateLocale(locale);
		
		loadLanguages();
		adapter.notifyDataSetChanged();
		
		OnLocaleChangedListener onLocaleChangedListener = (OnLocaleChangedListener) FragmentUtil.getActivity(this);
		onLocaleChangedListener.onLocaleChanged();
	}
	
	private void loadLanguages() {
		languages = Locales.getMobileLocales();
	}

	@Override
	public void onStop() {
		super.onStop();
		//Log.d(TAG, "onStop()");
		
		/**
		 * Following call is required to prevent non-removal of onGlobalLayoutListener. If onGlobalLayout() 
		 * is not called yet & screen gets destroyed, then removal of onGlobalLayoutListener will not happen ever 
		 * since fragment won't be able to find its view tree observer. So, better to make sure
		 * that it gets removed at the end
		 */
		try {
			if (VersionUtil.isApiLevelAbove15()) {
				getListView().getViewTreeObserver().removeOnGlobalLayoutListener(onGlobalLayoutListener);
	
			} else {
				getListView().getViewTreeObserver().removeGlobalOnLayoutListener(onGlobalLayoutListener);
			}
			
		} catch (NullPointerException ne) {
			// if listview is not yet created
			Log.e(TAG, ne.getMessage());
			
		} catch (IllegalStateException ie) {
			// if contentview is not yet created
			Log.e(TAG, ie.getMessage());
		}
	}
	
	private class LanguageAdapter extends BaseAdapter {

		private List<Locales> languages;
		private Fragment fragment;
		private int rowHt;
		
		public LanguageAdapter(List<Locales> languages, Fragment fragment) {
			this.languages = languages;
			this.fragment = fragment;
		}
		
		public void setHtForLanguageList(int htForSettingsList) {
			this.rowHt = htForSettingsList / languages.size();
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return languages.size();
		}

		@Override
		public Object getItem(int position) {
			return languages.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			VHLanguages vhLanguages;
			
			if (convertView == null) {
				convertView = LayoutInflater.from(FragmentUtil.getActivity(fragment))
						.inflate(R.layout.list_item_language_tab, null);

				vhLanguages = new VHLanguages();
				vhLanguages.txtLanguage = (TextView) convertView.findViewById(R.id.txtLanguage);
				vhLanguages.imgSelected = (ImageView) convertView.findViewById(R.id.imgSelected);
				convertView.setTag(vhLanguages);
				
			} else {
				vhLanguages = (VHLanguages) convertView.getTag();
			}

			AbsListView.LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, rowHt);
			convertView.setLayoutParams(lp);
			
			Locales locale = (Locales) getItem(position);
			vhLanguages.txtLanguage.setText(locale.getLocaleLanguage());
			if (Locales.isDefaultLocale(FragmentUtil.getActivity(fragment), locale)) {
				vhLanguages.imgSelected.setVisibility(View.VISIBLE);
				
			} else {
				vhLanguages.imgSelected.setVisibility(View.INVISIBLE);
			}
			return convertView;
		}
		
		private class VHLanguages {
			private TextView txtLanguage;
			private ImageView imgSelected;
		}
	}
}
