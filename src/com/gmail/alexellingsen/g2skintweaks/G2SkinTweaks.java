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
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class G2SkinTweaks implements IXposedHookZygoteInit, IXposedHookLoadPackage, IXposedHookInitPackageResources {

	private static String MODULE_PATH = null;
	private static boolean ENABLE_REPLACE_SWITCH = false;
	private static boolean ENABLE_SQUARE_BUBBLE = false;
	private static int SQUARE_COLOR_LEFT = Color.WHITE;
	private static int SQUARE_COLOR_RIGHT = Color.WHITE;

	private static SettingsHelper settings;

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		MODULE_PATH = startupParam.modulePath;

		settings = new SettingsHelper();

		ENABLE_REPLACE_SWITCH = settings.getBoolean(Prefs.ENABLE_REPLACE_SWICTH, ENABLE_REPLACE_SWITCH);
		ENABLE_SQUARE_BUBBLE = settings.getBoolean(Prefs.ENABLE_SQUARE_BUBBLE, ENABLE_SQUARE_BUBBLE);
		SQUARE_COLOR_LEFT = settings.getInt(Prefs.SQUARE_COLOR_LEFT, Color.WHITE);
		SQUARE_COLOR_RIGHT = settings.getInt(Prefs.SQUARE_COLOR_RIGHT, Color.WHITE);

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
		if (lpparam.packageName.equals("com.android.mms")) {
			hookMessageListItem(lpparam);
			hookMessagingNotification(lpparam);
		}
	}

	private void hookMessagingNotification(final LoadPackageParam lpparam) {
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

		XposedHelpers.findAndHookMethod(
				finalClass,
				"turnOnBacklight",
				"android.content.Context",

				new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						boolean turnOnScreenNewSms = settings.getBoolean(Prefs.TURN_ON_SCREEN_NEW_SMS, true);

						if (!turnOnScreenNewSms) {
							param.setResult(null);

							// To-Do: Make SuperSU detect the call from my app,
							// not the Messaging app
							RootFunctions.flashRearPowerLed(1000);
						}
					}
				});
	}

	private void hookMessageListItem(final LoadPackageParam lpparam) {
		final Class<?> finalClass;

		try {
			Class<?> findClass = XposedHelpers.findClass("com.android.mms.ui.MessageListItem", lpparam.classLoader);

			finalClass = findClass;
		} catch (ClassNotFoundError e) {
			XposedBridge.log(e);

			return;
		}

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
							Object objBody = fieldBody.get(param.thisObject);
							Object objDate = fieldDate.get(param.thisObject);
							TextView tvBody = (TextView) objBody;
							TextView tvDate = (TextView) objDate;

							boolean enableSmsFontSize = settings.getBoolean(Prefs.ENABLE_SMS_TEXT_COLOR, false);

							if (enableSmsFontSize) {
								int body = settings.getInt(Prefs.SMS_BODY_SIZE, 18);
								int date = settings.getInt(Prefs.SMS_DATE_SIZE, 18);

								tvBody.setTextSize(body);
								tvDate.setTextSize(date);
							}

							boolean enableSmsTextColor = settings.getBoolean(Prefs.ENABLE_SMS_TEXT_COLOR, false);

							if (enableSmsTextColor) {
								int color = settings.getInt(Prefs.SMS_TEXT_COLOR, Color.BLACK);

								tvBody.setTextColor(color);
								tvDate.setTextColor(color);
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
	}

}