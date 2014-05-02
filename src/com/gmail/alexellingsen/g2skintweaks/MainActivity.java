package com.gmail.alexellingsen.g2skintweaks;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.CompoundButton.OnCheckedChangeListener;
import it.gmariotti.android.colorpicker.calendarstock.ColorPickerDialog;
import it.gmariotti.android.colorpicker.calendarstock.ColorPickerSwatch.OnColorSelectedListener;

public class MainActivity extends Activity {

    private static final int PREFERENCE_ACTIVITY = 100;

    private MainFragment fragment = null;
    private static SettingsHelper settings = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        settings = new SettingsHelper(this);

        if (savedInstanceState == null) {
            fragment = new MainFragment();

            getFragmentManager().beginTransaction().add(R.id.container, fragment).commit();
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

        if (id == R.id.action_reset_default) {
            fragment.askResetToDefault();
            return true;
        } else if (id == R.id.action_enable_debugging) {
            item.setChecked(!item.isChecked());
            settings.putBoolean(Prefs.ENABLE_DEBUGGING, item.isChecked());
            return true;
        } else if (id == R.id.action_settings) {
            Intent i = new Intent(getApplicationContext(), PrefsActivity.class);
            startActivityForResult(i, PREFERENCE_ACTIVITY);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PREFERENCE_ACTIVITY) {
            // Update text to show new preferences, if any.
            fragment.updateAfterPreferences();
        }
    }

    @SuppressWarnings("ConstantConditions")
    public static class MainFragment extends Fragment {

        private View rootView = null;

        public MainFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            rootView = inflater.inflate(R.layout.fragment_main, container, false);

            setup();

            return rootView;
        }

