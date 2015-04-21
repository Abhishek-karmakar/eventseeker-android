package com.wcities.eventseeker;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.wcities.eventseeker.GeneralDialogFragment.DialogBtnClickListener;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.SyncArtists;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.constants.Enums.Service;
import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.SyncArtistListener;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.viewdata.ServiceAccount;

public class PandoraFragment extends FragmentLoadableFromBackStack implements OnClickListener, DialogBtnClickListener {

	private static final String TAG = PandoraFragment.class.getName();
	private static final String DIALOG_FRAGMENT_TAG_ERROR = "Error";
	
	private EditText edtUserCredential;
	private Button btnRetrieveArtists;
	
	private ServiceAccount serviceAccount;

	private SyncArtistListener syncArtistListener;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		serviceAccount = (ServiceAccount) getArguments().getSerializable(BundleKeys.SERVICE_ACCOUNTS);

		String tag = getArguments().getString(BundleKeys.SYNC_ARTIST_LISTENER);
		syncArtistListener = (SyncArtistListener) 
				((BaseActivity) FragmentUtil.getActivity(this)).getFragmentByTag(tag);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_service_enter_credentials_layout, null);		
		
		TextView txtServiceTitle = (TextView) v.findViewById(R.id.txtServiceTitle);
		txtServiceTitle.setText(getResources().getString(R.string.title_pandora));
		txtServiceTitle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.pandora, 0, 0, 0);
		
		btnRetrieveArtists = (Button) v.findViewById(R.id.btnRetrieveArtists);
		btnRetrieveArtists.setOnClickListener(this);
		
		edtUserCredential = (EditText) v.findViewById(R.id.edtUserCredential);
		edtUserCredential.setOnEditorActionListener(new OnEditorActionListener() {
			
			@Override
			public boolean onEditorAction(final TextView v, final int actionId, final KeyEvent event) {
				if (event == null || event.getAction() == KeyEvent.ACTION_DOWN || actionId == EditorInfo.IME_NULL) {
					searchUserId(v.getText().toString().trim());
					return true;
				}
				return false;
			}
		});
		edtUserCredential.setOnClickListener(this);
		
		return v;
	}
	
	private void searchUserId(String userId) {
		if (userId == null || userId.length() == 0) {
			return;
		}
		
		serviceAccount.isInProgress = true;
		syncArtistListener.onArtistSyncStarted(true);
		
		userId = userId.replaceAll("\\s", "");

		new NetworkTask().execute("http://feeds.pandora.com/feeds/people/" + userId + "/stations.xml?max=200");
	}
	
	private class NetworkTask extends AsyncTask<String, Void, List<String>> {
		private boolean doesErrorExist;
		private String errorMsg;
		private String errorCode;
		
		private EventSeekr eventSeekr;
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			eventSeekr = (EventSeekr) FragmentUtil.getActivity(PandoraFragment.this).getApplicationContext();
		}
		
		@Override
		protected List<String> doInBackground(String... params) {
			String url = params[0];
			
			List<String> artistNames = new ArrayList<String>();
			try {
				
				/*
				 * We aren't using this method for parsing as if certain error occurs,
				 * like if user has enabled the privacy settings then the parsing method
				 * directly throws FileNotFoundException and doc obj doesn't get initialize.
				 * So, instead of this we are using an alternative approach
				 * 
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				doc = db.parse(link);
				doc.normalize();*/
				
				String xml = getXmlFromUrl(url);
				Document doc = getDocumentFromXml(xml);		
				if (checkForErrorInDoc(doc)) {
					return null;
				}
				
				NodeList nodesList = doc.getElementsByTagName("pandora:artist");
				Log.d(TAG, "nodesList size = " + nodesList.getLength());
				
				for (int i = 0; i < nodesList.getLength(); i++) {
					Node item = nodesList.item(i);
					Log.d(TAG, "name = " + item.getTextContent());
					if (!artistNames.contains(item.getTextContent())) {
						artistNames.add(item.getTextContent());
					}
				}
				return artistNames;
				
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
				
			} catch (SAXException e) {
				e.printStackTrace();
				doesErrorExist = true;
				errorMsg = e.getMessage();
				
			} catch (IOException e) {
				e.printStackTrace();
			} 
			return null;
		}
		
		public String getXmlFromUrl(String url) throws ParseException, IOException {
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(url);
            HttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity httpEntity = httpResponse.getEntity();

	        return EntityUtils.toString(httpEntity);
	    }
		
		public Document getDocumentFromXml(String xml) throws ParserConfigurationException, SAXException, IOException{
			InputSource is = new InputSource();
			is.setCharacterStream(new StringReader(xml));

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            return db.parse(is);
	    }
		
		public boolean checkForErrorInDoc(Document doc) {
			/*NodeList nodeList = doc.getElementsByTagName("h2");
        	Node item = nodeList.item(0); 
        	errorCode = item.getTextContent();
        	Log.d(TAG, errorCode);*/
        	
        	NodeList nodeList = doc.getElementsByTagName("pre");
        	if (nodeList.getLength() > 0) {
	        	Node item = nodeList.item(0); 
	        	errorMsg = item.getTextContent();
	        	Log.d(TAG, errorMsg);
	        	
	        	doesErrorExist = true;
				return doesErrorExist;
        	}
	        	
	        doesErrorExist = false;
			return doesErrorExist;
	    }
		
		@Override
		protected void onPostExecute(List<String> artistNames) {
			if (artistNames == null) {

				if (doesErrorExist) {
					GeneralDialogFragment generalDialogFragment = GeneralDialogFragment.newInstance(PandoraFragment.this,
							"Error", errorMsg, "Ok", null, false);
					generalDialogFragment.show(((ActionBarActivity) FragmentUtil.getActivity(PandoraFragment.this))
							.getSupportFragmentManager(), DIALOG_FRAGMENT_TAG_ERROR);
				}
				
				eventSeekr.setSyncCount(Service.Pandora, EventSeekr.UNSYNC_COUNT);
			} else {
				apiCallFinished(artistNames, eventSeekr);
			}
		}
	}
	
	private void apiCallFinished(List<String> artistNames, EventSeekr app) {
		//Log.d(TAG, "artists size = " + artistNames.size());
		if (artistNames != null) {
			new SyncArtists(Api.OAUTH_TOKEN, artistNames, app, Service.Pandora, /*this,*/ 
					Service.Pandora.getArtistSource()).execute();
		} else {
			Activity activity = FragmentUtil.getActivity(this);
			if(activity != null) {
				activity.onBackPressed();
			}
		}
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.btnRetrieveArtists:
			searchUserId(edtUserCredential.getText().toString().trim());
			break;
		
		case R.id.edtUserCredential:
			edtUserCredential.selectAll();
			break;

		default:
			break;
		}
	}

	@Override
	public void doPositiveClick(String dialogTag) {}

	@Override
	public void doNegativeClick(String dialogTag) {
		DialogFragment dialogFragment = (DialogFragment) getChildFragmentManager().findFragmentByTag(DIALOG_FRAGMENT_TAG_ERROR);
		if (dialogFragment != null) {
			dialogFragment.dismiss();
		}
	}

	@Override
	public String getScreenName() {
		return ScreenNames.PANDORA_SYNC;
	}
}
