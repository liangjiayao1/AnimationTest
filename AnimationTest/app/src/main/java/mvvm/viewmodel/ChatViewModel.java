package mvvm.viewmodel;

import android.app.Activity;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;

import com.avos.avoscloud.im.v2.AVIMClient;
import com.avos.avoscloud.im.v2.AVIMConversation;
import com.avos.avoscloud.im.v2.AVIMException;
import com.avos.avoscloud.im.v2.AVIMMessage;
import com.avos.avoscloud.im.v2.callback.AVIMClientCallback;
import com.avos.avoscloud.im.v2.callback.AVIMConversationCallback;
import com.avos.avoscloud.im.v2.callback.AVIMMessagesQueryCallback;
import com.example.lucas.animationtest.databinding.ActivityChatBinding;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

import constant.Constant;
import mvvm.adapter.ChatAdapter;
import mvvm.model.MessageEvent;
import util.StringUtil;

/**
 * Created by lucas on 16/11/2016.
 */

public class ChatViewModel implements SwipeRefreshLayout.OnRefreshListener {
    private Activity context;
    private ActivityChatBinding binding;
    private ChatAdapter chatAdapter;
    private ChatItemViewModel chatItemViewModel;
    private LinearLayoutManager layoutManager;
    private AVIMConversation conversation;
    private AVIMClient mClient;

    private String userId = "";

    public ChatViewModel(Activity context, ActivityChatBinding binding) {
        this.context = context;
        this.binding = binding;
    }

    public void onCreate() {
        EventBus.getDefault().register(this);
        initRecyclerView();
        userId = context.getIntent().getStringExtra(Constant.ID);
        mClient = AVIMClient.getInstance(userId);
        getConversation();
    }

    public void onDestroy() {
        EventBus.getDefault().unregister(this);
    }

    private void initRecyclerView() {
        binding.srlChatList.setOnRefreshListener(this);
        layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
        binding.rvChatList.setLayoutManager(layoutManager);
        chatAdapter = new ChatAdapter(context);
        binding.rvChatList.setAdapter(chatAdapter);
    }

    private void getConversation() {
        conversation = mClient.getConversation(Constant.OBJECT_ID);
        //join chatting group
        conversation.join(new AVIMConversationCallback() {
            @Override
            public void done(AVIMException e) {
                if (null != e) {
                    getConversation();
                }
            }
        });
    }

    public void sendMessage(View view) {
        String content = binding.etContent.getText().toString().trim();
        if (StringUtil.isEmpty(content) || content.isEmpty()) {
            return;
        }
        sendByLeanCloud(content);
        binding.etContent.setText("");
        chatAdapter.notifyDataSetChanged();
    }

    private void sendByLeanCloud(final String content) {
        mClient.open(new AVIMClientCallback() {
                         @Override
                         public void done(AVIMClient avimClient, AVIMException e) {
                             if (handleExcept(e)) return;
                             sendMsg(content);
                         }
                     }
        );
    }

    private void sendMsg(final String content) {
        final AVIMMessage msg = new AVIMMessage();
        msg.setContent(content);
        msg.setTimestamp(System.currentTimeMillis());
        conversation.sendMessage(msg, new AVIMConversationCallback() {
            @Override
            public void done(AVIMException e) {
                if (handleExcept(e)) return;
                chatItemViewModel = new ChatItemViewModel(msg, userId);
                chatAdapter.add(chatItemViewModel);
                chatAdapter.notifyDataSetChanged();
                layoutManager.scrollToPositionWithOffset(chatAdapter.size() - 1, 0);
            }
        });
    }


    @Override
    public void onRefresh() {
        mClient.open(new AVIMClientCallback() {
            @Override
            public void done(AVIMClient avimClient, AVIMException e) {
                if (handleExcept(e)) return;
                getHistory(conversation);
            }
        });
    }

    private void getHistory(AVIMConversation conversation) {
        conversation.queryMessages(new AVIMMessagesQueryCallback() {
            @Override
            public void done(List<AVIMMessage> list, AVIMException e) {
                if (handleExcept(e)) return;
                chatAdapter.clear();
                List<ChatItemViewModel> viewModels = new ArrayList<ChatItemViewModel>();
                for (AVIMMessage msg : list) {
                    chatItemViewModel = new ChatItemViewModel(msg, userId);
                    viewModels.add(chatItemViewModel);
                }
                chatAdapter.addAll(0, viewModels);
                chatAdapter.notifyDataSetChanged();
                binding.srlChatList.setRefreshing(false);
            }
        });
    }

    @Subscribe
    public void receiveMessage(MessageEvent event) {
        if (null != event && null != event.getConversation()
                || event.getConversation().getConversationId().equals(conversation.getConversationId()))
            chatItemViewModel = new ChatItemViewModel(event.getMessage(), userId);
        chatAdapter.add(chatItemViewModel);
        chatAdapter.notifyDataSetChanged();
        layoutManager.scrollToPositionWithOffset(chatAdapter.size() - 1, 0);
    }

    private boolean handleExcept(AVIMException e) {
        if (null != e) {
            e.printStackTrace();
            return true;
        }
        return false;
    }
}
