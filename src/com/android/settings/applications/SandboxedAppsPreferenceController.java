/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.applications;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.dashboard.profileselector.ProfileSelectFragment;

import java.util.Collections;
import java.util.List;

/**
 * A preference controller handling the logic for updating the summary of sandboxed apps.
 */
public class SandboxedAppsPreferenceController extends BasePreferenceController
        implements LifecycleObserver {
    private Preference mPreference;

    public SandboxedAppsPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        UserManager um = mContext.getSystemService(UserManager.class);
        return um.isMainUser() ? AVAILABLE : DISABLED_FOR_USER;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    /**
     * On lifecycle resume event.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {
        updatePreferenceSummary();
    }

    private void updatePreferenceSummary() {
        if (!isAvailable() || mPreference == null) {
            return;
        }
        new AsyncTask<Void, Void, Integer[]>() {

            @Override
            protected Integer[] doInBackground(Void... unused) {
                List<String> cloneableApps = mContext.getPackageManager()
                        .getInstalledApplicationsAsUser(0, UserHandle.myUserId())
                        .stream()
                        .filter(x -> AppStateClonedAppsBridge.isCloneable(
                                mContext, x, Collections.emptyList()))
                        .map(x -> x.packageName).toList();

                int sandboxUserId = Utils.getSandboxUserId(mContext);
                if (sandboxUserId == -1) {
                    return new Integer[]{0, cloneableApps.size()};
                }
                List<String> sandboxApps = mContext.getPackageManager()
                        .getInstalledApplicationsAsUser(0, sandboxUserId).stream()
                        .filter(x -> (x.flags & (ApplicationInfo.FLAG_SYSTEM
                                | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) == 0)
                        .map(x -> x.packageName).toList();
                int available = (int) cloneableApps.stream()
                        .filter(x -> !sandboxApps.contains(x)).count();
                return new Integer[]{sandboxApps.size(), available};
            }

            @Override
            protected void onPostExecute(Integer[] countInfo) {
                mPreference.setSummary(mContext.getString(
                        R.string.sandboxed_apps_summary, countInfo[0], countInfo[1]));
            }
        }.execute();
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (getPreferenceKey().equals(preference.getKey())) {
            final Bundle extras = preference.getExtras();
            extras.putInt(ProfileSelectFragment.EXTRA_PROFILE,
                    ProfileSelectFragment.ProfileType.PERSONAL);
        }
        return super.handlePreferenceTreeClick(preference);
    }
}
