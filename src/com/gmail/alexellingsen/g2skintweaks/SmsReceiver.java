package com.gmail.alexellingsen.g2skintweaks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import com.gmail.alexellingsen.g2skintweaks.utils.SettingsHelper;

public class SmsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (isScreenOn(context)) {
            return;
        }

        SettingsHelper settings = new SettingsHelper(context);

        boolean shouldFlash = settings.getBoolean(Prefs.DONT_TURN_SCREEN_ON_SMS, false) &&
                settings.getBoolean(Prefs.ENABLE_POWER_LED, true);

        if (shouldFlash) {
            // Flash rear power led.
            RootFunctions.flashRearPowerLed(context, settings.getInt(Prefs.POWER_LED_TIME, 1000));
        }
    }

    private boolean isScreenOn(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        return powerManager.isScreenOn();
    }

}
