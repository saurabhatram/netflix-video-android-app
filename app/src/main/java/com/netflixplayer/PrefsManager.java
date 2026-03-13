package com.netflixplayer;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefsManager {
    private static final String PREF_NAME = "netflix_player_prefs";
    private static final String KEY_PROGRESS_PREFIX = "progress_";
    private static final String KEY_POSITION_PREFIX = "position_";

    private SharedPreferences prefs;

    public PrefsManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveProgress(String videoId, int progress, long position) {
        prefs.edit()
                .putInt(KEY_PROGRESS_PREFIX + videoId, progress)
                .putLong(KEY_POSITION_PREFIX + videoId, position)
                .apply();
    }

    public int getProgress(String videoId) {
        return prefs.getInt(KEY_PROGRESS_PREFIX + videoId, 0);
    }

    public long getPosition(String videoId) {
        return prefs.getLong(KEY_POSITION_PREFIX + videoId, 0);
    }

    public void clearProgress(String videoId) {
        prefs.edit()
                .remove(KEY_PROGRESS_PREFIX + videoId)
                .remove(KEY_POSITION_PREFIX + videoId)
                .apply();
    }
}
