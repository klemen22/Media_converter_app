package com.example.media_converter_app.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.example.media_converter_app.R;

public class TikTokFragment extends Fragment {

    public TikTokFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstance) {

        return inflater.inflate(R.layout.fragment_tiktok, container, false);
    }


}
