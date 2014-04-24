package com.gmail.alexellingsen.g2skintweaks;

import it.gmariotti.android.colorpicker.calendarstock.ColorPickerDialog;
import it.gmariotti.android.colorpicker.calendarstock.ColorPickerSwatch.OnColorSelectedListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private PlaceholderFragment fragment = null;
	private static SettingsHelper settings = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		settings = new SettingsHelper(this);

		if (savedInstanceState == null) {
			fragment = new PlaceholderFragment();

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
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();

		if (id == R.id.action_reset_default) {
			fragment.askResetToDefault();
			return true;
		} else if (id == R.id.action_enable_debugging) {
			item.setChecked(!item.isChecked());
			settings.putBoolean(Prefs.ENABLE_DEBUGGING, item.isChecked());
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	public static class PlaceholderFragment extends Fragment {

		private View rootView = null;

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			rootView = inflater.inflate(R.layout.fragment_main, container, false);

			setup();

			return rootView;
		}

		public void askResetToDefault() {
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
			((CheckBox) rootView.findViewById(R.id.chb_replace_switch)).setChecked(false);
			((CheckBox) rootView.findViewById(R.id.chb_square_bubble)).setChecked(false);
			((Button) rootView.findViewById(R.id.btn_square_left_color)).setBackgroundColor(Color.WHITE);
			((Button) rootView.findViewById(R.id.btn_square_right_color)).setBackgroundColor(Color.WHITE);
			((CheckBox) rootView.findViewById(R.id.chb_sms_text_color)).setChecked(false);
			((Button) rootView.findViewById(R.id.btn_sms_text_color_left)).setBackgroundColor(Color.BLACK);
			((Button) rootView.findViewById(R.id.btn_sms_text_color_right)).setBackgroundColor(Color.BLACK);
			((CheckBox) rootView.findViewById(R.id.chb_messenger_font_size)).setChecked(false);
			((CheckBox) rootView.findViewById(R.id.chb_turn_on_screen)).setChecked(true);

			// Listeners will update most preferences
			settings.putInt(Prefs.SQUARE_COLOR_LEFT, Color.WHITE);
			settings.putInt(Prefs.SQUARE_COLOR_RIGHT, Color.WHITE);
			settings.putInt(Prefs.SMS_TEXT_COLOR_LEFT, Color.BLACK);
			settings.putInt(Prefs.SMS_TEXT_COLOR_RIGHT, Color.BLACK);
			settings.putInt(Prefs.SMS_BODY_SIZE, 18);
			settings.putInt(Prefs.SMS_DATE_SIZE, 18);

			updateFontSizeButton(18, 18);

			String text = getString(R.string.reboot_notice);
			Toast.makeText(getActivity(), text, Toast.LENGTH_LONG).show();
		}

		private void setup() {
			setupReplaceSwitch();
			setupMessengerCustomization();
			setupMessengerFontSize();
			setupTurnOnScreenNewSMS();
		}

		private void setupMessengerFontSize() {
			boolean ENABLE_MESSENGER_FONT_SIZE = settings.getBoolean(Prefs.ENABLE_SMS_FONT_SIZE, false);

			final Button btnMessengerSetFontSize = (Button) rootView.findViewById(R.id.btn_messenger_set_font_size);

			int body_size = settings.getInt(Prefs.SMS_BODY_SIZE, 18);
			int date_size = settings.getInt(Prefs.SMS_DATE_SIZE, 18);
			updateFontSizeButton(body_size, date_size);

			btnMessengerSetFontSize.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					showMessengerFontSizePicker();
				}
			});

			CheckBox chbMessengerFontSize = (CheckBox) rootView.findViewById(R.id.chb_messenger_font_size);
			chbMessengerFontSize.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					btnMessengerSetFontSize.setEnabled(isChecked);
					settings.putBoolean(Prefs.ENABLE_SMS_FONT_SIZE, isChecked);
				}
			});
			chbMessengerFontSize.setChecked(ENABLE_MESSENGER_FONT_SIZE);
		}

		private void setupReplaceSwitch() {
			boolean ENABLE_REPLACE_SWITCH = settings.getBoolean(Prefs.ENABLE_REPLACE_SWICTH, false);

			CheckBox chbReplaceSwitch = (CheckBox) rootView.findViewById(R.id.chb_replace_switch);
			chbReplaceSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					settings.putBoolean(Prefs.ENABLE_REPLACE_SWICTH, isChecked);
				}
			});
			chbReplaceSwitch.setChecked(ENABLE_REPLACE_SWITCH);
		}

		private void setupMessengerCustomization() {
			boolean ENABLE_SQUARE_BUBBLE = settings.getBoolean(Prefs.ENABLE_SQUARE_BUBBLE, false);
			boolean ENABLE_SMS_TEXT_COLOR = settings.getBoolean(Prefs.ENABLE_SMS_TEXT_COLOR, false);

			final Button btnSquareLeftColor = (Button) rootView.findViewById(R.id.btn_square_left_color);
			final Button btnSquareRightColor = (Button) rootView.findViewById(R.id.btn_square_right_color);
			final Button btnSmsTextColorLeft = (Button) rootView.findViewById(R.id.btn_sms_text_color_left);
			final Button btnSmsTextColorRight = (Button) rootView.findViewById(R.id.btn_sms_text_color_right);

			btnSquareLeftColor.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					showSquareColorPicker(v, true);
				}
			});
			btnSquareRightColor.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					showSquareColorPicker(v, false);
				}
			});

			btnSquareLeftColor.setEnabled(ENABLE_SQUARE_BUBBLE);
			btnSquareRightColor.setEnabled(ENABLE_SQUARE_BUBBLE);
			btnSquareLeftColor.setBackgroundColor(settings.getInt(Prefs.SQUARE_COLOR_LEFT, Color.WHITE));
			btnSquareRightColor.setBackgroundColor(settings.getInt(Prefs.SQUARE_COLOR_RIGHT, Color.WHITE));

			btnSmsTextColorLeft.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					showSmsTextColorPicker(v, true);
				}
			});
			btnSmsTextColorLeft.setBackgroundColor(settings.getInt(Prefs.SMS_TEXT_COLOR_LEFT, Color.BLACK));

			btnSmsTextColorRight.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					showSmsTextColorPicker(v, false);
				}
			});
			btnSmsTextColorRight.setBackgroundColor(settings.getInt(Prefs.SMS_TEXT_COLOR_RIGHT, Color.BLACK));

			CheckBox chbSquareBubble = (CheckBox) rootView.findViewById(R.id.chb_square_bubble);
			chbSquareBubble.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					settings.putBoolean(Prefs.ENABLE_SQUARE_BUBBLE, isChecked);

					btnSquareLeftColor.setEnabled(isChecked);
					btnSquareRightColor.setEnabled(isChecked);
				}
			});
			chbSquareBubble.setChecked(ENABLE_SQUARE_BUBBLE);

			CheckBox chbSmsTextColor = (CheckBox) rootView.findViewById(R.id.chb_sms_text_color);
			chbSmsTextColor.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					settings.putBoolean(Prefs.ENABLE_SMS_TEXT_COLOR, isChecked);

					btnSmsTextColorLeft.setEnabled(isChecked);
					btnSmsTextColorRight.setEnabled(isChecked);
				}
			});
			chbSmsTextColor.setChecked(ENABLE_SMS_TEXT_COLOR);
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

		private void showSmsTextColorPicker(final View v, boolean left) {
			int[] mColor = getColorChoice();
			final String key = left ? Prefs.SMS_TEXT_COLOR_LEFT : Prefs.SMS_TEXT_COLOR_RIGHT;
			int mSelectedColor = settings.getInt(key, Color.BLACK);

			ColorPickerDialog colorCalendar = ColorPickerDialog.newInstance(
					R.string.color_picker_default_title,
					mColor,
					mSelectedColor,
					4,
					ColorPickerDialog.SIZE_SMALL);

			colorCalendar.setOnColorSelectedListener(new OnColorSelectedListener() {
				@Override
				public void onColorSelected(int color) {
					settings.putInt(key, color);

					v.setBackgroundColor(color);
				}
			});

			colorCalendar.show(getFragmentManager(), "cal");
		}

		private void showSquareColorPicker(final View v, boolean left) {
			int[] mColor = getColorChoice();
			final String key = left ? Prefs.SQUARE_COLOR_LEFT : Prefs.SQUARE_COLOR_RIGHT;
			int mSelectedColor = settings.getInt(key, Color.WHITE);

			ColorPickerDialog colorCalendar = ColorPickerDialog.newInstance(
					R.string.color_picker_default_title,
					mColor,
					mSelectedColor,
					4,
					ColorPickerDialog.SIZE_SMALL);

			colorCalendar.setOnColorSelectedListener(new OnColorSelectedListener() {
				@Override
				public void onColorSelected(int color) {
					settings.putInt(key, color);

					v.setBackgroundColor(color);
				}
			});

			colorCalendar.show(getFragmentManager(), "cal");
		}

		private void showMessengerFontSizePicker() {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			LayoutInflater inflater = LayoutInflater.from(getActivity());

			final View inflator = inflater.inflate(R.layout.text_size_picker, null);

			final TextView txt_sms_body_size = (TextView) inflator.findViewById(R.id.txt_sms_body_size);
			final TextView txt_sms_date_size = (TextView) inflator.findViewById(R.id.txt_sms_date_size);
			final SeekBar sms_body_size = (SeekBar) inflator.findViewById(R.id.sms_body_size);
			final SeekBar sms_date_size = (SeekBar) inflator.findViewById(R.id.sms_date_size);

			txt_sms_body_size.setText(settings.getInt(Prefs.SMS_BODY_SIZE, 18) + "");
			txt_sms_date_size.setText(settings.getInt(Prefs.SMS_DATE_SIZE, 18) + "");
			sms_body_size.setProgress(settings.getInt(Prefs.SMS_BODY_SIZE, 18) - 12);
			sms_date_size.setProgress(settings.getInt(Prefs.SMS_DATE_SIZE, 18) - 12);

			sms_body_size.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
				}

				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					int sp = progress + 12;

					txt_sms_body_size.setText(sp + "");
				}
			});

			sms_date_size.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
				}

				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					int sp = progress + 12;

					txt_sms_date_size.setText(sp + "");
				}
			});

			builder.setView(inflator)
					.setPositiveButton(getString(R.string.save), new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							int spBody = sms_body_size.getProgress() + 12;
							int spDate = sms_date_size.getProgress() + 12;

							settings.putInt(Prefs.SMS_BODY_SIZE, spBody);
							settings.putInt(Prefs.SMS_DATE_SIZE, spDate);

							updateFontSizeButton(spBody, spDate);
						}
					})
					.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
						}
					});

			AlertDialog alertDialog = builder.create();

			alertDialog.show();
		}

		private void updateFontSizeButton(int body, int date) {
			String text = getString(R.string.button_set_font_size, body, date);
			Button button = (Button) rootView.findViewById(R.id.btn_messenger_set_font_size);

			button.setText(Html.fromHtml(text));
		}
	}

}
