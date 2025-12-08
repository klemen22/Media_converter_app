package com.example.media_converter_app.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.media_converter_app.fragments.InstagramFragment;
import com.example.media_converter_app.fragments.TikTokFragment;
import com.example.media_converter_app.fragments.YoutubeFragment;

public class PagerAdapter extends FragmentStateAdapter {

    public PagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new InstagramFragment();
            case 1:
                return new YoutubeFragment();
            case 2:
                return new TikTokFragment();
            default:
                return new YoutubeFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
