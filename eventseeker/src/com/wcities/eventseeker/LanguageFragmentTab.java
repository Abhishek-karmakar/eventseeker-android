package com.wcities.eventseeker;

import java.util.List;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AbsListView.LayoutParams;

import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.constants.Enums.Locales;
import com.wcities.eventseeker.interfaces.OnLocaleChangedListener;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.VersionUtil;

public class LanguageFragmentTab extends ListFragment {

	private static final String TAG = LanguageFragmentTab.class.getName();

	private List<Locales> languages;

	private LanguageAdapter adapter;
	
	private int htForSettingsList;
	
	private OnGlobalLayoutListener onGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
		
		private int count = 0;
		
        @Override
        public void onGlobalLayout() {
        	Log.d(TAG, "onGlobalLayout()");
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
    		
    		Log.d(TAG, "orientation == ORIENTATION_PORTRAIT : " + 
    				(res.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT));
    		int lstHt = res.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ? 
    				displaymetrics.heightPixels : displaymetrics.widthPixels;
    		
    		Log.d(TAG, "displaymetrics.widthPixels : " + displaymetrics.widthPixels);
    		Log.d(TAG, "displaymetrics.heightPixels : " + displaymetrics.heightPixels);
    		Log.d(TAG, "lstHt : " + lstHt);
    		htForSettingsList = lstHt - res.getDimensionPixelSize(R.dimen.action_bar_ht)
    				/**
    				 * subtracting the top-bottom margins
    				 */
    				- 2 * res.getDimensionPixelSize(R.dimen.list_view_m_t_b_language_fragment_tab);
    		Log.d(TAG, "htForSettingsList : " + htForSettingsList);
        	adapter.setHtForSettingsList(htForSettingsList);
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
			adapter = new LanguageAdapter(languages, FragmentUtil.getActivity(this));
			setListAdapter(adapter);
			
		} else {
			adapter.updateContext(FragmentUtil.getActivity(this));
		}
		getListView().setDivider(null);
		
		EventSeekr eventSeekr = FragmentUtil.getApplication(this);
		if (eventSeekr.isTablet()) {
        	getListView().getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
        }
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

	private class LanguageAdapter extends BaseAdapter {

		private List<Locales> languages;
		private Context context;
		private int rowHt;
		
		public LanguageAdapter(List<Locales> languages, Context context) {
			this.languages = languages;
			this.context = context;
		}
		
		public void setHtForSettingsList(int htForSettingsList) {
			this.rowHt = htForSettingsList / languages.size();
			notifyDataSetChanged();
		}


		public void updateContext(Context context) {
			this.context = context;
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
				convertView = LayoutInflater.from(context).inflate(R.layout.list_item_language_tab, null);

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
			if (Locales.isDefaultLocale(context, locale)) {
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
