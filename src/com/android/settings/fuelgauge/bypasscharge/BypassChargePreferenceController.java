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

import static androidx.lifecycle.Lifecycle.Event.ON_START;
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

/**
 * Controller for the "bypass charging" switch on the battery screen.
 *
 * <p>Bypass charging is a runtime feature: the charger driver only keeps it
 * active while a wired charger is connected and the negotiated charging
 * protocol supports it. The switch stores the user intent, the live state is
 * read back from the driver and the intent is re-applied whenever the driver
 * drops it (for example after a protocol renegotiation).
 */
public class BypassChargePreferenceController extends TogglePreferenceController
        implements LifecycleObserver {

    private final BroadcastReceiver mChargerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onChargerStateChanged(BypassChargeManager.isWiredChargerConnected(context));
        }
    };

    @Nullable
    private TwoStatePreference mPreference;
    private boolean mPlugged;

    public BypassChargePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return BypassChargeManager.isSupported(mContext) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_battery;
    }

    @Override
    public boolean isChecked() {
        return BypassChargeManager.isEnabled(mContext);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (isChecked && !BypassChargeManager.isWiredChargerConnected(mContext)) {
            Toast.makeText(mContext, R.string.bypass_charge_connect_charger_toast,
                    Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!BypassChargeManager.setEnabled(mContext, isChecked)) {
            Toast.makeText(mContext, R.string.bypass_charge_failed_toast,
                    Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    @Override
    public CharSequence getSummary() {
        if (!BypassChargeManager.isWiredChargerConnected(mContext)) {
            return mContext.getString(R.string.bypass_charge_summary_connect);
        }
        if (!isChecked()) {
            return mContext.getString(R.string.bypass_charge_summary_off);
        }
        final int status = BypassChargeManager.getStatus(mContext);
        if (status == BypassChargeManager.STATUS_NOT_ALLOWED) {
            return mContext.getString(R.string.bypass_charge_summary_unavailable);
        }
        if (status == BypassChargeManager.STATUS_DISABLED
                || status == BypassChargeManager.STATUS_WAITING) {
            return mContext.getString(R.string.bypass_charge_summary_pending);
        }
        return mContext.getString(R.string.bypass_charge_summary_on);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final Preference preference = screen.findPreference(getPreferenceKey());
        if (preference instanceof TwoStatePreference) {
            mPreference = (TwoStatePreference) preference;
        }
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        refreshSummary(preference);
    }

    @OnLifecycleEvent(ON_START)
    public void onStart() {
        mPlugged = BypassChargeManager.isWiredChargerConnected(mContext);
        if (!mPlugged && isChecked()) {
            // The charger driver has already dropped bypass charging; clear the
            // stale user intent so the switch matches reality.
            setChecked(false);
        } else if (mPlugged && isChecked()) {
            // The charger driver clears PLC when the charger is removed or the
            // negotiated protocol changes, so re-apply the stored intent.
            ensurePlcActive();
        }
        mContext.registerReceiver(mChargerReceiver,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        refreshUi();
    }

    @OnLifecycleEvent(ON_STOP)
    public void onStop() {
        mContext.unregisterReceiver(mChargerReceiver);
    }

    private void onChargerStateChanged(boolean plugged) {
        if (plugged != mPlugged) {
            mPlugged = plugged;
            if (!plugged) {
                if (isChecked()) {
                    setChecked(false);
                }
            } else if (isChecked()) {
                // Re-apply the user intent, PLC is cleared when the charger goes away.
                ensurePlcActive();
            }
        } else if (plugged) {
            // The charger driver can drop PLC when the negotiated charging
            // protocol changes; re-apply it like the stock implementation does.
            ensurePlcActive();
        }
        refreshUi();
    }

    /**
     * Re-applies the user intent without user facing errors.
     *
     * <p>PLC is refused while the current charger protocol has no PLC support
     * ({@link BypassChargeManager#STATUS_NOT_ALLOWED}); in that case wait until
     * the charger driver reports a usable state again.
     */
    private void ensurePlcActive() {
        if (!isChecked() || !BypassChargeManager.isWiredChargerConnected(mContext)) {
            return;
        }
        final int status = BypassChargeManager.getStatus(mContext);
        if (status == BypassChargeManager.STATUS_ENABLED
                || status == BypassChargeManager.STATUS_NOT_ALLOWED) {
            return;
        }
        BypassChargeManager.setEnabled(mContext, true);
    }

    private void refreshUi() {
        if (mPreference != null) {
            updateState(mPreference);
        }
    }
}
