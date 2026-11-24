package app.hereisswapnil.cameratv;

import android.app.Activity;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.rtsp.RtspMediaSource;

public class CameraStreamController {
    private static final String TAG = "CameraStreamController";
    private static final int RETRY_INTERVAL_SECONDS = 4;
    private static final long WATCHDOG_INTERVAL_MS = 2000;
    private static final int STALL_POSITION_THRESHOLD_TICKS = 3; // 3 * 2s = 6s of identical position
    private static final int STALL_BUFFERING_THRESHOLD_TICKS = 4; // 4 * 2s = 8s stuck in buffering

    private final Activity activity;
    private final Handler handler;
    private final CameraItem camera;
    private final ExoPlayer player;
    private final View normalSpinner;
    private final View errorContainer;
    private final TextView tvErrorTitle;
    private final TextView tvErrorDetail;
    private final TextView tvRetryStatus;
    private final TextView tvBadge;

    private int retryCount = 0;
    private int secondsRemaining = 0;
    private Runnable countdownRunnable;
    private Runnable watchdogRunnable;

    private boolean isDestroyed = false;
    private boolean isErrorOrStall = false;
    private boolean isMuted = false;

    // Watchdog tracking
    private long lastPosition = C.TIME_UNSET;
    private int stuckPositionCount = 0;
    private int bufferingCount = 0;
    private boolean hasRenderedFirstFrame = false;

    public CameraStreamController(
            Activity activity,
            Handler handler,
            CameraItem camera,
            View normalSpinner,
            View errorContainer,
            TextView tvErrorTitle,
            TextView tvErrorDetail,
            TextView tvRetryStatus,
            TextView tvBadge) {
        this.activity = activity;
        this.handler = handler;
        this.camera = camera;
        this.normalSpinner = normalSpinner;
        this.errorContainer = errorContainer;
        this.tvErrorTitle = tvErrorTitle;
        this.tvErrorDetail = tvErrorDetail;
        this.tvRetryStatus = tvRetryStatus;
        this.tvBadge = tvBadge;

        this.player = buildPlayer(activity, camera.audio);
        setupPlayerListeners();
        setupWatchdog();
    }

    public ExoPlayer getPlayer() {
        return player;
    }

    public void setMuted(boolean muted) {
        this.isMuted = muted;
        if (player != null) {
            if (!camera.audio) {
                player.setVolume(0f);
            } else {
                player.setVolume(muted ? 0f : 1f);
            }
        }
    }

    public static ExoPlayer buildPlayer(Activity activity, boolean enableAudio) {
        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                        1500, // minBufferMs
                        5000, // maxBufferMs
                        500,  // bufferForPlaybackMs
                        1000  // bufferForPlaybackAfterRebufferMs
                )
                .setBackBuffer(0, false)
                .build();

        ExoPlayer player = new ExoPlayer.Builder(activity)
                .setLoadControl(loadControl)
                .build();

        if (!enableAudio) {
            player.setTrackSelectionParameters(
                    player.getTrackSelectionParameters()
                            .buildUpon()
                            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                            .build()
            );
            player.setVolume(0f);
        }

