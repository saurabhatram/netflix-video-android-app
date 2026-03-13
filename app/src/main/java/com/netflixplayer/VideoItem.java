package com.netflixplayer;

public class VideoItem {
    private String id;
    private String title;
    private String path;
    private long duration;
    private long size;
    private String mimeType;
    private long dateAdded;
    private int watchProgress; // 0-100
    private boolean isNew;
    private String genre;

    public VideoItem(String id, String title, String path, long duration, long size,
                     String mimeType, long dateAdded) {
        this.id = id;
        this.title = title;
        this.path = path;
        this.duration = duration;
        this.size = size;
        this.mimeType = mimeType;
        this.dateAdded = dateAdded;
        this.watchProgress = 0;
        this.isNew = (System.currentTimeMillis() / 1000 - dateAdded) < 7 * 24 * 3600;
        this.genre = pickGenre(title);
    }

    private String pickGenre(String title) {
        String[] genres = {"Action", "Drama", "Comedy", "Thriller", "Sci-Fi", "Romance"};
        return genres[Math.abs(title.hashCode()) % genres.length];
    }

    public String getFormattedDuration() {
        long totalSeconds = duration / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%d:%02d", minutes, seconds);
        }
    }

    public String getFormattedSize() {
        if (size >= 1024 * 1024 * 1024) {
            return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
        } else {
            return String.format("%.0f MB", size / (1024.0 * 1024));
        }
    }

    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getPath() { return path; }
    public long getDuration() { return duration; }
    public long getSize() { return size; }
    public String getMimeType() { return mimeType; }
    public long getDateAdded() { return dateAdded; }
    public int getWatchProgress() { return watchProgress; }
    public boolean isNew() { return isNew; }
    public String getGenre() { return genre; }

    // Setters
    public void setWatchProgress(int watchProgress) { this.watchProgress = watchProgress; }
    public void setNew(boolean aNew) { isNew = aNew; }
}
