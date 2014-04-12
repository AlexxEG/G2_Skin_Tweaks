package com.gmail.alexellingsen.g2skintweaks;

import java.lang.reflect.Field;

import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.graphics.Color;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.TextView;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class G2SkinTweaks implements IXposedHookZygoteInit, IXposedHookLoadPackage, IXposedHookInitPackageResources {

	private static String	MODULE_PATH				= null;
	private static boolean	ENABLE_SMS_FONT_SIZE	= false;
	private static boolean	ENABLE_REPLACE_SWITCH	= false;
	private static boolean	ENABLE_SQUARE_BUBBLE	= false;
	private static boolean	ENABLE_SMS_TEXT_COLOR	= false;
	private static int		SMS_BODY_SIZE			= 12;
	private static int		SMS_DATE_SIZE			= 12;
	private static int		SMS_TEXT_COLOR			= Color.BLACK;
	private static int		SQUARE_COLOR_LEFT		= Color.WHITE;
	private static int		SQUARE_COLOR_RIGHT		= Color.WHITE;

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		MODULE_PATH = startupParam.modulePath;

		XSharedPreferences settings = new XSharedPreferences("com.gmail.alexellingsen.g2skintweaks", MainActivity.PREF_NAME);

		ENABLE_SMS_FONT_SIZE = settings.getBoolean(MainActivity.PREF_ENABLE_MESSENGER_FONT_SIZE, false);
		ENABLE_REPLACE_SWITCH = settings.getBoolean(MainActivity.PREF_ENABLE_REPLACE_SWICTH, ENABLE_REPLACE_SWITCH);
		ENABLE_SQUARE_BUBBLE = settings.getBoolean(MainActivity.PREF_ENABLE_SQUARE_BUBBLE, ENABLE_SQUARE_BUBBLE);
		ENABLE_SMS_TEXT_COLOR = settings.getBoolean(MainActivity.PREF_ENABLE_SMS_TEXT_COLOR, false);
		SMS_BODY_SIZE = settings.getInt(MainActivity.PREF_SMS_BODY_SIZE, SMS_BODY_SIZE);
		SMS_DATE_SIZE = settings.getInt(MainActivity.PREF_SMS_DATE_SIZE, SMS_DATE_SIZE);
		SMS_TEXT_COLOR = settings.getInt(MainActivity.PREF_SMS_TEXT_COLOR, Color.BLACK);
		SQUARE_COLOR_LEFT = settings.getInt(MainActivity.PREF_SQUARE_COLOR_LEFT, Color.WHITE);
		SQUARE_COLOR_RIGHT = settings.getInt(MainActivity.PREF_SQUARE_COLOR_RIGHT, Color.WHITE);

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

			final XModuleResources modRes = XModuleResources.createInstance(MODULE_PATH, resparam.res);

			resparam.res.setReplacement(packageName, "drawable", "message_set_bubble_04", modRes.fwd(R.drawable.message_set_bubble_04));
			resparam.res.setReplacement(packageName, "drawable", "bubble_inbox_bg_04", new XResources.DrawableLoader() {
				@Override
				public Drawable newDrawable(XResources res, int id) throws Throwable {
					Drawable mDrawable = modRes.getDrawable(R.drawable.balloon_bg_04_left_normal);
					mDrawable.setColorFilter(new PorterDuffColorFilter(SQUARE_COLOR_LEFT, android.graphics.PorterDuff.Mode.MULTIPLY));
					return mDrawable;
				}
			});
			resparam.res.setReplacement(packageName, "drawable", "bubble_outbox_bg_04", new XResources.DrawableLoader() {
				@Override
				public Drawable newDrawable(XResources res, int id) throws Throwable {
					Drawable mDrawable = modRes.getDrawable(R.drawable.balloon_bg_04_right_normal);
					mDrawable.setColorFilter(new PorterDuffColorFilter(SQUARE_COLOR_RIGHT, android.graphics.PorterDuff.Mode.MULTIPLY));
					return mDrawable;
				}
			});
			resparam.res.setReplacement(packageName, "drawable", "bubble_reserved_bg_04", new XResources.DrawableLoader() {
				@Override
				public Drawable newDrawable(XResources res, int id) throws Throwable {
					Drawable mDrawable = modRes.getDrawable(R.drawable.balloon_bg_04_right_normal);
					mDrawable.setColorFilter(new PorterDuffColorFilter(SQUARE_COLOR_RIGHT, android.graphics.PorterDuff.Mode.MULTIPLY));
					return mDrawable;
				}
			});
		}
	}

	@Override
	public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
		String packageName = "com.android.mms";

		if (!lpparam.packageName.equals(packageName))
			return;

		if (!ENABLE_SMS_FONT_SIZE && !ENABLE_SMS_TEXT_COLOR)
			return;

		final Class<?> finalClass;

		try {
			Class<?> findClass = XposedHelpers.findClass("com.android.mms.ui.MessageListItem", lpparam.classLoader);

			finalClass = findClass;
		} catch (ClassNotFoundError e) {
			Log.e("myTag", "Error", e);

			return;
		}

		Log.d("myTag", "Begin hooking 'bind'");

		// mBodyTextView seems to be initialized after 'bind' method
		XposedHelpers.findAndHookMethod(
				finalClass,
				"bind",
				"com.android.mms.ui.MessageListAdapter$AvatarCache",
				"com.android.mms.ui.MessageItem",
				"android.widget.ListView",
				"int",
				"boolean",
				"boolean",

				new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						try {
							Field fieldBody = XposedHelpers.findField(finalClass, "mBodyTextView");
							Field fieldDate = XposedHelpers.findField(finalClass, "mSmallTextView");

							Log.d("myTag", "Found 'mBodyTextView', type: " + fieldBody.getType().toString());

							Object objBody = fieldBody.get(param.thisObject);
							Object objDate = fieldDate.get(param.thisObject);
							TextView tvBody = (TextView) objBody;
							TextView tvDate = (TextView) objDate;

							if (ENABLE_SMS_FONT_SIZE) {
								tvBody.setTextSize(SMS_BODY_SIZE);
								tvDate.setTextSize(SMS_DATE_SIZE);
							}

							if (ENABLE_SMS_TEXT_COLOR) {
								tvBody.setTextColor(SMS_TEXT_COLOR);
								tvDate.setTextColor(SMS_TEXT_COLOR);
							}
						} catch (NoSuchFieldError e) {
							Log.e("myTag", "'mBodyTextView' not found.");
						} catch (IllegalArgumentException e) {
							Log.e("myTag", "Can't get value of 'mBodyTextView'.");
						} catch (Exception e) {
							Log.e("myTag", "Error", e);
						}
					}
				});

		Log.d("myTag", "Hooked 'bind'");
	}
}
