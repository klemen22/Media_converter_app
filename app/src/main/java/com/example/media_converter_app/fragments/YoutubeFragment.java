package com.example.media_converter_app.fragments;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import static androidx.core.content.ContentProviderCompat.requireContext;
import static androidx.core.content.ContextCompat.startActivity;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.media_converter_app.LoginActivity;
import com.example.media_converter_app.NotificationClass;
import com.example.media_converter_app.PreferencesClass;
import com.example.media_converter_app.R;
import com.example.media_converter_app.UnsafeOkHttpClient;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;

import eightbitlab.com.blurview.BlurTarget;
import eightbitlab.com.blurview.BlurView;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class YoutubeFragment extends Fragment {

    private EditText inputURL;
    private Spinner spinnerType;
    private Spinner spinnerRes;

    private Button convertButton;
    private Button downloadButton;
    private boolean alreadyRedirected = false;

    private ImageView goBackButton;
    private ImageView logOutButton;
    private ImageView infoButton;

    private BlurView blurView;
    private BlurTarget blurTarget;
    private TextView user;

    private LinearLayout ytMenuButton;

    private ImageView ytConnectionButton;

    private LinearLayout ytBlurMenu;
    private LinearLayout ytBlurConnection;

    private ImageView ytBlurBackButton;
    private ImageView ytBlurRetryButton;
    private Spinner ytBlurServerSpinner;

    String[] availableServers = {"https://192.168.64.95:9999", "https://100.104.214.108:9999"};
    private final OkHttpClient client = UnsafeOkHttpClient.getUnsafeClient();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstance) {
        View view = inflater.inflate(R.layout.fragment_youtube, container, false);

        inputURL = view.findViewById(R.id.youtubeInput);
        spinnerType = view.findViewById(R.id.youtubeDropDownType);
        spinnerRes = view.findViewById(R.id.youtubeDropDownResolution);
        convertButton = view.findViewById(R.id.youtubeConvertBtn);
        downloadButton = view.findViewById(R.id.youtubeDownloadBtn);
        user = view.findViewById(R.id.youtubeUser);
        blurView = view.findViewById(R.id.youtubeBlur);
        blurTarget = view.findViewById(R.id.youtubeBlurTarget);
        ytMenuButton = view.findViewById(R.id.youtubeMenuBtn);
        goBackButton = view.findViewById(R.id.youtubeBackButton);
        logOutButton = view.findViewById(R.id.youtubeLogOutButton);
        infoButton = view.findViewById(R.id.youtubeInfoButton);
        ytConnectionButton = view.findViewById(R.id.youtubeConnectionButton);
        ytBlurMenu = view.findViewById(R.id.youtubeBlurMenu);
        ytBlurConnection = view.findViewById(R.id.youtubeBlurConnection);
        ytBlurServerSpinner = view.findViewById(R.id.youtubeBlurServerSelect);
        ytBlurBackButton = view.findViewById(R.id.youtubeBlurBack);
        ytBlurRetryButton = view.findViewById(R.id.youtubeBlurRetry);


        tokenCheck();
        checkConnection(PreferencesClass.getServer(requireContext()));
        enableBackUI(true); // quick reset

        downloadButton.setVisibility(INVISIBLE);
        downloadButton.setClickable(false);
        convertButton.setClickable(true);
        convertButton.setVisibility(VISIBLE);

        spinnerRes.setVisibility(INVISIBLE);
        spinnerRes.setClickable(false);
        spinnerType.setVisibility(VISIBLE);
        spinnerType.setClickable(true);

        // I. dropdown menu for media type
        String[] mediaType = {"mp3", "mp4"};
        ArrayAdapter<String> adapter1 = new ArrayAdapter<>(
                requireContext(),
                R.layout.spinner_selected_item,
                mediaType
        );
        adapter1.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerType.setAdapter(adapter1);


        // II. dropdown menu for resolution
        String[] mediaRes = {"144p", "480p", "720p", "1080p"};
        ArrayAdapter<String> adapter2 = new ArrayAdapter<>(
                requireContext(),
                R.layout.spinner_selected_item,
                mediaRes
        );
        adapter2.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerRes.setAdapter(adapter2);

        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                String selectedMediaType = spinnerType.getSelectedItem().toString();

                if (selectedMediaType.equals("mp4")) {
                    spinnerRes.setVisibility(VISIBLE);
                    spinnerRes.setClickable(true);
                } else {
                    spinnerRes.setVisibility(INVISIBLE);
                    spinnerRes.setClickable(false);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        convertButton.setOnClickListener(v -> {
            convertButton.setClickable(false);
            startConverting();
        });

        ytMenuButton.setOnClickListener(v -> {
            blurBackground();
        });

        ytConnectionButton.setOnClickListener(v->{
            blurBackgroundConnection();
        });

        return view;
    }

    private void startConverting() {
        String resolution = "";
        String ytURL = inputURL.getText().toString();
        String mediaType = spinnerType.getSelectedItem().toString();
        JSONObject json = new JSONObject();

        if (ytURL.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a URL...", Toast.LENGTH_SHORT).show();
            convertButton.setClickable(true);
            return;
        }

        if (mediaType.equals("mp4")) {
            resolution = spinnerRes.getSelectedItem().toString();
        }

        try {
            json.put("url", ytURL);
            json.put("format", mediaType);
            json.put("resolution", resolution);
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        RequestBody payloadBody = RequestBody.create(json.toString(), MediaType.parse("application/json; charset=utf-8"));
        Request payload = new Request.Builder().url(PreferencesClass.getServer(requireContext()) + "/api/youtube/convert")
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + PreferencesClass.getToken(requireContext()))
                .post(payloadBody)
                .build();

        new Thread(() -> {
            try {
                Response response = client.newCall(payload).execute();

                if (!response.isSuccessful()) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                        convertButton.setClickable(true);
                    });
                    return;
                }

                assert response.body() != null;
                String responseText = response.body().string();
                Log.d("ConvertResponse", responseText);

                JSONObject jsonObject = new JSONObject(responseText);
                String fileName = jsonObject.getString("filename");

                requireActivity().runOnUiThread(() -> {
                    convertButton.setVisibility(INVISIBLE);
                    convertButton.setClickable(false);
                    downloadButton.setVisibility(VISIBLE);
                    downloadButton.setClickable(true);
                    NotificationClass.pushNotification(getContext(), "conversion", fileName);

                    downloadButton.setOnClickListener(v -> {
                        downloadButton.setClickable(false);
                        downloadYoutubeMedia(fileName);
                    });
                });

            } catch (Exception exception) {
                exception.printStackTrace();

                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Error: " + exception, Toast.LENGTH_SHORT).show()
                );
            }

        }).start();


    }

    private void downloadYoutubeMedia(String filename) {
        if (filename == null) {
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "No file ready to download...", Toast.LENGTH_SHORT).show();
                downloadButton.setVisibility(View.INVISIBLE);
                downloadButton.setClickable(false);
                convertButton.setVisibility(View.VISIBLE);
                convertButton.setClickable(true);
            });
            return;
        }

        JSONObject json = new JSONObject();

        try {
            json.put("filename", filename);
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        RequestBody payloadBody =
                RequestBody.create(json.toString(), MediaType.parse("application/json; charset=utf-8"));

        Request payload = new Request.Builder()
                .url(PreferencesClass.getServer(requireContext()) + "/api/youtube/download")
                .post(payloadBody)
                .build();

        new Thread(() -> {
            try {
                Response response = client.newCall(payload).execute();

                if (!response.isSuccessful()) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                        downloadButton.setClickable(true);
                    });
                    return;
                }

                // --- download file ---
                InputStream inputStream = response.body().byteStream();
                File downloadDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File file = new File(downloadDirectory, filename);

                FileOutputStream fileOutputStream = new FileOutputStream(file);

                byte[] buffer = new byte[4096];
                int read;

                while ((read = inputStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, read);
                }

                fileOutputStream.flush();
                fileOutputStream.close();
                inputStream.close();

                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Downloaded: " + filename, Toast.LENGTH_SHORT).show()
                );

                // delete from backend
                deleteBackendFile(filename);

            } catch (Exception exception) {
                exception.printStackTrace();

                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Error: " + exception, Toast.LENGTH_SHORT).show();
                    downloadButton.setClickable(true);
                });
            }
        }).start();
    }


    private void deleteBackendFile(String filename) {
        JSONObject json = new JSONObject();
        NotificationClass.pushNotification(getContext(), "download", filename);

        try {
            json.put("filename", filename);
        } catch (Exception ignored) {
        }

        RequestBody payloadBody =
                RequestBody.create(json.toString(), MediaType.parse("application/json; charset=utf-8"));

        Request payload = new Request.Builder()
                .url(PreferencesClass.getServer(requireContext()) + "/api/youtube/delete")
                .delete(payloadBody)
                .build();

        new Thread(() -> {
            try {
                client.newCall(payload).execute();
            } catch (Exception ignored) {
            }

            // ui update
            requireActivity().runOnUiThread(() -> {
                downloadButton.setClickable(false);
                downloadButton.setVisibility(View.INVISIBLE);

                convertButton.setVisibility(View.VISIBLE);
                convertButton.setClickable(true);

                inputURL.setText("");
            });
        }).start();
    }

    private void blurBackgroundConnection(){
        float radius = 20f;
        Drawable windowBackground = requireActivity().getWindow().getDecorView().getBackground();

        blurView.setupWith(blurTarget).setFrameClearDrawable(windowBackground).setBlurRadius(radius);
        ytBlurConnection.setVisibility(VISIBLE);
        blurView.setVisibility(VISIBLE);
        blurView.setAlpha(0f);
        blurView.animate().alpha(1f).setDuration(400).start();

        enableBackUI(false);

        ArrayAdapter<String> serverAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_selected_item, availableServers);
        serverAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        ytBlurServerSpinner.setAdapter(serverAdapter);
        int currentIndex = Arrays.stream(availableServers).toList().indexOf(PreferencesClass.getServer(requireContext()));
        ytBlurServerSpinner.setSelection(currentIndex);

        ytBlurBackButton.setOnClickListener(v->{
            blurView.setAlpha(1f);
            blurView.animate().alpha(0f).setDuration(400).start();
            blurView.setVisibility(GONE);
            ytBlurConnection.setVisibility(GONE);
            enableBackUI(true);
        });

        ytBlurRetryButton.setOnClickListener(v->{
            String selectedServer = ytBlurServerSpinner.getSelectedItem().toString();
            checkConnection(selectedServer);
        });
    }


    private void blurBackground() {
        float radius = 20f;
        Drawable windowBackground = requireActivity().getWindow().getDecorView().getBackground();

        blurView.setupWith(blurTarget).setFrameClearDrawable(windowBackground).setBlurRadius(radius);
        ytBlurMenu.setVisibility(VISIBLE);
        blurView.setVisibility(VISIBLE);
        blurView.setAlpha(0f);
        blurView.animate().alpha(1f).setDuration(400).start();

        enableBackUI(false);

        goBackButton.setOnClickListener(v -> {
            blurView.setAlpha(1f);
            blurView.animate().alpha(0f).setDuration(400).start();
            blurView.setVisibility(GONE);
            ytBlurMenu.setVisibility(GONE);
            enableBackUI(true);

        });

        logOutButton.setOnClickListener(v -> {
            PreferencesClass.clearToken(requireContext());
            PreferencesClass.clearUser(requireContext());

            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            startActivity(intent);
            requireActivity().finish();

        });

        infoButton.setOnClickListener(v -> {
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "Boomshakalaka", Toast.LENGTH_SHORT).show();
            });
        });

    }

    private void enableBackUI(boolean flag) {
        inputURL.setEnabled(flag);
        spinnerRes.setEnabled(flag);
        spinnerType.setEnabled(flag);
        convertButton.setEnabled(flag);
        downloadButton.setEnabled(flag);
        ytMenuButton.setEnabled(flag);
        ytConnectionButton.setEnabled(flag);
    }


    private void tokenCheck() {
        String savedToken = PreferencesClass.getToken(requireContext());
        if (savedToken == null) {
            redirectToLoginOnce();
            return;
        }

        String url = PreferencesClass.getServer(requireContext()) + "/api/user_info";
        Log.d("userDebug", "Checking token");

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + savedToken)
                .build();

        new Thread(() -> {
            try {
                Response response = client.newCall(request).execute();
                Log.d("userDebug", "Response: " + response);

                if (response.code() == 200) {
                    requireActivity().runOnUiThread(() -> {
                        if (isAdded()) {
                            user.setText(PreferencesClass.getUser(requireContext()));
                            Log.d("UserDebug", "User updated to: " + PreferencesClass.getUser(requireContext()));
                        }
                    });
                } else {
                    redirectToLoginOnce();
                }
            } catch (Exception exception) {
                requireActivity().runOnUiThread(()->{
                    user.setText("User");
                });
                exception.printStackTrace();
                redirectToLoginOnce();
            }
        }).start();
    }


    private void redirectToLoginOnce() {
        if (alreadyRedirected) return;
        alreadyRedirected = true;

        requireActivity().runOnUiThread(() -> {
            Toast.makeText(requireContext(), "Session expired!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });
    }

    private void checkConnection(String address) {
        String url = address + "/api/ping";
        Request request = new Request.Builder().url(url).get().build();

        new Thread(() -> {
            try {
                Response response = client.newCall(request).execute();
                if (response.code() == 200) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Connected!", Toast.LENGTH_SHORT).show();
                        PreferencesClass.setServer(requireContext(), address);
                        ytConnectionButton.setImageResource(R.drawable.link_100);
                    });
                } else {
                    requireActivity().runOnUiThread(()->{
                        Toast.makeText(requireContext(), "Error while trying to connect!", Toast.LENGTH_SHORT).show();
                        ytConnectionButton.setImageResource(R.drawable.broken_link_100);
                    });

                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }).start();

    }
}




