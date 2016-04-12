/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.tanrabad.team.utils;

import android.os.Build;
import android.provider.Settings;
import android.support.graphics.drawable.BuildConfig;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class UuidUtils {

    private static final String TAG = "UuidUtils";

    private UuidUtils() {
    }

    public static String generateDeviceUuid() {
        String serial = Build.SERIAL;
        String androidId = Settings.Secure.ANDROID_ID;
        String deviceUuid = serial + androidId;

        MessageDigest digest;
        byte[] result;
        try {
            digest = MessageDigest.getInstance("SHA-1");
            result = digest.digest(deviceUuid.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException exception) {
            if (BuildConfig.DEBUG) Log.e(TAG, "generateDeviceUuid", exception);
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (byte b : result) {
            sb.append(String.format("%02X", b));
        }

        return sb.toString();
    }
}
