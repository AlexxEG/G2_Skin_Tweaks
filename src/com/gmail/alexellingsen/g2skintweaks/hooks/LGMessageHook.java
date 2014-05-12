package com.gmail.alexellingsen.g2skintweaks.hooks;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import com.gmail.alexellingsen.g2skintweaks.Prefs;
import com.gmail.alexellingsen.g2skintweaks.utils.Devices;
import com.gmail.alexellingsen.g2skintweaks.utils.SettingsHelper;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.io.File;

public class LGMessageHook {

    private static final String PACKAGE = "com.android.mms";

    private static SettingsHelper mSettings;
    private static LinearLayout frame;

    public static void init(SettingsHelper settings) {
        mSettings = settings;
    }

    public static void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) {
        if (!resparam.packageName.equals(PACKAGE))
            return;

        resparam.res.hookLayout(PACKAGE, "layout", "conversation_list_screen", new XC_LayoutInflated() {
            @Override
            public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                frame = (LinearLayout) liparam.view.findViewById(liparam.res.getIdentifier("converation_screen", "id", PACKAGE));
            }
        });
    }

    public static void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
        if (!lpparam.packageName.equals(PACKAGE))
            return;

        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (mSettings.getBoolean(Prefs.ENABLE_CONVERSATION_LIST_BG, false)) {
                    File folder = new File(Environment.getExternalStorageDirectory(), "G2SkinTweaks");

                    if (!folder.exists())
                        return;

                    // Create .nomedia file to hide background image from gallery
                    File noMediaFile = new File(folder, ".nomedia");

                    if (!noMediaFile.exists())
                        noMediaFile.createNewFile();

                    File file = new File(folder, "background.png");

                    if (!file.exists())
                        return;

                    Drawable d = Drawable.createFromPath(file.getPath());

                    if (d == null)
                        return;

                    frame.setBackground(d);
                }

                if (mSettings.getBoolean(Prefs.CONVERSATION_LIST_BG_COLOR, false)) {
                    int color = mSettings.getInt(Prefs.CONVERSATION_LIST_BG_COLOR_VALUE, Color.TRANSPARENT);

                    // Set the parent view's background color to create a overlay effect.
                    ((RelativeLayout) frame.getParent()).setBackgroundColor(color);

                    int alpha = mSettings.getInt(Prefs.CONVERSATION_LIST_BG_COLOR_ALPHA, 255);

                    // Setting the background's alpha seems to have the opposite
                    // effect, 255 being fully transparent. Therefore reverse the number.
                    alpha = reverseNumber(alpha, 0, 255);

                    if (frame.getBackground() != null)
                        frame.getBackground().setAlpha(alpha);
                }
            }
        };

        if (Devices.getDevice() == Devices.SPRINT) {
            hookConversationListBackgroundSprint(lpparam, hook);
        } else {
            hookConversationListBackgroundOther(lpparam, hook);
        }
    }

    private static int reverseNumber(int num, int min, int max) {
        return (max + min) - num;
    }

    public static void hookConversationListBackgroundOther(XC_LoadPackage.LoadPackageParam lpparam, XC_MethodHook hook) {
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

    public static void hookConversationListBackgroundSprint(XC_LoadPackage.LoadPackageParam lpparam, XC_MethodHook hook) {
        XposedHelpers.findAndHookMethod(
                PACKAGE + ".ui.ConversationList",
                lpparam.classLoader,
                "onCreate",
                "android.os.Bundle",

                hook
        );
    }

}
