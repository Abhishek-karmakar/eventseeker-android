package com.wcities.eventseeker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.ford.syncV4.proxy.rpc.enums.Language;
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
		PORTUGUESE("pt", R.string.lang_portuguese),
		
		ENGLISH_AUSTRALIA("en", "AU", Language.EN_AU),
		ENGLISH_UNITED_KINGDOM("en", "GB", Language.EN_GB),
		ENGLISH_UNITED_STATES("en", "US", Language.EN_US),
		FRENCH_CANADA("fr", "CA", Language.FR_CA),
		FRENCH_FRANCE("fr", "FR", Language.FR_FR),
		GERMAN_GERMANY("de", "DE", Language.DE_DE),
		ITALIAN_ITALY("it", "IT", Language.IT_IT),
		SPANISH_SPAIN("es", "ES", Language.ES_ES),
		SPANISH_MEXICO("es", "MX", Language.ES_MX),
		PORTUGUESE_BRAZIL("pt", "BR", Language.PT_BR),
		PORTUGUESE_PORTUGAL("pt", "PT", Language.PT_PT);
		
		private Locales(String localeCode, int localeLanguage) {
			this.localeCode = localeCode;
			this.localeLanguage = localeLanguage;
		}
		
		private Locales(String localeCode, String countryCode, Language fordLanguage) {
			this.localeCode = localeCode;
			this.countryCode = countryCode;
			this.fordLanguage = fordLanguage;
		}
		
		private String localeCode;
		private int localeLanguage;
		private String countryCode;
		private Language fordLanguage;
		
		public String getLocaleCode() {
			return localeCode;
		}
		
		public void setLocaleCode(String localeCode) {
			this.localeCode = localeCode;
		}
		
		public int getLocaleLanguage() {
			return localeLanguage;
		}

		public String getCountryCode() {
			return countryCode;
		}

		public void setLocaleLanguage(int localeLanguage) {
			this.localeLanguage = localeLanguage;
		}
		
		// this function should not be used for ford
		public static Locales getLocaleByLocaleCode(String localeCode) {
			List<Locales> locales = Arrays.asList(Locales.values());
			for (Locales locale : locales) {
				if (locale.getLocaleCode().equals(localeCode) && locale.countryCode == null) {
					return locale;
				}
			}
			return ENGLISH;
		}

		public static boolean isDefaultLocale(Context context, Locales locale) {
			EventSeekr app = (EventSeekr) context.getApplicationContext();
			return locale.equals(app.getLocale());
		}
		
		public static Locales getFordLocaleByLanguage(Language language) {
			if (language == null) {
				return ENGLISH_UNITED_STATES;
			}
			List<Locales> locales = Arrays.asList(Locales.values());
			for (Locales locale : locales) {
				if (locale.fordLanguage == language) {
					return locale;
				}
			}
			return ENGLISH_UNITED_STATES;
		}
		
		public static Locales getFordLocaleByAppLocale(Locale locale) {
			String countryCode = locale.getCountry();
			String languageCode = locale.getLanguage();
			List<Locales> locales = Arrays.asList(Locales.values());
			for (Locales tmpLocale : locales) {
				if (tmpLocale.localeCode.equals(languageCode) && tmpLocale.countryCode != null && 
						tmpLocale.countryCode.equals(countryCode)) {
					return tmpLocale;
				}
			}
			return ENGLISH_UNITED_STATES;
		}
		
		public Language getFordLanguage() {
			return fordLanguage;
		}
		
		public static List<Locales> getMobileLocales() {
			List<Locales> mobileLocales = new ArrayList<LanguageFragment.Locales>();
			List<Locales> locales = Arrays.asList(Locales.values());
			for (Iterator<Locales> iterator = locales.iterator(); iterator.hasNext();) {
				Locales tmpLocale = iterator.next();
				if (tmpLocale.countryCode == null) {
					mobileLocales.add(tmpLocale);
					
				} else {
					break;
				}
			}
			return mobileLocales;
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
		return "Language Screen";
	}
}
