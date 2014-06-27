package com.gmail.alexellingsen.g2skintweaks.hooks;

import android.view.View;
import android.widget.ImageView;
import com.gmail.alexellingsen.g2skintweaks.Prefs;
import com.gmail.alexellingsen.g2skintweaks.utils.SettingsHelper;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;

public class LGSettings {

    public static final String PACKAGE = "com.android.settings";
    public static final String PACKAGE_EASY = "com.lge.settings.easy";

    private static SettingsHelper mSettings;

    public static void init(SettingsHelper settings) {
        mSettings = settings;
    }

    public static void handleInitPackageResources(InitPackageResourcesParam resparam) {
        if (resparam.packageName.equals(PACKAGE) || resparam.packageName.equals(PACKAGE_EASY)) {
            boolean enableRemoveDividers = mSettings.getBoolean(Prefs.ENABLE_REMOVE_DIVIDERS, false);

            if (enableRemoveDividers) {
                if (resparam.packageName.equals(PACKAGE)) {
                    removeDividers(resparam);
                } else {
                    removeDividersEasy(resparam);
                }
            }
        }
    }

    private static void removeDividers(InitPackageResourcesParam resparam) {
        try {
            resparam.res.hookLayout(
                    PACKAGE,
                    "layout",
                    "preference_widget_switch",

                    new XC_LayoutInflated() {
                        @Override
                        public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                            ImageView divider = (ImageView) liparam.view.findViewById(
                                    liparam.res.getIdentifier("switchImage", "id", PACKAGE));

                            divider.setVisibility(View.INVISIBLE);
                        }
                    }
            );

            resparam.res.hookLayout(
                    PACKAGE,
                    "layout",
                    "preference_header_switch_item",

                    new XC_LayoutInflated() {
                        @Override
                        public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                            View divider = liparam.view.findViewById(
                                    liparam.res.getIdentifier("switchDivider", "id", PACKAGE));

                            divider.setVisibility(View.INVISIBLE);
                        }
                    }
            );
        } catch (Throwable e) {
            XposedBridge.log(e);
        }
    }

    private static void removeDividersEasy(InitPackageResourcesParam resparam) {
        try {
            resparam.res.hookLayout(
                    PACKAGE_EASY,
                    "layout",
                    "easy_preference",

                    new XC_LayoutInflated() {
                        @Override
                        public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                            ImageView divider = (ImageView) liparam.view.findViewById(
                                    liparam.res.getIdentifier("easy_vertical_divider", "id", PACKAGE_EASY));
                            divider.setVisibility(View.INVISIBLE);
                        }
                    }
            );
        } catch (Throwable e) {
            XposedBridge.log(e);
        }
    }
}