        private void askResetToDefault() {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            builder.setTitle(getString(R.string.are_you_sure));
            builder.setMessage(getString(R.string.confirm_reset_message));
            builder.setPositiveButton(getString(R.string.yes), new AlertDialog.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    resetToDefault();
                }
            });
            builder.setNegativeButton(getString(R.string.no), new AlertDialog.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Do nothing
                }
            });
            builder.show();
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

        public void resetToDefault() {
            View viewCustomBubbleColors = rootView.findViewById(R.id.custom_bubble_colors);
            View viewMessagesColors = rootView.findViewById(R.id.sms_text_colors);
            View viewConversationColors = rootView.findViewById(R.id.conversation_colors);

            viewCustomBubbleColors.findViewById(R.id.btn_color_left).setBackgroundColor(Color.WHITE);
            viewCustomBubbleColors.findViewById(R.id.btn_color_right).setBackgroundColor(Color.WHITE);
            viewMessagesColors.findViewById(R.id.btn_color_left).setBackgroundColor(Color.BLACK);
            viewMessagesColors.findViewById(R.id.btn_color_right).setBackgroundColor(Color.BLACK);
            viewConversationColors.findViewById(R.id.btn_color_left).setBackgroundColor(Color.BLACK);
            viewConversationColors.findViewById(R.id.btn_color_right).setBackgroundColor(Color.BLACK);
            ((CheckBox) rootView.findViewById(R.id.chb_replace_switch)).setChecked(false);
            ((CheckBox) rootView.findViewById(R.id.chb_custom_bubble)).setChecked(false);
            ((CheckBox) rootView.findViewById(R.id.chb_custom_bubble_color)).setChecked(false);
            ((CheckBox) rootView.findViewById(R.id.chb_sms_text_color)).setChecked(false);
            ((CheckBox) rootView.findViewById(R.id.chb_conversation_color)).setChecked(false);
            ((CheckBox) rootView.findViewById(R.id.chb_smaller_sms_size)).setChecked(false);
            ((CheckBox) rootView.findViewById(R.id.chb_turn_on_screen)).setChecked(true);
            ((Spinner) rootView.findViewById(R.id.spinner_bubbles)).setSelection(0);

            // Listeners will update most preferences
            settings.putInt(Prefs.BUBBLE_COLOR_LEFT, Color.WHITE);
            settings.putInt(Prefs.BUBBLE_COLOR_RIGHT, Color.WHITE);
            settings.putInt(Prefs.SMS_TEXT_COLOR_LEFT, Color.BLACK);
            settings.putInt(Prefs.SMS_TEXT_COLOR_RIGHT, Color.BLACK);
            settings.putInt(Prefs.CONVERSATION_COLOR_BOTTOM, Color.BLACK);
            settings.putInt(Prefs.CONVERSATION_COLOR_TOP, Color.BLACK);

            String text = getString(R.string.reboot_notice);
            Toast.makeText(getActivity(), text, Toast.LENGTH_LONG).show();
        }

        private void setup() {
            setupConversationColor();
            setupCustomBubble();
            setupCustomBubbleColor();
            setupLowerMinimumZoom();
            setupMessagesColor();
            setupRecentAppsOpacity();
            setupRemoveDividers();
            setupReplacementSwitch();
            setupTurnOnScreenNewSMS();
        }

        private void setupConversationColor() {
            boolean b = settings.getBoolean(Prefs.ENABLE_CONVERSATION_COLOR, false);

            View view = rootView.findViewById(R.id.conversation_colors);
            final Button top = (Button) view.findViewById(R.id.btn_color_left);
            final Button bottom = (Button) view.findViewById(R.id.btn_color_right);
            final CheckBox chbConversationColor = (CheckBox) rootView.findViewById(R.id.chb_conversation_color);

            chbConversationColor.setChecked(b);
            chbConversationColor.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    settings.putBoolean(Prefs.ENABLE_CONVERSATION_COLOR, isChecked);

                    top.setEnabled(isChecked);
                    bottom.setEnabled(isChecked);
                }
            });

            bottom.setEnabled(b);
            bottom.setBackgroundColor(settings.getInt(Prefs.CONVERSATION_COLOR_BOTTOM, Color.BLACK));
            bottom.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    showColorPicker(v, Prefs.CONVERSATION_COLOR_BOTTOM, Color.BLACK);
                }
            });

            top.setEnabled(b);
            top.setBackgroundColor(settings.getInt(Prefs.CONVERSATION_COLOR_TOP, Color.BLACK));
            top.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    showColorPicker(v, Prefs.CONVERSATION_COLOR_TOP, Color.BLACK);
                }
            });
        }

        private void setupCustomBubble() {
            boolean enableCustomBubble = settings.getBoolean(Prefs.ENABLE_CUSTOM_BUBBLE, false);

            CheckBox chbCustomBubble = (CheckBox) rootView.findViewById(R.id.chb_custom_bubble);
            Spinner spinnerBubbles = (Spinner) rootView.findViewById(R.id.spinner_bubbles);

            chbCustomBubble.setText(Html.fromHtml(getString(R.string.enable_custom_bubble)));
            chbCustomBubble.setChecked(enableCustomBubble);
            chbCustomBubble.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    settings.putBoolean(Prefs.ENABLE_CUSTOM_BUBBLE, isChecked);
                }
            });

            ArrayAdapter<String> items = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_dropdown_item);

            items.addAll(getResources().getStringArray(R.array.spinner_bubbles));

            spinnerBubbles.setAdapter(items);
            spinnerBubbles.setSelection(settings.getInt(Prefs.SELECTED_BUBBLE, 0));
            spinnerBubbles.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    settings.putInt(Prefs.SELECTED_BUBBLE, position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }

        private void setupCustomBubbleColor() {
            boolean enableCustomBubbleColor = settings.getBoolean(Prefs.ENABLE_CUSTOM_BUBBLE_COLOR, false);

            CheckBox chbCustomBubbleColor = (CheckBox) rootView.findViewById(R.id.chb_custom_bubble_color);
            View view = rootView.findViewById(R.id.custom_bubble_colors);
            final Button btnBubbleLeftColor = (Button) view.findViewById(R.id.btn_color_left);
            final Button btnBubbleRightColor = (Button) view.findViewById(R.id.btn_color_right);

            chbCustomBubbleColor.setChecked(enableCustomBubbleColor);
            chbCustomBubbleColor.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    settings.putBoolean(Prefs.ENABLE_CUSTOM_BUBBLE_COLOR, isChecked);

                    btnBubbleLeftColor.setEnabled(isChecked);
                    btnBubbleRightColor.setEnabled(isChecked);
                }
            });

            btnBubbleLeftColor.setEnabled(enableCustomBubbleColor);
            btnBubbleLeftColor.setBackgroundColor(settings.getInt(Prefs.BUBBLE_COLOR_LEFT, Color.WHITE));
            btnBubbleLeftColor.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    showColorPicker(v, Prefs.BUBBLE_COLOR_LEFT, Color.WHITE);
                }
            });

            btnBubbleRightColor.setEnabled(enableCustomBubbleColor);
            btnBubbleRightColor.setBackgroundColor(settings.getInt(Prefs.BUBBLE_COLOR_RIGHT, Color.WHITE));
            btnBubbleRightColor.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    showColorPicker(v, Prefs.BUBBLE_COLOR_RIGHT, Color.WHITE);
                }
            });
        }

        private void setupLowerMinimumZoom() {
            int minimumZoom = settings.getInt(Prefs.MINIMUM_ZOOM_LEVEL, 30);
            boolean enableSmallerSmsSize = settings.getBoolean(Prefs.ENABLE_SMALLER_SMS_SIZE, false);

            CheckBox chbMessengerFontSize = (CheckBox) rootView.findViewById(R.id.chb_smaller_sms_size);

            chbMessengerFontSize.setText(getString(R.string.enable_sms_smaller_size, minimumZoom));
            chbMessengerFontSize.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    settings.putBoolean(Prefs.ENABLE_SMALLER_SMS_SIZE, isChecked);
                }
            });
            chbMessengerFontSize.setChecked(enableSmallerSmsSize);
        }

        private void setupMessagesColor() {
            boolean enableSmsTextColor = settings.getBoolean(Prefs.ENABLE_SMS_TEXT_COLOR, false);

            View view = rootView.findViewById(R.id.sms_text_colors);
            final Button btnSmsTextColorLeft = (Button) view.findViewById(R.id.btn_color_left);
            final Button btnSmsTextColorRight = (Button) view.findViewById(R.id.btn_color_right);

            CheckBox chbSmsTextColor = (CheckBox) rootView.findViewById(R.id.chb_sms_text_color);
            chbSmsTextColor.setChecked(enableSmsTextColor);
            chbSmsTextColor.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    settings.putBoolean(Prefs.ENABLE_SMS_TEXT_COLOR, isChecked);

                    btnSmsTextColorLeft.setEnabled(isChecked);
                    btnSmsTextColorRight.setEnabled(isChecked);
                }
            });

            btnSmsTextColorLeft.setEnabled(enableSmsTextColor);
            btnSmsTextColorLeft.setBackgroundColor(settings.getInt(Prefs.SMS_TEXT_COLOR_LEFT, Color.BLACK));
            btnSmsTextColorLeft.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    showColorPicker(v, Prefs.SMS_TEXT_COLOR_LEFT, Color.BLACK);
                }
            });

            btnSmsTextColorRight.setEnabled(enableSmsTextColor);
            btnSmsTextColorRight.setBackgroundColor(settings.getInt(Prefs.SMS_TEXT_COLOR_RIGHT, Color.BLACK));
            btnSmsTextColorRight.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    showColorPicker(v, Prefs.SMS_TEXT_COLOR_RIGHT, Color.BLACK);
                }
            });
        }

        private void setupRecentAppsOpacity() {
            boolean enableOpacity = settings.getBoolean(Prefs.RECENT_APPS_CUSTOM_OPACITY, false);
            int opacity = settings.getInt(Prefs.RECENT_APPS_CUSTOM_OPACITY_VALUE, 0);

            CheckBox chbOpacity = (CheckBox) rootView.findViewById(R.id.chb_recent_apps_opacity);
            final TextView txtOpacity = (TextView) rootView.findViewById(R.id.txt_recent_apps_opacity);
            final SeekBar seekOpacity = (SeekBar) rootView.findViewById(R.id.seek_recent_apps_opacity);

            chbOpacity.setChecked(enableOpacity);
            chbOpacity.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    settings.putBoolean(Prefs.RECENT_APPS_CUSTOM_OPACITY, isChecked);
                    txtOpacity.setEnabled(isChecked);
                    seekOpacity.setEnabled(isChecked);
                }
            });

            txtOpacity.setText(opacity + "");
            txtOpacity.setEnabled(enableOpacity);

            seekOpacity.setProgress(opacity);
            seekOpacity.setEnabled(enableOpacity);
            seekOpacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    txtOpacity.setText(progress + "");
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    txtOpacity.setText(seekBar.getProgress() + "");
                    settings.putInt(Prefs.RECENT_APPS_CUSTOM_OPACITY_VALUE, seekBar.getProgress());
                }
            });
        }

        private void setupRemoveDividers() {
            boolean enableRemoveDividers = settings.getBoolean(Prefs.ENABLE_REMOVE_DIVIDERS, false);

            CheckBox chbRemoveDividers = (CheckBox) rootView.findViewById(R.id.chb_remove_dividers);
            chbRemoveDividers.setChecked(enableRemoveDividers);
            chbRemoveDividers.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    settings.putBoolean(Prefs.ENABLE_REMOVE_DIVIDERS, isChecked);
                }
            });
        }

        private void setupReplacementSwitch() {
            boolean ENABLE_REPLACE_SWITCH = settings.getBoolean(Prefs.ENABLE_REPLACE_SWITCH, false);

            CheckBox chbReplaceSwitch = (CheckBox) rootView.findViewById(R.id.chb_replace_switch);
            chbReplaceSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    settings.putBoolean(Prefs.ENABLE_REPLACE_SWITCH, isChecked);
                }
            });
            chbReplaceSwitch.setChecked(ENABLE_REPLACE_SWITCH);
        }

        private void setupTurnOnScreenNewSMS() {
            boolean enableTurnOnScreenNewSms = settings.getBoolean(Prefs.TURN_ON_SCREEN_NEW_SMS, true);
            boolean enablePowerLed = settings.getBoolean(Prefs.ENABLE_POWER_LED, true);

            final CheckBox chbTurnOnScreenNewSMS = (CheckBox) rootView.findViewById(R.id.chb_turn_on_screen);
            final CheckBox chbEnablePowerLed = (CheckBox) rootView.findViewById(R.id.chb_enable_power_led);
            final Button btnRequestRoot = (Button) rootView.findViewById(R.id.btn_request_root);

            chbTurnOnScreenNewSMS.setChecked(enableTurnOnScreenNewSms);
            chbTurnOnScreenNewSMS.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    settings.putBoolean(Prefs.TURN_ON_SCREEN_NEW_SMS, isChecked);

                    chbEnablePowerLed.setEnabled(!isChecked);
                    btnRequestRoot.setEnabled(!isChecked);
                }
            });

            chbEnablePowerLed.setChecked(enablePowerLed);
            chbEnablePowerLed.setEnabled(!enableTurnOnScreenNewSms);
            chbEnablePowerLed.setText(Html.fromHtml(getString(R.string.chb_flash_power_led)));
            chbEnablePowerLed.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    settings.putBoolean(Prefs.ENABLE_POWER_LED, isChecked);
                }
            });

            btnRequestRoot.setEnabled(!enableTurnOnScreenNewSms);
            btnRequestRoot.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    RootFunctions.requestRoot();
                }
            });
        }

        private void showColorPicker(final View v, final String key, int defaultColor) {
            int[] mColor = getColorChoice();
            int mSelectedColor = settings.getInt(key, defaultColor);

            ColorPickerDialog colorPicker = ColorPickerDialog.newInstance(
                    R.string.color_picker_default_title,
                    mColor,
                    mSelectedColor,
                    4,
                    ColorPickerDialog.SIZE_SMALL);

            colorPicker.setOnColorSelectedListener(new OnColorSelectedListener() {
                @Override
                public void onColorSelected(int color) {
                    settings.putInt(key, color);

                    v.setBackgroundColor(color);
                }
            });

            colorPicker.show(getFragmentManager(), "cal");
        }

        public void updateAfterPreferences() {
            CheckBox chbMessengerFontSize = (CheckBox) rootView.findViewById(R.id.chb_smaller_sms_size);

            int minimumZoom = settings.getInt(Prefs.MINIMUM_ZOOM_LEVEL, 30);

            chbMessengerFontSize.setText(getString(R.string.enable_sms_smaller_size, minimumZoom));
        }

    }

}
