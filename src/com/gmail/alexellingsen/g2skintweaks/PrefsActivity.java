package com.gmail.alexellingsen.g2skintweaks;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.MenuItem;
import android.widget.Toast;

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
		}

		public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
			if (preference.getKey().equals("btn_turn_off_power_led")) {
				RootFunctions.turnOffRearPowerLed(getActivity());
				Toast.makeText(getActivity(), "Turned power LED off", Toast.LENGTH_SHORT).show();
			}

			return true;
		}

	}
}