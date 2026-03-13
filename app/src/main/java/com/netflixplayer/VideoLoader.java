package com.netflixplayer;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VideoLoader {

    public static List<VideoItem> loadAllVideos(Context context) {
        List<VideoItem> videos = new ArrayList<>();
        ContentResolver resolver = context.getContentResolver();

        String[] projection = {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.MIME_TYPE,
                MediaStore.Video.Media.DATE_ADDED
        };

        String sortOrder = MediaStore.Video.Media.DATE_ADDED + " DESC";

        try (Cursor cursor = resolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
        )) {
            if (cursor != null) {
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
                int nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
                int dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                int durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
                int sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);
                int mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE);
                int dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idCol);
                    String name = cursor.getString(nameCol);
                    String data = cursor.getString(dataCol);
                    long duration = cursor.getLong(durationCol);
                    long size = cursor.getLong(sizeCol);
                    String mime = cursor.getString(mimeCol);
                    long dateAdded = cursor.getLong(dateCol);

                    // Clean up title - remove extension
                    String title = name;
                    if (title.contains(".")) {
                        title = title.substring(0, title.lastIndexOf('.'));
                    }
                    // Replace underscores and hyphens with spaces
                    title = title.replace('_', ' ').replace('-', ' ');
                    // Capitalize words
                    title = capitalizeWords(title);

                    // Use content URI for better compatibility
                    Uri contentUri = ContentUris.withAppendedId(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);
                    String uri = contentUri.toString();

                    if (duration > 0) { // skip zero-duration
                        videos.add(new VideoItem(
                                String.valueOf(id),
                                title,
                                uri,
                                duration,
                                size,
                                mime,
                                dateAdded
                        ));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return videos;
    }

    private static String capitalizeWords(String str) {
        if (str == null || str.isEmpty()) return str;
        String[] words = str.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) sb.append(word.substring(1).toLowerCase());
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }

    public static Uri getVideoThumbnailUri(long videoId) {
        return ContentUris.withAppendedId(
                MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI, videoId);
    }
}
