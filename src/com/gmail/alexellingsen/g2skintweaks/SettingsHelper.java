package com.gmail.alexellingsen.g2skintweaks;

import android.content.Context;
import android.content.SharedPreferences;
import de.robv.android.xposed.XSharedPreferences;

public class SettingsHelper {

	private SharedPreferences preferences;
	private XSharedPreferences xPreferences;

	public SettingsHelper() {
		xPreferences = new XSharedPreferences(Prefs.PACKAGE_NAME, Prefs.NAME);
		xPreferences.makeWorldReadable();
	}

	@SuppressWarnings("deprecation")
	public SettingsHelper(Context context) {
		preferences = context.getSharedPreferences(Prefs.NAME, Context.MODE_WORLD_READABLE);
	}

	public boolean getBoolean(String key, boolean defValue) {
		if (preferences != null) {
			return preferences.getBoolean(key, defValue);
		} else {
			xPreferences.reload();

			return xPreferences.getBoolean(key, defValue);
		}
	}

	public int getInt(String key, int defValue) {
		if (preferences != null) {
			return preferences.getInt(key, defValue);
		} else {
			xPreferences.reload();

			return xPreferences.getInt(key, defValue);
		}
	}

	public boolean putBoolean(String key, boolean value) {
		if (preferences != null) {
			return preferences.edit().putBoolean(key, value).commit();
		} else {
			return xPreferences.edit().putBoolean(key, value).commit();
		}
	}

	public boolean putInt(String key, int value) {
		if (preferences != null) {
			return preferences.edit().putInt(key, value).commit();
		} else {
			return xPreferences.edit().putInt(key, value).commit();
		}
	}

}