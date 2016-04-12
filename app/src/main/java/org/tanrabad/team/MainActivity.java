/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.tanrabad.team;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import org.tanrabad.team.utils.UuidUtils;

public class MainActivity extends AppCompatActivity {

    private final String generateUserId = UuidUtils.generateDeviceUuid();
    private final String generateUsername = "User-" + generateUserId.substring(0, 5);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startPublicChat();
    }

    private void startPublicChat() {
        Intent intent = new Intent(this, PublicChatActivity.class);
        intent.putExtras(PublicChatActivity.makeSendBirdArgs(generateUserId, generateUsername));

        startActivity(intent);
    }

}
