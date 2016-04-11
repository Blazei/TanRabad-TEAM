/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.tanrabad.team;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.test.ApplicationTestCase;
import android.test.MoreAsserts;
import org.junit.Before;
import org.junit.Test;

public class ApplicationTest extends ApplicationTestCase<Application> {

    private Application application;

    public ApplicationTest() {
        super(Application.class);
    }

    @Before
    protected void setUp() throws Exception {
        super.setUp();
        createApplication();
        application = getApplication();
    }

    @Test
    public void testCorrectVersion() throws PackageManager.NameNotFoundException {
        PackageInfo info = application.getPackageManager().getPackageInfo(application.getPackageName(), 0);
        assertNotNull(info);
        MoreAsserts.assertMatchesRegex("\\d\\.\\d", info.versionName);
    }
}
