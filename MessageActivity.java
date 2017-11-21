package com.example.administrator.mycampus.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.example.administrator.mycampus.R;
import com.example.administrator.mycampus.adapter.EmotionAdapter;
import com.example.administrator.mycampus.adapter.MessageAdapter;
import com.example.administrator.mycampus.cache.MyCache;
import com.example.administrator.mycampus.info.MsgInfo;
import com.example.administrator.mycampus.util.MyUtils;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.Observer;
import com.netease.nimlib.sdk.msg.MessageBuilder;
import com.netease.nimlib.sdk.msg.MsgService;
import com.netease.nimlib.sdk.msg.MsgServiceObserve;
import com.netease.nimlib.sdk.msg.constant.SessionTypeEnum;
import com.netease.nimlib.sdk.msg.model.IMMessage;

import java.lang.reflect.Field;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.example.administrator.mycampus.util.MyUtils.myExpressionString;

public class MessageActivity extends AppCompatActivity implements View.OnClickListener{
    private ListView messageList;
    private EditText editMessage;
    private Button emotionButton;
    private Button sendButton;
    private Button backButton;
    private Html.ImageGetter myImageGetter;
    private Html.ImageGetter myReceiveImageGetter;
    private EmotionAdapter myEmotionAdapter;
    private GridView emotionGridView;
    private Spanned mySpanned;
    private String receiverId;
    private String receive_text;
    private MessageAdapter messageAdapter;
    private InputMethodManager myInputMethodManager;
    private Observer<List<IMMessage>> incomingMessageObserver;
    private TextView userId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);
        handelReceiveMessage();
        NIMClient.getService(MsgServiceObserve.class).observeReceiveMessage(incomingMessageObserver, true);
        Intent intent=getIntent();
        receiverId=intent.getStringExtra("receiveId");
        init();
        getImageGetter();

        setClickListener();
        messageAdapter=new MessageAdapter(this,myImageGetter,myReceiveImageGetter);
    }
    //***********注销消息接受观察者****************
    @Override
    protected void onDestroy() {
        super.onDestroy();
        NIMClient.getService(MsgServiceObserve.class).observeReceiveMessage(incomingMessageObserver, false);
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.send_button:
                sendMessage();
                break;
            case R.id.back_button:
                finish();
                break;
            case R.id.emoticon:
                if (emotionGridView.getVisibility() == View.VISIBLE) {
                    emotionGridView.setVisibility(View.GONE);
                } else {
                    emotionGridView.setVisibility(View.VISIBLE);
                    myInputMethodManager.hideSoftInputFromWindow(editMessage.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                }
                break;
            default:
                break;

        }
    }

    private void init(){
        messageList=(ListView)findViewById(R.id.listview);
        editMessage=(EditText)findViewById(R.id.edit_message);
        backButton=(Button)findViewById(R.id.back_button);
        sendButton=(Button)findViewById(R.id.send_button);
        emotionButton=(Button)findViewById(R.id.emoticon);
        userId=(TextView)findViewById(R.id.user_id);
        emotionGridView=(GridView)findViewById(R.id.emotion_gridView);
        myInputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        myEmotionAdapter = new EmotionAdapter(getLayoutInflater());
        emotionGridView.setAdapter(myEmotionAdapter);
    }

    private void setClickListener(){
        sendButton.setOnClickListener(this);
        backButton.setOnClickListener(this);
        emotionButton.setOnClickListener(this);
        editMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (emotionGridView.getVisibility() == View.VISIBLE) {
                    emotionGridView.setVisibility(View.GONE);
                }
            }
        });

        emotionGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                mySpanned = Html.fromHtml("<img src='" + myExpressionString[position] + "'/>", myImageGetter, null);
                editMessage.getText().insert(editMessage.getSelectionStart(), mySpanned);
            }
        });
    }

    private void sendMessage(){
        String send = MyUtils.filterHtml(Html.toHtml(editMessage.getText()));
        IMMessage message = MessageBuilder.createTextMessage(receiverId, SessionTypeEnum.P2P, send);
        NIMClient.getService(MsgService.class).sendMessage(message, false);
        editMessage.setText(null);
        Log.d("这是发出的消息内容", "" + send);
        messageAdapter.addDataToAdapter(new MsgInfo(null, send, MyCache.getAccount()));
        messageAdapter.notifyDataSetChanged();
        messageList.smoothScrollToPosition(messageList.getCount() - 1);
    }

    private void handelReceiveMessage(){
         incomingMessageObserver =
                new Observer<List<IMMessage>>() {
                    @Override
                    public void onEvent(List<IMMessage> messages) {
                        for (IMMessage message : messages) {
                            receive_text = message.getContent();                    //接受到的文本消息内容
                        }
                        SpannableString spannableString = new SpannableString(receive_text);
                        String regexEmotion = "(([abc]{2})[a-z]){0,}";
                        Pattern patternEmotion = Pattern.compile(regexEmotion);
                        Matcher matcherEmotion = patternEmotion.matcher(spannableString);
                        while (matcherEmotion.find()) {
                            myReceiveImageGetter = new Html.ImageGetter() {
                                @Override
                                public Drawable getDrawable(String source) {
                                    Drawable drawable = null;
                                    int id = R.drawable.aaa;
                                    if (source != null) {
                                        Class clazz = R.drawable.class;
                                        try {
                                            Field field = clazz.getDeclaredField(source);
                                            id = field.getInt(source);

                                        } catch (NoSuchFieldException e) {
                                            e.printStackTrace();
                                        } catch (IllegalAccessException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    drawable = getResources().getDrawable(id);
                                    drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
                                    return drawable;
                                }
                            };
                        }
                        messageAdapter.addDataToAdapter(new MsgInfo(receive_text, null,null));
                        messageAdapter.notifyDataSetChanged();
                        receive_text = null;
                        messageList.smoothScrollToPosition(messageList.getCount() - 1);
                    }
                };
    }

    private void getImageGetter(){
        myImageGetter =  new Html.ImageGetter() {
            @Override
            public Drawable getDrawable(String s) {
                Drawable drawable = null;
                int id = R.drawable.aaa;
                if (s != null) {
                    Class clazz = R.drawable.class;
                    try {
                        Field field = clazz.getDeclaredField(s);
                        id = field.getInt(s);

                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }

                }
                drawable = getResources().getDrawable(id);
                drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
                return drawable;
            }
        };
    }


}
