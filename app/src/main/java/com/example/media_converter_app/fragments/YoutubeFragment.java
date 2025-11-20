package com.example.media_converter_app.fragments;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
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
import android.widget.Spinner;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;

import com.example.media_converter_app.R;
import com.example.media_converter_app.UnsafeOkHttpClient;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

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

    private final OkHttpClient client = UnsafeOkHttpClient.getUnsafeClient();

    private final String baseAddr = "https://192.168.64.95:9999/api"; //change this slop, this aint it man


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstance) {
        View view = inflater.inflate(R.layout.fragment_youtube, container, false);

        inputURL = view.findViewById(R.id.youtubeInput);
        spinnerType = view.findViewById(R.id.youtubeDropDownType);
        spinnerRes = view.findViewById(R.id.youtubeDropDownResolution);
        convertButton = view.findViewById(R.id.youtubeConvertBtn);
        downloadButton = view.findViewById(R.id.youtubeDownloadBtn);

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
                android.R.layout.simple_spinner_item,
                mediaType
        );
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(adapter1);


        // II. dropdown menu for resolution
        String[] mediaRes = {"144p", "480p", "720p", "1080p"};
        ArrayAdapter<String> adapter2 = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                mediaRes
        );
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
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
        return view;
    }

    private void startConverting() {
        String resolution = "";

        String ytURL = inputURL.getText().toString();
        String mediaType = spinnerType.getSelectedItem().toString();
        JSONObject json = new JSONObject();

        if (ytURL.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a URL...", Toast.LENGTH_SHORT);
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
        Request payload = new Request.Builder().url(baseAddr + "/youtube/convert").post(payloadBody).build();

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
                    convertButton.setVisibility(View.INVISIBLE);
                    downloadButton.setVisibility(View.VISIBLE);
                    downloadButton.setClickable(true);

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
                .url(baseAddr + "/youtube/download")
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

                // --- Download file ---
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
        pushNotification("download");

        try {
            json.put("filename", filename);
        } catch (Exception ignored) {
        }

        RequestBody payloadBody =
                RequestBody.create(json.toString(), MediaType.parse("application/json; charset=utf-8"));

        Request payload = new Request.Builder()
                .url(baseAddr + "/youtube/delete")
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

    private void pushNotification(String type) {
        Context context = getContext();
        if (context == null) return;
        
        String CHANNEL_ID = "conversion_channel";

        String title = "";
        String message = "";
        String description = "";

        if (type.equals("conversion")) {
            title = "Conversion Completed";
            message = "Your video is converted!";
            description = "Notification when conversion is completed";
        } else if (type.equals("download")) {
            title = "Download Completed";
            message = "Your video is downloaded!";
            description = "Notification when download is completed";
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Media Converter Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            channel.setDescription(description);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 100, 500});

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVibrate(new long[]{0, 500, 100, 500})
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(1001, builder.build());
    }
}




