package com.example.media_converter_app;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.security.cert.CertPathBuilderSpi;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import eightbitlab.com.blurview.BlurTarget;
import eightbitlab.com.blurview.BlurView;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText loginUsername;
    private EditText loginPassword;
    private Button loginBtn;
    private ImageView loginConnectionStatus;
    private Spinner loginServerSelect;

    private BlurView loginBlurView;
    private BlurTarget loginBlurTarget;

    private ImageView loginRetryBtn;
    private ImageView loginBackBtn;

    String[] availableServers = {"https://192.168.64.95:9999", "https://100.104.214.108:9999"};

    OkHttpClient client = UnsafeOkHttpClient.getUnsafeClient();

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        View view = findViewById(android.R.id.content);

        view.setOnApplyWindowInsetsListener((v, insets) -> {
            int topInset = insets.getInsets(android.view.WindowInsets.Type.systemBars()).top;
            int botInset = insets.getInsets(WindowInsets.Type.systemBars()).bottom;
            v.setPadding(0, topInset, 0, botInset);
            return insets;
        });

        // check the token if present
        String token = PreferencesClass.getToken(this);
        if (token != null) {
            Log.d("TokenDebug", "Checking token...");
            autoLogin(token);
        }

        loginUsername = findViewById(R.id.loginUsername);
        loginPassword = findViewById(R.id.loginPassword);
        loginBtn = findViewById(R.id.loginBtn);
        loginConnectionStatus = findViewById(R.id.loginConnectionStatus);
        loginServerSelect = findViewById(R.id.loginServerSelect);
        loginBlurView = findViewById(R.id.loginBlur);
        loginBlurTarget = findViewById(R.id.loginBlurTarget);
        loginRetryBtn = findViewById(R.id.loginRetry);
        loginBackBtn = findViewById(R.id.loginBack);

        // check connection to server
        checkConnection(PreferencesClass.getServer(this));

        // quick reset
        enableUI(true);

        loginBtn.setOnClickListener(v -> {
            Log.d("LoginDebug", "Login button was pressed");
            Login();
        });

        loginConnectionStatus.setOnClickListener(v->{
            blurBackground();
        });
    }


    private void autoLogin(String token) {
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

                    Log.d("TokenDebug", "User" + jsonObject.getString("user"));
                    Log.d("TokenDebug", "Token:" + token);

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

    private void Login() {
        String username = loginUsername.getText().toString();
        String password = loginPassword.getText().toString();

        String url = PreferencesClass.getServer(this) + "/api/login";
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("username", username);
            jsonObject.put("password", password);
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        RequestBody requestBody = RequestBody.create(jsonObject.toString(), MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder().url(url).post(requestBody).build();

        new Thread(() -> {
            try {
                Response response = client.newCall(request).execute();
                assert response.body() != null;
                String responseText = response.body().string();
                JSONObject responseJsonObject = new JSONObject(responseText);

                if (responseJsonObject.getString("status").equals("success")) {
                    PreferencesClass.setUser(this, username);
                    PreferencesClass.setToken(this, responseJsonObject.getString("access_token"));

                    // debug section
                    Log.d("LoginDebug", "Login username: " + username);
                    Log.d("LoginDebug", "Login token: " + responseJsonObject.getString("access_token"));

                    runOnUiThread(() -> {
                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    });

                } else {
                    Log.d("LoginDebug", "status: " + responseJsonObject.getString("status"));
                    Log.d("LoginDebug", "Message: " + responseJsonObject.getString("message"));

                    String message = responseJsonObject.getString("message"); // separate string because of exception

                    runOnUiThread(() -> {
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                        return;
                    });
                }

            } catch (Exception exception) {
                exception.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error: " + exception, Toast.LENGTH_SHORT).show();
                });
            }
        }).start();

    }

    private void blurBackground(){
        float radius = 20f;
        Drawable windowBackground = getWindow().getDecorView().getBackground();

        loginBlurView.setupWith(loginBlurTarget).setFrameClearDrawable(windowBackground);
        loginBlurView.setVisibility(VISIBLE);
        loginBlurView.setAlpha(0f);
        loginBlurView.animate().alpha(1f).setDuration(400).start();

        enableUI(false);

        ArrayAdapter<String> serverAdapter = new ArrayAdapter<>(this, R.layout.spinner_selected_item, availableServers);
        serverAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        loginServerSelect.setAdapter(serverAdapter);
        int currentIndex = Arrays.stream(availableServers).toList().indexOf(PreferencesClass.getServer(this));
        loginServerSelect.setSelection(currentIndex);

        loginRetryBtn.setOnClickListener(v->{
            String selectedServer = loginServerSelect.getSelectedItem().toString();
            checkConnection(selectedServer);
        });

        loginBackBtn.setOnClickListener(v->{
            loginBlurView.setAlpha(1f);
            loginBlurView.animate().alpha(0f).setDuration(400).start();
            loginBlurView.setVisibility(GONE);
            enableUI(true);
        });
    }

    private void checkConnection(String address){
        String url = address + "/api/ping";
        Request request = new Request.Builder().url(url).get().build();

        new Thread(()->{
            try{
                Response response = client.newCall(request).execute();
                if(response.code() == 200){
                    runOnUiThread(()->{
                        Toast.makeText(this, "Connected!", Toast.LENGTH_SHORT).show();
                        PreferencesClass.setServer(this, address);
                        loginConnectionStatus.setImageResource(R.drawable.link_100);
                    });
                }
                else{
                    runOnUiThread(()->{
                        Toast.makeText(this, "Error while trying to connect!", Toast.LENGTH_SHORT).show();
                        loginConnectionStatus.setImageResource(R.drawable.link_100);
                    });
                }
            } catch (Exception exception){
                exception.printStackTrace();
            }
        }).start();

    }

    private void enableUI(boolean flag){
        loginUsername.setEnabled(flag);
        loginPassword.setEnabled(flag);
        loginBtn.setEnabled(flag);
        loginConnectionStatus.setEnabled(flag);
    }


}