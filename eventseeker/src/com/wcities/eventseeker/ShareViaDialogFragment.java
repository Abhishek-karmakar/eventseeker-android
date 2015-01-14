package com.wcities.eventseeker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.wcities.eventseeker.analytics.GoogleAnalyticsTracker;
import com.wcities.eventseeker.analytics.GoogleAnalyticsTracker.Type;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.util.FileUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public class ShareViaDialogFragment extends DialogFragment {

	private static final String TAG = ShareViaDialogFragment.class.getSimpleName();
	
	private List<ShareDropdownItem> shareDropdownItems;
	
	public static ShareViaDialogFragment newInstance(Event event, String screenName) {
		ShareViaDialogFragment frag = new ShareViaDialogFragment();
		Bundle args = new Bundle();
		args.putSerializable(BundleKeys.EVENT, event);
		args.putSerializable(BundleKeys.SCREEN_NAME, screenName);
		frag.setArguments(args);
		return frag;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.share_via)
		.setAdapter(new ShareDropdownAdapter(this), new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				//Log.d(TAG, "onClick()");
				Event event = (Event) getArguments().getSerializable(BundleKeys.EVENT);
				Resources res = FragmentUtil.getResources(ShareViaDialogFragment.this);
				EventSeekr eventSeekr = (EventSeekr) FragmentUtil.getActivity(ShareViaDialogFragment.this).getApplication();
				
				ShareDropdownItem shareDropdownItem = shareDropdownItems.get(which);
				Intent intent = new Intent(Intent.ACTION_SEND);
				intent.setType("image/*");
				intent.setClassName(shareDropdownItem.pkgName, shareDropdownItem.clsName);
				intent.putExtra(Intent.EXTRA_SUBJECT, res.getString(R.string.title_event_details));
				String message = "Checkout " + event.getName();
			    if (event.getSchedule() != null && event.getSchedule().getVenue() != null) {
			    	message += " @ " + event.getSchedule().getVenue().getName();
			    }
			    if (event.getEventUrl() != null) {
			    	message += ": " + event.getEventUrl();
			    }
			    intent.putExtra(Intent.EXTRA_TEXT, message);
			    String key = event.getKey(ImgResolution.LOW);
		        BitmapCache bitmapCache = BitmapCache.getInstance();
				Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
				if (bitmap != null) {
					File tmpFile = FileUtil.createTempShareImgFile(eventSeekr, bitmap);
					if (tmpFile != null) {
						intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(tmpFile));
					}
				}
				
				//Log.d(TAG, "shareTarget = " + shareTarget);
				if (eventSeekr.getPackageName().equals(shareDropdownItem.pkgName)) {
					//Log.d(TAG, "shareTarget = " + shareTarget);
					// required to handle "add to calendar" action
					eventSeekr.setEventToAddToCalendar(event);
				}
				
				GoogleAnalyticsTracker.getInstance().sendShareEvent(eventSeekr, getArguments().getString(
						BundleKeys.SCREEN_NAME), shareDropdownItem.pkgName, Type.Event, event.getId());
				
	            startActivity(intent);
			}
		});
		
        // Create the AlertDialog object and return it
        Dialog dialog = builder.create();
		return dialog;
	}
	
	private void buildShareListItems() {
		if (shareDropdownItems == null) {
			shareDropdownItems = new ArrayList<ShareDropdownItem>();
			
			Intent sendIntent = new Intent();
			sendIntent.setAction(Intent.ACTION_SEND);
			sendIntent.setType("image/*");
			
			List<ResolveInfo> resolveInfos = getActivity().getPackageManager().queryIntentActivities(sendIntent, 0);
			//Log.i(TAG, "list size="+resolveInfos.size());
			for (int i = 0; i < resolveInfos.size(); i++) {
			    ResolveInfo resolveInfo = resolveInfos.get(i);
			    ShareDropdownItem shareDropdownItem = new ShareDropdownItem();
			    shareDropdownItem.appLabel = resolveInfo.loadLabel(getActivity().getPackageManager()).toString();
			    shareDropdownItem.icon = resolveInfo.loadIcon(getActivity().getPackageManager());
			    shareDropdownItem.pkgName = resolveInfo.activityInfo.packageName;
			    shareDropdownItem.clsName = resolveInfo.activityInfo.name;
			    shareDropdownItems.add(shareDropdownItem);
			}
		}
	}
	
	private static class ShareDropdownAdapter extends BaseAdapter {
		
	    private ShareViaDialogFragment shareViaDialogFragment;

		public ShareDropdownAdapter(ShareViaDialogFragment shareViaDialogFragment) {
			this.shareViaDialogFragment = shareViaDialogFragment;
			this.shareViaDialogFragment.buildShareListItems();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = LayoutInflater.from(FragmentUtil.getActivity(shareViaDialogFragment)).inflate(
						R.layout.share_dropdown_item, null);
			} 
			
			ShareDropdownItem shareDropdownItem = getItem(position);
			((ImageView)convertView.findViewById(R.id.imgAppIcon)).setImageDrawable(shareDropdownItem.icon);
			((TextView)convertView.findViewById(R.id.txtAppLabel)).setText(shareDropdownItem.appLabel);
			
			return convertView;
		}
		
		@Override
		public int getCount() {
			return shareViaDialogFragment.shareDropdownItems.size();
		}

		@Override
		public ShareDropdownItem getItem(int position) {
			return shareViaDialogFragment.shareDropdownItems.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}
    }
	
	private class ShareDropdownItem {
		private Drawable icon;
		private String appLabel, pkgName, clsName;
	}
}
