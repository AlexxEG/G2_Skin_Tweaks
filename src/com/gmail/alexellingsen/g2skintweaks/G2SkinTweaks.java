package com.gmail.alexellingsen.g2skintweaks;

import java.lang.reflect.Field;

import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.graphics.Color;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.widget.TextView;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class G2SkinTweaks implements IXposedHookZygoteInit, IXposedHookLoadPackage, IXposedHookInitPackageResources {

	private static String				MODULE_PATH				= null;
	private static boolean				ENABLE_REPLACE_SWITCH	= false;
	private static boolean				ENABLE_SQUARE_BUBBLE	= false;
	private static int					SQUARE_COLOR_LEFT		= Color.WHITE;
	private static int					SQUARE_COLOR_RIGHT		= Color.WHITE;

	private static XSharedPreferences	settings;

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		MODULE_PATH = startupParam.modulePath;

		initializeSettings();

		ENABLE_REPLACE_SWITCH = settings.getBoolean(MainActivity.PREF_ENABLE_REPLACE_SWICTH, ENABLE_REPLACE_SWITCH);
		ENABLE_SQUARE_BUBBLE = settings.getBoolean(MainActivity.PREF_ENABLE_SQUARE_BUBBLE, ENABLE_SQUARE_BUBBLE);
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

		hookMsgText(lpparam);
		hookTurnOnBacklight(lpparam);
	}

	private void hookTurnOnBacklight(final LoadPackageParam lpparam) {
		final Class<?> finalClass;

		try {
			Class<?> findClass = XposedHelpers.findClass(
					"com.android.mms.transaction.MessagingNotification",
					lpparam.classLoader);

			finalClass = findClass;
		} catch (ClassNotFoundError e) {
			XposedBridge.log(e);

			return;
		}

		XposedBridge.log("Begin hooking 'turnOnBacklight'");

		XposedHelpers.findAndHookMethod(
				finalClass,
				"turnOnBacklight",
				"android.content.Context",

				new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						if (!getTurnOnScreenOnNewSms()) {
							XposedBridge.log("Don't run 'turnOnBacklight'");

							param.setResult(null);

							// Flash rear power led for 1 second.
							RootFunctions.flashRearPowerLed(1000);
						} else {
							XposedBridge.log("Run 'turnOnBacklight'");
						}
					}

					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						XposedBridge.log("'turnOnBacklight' ran...");
					}
				});

		XposedBridge.log("Hooked 'turnOnBacklight'");
	}

	private void hookMsgText(final LoadPackageParam lpparam) {
		final Class<?> finalClass;

		try {
			Class<?> findClass = XposedHelpers.findClass("com.android.mms.ui.MessageListItem", lpparam.classLoader);

			finalClass = findClass;
		} catch (ClassNotFoundError e) {
			XposedBridge.log(e);

			return;
		}

		XposedBridge.log("Begin hooking 'bind'");

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
							XposedBridge.log("Finding TextViews!");

							Field fieldBody = XposedHelpers.findField(finalClass, "mBodyTextView");
							Field fieldDate = XposedHelpers.findField(finalClass, "mSmallTextView");
							Object objBody = fieldBody.get(param.thisObject);
							Object objDate = fieldDate.get(param.thisObject);
							TextView tvBody = (TextView) objBody;
							TextView tvDate = (TextView) objDate;

							if (getEnableSmsFontSize()) {
								tvBody.setTextSize(getSmsBodySize());
								tvDate.setTextSize(getSmsDateSize());

								XposedBridge.log("Body Size: " + getSmsBodySize() + "sp");
								XposedBridge.log("Date Size: " + getSmsDateSize() + "sp");
							}

							if (getEnableSmsTextColor()) {
								tvBody.setTextColor(getSmsTextColor());
								tvDate.setTextColor(getSmsTextColor());

								XposedBridge.log("Color: " + getSmsTextColor());
							}
						} catch (NoSuchFieldError e) {
							XposedBridge.log("'mBodyTextView' not found.");
						} catch (IllegalArgumentException e) {
							XposedBridge.log("Can't get value of 'mBodyTextView'.");
						} catch (Exception e) {
							XposedBridge.log(e);
						}
					}
				});

		XposedBridge.log("Hooked 'bind'");
	}

	public void initializeSettings() {
		String packageName = "com.gmail.alexellingsen.g2skintweaks";

		if (settings == null) {
			settings = new XSharedPreferences(packageName, MainActivity.PREF_NAME);
			settings.makeWorldReadable();
		}

		settings.reload();
	}

	public boolean getEnableSmsFontSize() {
		initializeSettings();

		boolean b = settings.getBoolean(MainActivity.PREF_ENABLE_MESSENGER_FONT_SIZE, false);

		XposedBridge.log("getEnableSmsFontSize: " + b);

		return settings.getBoolean(MainActivity.PREF_ENABLE_MESSENGER_FONT_SIZE, false);
	}

	public boolean getEnableSmsTextColor() {
		initializeSettings();

		return settings.getBoolean(MainActivity.PREF_ENABLE_SMS_TEXT_COLOR, false);
	}

	public int getSmsBodySize() {
		initializeSettings();

		return settings.getInt(MainActivity.PREF_SMS_BODY_SIZE, 12);
	}

	public int getSmsDateSize() {
		initializeSettings();

		return settings.getInt(MainActivity.PREF_SMS_DATE_SIZE, 12);
	}

	public int getSmsTextColor() {
		initializeSettings();

		return settings.getInt(MainActivity.PREF_SMS_TEXT_COLOR, Color.BLACK);
	}

	public boolean getTurnOnScreenOnNewSms() {
		initializeSettings();

		return settings.getBoolean(MainActivity.PREF_TURN_ON_SCREEN_NEW_SMS, true);
	}

}