/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.system

import android.content.Context
import android.os.SystemProperties
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.core.BasePreferenceController

/** Applies the chosen value through persist.sys.<key>, which device init writes to the kernel. */
class KernelTunablePreferenceController(context: Context, key: String) :
    BasePreferenceController(context, key), Preference.OnPreferenceChangeListener {

    private val property = "persist.sys.$key"

    override fun getAvailabilityStatus() =
        if (mContext.resources.getBoolean(R.bool.config_kernelTunables)) AVAILABLE
        else UNSUPPORTED_ON_DEVICE

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        SystemProperties.set(property, newValue.toString())
        updateState(preference)
        return true
    }

    override fun updateState(preference: Preference) {
        val list = preference as ListPreference
        list.value = SystemProperties.get(property)
        list.summary = list.entry
    }
}
