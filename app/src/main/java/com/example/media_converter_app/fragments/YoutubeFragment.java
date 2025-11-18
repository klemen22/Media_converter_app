package com.example.media_converter_app.fragments;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.media_converter_app.R;

import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class YoutubeFragment extends Fragment {

    private EditText inputURL;
    private Spinner spinnerType;
    private Spinner spinnerRes;

    private Button convertButton;
    private Button downloadButton;

    private final OkHttpClient client = new OkHttpClient();
    private static final MediaType json = MediaType.get("application/json; charset=utf-8");
    private final String baseAddr = "https://192.168.64.95:9999/api"; //change this slop, this aint it man


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstance) {
        View view = inflater.inflate(R.layout.fragment_youtube, container, false);

        inputURL = view.findViewById(R.id.youtubeInput);
        spinnerType = view.findViewById(R.id.youtubeDropDownType);
        spinnerRes = view.findViewById(R.id.youtubeDropDownResolution);
        convertButton = view.findViewById(R.id.youtubeConvertdBtn);
        downloadButton = view.findViewById(R.id.youtubeDownloadBtn);

        downloadButton.setVisibility(INVISIBLE);
        downloadButton.setClickable(false);
        spinnerRes.setVisibility(INVISIBLE);
        spinnerRes.setClickable(false);

        // I. dropdown menu for media type
        String[] mediaType = {"mp3", "mp4"};
        ArrayAdapter<String> adapter1 = new ArrayAdapter<>(requireContext(), R.id.youtubeDropDownType, mediaType);
        spinnerType.setAdapter(adapter1);


        // II. dropdown menu for resolution
        String[] mediaRes = {"144p", "480p", "720p", "1080p"};
        ArrayAdapter<String> adapter2 = new ArrayAdapter<>(requireContext(), R.id.youtubeDropDownResolution, mediaRes);
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


    }
}




