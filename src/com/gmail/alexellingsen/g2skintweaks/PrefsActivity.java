package com.gmail.alexellingsen.g2skintweaks;

import android.os.Bundle;
import android.preference.*;
import android.view.MenuItem;
import android.widget.Toast;

@SuppressWarnings({"ConstantConditions", "NullableProblems"})
public class PrefsActivity extends PreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        getFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new PrefsFragment())
                .commit();
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int itemId = item.getItemId();

        switch (itemId) {
            case android.R.id.home:
                finish();
                break;
        }

        return true;
    }

    public static class PrefsFragment extends PreferenceFragment {

        private static final int MAX_MINIMUM_ZOOM = 85;

        public PrefsFragment() {
        }

        @SuppressWarnings("deprecation")
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            PreferenceManager prefMgr = getPreferenceManager();
            prefMgr.setSharedPreferencesName(Prefs.NAME);
            prefMgr.setSharedPreferencesMode(MODE_WORLD_READABLE);

            addPreferencesFromResource(R.xml.preferences);

            Preference minimumZoomLevel = findPreference(Prefs.MINIMUM_ZOOM_LEVEL);

            minimumZoomLevel.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            int i = Integer.parseInt(newValue.toString());

                            if (i > MAX_MINIMUM_ZOOM) {
                                Toast.makeText(getActivity(),
                                        getString(R.string.minimum_zoom_too_high),
                                        Toast.LENGTH_LONG).show();
                                return false;
                            }

                            return true;
                        }
                    }
            );
        }

        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
            if (preference.getKey().equals("btn_turn_off_power_led")) {
                RootFunctions.turnOffRearPowerLed(getActivity());

                Toast.makeText(getActivity(), getString(R.string.turned_power_led_off),
                        Toast.LENGTH_SHORT).show();
            }

            return true;
        }

    }
}