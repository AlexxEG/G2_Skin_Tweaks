package com.gmail.alexellingsen.g2skintweaks.hooks;

import android.content.res.XModuleResources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import com.gmail.alexellingsen.g2skintweaks.Prefs;
import com.gmail.alexellingsen.g2skintweaks.R;
import com.gmail.alexellingsen.g2skintweaks.utils.SettingsHelper;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class LGLockScreenHook {

    private static final String PACKAGE = "com.android.keyguard";
    private static final String USE_PACKAGE = "com.lge.lockscreen";

    private static SettingsHelper mSettings;

    private static Bitmap mBitmap1;
    private static Bitmap mBitmap2;
    private static Bitmap mBitmap3;

    private static Bitmap mOriginalBitmap1;
    private static Bitmap mOriginalBitmap2;
    private static Bitmap mOriginalBitmap3;

    public static void init(SettingsHelper settings) {
        mSettings = settings;
    }

    public static void handleInitPackageResources(InitPackageResourcesParam resparam, XModuleResources modRes) {
        if (!resparam.packageName.equals(PACKAGE)) {
            return;
        }

        Drawable d1 = modRes.getDrawable(R.drawable.indicator_code_lock_point_area_default_holo);
        Drawable d2 = modRes.getDrawable(R.drawable.indicator_code_lock_point_area_green_holo);
        Drawable d3 = modRes.getDrawable(R.drawable.indicator_code_lock_point_area_red_holo);

        mBitmap1 = drawableToBitmap(d1);
        mBitmap2 = drawableToBitmap(d2);
        mBitmap3 = drawableToBitmap(d3);
    }

    private static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    public static void handleLoadPackage(final LoadPackageParam lpparam) {
        if (!lpparam.packageName.equals(PACKAGE)) {
            return;
        }

        Class<?> findClass;

        try {
            findClass = XposedHelpers.findClass(
                    USE_PACKAGE + ".widget.pattern.LgeLockPatternView",
                    lpparam.classLoader
            );
        } catch (Throwable e) {
            XposedBridge.log(e);
            XposedBridge.log("Couldn't find class");
            return;
        }

        XposedHelpers.findAndHookMethod(
                findClass,
                "drawCircle",
                "android.graphics.Canvas",
                "int",
                "int",
                "boolean",

                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (mOriginalBitmap1 == null || mOriginalBitmap2 == null || mOriginalBitmap3 == null) {
                            mOriginalBitmap1 = (Bitmap) XposedHelpers.getObjectField(param.thisObject, "mBitmapCircleDefault");
                            mOriginalBitmap2 = (Bitmap) XposedHelpers.getObjectField(param.thisObject, "mBitmapCircleGreen");
                            mOriginalBitmap3 = (Bitmap) XposedHelpers.getObjectField(param.thisObject, "mBitmapCircleRed");
                        }

                        if (mSettings.getBoolean(Prefs.ENABLE_AOSP_PATTERN_DOTS, false)) {
                            XposedHelpers.setObjectField(param.thisObject, "mBitmapCircleDefault", mBitmap1);
                            XposedHelpers.setObjectField(param.thisObject, "mBitmapCircleGreen", mBitmap2);
                            XposedHelpers.setObjectField(param.thisObject, "mBitmapCircleRed", mBitmap3);
                        } else {
                            XposedHelpers.setObjectField(param.thisObject, "mBitmapCircleDefault", mOriginalBitmap1);
                            XposedHelpers.setObjectField(param.thisObject, "mBitmapCircleGreen", mOriginalBitmap2);
                            XposedHelpers.setObjectField(param.thisObject, "mBitmapCircleRed", mOriginalBitmap3);
                        }
                    }
                }
        );
    }
}