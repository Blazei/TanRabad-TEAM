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
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import com.sendbird.android.MessageListQuery;
import com.sendbird.android.SendBird;
import com.sendbird.android.SendBirdEventHandler;
import com.sendbird.android.model.*;

import java.util.List;


public class SendBirdChatActivity extends FragmentActivity {

    public static final int REQUEST_CHANNEL_LIST = 100;

    protected SendBirdChatAdapter mSendBirdChatAdapter;
    protected boolean mDoNotDisconnect;
    private SendBirdChatFragment mSendBirdChatFragment;
    private ImageButton mBtnClose;
    private ImageButton mBtnSettings;
    private TextView mTxtChannelUrl;
    private View mTopBarContainer;
    private View mSettingsContainer;
    private Button mBtnLeave;
    private String mChannelUrl;
    private Button mBtnMembers;


    public static Bundle makeSendBirdArgs(String uuid, String nickname, String channelUrl) {
        Bundle args = new Bundle();
        args.putString("uuid", uuid);
        args.putString("nickname", nickname);
        args.putString("channelUrl", channelUrl);
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

        SendBird.queryMessageList(mChannelUrl).prev(Long.MAX_VALUE, 50, new MessageListQuery.MessageListQueryResult() {
            @Override
            public void onResult(List<MessageModel> messageModels) {
                for (MessageModel model : messageModels) {
                    mSendBirdChatAdapter.addMessageModel(model);
                }


                mSendBirdChatAdapter.notifyDataSetChanged();
                mSendBirdChatFragment.mListView.setSelection(mSendBirdChatAdapter.getCount());
                SendBird.join(mChannelUrl);
                SendBird.connect(mSendBirdChatAdapter.getMaxMessageTimestamp());
            }

            @Override
            public void onError(Exception e) {

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
        mSendBirdChatFragment = new SendBirdChatFragment();

        mSendBirdChatAdapter = new SendBirdChatAdapter(this, this);
        mSendBirdChatFragment.setSendBirdChatAdapter(mSendBirdChatAdapter);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, mSendBirdChatFragment)
                .commit();
    }

    private void initSendBird(Bundle extras) {
        String uuid = extras.getString("uuid");
        String nickname = extras.getString("nickname");
        String gcmRegToken = PreferenceManager.getDefaultSharedPreferences(this).getString("SendBirdGCMToken", "");

        mChannelUrl = extras.getString("channelUrl");

        SendBird.init(this, BuildConfig.SENDBIRD_APP_ID);
        SendBird.login(SendBird.LoginOption.build(uuid)
                .setUserName(nickname)
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
            }

            @Override
            public void onMessageReceived(Message message) {
                mSendBirdChatAdapter.addMessageModel(message);
            }

            @Override
            public void onMutedMessageReceived(Message message) {

            }

            @Override
            public void onSystemMessageReceived(SystemMessage systemMessage) {
                mSendBirdChatAdapter.addMessageModel(systemMessage);
            }

            @Override
            public void onBroadcastMessageReceived(BroadcastMessage broadcastMessage) {
                mSendBirdChatAdapter.addMessageModel(broadcastMessage);
            }

            @Override
            public void onFileReceived(FileLink fileLink) {
                mSendBirdChatAdapter.addMessageModel(fileLink);
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
                mSendBirdChatAdapter.notifyDataSetChanged();
                mSendBirdChatFragment.mListView.setSelection(mSendBirdChatAdapter.getCount());
            }

            @Override
            public void onMessageDelivery(boolean sent, String message, String data, String id) {
                if (!sent) {
                    mSendBirdChatFragment.mEtxtMessage.setText(message);
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

        mBtnClose = (ImageButton) findViewById(R.id.btn_close);
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
