package com.gmail.alexellingsen.g2skintweaks;

import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.graphics.Color;
import com.gmail.alexellingsen.g2skintweaks.hooks.*;
import com.gmail.alexellingsen.g2skintweaks.utils.Devices;
import com.gmail.alexellingsen.g2skintweaks.utils.SettingsHelper;
import de.robv.android.xposed.*;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class G2SkinTweaks implements IXposedHookZygoteInit, IXposedHookLoadPackage, IXposedHookInitPackageResources {

    private static String MODULE_PATH = null;
    private static SettingsHelper mSettings;

    public static void log(String text) {
        boolean debug = false;

        if (mSettings != null) {
            debug = mSettings.getBoolean(Prefs.ENABLE_DEBUGGING, false);
        }

        if (debug) {
            XposedBridge.log(String.format("G2SkinTweaks: %s", text));
        }
    }

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        MODULE_PATH = startupParam.modulePath;
        mSettings = new SettingsHelper();

        boolean enableReplaceSwitch = mSettings.getBoolean(Prefs.ENABLE_REPLACE_SWITCH, false);

        if (enableReplaceSwitch) {
            String packageName = "com.lge.internal";
            XModuleResources modRes = XModuleResources.createInstance(MODULE_PATH, null);

            XResources.setSystemWideReplacement(packageName, "drawable", "switch_track_holo_dark", modRes.fwd(R.drawable.replacement_switch));
            XResources.setSystemWideReplacement(packageName, "drawable", "switch_track_holo_light", modRes.fwd(R.drawable.replacement_switch));
        }

        LGHomeHook.init(mSettings);
        LGLockScreenHook.init(mSettings);
        LGMessageHook.init(mSettings);
        LGMessageBubblesHook.init(mSettings);
        LGSettings.init(mSettings);
        RecentAppsHook.init(mSettings);
        StatusBarHook.init(mSettings);
    }

    @Override
    public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {
        XModuleResources modRes = XModuleResources.createInstance(MODULE_PATH, resparam.res);

        LGHomeHook.handleInitPackageResources(resparam);
        LGLockScreenHook.handleInitPackageResources(resparam, modRes);
        LGMessageBubblesHook.handleInitPackageResources(resparam, modRes);
        LGMessageHook.handleInitPackageResources(resparam);
        LGSettings.handleInitPackageResources(resparam);
        RecentAppsHook.handleInitPackageResources(resparam);
        StatusBarHook.handleInitPackageResources(resparam, modRes);
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        LGHomeHook.handleLoadPackage(lpparam);
        LGLockScreenHook.handleLoadPackage(lpparam);
        LGMessageHook.handleLoadPackage(lpparam);
        RecentAppsHook.handleLoadPackage(lpparam);
        StatusBarHook.handleLoadPackage(lpparam);

        if (Devices.isAnyDevice(Devices.SPRINT, Devices.VERIZON)) {
            hookPaintSetColorSprint(lpparam);
        }
    }

    private void hookPaintSetColorSprint(LoadPackageParam lpparam) {
        final Class<?> findClass;

        try {
            findClass = XposedHelpers.findClass(
                    "android.graphics.Paint",
                    lpparam.classLoader);
        } catch (ClassNotFoundError e) {
            return;
        }

        /*Has to hook this method since code can't be injected in middle of a method.
          Sprint's Messenger app draws top & bottom line in same method, unlike international version,
          therefore normally only allowing one color.*/

        XposedHelpers.findAndHookMethod(
                findClass,
                "setColor",
                "int",

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