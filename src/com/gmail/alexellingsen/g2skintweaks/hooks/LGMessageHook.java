package com.gmail.alexellingsen.g2skintweaks.hooks;

import android.app.ListActivity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextPaint;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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

    private static LinearLayout mBackgroundFrame;
    private static LinearLayout mEmptyTextLayout;

    public static void init(SettingsHelper settings) {
        mSettings = settings;
    }

    public static void handleInitPackageResources(InitPackageResourcesParam resparam) {
        if (!resparam.packageName.equals(PACKAGE))
            return;

        resparam.res.hookLayout(PACKAGE, "layout", "conversation_list_screen", new XC_LayoutInflated() {
            @Override
            public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                mBackgroundFrame = (LinearLayout) liparam.view.findViewById(
                        liparam.res.getIdentifier("converation_screen", "id", PACKAGE));
                mEmptyTextLayout = (LinearLayout) liparam.view.findViewById(
                        liparam.res.getIdentifier("emptyText", "id", PACKAGE));
            }
        });
    }

    public static void handleLoadPackage(LoadPackageParam lpparam) {
        if (Devices.isAnyDevice(Devices.SPRINT, Devices.VERIZON)) {
            hookPaintSetColorSprint(lpparam);
        }

        if (!lpparam.packageName.equals(PACKAGE))
            return;

        setMinFontSize(lpparam);
        hookConversationListItem(lpparam);
        handleConvoListBackground(lpparam);
        hookMessageListItem(lpparam);
        hookMessagingNotification(lpparam);
    }

    private static void handleConvoListBackground(LoadPackageParam lpparam) {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                boolean enableConvoListBG = mSettings.getBoolean(
                        Prefs.ENABLE_CONVERSATION_LIST_BG, false);

                if (enableConvoListBG) {
                    File folder = new File(Environment.getExternalStorageDirectory(), "G2SkinTweaks");

                    if (!folder.exists()) {
                        return;
                    }

                    // Create .nomedia file to hide background image from gallery
                    File noMediaFile = new File(folder, ".nomedia");

                    if (!noMediaFile.exists()) {
                        boolean success = noMediaFile.createNewFile();

                        if (!success) {
                            G2SkinTweaks.log("Couldn't create .nomedia file", true);
                        }
                    }

                    File file = new File(folder, "background.png");

                    if (!file.exists()) {
                        return;
                    }

                    Drawable d = Drawable.createFromPath(file.getPath());

                    if (d == null) {
                        return;
                    }

                    mBackgroundFrame.setBackground(d);
                }

                boolean enableConvoListBGColor = mSettings.getBoolean(
                        Prefs.CONVERSATION_LIST_BG_COLOR, false);

                if (enableConvoListBGColor) {
                    int alpha = mSettings.getInt(Prefs.CONVERSATION_LIST_BG_COLOR_ALPHA, 255);
                    int color = mSettings.getInt(Prefs.CONVERSATION_LIST_BG_COLOR_VALUE, Color.TRANSPARENT);

                    // Set the parent view's background color to create a overlay effect.
                    ((ViewGroup) mBackgroundFrame.getParent()).setBackgroundColor(color);

                    // Setting the background's alpha seems to have the opposite
                    // effect, 255 being fully transparent. Therefore reverse the number.
                    alpha = reverseNumber(alpha, 0, 255);

                    if (mBackgroundFrame.getBackground() == null) {
                        mBackgroundFrame.setBackgroundColor(Color.WHITE);
                    }

                    mBackgroundFrame.getBackground().setAlpha(alpha);
                }

                if (enableConvoListBG || enableConvoListBGColor) {
                    // Remove color from ListView which are blocking background color on Sprint devices.
                    if (Devices.isAnyDevice(Devices.SPRINT, Devices.VERIZON)) {
                        ListView lv = ((ListActivity) param.thisObject).getListView();

                        lv.setBackgroundColor(Color.TRANSPARENT);

                        mEmptyTextLayout.setBackgroundColor(Color.TRANSPARENT);
                    }
                }
            }
        };

        switch (Devices.getDevice()) {
            case SPRINT:
            case VERIZON:
                handleConvoListBackgroundSprint(lpparam, hook);
                break;
            case OTHER:
                handleConvoListBackgroundOther(lpparam, hook);
                break;
        }
    }

    private static void handleConvoListBackgroundOther(LoadPackageParam lpparam, XC_MethodHook hook) {
        XposedHelpers.findAndHookMethod(
                PACKAGE + ".ui.ConversationListFragment",
                lpparam.classLoader,
                "onCreateView",
                LayoutInflater.class,
                ViewGroup.class,
                Bundle.class,

                hook
        );
    }

    private static void handleConvoListBackgroundSprint(LoadPackageParam lpparam, XC_MethodHook hook) {
        XposedHelpers.findAndHookMethod(
                PACKAGE + ".ui.ConversationList",
                lpparam.classLoader,
                "onCreate",
                Bundle.class,

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

    private static void hookConversationListItem(LoadPackageParam lpparam) {
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

    private static void hookConversationListItemOther(LoadPackageParam lpparam) {
        final Class<?> rootClass;
        final Class<?> subClass;

        try {
            rootClass = XposedHelpers.findClass(
                    PACKAGE + ".ui.ConversationListItem",
                    lpparam.classLoader);

            subClass = XposedHelpers.findClass(
                    PACKAGE + ".ui.ConversationListItem$ConversationListItemRight",
                    lpparam.classLoader);
        } catch (XposedHelpers.ClassNotFoundError e) {
            G2SkinTweaks.log(e);
            return;
        }

        XposedHelpers.findAndHookMethod(
                subClass,
                "onDrawBottomline",
                Canvas.class,
                int.class,
                boolean.class,

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
                Canvas.class,
                int.class,
                boolean.class,
                int.class,
                Drawable.class,
                int.class,

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

    private static void hookConversationListItemSprint(LoadPackageParam lpparam) {
        final Class<?> rootClass;
        final Class<?> subClass;

        try {
            rootClass = XposedHelpers.findClass(
                    PACKAGE + ".ui.ConversationListItem",
                    lpparam.classLoader);

            subClass = XposedHelpers.findClass(
                    PACKAGE + ".ui.ConversationListItem$ConversationListItemRight",
                    lpparam.classLoader);
        } catch (XposedHelpers.ClassNotFoundError e) {
            G2SkinTweaks.log(e);
            return;
        }

        XposedHelpers.findAndHookMethod(
                subClass,
                "onDraw",
                Canvas.class,

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

    private static void hookMessageListItem(final LoadPackageParam lpparam) {
        final Class<?> findClass;

        try {
            findClass = XposedHelpers.findClass(
                    PACKAGE + ".ui.MessageListItem",
                    lpparam.classLoader);
        } catch (XposedHelpers.ClassNotFoundError e) {
            G2SkinTweaks.log(e);
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
                    G2SkinTweaks.log(e);
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
                PACKAGE + ".ui.MessageListAdapter$AvatarCache",
                PACKAGE + ".ui.MessageItem",
                ListView.class,
                int.class,
                boolean.class,
                boolean.class,

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
                PACKAGE + ".ui.MessageListAdapter$AvatarCache",
                PACKAGE + ".ui.MessageItem",
                ListView.class,
                int.class,
                boolean.class,
                boolean.class,
                ArrayList.class,

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
                    PACKAGE + ".transaction.MessagingNotification",
                    lpparam.classLoader);
        } catch (XposedHelpers.ClassNotFoundError e) {
            G2SkinTweaks.log(e);
            return;
        }

        XposedHelpers.findAndHookMethod(
                finalClass,
                "turnOnBacklight",
                Context.class,

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

    private static void setMinFontSizeOther(LoadPackageParam lpparam) {
        final Class<?> finalClass;
        try {
            finalClass = XposedHelpers.findClass(
                    PACKAGE + ".pinchApi.PinchDetector",
                    lpparam.classLoader);
        } catch (XposedHelpers.ClassNotFoundError e) {
            G2SkinTweaks.log(e);
            throw e;
        }

        XposedHelpers.findAndHookMethod(
                finalClass,
                "processActionMove",
                MotionEvent.class,

                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        int min = XposedHelpers.getIntField(param.thisObject, "MIN_ZOOM");

                        G2SkinTweaks.log("'processActionMove' ran", true);

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

    private static void setMinFontSizeSprint(LoadPackageParam lpparam) {
        final Class<?> finalClass;

        try {
            finalClass = XposedHelpers.findClass(
                    "com.lge.mms.pinchApi.PinchDetector",
                    lpparam.classLoader);
        } catch (XposedHelpers.ClassNotFoundError e) {
            G2SkinTweaks.log(e);
            throw e;
        }

        XposedHelpers.findAndHookMethod(
                finalClass,
                "processTouchEvent",
                MotionEvent.class,

                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        int min = XposedHelpers.getIntField(param.thisObject, "MIN_ZOOM");

                        G2SkinTweaks.log("'processTouchEvent' ran", true);

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

    private static void hookPaintSetColorSprint(LoadPackageParam lpparam) {
        final Class<?> findClass;

        try {
            findClass = XposedHelpers.findClass(
                    "android.graphics.Paint",
                    lpparam.classLoader);
        } catch (XposedHelpers.ClassNotFoundError e) {
            return;
        }

        /*Has to hook this method since code can't be injected in middle of a method.
          Sprint's Messenger app draws top & bottom line in same method, unlike international version,
          therefore normally only allowing one color.*/

        XposedHelpers.findAndHookMethod(
                findClass,
                "setColor",
                int.class,

                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!mSettings.getBoolean(Prefs.ENABLE_CONVERSATION_TEXT_COLOR, false)) {
                            return;
                        }

                        StackTraceElement[] elements = Thread.currentThread().getStackTrace();

                        for (StackTraceElement element : elements) {
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
}
