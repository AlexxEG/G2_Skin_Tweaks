package com.gmail.alexellingsen.g2skintweaks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.gmail.alexellingsen.g2skintweaks.utils.SettingsHelper;

public class UpdateLedPaths extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        final SettingsHelper settings = new SettingsHelper(context);

        boolean shouldCache = settings.getBoolean(Prefs.DONT_TURN_SCREEN_ON_SMS, false) &&
                settings.getBoolean(Prefs.ENABLE_POWER_LED, true);

        if (shouldCache) {
            // Cache the path to backlights so it wont have to search for it.
            final Context finalContext = context;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    RootFunctions.updatePowerLedPaths(finalContext);
                    Log.e(G2SkinTweaks.TAG, "Updated LED paths");
                }
            }).start();
        }
    }

}
