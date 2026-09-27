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
import android.os.UserHandle;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

/**
 * Controller for lock screen weather detail toggles.
 */
public class LockscreenWeatherExtraPreferenceController extends TogglePreferenceController {

    public LockscreenWeatherExtraPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getIntForUser(
                mContext.getContentResolver(),
                getSettingName(),
                getDefaultValue(),
                UserHandle.USER_CURRENT) != 0;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.System.putIntForUser(
                mContext.getContentResolver(),
                getSettingName(),
                isChecked ? 1 : 0,
                UserHandle.USER_CURRENT);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_display;
    }

    private String getSettingName() {
        final String key = getPreferenceKey();
        if ("lockscreen_weather_location".equals(key)) {
            return Settings.System.LOCKSCREEN_WEATHER_LOCATION;
        } else if ("lockscreen_weather_text".equals(key)) {
            return Settings.System.LOCKSCREEN_WEATHER_TEXT;
        } else if ("lockscreen_weather_wind_info".equals(key)) {
            return Settings.System.LOCKSCREEN_WEATHER_WIND_INFO;
        } else if ("lockscreen_weather_humidity_info".equals(key)) {
            return Settings.System.LOCKSCREEN_WEATHER_HUMIDITY_INFO;
        }
        throw new IllegalArgumentException("Unsupported preference key: " + key);
    }

    private int getDefaultValue() {
        final String key = getPreferenceKey();
        if ("lockscreen_weather_text".equals(key)) {
            return 1;
        }
        return 0;
    }
}
