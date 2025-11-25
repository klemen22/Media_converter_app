package com.example.media_converter_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText loginUsername;
    private EditText loginPassword;
    private Button loginBtn;
    private ImageView loginConnectionStatus;
    private Spinner loginServerSelect;

    String[] availableServers = {"https://192.168.64.95:9999", "https://100.104.214.108:9999"};

    OkHttpClient client = new OkHttpClient();

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private String token = PreferencesClass.getToken(this);


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        View view = findViewById(android.R.id.content);

        view.setOnApplyWindowInsetsListener((v, insets) -> {
            int topInset = insets.getInsets(android.view.WindowInsets.Type.systemBars()).top;
            int botInset = insets.getInsets(WindowInsets.Type.systemBars()).bottom;
            v.setPadding(0, topInset, 0, botInset);
            return insets;
        });

        // check the token if present
        checkInitialToken(token);

        loginUsername = view.findViewById(R.id.loginUsername);
        loginPassword = view.findViewById(R.id.loginPassword);
        loginBtn = view.findViewById(R.id.loginBtn);
        loginConnectionStatus = view.findViewById(R.id.loginConnectionStatus);
        loginServerSelect = view.findViewById(R.id.loginServerSelect);


    }


    private void checkInitialToken(String token) {
        String url = PreferencesClass.getServer(this) + "/api/user_info";

        Request request = new Request.Builder().url(url).get().addHeader("Authorization", "Bearer " + token).build();

        new Thread(() -> {

            try {
                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                assert response.body() != null;
                String responseText = response.body().string();
                JSONObject jsonObject = new JSONObject(responseText);

                if (jsonObject.getString("status").equals("success")) {
                    PreferencesClass.setUser(this, jsonObject.getString("user"));

                    runOnUiThread(() -> {
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    });

                } else {
                    PreferencesClass.clearToken(this);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Session expired!", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

            } catch (Exception exception) {
                exception.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error: " + exception, Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}