/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.settings.display;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.view.Display;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/**
 * Reads and writes the per-app refresh rate overrides stored in
 * {@link Settings.System#PER_APP_REFRESH_RATE}.
 */
public final class PerAppRefreshRateUtils {

    private static final String ENTRY_SEPARATOR = ";";
    private static final String VALUE_SEPARATOR = "=";
    private static final float MIN_SUPPORTED_REFRESH_RATE = 30f;

    private PerAppRefreshRateUtils() {
    }

    /** Returns the refresh rate overrides for the current user, keyed by package name. */
    public static ArrayMap<String, Float> getRefreshRates(Context context) {
        return parse(Settings.System.getStringForUser(context.getContentResolver(),
                Settings.System.PER_APP_REFRESH_RATE, UserHandle.myUserId()));
    }

    /** Sets the refresh rate override for the app, removing it if the rate is not positive. */
    public static void setRefreshRate(Context context, String packageName, float rate) {
        final ArrayMap<String, Float> rates = getRefreshRates(context);
        if (rate > 0) {
            rates.put(packageName, rate);
        } else {
            rates.remove(packageName);
        }
        Settings.System.putStringForUser(context.getContentResolver(),
                Settings.System.PER_APP_REFRESH_RATE, serialize(rates), UserHandle.myUserId());
    }

    /**
     * Returns the refresh rates the default display can run at, sorted from lowest to highest.
     */
    public static List<Float> getSupportedRefreshRates(Context context) {
        final TreeSet<Float> rates = new TreeSet<>();
        final DisplayManager displayManager = context.getSystemService(DisplayManager.class);
        final Display display =
                displayManager != null ? displayManager.getDisplay(Display.DEFAULT_DISPLAY) : null;
        if (display != null) {
            final Display.Mode currentMode = display.getMode();
            for (Display.Mode mode : display.getSupportedModes()) {
                if (mode.getPhysicalWidth() == currentMode.getPhysicalWidth()
                        && mode.getPhysicalHeight() == currentMode.getPhysicalHeight()
                        && mode.getRefreshRate() >= MIN_SUPPORTED_REFRESH_RATE) {
                    rates.add(mode.getRefreshRate());
                }
            }
        }
        return new ArrayList<>(rates);
    }

    /** Formats a refresh rate for display, for example {@code 120}. */
    public static String formatRefreshRate(float rate) {
        return String.format(Locale.US, "%.2f", rate).replaceAll("[\\.,]00$", "");
    }

    private static ArrayMap<String, Float> parse(String value) {
        final ArrayMap<String, Float> rates = new ArrayMap<>();
        if (TextUtils.isEmpty(value)) {
            return rates;
        }
        for (String entry : value.split(ENTRY_SEPARATOR)) {
            final int separator = entry.indexOf(VALUE_SEPARATOR);
            if (separator <= 0 || separator == entry.length() - 1) {
                continue;
            }
            try {
                final float rate = Float.parseFloat(entry.substring(separator + 1));
                if (rate > 0) {
                    rates.put(entry.substring(0, separator), rate);
                }
            } catch (NumberFormatException e) {
                // Skip malformed entries.
            }
        }
        return rates;
    }

    private static String serialize(ArrayMap<String, Float> rates) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < rates.size(); i++) {
            if (builder.length() > 0) {
                builder.append(ENTRY_SEPARATOR);
            }
            builder.append(rates.keyAt(i)).append(VALUE_SEPARATOR).append(rates.valueAt(i));
        }
        return builder.toString();
    }
}
