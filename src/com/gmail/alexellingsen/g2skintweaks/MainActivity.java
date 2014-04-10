package com.gmail.alexellingsen.g2skintweaks;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class MainActivity extends Activity {

	public static final String	PREF_NAME					= "G2SkinTweaks";
	public static final String	PREF_ENABLE_REPLACE_SWICTH	= "enableReplaceSwitch";
	public static final String	PREF_ENABLE_SQUARE_BUBBLE	= "enableSquareBubble";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction().add(R.id.container, new PlaceholderFragment()).commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();

		if (id == R.id.action_settings) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	public static class PlaceholderFragment extends Fragment {

		private CheckBox	chbReplaceSwitch		= null;
		private CheckBox	chbSquareBubble			= null;

		private boolean		ENABLE_REPLACE_SWITCH	= false;
		private boolean		ENABLE_SQUARE_BUBBLE	= false;

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container, false);

			SharedPreferences settings = getActivity().getSharedPreferences(PREF_NAME, 0);

			ENABLE_REPLACE_SWITCH = settings.getBoolean(PREF_ENABLE_REPLACE_SWICTH, ENABLE_REPLACE_SWITCH);
			ENABLE_SQUARE_BUBBLE = settings.getBoolean(PREF_ENABLE_SQUARE_BUBBLE, ENABLE_SQUARE_BUBBLE);

			this.chbReplaceSwitch = (CheckBox) rootView.findViewById(R.id.chb_replace_switch);
			this.chbReplaceSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					SharedPreferences settings = getActivity().getSharedPreferences(PREF_NAME, 0);
					SharedPreferences.Editor editor = settings.edit();
					editor.putBoolean(PREF_ENABLE_REPLACE_SWICTH, isChecked);

					// Commit the edits!
					editor.commit();
				}
			});
			this.chbReplaceSwitch.setChecked(ENABLE_REPLACE_SWITCH);

			this.chbSquareBubble = (CheckBox) rootView.findViewById(R.id.chb_square_bubble);
			this.chbSquareBubble.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					SharedPreferences settings = getActivity().getSharedPreferences(PREF_NAME, 0);
					SharedPreferences.Editor editor = settings.edit();
					editor.putBoolean(PREF_ENABLE_SQUARE_BUBBLE, isChecked);

					// Commit the edits!
					editor.commit();
				}
			});
			this.chbSquareBubble.setChecked(ENABLE_SQUARE_BUBBLE);

			return rootView;
		}
	}

}
