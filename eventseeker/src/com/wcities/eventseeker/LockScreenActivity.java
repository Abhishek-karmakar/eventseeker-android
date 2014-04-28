/**Ford Motor Company
 * September 2012
 * Elizabeth Halash
 */

package com.wcities.eventseeker;

import android.app.Activity;
import android.os.Bundle;

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
    }
    
    //disable back button on lock screen
    @Override
    public void onBackPressed() {
    }
    
    public void exit() {
    	super.finish();
    }
    
    public void onDestroy(){
    	super.onDestroy();
    	instance = null;
    }
}