package com.gmail.alexellingsen.g2skintweaks;

import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;
import com.gmail.alexellingsen.g2skintweaks.hooks.*;
import com.gmail.alexellingsen.g2skintweaks.utils.Devices;
import com.gmail.alexellingsen.g2skintweaks.utils.SettingsHelper;
import de.robv.android.xposed.*;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import java.util.ArrayList;

@SuppressWarnings("UnusedDeclaration")
public class G2SkinTweaks implements IXposedHookZygoteInit, IXposedHookLoadPackage, IXposedHookInitPackageResources {

    private static final int DEFAULT_MINIMUM_ZOOM = 85;

    private static String MODULE_PATH = null;
    private static SettingsHelper mSettings;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        MODULE_PATH = startupParam.modulePath;
        mSettings = new SettingsHelper();

        boolean enableReplaceSwitch = mSettings.getBoolean(Prefs.ENABLE_REPLACE_SWITCH, false);

        if (enableReplaceSwitch) {
            String packageName = "com.lge.internal";
            XModuleResources modRes = XModuleResources.createInstance(MODULE_PATH, null);

            XResources.setSystemWideReplacement(packageName, "drawable", "switch_track_holo_dark", modRes.fwd(R.drawable.replacement_switch));
            XResources.setSystemWideReplacement(packageName, "drawable", "switch_track_holo_light", modRes.fwd(R.drawable.replacement_switch));
        }

