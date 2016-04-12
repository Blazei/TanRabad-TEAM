/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.tanrabad.team;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import com.sendbird.android.MessageListQuery;
import com.sendbird.android.SendBird;
import com.sendbird.android.SendBirdFileUploadEventHandler;
import com.sendbird.android.model.FileInfo;
import com.sendbird.android.model.MessageModel;
import org.tanrabad.team.utils.FileUtils;
import org.tanrabad.team.utils.SoftKeyboard;

import java.io.File;
import java.util.List;
import java.util.Map;

public class SendBirdChatFragment extends Fragment {
    private static final int REQUEST_PICK_IMAGE = 100;

    protected ListView mListView;
    protected EditText mEtxtMessage;
    private SendBirdChatAdapter mAdapter;
    private Button mBtnSend;
    private ImageButton mBtnChannel;
    private ImageButton mBtnUpload;
    private ProgressBar mProgressBtnUpload;
    private SendBirdChatHandler mHandler;
    private MessageListQuery.MessageListQueryResult resultHandler;

    public SendBirdChatFragment() {
    }

    public void setSendBirdChatHandler(SendBirdChatHandler handler) {
        mHandler = handler;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_PICK_IMAGE && data != null && data.getData() != null) {
                upload(data.getData());
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.sendbird_fragment_chat, container, false);
        initUiComponents(rootView);
        return rootView;
    }


    private void initUiComponents(View rootView) {
        mListView = (ListView) rootView.findViewById(R.id.list);
        turnOffListViewDecoration(mListView);
        mListView.setAdapter(mAdapter);

        mBtnChannel = (ImageButton) rootView.findViewById(R.id.btn_channel);
        mBtnSend = (Button) rootView.findViewById(R.id.btn_send);
        mBtnUpload = (ImageButton) rootView.findViewById(R.id.btn_upload);
        mProgressBtnUpload = (ProgressBar) rootView.findViewById(R.id.progress_btn_upload);
        mEtxtMessage = (EditText) rootView.findViewById(R.id.etxt_message);

        mBtnSend.setEnabled(false);
        mBtnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send();
            }
        });


        mBtnChannel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mHandler != null) {
                    mHandler.onChannelListClicked();
                }
            }
        });

        mBtnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_PICK_IMAGE);
            }
        });

        mEtxtMessage.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        send();
                    }
                    return true; // Do not hide keyboard.
                }
                return false;
            }
        });
        mEtxtMessage.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        mEtxtMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mBtnSend.setEnabled(s.length() > 0);
            }
        });
        mListView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                SoftKeyboard.hide(getActivity());
                return false;
            }
        });
        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == SCROLL_STATE_IDLE) {
                    if (view.getFirstVisiblePosition() == 0
                            && view.getChildCount() > 0
                            && view.getChildAt(0).getTop() == 0) {
                        resultHandler = new MessageListQuery.MessageListQueryResult() {
                            @Override
                            public void onResult(List<MessageModel> messageModels) {
                                for (MessageModel model : messageModels) {
                                    mAdapter.addMessageModel(model);
                                }

                                mAdapter.notifyDataSetChanged();
                                mListView.setSelection(messageModels.size());
                            }

                            @Override
                            public void onError(Exception e) {
                            }
                        };
                        SendBird.queryMessageList(SendBird.getChannelUrl())
                                .prev(mAdapter.getMinMessageTimestamp(), 30, resultHandler);
                    }
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });
    }

    private void turnOffListViewDecoration(ListView listView) {
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setHorizontalFadingEdgeEnabled(false);
        listView.setVerticalFadingEdgeEnabled(false);
        listView.setHorizontalScrollBarEnabled(false);
        listView.setVerticalScrollBarEnabled(true);
        listView.setSelector(new ColorDrawable(0x00ffffff));
        listView.setCacheColorHint(0x00000000); // For Gingerbread scrolling bug fix
    }

    private void send() {
        SendBird.send(mEtxtMessage.getText().toString());
        mEtxtMessage.setText("");

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            SoftKeyboard.hide(getActivity());
        }
    }

    private void upload(Uri uri) {
        try {
            Map<String, Object> info = FileUtils.getFileInfo(getActivity(), uri);
            final String path = (String) info.get("path");
            final String mime = (String) info.get("mime");
            final int size = (Integer) info.get("size");

            if (path == null) {
                Toast.makeText(getActivity(), "Uploading file must be located in local storage.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            showUploadProgress(true);
            SendBird.uploadFile(new File(path), mime, size, "", new SendBirdFileUploadEventHandler() {
                @Override
                public void onUpload(FileInfo fileInfo, Exception e) {
                    showUploadProgress(false);
                    if (e != null) {
                        e.printStackTrace();
                        Toast.makeText(getActivity(), "Fail to upload the file.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    SendBird.sendFile(fileInfo);
                }
            });


        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "Fail to upload the file.", Toast.LENGTH_LONG).show();
        }
    }

    private void showUploadProgress(boolean tf) {
        if (tf) {
            mBtnUpload.setEnabled(false);
            mBtnUpload.setVisibility(View.INVISIBLE);
            mProgressBtnUpload.setVisibility(View.VISIBLE);
        } else {
            mBtnUpload.setEnabled(true);
            mBtnUpload.setVisibility(View.VISIBLE);
            mProgressBtnUpload.setVisibility(View.GONE);
        }
    }

    public void setSendBirdChatAdapter(SendBirdChatAdapter adapter) {
        mAdapter = adapter;
        if (mListView != null) {
            mListView.setAdapter(adapter);
        }
    }


    public interface SendBirdChatHandler {
        void onChannelListClicked();
    }
}
