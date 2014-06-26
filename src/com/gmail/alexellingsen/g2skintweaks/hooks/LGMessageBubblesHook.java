package com.gmail.alexellingsen.g2skintweaks.hooks;

import android.content.res.XModuleResources;
import android.content.res.XResForwarder;
import android.content.res.XResources;
import com.gmail.alexellingsen.g2skintweaks.Prefs;
import com.gmail.alexellingsen.g2skintweaks.R;
import com.gmail.alexellingsen.g2skintweaks.utils.SettingsHelper;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;

public class LGMessageBubblesHook {

    private static final String[] LEFT_BUBBLES = new String[]{
            "bubble_inbox_bg_01",
            "bubble_inbox_bg_02",
            "bubble_inbox_bg_03",
            "bubble_inbox_bg_04",
            "bubble_inbox_bg_05",
            "bubble_inbox_bg_06"
    };
    private static final String[] RIGHT_BUBBLES = new String[]{
            "bubble_outbox_bg_01",
            "bubble_outbox_bg_02",
            "bubble_outbox_bg_03",
            "bubble_outbox_bg_04",
            "bubble_outbox_bg_05",
            "bubble_outbox_bg_06",
            "bubble_reserved_bg_01",
            "bubble_reserved_bg_02",
            "bubble_reserved_bg_03",
            "bubble_reserved_bg_04",
            "bubble_reserved_bg_05",
            "bubble_reserved_bg_06"
    };
    private static final String[] MESSAGE_SET_BUBBLES = new String[]{
            "message_set_bubble_01",
            "message_set_bubble_02",
            "message_set_bubble_03",
            "message_set_bubble_04",
            "message_set_bubble_05",
            "message_set_bubble_06"
    };

    private static final String PACKAGE_NAME = "com.android.mms";

    private static SettingsHelper mSettings;
    private static XModuleResources mModRes;
    private static XResources mResources;

    public static void init(SettingsHelper settings) {
        mSettings = settings;
    }

    public static void handleInitPackageResources(InitPackageResourcesParam resparam, XModuleResources modRes) {
        if (!resparam.packageName.equals(PACKAGE_NAME)) {
            return;
        }

        boolean enableCustomBubble = mSettings.getBoolean(Prefs.ENABLE_CUSTOM_BUBBLE, false);

        if (!enableCustomBubble) {
            return;
        }

        mModRes = modRes;
        mResources = resparam.res;

        for (String b : LEFT_BUBBLES) {
            handleBubble(b, true);
        }

        for (String b : RIGHT_BUBBLES) {
            handleBubble(b, false);
        }

        for (String b : MESSAGE_SET_BUBBLES) {
            handleBubbleSet(b);
        }
    }

    private static void handleBubble(String bubble, boolean left) {
        int selectedBubble = getSelectedBubbleIndex(bubble);

        if (selectedBubble == 0)
            return; // Default bubble, do nothing.

        mResources.setReplacement(PACKAGE_NAME, "drawable", bubble, getBubble(selectedBubble, left));
    }

    private static void handleBubbleSet(String bubble) {
        int selectedBubble = getSelectedBubbleIndex(bubble);

        if (selectedBubble == 0)
            return; // Default bubble, do nothing.

        mResources.setReplacement(PACKAGE_NAME, "drawable", bubble, getBubbleSet(selectedBubble));
    }

    private static XResForwarder getBubble(int index, boolean left) {
        int id;

        switch (index) {
            case 1:
                id = left ? R.drawable.balloon_bg_04_left_normal : R.drawable.balloon_bg_04_right_normal;
                break;
            case 2:
                id = left ? R.drawable.hangouts_balloon_left : R.drawable.hangouts_balloon_right;
                break;
            default:
                return null;
        }

        return mModRes.fwd(id);
    }

    private static XResForwarder getBubbleSet(int index) {
        int id;

        switch (index) {
            case 1:
                id = R.drawable.message_set_bubble_04;
                break;
            case 2:
                id = R.drawable.message_set_hangouts;
                break;
            default:
                return null;
        }

        return mModRes.fwd(id);
    }

    private static int getSelectedBubbleIndex(String bubble) {
        // Get last character to get bubble index
        int i = Integer.parseInt(bubble.substring(bubble.length() - 1));

        // Take any of the keys & replace the last character with our index
        String key = Prefs.CUSTOM_BUBBLE_1.substring(0, Prefs.CUSTOM_BUBBLE_1.length() - 1) + i;

        // Use our new key to get the correct bubble
        return Integer.parseInt(mSettings.getString(key, "0"));
    }
}
