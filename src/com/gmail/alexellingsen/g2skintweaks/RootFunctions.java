package com.gmail.alexellingsen.g2skintweaks;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XposedBridge;

public class RootFunctions {

	public static void flashRearPowerLed(final int duration) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				turnOnRearPowerLed();
				try {
					Thread.sleep(duration);
				} catch (InterruptedException e) {
					XposedBridge.log(e);
				}
				turnOffRearPowerLed();
			}
		}).start();
	}

	private static void runCmds(List<String> cmds) throws IOException, InterruptedException {
		Process process = Runtime.getRuntime().exec("su");
		DataOutputStream os = new DataOutputStream(process.getOutputStream());

		for (String tmpCmd : cmds) {
			os.writeBytes(tmpCmd + "\n");
		}

		os.writeBytes("exit\n");
		os.flush();
		os.close();

		process.waitFor();
	}

	public static void turnOffRearPowerLed() {
		ArrayList<String> cmds = new ArrayList<String>();

		cmds.add("rear_left=\"/sys/devices/leds-qpnp-d8157c00/leds/button-backlight1/brightness\"");
		cmds.add("rear_right=\"/sys/devices/leds-qpnp-d8157c00/leds/button-backlight2/brightness\"");
		cmds.add("echo 0 > $rear_left");
		cmds.add("echo 0 > $rear_right");

		try {
			runCmds(cmds);
		} catch (Exception e) {
			XposedBridge.log(e);
		}
	}

	public static void turnOnRearPowerLed() {
		ArrayList<String> cmds = new ArrayList<String>();

		cmds.add("rear_left=\"/sys/devices/leds-qpnp-d8157c00/leds/button-backlight1/brightness\"");
		cmds.add("rear_right=\"/sys/devices/leds-qpnp-d8157c00/leds/button-backlight2/brightness\"");
		cmds.add("echo 71 > $rear_left");
		cmds.add("echo 71 > $rear_right");

		try {
			runCmds(cmds);
		} catch (Exception e) {
			XposedBridge.log(e);
		}
	}

}