        return player;
    }

    private void setupPlayerListeners() {
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (isDestroyed) return;
                Log.d(TAG, "cam [" + camera.id + "] playbackState=" + state);
                if (state == Player.STATE_READY) {
                    onStreamWorking();
                } else if (state == Player.STATE_BUFFERING) {
                    if (!isErrorOrStall && normalSpinner != null) {
                        normalSpinner.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onRenderedFirstFrame() {
                if (isDestroyed) return;
                hasRenderedFirstFrame = true;
                onStreamWorking();
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                if (isDestroyed) return;
                Log.e(TAG, "cam [" + camera.id + "] onPlayerError: " + error.getMessage(), error);
                handleErrorOrStall(getFriendlyError(error), false);
            }
        });
    }

    public void start() {
        if (isDestroyed) return;
        showInitialLoading();
        connectStream();
        startWatchdog();
    }

    private void showInitialLoading() {
        if (normalSpinner != null) normalSpinner.setVisibility(View.VISIBLE);
        if (errorContainer != null) errorContainer.setVisibility(View.GONE);
        if (tvBadge != null) tvBadge.setVisibility(View.GONE);
    }

    private void connectStream() {
        if (isDestroyed) return;
        try {
            RtspMediaSource source = new RtspMediaSource.Factory()
                    .setForceUseRtpTcp(true)
                    .setTimeoutMs(6000)
                    .createMediaSource(MediaItem.fromUri(Uri.parse(camera.url)));

            player.setMediaSource(source, /* resetPosition= */ true);
            player.prepare();
            player.play();
            // Apply current mute state
            setMuted(isMuted);
        } catch (Exception e) {
            Log.e(TAG, "Error connecting stream for " + camera.id, e);
            handleErrorOrStall("Invalid stream URL", false);
        }
    }

    private void onStreamWorking() {
        isErrorOrStall = false;
        retryCount = 0;
        stuckPositionCount = 0;
        bufferingCount = 0;
        cancelCountdown();

        if (normalSpinner != null) normalSpinner.setVisibility(View.GONE);
        if (errorContainer != null) errorContainer.setVisibility(View.GONE);
        if (tvBadge != null) tvBadge.setVisibility(View.GONE);
    }

    private void handleErrorOrStall(String reason, boolean isStall) {
        cancelCountdown();
        isErrorOrStall = true;
        hasRenderedFirstFrame = false;
        retryCount++;

        if (normalSpinner != null) normalSpinner.setVisibility(View.GONE);
        if (tvBadge != null) tvBadge.setVisibility(View.VISIBLE);

        String host = camera.getHost();
        String title = isStall ? "Stream Frozen" : "Camera Unreachable";

        if (tvErrorTitle != null) {
            tvErrorTitle.setText(title);
        }

        if (tvErrorDetail != null) {
            if (!host.isEmpty()) {
                tvErrorDetail.setText(reason + " (" + host + ")");
            } else {
                tvErrorDetail.setText(reason);
            }
        }

        if (errorContainer != null) {
            errorContainer.setVisibility(View.VISIBLE);
        }

        secondsRemaining = RETRY_INTERVAL_SECONDS;
        updateRetryStatusText();

        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                if (isDestroyed || activity.isFinishing() || activity.isDestroyed()) return;

                secondsRemaining--;
                if (secondsRemaining > 0) {
                    updateRetryStatusText();
                    handler.postDelayed(this, 1000);
                } else {
                    if (tvRetryStatus != null) {
                        tvRetryStatus.setText("Reconnecting... (Attempt #" + retryCount + ")");
                    }
                    connectStream();
                }
            }
        };
        handler.postDelayed(countdownRunnable, 1000);
    }

    private void updateRetryStatusText() {
        if (tvRetryStatus != null) {
            String attempt = (retryCount > 1) ? " (#" + retryCount + ")" : "";
            tvRetryStatus.setText("Retrying in " + secondsRemaining + "s..." + attempt);
        }
    }

    private void cancelCountdown() {
        if (countdownRunnable != null) {
            handler.removeCallbacks(countdownRunnable);
            countdownRunnable = null;
        }
    }

    private void setupWatchdog() {
        watchdogRunnable = new Runnable() {
            @Override
            public void run() {
                if (isDestroyed || activity.isFinishing() || activity.isDestroyed()) return;

                checkStreamHealth();
                handler.postDelayed(this, WATCHDOG_INTERVAL_MS);
            }
        };
    }

    private void startWatchdog() {
        handler.removeCallbacks(watchdogRunnable);
        handler.postDelayed(watchdogRunnable, WATCHDOG_INTERVAL_MS);
    }

    private void stopWatchdog() {
        handler.removeCallbacks(watchdogRunnable);
    }

    private void checkStreamHealth() {
        if (isErrorOrStall || !player.isPlaying()) {
            return;
        }

        int state = player.getPlaybackState();

        // 1. Stuck in buffering for > 8 seconds
        if (state == Player.STATE_BUFFERING) {
            bufferingCount++;
            if (bufferingCount >= STALL_BUFFERING_THRESHOLD_TICKS) {
                Log.w(TAG, "Camera [" + camera.id + "] stuck buffering for > 8s, reconnecting");
                handleErrorOrStall("Buffering timeout", true);
            }
            return;
        } else {
            bufferingCount = 0;
        }

        // 2. Supposed to be playing, but position/frames are frozen for > 6 seconds
        if (state == Player.STATE_READY && hasRenderedFirstFrame) {
            long currentPos = player.getCurrentPosition();
            if (currentPos != C.TIME_UNSET && currentPos == lastPosition) {
                stuckPositionCount++;
                if (stuckPositionCount >= STALL_POSITION_THRESHOLD_TICKS) {
                    Log.w(TAG, "Camera [" + camera.id + "] video position frozen for > 6s, reconnecting");
                    handleErrorOrStall("Video feed frozen", true);
                }
            } else {
                lastPosition = currentPos;
                stuckPositionCount = 0;
            }
        }
    }

    public void play() {
        if (!isDestroyed && player != null) player.play();
    }

    public void pause() {
        if (!isDestroyed && player != null) player.pause();
    }

    public void release() {
        isDestroyed = true;
        cancelCountdown();
        stopWatchdog();
        if (player != null) {
            player.stop();
            player.release();
        }
    }

    private static String getFriendlyError(PlaybackException error) {
        if (error == null) return "Connection error";
        Throwable cause = error.getCause();
        if (cause == null) cause = error;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }

        String cls = cause.getClass().getSimpleName();
        String msg = cause.getMessage() != null ? cause.getMessage() : "";

        if (cause instanceof java.net.NoRouteToHostException || cls.contains("NoRouteToHost")) {
            return "Host unreachable";
        }
        if (cause instanceof java.net.ConnectException || cls.contains("ConnectException")) {
            return "Connection refused";
        }
        if (cause instanceof java.net.SocketTimeoutException || cls.contains("SocketTimeout")) {
            return "Connection timed out";
        }
        if (cause instanceof java.net.UnknownHostException || cls.contains("UnknownHost")) {
            return "Unknown host";
        }
        if (msg.contains("401") || msg.toLowerCase().contains("unauthorized")) {
            return "Auth failed (401)";
        }
        if (msg.contains("404") || msg.toLowerCase().contains("not found")) {
            return "Stream not found (404)";
        }
        if (msg.contains("Host unreachable")) {
            return "Host unreachable";
        }
        return "Stream offline";
    }
}
