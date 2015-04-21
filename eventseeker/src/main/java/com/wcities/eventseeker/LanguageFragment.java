package com.wcities.eventseeker;

import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.constants.Enums.Locales;
import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.custom.fragment.ListFragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.OnLocaleChangedListener;
import com.wcities.eventseeker.util.FragmentUtil;

public class LanguageFragment extends ListFragmentLoadableFromBackStack {

	private static final String TAG = LanguageFragment.class.getName();

	private List<Locales> languages;

	private LanguageAdapter adapter;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		getListView().setBackgroundColor(Color.WHITE);
		getListView().setPadding(0, 
			(int) FragmentUtil.getResources(this).getDimensionPixelSize(R.dimen.common_t_mar_pad_for_all_layout), 0, 0);			
		
		getListView().setDivider(null);
		
		if (languages == null) {
			loadLanguages();
			adapter = new LanguageAdapter(languages, FragmentUtil.getActivity(this));
			setListAdapter(adapter);
			
		} else {
			adapter.updateContext(FragmentUtil.getActivity(this));
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
		
		public LanguageAdapter(List<Locales> languages, Context context) {
			this.languages = languages;
			this.context = context;
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
				convertView = LayoutInflater.from(context).inflate(R.layout.list_item_language, null);

				vhLanguages = new VHLanguages();
				vhLanguages.txtLanguage = (TextView) convertView.findViewById(R.id.txtLanguage);
				vhLanguages.imgSelected = (ImageView) convertView.findViewById(R.id.imgSelected);
				convertView.setTag(vhLanguages);
				
			} else {
				vhLanguages = (VHLanguages) convertView.getTag();
			}

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

	@Override
	public String getScreenName() {
		return ScreenNames.LANGUAGE_SCREEN;
	}
}
