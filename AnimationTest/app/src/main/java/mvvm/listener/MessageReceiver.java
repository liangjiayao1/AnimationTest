package mvvm.listener;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.avos.avoscloud.im.v2.AVIMClient;
import com.avos.avoscloud.im.v2.AVIMConversation;
import com.avos.avoscloud.im.v2.AVIMMessage;
import com.avos.avoscloud.im.v2.AVIMMessageHandler;
import com.avos.avospush.notification.NotificationCompat;
import com.example.lucas.animationtest.R;

import org.greenrobot.eventbus.EventBus;

import java.util.Random;

import constant.Constant;
import mvvm.model.MessageEvent;
import util.BaseApp;
import util.Config;

/**
 * Created by lucas on 18/11/2016.
 */

public class MessageReceiver extends AVIMMessageHandler {
    private Context context;

    public MessageReceiver(Context context) {
        this.context = context;
    }

    @Override
    public void onMessage(AVIMMessage message, AVIMConversation conversation, AVIMClient client) {
        super.onMessage(message, conversation, client);
        String clientId = "";
        clientId = AvImClientManager.getInstance().getClientId();
        if (client.getClientId().equals(clientId)) {
            if (!message.getFrom().equals(clientId)) {
                MessageEvent event = new MessageEvent(message, conversation);
                EventBus.getDefault().post(event);
                if (!BaseApp.getCurrentActivityName(context).equals("ChatActivity")) {
                    sendNotification(message, conversation);
                }
            }
        } else {
            client.close(null);
        }
    }

    private void sendNotification(AVIMMessage message, AVIMConversation conversation) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        String userId = Config.getString(Constant.USER_ID);
        intent.putExtra(Constant.MEMBER_ID, userId);
        intent.putExtra(Constant.CONVERSATION_ID, conversation.getConversationId());
        intent.setFlags(0);
        PendingIntent pending = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        int notificationId = (new Random()).nextInt();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_suggestion)
                .setContentText(message.getContent())
                .setContentTitle(message.getFrom())
                .setAutoCancel(true)
                .setContentIntent(pending);
        Notification notification = builder.build();

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(notificationId, notification);
    }
}
