/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.applications;

import static com.android.settingslib.applications.ApplicationsState.AppEntry;
import static com.android.settingslib.applications.ApplicationsState.AppFilter;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.UserHandle;

import com.android.settings.Utils;
import com.android.settingslib.applications.ApplicationsState;

import java.util.Collections;
import java.util.List;

/**
 * Filter to display apps that can be sandboxed, and sandboxed apps, on the Sandboxed Apps page.
 */
public class AppStateSandboxedAppsBridge extends AppStateBaseBridge {

    public static final int STATE_NOT_SANDBOXED = 0;
    public static final int STATE_SANDBOXED = 1;
    public static final int STATE_MAIN_COPY = 2;

    private final Context mContext;
    private List<String> mSandboxApps = Collections.emptyList();

    public AppStateSandboxedAppsBridge(Context context, ApplicationsState appState,
            Callback callback) {
        super(appState, callback);
        mContext = context;
    }

    @Override
    protected void loadAllExtraInfo() {
        int sandboxUserId = Utils.getSandboxUserId(mContext);
        if (sandboxUserId != -1) {
            mSandboxApps = mContext.getPackageManager()
                    .getInstalledPackagesAsUser(0, sandboxUserId).stream()
                    .map(x -> x.packageName).toList();
        } else {
            mSandboxApps = Collections.emptyList();
        }

        final List<AppEntry> allApps = mAppSession.getAllApps();
        for (int i = 0; i < allApps.size(); i++) {
            AppEntry app = allApps.get(i);
            updateExtraInfo(app, app.info.packageName, app.info.uid);
        }
    }

    @Override
    protected void updateExtraInfo(AppEntry app, String pkg, int uid) {
        if (app.isSandboxProfile()) {
            app.extraInfo = isUserApp(app.info) ? STATE_SANDBOXED : null;
        } else if (UserHandle.getUserId(uid) == UserHandle.myUserId()
                && AppStateClonedAppsBridge.isCloneable(mContext, app.info,
                        Collections.emptyList())) {
            app.extraInfo = mSandboxApps.contains(pkg) ? STATE_MAIN_COPY : STATE_NOT_SANDBOXED;
        } else {
            app.extraInfo = null;
        }
    }

    private static boolean isUserApp(ApplicationInfo info) {
        return info != null && (info.flags & (ApplicationInfo.FLAG_SYSTEM
                | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) == 0;
    }

    public static final AppFilter FILTER_APPS_SANDBOX =
            new AppFilter() {
                @Override
                public void init() {
                }

                @Override
                public boolean filterApp(AppEntry entry) {
                    return entry.extraInfo instanceof Integer;
                }
            };
}
