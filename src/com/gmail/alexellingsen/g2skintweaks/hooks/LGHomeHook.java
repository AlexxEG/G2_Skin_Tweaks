package com.gmail.alexellingsen.g2skintweaks.hooks;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import com.gmail.alexellingsen.g2skintweaks.Prefs;
import com.gmail.alexellingsen.g2skintweaks.utils.SettingsHelper;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class LGHomeHook {

    private static final String PACKAGE = "com.lge.launcher2";

    private static SettingsHelper mSettings;
    private static ImageView icon;

    public static void init(SettingsHelper settings) {
        mSettings = settings;
    }

    public static void handleInitPackageResources(final XC_InitPackageResources.InitPackageResourcesParam resparam) {
        if (!resparam.packageName.equals(PACKAGE))
            return;

        resparam.res.hookLayout(PACKAGE, "layout", "lg_appinfo", new XC_LayoutInflated() {
            @Override
            public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                icon = (ImageView) liparam.view.findViewById(resparam.res.getIdentifier("app_icon", "id", PACKAGE));
            }
        });
    }

    public static void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!lpparam.packageName.equals(PACKAGE))
            return;

        XposedHelpers.findAndHookMethod(
                "com.lge.launcher2.appinfo.LGAppInfoDialog",
                lpparam.classLoader,
                "createDialog",
                "int",

                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (mSettings.getBoolean(Prefs.ENABLE_APPLICATIONS_SHORTCUT, false)) {
                            final Activity activity = (Activity) XposedHelpers.getObjectField(param.thisObject, "mActivity");
                            final Object mAppInfo = XposedHelpers.getObjectField(param.thisObject, "mAppInfo");
                            // Get the intent from mAppInfo to get the app's package.
                            final Intent intent = (Intent) XposedHelpers.getObjectField(mAppInfo, "intent");

                            icon.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent appInfoIntent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    appInfoIntent.setData(Uri.parse("package:" + intent.getComponent().getPackageName()));
                                    activity.startActivity(appInfoIntent);
                                }
                            });
                        }
                    }
                }
        );
    }
}
