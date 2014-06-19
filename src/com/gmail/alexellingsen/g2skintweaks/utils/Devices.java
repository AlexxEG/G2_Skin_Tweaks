package com.gmail.alexellingsen.g2skintweaks.utils;

import android.os.Build;

import java.util.Arrays;

public enum Devices {
    SPRINT, OTHER, VERIZON;

    public static Devices getDevice() {
        // Currently only Sprint version need special treatment.
        if (Build.MODEL.toUpperCase().contains("LS980")) {
            return Devices.SPRINT;
        } else if (Build.MODEL.toUpperCase().contains("VS980")) {
            return Devices.VERIZON;
        } else {
            return Devices.OTHER;
        }
    }

    /**
     * Returns whether the current device is any of the given devices.
     *
     * @param devices Devices to check for.
     * @return True if given devices contains current device.
     */
    public static boolean isAnyDevice(Devices... devices) {
        Devices current = getDevice();

        return Arrays.asList(devices).contains(current);
    }
}
