/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.applications.manageapplications;

import static android.content.pm.PackageManager.INSTALL_ALL_WHITELIST_RESTRICTED_PERMISSIONS;
import static android.content.pm.PackageManager.INSTALL_FAILED_INVALID_URI;
import static android.content.pm.PackageManager.INSTALL_REASON_USER;
import static android.os.UserManager.USER_TYPE_PROFILE_SANDBOX;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

import com.android.settings.Utils;

import java.util.HashSet;

/**
 * Handles sandbox profile creation and sandboxed app install/uninstall.
 */
public class SandboxBackend {

    public static final String TAG = "SandboxBackend";
    public static final int SUCCESS = 0;
    private static final int ERROR_CREATING_SANDBOX_USER = 1;
    private static final int ERROR_STARTING_SANDBOX_USER = 2;
    private static final int ERROR_INSTALLING_PACKAGE = 3;
    private static SandboxBackend sInstance;
    private final Context mContext;
    private int mSandboxUserId;

    private SandboxBackend(Context context) {
        mContext = context;
        mSandboxUserId = Utils.getSandboxUserId(context);
    }

    /** Returns the SandboxBackend instance. */
    public static SandboxBackend getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new SandboxBackend(context.getApplicationContext());
        }
        return sInstance;
    }

    /** Starts the uninstaller for the sandboxed instance of the given package. */
    void uninstallFromSandbox(String packageName, FragmentActivity activity) {
        uninstallForUser(packageName, UserHandle.of(mSandboxUserId), activity);
    }

    /** Starts the uninstaller for the main profile instance of the given package. */
    void uninstallFromMain(String packageName, FragmentActivity activity) {
        uninstallForUser(packageName, UserHandle.of(UserHandle.myUserId()), activity);
    }

    private void uninstallForUser(String packageName, UserHandle user,
            FragmentActivity activity) {
        Uri packageUri = Uri.parse("package:" + packageName);
        Intent uninstallIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri);
        uninstallIntent.putExtra(Intent.EXTRA_UNINSTALL_ALL_USERS, false);
        uninstallIntent.putExtra(Intent.EXTRA_USER, user);
        activity.startActivityAsUser(uninstallIntent, user);
    }

    /**
     * Installs another instance of given package in the sandbox profile.
     *
     * <p> Creates the sandbox profile if it doesn't exist and starts it before installing.
     */
    public int installInSandbox(String packageName) {
        boolean newlyCreated = false;

        if (mSandboxUserId == -1) {
            UserManager um = mContext.getSystemService(UserManager.class);
            UserHandle sandboxUserHandle;
            try {
                sandboxUserHandle = um.createProfile("Sandbox", USER_TYPE_PROFILE_SANDBOX,
                        new HashSet<>());
            } catch (Exception e) {
                Log.e(TAG, "Error creating sandbox user", e);
                return ERROR_CREATING_SANDBOX_USER;
            }
            if (sandboxUserHandle == null) {
                return ERROR_CREATING_SANDBOX_USER;
            }
            mSandboxUserId = sandboxUserHandle.getIdentifier();
            newlyCreated = true;
        }

        if (newlyCreated) {
            try {
                ActivityManager.getService().startProfile(mSandboxUserId);
            } catch (RemoteException e) {
                Log.e(TAG, "Error starting sandbox user", e);
                return ERROR_STARTING_SANDBOX_USER;
            }
        }

        int res;
        try {
            res = AppGlobals.getPackageManager().installExistingPackageAsUser(
                    packageName, mSandboxUserId,
                    INSTALL_ALL_WHITELIST_RESTRICTED_PERMISSIONS, INSTALL_REASON_USER, null);
        } catch (RemoteException e) {
            Log.e(TAG, "Error installing " + packageName + " in sandbox user", e);
            return ERROR_INSTALLING_PACKAGE;
        }
        if (res == INSTALL_FAILED_INVALID_URI) {
            Log.e(TAG, "Package " + packageName + " doesn't exist.");
            return ERROR_INSTALLING_PACKAGE;
        }
        return SUCCESS;
    }

    public int getSandboxUserId() {
        return mSandboxUserId;
    }

    /** Resets the cached sandbox user id after the profile has been removed. */
    public void resetSandboxUserId() {
        mSandboxUserId = -1;
    }
}
