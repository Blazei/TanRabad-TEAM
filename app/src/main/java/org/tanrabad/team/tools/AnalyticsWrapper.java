/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.tanrabad.team.tools;

import android.content.Context;
import android.util.Log;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import org.tanrabad.team.BuildConfig;
import org.tanrabad.team.R;

public final class AnalyticsWrapper {

    private static Tracker tracker;

    private AnalyticsWrapper() {
    }

    public static void init(Context context) {
        AnalyticsWrapper.tracker = buildTracker(context);
        suppressLintWarning(context);
    }

    private static Tracker buildTracker(Context context) {
        GoogleAnalytics analytics = GoogleAnalytics.getInstance(context);
        analytics.setDryRun(BuildConfig.DEBUG);
        Tracker tracker = analytics.newTracker(R.xml.global_tracker);
        tracker.setAppName(context.getString(R.string.app_name));
        tracker.setAppVersion(BuildConfig.VERSION_NAME);
        tracker.enableAutoActivityTracking(true);
        tracker.enableExceptionReporting(true);
        tracker.enableAdvertisingIdCollection(true);
        return tracker;

    }

    private static void suppressLintWarning(Context context) {
        //lint warning UnusedResource
        if (BuildConfig.DEBUG) {
            Log.d("GA", context.getString(R.string.ga_trackingId));
            Log.d("GA", context.getString(R.string.gcm_defaultSenderId));
            Log.d("GA", context.getString(R.string.google_app_id));
            Log.d("GA", context.getString(R.string.test_banner_ad_unit_id));
            Log.d("GA", context.getString(R.string.test_interstitial_ad_unit_id));
        }
    }

    public static Tracker getTracker() {
        return tracker;
    }
}
