package com.gmail.alexellingsen.g2skintweaks;

import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.graphics.Color;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;
import de.robv.android.xposed.*;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import java.util.ArrayList;

@SuppressWarnings("UnusedDeclaration")
public class G2SkinTweaks implements IXposedHookZygoteInit, IXposedHookLoadPackage, IXposedHookInitPackageResources {

    private static String MODULE_PATH = null;

    private static SettingsHelper settings;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        MODULE_PATH = startupParam.modulePath;

        settings = new SettingsHelper();

        boolean enableReplaceSwitch = settings.getBoolean(Prefs.ENABLE_REPLACE_SWITCH, false);

        if (enableReplaceSwitch) {
            XModuleResources modRes = XModuleResources.createInstance(MODULE_PATH, null);

            XResources.setSystemWideReplacement("com.lge.internal", "drawable", "switch_track_holo_dark", modRes.fwd(R.drawable.replacement_switch));
            XResources.setSystemWideReplacement("com.lge.internal", "drawable", "switch_track_holo_light", modRes.fwd(R.drawable.replacement_switch));
        }
    }

    @Override
    public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {
        String packageName = "com.android.mms";

        if (resparam.packageName.equals("com.android.mms")) {
            boolean enableSquareBubble = settings.getBoolean(Prefs.ENABLE_SQUARE_BUBBLE, false);

            if (enableSquareBubble) {
                final XModuleResources modRes = XModuleResources.createInstance(MODULE_PATH, resparam.res);

                resparam.res.setReplacement(packageName, "drawable", "message_set_bubble_04", modRes.fwd(R.drawable.message_set_bubble_04));
                resparam.res.setReplacement(packageName, "drawable", "bubble_inbox_bg_04", modRes.fwd(R.drawable.balloon_bg_04_left_normal));
                resparam.res.setReplacement(packageName, "drawable", "bubble_outbox_bg_04", modRes.fwd(R.drawable.balloon_bg_04_right_normal));
                resparam.res.setReplacement(packageName, "drawable", "bubble_reserved_bg_04", modRes.fwd(R.drawable.balloon_bg_04_right_normal));
            }
        }
    }

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals("com.android.mms")) {
            setMinFontSize(lpparam);
            hookMessageListItem(lpparam);
            hookMessagingNotification(lpparam);
        }
    }

    private void hookMessagingNotification(final LoadPackageParam lpparam) {
        final Class<?> finalClass;

        try {
            finalClass = XposedHelpers.findClass(
                    "com.android.mms.transaction.MessagingNotification",
                    lpparam.classLoader);
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
                }
        );
    }

    private void hookMessageListItem(final LoadPackageParam lpparam) {
        final Class<?> finalClass;

        try {
            finalClass = XposedHelpers.findClass(
                    "com.android.mms.ui.MessageListItem",
                    lpparam.classLoader);
        } catch (ClassNotFoundError e) {
            XposedBridge.log(e);

            return;
        }

        int fails = 0;
        // Store exceptions, and only print them if both hooks fail
        ArrayList<Throwable> exceptions = new ArrayList<Throwable>();

        try {
            hookMessageListItemOther(finalClass);
            return; // No need to continue
        } catch (Throwable e) {
            fails++;
            exceptions.add(e);
        }

        try {
            hookMessageListItemSprint(lpparam, finalClass);
            return; // No need to continue
        } catch (Throwable e) {
            fails++;
            exceptions.add(e);
        }

        if (fails == 2) { // Both failed
            for (Throwable e : exceptions) {
                XposedBridge.log(e);
            }

            XposedBridge.log("G2 Skin Tweaks couldn't find a proper method to hook.");
            XposedBridge.log("Please let the developer know your device model if you want to help.");
        }
    }

    private void hookMessageListItemOther(Class<?> finalClass) throws Throwable {
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

                            boolean isIncomingMessage = isIncomingMessage(param);
                            boolean enableSmsTextColor = settings.getBoolean(Prefs.ENABLE_SMS_TEXT_COLOR, false);
                            boolean enableSquareBubble = settings.getBoolean(Prefs.ENABLE_SQUARE_BUBBLE, false);

                            if (enableSquareBubble) {
                                View parent = (View) ((TextView) XposedHelpers.getObjectField(param.thisObject, "mBodyTextView")).getParent();

                                while (parent != null) {
                                    if (parent.getBackground() != null) {
                                        Drawable d = parent.getBackground();

                                        int color = settings.getInt(isIncomingMessage ?
                                                Prefs.SQUARE_COLOR_LEFT :
                                                Prefs.SQUARE_COLOR_RIGHT, Color.WHITE);

                                        d.setColorFilter(new PorterDuffColorFilter(color, android.graphics.PorterDuff.Mode.MULTIPLY));
                                    }

                                    parent = (View) parent.getParent();
                                }
                            }

                            if (enableSmsTextColor) {
                                int color = settings.getInt(isIncomingMessage ?
                                        Prefs.SMS_TEXT_COLOR_LEFT :
                                        Prefs.SMS_TEXT_COLOR_RIGHT, Color.BLACK);

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
                                new Class<?>[]{messageItem.getClass()},
                                messageItem);

                        return (Boolean) returnVal;
                    }
                }
        );

        XposedHelpers.findAndHookMethod(
                finalClass,
                "resizeFonts",
                "boolean",

                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        TextView tvBody = (TextView) XposedHelpers.getObjectField(param.thisObject, "mBodyTextView");
                        TextView tvDate = (TextView) XposedHelpers.getObjectField(param.thisObject, "mSmallTextView");

                        tvDate.setTextSize(TypedValue.COMPLEX_UNIT_PX, tvBody.getTextSize());
                    }
                }
        );
    }

    private void hookMessageListItemSprint(final LoadPackageParam lpparam, Class<?> finalClass) throws Throwable {
        XposedHelpers.findAndHookMethod(
                finalClass,
                "bind",
                "com.android.mms.ui.MessageListAdapter$AvatarCache",
                "com.android.mms.ui.MessageItem",
                "android.widget.ListView",
                "int",
                "boolean",
                "boolean",
                "java.util.ArrayList",

                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            TextView tvBody = (TextView) XposedHelpers.getObjectField(param.thisObject, "mBodyTextView");
                            TextView tvDate = (TextView) XposedHelpers.getObjectField(param.thisObject, "mBodySubTextView");

                            boolean isIncomingMessage = isIncomingMessage(param);
                            boolean enableSmsTextColor = settings.getBoolean(Prefs.ENABLE_SMS_TEXT_COLOR, false);
                            boolean enableSquareBubble = settings.getBoolean(Prefs.ENABLE_SQUARE_BUBBLE, false);

                            if (enableSquareBubble) {
                                View parent = (View) ((TextView) XposedHelpers.getObjectField(param.thisObject, "mBodyTextView")).getParent();

                                while (parent != null) {
                                    if (parent.getBackground() != null) {
                                        Drawable d = parent.getBackground();

                                        int color = settings.getInt(isIncomingMessage ?
                                                Prefs.SQUARE_COLOR_LEFT :
                                                Prefs.SQUARE_COLOR_RIGHT, Color.WHITE);

                                        d.setColorFilter(new PorterDuffColorFilter(color, android.graphics.PorterDuff.Mode.MULTIPLY));
                                    }

                                    parent = (View) parent.getParent();
                                }
                            }

                            if (enableSmsTextColor) {
                                int color = settings.getInt(isIncomingMessage ?
                                        Prefs.SMS_TEXT_COLOR_LEFT :
                                        Prefs.SMS_TEXT_COLOR_RIGHT, Color.BLACK);

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
                                new Class<?>[]{messageItem.getClass()},
                                messageItem);

                        return (Boolean) returnVal;
                    }
                }
        );

        XposedHelpers.findAndHookMethod(
                finalClass,
                "resizeFonts",

                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        TextView tvBody = (TextView) XposedHelpers.getObjectField(param.thisObject, "mBodyTextView");
                        TextView tvDate = (TextView) XposedHelpers.getObjectField(param.thisObject, "mBodySubTextView");

                        tvDate.setTextSize(TypedValue.COMPLEX_UNIT_PX, tvBody.getTextSize());
                    }
                }
        );
    }

    private void setMinFontSize(LoadPackageParam lpparam) {
        int fails = 0;
        ArrayList<Throwable> errors = new ArrayList<Throwable>();

        try {
            setMinFontSizeOther(lpparam);
            return;
        } catch (Throwable e) {
            fails++;
            errors.add(e);
        }

        try {
            setMinFontSizeSprint(lpparam);
            return;
        } catch (Throwable e) {
            fails++;
            errors.add(e);
        }

        if (fails == 2) { // Both failed
            for (Throwable e : errors) {
                XposedBridge.log(e);
            }

            XposedBridge.log("G2 Skin Tweaks couldn't find a proper method to hook.");
            XposedBridge.log("Please let the developer know your device model if you want to help.");
        }
    }

    private void setMinFontSizeOther(final LoadPackageParam lpparam) throws Throwable {
        final Class<?> finalClass;
        try {
            finalClass = XposedHelpers.findClass(
                    "com.android.mms.pinchApi.PinchDetector",
                    lpparam.classLoader);
        } catch (ClassNotFoundError e) {
            XposedBridge.log(e);
            throw e;
        }

        XposedHelpers.findAndHookMethod(
                finalClass,
                "processActionMove",
                "android.view.MotionEvent",

                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        int min = XposedHelpers.getIntField(param.thisObject, "MIN_ZOOM");

                        if (settings.getBoolean(Prefs.ENABLE_SMALLER_SMS_SIZE, false)) {
                            if (min != 30) {
                                XposedHelpers.setIntField(param.thisObject, "MIN_ZOOM", 30);
                            }
                        } else {
                            if (min != 85) {
                                XposedHelpers.setIntField(param.thisObject, "MIN_ZOOM", 85);
                            }
                        }
                    }
                }
        );
    }

    private void setMinFontSizeSprint(final LoadPackageParam lpparam) throws Throwable {
        final Class<?> finalClass;
        try {
            finalClass = XposedHelpers.findClass(
                    "com.lge.mms.pinchApi.PinchDetector",
                    lpparam.classLoader);
        } catch (ClassNotFoundError e) {
            XposedBridge.log(e);
            throw e;
        }

        XposedHelpers.findAndHookMethod(
                finalClass,
                "processTouchEvent",
                "android.view.MotionEvent",

                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        int min = XposedHelpers.getIntField(param.thisObject, "MIN_ZOOM");

                        if (settings.getBoolean(Prefs.ENABLE_SMALLER_SMS_SIZE, false)) {
                            if (min != 30) {
                                XposedHelpers.setIntField(param.thisObject, "MIN_ZOOM", 30);
                            }
                        } else {
                            if (min != 85) {
                                XposedHelpers.setIntField(param.thisObject, "MIN_ZOOM", 85);
                            }
                        }
                    }
                }
        );
    }

    @SuppressWarnings("unused")
    private void log(String text) {
        boolean debug = false;

        if (settings != null) {
            debug = settings.getBoolean(Prefs.ENABLE_DEBUGGING, false);
        }

        if (debug) {
            XposedBridge.log(String.format("G2SkinTweaks: %s", text));
        }
    }
}