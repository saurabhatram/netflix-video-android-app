package com.netflixplayer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import android.widget.ImageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;

    private RecyclerView rvContinue, rvTrending, rvPicks, rvAll;
    private LinearLayout permissionOverlay;
    private LinearLayout sectionContinue;
    private ImageView ivFeatured;
    private TextView tvFeaturedTitle, tvFeaturedInfo;
    private LinearLayout btnFeaturedPlay;

    private List<VideoItem> allVideos = new ArrayList<>();
    private PrefsManager prefsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefsManager = new PrefsManager(this);
        initViews();
        checkPermissionAndLoad();
    }

    private void initViews() {
        rvContinue = findViewById(R.id.rv_continue);
        rvTrending = findViewById(R.id.rv_trending);
        rvPicks = findViewById(R.id.rv_picks);
        rvAll = findViewById(R.id.rv_all);
        permissionOverlay = findViewById(R.id.permission_overlay);
        sectionContinue = findViewById(R.id.section_continue);
        ivFeatured = findViewById(R.id.iv_featured);
        tvFeaturedTitle = findViewById(R.id.tv_featured_title);
        tvFeaturedInfo = findViewById(R.id.tv_featured_info);
        btnFeaturedPlay = null; // handled via MaterialButton id

        // Set up RecyclerViews
        setupHorizontalRecyclerView(rvContinue);
        setupHorizontalRecyclerView(rvTrending);
        setupHorizontalRecyclerView(rvPicks);
        setupHorizontalRecyclerView(rvAll);

        // Grant permission button
        View btnGrant = findViewById(R.id.btn_grant_permission);
        btnGrant.setOnClickListener(v -> requestPermissions());

        // Featured play button
        View featuredPlay = findViewById(R.id.btn_featured_play);
        featuredPlay.setOnClickListener(v -> {
            if (!allVideos.isEmpty()) {
                openPlayer(allVideos.get(0));
            }
        });

        // Bottom nav clicks (decorative for now)
        setupBottomNav();
    }

    private void setupHorizontalRecyclerView(RecyclerView rv) {
        LinearLayoutManager lm = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rv.setLayoutManager(lm);
        rv.setHasFixedSize(true);
        rv.setNestedScrollingEnabled(false);
    }

    private void setupBottomNav() {
        // Home already selected, others show toast
        View navSearch = findViewById(R.id.nav_search);
        View navDownloads = findViewById(R.id.nav_downloads);
        View navMore = findViewById(R.id.nav_more);

        navSearch.setOnClickListener(v -> Toast.makeText(this, "Search coming soon", Toast.LENGTH_SHORT).show());
        navDownloads.setOnClickListener(v -> Toast.makeText(this, "Downloads coming soon", Toast.LENGTH_SHORT).show());
        navMore.setOnClickListener(v -> Toast.makeText(this, "More coming soon", Toast.LENGTH_SHORT).show());

        View btnSearch = findViewById(R.id.btn_search);
        btnSearch.setOnClickListener(v -> Toast.makeText(this, "Search coming soon", Toast.LENGTH_SHORT).show());
    }

    private void checkPermissionAndLoad() {
        if (hasStoragePermission()) {
            loadVideos();
        } else {
            requestPermissions();
        }
    }

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermissions() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{Manifest.permission.READ_MEDIA_VIDEO};
        } else {
            permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        }
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                permissionOverlay.setVisibility(View.GONE);
                loadVideos();
            } else {
                permissionOverlay.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Permission required to load videos", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void loadVideos() {
        new Thread(() -> {
            allVideos = VideoLoader.loadAllVideos(this);

            // Load saved progress
            for (VideoItem video : allVideos) {
                int progress = prefsManager.getProgress(video.getId());
                video.setWatchProgress(progress);
            }

            runOnUiThread(this::setupUI);
        }).start();
    }

    private void setupUI() {
        if (allVideos.isEmpty()) {
            Toast.makeText(this, "No videos found on device", Toast.LENGTH_SHORT).show();
            tvFeaturedTitle.setText("No Videos Found");
            tvFeaturedInfo.setText("Add videos to your device to start watching");
            return;
        }

        // Setup featured banner with random video
        Random random = new Random();
        VideoItem featured = allVideos.get(random.nextInt(Math.min(5, allVideos.size())));
        setupFeaturedBanner(featured);

        // Continue watching (videos with progress > 0)
        List<VideoItem> continueWatching = new ArrayList<>();
        for (VideoItem v : allVideos) {
            if (v.getWatchProgress() > 0 && v.getWatchProgress() < 95) {
                continueWatching.add(v);
            }
        }

        if (!continueWatching.isEmpty()) {
            sectionContinue.setVisibility(View.VISIBLE);
            setAdapter(rvContinue, continueWatching, true);
        } else {
            sectionContinue.setVisibility(View.GONE);
        }

        // Trending - shuffle top 15
        List<VideoItem> trending = new ArrayList<>(allVideos.subList(0, Math.min(15, allVideos.size())));
        Collections.shuffle(trending, random);
        setAdapter(rvTrending, trending, false);

        // Top picks - different shuffle
        List<VideoItem> picks = new ArrayList<>(allVideos);
        Collections.shuffle(picks, new Random(42));
        setAdapter(rvPicks, picks.subList(0, Math.min(12, picks.size())), false);

        // All videos
        setAdapter(rvAll, allVideos, false);
    }

    private void setupFeaturedBanner(VideoItem video) {
        tvFeaturedTitle.setText(video.getTitle());
        tvFeaturedInfo.setText("2024 • " + video.getGenre() + " • " + video.getFormattedDuration());

        // Load video thumbnail as featured image
        Uri videoUri = Uri.parse(video.getPath());
        Glide.with(this)
                .load(videoUri)
                .placeholder(R.drawable.placeholder_featured)
                .error(R.drawable.placeholder_featured)
                .centerCrop()
                .into(ivFeatured);

        // Featured play action
        View featuredPlay = findViewById(R.id.btn_featured_play);
        featuredPlay.setOnClickListener(v -> openPlayer(video));
    }

    private void setAdapter(RecyclerView rv, List<VideoItem> videos, boolean showProgress) {
        VideoAdapter adapter = new VideoAdapter(this, videos, new VideoAdapter.OnVideoClickListener() {
            @Override
            public void onVideoClick(VideoItem video, int position) {
                openPlayer(video);
            }

            @Override
            public void onVideoLongClick(VideoItem video, int position) {
                Toast.makeText(MainActivity.this, video.getTitle() + "\n" +
                        video.getFormattedDuration() + " • " + video.getFormattedSize(),
                        Toast.LENGTH_SHORT).show();
            }
        }, showProgress);

        rv.setAdapter(adapter);
    }

    private void openPlayer(VideoItem video) {
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("video_path", video.getPath());
        intent.putExtra("video_title", video.getTitle());
        intent.putExtra("video_id", video.getId());
        intent.putExtra("video_duration", video.getDuration());

        long savedPosition = prefsManager.getPosition(video.getId());
        intent.putExtra("start_position", savedPosition);

        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload to update watch progress
        if (hasStoragePermission() && !allVideos.isEmpty()) {
            for (VideoItem video : allVideos) {
                int progress = prefsManager.getProgress(video.getId());
                video.setWatchProgress(progress);
            }
            // Refresh continue watching
            List<VideoItem> continueWatching = new ArrayList<>();
            for (VideoItem v : allVideos) {
                if (v.getWatchProgress() > 0 && v.getWatchProgress() < 95) {
                    continueWatching.add(v);
                }
            }
            if (!continueWatching.isEmpty()) {
                sectionContinue.setVisibility(View.VISIBLE);
                setAdapter(rvContinue, continueWatching, true);
            } else {
                sectionContinue.setVisibility(View.GONE);
            }
        }
    }
}
