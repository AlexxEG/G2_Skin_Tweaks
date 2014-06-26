package com.gmail.alexellingsen.g2skintweaks.hooks;

import android.app.ListActivity;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.text.TextPaint;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import com.gmail.alexellingsen.g2skintweaks.G2SkinTweaks;
import com.gmail.alexellingsen.g2skintweaks.Prefs;
import com.gmail.alexellingsen.g2skintweaks.utils.Devices;
import com.gmail.alexellingsen.g2skintweaks.utils.SettingsHelper;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import java.io.File;
import java.util.ArrayList;

public class LGMessageHook {

    private static final String PACKAGE = "com.android.mms";
    private static final int DEFAULT_MINIMUM_ZOOM = 85;

    private static SettingsHelper mSettings;
    private static LinearLayout frame;
    private static LinearLayout emptyTextLayout;

    public static void init(SettingsHelper settings) {
        mSettings = settings;
    }

    public static void handleInitPackageResources(InitPackageResourcesParam resparam) {
        if (!resparam.packageName.equals(PACKAGE))
            return;

        resparam.res.hookLayout(PACKAGE, "layout", "conversation_list_screen", new XC_LayoutInflated() {
            @Override
            public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                frame = (LinearLayout) liparam.view.findViewById(
                        liparam.res.getIdentifier("converation_screen", "id", PACKAGE));
                emptyTextLayout = (LinearLayout) liparam.view.findViewById(
                        liparam.res.getIdentifier("emptyText", "id", PACKAGE));
            }
        });
    }

    public static void handleLoadPackage(final LoadPackageParam lpparam) {
        if (!lpparam.packageName.equals(PACKAGE))
            return;

        setMinFontSize(lpparam);
        hookConversationListItem(lpparam);
        hookConversationListBackground(lpparam);
        hookMessageListItem(lpparam);
        hookMessagingNotification(lpparam);
    }

    private static void hookConversationListItem(final LoadPackageParam lpparam) {
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

    private static void hookConversationListItemOther(final LoadPackageParam lpparam) {
        final Class<?> rootClass;
        final Class<?> subClass;

        try {
            rootClass = XposedHelpers.findClass(
                    "com.android.mms.ui.ConversationListItem",
                    lpparam.classLoader);

            subClass = XposedHelpers.findClass(
                    "com.android.mms.ui.ConversationListItem$ConversationListItemRight",
                    lpparam.classLoader);
        } catch (XposedHelpers.ClassNotFoundError e) {
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

    private static void hookConversationListItemSprint(final LoadPackageParam lpparam) {
        final Class<?> rootClass;
        final Class<?> subClass;

        try {
            rootClass = XposedHelpers.findClass(
                    "com.android.mms.ui.ConversationListItem",
                    lpparam.classLoader);

            subClass = XposedHelpers.findClass(
                    "com.android.mms.ui.ConversationListItem$ConversationListItemRight",
                    lpparam.classLoader);
        } catch (XposedHelpers.ClassNotFoundError e) {
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

    private static void hookConversationListBackground(LoadPackageParam lpparam) {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                boolean enableConversationListBG = mSettings.getBoolean(
                        Prefs.ENABLE_CONVERSATION_LIST_BG, false);

                if (enableConversationListBG) {
                    File folder = new File(Environment.getExternalStorageDirectory(), "G2SkinTweaks");

                    if (!folder.exists()) {
                        return;
                    }

                    // Create .nomedia file to hide background image from gallery
                    File noMediaFile = new File(folder, ".nomedia");

                    if (!noMediaFile.exists()) {
                        noMediaFile.createNewFile();
                    }

                    File file = new File(folder, "background.png");

                    if (!file.exists()) {
                        return;
                    }

                    Drawable d = Drawable.createFromPath(file.getPath());

                    if (d == null) {
                        return;
                    }

                    frame.setBackground(d);
                }

                boolean enableConversationListBGColor = mSettings.getBoolean(
                        Prefs.CONVERSATION_LIST_BG_COLOR, false);

                if (enableConversationListBGColor) {
                    int color = mSettings.getInt(Prefs.CONVERSATION_LIST_BG_COLOR_VALUE, Color.TRANSPARENT);

                    // Set the parent view's background color to create a overlay effect.
                    ((ViewGroup) frame.getParent()).setBackgroundColor(color);

                    int alpha = mSettings.getInt(Prefs.CONVERSATION_LIST_BG_COLOR_ALPHA, 255);

                    // Setting the background's alpha seems to have the opposite
                    // effect, 255 being fully transparent. Therefore reverse the number.
                    alpha = reverseNumber(alpha, 0, 255);

                    if (frame.getBackground() == null) {
                        frame.setBackgroundColor(Color.WHITE);
                    }

                    frame.getBackground().setAlpha(alpha);
                }

                if (enableConversationListBG || enableConversationListBGColor) {
                    // Remove color from ListView which are blocking background color on Sprint devices.
                    if (Devices.isAnyDevice(Devices.SPRINT, Devices.VERIZON)) {
                        ListView lv = ((ListActivity) param.thisObject).getListView();

                        lv.setBackgroundColor(Color.TRANSPARENT);

                        emptyTextLayout.setBackgroundColor(Color.TRANSPARENT);
                    }
                }
            }
        };

        switch (Devices.getDevice()) {
            case SPRINT:
            case VERIZON:
                hookConversationListBackgroundSprint(lpparam, hook);
                break;
            case OTHER:
                hookConversationListBackgroundOther(lpparam, hook);
                break;
        }
    }

    private static void hookConversationListBackgroundOther(LoadPackageParam lpparam, XC_MethodHook hook) {
        XposedHelpers.findAndHookMethod(
                PACKAGE + ".ui.ConversationListFragment",
                lpparam.classLoader,
                "onCreateView",
                "android.view.LayoutInflater",
                "android.view.ViewGroup",
                "android.os.Bundle",

                hook
        );
    }

    private static void hookConversationListBackgroundSprint(LoadPackageParam lpparam, XC_MethodHook hook) {
        XposedHelpers.findAndHookMethod(
                PACKAGE + ".ui.ConversationList",
                lpparam.classLoader,
                "onCreate",
                "android.os.Bundle",

                hook
        );

        // If a option that changes background is enable, turn all the
        // list item transparent so that the background can be seen.
        XposedHelpers.findAndHookMethod(
                PACKAGE + ".ui.ConversationListItem",
                lpparam.classLoader,
                "updateBackground",

                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        // Remove background from each list item so background can be seen.
                        if (mSettings.getBoolean(Prefs.CONVERSATION_LIST_BG_COLOR, false) ||
                                mSettings.getBoolean(Prefs.ENABLE_CONVERSATION_LIST_BG, false)) {
                            ((View) param.thisObject).setBackgroundColor(Color.TRANSPARENT);
                        }
                    }
                }
        );
    }

    private static void hookMessageListItem(final LoadPackageParam lpparam) {
        final Class<?> findClass;

        try {
            findClass = XposedHelpers.findClass(
                    "com.android.mms.ui.MessageListItem",
                    lpparam.classLoader);
        } catch (XposedHelpers.ClassNotFoundError e) {
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

    private static void hookMessageListItemOther(Class<?> finalClass, XC_MethodHook hook, XC_MethodHook resizeHook) {
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

    private static void hookMessageListItemSprint(Class<?> finalClass, XC_MethodHook hook, XC_MethodHook resizeHook) {
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

    private static void hookMessagingNotification(final LoadPackageParam lpparam) {
        final Class<?> finalClass;

        try {
            finalClass = XposedHelpers.findClass(
                    "com.android.mms.transaction.MessagingNotification",
                    lpparam.classLoader);
        } catch (XposedHelpers.ClassNotFoundError e) {
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

    private static int reverseNumber(int num, int min, int max) {
        return (max + min) - num;
    }

    private static void setMinFontSize(LoadPackageParam lpparam) {
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

    private static void setMinFontSizeOther(final LoadPackageParam lpparam) {
        final Class<?> finalClass;
        try {
            finalClass = XposedHelpers.findClass(
                    "com.android.mms.pinchApi.PinchDetector",
                    lpparam.classLoader);
        } catch (XposedHelpers.ClassNotFoundError e) {
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

                        G2SkinTweaks.log("'processActionMove' ran");

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

    private static void setMinFontSizeSprint(final LoadPackageParam lpparam) {
        final Class<?> finalClass;

        try {
            finalClass = XposedHelpers.findClass(
                    "com.lge.mms.pinchApi.PinchDetector",
                    lpparam.classLoader);
        } catch (XposedHelpers.ClassNotFoundError e) {
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

                        G2SkinTweaks.log("'processTouchEvent' ran");

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
}
