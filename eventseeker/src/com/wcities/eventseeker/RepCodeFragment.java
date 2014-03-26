package com.wcities.eventseeker;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.wcities.eventseeker.DrawerListFragment.DrawerListFragmentListener;
import com.wcities.eventseeker.GeneralDialogFragment.DialogBtnClickListener;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi;
import com.wcities.eventseeker.api.UserInfoApi.LoginType;
import com.wcities.eventseeker.api.UserInfoApi.RepCodeResponse;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public class RepCodeFragment extends FragmentLoadableFromBackStack implements OnClickListener, DialogBtnClickListener {
	
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
		
		setVisibility();
		
		return v;
	}
	
	private class SubmitRepCode extends AsyncTask<Void, Void, Integer> {
		
		private EventSeekr eventSeekr;
		private String repCode;
		
		public SubmitRepCode(EventSeekr eventSeekr, String repCode) {
			this.eventSeekr = eventSeekr;
			this.repCode = repCode;
		}

		@Override
		protected Integer doInBackground(Void... params) {
			int repCodeResponse = RepCodeResponse.UNKNOWN_ERROR.getRepCode();
			LoginType loginType = null;
			UserInfoApi userInfoApi = new UserInfoApi(Api.OAUTH_TOKEN);
			try {
				if (eventSeekr.getFbUserId() != null) {
					loginType = LoginType.facebook;
					userInfoApi.setFbUserId(eventSeekr.getFbUserId());
					userInfoApi.setFbEmailId(eventSeekr.getFbEmailId());
					
				} else if (eventSeekr.getGPlusUserId() != null) {
					loginType = LoginType.googlePlus;
					userInfoApi.setGPlusUserId(eventSeekr.getGPlusUserId());
					userInfoApi.setGPlusEmailId(eventSeekr.getGPlusEmailId());
				}
				userInfoApi.setUserId(eventSeekr.getWcitiesId());
				JSONObject jsonObject = userInfoApi.syncAccount(repCode, loginType);
				//Log.d(TAG, "response = " + jsonObject);
				UserInfoApiJSONParser userInfoApiJSONParser = new UserInfoApiJSONParser();
				repCodeResponse = userInfoApiJSONParser.getRepCodeResponse(jsonObject);

			} catch (ClientProtocolException e) {
				e.printStackTrace();

			} catch (IOException e) {
				e.printStackTrace();

			} catch (JSONException e) {
				e.printStackTrace();
			}
			return repCodeResponse;
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			super.onPostExecute(result);
			RepCodeResponse repCodeResponse = RepCodeResponse.getRepCodeResponse(result);
			Activity activity = FragmentUtil.getActivity(RepCodeFragment.this);
			Toast.makeText(activity, repCodeResponse.getMsg(activity.getResources()), Toast.LENGTH_SHORT).show();
			isSubmitting = false;
			setVisibility();
		}
	}
	
	private void setVisibility() {
		if (isSubmitting) {
			btnSubmit.setVisibility(View.INVISIBLE);
			progressBar.setVisibility(View.VISIBLE);
			
		} else {
			btnSubmit.setVisibility(View.VISIBLE);
			progressBar.setVisibility(View.INVISIBLE);
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.btnSubmit:
			EventSeekr eventSeekr = (EventSeekr) FragmentUtil.getActivity(this).getApplication();
			if (eventSeekr.getWcitiesId() == null || (eventSeekr.getFbUserId() == null && eventSeekr.getGPlusUserId() == null)) {
				FragmentUtil.showLoginNeededForRepCodeSubmission(getChildFragmentManager(), FragmentUtil.getActivity(this));
				return;
			}
			
			String repCode = edtRepCode.getText().toString();
			if (repCode == null || repCode.length() == 0) {
				Toast.makeText(FragmentUtil.getActivity(this), R.string.rep_code_enter_a_valid_repcode, Toast.LENGTH_SHORT).show();
				return;
			}
			
			if (repCode != null && repCode.length() > 0) {
				boolean isStarted = AsyncTaskUtil.executeAsyncTask(new SubmitRepCode(eventSeekr, repCode), true);
				if (isStarted) {
					isSubmitting = true;
					setVisibility();
				}
			}
			break;

		default:
			break;
		}
	}

	@Override
	public void doPositiveClick(String dialogTag) {
		if (dialogTag.equals(AppConstants.DIALOG_FRAGMENT_TAG_LOGIN_TO_SUBMIT_REP_CODE)) {
			((DrawerListFragmentListener)FragmentUtil.getActivity(this)).onDrawerItemSelected(
					MainActivity.INDEX_NAV_ITEM_CONNECT_ACCOUNTS);
		}
	}
	
	@Override
	public void doNegativeClick(String dialogTag) {
		if (dialogTag.equals(AppConstants.DIALOG_FRAGMENT_TAG_LOGIN_TO_SUBMIT_REP_CODE)) {
			DialogFragment dialogFragment = (DialogFragment) getChildFragmentManager().findFragmentByTag(
					AppConstants.DIALOG_FRAGMENT_TAG_LOGIN_TO_SUBMIT_REP_CODE);
			if (dialogFragment != null) {
				dialogFragment.dismiss();
			}
		}
	}
}
