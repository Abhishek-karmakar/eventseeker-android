package com.wcities.eventseeker.custom.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import java.lang.reflect.Field;

/**
 * This class is added to retain child fragment manager which is not done in latest support library due to a
 * left over bug. Ref: http://ideaventure.blogspot.com.au/2014/10/nested-retained-fragment-lost-state.html
 * https://code.google.com/p/android/issues/detail?id=74222
 */
public abstract class PublishEventFragmentRetainingChildFragmentManager extends PublishEventFragment {

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
