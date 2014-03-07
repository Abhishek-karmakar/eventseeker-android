package com.wcities.eventseeker;

import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.custom.fragment.ListFragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.OnLocaleChangedListener;
import com.wcities.eventseeker.util.FragmentUtil;

public class LanguageFragment extends ListFragmentLoadableFromBackStack {

	private static final String TAG = LanguageFragment.class.getName();

	private List<Locales> languages;

	private LanguageAdapter adapter;
	
	public enum Locales {
		ENGLISH("en", R.string.lang_english),
		FRENCH("fr", R.string.lang_french),
		GERMAN("de", R.string.lang_german),
		ITALIAN("it", R.string.lang_italian),
		SPANISH("es", R.string.lang_spanish),
		PORTUGUESE("pt", R.string.lang_portuguese);
		
		private Locales(String localeCode, int localeLanguage) {
			this.localeCode = localeCode;
			this.localeLanguage = localeLanguage;
		}
		
		private String localeCode;
		private int localeLanguage;
		
		public String getLocaleCode() {
			return localeCode;
		}
		
		public void setLocaleCode(String localeCode) {
			this.localeCode = localeCode;
		}
		
		public int getLocaleLanguage() {
			return localeLanguage;
		}

		public void setLocaleLanguage(int localeLanguage) {
			this.localeLanguage = localeLanguage;
		}
		
		public static Locales getLocaleByLocaleCode(String localeCode) {
			List<Locales> locales = Arrays.asList(Locales.values());
			for (Locales locale : locales) {
				if (locale.getLocaleCode().equals(localeCode)) {
					return locale;
				}
			}
			return ENGLISH;
		}

		public static boolean isDefaultLocale(Context context, Locales locale) {
			EventSeekr app = (EventSeekr) context.getApplicationContext();
			return locale.equals(app.getLocale());
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

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
		languages = Arrays.asList(Locales.values());
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
}
