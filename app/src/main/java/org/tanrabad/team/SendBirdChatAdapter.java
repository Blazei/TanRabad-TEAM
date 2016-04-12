/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.tanrabad.team;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.sendbird.android.SendBird;
import com.sendbird.android.SendBirdException;
import com.sendbird.android.handler.DeleteMessageHandler;
import com.sendbird.android.model.*;
import org.tanrabad.team.utils.FileUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SendBirdChatAdapter extends BaseAdapter {
    private static final int TYPE_UNSUPPORTED = 0;
    private static final int TYPE_MESSAGE = 1;
    private static final int TYPE_SYSTEM_MESSAGE = 2;
    private static final int TYPE_FILELINK = 3;
    private static final int TYPE_BROADCAST_MESSAGE = 4;

    private SendBirdChatActivity sendBirdChatActivity;
    private final Context mContext;
    private final LayoutInflater mInflater;
    private final List<Object> mItemList;
    private long mMaxMessageTimestamp = Long.MIN_VALUE;
    private long mMinMessageTimestamp = Long.MAX_VALUE;
    private DeleteMessageHandler deleteMessageHandler;

    public SendBirdChatAdapter(SendBirdChatActivity sendBirdChatActivity, Context context) {
        this.sendBirdChatActivity = sendBirdChatActivity;
        mContext = context;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mItemList = new ArrayList<>();
    }

    public long getMaxMessageTimestamp() {
        return mMaxMessageTimestamp == Long.MIN_VALUE ? Long.MAX_VALUE : mMaxMessageTimestamp;
    }

    public long getMinMessageTimestamp() {
        return mMinMessageTimestamp == Long.MAX_VALUE ? Long.MIN_VALUE : mMinMessageTimestamp;
    }

    @Override
    public int getCount() {
        return mItemList.size();
    }

    @Override
    public Object getItem(int position) {
        return mItemList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        final Object item = getItem(position);

        if (convertView == null || ((ViewHolder) convertView.getTag()).getViewType() != getItemViewType(position)) {
            viewHolder = new ViewHolder();
            viewHolder.setViewType(getItemViewType(position));

            switch (getItemViewType(position)) {
                case TYPE_UNSUPPORTED:
                    convertView = new View(mInflater.getContext());
                    convertView.setTag(viewHolder);
                    break;
                case TYPE_MESSAGE: {
                    TextView tv;

                    convertView = mInflater.inflate(R.layout.sendbird_view_message, parent, false);
                    tv = (TextView) convertView.findViewById(R.id.txt_message);
                    viewHolder.setView("message", tv);
                    viewHolder.setView("img_op_icon", convertView.findViewById(R.id.img_op_icon));
                    convertView.setTag(viewHolder);
                    break;
                }
                case TYPE_SYSTEM_MESSAGE: {
                    convertView = mInflater.inflate(R.layout.sendbird_view_system_message, parent, false);
                    viewHolder.setView("message", convertView.findViewById(R.id.txt_message));
                    convertView.setTag(viewHolder);
                    break;
                }
                case TYPE_BROADCAST_MESSAGE: {
                    convertView = mInflater.inflate(R.layout.sendbird_view_system_message, parent, false);
                    viewHolder.setView("message", convertView.findViewById(R.id.txt_message));
                    convertView.setTag(viewHolder);
                    break;
                }
                case TYPE_FILELINK: {
                    TextView tv;

                    convertView = mInflater.inflate(R.layout.sendbird_view_filelink, parent, false);
                    tv = (TextView) convertView.findViewById(R.id.txt_sender_name);
                    viewHolder.setView("txt_sender_name", tv);
                    viewHolder.setView("img_op_icon", convertView.findViewById(R.id.img_op_icon));

                    viewHolder.setView("img_file_container", convertView.findViewById(R.id.img_file_container));

                    viewHolder.setView("image_container", convertView.findViewById(R.id.image_container));
                    viewHolder.setView("img_thumbnail", convertView.findViewById(R.id.img_thumbnail));
                    viewHolder.setView("txt_image_name", convertView.findViewById(R.id.txt_image_name));
                    viewHolder.setView("txt_image_size", convertView.findViewById(R.id.txt_image_size));

                    viewHolder.setView("file_container", convertView.findViewById(R.id.file_container));
                    viewHolder.setView("txt_file_name", convertView.findViewById(R.id.txt_file_name));
                    viewHolder.setView("txt_file_size", convertView.findViewById(R.id.txt_file_size));

                    convertView.setTag(viewHolder);

                    break;
                }
            }
        }


        viewHolder = (ViewHolder) convertView.getTag();
        switch (getItemViewType(position)) {
            case TYPE_UNSUPPORTED:
                break;
            case TYPE_MESSAGE:
                Message message = (Message) item;
                if (message.isOpMessage()) {
                    viewHolder.getView("img_op_icon", ImageView.class).setVisibility(View.VISIBLE);
                    viewHolder.getView("message", TextView.class)
                            .setText(Html.fromHtml("&nbsp;&nbsp;&nbsp;<font color='#824096'><b>"
                                    + message.getSenderName()
                                    + "</b></font>: "
                                    + message.getMessage()));
                } else {
                    viewHolder.getView("img_op_icon", ImageView.class).setVisibility(View.GONE);
                    viewHolder.getView("message", TextView.class)
                            .setText(Html.fromHtml("<font color='#824096'><b>"
                                    + message.getSenderName()
                                    + "</b></font>: "
                                    + message.getMessage()));
                }
                viewHolder.getView("message").setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new AlertDialog.Builder(mContext)
                                .setTitle("SendBird")
                                .setMessage("Do you want to start 1:1 messaging with "
                                        + ((Message) item).getSenderName() + "?")
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent data = new Intent();
                                        data.putExtra("userIds", new String[]{((Message) item).getSenderId()});
                                        sendBirdChatActivity.setResult(Activity.RESULT_OK, data);
                                        sendBirdChatActivity.mDoNotDisconnect = true;
                                        sendBirdChatActivity.finish();
                                    }
                                })
                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                })
                                .create()
                                .show();
                    }
                });
                viewHolder.getView("message").setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        new AlertDialog.Builder(mContext)
                                .setTitle("SendBird")
                                .setMessage("Do you want to delete a message?")
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        deleteMessageHandler = new DeleteMessageHandler() {
                                            @Override
                                            public void onError(SendBirdException e) {
                                                e.printStackTrace();
                                            }

                                            @Override
                                            public void onSuccess(long messageId) {
                                                sendBirdChatActivity.mSendBirdChatAdapter.delete(item);
                                                sendBirdChatActivity.mSendBirdChatAdapter.notifyDataSetChanged();
                                                Toast.makeText(mContext, "Message has been deleted.",
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        };
                                        SendBird.deleteMessage(((Message) item).getMessageId(), deleteMessageHandler);
                                    }
                                })
                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                })
                                .create()
                                .show();

                        return true;
                    }
                });
                break;
            case TYPE_SYSTEM_MESSAGE:
                SystemMessage systemMessage = (SystemMessage) item;
                viewHolder.getView("message", TextView.class).setText(Html.fromHtml(systemMessage.getMessage()));
                break;
            case TYPE_BROADCAST_MESSAGE:
                BroadcastMessage broadcastMessage = (BroadcastMessage) item;
                viewHolder.getView("message", TextView.class).setText(Html.fromHtml(broadcastMessage.getMessage()));
                break;
            case TYPE_FILELINK:
                FileLink fileLink = (FileLink) item;

                if (fileLink.isOpMessage()) {
                    viewHolder.getView("img_op_icon", ImageView.class).setVisibility(View.VISIBLE);
                    viewHolder.getView("txt_sender_name", TextView.class)
                            .setText(Html.fromHtml("&nbsp;&nbsp;&nbsp;<font color='#824096'><b>"
                                    + fileLink.getSenderName()
                                    + "</b></font>: "));
                } else {
                    viewHolder.getView("img_op_icon", ImageView.class).setVisibility(View.GONE);
                    viewHolder.getView("txt_sender_name", TextView.class)
                            .setText(Html.fromHtml("<font color='#824096'><b>"
                                    + fileLink.getSenderName()
                                    + "</b></font>: "));
                }
                if (fileLink.getFileInfo().getType().toLowerCase().startsWith("image")) {
                    viewHolder.getView("file_container").setVisibility(View.GONE);

                    viewHolder.getView("image_container").setVisibility(View.VISIBLE);
                    viewHolder.getView("txt_image_name", TextView.class).setText(fileLink.getFileInfo().getName());
                    viewHolder.getView("txt_image_size", TextView.class)
                            .setText(FileUtils.readableFileSize(fileLink.getFileInfo().getSize()));
                    SendBirdChatActivity.displayUrlImage(viewHolder.getView("img_thumbnail", ImageView.class),
                            fileLink.getFileInfo().getUrl());
                } else {
                    viewHolder.getView("image_container").setVisibility(View.GONE);

                    viewHolder.getView("file_container").setVisibility(View.VISIBLE);
                    viewHolder.getView("txt_file_name", TextView.class)
                            .setText(fileLink.getFileInfo().getName());
                    viewHolder.getView("txt_file_size", TextView.class)
                            .setText(String.valueOf(fileLink.getFileInfo().getSize()));
                }
                viewHolder.getView("txt_sender_name").setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new AlertDialog.Builder(mContext)
                                .setTitle("SendBird")
                                .setMessage("Do you want to start 1:1 messaging with "
                                        + ((FileLink) item).getSenderName() + "?")
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent data = new Intent();
                                        data.putExtra("userIds", new String[]{((FileLink) item).getSenderId()});
                                        sendBirdChatActivity.setResult(Activity.RESULT_OK, data);
                                        sendBirdChatActivity.finish();
                                    }
                                })
                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                })
                                .create()
                                .show();
                    }
                });
                viewHolder.getView("img_file_container").setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new AlertDialog.Builder(mContext)
                                .setTitle("SendBird")
                                .setMessage("Do you want to download this file?")
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        try {
                                            SendBirdChatActivity.downloadUrl((FileLink) item, mContext);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                })
                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                })
                                .create()
                                .show();
                    }
                });
                break;
        }

        return convertView;
    }

    public void delete(Object message) {
        mItemList.remove(message);
    }

    @Override
    public int getItemViewType(int position) {
        Object item = mItemList.get(position);
        if (item instanceof Message) {
            return TYPE_MESSAGE;
        } else if (item instanceof FileLink) {
            return TYPE_FILELINK;
        } else if (item instanceof SystemMessage) {
            return TYPE_SYSTEM_MESSAGE;
        } else if (item instanceof BroadcastMessage) {
            return TYPE_BROADCAST_MESSAGE;
        }

        return TYPE_UNSUPPORTED;
    }

    @Override
    public int getViewTypeCount() {
        return 5;
    }

    public void clear() {
        mMaxMessageTimestamp = Long.MIN_VALUE;
        mMinMessageTimestamp = Long.MAX_VALUE;
        mItemList.clear();
    }

    public void addMessageModel(MessageModel model) {
        if (model.isPast()) {
            mItemList.add(0, model);
        } else {
            mItemList.add(model);
        }
        updateMessageTimestamp(model);
    }

    private void updateMessageTimestamp(MessageModel model) {
        mMaxMessageTimestamp = mMaxMessageTimestamp < model.getTimestamp()
                ? model.getTimestamp() : mMaxMessageTimestamp;
        mMinMessageTimestamp = mMinMessageTimestamp > model.getTimestamp()
                ? model.getTimestamp() : mMinMessageTimestamp;
    }

    private class ViewHolder {
        private Map<String, View> holder = new ConcurrentHashMap<>();
        private int type;

        public int getViewType() {
            return this.type;
        }

        public void setViewType(int type) {
            this.type = type;
        }

        public void setView(String k, View v) {
            holder.put(k, v);
        }

        public <T> T getView(String k, Class<T> type) {
            return type.cast(getView(k));
        }

        public View getView(String k) {
            return holder.get(k);
        }
    }
}
