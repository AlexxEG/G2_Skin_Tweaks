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
import android.widget.ImageView;
import android.widget.TextView;
import com.gmail.alexellingsen.g2skintweaks.hooks.*;
import com.gmail.alexellingsen.g2skintweaks.utils.Devices;
import com.gmail.alexellingsen.g2skintweaks.utils.SettingsHelper;
import de.robv.android.xposed.*;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import java.util.ArrayList;

public class G2SkinTweaks implements IXposedHookZygoteInit, IXposedHookLoadPackage, IXposedHookInitPackageResources {

    private static final int DEFAULT_MINIMUM_ZOOM = 85;

    private static String MODULE_PATH = null;
    private static SettingsHelper settings;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        MODULE_PATH = startupParam.modulePath;

        settings = new SettingsHelper();

        boolean enableReplaceSwitch = settings.getBoolean(Prefs.ENABLE_REPLACE_SWITCH, false);

        if (enableReplaceSwitch) {
            String packageName = "com.lge.internal";
            XModuleResources modRes = XModuleResources.createInstance(MODULE_PATH, null);

            XResources.setSystemWideReplacement(packageName, "drawable", "switch_track_holo_dark", modRes.fwd(R.drawable.replacement_switch));
            XResources.setSystemWideReplacement(packageName, "drawable", "switch_track_holo_light", modRes.fwd(R.drawable.replacement_switch));
        }

