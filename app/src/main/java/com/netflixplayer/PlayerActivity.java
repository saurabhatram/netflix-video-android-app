package com.netflixplayer;

import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

public class PlayerActivity extends AppCompatActivity {

    // Constants
    private static final long SKIP_DURATION_MS = 10_000L;
    private static final long CONTROLS_AUTO_HIDE_MS = 3500L;

    // Views
    private PlayerView playerView;
    private View controlsOverlay;
    private ImageView ivPlayPause;
    private SeekBar seekbar;
    private TextView tvCurrentTime, tvTotalTime, tvVideoTitle, tvEpisodeInfo;
    private ProgressBar loadingSpinner;
    private LinearLayout brightnessIndicator, volumeIndicator;
    private ProgressBar pbBrightness, pbVolume;
    private TextView tvBrightnessVal, tvVolumeVal;
    private LinearLayout seekFeedbackLeft, seekFeedbackRight;
    private LinearLayout btnSkipBack, btnSkipForward, btnPlayPause;
    private ImageView btnBack, btnVolume;

    // Player
    private ExoPlayer player;

    // State
    private String videoPath;
    private String videoTitle;
    private String videoId;
    private long videoDuration;
    private long startPosition;
    private boolean isControlsVisible = false;
    private boolean isDraggingSeekbar = false;
    private boolean isLocked = false;

    // Audio
    private AudioManager audioManager;
    private int maxVolume, currentVolume;

    // Handlers
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable hideControlsRunnable;
    private Runnable updateProgressRunnable;

    // Prefs
    private PrefsManager prefsManager;

