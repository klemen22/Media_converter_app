package com.example.media_converter_app;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesClass {
    private static final String PREFERENCE = "media_converter_app";
    private static final String URL = "server_url";
    private static final String TOKEN = "token";
    private static final String USER = "user";

    private static final String DEFAULT_SERVER = "https://192.168.64.95:9999/"; // default server

    // server
    public static void setServer(Context context, String url) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE);
        preferences.edit().putString(URL, url).apply();
    }

    public static String getServer(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE);
        return preferences.getString(URL, DEFAULT_SERVER);
    }

    // token

    public static void setToken(Context context, String token) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE);
        preferences.edit().putString(TOKEN, token).apply();
    }

    public static String getToken(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE);
        return preferences.getString(TOKEN, null);
    }

    public static void clearToken(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE);
        preferences.edit().remove(TOKEN).apply();
    }

    // user
    public static void setUser(Context context, String user) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE);
        preferences.edit().putString(USER, user).apply();
    }

    public static String getUser(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE);
        return preferences.getString(USER, null);
    }
}
