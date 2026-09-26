/*
 * Copyright (C) 2026 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.fuelgauge.bypasscharge;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.settings.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Helper around the OPPO/OnePlus "bypass charging" (internally called PLC)
 * charger kernel interface.
 *
 * <p>The charger driver exposes a small text node:
 *
 * <pre>
 *     /sys/class/oplus_chg/common/plc
 *
 *     switch=1|callname=N   route system power through the wired charger
 *     switch=0|callname=N   resume normal charging
 * </pre>
 *
 * <p>The node reports the live state as {@code status=N}, see the
 * {@code STATUS_*} constants below. The user intent is stored separately in
 * Settings.System, because the kernel drops PLC when the charger is removed or
 * when the negotiated charging protocol changes.
 *
 * <p>Only the {@code switch} parameter is used on purpose. The {@code buck} and
 * the BYB output voltage controls are left untouched so the kernel keeps
 * managing the actual power path and its voltage/thermal protection.
 */
public final class BypassChargeManager {

    private static final String TAG = "BypassCharge";

    /** Node reported status values, from the charger driver (PLC_STATUS_*). */
    public static final int STATUS_UNKNOWN = -1;
    public static final int STATUS_NOT_ALLOWED = 1;
    public static final int STATUS_DISABLED = 2;
    public static final int STATUS_ENABLED = 3;
    public static final int STATUS_WAITING = 4;

    /** Settings.System key that stores the user's intent (0 = off, 1 = on). */
    private static final String KEY_BYPASS_CHARGE_ENABLED = "bypass_charge_enabled";

    /*
     * Value written as "callname". The charger driver only logs it and never
     * interprets it, so any stable, non-negative identifier works.
     */
    private static final int CALLER_ID = 0;

    private BypassChargeManager() {
    }

    /** Whether the device exposes a usable bypass charging node. */
    public static boolean isSupported(@NonNull Context context) {
        return context.getResources().getBoolean(R.bool.config_bypassChargeSupported)
                && getSysfsFile(context).exists();
    }

    /** Whether the user asked for bypass charging. */
    public static boolean isEnabled(@NonNull Context context) {
        try {
            return Settings.System.getInt(context.getContentResolver(),
                    KEY_BYPASS_CHARGE_ENABLED, 0) == 1;
        } catch (RuntimeException e) {
            Log.w(TAG, "Unable to read the bypass charging state", e);
            return false;
        }
    }

    /**
     * Applies the requested state to the charger driver and records the user
     * intent.
     *
     * <p>Disabling always updates the stored intent so the UI cannot get stuck
     * in the "on" state if the node disappears (for example after a vendor
     * update that changes the charger driver).
     *
     * @return {@code true} when the charger node accepted the command
     */
    public static boolean setEnabled(@NonNull Context context, boolean enabled) {
        final String command = "switch=" + (enabled ? 1 : 0) + "|callname=" + CALLER_ID;
        final boolean written = writeCommand(getSysfsFile(context), command);
        if (written || !enabled) {
            storeState(context, enabled);
        }
        return written;
    }

    /** Whether a wired (AC or USB) charger is currently connected. */
    public static boolean isWiredChargerConnected(@NonNull Context context) {
        final Intent intent = context.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (intent == null) {
            return false;
        }
        final int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        return plugged == BatteryManager.BATTERY_PLUGGED_AC
                || plugged == BatteryManager.BATTERY_PLUGGED_USB;
    }

    /**
     * Reads the live PLC state from the charger driver.
     *
     * @return one of the {@code STATUS_*} constants, or {@link #STATUS_UNKNOWN}
     *         when the node cannot be read
     */
    public static int getStatus(@NonNull Context context) {
        final File node = getSysfsFile(context);
        try (BufferedReader reader = new BufferedReader(new FileReader(node))) {
            final String line = reader.readLine();
            if (line != null && line.startsWith("status=")) {
                return Integer.parseInt(line.substring("status=".length()).trim());
            }
        } catch (IOException | NumberFormatException | SecurityException e) {
            Log.w(TAG, "Unable to read the bypass charging status", e);
        }
        return STATUS_UNKNOWN;
    }

    private static void storeState(@NonNull Context context, boolean enabled) {
        try {
            Settings.System.putInt(context.getContentResolver(),
                    KEY_BYPASS_CHARGE_ENABLED, enabled ? 1 : 0);
        } catch (RuntimeException e) {
            Log.w(TAG, "Unable to store the bypass charging state", e);
        }
    }

    @NonNull
    private static File getSysfsFile(@NonNull Context context) {
        return new File(context.getResources().getString(R.string.config_bypassChargeSysfsPath));
    }

    private static boolean writeCommand(@NonNull File node, @NonNull String command) {
        try (FileOutputStream out = new FileOutputStream(node)) {
            out.write(command.getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (IOException | SecurityException e) {
            Log.w(TAG, "Failed to write \"" + command + "\" to " + node, e);
            return false;
        }
    }
}
