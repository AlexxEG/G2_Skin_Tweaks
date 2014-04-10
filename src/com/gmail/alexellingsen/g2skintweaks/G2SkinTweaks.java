package com.gmail.alexellingsen.g2skintweaks;

import android.content.res.XModuleResources;
import android.content.res.XResources;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;

public class G2SkinTweaks implements IXposedHookZygoteInit, IXposedHookInitPackageResources {

	private static String	MODULE_PATH				= null;
	private static boolean	ENABLE_REPLACE_SWITCH	= false;
	private static boolean	ENABLE_SQUARE_BUBBLE	= false;

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		MODULE_PATH = startupParam.modulePath;

		XSharedPreferences settings = new XSharedPreferences("com.gmail.alexellingsen.g2skintweaks", MainActivity.PREF_NAME);

		ENABLE_REPLACE_SWITCH = settings.getBoolean(MainActivity.PREF_ENABLE_REPLACE_SWICTH, ENABLE_REPLACE_SWITCH);
		ENABLE_SQUARE_BUBBLE = settings.getBoolean(MainActivity.PREF_ENABLE_SQUARE_BUBBLE, ENABLE_SQUARE_BUBBLE);

		if (ENABLE_REPLACE_SWITCH) {
			XModuleResources modRes = XModuleResources.createInstance(MODULE_PATH, null);
			XResources.setSystemWideReplacement("com.lge.internal", "drawable", "switch_track_holo_dark", modRes.fwd(R.drawable.replacement_switch));
			XResources.setSystemWideReplacement("com.lge.internal", "drawable", "switch_track_holo_light", modRes.fwd(R.drawable.replacement_switch));
		}
	}

	@Override
	public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {
		if (ENABLE_SQUARE_BUBBLE) {
			String packageName = "com.android.mms";

			if (!resparam.packageName.equals(packageName))
				return;

			XModuleResources modRes = XModuleResources.createInstance(MODULE_PATH, resparam.res);

			resparam.res.setReplacement(packageName, "drawable", "bubble_inbox_bg_04", modRes.fwd(R.drawable.replacement_bubble_in_01));
			resparam.res.setReplacement(packageName, "drawable", "bubble_outbox_bg_04", modRes.fwd(R.drawable.replacement_bubble_out_01));
			resparam.res.setReplacement(packageName, "drawable", "bubble_reserved_bg_04", modRes.fwd(R.drawable.replacement_bubble_out_01));
			resparam.res.setReplacement(packageName, "drawable", "message_set_bubble_04", modRes.fwd(R.drawable.message_set_bubble_04));
		}
	}

}
