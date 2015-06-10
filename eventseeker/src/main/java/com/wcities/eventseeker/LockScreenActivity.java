/**Ford Motor Company
 * September 2012
 * Elizabeth Halash
 */

package com.wcities.eventseeker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;

import com.wcities.eventseeker.applink.service.AppLinkService;
import com.wcities.eventseeker.constants.AppConstants;

public class LockScreenActivity extends Activity {
	
	protected static final String TAG = "LockScreenActivity";
	int itemcmdID = 0;
	int subMenuId = 0;
	private static LockScreenActivity instance = null;
	
	public static LockScreenActivity getInstance() {
		return instance;
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        setContentView(R.layout.activity_lock_screen);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onResume() {
        super.onResume();
        /**
         * 03-06-2015:
         * This just to put the Service back to Background Process, which was made to Foreground
         * in onPause(). Here, if the Service is already running then new instance would not be
         * created, instead the onStartCommand() would be called with this new intent.
         */
        Intent intent = new Intent(getApplicationContext(), AppLinkService.class);
        intent.setAction(AppConstants.ACTION_APPLINK_SERVICE_STOP_FOREGROUND);
        startService(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        /**
         * 03-06-2015:
         * This is just to put the Service in Foreground Process, because this Activity(actually the app)
         * is now going to run in background and thus if we will not make this Service as Foreground
         * Process then in Low Memory Scenario it might get killed (seen this issue in Sony Xperia L).
         * Here, if the Service is already running then new instance would not be created, instead
         * the onStartCommand() would be called with this new intent.
         */
        Intent intent = new Intent(getApplicationContext(), AppLinkService.class);
        intent.setAction(AppConstants.ACTION_APPLINK_SERVICE_START_FOREGROUND);
        startService(intent);
    }
    
    //disable back button on lock screen
    @Override
    public void onBackPressed() {}
    
    public void exit() {
    	super.finish();
    }

    public void onDestroy(){
        super.onDestroy();
        instance = null;

        /**
         * 03-06-2015:
         * This just to put the Service back to Background Process, which was made to Foreground
         * in onPause(). Here, if the Service is already running then new instance would not be
         * created, instead the onStartCommand() would be called with this new intent.
         */
        Intent intent = new Intent(getApplicationContext(), AppLinkService.class);
        intent.setAction(AppConstants.ACTION_APPLINK_SERVICE_STOP_FOREGROUND);
        startService(intent);
    }
}