package step.learning.basics;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ChatActivity extends AppCompatActivity {
    private final String CHAT_URL = "https://diorama-chat.ew.r.appspot.com/story";
    private String content;
    private LinearLayout chatContainer;
    private List<ChatMessage> chatMessages;
    private ChatMessage chatMessage;
    private EditText etAuthor;
    private EditText etMessage;
    private ScrollView svContainer;
    private Handler handler;
    int tempDay = 0;
    boolean showMonthAndDay = true;
    private static final String CHANNEL_ID = "chat_channel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // new Thread(this::loadUrl).start();

        chatContainer = findViewById(R.id.chatContainer);
        svContainer = findViewById(R.id.sv_container);
        etAuthor = findViewById(R.id.etUserName);
        etMessage = findViewById(R.id.etMessage);
        findViewById(R.id.chatButtonSend).setOnClickListener(this::sendButtonClick);

        chatMessages = new ArrayList<>();
        handler = new Handler();
        handler.post(this::updateChat);
        //handler.postDelayed(this::showNotification, 2000);
    }

    private void updateChat() {
        new Thread(this::loadUrl).start();
        handler.postDelayed(this::updateChat, 2000);
    }


    private void showNotification() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.sym_def_app_icon)
                        .setContentTitle("Chat")
                        .setContentText("Message from chat")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        Notification notification = builder.build();
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(ChatActivity.this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(ChatActivity.this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 100);
                return;
            }
        }
        notificationManager.notify(1001, notification);
    }

    private void sendButtonClick(View view) {
        this.chatMessage = new ChatMessage() ;

        String author = etAuthor.getText().toString().trim();      // .trim remove whitespace from sides of a string
        String message = etMessage.getText().toString().trim();

        if (author.isEmpty()) {     // Show dialog box when username is empty

            new AlertDialog.Builder(this)
                    .setTitle("ERROR")
                    .setMessage("Username no valid!")
                    .setPositiveButton("OK",null)
                    .setNegativeButton("Cancel", null)
                    .setCancelable(false)
                    .show();

            return;
        }

        if (message.isEmpty() ) {   // Do nothing when message is empty
            return;
        }

        chatMessage.setAuthor( author ) ;
        chatMessage.setTxt( message ) ;
        etMessage.setText("");
        new Thread( this::postChatMessage ).start() ;
    }

    private void postChatMessage() {
        try {
            HttpURLConnection urlConnection =
                    (HttpURLConnection) new URL(CHAT_URL).openConnection();

            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestProperty("Accept", "*/*");
            urlConnection.setChunkedStreamingMode(0);

            OutputStream body = urlConnection.getOutputStream();
            body.write(
                    String.format(
                            "{\"author\": \"%s\", \"txt\":\"%s\"}",
                            chatMessage.getAuthor(), chatMessage.getTxt()
                    ).getBytes());
            body.flush();
            body.close();

            int responseCode = urlConnection.getResponseCode();
            if (responseCode != 200) {
                Log.d("postChatMessage", "Response code: " + responseCode);
                return;
            }
            InputStream reader = urlConnection.getInputStream();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int len;
            while ((len = reader.read(chunk)) != -1) {
                bytes.write(chunk, 0, len);
            }
            Log.d("postChatMessage",
                    new String(bytes.toByteArray(), StandardCharsets.UTF_8));
            bytes.close();
            reader.close();
            urlConnection.disconnect();
        } catch (Exception ex) {
            Log.d("postChatMessage", ex.getMessage());
        }
        loadUrl();
    }

    private void loadUrl() {
        try (InputStream inputStream = new URL(CHAT_URL).openStream()) {
            StringBuilder sb = new StringBuilder();
            int sym;
            while ((sym = inputStream.read()) != -1) {
                sb.append((char) sym);
            }
            content = new String(
                    sb.toString().getBytes(StandardCharsets.ISO_8859_1),
                    StandardCharsets.UTF_8);

            new Thread(this::parseContent).start();
        } catch (android.os.NetworkOnMainThreadException ex) {
            Log.d("loadUrl", "NetworkOnMainThreadException: " + ex.getMessage());
        } catch (MalformedURLException ex) {
            Log.d("loadUrl", "MalformedURLException: " + ex.getMessage());
        } catch (IOException ex) {
            Log.d("loadUrl", "IOException: " + ex.getMessage());
        }
    }

    private void parseContent() {
        try {
            JSONObject js = new JSONObject(content);
            JSONArray jMessages = js.getJSONArray("data");
            if ("success".equals(js.get("status"))) {

                for (int i = 0; i < jMessages.length(); ++i) {
                    ChatMessage tmp = new ChatMessage(jMessages.getJSONObject(i));

                    if (chatMessages.stream().noneMatch(obj -> obj.getId().equals(tmp.getId()))) {
                        chatMessages.add(tmp);
                    }
                }


                chatMessages.sort(Comparator.comparing(ChatMessage::getMoment));


                runOnUiThread(this::showChatMessages);
            } else {
                Log.d("parseContent",
                        "Server responses status: " + js.getString("status"));
            }
        } catch (JSONException ex) {
            Log.d("parseContent", ex.getMessage());
        }
    }

    private void showChatMessages() {

        LinearLayout.LayoutParams monthLayoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        monthLayoutParams.setMargins(7,5,7,5);
        monthLayoutParams.gravity = Gravity.CENTER;


        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM dd",Locale.US);
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:ss a",Locale.US);
        String monthAndDay;
        String time ;

        boolean needScrollDown = false;


        String author = etAuthor.getText().toString();
        for (ChatMessage chatMessage : this.chatMessages) {
            if (chatMessage.getView() != null) continue;


            if(tempDay != chatMessage.getMoment().getDay())
            {
                showMonthAndDay = true;
                tempDay = chatMessage.getMoment().getDay();
            }
            if(showMonthAndDay)
            {
                TextView tvMonthAndDay = new TextView( this );
                monthAndDay = monthFormat.format(chatMessage.getMoment());
                tvMonthAndDay.setText(monthAndDay);
                tvMonthAndDay.setTextSize(30);
                tvMonthAndDay.setTypeface(null, Typeface.BOLD);
                tvMonthAndDay.setLayoutParams(monthLayoutParams);
                tvMonthAndDay.setTextColor(Color.BLACK);
                chatContainer.addView( tvMonthAndDay ) ;
                tempDay = chatMessage.getMoment().getDay();
                showMonthAndDay = false;
            }

            LinearLayout combineMessage = new LinearLayout(this);

            TextView tvAuthor = new TextView(this);
            tvAuthor.setText(chatMessage.getAuthor());
            tvAuthor.setTypeface(null, Typeface.BOLD);

            TextView tvTextMessage = new TextView(this);
            tvTextMessage.setText(chatMessage.getTxt());


            TextView tvTime = new TextView(this);
            time = timeFormat.format(chatMessage.getMoment());
            tvTime.setText(time);
            tvTime.setTextSize(12);
            tvTime.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);

            combineMessage.addView(tvAuthor);
            combineMessage.addView(tvTextMessage);
            combineMessage.addView(tvTime);

            TextView tv = new TextView( this ) ;
            chatMessage.ChatMessageStyle(combineMessage,author,tvAuthor,tvTextMessage,tvTime);

            chatContainer.addView(combineMessage);
            chatMessage.setView(combineMessage);
            needScrollDown = true;
        }
        if (needScrollDown) {
            svContainer.post(() -> svContainer.fullScroll(View.FOCUS_DOWN));
        }
    }

    private static class ChatMessage {
        private View view;

        public View getView() {
            return view;
        }

        public void setView(View view) {
            this.view = view;
        }

        private UUID id;
        private String author;
        private String txt;
        private Date moment;
        private UUID idReply;
        private String replyPreview;
        private static final SimpleDateFormat dateFormat =
                new SimpleDateFormat("MMM dd, yyyy KK:mm:ss a", Locale.US);

        public ChatMessage() {
        }

        public ChatMessage(JSONObject obj) throws JSONException {
            setId(UUID.fromString(obj.getString("id")));
            setAuthor(obj.getString("author"));
            setTxt(obj.getString("txt"));
            try {
                setMoment(dateFormat.parse(obj.getString("moment")));
            } catch (ParseException ex) {
                throw new JSONException("Invalid moment format " + obj.getString("moment"));
            }

            if (obj.has("idReply"))
                setIdReply(UUID.fromString(obj.getString("idReply")));
            if (obj.has("replyPreview"))
                setReplyPreview(obj.getString("replyPreview"));
        }

        public void ChatMessageStyle(LinearLayout liner, String author, TextView tvAuthor, TextView tvText, TextView tvTime ){
            LinearLayout.LayoutParams incomeLayoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            incomeLayoutParams.setMargins(7,5,7,5);

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            layoutParams.setMargins(7,5,7,5);
            layoutParams.gravity = Gravity.END;


            if((this.author).equals(author)){
                liner.setBackgroundResource(R.drawable.message_bg);
                liner.setLayoutParams(layoutParams);

                tvAuthor.setTextColor(Color.WHITE);
                tvAuthor.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);


                tvText.setTextColor(Color.WHITE);
                tvText.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);

                tvTime.setTextColor(Color.rgb(229,229,229));
            }
            else{
                liner.setBackgroundResource(R.drawable.incom_message_bg);
                liner.setLayoutParams(incomeLayoutParams);

                tvAuthor.setTextColor(Color.BLACK);
                tvAuthor.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);

                tvText.setTextColor(Color.BLACK);
                tvText.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);

            }
            liner.setPadding(7, 5, 7, 5);
            liner.setOrientation(LinearLayout.VERTICAL);

        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public String getTxt() {
            return txt;
        }

        public void setTxt(String txt) {
            this.txt = txt;
        }

        public Date getMoment() {
            return moment;
        }

        public void setMoment(Date moment) {
            this.moment = moment;
        }

        public UUID getIdReply() {
            return idReply;
        }

        public void setIdReply(UUID idReply) {
            this.idReply = idReply;
        }

        public String getReplyPreview() {
            return replyPreview;
        }

        public void setReplyPreview(String replyPreview) {
            this.replyPreview = replyPreview;
        }
    }
}
