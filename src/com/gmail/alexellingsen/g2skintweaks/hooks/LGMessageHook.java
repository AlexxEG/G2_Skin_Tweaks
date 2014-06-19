package com.gmail.alexellingsen.g2skintweaks.hooks;

import android.app.ListActivity;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import com.gmail.alexellingsen.g2skintweaks.Prefs;
import com.gmail.alexellingsen.g2skintweaks.utils.Devices;
import com.gmail.alexellingsen.g2skintweaks.utils.SettingsHelper;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import java.io.File;

public class LGMessageHook {

    private static final String PACKAGE = "com.android.mms";

    private static SettingsHelper mSettings;
    private static LinearLayout frame;
    private static LinearLayout emptyTextLayout;

    public static void init(SettingsHelper settings) {
        mSettings = settings;
    }

    public static void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) {
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

        hookConversationListBackground(lpparam);
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

    public static void hookConversationListBackgroundOther(LoadPackageParam lpparam, XC_MethodHook hook) {
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

    public static void hookConversationListBackgroundSprint(LoadPackageParam lpparam, XC_MethodHook hook) {
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

    private static int reverseNumber(int num, int min, int max) {
        return (max + min) - num;
    }
}