    // Gesture
    private GestureDetector gestureDetector;
    private float touchStartX, touchStartY;
    private float touchStartBrightness, touchStartVolume;
    private boolean isSwipingBrightness, isSwipingVolume;
    private int screenWidth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full screen immersive
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_player);

        // Get intent data
        videoPath = getIntent().getStringExtra("video_path");
        videoTitle = getIntent().getStringExtra("video_title");
        videoId = getIntent().getStringExtra("video_id");
        videoDuration = getIntent().getLongExtra("video_duration", 0);
        startPosition = getIntent().getLongExtra("start_position", 0);

        prefsManager = new PrefsManager(this);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        screenWidth = getResources().getDisplayMetrics().widthPixels;

        initViews();
        initGestures();
        initPlayer();
        setupSeekbar();
        startProgressUpdater();

        // Auto show controls briefly at start
        showControls();
    }

    private void initViews() {
        playerView = findViewById(R.id.player_view);
        controlsOverlay = findViewById(R.id.controls_overlay);
        ivPlayPause = findViewById(R.id.iv_play_pause);
        seekbar = findViewById(R.id.seekbar);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvTotalTime = findViewById(R.id.tv_total_time);
        tvVideoTitle = findViewById(R.id.tv_video_title);
        tvEpisodeInfo = findViewById(R.id.tv_episode_info);
        loadingSpinner = findViewById(R.id.loading_spinner);
        brightnessIndicator = findViewById(R.id.brightness_indicator);
        volumeIndicator = findViewById(R.id.volume_indicator);
        pbBrightness = findViewById(R.id.pb_brightness);
        pbVolume = findViewById(R.id.pb_volume);
        tvBrightnessVal = findViewById(R.id.tv_brightness_val);
        tvVolumeVal = findViewById(R.id.tv_volume_val);
        seekFeedbackLeft = findViewById(R.id.seek_feedback_left);
        seekFeedbackRight = findViewById(R.id.seek_feedback_right);

        btnSkipBack = findViewById(R.id.btn_skip_back);
        btnSkipForward = findViewById(R.id.btn_skip_forward);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        btnBack = findViewById(R.id.btn_back);
        btnVolume = findViewById(R.id.btn_volume);

        // Set video title
        tvVideoTitle.setText(videoTitle != null ? videoTitle : "Video");
        tvEpisodeInfo.setText("NetflixPlayer");
        tvTotalTime.setText(formatTime(videoDuration));

        // Button click listeners
        btnBack.setOnClickListener(v -> onBackPressed());

        btnSkipBack.setOnClickListener(v -> {
            skipBy(-SKIP_DURATION_MS);
            showSeekFeedback(false);
            resetHideControlsTimer();
        });

        btnSkipForward.setOnClickListener(v -> {
            skipBy(SKIP_DURATION_MS);
            showSeekFeedback(true);
            resetHideControlsTimer();
        });

        btnPlayPause.setOnClickListener(v -> {
            togglePlayPause();
            resetHideControlsTimer();
        });

        btnVolume.setOnClickListener(v -> {
            // Toggle mute
            int vol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            if (vol > 0) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
                btnVolume.setImageResource(android.R.drawable.ic_lock_silent_mode);
            } else {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume / 2, 0);
                btnVolume.setImageResource(android.R.drawable.ic_lock_silent_mode_off);
            }
            resetHideControlsTimer();
        });

        View btnLock = findViewById(R.id.btn_lock);
        btnLock.setOnClickListener(v -> {
            isLocked = !isLocked;
            if (isLocked) {
                Toast.makeText(this, "Screen locked", Toast.LENGTH_SHORT).show();
                ((ImageView) btnLock).setImageResource(android.R.drawable.ic_lock_lock);
            } else {
                Toast.makeText(this, "Screen unlocked", Toast.LENGTH_SHORT).show();
                ((ImageView) btnLock).setImageResource(android.R.drawable.ic_lock_lock);
            }
            resetHideControlsTimer();
        });

        View btnNext = findViewById(R.id.btn_next);
        btnNext.setOnClickListener(v -> {
            Toast.makeText(this, "Next episode", Toast.LENGTH_SHORT).show();
        });

        View btnSettings = findViewById(R.id.btn_settings);
        btnSettings.setOnClickListener(v -> {
            Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show();
        });

        View btnCast = findViewById(R.id.btn_cast);
        btnCast.setOnClickListener(v -> {
            Toast.makeText(this, "Cast screen", Toast.LENGTH_SHORT).show();
        });

        View btnFullscreen = findViewById(R.id.btn_fullscreen);
        btnFullscreen.setOnClickListener(v -> {
            Toast.makeText(this, "Already in fullscreen", Toast.LENGTH_SHORT).show();
        });
    }

    private void initGestures() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                toggleControls();
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (isLocked) return true;
                float x = e.getX();
                if (x < screenWidth / 2f) {
                    // Double tap left = skip back
                    skipBy(-SKIP_DURATION_MS);
                    showSeekFeedback(false);
                } else {
                    // Double tap right = skip forward
                    skipBy(SKIP_DURATION_MS);
                    showSeekFeedback(true);
                }
                return true;
            }
        });

        // Set touch on the player root to handle gestures
        View root = findViewById(R.id.player_view);
        root.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            handleSwipeGesture(event);
            return true;
        });
    }

    private void handleSwipeGesture(MotionEvent event) {
        if (isLocked) return;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStartX = event.getX();
                touchStartY = event.getY();
                isSwipingBrightness = touchStartX < screenWidth / 2f;
                isSwipingVolume = touchStartX >= screenWidth / 2f;

                // Capture starting values
                WindowManager.LayoutParams lp = getWindow().getAttributes();
                touchStartBrightness = lp.screenBrightness < 0 ? 0.5f : lp.screenBrightness;
                touchStartVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) / maxVolume;
                break;

            case MotionEvent.ACTION_MOVE:
                float dy = touchStartY - event.getY();
                float screenHeight = getResources().getDisplayMetrics().heightPixels;
                float delta = dy / screenHeight; // -1 to 1

                if (Math.abs(dy) > 20) { // threshold to distinguish from tap
                    if (isSwipingBrightness) {
                        float newBrightness = Math.max(0.01f, Math.min(1f, touchStartBrightness + delta));
                        setBrightness(newBrightness);
                        showBrightnessIndicator((int) (newBrightness * 100));
                    } else if (isSwipingVolume) {
                        float newVolume = Math.max(0f, Math.min(1f, touchStartVolume + delta));
                        int volLevel = (int) (newVolume * maxVolume);
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volLevel, 0);
                        showVolumeIndicator((int) (newVolume * 100));
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // Hide indicators after delay
                mainHandler.postDelayed(() -> {
                    brightnessIndicator.setVisibility(View.GONE);
                    volumeIndicator.setVisibility(View.GONE);
                }, 1200);
                isSwipingBrightness = false;
                isSwipingVolume = false;
                break;
        }
    }

    private void setBrightness(float brightness) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = brightness;
        getWindow().setAttributes(lp);
    }

    private void showBrightnessIndicator(int percent) {
        brightnessIndicator.setVisibility(View.VISIBLE);
        volumeIndicator.setVisibility(View.GONE);
        pbBrightness.setProgress(percent);
        tvBrightnessVal.setText(percent + "%");
    }

    private void showVolumeIndicator(int percent) {
        volumeIndicator.setVisibility(View.VISIBLE);
        brightnessIndicator.setVisibility(View.GONE);
        pbVolume.setProgress(percent);
        tvVolumeVal.setText(percent + "%");
    }

    private void initPlayer() {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        Uri uri = Uri.parse(videoPath);
        MediaItem mediaItem = MediaItem.fromUri(uri);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.setPlayWhenReady(true);

        if (startPosition > 0) {
            player.seekTo(startPosition);
        }

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_BUFFERING) {
                    loadingSpinner.setVisibility(View.VISIBLE);
                } else {
                    loadingSpinner.setVisibility(View.GONE);
                }

                if (state == Player.STATE_READY) {
                    long dur = player.getDuration();
                    if (dur > 0) {
                        tvTotalTime.setText(formatTime(dur));
                        seekbar.setMax(100);
                    }
                }

                if (state == Player.STATE_ENDED) {
                    showControls();
                    // Mark as completed
                    prefsManager.saveProgress(videoId, 100, 0);
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                updatePlayPauseIcon();
            }
        });
    }

    private void setupSeekbar() {
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && player != null) {
                    long duration = player.getDuration();
                    if (duration > 0) {
                        long seekTo = (duration * progress) / 100;
                        tvCurrentTime.setText(formatTime(seekTo));
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isDraggingSeekbar = true;
                mainHandler.removeCallbacks(hideControlsRunnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isDraggingSeekbar = false;
                long duration = player.getDuration();
                if (duration > 0) {
                    long seekTo = (duration * seekBar.getProgress()) / 100;
                    player.seekTo(seekTo);
                }
                resetHideControlsTimer();
            }
        });
    }

    private void startProgressUpdater() {
        updateProgressRunnable = new Runnable() {
            @Override
            public void run() {
                if (player != null && !isDraggingSeekbar) {
                    long position = player.getCurrentPosition();
                    long duration = player.getDuration();

                    if (duration > 0) {
                        int progress = (int) ((position * 100) / duration);
                        seekbar.setProgress(progress);
                        tvCurrentTime.setText(formatTime(position));

                        // Save progress every 5 seconds (roughly)
                        prefsManager.saveProgress(videoId, progress, position);
                    }
                }
                mainHandler.postDelayed(this, 500);
            }
        };
        mainHandler.post(updateProgressRunnable);
    }

    private void toggleControls() {
        if (isControlsVisible) {
            hideControls();
        } else {
            showControls();
        }
    }

    private void showControls() {
        controlsOverlay.setVisibility(View.VISIBLE);
        controlsOverlay.animate().alpha(1f).setDuration(200).start();
        isControlsVisible = true;
        resetHideControlsTimer();

        // Keep status bar hidden
        hideSystemUI();
    }

    private void hideControls() {
        controlsOverlay.animate().alpha(0f).setDuration(300).withEndAction(() -> {
            controlsOverlay.setVisibility(View.GONE);
            isControlsVisible = false;
        }).start();
        hideSystemUI();
    }

    private void resetHideControlsTimer() {
        if (hideControlsRunnable != null) {
            mainHandler.removeCallbacks(hideControlsRunnable);
        }
        hideControlsRunnable = () -> {
            if (player != null && player.isPlaying()) {
                hideControls();
            }
        };
        mainHandler.postDelayed(hideControlsRunnable, CONTROLS_AUTO_HIDE_MS);
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    private void togglePlayPause() {
        if (player == null) return;
        if (player.isPlaying()) {
            player.pause();
        } else {
            if (player.getPlaybackState() == Player.STATE_ENDED) {
                player.seekTo(0);
            }
            player.play();
        }
        updatePlayPauseIcon();
    }

    private void updatePlayPauseIcon() {
        if (player != null && player.isPlaying()) {
            ivPlayPause.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            ivPlayPause.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    private void skipBy(long ms) {
        if (player == null) return;
        long newPosition = player.getCurrentPosition() + ms;
        newPosition = Math.max(0, Math.min(newPosition, player.getDuration()));
        player.seekTo(newPosition);
    }

    private void showSeekFeedback(boolean forward) {
        LinearLayout feedback = forward ? seekFeedbackRight : seekFeedbackLeft;
        feedback.setVisibility(View.VISIBLE);
        feedback.setAlpha(1f);
        feedback.animate()
                .alpha(0f)
                .setDuration(600)
                .withEndAction(() -> feedback.setVisibility(View.GONE))
                .start();
    }

    private String formatTime(long ms) {
        long totalSeconds = ms / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%d:%02d", minutes, seconds);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
        if (player != null) {
            player.play();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.pause();
            // Save position
            if (videoId != null) {
                long pos = player.getCurrentPosition();
                long dur = player.getDuration();
                int progress = dur > 0 ? (int) ((pos * 100) / dur) : 0;
                prefsManager.saveProgress(videoId, progress, pos);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mainHandler.removeCallbacks(updateProgressRunnable);
        if (hideControlsRunnable != null) {
            mainHandler.removeCallbacks(hideControlsRunnable);
        }
        if (player != null) {
            player.release();
            player = null;
        }
    }

    @Override
    public void onBackPressed() {
        // Save progress before leaving
        if (player != null && videoId != null) {
            long pos = player.getCurrentPosition();
            long dur = player.getDuration();
            int progress = dur > 0 ? (int) ((pos * 100) / dur) : 0;
            prefsManager.saveProgress(videoId, progress, pos);
        }
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
