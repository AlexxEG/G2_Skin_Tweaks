package com.gmail.alexellingsen.g2skintweaks;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.*;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.gmail.alexellingsen.g2skintweaks.preference.PreviewColorPreference;
import com.gmail.alexellingsen.g2skintweaks.preference.PreviewImagePreference;
import com.gmail.alexellingsen.g2skintweaks.utils.SettingsHelper;
import it.gmariotti.android.colorpicker.calendarstock.ColorPickerDialog;
import it.gmariotti.android.colorpicker.calendarstock.ColorPickerSwatch;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class MainActivity extends PreferenceActivity {

    private static final int CROP_IMAGE = 112;
    private static final int PICK_IMAGE = 111;
    private static final String INSTALL_SHORTCUT_ACTION = "com.android.launcher.action.INSTALL_SHORTCUT";
    private static final String XPOSED_INSTALLER_PACKAGE = "de.robv.android.xposed.installer";

    private MainFragment mainFragment;
    private static SettingsHelper settings = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settings = new SettingsHelper(this);

        mainFragment = new MainFragment();

        getFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, mainFragment)
                .commit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK)
            return;

        if (requestCode == PICK_IMAGE) {
            cropImage(this, data.getData());
        } else if (requestCode == CROP_IMAGE) {
            mainFragment.updateBackgroundImage();
            Toast.makeText(this, getString(R.string.set_background_complete), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean debugging = settings.getBoolean(Prefs.ENABLE_DEBUGGING, false);

        menu.findItem(R.id.action_enable_debugging).setChecked(debugging);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_create_shortcut) {
            createHomeShortcut();
            return true;
        } else if (id == R.id.action_enable_debugging) {
            item.setChecked(!item.isChecked());
            settings.putBoolean(Prefs.ENABLE_DEBUGGING, item.isChecked());
            return true;
        } else if (id == R.id.action_settings) {
            Intent i = new Intent(getApplicationContext(), PrefsActivity.class);
            startActivity(i);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void createHomeShortcut() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);

        intent.setAction(Intent.ACTION_MAIN);

        Intent addIntent = new Intent();
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.app_name));
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(getApplicationContext(),
                        R.drawable.ic_launcher)
        );

        addIntent.setAction(INSTALL_SHORTCUT_ACTION);
        getApplicationContext().sendBroadcast(addIntent);
    }

    private static void pickImage(Activity activity) {
        if (!prepareFolder()) {
            Toast.makeText(activity, "Couldn't create folder.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        activity.startActivityForResult(Intent.createChooser(intent, "Complete action using"), PICK_IMAGE);
    }

    private static void cropImage(Activity activity, Uri data) {
        Intent intent = new Intent("com.android.camera.action.CROP");

        intent.setData(data);
        intent.putExtra("outputX", 1080);
        intent.putExtra("outputY", 1584);
        intent.putExtra("aspectX", 15);
        intent.putExtra("aspectY", 22);
        intent.putExtra("scale", true);

        Uri path = Uri.fromFile(new File(Environment.getExternalStorageDirectory().toString() +
                "/G2SkinTweaks/background.png"));

        intent.putExtra("output", path);
        activity.startActivityForResult(intent, CROP_IMAGE);
    }

    private static boolean prepareFolder() {
        File folder = new File(Environment.getExternalStorageDirectory(), "G2SkinTweaks");

        if (!folder.mkdir()) {
            try {
                folder.mkdir();
            } catch (Throwable e) {
                e.printStackTrace();
                return false;
            }
        }

        File noMediaFile = new File(folder, ".nomedia");

        if (!noMediaFile.exists()) {
            try {
                noMediaFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }

    public static class MainFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

        public MainFragment() {
        }

        @SuppressWarnings("deprecation")
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            PreferenceManager prefMgr = getPreferenceManager();
            prefMgr.setSharedPreferencesName(Prefs.NAME);
            prefMgr.setSharedPreferencesMode(MODE_WORLD_READABLE);

            addPreferencesFromResource(R.xml.preferences_main);

            setup();
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        }

        @Override
        public void onPause() {
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            super.onPause();
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.startsWith("pref_custom_bubble_")) {
                // Display the select bubble in summary
                String[] keys = new String[]{Prefs.CUSTOM_BUBBLE_1, Prefs.CUSTOM_BUBBLE_2, Prefs.CUSTOM_BUBBLE_3,
                        Prefs.CUSTOM_BUBBLE_4, Prefs.CUSTOM_BUBBLE_5, Prefs.CUSTOM_BUBBLE_6};

                if (Arrays.asList(keys).contains(key)) {
                    String[] entries = getResources().getStringArray(R.array.custom_bubbles);

                    findPreference(key).setSummary(entries[Integer.parseInt(settings.getString(key, "0"))]);
                }
            }
        }

        private final String CONVERSATION_LIST_BG = "set_conversation_list_bg";
        private final String REBOOT = "reboot";
        private final String REQUEST_ROOT = "request_root";
        private final String SOFT_REBOOT = "soft_reboot";
        private final String XPOSED_INSTALLER = "shortcut_xposed_installer";

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
            if (preference == null || preference.getKey() == null)
                return super.onPreferenceTreeClick(preferenceScreen, preference);

            if (preference instanceof PreviewColorPreference) {
                showColorPicker((PreviewColorPreference) preference);
            } else if (preference.getKey().equals(REQUEST_ROOT)) {
                RootFunctions.requestRoot();
            } else if (preference.getKey().equals(REBOOT)) {
                RootFunctions.reboot();
            } else if (preference.getKey().equals(SOFT_REBOOT)) {
                RootFunctions.softReboot();
            } else if (preference.getKey().equals(XPOSED_INSTALLER)) {
                Intent intent = getActivity().getPackageManager().getLaunchIntentForPackage(XPOSED_INSTALLER_PACKAGE);

                if (intent == null) {
                    Toast.makeText(getActivity(),
                            getString(R.string.xposed_installer_not_found), Toast.LENGTH_SHORT).show();
                } else {
                    startActivity(intent);
                }
            }

            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        private int[] getColorChoice() {
            int[] mColorChoices = null;
            String[] color_array = getResources().getStringArray(R.array.default_color_choice_values);

            if (color_array != null && color_array.length > 0) {
                mColorChoices = new int[color_array.length];
                for (int i = 0; i < color_array.length; i++) {
                    mColorChoices[i] = Color.parseColor(color_array[i]);
                }
            }
            return mColorChoices;
        }

        private Drawable getConversationListBackground() {
            File folder = new File(Environment.getExternalStorageDirectory(), "G2SkinTweaks");

            if (!folder.exists()) {
                return null;
            }

            File file = new File(folder, "background.png");

            if (!file.exists()) {
                return null;
            }

            return Drawable.createFromPath(file.getPath());
        }

        private void setup() {
            String[] entries = getResources().getStringArray(R.array.custom_bubbles);
            String[] keys = new String[]{Prefs.CUSTOM_BUBBLE_1, Prefs.CUSTOM_BUBBLE_2, Prefs.CUSTOM_BUBBLE_3,
                    Prefs.CUSTOM_BUBBLE_4, Prefs.CUSTOM_BUBBLE_5, Prefs.CUSTOM_BUBBLE_6};

            for (String key : keys) {
                findPreference(key).setSummary(entries[Integer.parseInt(settings.getString(key, "0"))]);
            }

            updateBackgroundImage();
        }

        private void showColorPicker(final PreviewColorPreference preference) {
            int[] mColor = getColorChoice();
            int mSelectedColor = settings.getInt(preference.getKey(), Color.BLACK);

            ColorPickerDialog colorPicker = ColorPickerDialog.newInstance(
                    R.string.color_picker_default_title,
                    mColor,
                    mSelectedColor,
                    4,
                    ColorPickerDialog.SIZE_SMALL);

            colorPicker.setOnColorSelectedListener(new ColorPickerSwatch.OnColorSelectedListener() {
                @Override
                public void onColorSelected(int color) {
                    settings.putInt(preference.getKey(), color);

                    preference.setColor(color);
                }
            });

            colorPicker.show(getFragmentManager(), "cal");
        }

        public void updateBackgroundImage() {
            PreviewImagePreference preference = (PreviewImagePreference) findPreference(CONVERSATION_LIST_BG);

            Drawable drawable = getConversationListBackground();

            preference.setDrawable(drawable);
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    pickImage(getActivity());
                    return true;
                }
            });
        }
    }

}
