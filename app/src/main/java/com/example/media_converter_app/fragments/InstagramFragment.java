package com.example.media_converter_app.fragments;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.media_converter_app.LoginActivity;
import com.example.media_converter_app.MainActivity;
import com.example.media_converter_app.NotificationClass;
import com.example.media_converter_app.PreferencesClass;
import com.example.media_converter_app.R;
import com.example.media_converter_app.UnsafeOkHttpClient;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import eightbitlab.com.blurview.BlurTarget;
import eightbitlab.com.blurview.BlurView;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class InstagramFragment extends Fragment {

    private final OkHttpClient client = UnsafeOkHttpClient.getUnsafeClient();
    String[] availableServers = {"https://192.168.64.95:9999", "https://100.104.214.108:9999"};
    private EditText inputURL;
    private Spinner spinnerType;
    private Button convertButton;
    private Button downloadButton;
    private boolean alreadyRedirected = false;
    private ImageView instaBlurMenuBackButton;
    private ImageView instaBlurMenuLogOutButton;
    private ImageView instaBlurMenuInfoButton;
    private BlurView blurView;
    private BlurTarget blurTarget;
    private TextView instaUser;
    private LinearLayout instaMenuButton;
    private ImageView instaConnectionButton;
    private LinearLayout instaBlurMenu;
    private LinearLayout instaBlurConnection;
    private ImageView instaConnectionBackButton;
    private ImageView instaConnectionRetryButton;
    private Spinner instaConnectionServerSpinner;
    private ProgressBar instaProgressBar;
    private TextView instaProgressText;
    private ProgressBar instaReconnectBar;

    private Runnable timeoutRun;
    private Call currentPingCall;
    private final android.os.Handler timeoutHandler = new android.os.Handler();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstance) {

        View view = inflater.inflate(R.layout.fragment_instagram, container, false);

        inputURL = view.findViewById(R.id.instagramInput);
        spinnerType = view.findViewById(R.id.instagramDropDownType);
        convertButton = view.findViewById(R.id.instagramConvertBtn);
        downloadButton = view.findViewById(R.id.instagramDownloadBtn);

        instaUser = view.findViewById(R.id.instagramUser);
        blurView = view.findViewById(R.id.instagramBlur);
        blurTarget = view.findViewById(R.id.instagramBlurTarget);
        instaMenuButton = view.findViewById(R.id.instagramMenuBtn);
        instaBlurMenu = view.findViewById(R.id.instagramBlurMenu);
        instaBlurMenuBackButton = view.findViewById(R.id.instagramBackButton);
        instaBlurMenuLogOutButton = view.findViewById(R.id.instagramLogOutButton);
        instaBlurMenuInfoButton = view.findViewById(R.id.instagramInfoButton);

        instaConnectionButton = view.findViewById(R.id.instagramConnectionButton);
        instaBlurMenu = view.findViewById(R.id.instagramBlurMenu);
        instaBlurConnection = view.findViewById(R.id.instagramBlurConnection);
        instaConnectionServerSpinner = view.findViewById(R.id.instagramBlurServerSelect);
        instaConnectionBackButton = view.findViewById(R.id.instagramBlurBack);
        instaConnectionRetryButton = view.findViewById(R.id.instagramBlurRetry);

        instaProgressBar = view.findViewById(R.id.instagramProgressBar);
        instaProgressText = view.findViewById(R.id.instagramTextProgress);

        instaReconnectBar = view.findViewById(R.id.instagramReconnectBar);

        // quick reset
        tokenCheck();
        checkConnection(PreferencesClass.getServer(requireContext()), false);
        enableBackUI(true);
        enableConnectionUI(true);
        unlockPager();
        instaConnectionRetryButton.setImageResource(R.drawable.retry_50);

        instaProgressBar.setVisibility(INVISIBLE);
        instaProgressText.setVisibility(INVISIBLE);
        instaReconnectBar.setVisibility(INVISIBLE);

        downloadButton.setVisibility(INVISIBLE);
        downloadButton.setClickable(false);
        convertButton.setVisibility(VISIBLE);
        convertButton.setClickable(true);

        // dropdown menu for media type
        String[] mediaType = {"picture", "video"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.spinner_selected_item,
                mediaType
        );
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerType.setAdapter(adapter);

        convertButton.setOnClickListener(v -> {
            convertButton.setClickable(false);
            startConverting();
        });

        instaMenuButton.setOnClickListener(v -> blurBackground());

        instaConnectionButton.setOnClickListener(v -> blurBackgroundConnection());

        return view;
    }

    @SuppressWarnings("CallToPrintStackTrace")
    private void startConverting() {

        String instaURL = inputURL.getText().toString();
        String mediaType = spinnerType.getSelectedItem().toString();
        JSONObject json = new JSONObject();

        instaProgressBar.setVisibility(VISIBLE);
        instaProgressText.setText(R.string.converting);
        instaProgressText.setVisibility(VISIBLE);

        if (instaURL.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a URL...", Toast.LENGTH_SHORT).show();
            convertButton.setClickable(true);
            instaProgressBar.setVisibility(INVISIBLE);
            instaProgressText.setVisibility(INVISIBLE);
            return;
        }

        try {
            json.put("url", instaURL);
            json.put("type", mediaType);
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        RequestBody payloadBody = RequestBody.create(json.toString(), MediaType.parse("application/json; charset=utf-8"));
        Request payload = new Request.Builder().url(PreferencesClass.getServer(requireContext()) + "/api/instagram/convert")
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

                    instaProgressBar.setVisibility(INVISIBLE);
                    instaProgressText.setVisibility(INVISIBLE);

                    downloadButton.setOnClickListener(v -> {
                        downloadButton.setClickable(false);
                        downloadInstagramContent(fileName);
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

    @SuppressWarnings("CallToPrintStackTrace")
    private void downloadInstagramContent(String filename) {
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


        instaProgressBar.setVisibility(VISIBLE);
        instaProgressText.setText(R.string.downloading);
        instaProgressText.setVisibility(VISIBLE);


        JSONObject json = new JSONObject();

        try {
            json.put("filename", filename);
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        RequestBody payloadBody = RequestBody.create(json.toString(), MediaType.parse("application/json; charset=utf-8"));

        Request payload = new Request.Builder()
                .url(PreferencesClass.getServer(requireContext()) + "/api/instagram/download")
                .post(payloadBody)
                .build();

        new Thread(() -> {
            try {
                Response response = client.newCall(payload).execute();

                if (!response.isSuccessful()) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                        downloadButton.setClickable(true);
                        instaProgressText.setVisibility(INVISIBLE);
                        instaProgressBar.setVisibility(INVISIBLE);
                    });
                    return;
                }

                // --- download file ---
                assert response.body() != null;
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

                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Downloaded: " + filename, Toast.LENGTH_SHORT).show();
                    instaProgressBar.setVisibility(INVISIBLE);
                    instaProgressText.setVisibility(INVISIBLE);
                });

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

        RequestBody payloadBody = RequestBody.create(json.toString(), MediaType.parse("application/json; charset=utf-8"));

        Request payload = new Request.Builder()
                .url(PreferencesClass.getServer(requireContext()) + "/api/instagram/delete")
                .delete(payloadBody)
                .build();

        new Thread(() -> {
            try {
                client.newCall(payload).execute();
            } catch (Exception ignored) {
            }

            requireActivity().runOnUiThread(() -> {
                downloadButton.setClickable(false);
                downloadButton.setVisibility(View.INVISIBLE);

                convertButton.setVisibility(View.VISIBLE);
                convertButton.setClickable(true);

                inputURL.setText("");
            });

        }).start();
    }

    private void blurBackgroundConnection() {
        lockPager();

        float radius = 20f;
        Drawable windowBackground = requireActivity().getWindow().getDecorView().getBackground();

        blurView.setupWith(blurTarget).setFrameClearDrawable(windowBackground).setBlurRadius(radius);
        instaBlurConnection.setVisibility(VISIBLE);
        blurView.setVisibility(VISIBLE);
        blurView.setAlpha(0f);
        blurView.animate().alpha(1f).setDuration(400).start();

        enableBackUI(false);

        ArrayAdapter<String> serverAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_selected_item, availableServers);
        serverAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        instaConnectionServerSpinner.setAdapter(serverAdapter);

        int currentIndex = Arrays.stream(availableServers).toList().indexOf(PreferencesClass.getServer(requireContext()));
        instaConnectionServerSpinner.setSelection(currentIndex);

        instaConnectionBackButton.setOnClickListener(v -> {
            unlockPager();

            blurView.setAlpha(1f);
            blurView.animate().alpha(0f).setDuration(400).start();
            blurView.setVisibility(GONE);
            instaBlurConnection.setVisibility(GONE);
            enableBackUI(true);
        });

        instaConnectionRetryButton.setOnClickListener(v -> {
            String selectedServer = instaConnectionServerSpinner.getSelectedItem().toString();
            enableConnectionUI(false);
            checkConnection(selectedServer, true);
        });

    }

    private void blurBackground() {
        lockPager();

        float radius = 20f;
        Drawable windowBackground = requireActivity().getWindow().getDecorView().getBackground();

        blurView.setupWith(blurTarget).setFrameClearDrawable(windowBackground).setBlurRadius(radius);
        instaBlurMenu.setVisibility(VISIBLE);
        blurView.setVisibility(VISIBLE);
        blurView.setAlpha(0f);
        blurView.animate().alpha(1f).setDuration(400).start();

        enableBackUI(false);

        instaBlurMenuBackButton.setOnClickListener(v -> {
            unlockPager();

            blurView.setAlpha(1f);
            blurView.animate().alpha(0f).setDuration(400).start();
            blurView.setVisibility(GONE);
            instaBlurMenu.setVisibility(GONE);
            enableBackUI(true);
        });

        instaBlurMenuLogOutButton.setOnClickListener(v -> {
            unlockPager();

            PreferencesClass.clearToken(requireContext());
            PreferencesClass.clearUser(requireContext());

            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            startActivity(intent);
            requireActivity().finish();
        });

        instaBlurMenuInfoButton.setOnClickListener(v -> requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Boomshakalaka", Toast.LENGTH_SHORT).show()));
    }

    private void enableBackUI(boolean flag) {
        inputURL.setEnabled(flag);
        spinnerType.setEnabled(flag);
        convertButton.setEnabled(flag);
        downloadButton.setEnabled(flag);
        instaMenuButton.setEnabled(flag);
        instaConnectionButton.setEnabled(flag);
    }

    private void enableConnectionUI(boolean flag) {
        instaConnectionRetryButton.setEnabled(flag);
        instaConnectionBackButton.setEnabled(flag);
    }

    @SuppressWarnings("CallToPrintStackTrace")
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

                assert response.body() != null;
                String responseText = response.body().string();
                JSONObject jsonObject = new JSONObject(responseText);
                String status = jsonObject.getString("status");

                if (response.code() == 200 && !status.equals("error")) {
                    requireActivity().runOnUiThread(() -> {
                        if (isAdded()) {
                            instaUser.setText(PreferencesClass.getUser(requireContext()));
                            Log.d("UserDebug", "User updated to: " + PreferencesClass.getUser(requireContext()));
                        }
                    });
                } else {
                    redirectToLoginOnce();
                }
            } catch (Exception exception) {
                requireActivity().runOnUiThread(() -> instaUser.setText(R.string.user));
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

    @SuppressWarnings("CallToPrintStackTrace")
    private void checkConnection(String address, Boolean blurFlag) {
        String url = address + "/api/ping";
        OkHttpClient pingClient = client.newBuilder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).callTimeout(15, TimeUnit.SECONDS).build();
        Request request = new Request.Builder().url(url).get().build();
        currentPingCall = pingClient.newCall(request);

        if (blurFlag) {
            requireActivity().runOnUiThread(() -> {
                instaReconnectBar.setVisibility(VISIBLE);
                instaConnectionRetryButton.setImageResource(R.drawable.close_50);
                enableConnectionUI(false);
            });

            timeoutRun = () -> {
                if (currentPingCall != null && !currentPingCall.isCanceled()) {
                    currentPingCall.cancel();
                }
            };
            timeoutHandler.postDelayed(timeoutRun, 60_000);
        }

        new Thread(() -> {
            try {
                instaConnectionRetryButton.setOnClickListener(v -> {
                    if (currentPingCall != null && !currentPingCall.isCanceled()) {
                        currentPingCall.cancel();
                    }
                    timeoutHandler.removeCallbacks(timeoutRun);
                });

                Response response = currentPingCall.execute();

                if (blurFlag) {
                    timeoutHandler.removeCallbacks(timeoutRun);
                }

                if (response.code() == 200) {
                    requireActivity().runOnUiThread(() -> {
                        PreferencesClass.setServer(requireContext(), address);
                        instaConnectionButton.setImageResource(R.drawable.link_100);

                        if (blurFlag) {
                            instaReconnectBar.setVisibility(INVISIBLE);
                            instaConnectionRetryButton.setImageResource(R.drawable.retry_50);
                            enableConnectionUI(true);
                            Toast.makeText(requireContext(), "Connected!", Toast.LENGTH_SHORT).show();
                        }

                        instaConnectionRetryButton.setOnClickListener(v -> {
                            String selectedServer = instaConnectionServerSpinner.getSelectedItem().toString();
                            enableConnectionUI(false);
                            checkConnection(selectedServer, true);
                        });
                    });
                } else {
                    requireActivity().runOnUiThread(() -> {
                        PreferencesClass.setServer(requireContext(), address);
                        instaConnectionButton.setImageResource(R.drawable.broken_link_100);

                        if (blurFlag) {
                            instaReconnectBar.setVisibility(INVISIBLE);
                            instaConnectionRetryButton.setImageResource(R.drawable.retry_50);
                            enableConnectionUI(true);
                        }

                        instaConnectionRetryButton.setOnClickListener(v -> {
                            String selectedServer = instaConnectionServerSpinner.getSelectedItem().toString();
                            enableConnectionUI(false);
                            checkConnection(selectedServer, true);
                        });
                    });

                }
            } catch (Exception exception) {
                exception.printStackTrace();
                PreferencesClass.setServer(requireContext(), address);
                instaConnectionButton.setImageResource(R.drawable.broken_link_100);

                if (blurFlag) {
                    timeoutHandler.removeCallbacks(timeoutRun);

                    requireActivity().runOnUiThread(() -> {
                        instaReconnectBar.setVisibility(INVISIBLE);
                        instaConnectionRetryButton.setImageResource(R.drawable.retry_50);
                        enableConnectionUI(true);
                        Toast.makeText(requireContext(), "Error while trying to connect!", Toast.LENGTH_SHORT).show();
                    });

                    instaConnectionRetryButton.setOnClickListener(v -> {
                        String selectedServer = instaConnectionServerSpinner.getSelectedItem().toString();
                        enableConnectionUI(false);
                        checkConnection(selectedServer, true);
                    });
                }
            }
        }).start();
    }

    private void lockPager() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).lockNavigation();
        }
    }

    private void unlockPager() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).unlockNavigation();
        }
    }

}
