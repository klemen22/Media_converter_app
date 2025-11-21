package com.example.media_converter_app.fragments;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.media_converter_app.R;
import com.example.media_converter_app.UnsafeOkHttpClient;
import com.example.media_converter_app.NotificationClass;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class InstagramFragment extends Fragment {


    private final String baseAddr = "https://192.168.64.95:9999/api"; //change this
    private EditText inputURL;
    private Spinner spinnerType;
    private Button convertButton;
    private Button downloadButton;

    private final OkHttpClient client = UnsafeOkHttpClient.getUnsafeClient();


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstance) {

        View view = inflater.inflate(R.layout.fragment_instagram, container, false);

        inputURL = view.findViewById(R.id.instagramInput);
        spinnerType = view.findViewById(R.id.instagramDropDownType);
        convertButton = view.findViewById(R.id.instagramConvertBtn);
        downloadButton = view.findViewById(R.id.instagramDownloadBtn);

        downloadButton.setVisibility(INVISIBLE);
        downloadButton.setClickable(false);
        convertButton.setVisibility(VISIBLE);
        convertButton.setClickable(true);

        // dropdown menu for media type
        String[] mediaType = {"picture", "video"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                mediaType
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(adapter);

        convertButton.setOnClickListener(v -> {
            convertButton.setClickable(false);
            startConverting();
        });

        return view;
    }

    private void startConverting() {

        String instaURL = inputURL.getText().toString();
        String mediaType = spinnerType.getSelectedItem().toString();
        JSONObject json = new JSONObject();

        if(instaURL.isEmpty()){
            Toast.makeText(requireContext(), "Please enter a URL...", Toast.LENGTH_SHORT).show();
            convertButton.setClickable(true);
            return;
        }

        try{
            json.put("url", instaURL);
            json.put("type", mediaType);
        }catch (Exception exception){
            exception.printStackTrace();
        }

        RequestBody payloadBody = RequestBody.create(json.toString(), MediaType.parse("application/json; charset=utf-8"));
        Request payload = new Request.Builder().url(baseAddr + "/instagram/convert").post(payloadBody).build();

        new Thread(()->{
           try{
               Response response = client.newCall(payload).execute();

               if(!response.isSuccessful()){
                   requireActivity().runOnUiThread(()->{
                       Toast.makeText(requireContext(), "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                       convertButton.setClickable(true);
                   });
               }

               String responseText = response.body().string();
               Log.d("ConvertResponse", responseText);

               JSONObject jsonObject = new JSONObject(responseText);
               String fileName = jsonObject.getString("filename");

               requireActivity().runOnUiThread(()->{
                   convertButton.setVisibility(INVISIBLE);
                   downloadButton.setVisibility(VISIBLE);
                   downloadButton.setClickable(true);
                   NotificationClass.pushNotification(getContext(), "conversion", fileName);

                   downloadButton.setOnClickListener(v -> {
                       downloadButton.setClickable(false);
                       downloadInstagramContent(fileName);
                   });
               });
           } catch (Exception exception){
               exception.printStackTrace();
           }
        });

    }

    private void downloadInstagramContent(String filename){
        if(filename == null){
            requireActivity().runOnUiThread(()->{
                Toast.makeText(requireContext(), "No file ready to download...", Toast.LENGTH_SHORT).show();
                downloadButton.setVisibility(View.INVISIBLE);
                downloadButton.setClickable(false);
                convertButton.setVisibility(View.VISIBLE);
                convertButton.setClickable(true);
            });
            return;
        }

        JSONObject json = new JSONObject();

        try{
            json.put("filename", filename);
        } catch (Exception exception){
            exception.printStackTrace();
        }

        RequestBody payloadBody = RequestBody.create(json.toString(), MediaType.parse("application/json; charset=utf-8"));

        Request payload = new Request.Builder()
                .url(baseAddr + "/instagram/download")
                .post(payloadBody)
                .build();

        new Thread(()->{
            try{
                Response response = client.newCall(payload).execute();

                if(!response.isSuccessful()){
                    requireActivity().runOnUiThread(()->{
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

                while((read = inputStream.read(buffer)) != -1){
                    fileOutputStream.write(buffer, 0, read);
                }
                fileOutputStream.flush();
                fileOutputStream.close();
                inputStream.close();

                requireActivity().runOnUiThread(()->{
                    Toast.makeText(requireContext(), "Downloaded: " + filename, Toast.LENGTH_SHORT).show();
                });

                deleteBackendFile(filename);

            } catch (Exception exception){
                exception.printStackTrace();
                requireActivity().runOnUiThread(()->{
                    Toast.makeText(requireContext(), "Error: " + exception, Toast.LENGTH_SHORT).show();
                    downloadButton.setClickable(true);
                });
            }
        }).start();

    }

    private void  deleteBackendFile(String filename){
        JSONObject json = new JSONObject();
        NotificationClass.pushNotification(getContext(), "download", filename);
        try{
            json.put("filename", filename);
        }catch (Exception ignored){}

        RequestBody payloadBody = RequestBody.create(json.toString(), MediaType.parse("application/json; charset=utf-8"));

        Request payload = new Request.Builder()
                .url(baseAddr + "/instagram/delete")
                .delete(payloadBody)
                .build();

        new Thread(()->{
           try{
               client.newCall(payload).execute();
           } catch (Exception ignored){}

            requireActivity().runOnUiThread(()->{
                downloadButton.setClickable(false);
                downloadButton.setVisibility(INVISIBLE);

                convertButton.setVisibility(VISIBLE);
                convertButton.setClickable(false);

                inputURL.setText("");
            });

        }).start();
    }
}
