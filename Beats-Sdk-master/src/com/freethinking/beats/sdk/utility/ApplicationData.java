package com.freethinking.beats.sdk.utility;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

public class ApplicationData {

    @SuppressWarnings("SpellCheckingInspection")
    public static String getStorePreferencesKey(Context context) {
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            return ai.metaData.getString("com.freethinking.beats.sdk.storedprefskey");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (NullPointerException e) {
            e.printStackTrace();
            return null;
        }
    }

    @SuppressWarnings("SpellCheckingInspection")
    public static String getApplicationName(Context context) {
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            return ai.metaData.getString("com.freethinking.beats.sdk.applicationname");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (NullPointerException e) {
            e.printStackTrace();
            return null;
        }
    }

    @SuppressWarnings("SpellCheckingInspection")
    public static String getApplicationWelcome(Context context) {
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            return ai.metaData.getString("com.freethinking.beats.sdk.applicationwelcome");
        } catch (PackageManager.NameNotFoundException e) {
            return "Welcome!";
        } catch (NullPointerException e) {
            return "Welcome!";
        }
    }



}
