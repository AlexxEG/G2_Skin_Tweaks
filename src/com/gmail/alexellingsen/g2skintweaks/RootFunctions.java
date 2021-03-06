package com.gmail.alexellingsen.g2skintweaks;

import android.content.Context;
import android.util.Log;
import com.gmail.alexellingsen.g2skintweaks.utils.SettingsHelper;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class RootFunctions {

    private static String execute(List<String> cmds) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec("su");
        DataOutputStream os = new DataOutputStream(process.getOutputStream());

        if (cmds != null) {
            for (String tmpCmd : cmds) {
                os.writeBytes(tmpCmd + "\n");
            }
        }

        os.writeBytes("exit\n");
        os.flush();

        String input = readToEnd(process.getInputStream());

        os.close();

        process.waitFor();

        return input;
    }

    private static boolean filesExists(String file1, String file2) {
        ArrayList<String> cmds = new ArrayList<String>();

        cmds.add("[[ -f " + file1 + " && -f " + file2 + " ]] && echo \"Found\" || echo \"Not found\"");

        try {
            String result = execute(cmds);

            if (result.trim().equals("Found")) {
                return true;
            } else if (result.trim().equals("Not found")) {
                return false;
            } else {
                throw new Exception("Weird result: " + result);
            }
        } catch (Exception e) {
            Log.e(G2SkinTweaks.TAG, "Error", e);
        }

        return false;
    }

    public static void flashRearPowerLed(final Context context, final int duration) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                turnOnRearPowerLed(context);
                try {
                    Thread.sleep(duration);
                } catch (InterruptedException e) {
                    Log.e(G2SkinTweaks.TAG, "Error", e);
                }
                turnOffRearPowerLed(context);
            }
        }).start();
    }

    private static String readToEnd(InputStream stream) {
        StringBuilder sb = new StringBuilder();

        int b;
        DataInputStream is = new DataInputStream(stream);

        try {
            while ((b = is.read()) != -1) {
                sb.append((char) b);
            }
        } catch (IOException e) {
            Log.e(G2SkinTweaks.TAG, "Error", e);
        }

        return sb.toString();
    }

    public static void requestRoot() {
        try {
            execute(null);
        } catch (Exception e) {
            Log.e(G2SkinTweaks.TAG, "Error", e);
        }
    }

    public static void softReboot() {
        ArrayList<String> cmds = new ArrayList<String>();

        cmds.add("setprop ctl.restart surfaceflinger; setprop ctl.restart zygote");

        try {
            execute(cmds);
        } catch (Throwable e) {
            Log.e(G2SkinTweaks.TAG, "Error", e);
        }
    }

    public static void turnOffRearPowerLed(Context context) {
        ArrayList<String> cmds = new ArrayList<String>();

        SettingsHelper settings = new SettingsHelper(context);

        String backlight1 = settings.getString(Prefs.CACHED_BACKLIGHT1_PATH, "");
        String backlight2 = settings.getString(Prefs.CACHED_BACKLIGHT2_PATH, "");

        if (!filesExists(backlight1, backlight2)) {
            updatePowerLedPaths(context);

            backlight1 = settings.getString(Prefs.CACHED_BACKLIGHT1_PATH, "");
            backlight2 = settings.getString(Prefs.CACHED_BACKLIGHT2_PATH, "");
        }

        cmds.add("rear_left=\"" + backlight1 + "\"");
        cmds.add("rear_right=\"" + backlight2 + "\"");
        cmds.add("echo 0 > $rear_left");
        cmds.add("echo 0 > $rear_right");

        try {
            execute(cmds);
        } catch (Exception e) {
            Log.e(G2SkinTweaks.TAG, "Error", e);
        }
    }

    public static void turnOnRearPowerLed(Context context) {
        ArrayList<String> cmds = new ArrayList<String>();

        SettingsHelper settings = new SettingsHelper(context);

        String backlight1 = settings.getString(Prefs.CACHED_BACKLIGHT1_PATH, "");
        String backlight2 = settings.getString(Prefs.CACHED_BACKLIGHT2_PATH, "");

        if (!filesExists(backlight1, backlight2)) {
            updatePowerLedPaths(context);

            backlight1 = settings.getString(Prefs.CACHED_BACKLIGHT1_PATH, "");
            backlight2 = settings.getString(Prefs.CACHED_BACKLIGHT2_PATH, "");
        }

        cmds.add("rear_left=\"" + backlight1 + "\"");
        cmds.add("rear_right=\"" + backlight2 + "\"");
        cmds.add("echo 71 > $rear_left");
        cmds.add("echo 71 > $rear_right");

        try {
            execute(cmds);
        } catch (Exception e) {
            Log.e(G2SkinTweaks.TAG, "Error", e);
        }
    }

    public static void updatePowerLedPaths(Context context) {
        ArrayList<String> cmds = new ArrayList<String>();

        Log.d(G2SkinTweaks.TAG, "Updating cached paths");

        cmds.add("find_left=$(find /sys/devices -type d -name button-backlight1)");
        cmds.add("find_right=$(find /sys/devices -type d -name button-backlight2)");
        cmds.add("rear_left=\"/brightness\"");
        cmds.add("rear_right=\"/brightness\"");
        cmds.add("echo $find_left$rear_left");
        cmds.add("echo $find_right$rear_right");

        try {
            String[] paths = execute(cmds).split("\\r?\\n");

            SettingsHelper settings = new SettingsHelper(context);

            settings.putString(Prefs.CACHED_BACKLIGHT1_PATH, paths[0]);
            settings.putString(Prefs.CACHED_BACKLIGHT2_PATH, paths[1]);
        } catch (Exception e) {
            Log.e(G2SkinTweaks.TAG, "Error", e);
        }
    }

    public static void reboot() {
        ArrayList<String> cmds = new ArrayList<String>();

        cmds.add("reboot");

        try {
            execute(cmds);
        } catch (Throwable e) {
            Log.e(G2SkinTweaks.TAG, "Error", e);
        }
    }
}
