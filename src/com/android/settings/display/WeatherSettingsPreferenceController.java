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

import com.android.settings.core.BasePreferenceController;

/**
 * Controller for lock screen weather provider settings shortcut.
 */
public class WeatherSettingsPreferenceController extends BasePreferenceController {

    private static final String WEATHER_PACKAGE = "org.omnirom.omnijaws";
    private static final String WEATHER_SETTINGS_CLASS =
            "org.omnirom.omnijaws.SettingsActivity";

    public WeatherSettingsPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        final Intent intent = new Intent(Intent.ACTION_MAIN)
                .setClassName(WEATHER_PACKAGE, WEATHER_SETTINGS_CLASS);
        return intent.resolveActivity(mContext.getPackageManager()) != null
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }
}
