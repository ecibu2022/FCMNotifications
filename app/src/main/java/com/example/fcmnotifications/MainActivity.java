package com.example.fcmnotifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.annotations.NotNull;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    Button sendNotificationButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sendNotificationButton=findViewById(R.id.sendNotificationButton);

        sendNotificationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendNotification("Hello World");
            }
        });


    }

//    Send Notification Method to a specific user
    private void sendNotification(String notificationTitle) {
        DatabaseReference adminTokenRef = FirebaseDatabase.getInstance().getReference().child("users");
        Query adminQuery = adminTokenRef.orderByChild("role").equalTo("admin");
        adminQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot adminSnapshot : dataSnapshot.getChildren()) {
                    String adminToken = adminSnapshot.child("deviceToken").getValue(String.class);
                    String name = adminSnapshot.child("role").getValue(String.class);
                    Log.d("Admin Token: ", adminToken);
                    Log.d("Role", name);
                    if (adminToken != null) {
                        // Send the notification using FCM
                        sendFCMNotificationToAdmin(adminToken, notificationTitle);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle any database error that occurred while fetching the data
            }
        });
    }

    private void sendFCMNotificationToAdmin(String adminToken, String notificationTitle) {
        // Set the FCM server key from Firebase Console
        String serverKey = "YOUR_SERVER_KEY";

        // Create the FCM message data payload (customize as needed)
        Map<String, String> data = new HashMap<>();
        data.put("title", "New Message");
        data.put("body", notificationTitle);

        // Create the FCM message body
        Map<String, Object> message = new HashMap<>();
        message.put("to", adminToken);
        message.put("data", data);

        // Send the FCM message using OkHttp
        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, new Gson().toJson(message));
        Request request = new Request.Builder()
                .url("https://fcm.googleapis.com/fcm/send")
                .post(body)
                .addHeader("Authorization", "key=" + serverKey)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e("FCM", "Failed to send notification to admin", e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d("FCM", "Notification sent to admin");
                } else {
                    Log.e("FCM", "Failed to send notification to admin");
                }
                response.close();
            }
        });
    }

}
