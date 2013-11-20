package com.wcities.eventseeker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.wcities.eventseeker.ConnectAccountsFragment.Service;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.SyncArtists;
import com.wcities.eventseeker.util.FragmentUtil;

public class PandoraFragment extends Fragment implements OnClickListener {

	private static final String TAG = PandoraFragment.class.getName();
	
	private ProgressBar progressBar;
	private TextView txtLoading;
	private EditText edtUserCredential;
	private Button btnRetrieveArtists;
	
	private boolean isLoading;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_service_enter_credentials_layout, null);
		
		edtUserCredential = (EditText) v.findViewById(R.id.edtUserCredential);
		btnRetrieveArtists = (Button) v.findViewById(R.id.btnRetrieveArtists);
		progressBar = (ProgressBar) v.findViewById(R.id.progressBar);
		txtLoading = (TextView) v.findViewById(R.id.txtLoading);
		
		TextView txtServiceTitle = (TextView) v.findViewById(R.id.txtServiceTitle);
		txtServiceTitle.setText(getResources().getString(R.string.title_pandora));
		txtServiceTitle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.pandora, 0, 0, 0);
		
		updateVisibility();
		
		btnRetrieveArtists.setOnClickListener(this);
		
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
	
	private void updateVisibility() {
		int visibilityDesc = isLoading ? View.GONE : View.VISIBLE;
		edtUserCredential.setVisibility(visibilityDesc);
		btnRetrieveArtists.setVisibility(visibilityDesc);
		
		int visibilityLoading = !isLoading ? View.GONE : View.VISIBLE;
		progressBar.setVisibility(visibilityLoading);
		txtLoading.setVisibility(visibilityLoading);
	}
	
	private void searchUserId(String userId) {
		if (userId == null || userId.isEmpty()) {
			return;
		}
		
		isLoading = true;
		updateVisibility();
		
		userId = userId.replaceAll("\\s", "");

		new NetworkTask().execute("http://feeds.pandora.com/feeds/people/" + userId + "/stations.xml?max=200");
	}
	
	private class NetworkTask extends AsyncTask<String, Void, List<String>> {
		
		@Override
		protected List<String> doInBackground(String... params) {
			String link = params[0];
			
			List<String> artistNames = new ArrayList<String>();
			try {
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				Document doc = db.parse(link);
				doc.normalize();
				
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
				
			} catch (IOException e) {
				e.printStackTrace();
			} 
			return null;
		}
		
		@Override
		protected void onPostExecute(List<String> artistNames) {
			if (artistNames == null) {
				isLoading = false;
				updateVisibility();
				
				Toast toast = Toast.makeText(FragmentUtil.getActivity(PandoraFragment.this),
						"User name could not be found", Toast.LENGTH_SHORT);
				toast.setGravity(Gravity.CENTER, 0, -100);
				toast.show();
				
			} else {
				apiCallFinished(artistNames);
			}
		}
	}
	
	private void apiCallFinished(List<String> artistNames) {
		//Log.d(TAG, "artists size = " + artistNames.size());
		if (artistNames != null && !artistNames.isEmpty()) {
			new SyncArtists(artistNames, (EventSeekr) FragmentUtil.getActivity(this).getApplication(), 
					Service.Pandora, this).execute();
			
		} else {
			FragmentUtil.getActivity(this).onBackPressed();
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
}