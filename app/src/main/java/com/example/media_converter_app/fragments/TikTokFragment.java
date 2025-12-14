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

public class TikTokFragment extends Fragment {

    private final OkHttpClient client = UnsafeOkHttpClient.getUnsafeClient();
    String[] availableServers = {"https://192.168.64.95:9999", "https://100.104.214.108:9999"};
    private EditText inputURL;
    private Button convertButton;
    private Button downloadButton;
    private boolean alreadyRedirected = false;
    private TextView tiktokUser;
    private BlurView blurView;
    private BlurTarget blurTarget;
    private LinearLayout tiktokBlurMenu;
    private ImageView tiktokBlurMenuBack;
    private ImageView tiktokBlurLogOut;
    private ImageView tiktokBlurInfo;
    private ImageView tiktokConnectionButton;
    private LinearLayout tiktokConnectionMenu;
    private Spinner tiktokConnectionSpinner;
    private ImageView tiktokConnectionRetry;
    private ImageView tiktokConnectionBack;
    private LinearLayout tiktokMenuButton;
    private ProgressBar tiktokProgressBar;
    private TextView tiktokProgressText;
    private ProgressBar tiktokReconnectBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstance) {
        View view = inflater.inflate(R.layout.fragment_tiktok, container, false);

        inputURL = view.findViewById(R.id.tiktokInput);
        convertButton = view.findViewById(R.id.tiktokConvertBtn);
        downloadButton = view.findViewById(R.id.tiktokDownloadBtn);

        tiktokUser = view.findViewById(R.id.tiktokUser);
        blurView = view.findViewById(R.id.tiktokBlur);
        blurTarget = view.findViewById(R.id.tiktokBlurTarget);
        tiktokMenuButton = view.findViewById(R.id.tiktokMenuButton);
        tiktokBlurMenu = view.findViewById(R.id.tiktokBlurMenu);
        tiktokBlurMenuBack = view.findViewById(R.id.tiktokBlurBackButton);
        tiktokBlurLogOut = view.findViewById(R.id.tiktokBlurLogOutButton);
        tiktokBlurInfo = view.findViewById(R.id.tiktokBlurInfoButton);

        tiktokConnectionButton = view.findViewById(R.id.tiktokConnectionButton);
        tiktokConnectionMenu = view.findViewById(R.id.tiktokBlurConnection);
        tiktokConnectionSpinner = view.findViewById(R.id.tiktokConnectionServerSelect);
        tiktokConnectionRetry = view.findViewById(R.id.tiktokConnectionRetry);
        tiktokConnectionBack = view.findViewById(R.id.tiktokConnectionBack);

        tiktokProgressBar = view.findViewById(R.id.tiktokProgressBar);
        tiktokProgressText = view.findViewById(R.id.tiktokTextProgress);

        tiktokReconnectBar = view.findViewById(R.id.tiktokReconnectBar);

        tokenCheck();
        checkConnection(PreferencesClass.getServer(requireContext()), false);
        enableBackUI(true);
        tiktokProgressText.setVisibility(INVISIBLE);
        tiktokProgressBar.setVisibility(INVISIBLE);
        tiktokReconnectBar.setVisibility(INVISIBLE);

        downloadButton.setVisibility(INVISIBLE);
        downloadButton.setClickable(false);
        convertButton.setVisibility(VISIBLE);
        convertButton.setClickable(true);

        convertButton.setOnClickListener(v -> {
            convertButton.setClickable(false);
            startConverting();
        });

        tiktokMenuButton.setOnClickListener(v -> {
            blurBackground();
        });

        tiktokConnectionButton.setOnClickListener(v -> {
            blurBackgroundConnection();
        });

        return view;
    }

    private void startConverting() {
        String tikURL = inputURL.getText().toString();
        JSONObject json = new JSONObject();
        tiktokProgressBar.setVisibility(VISIBLE);
        tiktokProgressText.setText("Converting...");
        tiktokProgressText.setVisibility(VISIBLE);

        if (tikURL.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a URL...", Toast.LENGTH_SHORT).show();
            convertButton.setClickable(true);
            tiktokProgressText.setVisibility(INVISIBLE);
            tiktokProgressBar.setVisibility(INVISIBLE);
            return;
        }

        try {
            json.put("url", tikURL);
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        RequestBody payloadBody = RequestBody.create(json.toString(), MediaType.parse("application/json; charset=utf-8"));
        Request payload = new Request.Builder().url(PreferencesClass.getServer(requireContext()) + "/api/tiktok/convert")
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
                        tiktokProgressBar.setVisibility(INVISIBLE);
                        tiktokProgressText.setVisibility(INVISIBLE);
                    });
                    return;
                }

                assert response.body() != null;
                String responseText = response.body().string();

                JSONObject jsonObject = new JSONObject(responseText);
                String filename = jsonObject.getString("filename");

                requireActivity().runOnUiThread(() -> {
                    convertButton.setVisibility(INVISIBLE);
                    convertButton.setClickable(false);
                    downloadButton.setVisibility(VISIBLE);
                    downloadButton.setClickable(true);
                    NotificationClass.pushNotification(getContext(), "conversion", filename);
                    tiktokProgressBar.setVisibility(INVISIBLE);
                    tiktokProgressText.setVisibility(INVISIBLE);

                    downloadButton.setOnClickListener(v -> {
                        downloadButton.setClickable(false);
                        downloadTikTikMedia(filename);
                    });

                });


            } catch (Exception exception) {
                exception.printStackTrace();

                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Error: " + exception, Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void downloadTikTikMedia(String filename) {
        if (filename == null) {
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "No file ready to download...", Toast.LENGTH_SHORT).show();
                downloadButton.setVisibility(INVISIBLE);
                downloadButton.setClickable(false);
                convertButton.setVisibility(VISIBLE);
                convertButton.setClickable(true);
            });
            return;
        }


        tiktokProgressBar.setVisibility(VISIBLE);
        tiktokProgressText.setText("Downloading...");
        tiktokProgressText.setVisibility(VISIBLE);

        JSONObject json = new JSONObject();

        try {
            json.put("filename", filename);
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        RequestBody payloadBody = RequestBody.create(json.toString(), MediaType.parse("application/json; charset=utf-8"));

        Request payload = new Request.Builder()
                .url(PreferencesClass.getServer(requireContext()) + "/api/tiktok/download")
                .post(payloadBody)
                .build();

        new Thread(() -> {
            try {
                Response response = client.newCall(payload).execute();

                if (!response.isSuccessful()) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                        downloadButton.setClickable(true);
                        tiktokProgressText.setVisibility(INVISIBLE);
                        tiktokProgressBar.setVisibility(INVISIBLE);
                    });
                    return;
                }

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
                    tiktokProgressBar.setVisibility(INVISIBLE);
                    tiktokProgressText.setVisibility(INVISIBLE);


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
                .url(PreferencesClass.getServer(requireContext()) + "/api/tiktok/delete")
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

    private void blurBackground() {
        float radius = 20f;
        Drawable windowBackground = requireActivity().getWindow().getDecorView().getBackground();

        blurView.setupWith(blurTarget).setFrameClearDrawable(windowBackground).setBlurRadius(radius);
        tiktokBlurMenu.setVisibility(VISIBLE);
        blurView.setVisibility(VISIBLE);
        blurView.setAlpha(0f);
        blurView.animate().alpha(1f).setDuration(400).start();

        enableBackUI(false);

        tiktokBlurMenuBack.setOnClickListener(v -> {
            blurView.setAlpha(1f);
            blurView.animate().alpha(0f).setDuration(400).start();
            blurView.setVisibility(GONE);
            tiktokBlurMenu.setVisibility(GONE);
            enableBackUI(true);
        });

        tiktokBlurLogOut.setOnClickListener(v -> {
            PreferencesClass.clearToken(requireContext());
            PreferencesClass.clearUser(requireContext());

            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            startActivity(intent);
            requireActivity().finish();
        });

        tiktokBlurInfo.setOnClickListener(v -> {
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "Boomshakalaka", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void blurBackgroundConnection() {
        float radius = 20f;
        Drawable windowBackground = requireActivity().getWindow().getDecorView().getBackground();

        blurView.setupWith(blurTarget).setFrameClearDrawable(windowBackground).setBlurRadius(radius);
        tiktokConnectionMenu.setVisibility(VISIBLE);
        blurView.setVisibility(VISIBLE);
        blurView.setAlpha(0f);
        blurView.animate().alpha(1f).setDuration(400).start();

        enableBackUI(false);

        ArrayAdapter<String> serverAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_selected_item, availableServers);
        serverAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        tiktokConnectionSpinner.setAdapter(serverAdapter);

        int currentIndex = Arrays.stream(availableServers).toList().indexOf(PreferencesClass.getServer(requireContext()));
        tiktokConnectionSpinner.setSelection(currentIndex);

        tiktokConnectionBack.setOnClickListener(v -> {
            blurView.setAlpha(1f);
            blurView.animate().alpha(0f).setDuration(400).start();
            blurView.setVisibility(GONE);
            tiktokConnectionMenu.setVisibility(GONE);
            enableBackUI(true);
        });

        tiktokConnectionRetry.setOnClickListener(v -> {
            String selectedServer = tiktokConnectionSpinner.getSelectedItem().toString();
            checkConnection(selectedServer, true);
        });


    }

    private void enableBackUI(boolean flag) {
        inputURL.setEnabled(flag);
        convertButton.setEnabled(flag);
        downloadButton.setEnabled(flag);
        tiktokMenuButton.setEnabled(flag);
        tiktokConnectionButton.setEnabled(flag);
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

                assert response.body() != null;
                String responseText = response.body().string();
                JSONObject jsonObject = new JSONObject(responseText);
                String status = jsonObject.getString("status");

                if (response.code() == 200 && !status.equals("error")) {
                    requireActivity().runOnUiThread(() -> {
                        if (isAdded()) {
                            tiktokUser.setText(PreferencesClass.getUser(requireContext()));
                            Log.d("UserDebug", "User updated to: " + PreferencesClass.getUser(requireContext()));
                        }
                    });
                } else {
                    redirectToLoginOnce();
                }
            } catch (Exception exception) {
                requireActivity().runOnUiThread(() -> {
                    tiktokUser.setText("User");
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

    private void checkConnection(String address, Boolean blurFlag) {
        String url = address + "/api/ping";
        Request request = new Request.Builder().url(url).get().build();

        new Thread(() -> {
            try {
                if (blurFlag) {
                    requireActivity().runOnUiThread(() -> {
                        tiktokReconnectBar.setVisibility(VISIBLE);
                    });
                }

                Response response = client.newCall(request).execute();
                if (response.code() == 200) {
                    requireActivity().runOnUiThread(() -> {
                        // Toast.makeText(requireContext(), "Connected!", Toast.LENGTH_SHORT).show();
                        PreferencesClass.setServer(requireContext(), address);
                        tiktokConnectionButton.setImageResource(R.drawable.link_100);
                        if (blurFlag) {
                            tiktokReconnectBar.setVisibility(INVISIBLE);
                            Toast.makeText(requireContext(), "Connected!", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Error while trying to connect!", Toast.LENGTH_SHORT).show();
                        tiktokConnectionButton.setImageResource(R.drawable.broken_link_100);
                        if (blurFlag) {
                            tiktokReconnectBar.setVisibility(INVISIBLE);
                        }
                    });

                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }).start();

    }
}
