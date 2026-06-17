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

package com.android.settings.display;

import android.content.Context;
import android.provider.Settings;
import android.util.AttributeSet;
import com.android.settings.widget.SeekBarPreference;

public class SystemSettingSeekBarPreference extends SeekBarPreference {

    public SystemSettingSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public SystemSettingSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SystemSettingSeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SystemSettingSeekBarPreference(Context context) {
        super(context);
    }

    @Override
    protected boolean persistInt(int value) {
        Settings.System.putInt(getContext().getContentResolver(), getKey(), value);
        return true;
    }

    @Override
    protected int getPersistedInt(int defaultVal) {
        return Settings.System.getInt(getContext().getContentResolver(), getKey(), defaultVal);
    }
}
