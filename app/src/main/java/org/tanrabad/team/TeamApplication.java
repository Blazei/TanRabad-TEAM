/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.tanrabad.team;

import android.app.Application;
import org.tanrabad.team.tools.AnalyticsWrapper;

public class TeamApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AnalyticsWrapper.init(this);
    }
}
