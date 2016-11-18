package mvvm.viewmodel;

import android.content.Context;
import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.view.View;

import com.avos.avoscloud.im.v2.AVIMClient;
import com.avos.avoscloud.im.v2.AVIMException;
import com.avos.avoscloud.im.v2.callback.AVIMClientCallback;
import com.example.lucas.animationtest.databinding.ActivityLoginBinding;

import constant.Constant;
import mvvm.listener.AvImClientManager;
import mvvm.view.ChatActivity;
import util.StringUtil;

/**
 * Created by lucas on 18/11/2016.
 */

public class LoginViewModel {
    private Context context;
    private ActivityLoginBinding binding;
    private AvImClientManager manager = new AvImClientManager();

    public LoginViewModel(Context context, ActivityLoginBinding binding) {
        this.context = context;
        this.binding = binding;
    }

    public void enterChat(View view) {
        final String id = binding.etId.getText().toString();
        if (StringUtil.isEmpty(id)) {
            Snackbar.make(binding.getRoot(), "Id can not be empty!", Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (!id.equals("poster") && !id.equals("receiver")) {
            Snackbar.make(binding.getRoot(), "Id can only be poster or receiver", Snackbar.LENGTH_SHORT).show();
            return;
        }

        manager.getInstance().open(id, new AVIMClientCallback() {
            @Override
            public void done(AVIMClient avimClient, AVIMException e) {
                if (null == e) {
                    Intent intent = new Intent(context, ChatActivity.class);
                    intent.putExtra(Constant.ID, id);
                    context.startActivity(intent);
                }
            }
        });
    }
}
