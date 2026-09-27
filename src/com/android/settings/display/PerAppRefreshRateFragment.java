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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.SearchView;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Screen that lets the user pick a refresh rate for individual apps.
 */
public class PerAppRefreshRateFragment extends SettingsPreferenceFragment
        implements SearchView.OnQueryTextListener {

    private static final String KEY_APPS = "per_app_refresh_rate_apps";
    private static final String VALUE_DEFAULT = "0";
    private static final String STATE_SEARCH_QUERY = "search_query";

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private LauncherApps mLauncherApps;
    private List<Float> mRefreshRates;
    private String mSearchQuery = "";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DISPLAY;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.per_app_refresh_rate;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (icicle != null) {
            mSearchQuery = icicle.getString(STATE_SEARCH_QUERY, "");
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);

        final Context context = requireContext();
        mLauncherApps = context.getSystemService(LauncherApps.class);
        mRefreshRates = PerAppRefreshRateUtils.getSupportedRefreshRates(context);
        loadApps();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.per_app_refresh_rate, menu);

        final MenuItem searchItem = menu.findItem(R.id.per_app_refresh_rate_search);
        if (searchItem == null) {
            return;
        }
        final SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint(getString(R.string.per_app_refresh_rate_search_hint));
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setOnQueryTextListener(this);
        if (!TextUtils.isEmpty(mSearchQuery)) {
            searchItem.expandActionView();
            searchView.setQuery(mSearchQuery, false /* submit */);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_SEARCH_QUERY, mSearchQuery);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String query) {
        mSearchQuery = query;
        applyFilter(query);
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mExecutor.shutdownNow();
        mHandler.removeCallbacksAndMessages(null);
    }

    private void loadApps() {
        final int density = getResources().getDisplayMetrics().densityDpi;
        mExecutor.execute(() -> {
            final List<LauncherActivityInfo> apps = new ArrayList<>(
                    mLauncherApps.getActivityList(null, UserHandle.of(UserHandle.myUserId())));
            final Collator collator = Collator.getInstance();
            apps.sort((left, right) ->
                    collator.compare(left.getLabel().toString(), right.getLabel().toString()));

            final List<AppEntry> entries = new ArrayList<>(apps.size());
            for (LauncherActivityInfo app : apps) {
                entries.add(new AppEntry(app, app.getIcon(density)));
            }
            mHandler.post(() -> {
                if (isAdded()) {
                    addAppPreferences(entries);
                }
            });
        });
    }

    private void addAppPreferences(List<AppEntry> apps) {
        final PreferenceCategory category = findPreference(KEY_APPS);
        if (category == null) {
            return;
        }

        final Context context = requireContext();
        final CharSequence[] entries = buildEntries(context);
        final CharSequence[] entryValues = buildEntryValues();
        final Map<String, Float> rates = PerAppRefreshRateUtils.getRefreshRates(context);
        for (AppEntry entry : apps) {
            final LauncherActivityInfo app = entry.info;
            final String packageName = app.getApplicationInfo().packageName;
            final Float rate = rates.get(packageName);

            final ListPreference preference = new ListPreference(getPrefContext());
            preference.setPersistent(false);
            preference.setKey(packageName);
            preference.setTitle(app.getLabel());
            preference.setIcon(entry.icon);
            preference.setEntries(entries);
            preference.setEntryValues(entryValues);
            preference.setSummary("%s");
            preference.setValue(rate != null && rate > 0 ? Float.toString(rate) : VALUE_DEFAULT);
            if (preference.getEntry() == null) {
                preference.setValue(VALUE_DEFAULT);
            }
            preference.setOnPreferenceChangeListener((changed, value) -> {
                PerAppRefreshRateUtils.setRefreshRate(context, packageName,
                        Float.parseFloat((String) value));
                return true;
            });
            category.addPreference(preference);
        }
        applyFilter(mSearchQuery);
    }

    private void applyFilter(String query) {
        final PreferenceCategory category = findPreference(KEY_APPS);
        if (category == null) {
            return;
        }
        final String normalizedQuery = query.trim().toLowerCase(Locale.getDefault());
        for (int i = 0; i < category.getPreferenceCount(); i++) {
            final Preference preference = category.getPreference(i);
            final String title = String.valueOf(preference.getTitle()).toLowerCase(
                    Locale.getDefault());
            preference.setVisible(normalizedQuery.isEmpty() || title.contains(normalizedQuery));
        }
    }

    private CharSequence[] buildEntries(Context context) {
        final CharSequence[] entries = new CharSequence[mRefreshRates.size() + 1];
        entries[0] = context.getString(R.string.per_app_refresh_rate_default);
        for (int i = 0; i < mRefreshRates.size(); i++) {
            entries[i + 1] = context.getString(R.string.screen_refresh_rate_displayed_text,
                    PerAppRefreshRateUtils.formatRefreshRate(mRefreshRates.get(i)));
        }
        return entries;
    }

    private CharSequence[] buildEntryValues() {
        final CharSequence[] values = new CharSequence[mRefreshRates.size() + 1];
        values[0] = VALUE_DEFAULT;
        for (int i = 0; i < mRefreshRates.size(); i++) {
            values[i + 1] = Float.toString(mRefreshRates.get(i));
        }
        return values;
    }

    private static final class AppEntry {
        final LauncherActivityInfo info;
        final Drawable icon;

        AppEntry(LauncherActivityInfo info, Drawable icon) {
            this.info = info;
            this.icon = icon;
        }
    }
}
