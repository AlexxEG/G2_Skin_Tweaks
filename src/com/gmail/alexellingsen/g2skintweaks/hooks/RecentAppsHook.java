package com.gmail.alexellingsen.g2skintweaks.hooks;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.widget.LinearLayout;
import com.gmail.alexellingsen.g2skintweaks.Prefs;
import com.gmail.alexellingsen.g2skintweaks.utils.SettingsHelper;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class RecentAppsHook {

    private static SettingsHelper mSettings;

    private static LinearLayout frame;
    private static Drawable stockBackground;

    public static void init(SettingsHelper settings) {
        mSettings = settings;
    }

    public static void handleInitPackageResources(InitPackageResourcesParam resparam) {
        if (!resparam.packageName.equals("com.android.systemui"))
            return;

        // Store stock background to use if option is disabled.
        stockBackground = resparam.res.getDrawable(resparam.res.getIdentifier("status_bar_recents_background", "drawable", "com.android.systemui"));

        XC_LayoutInflated hook = new XC_LayoutInflated() {
            @Override
            public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                frame = (LinearLayout) liparam.view.findViewById(liparam.res.getIdentifier("recents_bg_protect", "id", "com.android.systemui"));
            }
        };

        resparam.res.hookLayout("com.android.systemui", "layout", "status_bar_recent_panel", hook);
    }

    public static void handleLoadPackage(LoadPackageParam lpparam) {
        if (!lpparam.packageName.equals("com.android.systemui"))
            return;

        XposedHelpers.findAndHookMethod(
                "com.android.systemui.recent.RecentsActivity",
                lpparam.classLoader,
                "onStart",

                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        XposedHelpers.callMethod(param.thisObject, "updateWallpaperVisibility", true);
                    }
                }
        );

        XposedHelpers.findAndHookMethod(
                "com.android.systemui.recent.RecentsActivity",
                lpparam.classLoader,
                "onResume",

                new XC_MethodHook() {
                    @SuppressWarnings("deprecation")
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        boolean enableTransparentBackground = mSettings.getBoolean(Prefs.RECENT_APPS_OPACITY, false);

                        if (enableTransparentBackground) {
                            int alpha = mSettings.getInt(Prefs.RECENT_APPS_OPACITY_VALUE, 0);

                            frame.setBackgroundDrawable(null);
                            frame.setBackgroundColor(Color.argb(alpha, 0, 0, 0));
                        } else {
                            frame.setBackgroundDrawable(stockBackground);
                        }
                    }
                }
        );
    }
}