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
		
		/*final Button resetSYNCButton = (Button)findViewById(R.id.lockreset);
		resetSYNCButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//if not already started, show main activity and end lock screen activity
				if (MainActivity.getInstance() == null) {
					Intent i = new Intent(getBaseContext(), MainActivity.class);
					i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					getApplication().startActivity(i);
				}
				Log.i(TAG, "start MainActivity");
				
				//reset proxy; do not shut down service
				AppLinkService serviceInstance = AppLinkService.getInstance();
				if (serviceInstance != null) {
					SyncProxyALM proxyInstance = serviceInstance.getProxy();
					if (proxyInstance != null) {
						serviceInstance.reset();
						
					} else {
						serviceInstance.startProxy();
					}
				}
				
				exit();
			}
		});*/
    }
    
    //disable back button on lockscreen
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