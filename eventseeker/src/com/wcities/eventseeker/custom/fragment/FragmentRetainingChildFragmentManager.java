package com.wcities.eventseeker.custom.fragment;

import java.lang.reflect.Field;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

public class FragmentRetainingChildFragmentManager extends Fragment {

    //As we setRetainInstanceState(true), this field will hold 
    //the reference of the old ChildFragmentManager
    private FragmentManager mRetainedChildFragmentManager;
 
    public FragmentManager childFragmentManager() {
        if (mRetainedChildFragmentManager == null) {
            mRetainedChildFragmentManager = getChildFragmentManager();
        }
        return mRetainedChildFragmentManager;
    }
 
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }
 
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
 
        if (mRetainedChildFragmentManager != null) {
            //restore the last retained child fragment manager to the new 
            //created fragment
            try {
                Field childFMField = Fragment.class.getDeclaredField("mChildFragmentManager");
                childFMField.setAccessible(true);
                childFMField.set(this, mRetainedChildFragmentManager);
                
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
                
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
}
