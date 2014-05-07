package com.gmail.alexellingsen.g2skintweaks.hooks;

import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.widget.LinearLayout;
import com.gmail.alexellingsen.g2skintweaks.Prefs;
import com.gmail.alexellingsen.g2skintweaks.SettingsHelper;
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

    public static void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {
        if (!resparam.packageName.equals(PACKAGE))
            return;

        resparam.res.hookLayout(PACKAGE, "layout", "conversation_list_screen", new XC_LayoutInflated() {
            @Override
            public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                frame = (LinearLayout) liparam.view.findViewById(liparam.res.getIdentifier("converation_screen", "id", PACKAGE));
            }
        });
    }

    public static void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(PACKAGE))
            return;

        XposedHelpers.findAndHookMethod(
                PACKAGE + ".ui.ConversationListFragment",
                lpparam.classLoader,
                "onCreateView",
                "android.view.LayoutInflater",
                "android.view.ViewGroup",
                "android.os.Bundle",

                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (mSettings.getBoolean(Prefs.ENABLE_CONVERSATION_LIST_BG, false)) {
                            File folder = new File(Environment.getExternalStorageDirectory(), "G2SkinTweaks");

                            if (!folder.exists())
                                return;

                            File file = new File(folder, "background.png");

                            if (!file.exists())
                                return;

                            Drawable d = Drawable.createFromPath(file.getPath());

                            if (d == null)
                                return;

                            frame.setBackground(d);
                        }
                    }
                }
        );
    }
}
