package com.example.mapdemo;

import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.SupportStreetViewPanoramaFragment;
import com.google.android.gms.maps.model.LatLng;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

/**
 * This shows how to create an activity with static streetview (all options have been switched off)
 */
public class StreetViewPanoramaOptionsDemoActivity extends FragmentActivity {

    // Cole St, San Fran
    private static final LatLng SAN_FRAN = new LatLng(37.765927, -122.449972);

    private StreetViewPanorama mSvp;

    private CheckBox mStreetNameCheckbox;
    private CheckBox mNavigationCheckbox;
    private CheckBox mZoomCheckbox;
    private CheckBox mPanningCheckbox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.street_view_panorama_options_demo);

        mStreetNameCheckbox = (CheckBox) findViewById(R.id.streetnames);
        mNavigationCheckbox = (CheckBox) findViewById(R.id.navigation);
        mZoomCheckbox = (CheckBox) findViewById(R.id.zoom);
        mPanningCheckbox = (CheckBox) findViewById(R.id.panning);

        setUpStreetViewPanoramaIfNeeded(savedInstanceState);
    }

    private void setUpStreetViewPanoramaIfNeeded(Bundle savedInstanceState) {
        if (mSvp == null) {
            mSvp = ((SupportStreetViewPanoramaFragment)
                getSupportFragmentManager().findFragmentById(R.id.streetviewpanorama))
                    .getStreetViewPanorama();
            if (mSvp != null) {
                if (savedInstanceState == null) {
                    mSvp.setPosition(SAN_FRAN);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mSvp != null) {
            mSvp.setStreetNamesEnabled(mStreetNameCheckbox.isChecked());
            mSvp.setUserNavigationEnabled(mNavigationCheckbox.isChecked());
            mSvp.setZoomGesturesEnabled(mZoomCheckbox.isChecked());
            mSvp.setPanningGesturesEnabled(mPanningCheckbox.isChecked());
        }
    }

    private boolean checkReady() {
        if (mSvp == null) {
            Toast.makeText(this, R.string.map_not_ready, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public void onStreetNamesToggled(View view) {
        if (!checkReady()) {
            return;
        }
        mSvp.setStreetNamesEnabled(mStreetNameCheckbox.isChecked());
    }

    public void onNavigationToggled(View view) {
        if (!checkReady()) {
            return;
        }
        mSvp.setUserNavigationEnabled(mNavigationCheckbox.isChecked());
    }

    public void onZoomToggled(View view) {
        if (!checkReady()) {
            return;
        }
        mSvp.setZoomGesturesEnabled(mZoomCheckbox.isChecked());
    }

    public void onPanningToggled(View view) {
        if (!checkReady()) {
            return;
        }
        mSvp.setPanningGesturesEnabled(mPanningCheckbox.isChecked());
    }
}
