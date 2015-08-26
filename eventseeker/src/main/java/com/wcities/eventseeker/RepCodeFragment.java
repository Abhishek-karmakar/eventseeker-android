package com.wcities.eventseeker;

import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.wcities.eventseeker.GeneralDialogFragment.DialogBtnClickListener;
import com.wcities.eventseeker.SettingsFragment.OnSettingsItemClickedListener;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi;
import com.wcities.eventseeker.api.UserInfoApi.RepCodeResponse;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.Enums.SettingsItem;
import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.FragmentUtil;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class RepCodeFragment extends FragmentLoadableFromBackStack implements OnClickListener, DialogBtnClickListener {
	
	private static final String TAG = RepCodeFragment.class.getName();
	
	private EditText edtRepCode;
	private Button btnSubmit;
	private RelativeLayout rltProgressBar;
	
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
		rltProgressBar = (RelativeLayout) v.findViewById(R.id.rltProgressBar);
		
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
			UserInfoApi userInfoApi = new UserInfoApi(Api.OAUTH_TOKEN);
			try {
				JSONObject jsonObject = null;
				/*if (eventSeekr.getFbUserId() != null) {
					jsonObject = userInfoApi.syncAccount(repCode, eventSeekr.getFbUserId(), eventSeekr.getFbEmailId(), 
							UserType.fb, eventSeekr.getWcitiesId());
					
				} else if (eventSeekr.getGPlusUserId() != null) {
					jsonObject = userInfoApi.syncAccount(repCode, eventSeekr.getGPlusUserId(), eventSeekr.getGPlusEmailId(), 
							UserType.google, eventSeekr.getWcitiesId());
				
				} else {*/
				/**
				 * 20-02-2015:
				 * as per discussion with Samir, now onwards whatever will be the user login type(fb, google, 
				 * wcities email). We have to make this new repcode api call. So, commented above checks and 
				 * SyncAccount repcode Api call.
				 * 
				 */
				jsonObject = userInfoApi.updateRepcodeWithWcitiesId(repCode, eventSeekr.getWcitiesId());
				/*}*/
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
			Resources res = eventSeekr.getResources();
			GeneralDialogFragment generalDialogFragment = GeneralDialogFragment.newInstance(RepCodeFragment.this,
					res.getString(R.string.rep_code_submission), repCodeResponse.getMsg(res), 
					res.getString(R.string.ok), null, false);
			generalDialogFragment.show(((ActionBarActivity) FragmentUtil.getActivity(RepCodeFragment.this))
					.getSupportFragmentManager(), AppConstants.DIALOG_FRAGMENT_TAG_SUBMIT_REP_CODE_RESPONSE);
			isSubmitting = false;
			setVisibility();
		}
	}
	
	private void setVisibility() {
		if (isSubmitting) {
			btnSubmit.setVisibility(View.INVISIBLE);
			rltProgressBar.setVisibility(View.VISIBLE);
			
		} else {
			btnSubmit.setVisibility(View.VISIBLE);
			rltProgressBar.setVisibility(View.INVISIBLE);
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.btnSubmit:
			EventSeekr eventSeekr = (EventSeekr) FragmentUtil.getActivity(this).getApplication();
			
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
			// set firstTimeLaunch=false so as to keep facebook & google sign in rows visible.
			((EventSeekr)FragmentUtil.getActivity(this).getApplication()).updateFirstTimeLaunch(false);
			/*((DrawerListFragmentListener)FragmentUtil.getActivity(this)).onDrawerItemSelected(
					MainActivity.INDEX_NAV_ITEM_CONNECT_ACCOUNTS, null);*/
			((OnSettingsItemClickedListener) FragmentUtil.getActivity(this)).onSettingsItemClicked(SettingsItem.SYNC_ACCOUNTS, null);
		}
	}
	
	@Override
	public void doNegativeClick(String dialogTag) {
		if (dialogTag.equals(AppConstants.DIALOG_FRAGMENT_TAG_LOGIN_TO_SUBMIT_REP_CODE) || 
				dialogTag.equals(AppConstants.DIALOG_FRAGMENT_TAG_SUBMIT_REP_CODE_RESPONSE)) {
			DialogFragment dialogFragment = (DialogFragment) getChildFragmentManager().findFragmentByTag(
					dialogTag);
			if (dialogFragment != null) {
				dialogFragment.dismiss();
			}
		}
	}

	@Override
	public String getScreenName() {
		return ScreenNames.REPCODE_SCREEN;
	}
}
