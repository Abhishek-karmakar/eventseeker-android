package com.wcities.eventseeker.bosch;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.util.FragmentUtil;

public class BoschSearchFragment extends FragmentLoadableFromBackStack implements OnClickListener {

	private static final String TAG = BoschSearchFragment.class.getName();
	
	private EditText edtSearch;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_bosch_search, null);
		
		view.findViewById(R.id.btnSearch).setOnClickListener(this);
		edtSearch = (EditText) view.findViewById(R.id.edtSearch);
		
		return view;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		((BoschMainActivity) FragmentUtil.getActivity(this)).onFragmentResumed(this, 
			BoschMainActivity.INDEX_NAV_ITEM_SEARCH, getResources().getString(R.string.title_search));
	}

	@Override
	public void onClick(View v) {
		
		switch (v.getId()) {

			case R.id.btnSearch:
				onSearchClicked();
				break;
		}
	}
		
	private void onSearchClicked() {
			
		String query = edtSearch.getText().toString().trim();
		query = query.replace("\\n", "");
			
		if(query.equals("")) {
			return;
		}

		Bundle args = new Bundle();
		args.putString(BundleKeys.QUERY, query);
			
		((BoschMainActivity) FragmentUtil.getActivity(this))
			.replaceByFragment(BoschSearchResultFragment.class.getSimpleName(), args);

	}

}