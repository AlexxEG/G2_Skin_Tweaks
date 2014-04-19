package com.gmail.alexellingsen.g2skintweaks;

import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.graphics.Color;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.view.View;
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

	private static SettingsHelper settings;

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		MODULE_PATH = startupParam.modulePath;

		settings = new SettingsHelper();

		boolean enableReplaceSwitch = settings.getBoolean(Prefs.ENABLE_REPLACE_SWICTH, false);

		if (enableReplaceSwitch) {
			XModuleResources modRes = XModuleResources.createInstance(MODULE_PATH, null);
			XResources.setSystemWideReplacement("com.lge.internal", "drawable", "switch_track_holo_dark", modRes.fwd(R.drawable.replacement_switch));
			XResources.setSystemWideReplacement("com.lge.internal", "drawable", "switch_track_holo_light", modRes.fwd(R.drawable.replacement_switch));
		}
	}

	@Override
	public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {
		boolean enableSquareBubble = settings.getBoolean(Prefs.ENABLE_SQUARE_BUBBLE, false);

		if (enableSquareBubble) {
			String packageName = "com.android.mms";

			if (!resparam.packageName.equals(packageName))
				return;

			final XModuleResources modRes = XModuleResources.createInstance(MODULE_PATH, resparam.res);

			resparam.res.setReplacement(packageName, "drawable", "message_set_bubble_04", modRes.fwd(R.drawable.message_set_bubble_04));
			resparam.res.setReplacement(packageName, "drawable", "bubble_inbox_bg_04", modRes.fwd(R.drawable.balloon_bg_04_left_normal));
			resparam.res.setReplacement(packageName, "drawable", "bubble_outbox_bg_04", modRes.fwd(R.drawable.balloon_bg_04_right_normal));
			resparam.res.setReplacement(packageName, "drawable", "bubble_reserved_bg_04", modRes.fwd(R.drawable.balloon_bg_04_right_normal));
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
							TextView tvBody = (TextView) XposedHelpers.getObjectField(param.thisObject, "mBodyTextView");
							TextView tvDate = (TextView) XposedHelpers.getObjectField(param.thisObject, "mSmallTextView");

							boolean enableSmsFontSize = settings.getBoolean(Prefs.ENABLE_SMS_TEXT_COLOR, false);
							boolean enableSmsTextColor = settings.getBoolean(Prefs.ENABLE_SMS_TEXT_COLOR, false);
							boolean enableSquareBubble = settings.getBoolean(Prefs.ENABLE_SQUARE_BUBBLE, false);

							if (enableSquareBubble) {
								boolean isIncomingMessage = isIncomingMessage(param);
								View parent = (View) ((TextView) XposedHelpers.getObjectField(param.thisObject, "mBodyTextView")).getParent();

								while (parent != null) {
									if (parent.getBackground() != null) {
										Drawable d = parent.getBackground();

										int color = isIncomingMessage ?
												settings.getInt(Prefs.SQUARE_COLOR_LEFT, Color.WHITE) :
												settings.getInt(Prefs.SQUARE_COLOR_RIGHT, Color.WHITE);

										d.setColorFilter(new PorterDuffColorFilter(color, android.graphics.PorterDuff.Mode.MULTIPLY));
									}

									parent = (View) parent.getParent();
								}
							}

							if (enableSmsFontSize) {
								int body = settings.getInt(Prefs.SMS_BODY_SIZE, 18);
								int date = settings.getInt(Prefs.SMS_DATE_SIZE, 18);

								tvBody.setTextSize(body);
								tvDate.setTextSize(date);
							}

							if (enableSmsTextColor) {
								int color = settings.getInt(Prefs.SMS_TEXT_COLOR, Color.BLACK);

								tvBody.setTextColor(color);
								tvDate.setTextColor(color);
							}
						} catch (Exception e) {
							XposedBridge.log(e);
						}
					}

					private boolean isIncomingMessage(MethodHookParam param) {
						Object messageItem = XposedHelpers.getObjectField(param.thisObject, "mMessageItem");

						Object returnVal = XposedHelpers.callMethod(
								param.thisObject,
								"isLeftItem",
								new Class<?>[] { messageItem.getClass() },
								messageItem);

						return (Boolean) returnVal;
					}
				});
	}
}