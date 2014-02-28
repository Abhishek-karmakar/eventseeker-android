package com.wcities.eventseeker;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi;
import com.wcities.eventseeker.api.UserInfoApi.LoginType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.util.FragmentUtil;

public class RepCodeFragment extends FragmentLoadableFromBackStack implements OnClickListener {
	
	private static final String TAG = RepCodeFragment.class.getName();
	
	private EditText edtRepCode;
	private Button btnSubmit;
	private ProgressBar progressBar;
	
	private boolean isSubmitting;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_rep_code, null);
		
		btnSubmit = (Button) v.findViewById(R.id.btnSubmit);
		btnSubmit.setOnClickListener(this);
		edtRepCode = (EditText) v.findViewById(R.id.edtRepCode);
		progressBar = (ProgressBar) v.findViewById(R.id.progressBar);
		
		setVisibility(false);
		
		return v;
	}
	
	private class SubmitRepCode extends AsyncTask<Void, Void, String> {
		
		private EventSeekr eventSeekr;
		private String repCode;
		
		public SubmitRepCode(EventSeekr eventSeekr, String repCode) {
			this.eventSeekr = eventSeekr;
			this.repCode = repCode;
		}

		@Override
		protected String doInBackground(Void... params) {
			String wcitiesId = null;
			LoginType loginType = null;
			UserInfoApi userInfoApi = new UserInfoApi(Api.OAUTH_TOKEN);
			try {
				if (eventSeekr.getFbUserId() != null) {
					loginType = LoginType.facebook;
					userInfoApi.setFbUserId(eventSeekr.getFbUserId());
					
				} else if (eventSeekr.getGPlusUserId() != null) {
					loginType = LoginType.googlePlus;
					userInfoApi.setGPlusUserId(eventSeekr.getGPlusUserId());
				}
				userInfoApi.setUserId(eventSeekr.getWcitiesId());
				JSONObject jsonObject = userInfoApi.syncAccount(repCode, loginType);
				Log.d(TAG, "response = " + jsonObject);

			} catch (ClientProtocolException e) {
				e.printStackTrace();

			} catch (IOException e) {
				e.printStackTrace();

			} catch (JSONException e) {
				e.printStackTrace();
			}
			return wcitiesId;
		}
		
		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			isSubmitting = false;
			setVisibility(true);
		}
	}
	
	private void setVisibility(boolean clearInput) {
		if (isSubmitting) {
			btnSubmit.setVisibility(View.INVISIBLE);
			progressBar.setVisibility(View.VISIBLE);
			
		} else {
			btnSubmit.setVisibility(View.VISIBLE);
			progressBar.setVisibility(View.INVISIBLE);
			if (clearInput) {
				edtRepCode.setText("");
			}
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.btnSubmit:
			EventSeekr eventSeekr = (EventSeekr) FragmentUtil.getActivity(this).getApplication();
			String repCode = edtRepCode.getText().toString();
			if (eventSeekr.getWcitiesId() != null && repCode != null && repCode.length() > 0) {
				isSubmitting = true;
				setVisibility(false);
				new SubmitRepCode(eventSeekr, repCode).execute();
			}
			break;

		default:
			break;
		}
	}
}
