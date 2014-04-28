package com.gmail.alexellingsen.g2skintweaks.utils;

import android.os.Build;

public enum Devices {
    SPRINT, OTHER;

    public static Devices getDevice() {
        // Currently only Sprint version need special treatment.
        if (Build.MODEL == "LG-LS980") {
            return Devices.SPRINT;
        } else {
            return Devices.OTHER;
        }
    }
}
