package com.gmail.alexellingsen.g2skintweaks.hooks;

import android.content.Context;
import android.content.res.XModuleResources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;
import com.gmail.alexellingsen.g2skintweaks.G2SkinTweaks;
import com.gmail.alexellingsen.g2skintweaks.Prefs;
import com.gmail.alexellingsen.g2skintweaks.R;
import com.gmail.alexellingsen.g2skintweaks.utils.SettingsHelper;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class LGLockScreenHook {

    private static final String PACKAGE = "com.android.keyguard";
    private static final String USE_PACKAGE = "com.lge.lockscreen";

    private static XModuleResources mModRes;
    private static SettingsHelper mSettings;

    public static void init(SettingsHelper settings) {
        mSettings = settings;
    }

    public static void handleLoadPackage(LoadPackageParam lpparam) {
        if (!lpparam.packageName.equals(PACKAGE)) {
            return;
        }

        handleHideShortcutText(lpparam);
        handlePatternDots(lpparam);
    }

    public static void handleInitPackageResources(InitPackageResourcesParam resparam, XModuleResources modRes) {
        if (!resparam.packageName.equals(PACKAGE)) {
            return;
        }

        mModRes = modRes;
    }

    private static void handleHideShortcutText(LoadPackageParam lpparam) {
        Class<?> findClass;

        try {
            findClass = XposedHelpers.findClass(
                    USE_PACKAGE + ".widget.draglayer.LockScreenShortcut",
                    lpparam.classLoader
            );
        } catch (Throwable e) {
            G2SkinTweaks.log(e);
            G2SkinTweaks.log("Couldn't find class");
            return;
        }

        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (mSettings.getBoolean(Prefs.HIDE_LOCKSCREEN_SHORTCUT_TITLES, false)) {
                    TextView tvTitle = (TextView) XposedHelpers.getObjectField(param.thisObject, "mIconName");

                    tvTitle.setVisibility(View.INVISIBLE);
                }
            }
        };

        XposedHelpers.findAndHookMethod(findClass, "init", Context.class, hook);
        XposedHelpers.findAndHookMethod(findClass, "setOrientation", int.class, hook);
    }

    private static void handlePatternDots(LoadPackageParam lpparam) {
        Class<?> findClass;

        try {
            findClass = XposedHelpers.findClass(
                    USE_PACKAGE + ".widget.pattern.LgeLockPatternView",
                    lpparam.classLoader
            );
        } catch (Throwable e) {
            G2SkinTweaks.log(e);
            G2SkinTweaks.log("Couldn't find class");
            return;
        }

        XposedHelpers.findAndHookMethod(
                findClass,
                "drawCircle",
                Canvas.class,
                int.class,
                int.class,
                boolean.class,

                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (mSettings.getBoolean(Prefs.ENABLE_AOSP_PATTERN_DOTS, false)) {
                            Drawable d1 = mModRes.getDrawable(R.drawable.indicator_code_lock_point_area_default_holo);
                            Drawable d2 = mModRes.getDrawable(R.drawable.indicator_code_lock_point_area_green_holo);
                            Drawable d3 = mModRes.getDrawable(R.drawable.indicator_code_lock_point_area_red_holo);

                            Bitmap mBitmap1 = drawableToBitmap(d1);
                            Bitmap mBitmap2 = drawableToBitmap(d2);
                            Bitmap mBitmap3 = drawableToBitmap(d3);

                            XposedHelpers.setObjectField(param.thisObject, "mBitmapCircleDefault", mBitmap1);
                            XposedHelpers.setObjectField(param.thisObject, "mBitmapCircleGreen", mBitmap2);
                            XposedHelpers.setObjectField(param.thisObject, "mBitmapCircleRed", mBitmap3);
                        }
                    }
                }
        );
    }

    private static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        int width = drawable.getIntrinsicWidth(), height = drawable.getIntrinsicHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }
}