        LGHomeHook.init(settings);
        LGLockScreenHook.init(settings);
        LGMessageHook.init(settings);
        LGMessageBubblesHook.init(settings);
        RecentAppsHook.init(settings);
    }

    @Override
    public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {
        XModuleResources modRes = XModuleResources.createInstance(MODULE_PATH, resparam.res);

        LGHomeHook.handleInitPackageResources(resparam);
        LGLockScreenHook.handleInitPackageResources(resparam, modRes);
        LGMessageBubblesHook.handleInitPackageResources(resparam, modRes);
        LGMessageHook.handleInitPackageResources(resparam);
        RecentAppsHook.handleInitPackageResources(resparam);

        if (resparam.packageName.equals("com.android.settings")) {
            boolean enableRemoveDividers = settings.getBoolean(Prefs.ENABLE_REMOVE_DIVIDERS, false);

            if (enableRemoveDividers) {
                removeDividers(resparam);
            }
        }

        if (resparam.packageName.equals("com.lge.settings.easy")) {
            boolean enableRemoveDividers = settings.getBoolean(Prefs.ENABLE_REMOVE_DIVIDERS, false);

            if (enableRemoveDividers) {
                removeDividersEasy(resparam);
            }
        }
    }

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        LGHomeHook.handleLoadPackage(lpparam);
        LGLockScreenHook.handleLoadPackage(lpparam);
        LGMessageHook.handleLoadPackage(lpparam);
        RecentAppsHook.handleLoadPackage(lpparam);

        if (Devices.getDevice() == Devices.SPRINT) {
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
        if (Devices.getDevice() == Devices.SPRINT) {
            hookConversationListItemSprint(lpparam);
        } else {
            hookConversationListItemOther(lpparam);
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
                        if (settings.getBoolean(Prefs.ENABLE_SMALLER_TEXT_MESSAGES, false)) {
                            originalFontSmall = XposedHelpers.getStaticIntField(rootClass, "mFontSmall");

                            int size = XposedHelpers.getStaticIntField(rootClass, "mFontSmallScaled");

                            XposedHelpers.setStaticIntField(rootClass, "mFontSmall", size);
                        }

                        if (settings.getBoolean(Prefs.ENABLE_CONVERSATION_COLOR, false)) {
                            Object conversationListItem = XposedHelpers.getObjectField(param.thisObject, "this$0");
                            TextPaint tp = (TextPaint) XposedHelpers.getObjectField(conversationListItem, "tp");

                            originalFontColor = tp.getColor();

                            tp.setColor(settings.getInt(Prefs.CONVERSATION_COLOR_BOTTOM, Color.BLACK));
                        }
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (settings.getBoolean(Prefs.ENABLE_SMALLER_TEXT_MESSAGES, false)) {
                            if (originalFontSmall != -1) {
                                XposedHelpers.setStaticIntField(rootClass, "mFontSmall", originalFontSmall);

                                originalFontSmall = -1;
                            }
                        }

                        if (settings.getBoolean(Prefs.ENABLE_CONVERSATION_COLOR, false)) {
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
                        if (settings.getBoolean(Prefs.ENABLE_CONVERSATION_COLOR, false)) {
                            // When this method is called, it means it's safe to set the TextPaint color,
                            // and it should reset itself in the 'setTextPaintPropertyByTheme' method.
                            Object conversationListItem = XposedHelpers.getObjectField(param.thisObject, "this$0");
                            TextPaint tp = (TextPaint) XposedHelpers.getObjectField(conversationListItem, "tp");

                            tp.setColor(settings.getInt(Prefs.CONVERSATION_COLOR_TOP, Color.BLACK));
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
                        if (settings.getBoolean(Prefs.ENABLE_SMALLER_TEXT_MESSAGES, false)) {
                            originalFontSmall = XposedHelpers.getStaticIntField(rootClass, "mFontSmall");

                            int size = XposedHelpers.getStaticIntField(rootClass, "mFontSmallScaled");

                            XposedHelpers.setStaticIntField(rootClass, "mFontSmall", size);
                        }
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (settings.getBoolean(Prefs.ENABLE_SMALLER_TEXT_MESSAGES, false)) {
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
                        boolean dontTurnScreenOn = settings.getBoolean(Prefs.DONT_TURN_SCREEN_ON_SMS, false);

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

        if (Devices.getDevice() == Devices.SPRINT) {
            dateTextViewName = "mBodySubTextView";
        } else {
            dateTextViewName = "mSmallTextView";
        }

        // Create hooks
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    TextView tvBody = (TextView) XposedHelpers.getObjectField(param.thisObject, "mBodyTextView");
                    TextView tvDate = (TextView) XposedHelpers.getObjectField(param.thisObject, dateTextViewName);

                    boolean isIncomingMessage = isIncomingMessage(param);
                    boolean enableSmsTextColor = settings.getBoolean(Prefs.ENABLE_SMS_TEXT_COLOR, false);
                    boolean enableCustomBubbleColor = settings.getBoolean(Prefs.ENABLE_CUSTOM_BUBBLE_COLOR, false);
                    boolean enableTransparency = settings.getBoolean(Prefs.BUBBLE_TRANSPARENCY, false);

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
                                color = settings.getInt(Prefs.BUBBLE_COLOR_LEFT, Color.WHITE);
                            } else {
                                color = settings.getInt(Prefs.BUBBLE_COLOR_RIGHT, Color.WHITE);
                            }
                        }

                        // Looking for the second parent, index 1
                        Drawable bd = parents.get(1).getBackground();

                        if (enableTransparency) {
                            bd.setAlpha(settings.getInt(Prefs.BUBBLE_TRANSPARENCY_VALUE, 255));
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
        };
        XC_MethodHook resizeHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                TextView tvBody = (TextView) XposedHelpers.getObjectField(param.thisObject, "mBodyTextView");
                TextView tvDate = (TextView) XposedHelpers.getObjectField(param.thisObject, dateTextViewName);

                int offset = settings.getInt(Prefs.DATE_SIZE_OFFSET_MESSAGES, 0);

                tvDate.setTextSize(TypedValue.COMPLEX_UNIT_PX, tvBody.getTextSize() - offset);
            }
        };

        if (Devices.getDevice() == Devices.SPRINT) {
            hookMessageListItemSprint(findClass, hook, resizeHook);
        } else {
            hookMessageListItemOther(findClass, hook, resizeHook);
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
                        if (!settings.getBoolean(Prefs.ENABLE_CONVERSATION_COLOR, false)) {
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
                                        param.args[0] = settings.getInt(Prefs.CONVERSATION_COLOR_TOP, Color.BLACK);
                                        break;
                                    case 853: // Bottom line
                                        param.args[0] = settings.getInt(Prefs.CONVERSATION_COLOR_BOTTOM, Color.BLACK);
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

    private void removeDividers(final InitPackageResourcesParam resparam) {
        try {
            resparam.res.hookLayout(
                    "com.android.settings",
                    "layout",
                    "preference_widget_switch",

                    new XC_LayoutInflated() {
                        @Override
                        public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                            ImageView divider = (ImageView) liparam.view.findViewById(
                                    liparam.res.getIdentifier("switchImage", "id", "com.android.settings"));
                            
                            divider.setVisibility(View.INVISIBLE);
                        }
                    }
            );

            resparam.res.hookLayout(
                    "com.android.settings",
                    "layout",
                    "preference_header_switch_item",

                    new XC_LayoutInflated() {
                        @Override
                        public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                            View divider = liparam.view.findViewById(
                                    liparam.res.getIdentifier("switchDivider", "id", "com.android.settings"));

                            divider.setVisibility(View.INVISIBLE);
                        }
                    }
            );
        } catch (Throwable e) {
            XposedBridge.log(e);
        }
    }

    private void removeDividersEasy(final InitPackageResourcesParam resparam) {
        try {
            resparam.res.hookLayout(
                    "com.lge.settings.easy",
                    "layout",
                    "easy_preference",

                    new XC_LayoutInflated() {
                        @Override
                        public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                            ImageView divider = (ImageView) liparam.view.findViewById(
                                    liparam.res.getIdentifier("easy_vertical_divider", "id", "com.lge.settings.easy"));
                            divider.setVisibility(View.INVISIBLE);
                        }
                    }
            );
        } catch (Throwable e) {
            XposedBridge.log(e);
        }
    }

    private void setMinFontSize(LoadPackageParam lpparam) {
        if (Devices.getDevice() == Devices.SPRINT) {
            setMinFontSizeSprint(lpparam);
        } else {
            setMinFontSizeOther(lpparam);
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

                        if (settings.getBoolean(Prefs.ENABLE_SMALLER_TEXT_MESSAGES, false)) {
                            int minimumZoom = settings.getInt(Prefs.MINIMUM_ZOOM_LEVEL_MESSAGES, 30);

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

                        if (settings.getBoolean(Prefs.ENABLE_SMALLER_TEXT_MESSAGES, false)) {
                            int minimumZoom = settings.getInt(Prefs.MINIMUM_ZOOM_LEVEL_MESSAGES, 30);

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

        if (settings != null) {
            debug = settings.getBoolean(Prefs.ENABLE_DEBUGGING, false);
        }

        if (debug) {
            XposedBridge.log(String.format("G2SkinTweaks: %s", text));
        }
    }
}