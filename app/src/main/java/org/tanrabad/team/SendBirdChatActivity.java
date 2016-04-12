/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.tanrabad.team;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import com.sendbird.android.MessageListQuery;
import com.sendbird.android.SendBird;
import com.sendbird.android.SendBirdEventHandler;
import com.sendbird.android.model.*;
import org.tanrabad.team.task.UrlDownloadAsyncTask;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.text.DecimalFormat;
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


    public static Bundle makeSendBirdArgs(String appKey, String uuid, String nickname, String channelUrl) {
        Bundle args = new Bundle();
        args.putString("appKey", appKey);
        args.putString("uuid", uuid);
        args.putString("nickname", nickname);
        args.putString("channelUrl", channelUrl);
        return args;
    }

    protected static void displayUrlImage(ImageView imageView, String url) {
        UrlDownloadAsyncTask.display(url, imageView);
    }

    protected static void downloadUrl(FileLink fileLink, Context context) throws IOException {
        String url = fileLink.getFileInfo().getUrl();
        String name = fileLink.getFileInfo().getName();
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File downloadFile = File.createTempFile("SendBird", name.substring(name.lastIndexOf(".")), downloadDir);
        UrlDownloadAsyncTask.download(url, downloadFile, context);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CHANNEL_LIST) {
            if (resultCode == RESULT_OK && data != null) {
                mChannelUrl = data.getStringExtra("channelUrl");


                mSendBirdChatAdapter.clear();
                mSendBirdChatAdapter.notifyDataSetChanged();

                SendBird.queryMessageList(mChannelUrl)
                        .prev(Long.MAX_VALUE, 50, new MessageListQuery.MessageListQueryResult() {
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
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        resizeMenubar();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sendbird_chat);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        initFragment();

        initUIComponents();
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
        String appKey = extras.getString("appKey");
        String uuid = extras.getString("uuid");
        String nickname = extras.getString("nickname");
        String gcmRegToken = PreferenceManager.getDefaultSharedPreferences(SendBirdChatActivity.this)
                .getString("SendBirdGCMToken", "");

        mChannelUrl = extras.getString("channelUrl");

        SendBird.init(this, appKey);
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

    private void initUIComponents() {
        mTopBarContainer = findViewById(R.id.top_bar_container);
        mTxtChannelUrl = (TextView) findViewById(R.id.txt_channel_url);

        mBtnClose = (ImageButton) findViewById(R.id.btn_close);
        mBtnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        resizeMenubar();
    }

    @Override
    public void finish() {
        super.finish();
    }

    private void resizeMenubar() {
        ViewGroup.LayoutParams lp = mTopBarContainer.getLayoutParams();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            lp.height = (int) (28 * getResources().getDisplayMetrics().density);
        } else {
            lp.height = (int) (48 * getResources().getDisplayMetrics().density);
        }
        mTopBarContainer.setLayoutParams(lp);
    }

    public static class Helper {
        public static String generateDeviceUUID(Context context) {
            String serial = Build.SERIAL;
            String androidID = Settings.Secure.ANDROID_ID;
            String deviceUUID = serial + androidID;

        /*
         * SHA-1
         */
            MessageDigest digest;
            byte[] result;
            try {
                digest = MessageDigest.getInstance("SHA-1");
                result = digest.digest(deviceUUID.getBytes("UTF-8"));
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }

            StringBuilder sb = new StringBuilder();
            for (byte b : result) {
                sb.append(String.format("%02X", b));
            }

            return sb.toString();
        }

        public static void hideKeyboard(Activity activity) {
            if (activity == null || activity.getCurrentFocus() == null) {
                return;
            }

            InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
            if (inputMethodManager != null) {
                inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
            }
        }

        public static String readableFileSize(long size) {
            if (size <= 0) return "0KB";
            final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
            int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
            return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
        }

    }

}
