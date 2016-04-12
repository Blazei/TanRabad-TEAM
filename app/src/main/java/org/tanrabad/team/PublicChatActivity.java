/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.tanrabad.team;

import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.sendbird.android.MessageListQuery.MessageListQueryResult;
import com.sendbird.android.SendBird;
import com.sendbird.android.SendBirdEventHandler;
import com.sendbird.android.model.*;

import java.util.List;


public class PublicChatActivity extends FragmentActivity {

    private static final String PUBLIC_CHANNEL_URL = "f8346.general";
    public static final String EXTRA_USER_ID = "user-id";
    public static final String EXTRA_USERNAME = "username";

    protected ChatAdapter mChatAdapter;
    protected boolean mDoNotDisconnect;
    private ChatFragment mChatFragment;
    private TextView mTxtChannelUrl;
    private View mTopBarContainer;

    public static Bundle makeSendBirdArgs(String uuid, String nickname) {
        Bundle args = new Bundle();
        args.putString(EXTRA_USER_ID, uuid);
        args.putString(EXTRA_USERNAME, nickname);
        return args;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        resizeMenuBar();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sendbird_chat);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        initFragment();

        initUiComponents();
        initSendBird(getIntent().getExtras());

        SendBird.queryMessageList(PUBLIC_CHANNEL_URL).prev(Long.MAX_VALUE, 50, new MessageListQueryResult() {
            @Override
            public void onResult(List<MessageModel> messageModels) {
                for (MessageModel model : messageModels) {
                    mChatAdapter.addMessageModel(model);
                }
                mChatAdapter.notifyDataSetChanged();
                mChatFragment.mListView.setSelection(mChatAdapter.getCount());
                SendBird.join(PUBLIC_CHANNEL_URL);
                SendBird.connect(mChatAdapter.getMaxMessageTimestamp());
            }

            @Override
            public void onError(Exception e) {
                if (e != null) {
                    Toast.makeText(PublicChatActivity.this, "Can't load previous message", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!mDoNotDisconnect) {
            SendBird.disconnect();
        }
    }

    private void initFragment() {
        mChatFragment = new ChatFragment();

        mChatAdapter = new ChatAdapter(this);
        mChatFragment.setSendBirdChatAdapter(mChatAdapter);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, mChatFragment)
                .commit();
    }

    private void initSendBird(Bundle extras) {
        String userId = extras.getString(EXTRA_USER_ID);
        String username = extras.getString(EXTRA_USERNAME);
        String gcmRegToken = PreferenceManager.getDefaultSharedPreferences(this).getString("SendBirdGCMToken", "");

        SendBird.init(this, BuildConfig.SENDBIRD_APP_ID);
        SendBird.login(SendBird.LoginOption.build(userId)
                .setUserName(username)
                .setAccessToken(BuildConfig.SENDBIRD_API_TOKEN)
                .setGCMRegToken(gcmRegToken));
        SendBird.setEventHandler(new SendBirdEventHandler() {
            @Override
            public void onConnect(Channel channel) {
                mTxtChannelUrl.setText("#" + channel.getUrlWithoutAppPrefix());
            }

            @Override
            public void onError(int code) {
                Log.e("SendBird", "Error code: " + code);
            }

            @Override
            public void onChannelLeft(Channel channel) {
                //This never happen on public channel
            }

            @Override
            public void onMessageReceived(Message message) {
                mChatAdapter.addMessageModel(message);
            }

            @Override
            public void onMutedMessageReceived(Message message) {
            }

            @Override
            public void onSystemMessageReceived(SystemMessage systemMessage) {
                mChatAdapter.addMessageModel(systemMessage);
            }

            @Override
            public void onBroadcastMessageReceived(BroadcastMessage broadcastMessage) {
                mChatAdapter.addMessageModel(broadcastMessage);
            }

            @Override
            public void onFileReceived(FileLink fileLink) {
                mChatAdapter.addMessageModel(fileLink);
            }

            @Override
            public void onMutedFileReceived(FileLink fileLink) {
            }

            @Override
            public void onReadReceived(ReadStatus readStatus) {
            }

            @Override
            public void onTypeStartReceived(TypeStatus typeStatus) {
            }

            @Override
            public void onTypeEndReceived(TypeStatus typeStatus) {
            }

            @Override
            public void onAllDataReceived(SendBird.SendBirdDataType type, int count) {
                mChatAdapter.notifyDataSetChanged();
                mChatFragment.mListView.setSelection(mChatAdapter.getCount());
            }

            @Override
            public void onMessageDelivery(boolean sent, String message, String data, String id) {
                if (!sent) {
                    mChatFragment.mEtxtMessage.setText(message);
                }
            }

            @Override
            public void onMessagingStarted(MessagingChannel messagingChannel) {
            }

            @Override
            public void onMessagingUpdated(MessagingChannel messagingChannel) {
            }

            @Override
            public void onMessagingEnded(MessagingChannel messagingChannel) {
            }

            @Override
            public void onAllMessagingEnded() {
            }

            @Override
            public void onMessagingHidden(MessagingChannel messagingChannel) {
            }

            @Override
            public void onAllMessagingHidden() {
            }

        });
    }

    private void initUiComponents() {
        mTopBarContainer = findViewById(R.id.top_bar_container);
        mTxtChannelUrl = (TextView) findViewById(R.id.txt_channel_url);

        ImageButton mBtnClose = (ImageButton) findViewById(R.id.btn_close);
        mBtnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        resizeMenuBar();
    }


    private void resizeMenuBar() {
        ViewGroup.LayoutParams lp = mTopBarContainer.getLayoutParams();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            lp.height = (int) (28 * getResources().getDisplayMetrics().density);
        } else {
            lp.height = (int) (48 * getResources().getDisplayMetrics().density);
        }
        mTopBarContainer.setLayoutParams(lp);
    }
}
