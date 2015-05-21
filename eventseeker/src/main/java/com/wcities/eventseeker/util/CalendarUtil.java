package com.wcities.eventseeker.util;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v4.app.Fragment;

import com.wcities.eventseeker.BaseActivity;
import com.wcities.eventseeker.CalendarActivity;
import com.wcities.eventseeker.GeneralDialogFragment;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Event;

import java.io.File;

/**
 * Created by win6 on 5/19/2015.
 */
public class CalendarUtil {

    private static final String TAG = CalendarUtil.class.getSimpleName();

    public static void showAddToCalendarDialog(Fragment fragment, GeneralDialogFragment.DialogBtnClickListener dialogBtnClickListener) {
        //Log.d(TAG, "showAddToCalendarDialog()");
        Resources res = FragmentUtil.getResources(fragment);
        GeneralDialogFragment generalDialogFragment = GeneralDialogFragment.newInstance(dialogBtnClickListener,
                res.getString(R.string.event_saved),
                res.getString(R.string.add_event_to_google_calendar),
                res.getString(R.string.btn_cancel),
                res.getString(R.string.btn_Ok), true);
        BaseActivity baseActivity = (BaseActivity) FragmentUtil.getActivity(fragment);
        if (!baseActivity.isOnSaveInstanceStateCalled()) {
            //Log.d(TAG, "!onSaveInstanceCalled");
            generalDialogFragment.show(baseActivity.getSupportFragmentManager(),
                    AppConstants.DIALOG_FRAGMENT_TAG_EVENT_SAVED);
        }
        //Log.d(TAG, "showAddToCalendarDialog() ends");
    }

    public static void addEventToCalendar(Fragment fragment, Event event) {
        EventSeekr eventSeekr = FragmentUtil.getApplication(fragment);

        Intent intent = new Intent(eventSeekr, CalendarActivity.class);
        /**
         * following line is included although we are creating explicit intent since we have check
         * in CalendarActivity based on type.
         */
        intent.setType("image/*");
        Resources res = eventSeekr.getResources();

        intent.putExtra(Intent.EXTRA_SUBJECT, res.getString(R.string.title_event_details));
        String message = "Checkout " + event.getName();
        if (event.getSchedule() != null && event.getSchedule().getVenue() != null) {
            message += " @ " + event.getSchedule().getVenue().getName();
        }
        String link = event.getEventUrl();
        if (link == null) {
            link = "http://eventseeker.com/event/" + event.getId();
        }
        message += ": " + link;
        intent.putExtra(Intent.EXTRA_TEXT, message);

        // required to handle "add to calendar" action
        eventSeekr.setEventToAddToCalendar(event);

        String key = event.getKey(BitmapCacheable.ImgResolution.LOW);
        BitmapCache bitmapCache = BitmapCache.getInstance();
        Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
        if (bitmap != null) {
            File tmpFile = FileUtil.createTempShareImgFile(eventSeekr, bitmap);
            if (tmpFile != null) {
                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(tmpFile));
            }
        }

        //Log.d(TAG, "isAdded = " + isAdded());
        // Using any outside context (app context in this case) from activity requires this flag.
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        /**
         * If we use activity context here to call startActivity(), then for mobile app if user presses back button
         * instantly after clicking save event button (resulting in this fragment
         * getting destroyed), then startActivity() call won't work because fragment is already detached.
         * Hence using eventSeeker app context so that startActivity() call will work as
         * long as any eventseeker screen (fragment) is visible.
         */
        eventSeekr.startActivity(intent);
    }
}
