package com.gmail.alexellingsen.g2skintweaks.hooks;

import android.content.res.XModuleResources;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import com.gmail.alexellingsen.g2skintweaks.Prefs;
import com.gmail.alexellingsen.g2skintweaks.R;
import com.gmail.alexellingsen.g2skintweaks.utils.SettingsHelper;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class StatusBarHook {

    private static final String PACKAGE = "com.android.systemui";

    private static SettingsHelper mSettings;

    private static ImageView mClearButton;
    private static int mClearDrawableID = -1;

    public static void init(SettingsHelper settings) {
        mSettings = settings;
    }

    public static void handleInitPackageResources(final InitPackageResourcesParam resparam, final XModuleResources modRes) throws Throwable {
        if (!resparam.packageName.equals(PACKAGE))
            return;

        if (mSettings.getBoolean(Prefs.MOVE_CLEAR_NOTIFICATIONS_BTN, false)) {
            resparam.res.hookLayout(PACKAGE, "layout", "status_bar_expanded_setting_layout", new XC_LayoutInflated() {
                @Override
                public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                    RelativeLayout rl = (RelativeLayout) liparam.view.findViewById(
                            resparam.res.getIdentifier("setting_layout_normal", "id", PACKAGE)
                    );

                    rl.setVisibility(View.GONE);
                }
            });

            resparam.res.hookLayout(PACKAGE, "layout", "status_bar_expanded_header", new XC_LayoutInflated() {
                @Override
                public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                    FrameLayout fl = (FrameLayout) liparam.view.findViewById(
                            resparam.res.getIdentifier("settings_button_holder", "id", PACKAGE)
                    );

                    LinearLayout.LayoutParams flParams = (LinearLayout.LayoutParams) fl.getLayoutParams();

                    // Double the button holder panel's width to fit new button.
                    flParams.width = flParams.width * 2;

                    fl.setLayoutParams(flParams);

                    ImageView settingsButton = (ImageView) liparam.view.findViewById(
                            resparam.res.getIdentifier("settings_button", "id", PACKAGE)
                    );
                    ImageView clearButton = new ImageView(fl.getContext());
                    FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(settingsButton.getLayoutParams());
                    FrameLayout.LayoutParams paramsSettings = (FrameLayout.LayoutParams) settingsButton.getLayoutParams();

                    // Move settings to the right, and clear button to left.
                    params.gravity = Gravity.LEFT;
                    paramsSettings.gravity = Gravity.RIGHT;

                    settingsButton.setLayoutParams(paramsSettings);

                    clearButton.setLayoutParams(params);
                    clearButton.setScaleType(ImageView.ScaleType.CENTER);
                    clearButton.setImageDrawable(getClearNotificationsDrawable(resparam, modRes));
                    mClearButton = clearButton;

                    fl.addView(clearButton);
                }
            });
        }
    }

    public static void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(PACKAGE))
            return;

        if (mSettings.getBoolean(Prefs.MOVE_CLEAR_NOTIFICATIONS_BTN, false)) {
            XposedHelpers.findAndHookMethod(
                    PACKAGE + ".statusbar.phone.PhoneStatusBar",
                    lpparam.classLoader,
                    "makeStatusBarView",

                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            if (mClearButton != null) {
                                // Copy the clear notifications button click listener.
                                View.OnClickListener listener = (View.OnClickListener)
                                        XposedHelpers.getObjectField(param.thisObject, "mClearButtonListener");

                                mClearButton.setOnClickListener(listener);
                            }
                        }
                    }
            );
        }
    }

    /**
     * Injects the 'ic_clear_noti' drawable to SystemUI resources, then returns it.
     */
    private static Drawable getClearNotificationsDrawable(InitPackageResourcesParam resparam, XModuleResources modRes) {
        // Only insert the drawable if it's not already there.
        if (mClearDrawableID == -1) {
            mClearDrawableID = resparam.res.addResource(modRes, R.drawable.ic_clear_noti);
        }

        return resparam.res.getDrawable(mClearDrawableID);
    }
}