        LGHomeHook.init(mSettings);
        LGLockScreenHook.init(mSettings);
        LGMessageHook.init(mSettings);
        LGMessageBubblesHook.init(mSettings);
        LGSettings.init(mSettings);
        RecentAppsHook.init(mSettings);
        StatusBarHook.init(mSettings);
    }

    @Override
    public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {
        XModuleResources modRes = XModuleResources.createInstance(MODULE_PATH, resparam.res);

        LGHomeHook.handleInitPackageResources(resparam);
        LGLockScreenHook.handleInitPackageResources(resparam, modRes);
        LGMessageBubblesHook.handleInitPackageResources(resparam, modRes);
        LGMessageHook.handleInitPackageResources(resparam);
        LGSettings.handleInitPackageResources(resparam);
        RecentAppsHook.handleInitPackageResources(resparam);
        StatusBarHook.handleInitPackageResources(resparam, modRes);
    }

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        LGHomeHook.handleLoadPackage(lpparam);
        LGLockScreenHook.handleLoadPackage(lpparam);
        LGMessageHook.handleLoadPackage(lpparam);
        RecentAppsHook.handleLoadPackage(lpparam);
        StatusBarHook.handleLoadPackage(lpparam);

        if (Devices.isAnyDevice(Devices.SPRINT, Devices.VERIZON)) {
            hookPaintSetColorSprint(lpparam);
        }

        if (lpparam.packageName.equals("com.android.mms")) {
            setMinFontSize(lpparam);
            hookConversationListItem(lpparam);
            hookMessageListItem(lpparam);
            hookMessagingNotification(lpparam);
        }
    }

    private void hookConversationListItem(final LoadPackageParam lpparam) {
        switch (Devices.getDevice()) {
            case SPRINT:
            case VERIZON:
                hookConversationListItemSprint(lpparam);
                break;
            case OTHER:
                hookConversationListItemOther(lpparam);
                break;
        }
    }

    private void hookConversationListItemOther(final LoadPackageParam lpparam) {
        final Class<?> rootClass;
        final Class<?> subClass;

        try {
            rootClass = XposedHelpers.findClass(
                    "com.android.mms.ui.ConversationListItem",
                    lpparam.classLoader);

            subClass = XposedHelpers.findClass(
                    "com.android.mms.ui.ConversationListItem$ConversationListItemRight",
                    lpparam.classLoader);
        } catch (ClassNotFoundError e) {
            XposedBridge.log(e);
            return;
        }

        XposedHelpers.findAndHookMethod(
                subClass,
                "onDrawBottomline",
                "android.graphics.Canvas",
                "int",
                "boolean",

                new XC_MethodHook() {
                    int originalFontColor = -1;
                    int originalFontSmall = -1;

                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (mSettings.getBoolean(Prefs.ENABLE_SMALLER_TEXT_MESSAGES, false)) {
                            originalFontSmall = XposedHelpers.getStaticIntField(rootClass, "mFontSmall");

                            int size = XposedHelpers.getStaticIntField(rootClass, "mFontSmallScaled");

                            XposedHelpers.setStaticIntField(rootClass, "mFontSmall", size);
                        }

                        if (mSettings.getBoolean(Prefs.ENABLE_CONVERSATION_TEXT_COLOR, false)) {
                            Object conversationListItem = XposedHelpers.getObjectField(param.thisObject, "this$0");
                            TextPaint tp = (TextPaint) XposedHelpers.getObjectField(conversationListItem, "tp");

                            originalFontColor = tp.getColor();

                            tp.setColor(mSettings.getInt(Prefs.CONVERSATION_COLOR_BOTTOM, Color.BLACK));
                        }
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (mSettings.getBoolean(Prefs.ENABLE_SMALLER_TEXT_MESSAGES, false)) {
                            if (originalFontSmall != -1) {
                                XposedHelpers.setStaticIntField(rootClass, "mFontSmall", originalFontSmall);

                                originalFontSmall = -1;
                            }
                        }

                        if (mSettings.getBoolean(Prefs.ENABLE_CONVERSATION_TEXT_COLOR, false)) {
                            if (originalFontColor != 1) {
                                Object conversationListItem = XposedHelpers.getObjectField(param.thisObject, "this$0");
                                TextPaint tp = (TextPaint) XposedHelpers.getObjectField(conversationListItem, "tp");

                                tp.setColor(originalFontColor);

                                originalFontColor = -1;
                            }
                        }
                    }
                }
        );

        XposedHelpers.findAndHookMethod(
                subClass,
                "onDrawFailedIcon",
                "android.graphics.Canvas",
                "int",
                "boolean",
                "int",
                "android.graphics.drawable.Drawable",
                "int",

                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (mSettings.getBoolean(Prefs.ENABLE_CONVERSATION_TEXT_COLOR, false)) {
                            // When this method is called, it means it's safe to set the TextPaint color,
                            // and it should reset itself in the 'setTextPaintPropertyByTheme' method.
                            Object conversationListItem = XposedHelpers.getObjectField(param.thisObject, "this$0");
                            TextPaint tp = (TextPaint) XposedHelpers.getObjectField(conversationListItem, "tp");

                            tp.setColor(mSettings.getInt(Prefs.CONVERSATION_COLOR_TOP, Color.BLACK));
                        }
                    }
                }
        );
    }

    private void hookConversationListItemSprint(final LoadPackageParam lpparam) {
        final Class<?> rootClass;
        final Class<?> subClass;

        try {
            rootClass = XposedHelpers.findClass(
                    "com.android.mms.ui.ConversationListItem",
                    lpparam.classLoader);

            subClass = XposedHelpers.findClass(
                    "com.android.mms.ui.ConversationListItem$ConversationListItemRight",
                    lpparam.classLoader);
        } catch (ClassNotFoundError e) {
            XposedBridge.log(e);
            return;
        }

        XposedHelpers.findAndHookMethod(
                subClass,
                "onDraw",
                "android.graphics.Canvas",

                new XC_MethodHook() {
                    int originalFontSmall = -1;

                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (mSettings.getBoolean(Prefs.ENABLE_SMALLER_TEXT_MESSAGES, false)) {
                            originalFontSmall = XposedHelpers.getStaticIntField(rootClass, "mFontSmall");

                            int size = XposedHelpers.getStaticIntField(rootClass, "mFontSmallScaled");

                            XposedHelpers.setStaticIntField(rootClass, "mFontSmall", size);
                        }
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (mSettings.getBoolean(Prefs.ENABLE_SMALLER_TEXT_MESSAGES, false)) {
                            if (originalFontSmall != -1) {
                                XposedHelpers.setStaticIntField(rootClass, "mFontSmall", originalFontSmall);

                                originalFontSmall = -1;
                            }
                        }
                    }
                }
        );
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
                        boolean dontTurnScreenOn = mSettings.getBoolean(Prefs.DONT_TURN_SCREEN_ON_SMS, false);

                        if (dontTurnScreenOn) {
                            param.setResult(null);
                        }
                    }
                }
        );
    }

    private void hookMessageListItem(final LoadPackageParam lpparam) {
        final Class<?> findClass;

        try {
            findClass = XposedHelpers.findClass(
                    "com.android.mms.ui.MessageListItem",
                    lpparam.classLoader);
        } catch (ClassNotFoundError e) {
            XposedBridge.log(e);

            return;
        }

        final String dateTextViewName;

        switch (Devices.getDevice()) {
            case SPRINT:
            case VERIZON:
                dateTextViewName = "mBodySubTextView";
                break;
            default:
                dateTextViewName = "mSmallTextView";
                break;
        }

        // Create hooks
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    TextView tvBody = (TextView) XposedHelpers.getObjectField(param.thisObject, "mBodyTextView");
                    TextView tvDate = (TextView) XposedHelpers.getObjectField(param.thisObject, dateTextViewName);

                    boolean isIncomingMessage = isIncomingMessage(param);
                    boolean enableSmsTextColor = mSettings.getBoolean(Prefs.ENABLE_SMS_TEXT_COLOR, false);
                    boolean enableCustomBubbleColor = mSettings.getBoolean(Prefs.ENABLE_CUSTOM_BUBBLE_COLOR, false);
                    boolean enableTransparency = mSettings.getBoolean(Prefs.BUBBLE_TRANSPARENCY, false);

                    if (enableCustomBubbleColor || enableTransparency) {
                        View parent = (View) tvBody.getParent();
                        ArrayList<View> parents = new ArrayList<View>();

                        while (parent != null) {
                            parents.add(parent);
                            parent = (View) parent.getParent();
                        }

                        int color = Color.WHITE;

                        if (enableCustomBubbleColor) {
                            if (isIncomingMessage) {
                                color = mSettings.getInt(Prefs.BUBBLE_COLOR_LEFT, Color.WHITE);
                            } else {
                                color = mSettings.getInt(Prefs.BUBBLE_COLOR_RIGHT, Color.WHITE);
                            }
                        }

                        // Looking for the second parent, index 1
                        Drawable bd = parents.get(1).getBackground();

                        if (enableTransparency) {
                            bd.setAlpha(mSettings.getInt(Prefs.BUBBLE_TRANSPARENCY_VALUE, 255));
                        } else {
                            color = Color.argb(
                                    Color.alpha(bd.getOpacity()),
                                    Color.red(color),
                                    Color.green(color),
                                    Color.blue(color)
                            );
                        }

                        bd.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY));
                    }

                    if (enableSmsTextColor) {
                        int color = mSettings.getInt(isIncomingMessage ?
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
        };
        XC_MethodHook resizeHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                TextView tvBody = (TextView) XposedHelpers.getObjectField(param.thisObject, "mBodyTextView");
                TextView tvDate = (TextView) XposedHelpers.getObjectField(param.thisObject, dateTextViewName);

                int offset = mSettings.getInt(Prefs.DATE_SIZE_OFFSET_MESSAGES, 0);

                tvDate.setTextSize(TypedValue.COMPLEX_UNIT_PX, tvBody.getTextSize() - offset);
            }
        };

        switch (Devices.getDevice()) {
            case SPRINT:
            case VERIZON:
                hookMessageListItemSprint(findClass, hook, resizeHook);
                break;
            case OTHER:
                hookMessageListItemOther(findClass, hook, resizeHook);
                break;
        }
    }

    private void hookMessageListItemOther(Class<?> finalClass, XC_MethodHook hook, XC_MethodHook resizeHook) {
        XposedHelpers.findAndHookMethod(
                finalClass,
                "bind",
                "com.android.mms.ui.MessageListAdapter$AvatarCache",
                "com.android.mms.ui.MessageItem",
                "android.widget.ListView",
                "int",
                "boolean",
                "boolean",

                hook
        );

        XposedHelpers.findAndHookMethod(
                finalClass,
                "resizeFonts",
                "boolean",

                resizeHook
        );
    }

    private void hookMessageListItemSprint(Class<?> finalClass, XC_MethodHook hook, XC_MethodHook resizeHook) {
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

                hook
        );

        XposedHelpers.findAndHookMethod(
                finalClass,
                "resizeFonts",

                resizeHook
        );
    }

    private void hookPaintSetColorSprint(final LoadPackageParam lpparam) {
        final Class<?> findClass;

        try {
            findClass = XposedHelpers.findClass(
                    "android.graphics.Paint",
                    lpparam.classLoader);
        } catch (ClassNotFoundError e) {
            XposedBridge.log(e);
            return;
        }

        /*Has to hook this method since code can't be injected in middle of a method.
          Sprint's Messenger app draws top & bottom line in same method, unlike international version,
          therefore normally only allowing one color.*/

        XposedHelpers.findAndHookMethod(
                findClass,
                "setColor",
                "int",

                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!mSettings.getBoolean(Prefs.ENABLE_CONVERSATION_TEXT_COLOR, false)) {
                            return;
                        }

                        StackTraceElement[] elements = Thread.currentThread().getStackTrace();

                        for (StackTraceElement element : elements) {
                            if (!element.getClassName().contains("Conversation")) {
                                continue;
                            }

                            if (element.getClassName().equals("com.android.mms.ui.ConversationListItem$ConversationListItemRight")
                                    && element.getMethodName().equals("onDraw")) {
                                switch (element.getLineNumber()) {
                                    case 789: // Top line
                                        param.args[0] = mSettings.getInt(Prefs.CONVERSATION_COLOR_TOP, Color.BLACK);
                                        break;
                                    case 853: // Bottom line
                                        param.args[0] = mSettings.getInt(Prefs.CONVERSATION_COLOR_BOTTOM, Color.BLACK);
                                        break;
                                    // These are not used atm.
                                    /*case 718:
                                        param.args[0] = Color.RED;
                                        break;*/
                                    /*case 939: //
                                        param.args[0] = Color.BLUE;
                                        break;*/
                                }
                            }
                        }
                    }
                }
        );
    }

    private void setMinFontSize(LoadPackageParam lpparam) {
        switch (Devices.getDevice()) {
            case SPRINT:
            case VERIZON:
                setMinFontSizeSprint(lpparam);
                break;
            case OTHER:
                setMinFontSizeOther(lpparam);
                break;
        }
    }

    private void setMinFontSizeOther(final LoadPackageParam lpparam) {
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

                        log("'processActionMove' ran");

                        if (mSettings.getBoolean(Prefs.ENABLE_SMALLER_TEXT_MESSAGES, false)) {
                            int minimumZoom = mSettings.getInt(Prefs.MINIMUM_ZOOM_LEVEL_MESSAGES, 30);

                            if (min != minimumZoom) {
                                XposedHelpers.setIntField(param.thisObject, "MIN_ZOOM", minimumZoom);
                            }
                        } else {
                            if (min != DEFAULT_MINIMUM_ZOOM) {
                                XposedHelpers.setIntField(param.thisObject, "MIN_ZOOM", DEFAULT_MINIMUM_ZOOM);
                            }
                        }
                    }
                }
        );
    }

    private void setMinFontSizeSprint(final LoadPackageParam lpparam) {
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

                        log("'processTouchEvent' ran");

                        if (mSettings.getBoolean(Prefs.ENABLE_SMALLER_TEXT_MESSAGES, false)) {
                            int minimumZoom = mSettings.getInt(Prefs.MINIMUM_ZOOM_LEVEL_MESSAGES, 30);

                            if (min != minimumZoom) {
                                XposedHelpers.setIntField(param.thisObject, "MIN_ZOOM", minimumZoom);
                            }
                        } else {
                            if (min != DEFAULT_MINIMUM_ZOOM) {
                                XposedHelpers.setIntField(param.thisObject, "MIN_ZOOM", DEFAULT_MINIMUM_ZOOM);
                            }
                        }
                    }
                }
        );
    }

    private void log(String text) {
        boolean debug = false;

        if (mSettings != null) {
            debug = mSettings.getBoolean(Prefs.ENABLE_DEBUGGING, false);
        }

        if (debug) {
            XposedBridge.log(String.format("G2SkinTweaks: %s", text));
        }
    }
}