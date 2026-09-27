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
import android.content.Intent;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

/**
 * Preference for enabling lock screen weather.
 */
public class LockscreenWeatherPreferenceController extends TogglePreferenceController {

    private static final String SETTING_KEY = Settings.System.LOCKSCREEN_WEATHER_ENABLED;
    private static final String WEATHER_PACKAGE = "org.omnirom.omnijaws";
    private static final String WEATHER_SETTINGS_CLASS =
            "org.omnirom.omnijaws.SettingsActivity";

    public LockscreenWeatherPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getIntForUser(
                mContext.getContentResolver(),
                SETTING_KEY,
                0,
                UserHandle.USER_CURRENT) != 0;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.System.putIntForUser(
                mContext.getContentResolver(),
                SETTING_KEY,
                isChecked ? 1 : 0,
                UserHandle.USER_CURRENT);
    }

    @Override
    public CharSequence getSummary() {
        return mContext.getText(isWeatherServiceAvailable()
                ? R.string.lockscreen_weather_summary
                : R.string.lockscreen_weather_enabled_info);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setEnabled(getAvailabilityStatus() == AVAILABLE);
        refreshSummary(preference);
    }

    @Override
    public int getAvailabilityStatus() {
        return isWeatherServiceAvailable() ? AVAILABLE : DISABLED_DEPENDENT_SETTING;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_display;
    }

    private boolean isWeatherServiceAvailable() {
        final Intent intent = new Intent(Intent.ACTION_MAIN)
                .setClassName(WEATHER_PACKAGE, WEATHER_SETTINGS_CLASS);
        return intent.resolveActivity(mContext.getPackageManager()) != null;
    }
}
