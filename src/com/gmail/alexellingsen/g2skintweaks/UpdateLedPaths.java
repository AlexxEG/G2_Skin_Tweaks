package com.gmail.alexellingsen.g2skintweaks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class UpdateLedPaths extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SettingsHelper settings = new SettingsHelper(context);

        boolean shouldCache = !settings.getBoolean(Prefs.TURN_ON_SCREEN_NEW_SMS, true) &&
                settings.getBoolean(Prefs.ENABLE_POWER_LED, true);

        if (shouldCache) {
            // Cache the path to backlights so it wont have to search for it.
            final Context finalContext = context;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    RootFunctions.updatePowerLedPaths(finalContext);
                }
            }).start();
        }
    }

}
