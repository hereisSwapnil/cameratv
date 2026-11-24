package app.hereisswapnil.cameratv;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.media3.ui.PlayerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Camera TV — Main Activity
 *
 * Controls:
 *   ◀ ▶ ▲ ▼   — Navigate between cameras on screen.
 *                Only the selected camera plays live audio!
 *                Navigating past the screen edges pages forward/backward
 *                (e.g. going out of 2nd camera in 2-cam view pages to cameras 3 & 4).
 *   OK / ENTER — Cycle modes / Toggle Fullscreen on selected camera.
 *   BACK       — Exit app always.
 */
public class MainActivity extends Activity {

    public static final String ACTION_RELOAD = "app.hereisswapnil.cameratv.RELOAD_CONFIG";

    // ── Layout modes ──────────────────────────────────────────────────────────
    private static final int MODE_1 = 0;   // 1-cam fullscreen
    private static final int MODE_2 = 1;   // 2-cam side by side
    private static final int MODE_4 = 2;   // 4-cam 2×2 grid

    private int currentMode = MODE_2;      // Default to dual screen view
    private int offset = 0;               // Index of first camera in current page
    private int selectedSlot = 0;         // Currently selected slot in view (0..slots-1)

    // ── Data ──────────────────────────────────────────────────────────────────
    private List<CameraItem> cameraList = new ArrayList<>();

    // ── Views ─────────────────────────────────────────────────────────────────
    private FrameLayout  playerSingleContainer;
    private PlayerView   playerSingle;
    private ProgressBar  loadingSpinner;
    private GridLayout   gridContainer;
    private View         hudOverlay;
    private TextView     tvCamTitle;
    private TextView     tvCamHint;

    // Fullscreen error views
    private View         errorSingleContainer;
    private TextView     tvSingleErrorTitle;
    private TextView     tvSingleErrorDetail;
    private TextView     tvSingleRetryStatus;

    // ── Controllers ───────────────────────────────────────────────────────────
    private CameraStreamController singleController;
    private final Map<Integer, CameraStreamController> gridControllers = new HashMap<>();
    private final List<View> gridCells = new ArrayList<>();

    // ── HUD & Timing ──────────────────────────────────────────────────────────
    private final Handler  handler        = new Handler(Looper.getMainLooper());
    private       Runnable hideHudRunnable;

    // ── Broadcast reload ──────────────────────────────────────────────────────
    private final BroadcastReceiver reloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            reloadCameras();
            showHud("Config reloaded via ADB ✓", 3000);
        }
    };

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setImmersiveMode();
        setContentView(R.layout.activity_main);

        playerSingleContainer = findViewById(R.id.player_single_container);
        playerSingle          = findViewById(R.id.player_single);
        gridContainer         = findViewById(R.id.grid_container);
        hudOverlay            = findViewById(R.id.hud_overlay);
        tvCamTitle            = findViewById(R.id.tv_cam_title);
        tvCamHint             = findViewById(R.id.tv_cam_hint);
        loadingSpinner        = findViewById(R.id.loading_spinner);

        errorSingleContainer  = findViewById(R.id.error_single_container);
        tvSingleErrorTitle    = findViewById(R.id.tv_single_error_title);
        tvSingleErrorDetail   = findViewById(R.id.tv_single_error_detail);
        tvSingleRetryStatus   = findViewById(R.id.tv_single_retry_status);

        registerReceiver(reloadReceiver, new IntentFilter(ACTION_RELOAD));

        if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 101);
        } else {
            loadCamerasAndStart();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            loadCamerasAndStart();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setImmersiveMode();
        resumeAllActivePlayers();
    }

    @Override
    protected void onPause() {
        super.onPause();
        pauseAllActivePlayers();
    }

    @Override
    protected void onDestroy() {
        try { unregisterReceiver(reloadReceiver); } catch (Exception ignored) {}
        releaseAllPlayers();
        super.onDestroy();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Setup
    // ─────────────────────────────────────────────────────────────────────────

    private void setImmersiveMode() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    private void loadCamerasAndStart() {
        cameraList = ConfigManager.loadCameras(this);
        offset = 0;
        selectedSlot = 0;

        if (cameraList.isEmpty()) {
            if (loadingSpinner != null) {
                loadingSpinner.setVisibility(View.GONE);
            }
            tvCamTitle.setText("No cameras configured");
            tvCamHint.setText("Push cameras.json to TV: adb push cameras.json /sdcard/cameras.json");
            hudOverlay.setAlpha(1f);
            hudOverlay.setVisibility(View.VISIBLE);
            return;
        }

        // Default to MODE_1 if only 1 camera, else MODE_2
        if (cameraList.size() == 1) {
            currentMode = MODE_1;
        } else {
            currentMode = MODE_2;
        }

        applyMode();
    }

    private void reloadCameras() {
        releaseAllPlayers();
        gridContainer.removeAllViews();
        loadCamerasAndStart();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Key handling — DPAD Navigation & Audio Selection
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return super.dispatchKeyEvent(event);
        }

        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                handleOkKey();
                return true;

            case KeyEvent.KEYCODE_DPAD_RIGHT:
                handleNavigateRight();
                return true;

            case KeyEvent.KEYCODE_DPAD_LEFT:
                handleNavigateLeft();
                return true;

            case KeyEvent.KEYCODE_DPAD_DOWN:
                handleNavigateDown();
                return true;

            case KeyEvent.KEYCODE_DPAD_UP:
                handleNavigateUp();
                return true;

            case KeyEvent.KEYCODE_BACK:
                finish();
                return true;

            default:
                return super.dispatchKeyEvent(event);
        }
    }

    private void handleOkKey() {
        int n = cameraList.size();
        if (n <= 1) {
            showHud("Only 1 camera configured", 2000);
            return;
        }

        if (currentMode == MODE_1) {
            // Fullscreen -> Return to dual or quad grid
            currentMode = (n >= 3) ? MODE_2 : MODE_2;
            applyMode();
        } else if (currentMode == MODE_2) {
            if (n >= 3) {
                // Switch to 4-cam grid
                currentMode = MODE_4;
                applyMode();
            } else {
                // Only 2 cameras: Fullscreen the currently selected camera!
                offset = (offset + selectedSlot) % n;
                selectedSlot = 0;
                currentMode = MODE_1;
                applyMode();
            }
        } else { // MODE_4
            // Fullscreen the currently selected camera!
            offset = (offset + selectedSlot) % n;
            selectedSlot = 0;
            currentMode = MODE_1;
            applyMode();
        }
    }

    private void handleNavigateRight() {
        int n = cameraList.size();
        if (n == 0) return;

        if (currentMode == MODE_1) {
            offset = (offset + 1) % n;
            applyMode();
            return;
        }

        if (currentMode == MODE_2) {
            int actual = Math.min(2, n);
            if (selectedSlot < actual - 1) {
                // Move focus from Camera 1 to Camera 2
                setSelectedSlot(selectedSlot + 1);
            } else {
                // Going out from 2nd camera -> page to next 2 cameras!
                offset = (offset + 2) % n;
                selectedSlot = 0;
                applyMode();
            }
            return;
        }

        if (currentMode == MODE_4) {
            int actual = Math.min(4, n);
            if (selectedSlot == 0 && actual > 1) {
                setSelectedSlot(1);
            } else if (selectedSlot == 2 && actual > 3) {
                setSelectedSlot(3);
            } else if (selectedSlot == 1 && actual > 2) {
                setSelectedSlot(2);
            } else {
                // Out from bottom-right or boundary -> page to next 4 cameras!
                offset = (offset + 4) % n;
                selectedSlot = 0;
                applyMode();
            }
        }
    }

    private void handleNavigateLeft() {
        int n = cameraList.size();
        if (n == 0) return;

        if (currentMode == MODE_1) {
            offset = (offset - 1 + n) % n;
            applyMode();
            return;
        }

        if (currentMode == MODE_2) {
            if (selectedSlot > 0) {
                // Move focus from Camera 2 to Camera 1
                setSelectedSlot(selectedSlot - 1);
            } else {
                // Going back from 1st camera -> page to previous 2 cameras!
                offset = (offset - 2 % n + n) % n;
                int actualNew = Math.min(2, n);
                selectedSlot = Math.max(0, actualNew - 1);
                applyMode();
            }
            return;
        }

        if (currentMode == MODE_4) {
            if (selectedSlot == 1) {
                setSelectedSlot(0);
            } else if (selectedSlot == 3) {
                setSelectedSlot(2);
            } else if (selectedSlot == 2) {
                setSelectedSlot(1);
            } else {
                // Going back from slot 0 -> page to previous 4 cameras!
                offset = (offset - 4 % n + n) % n;
                int actualNew = Math.min(4, n);
                selectedSlot = Math.max(0, actualNew - 1);
                applyMode();
            }
        }
    }

    private void handleNavigateDown() {
        int n = cameraList.size();
        if (n == 0) return;

        if (currentMode == MODE_1) {
            handleNavigateRight();
            return;
        }

        if (currentMode == MODE_2) {
            // In 2-cam mode, Up/Down toggles slot or pages
            handleNavigateRight();
            return;
        }

        if (currentMode == MODE_4) {
            int actual = Math.min(4, n);
            if (selectedSlot == 0 && actual > 2) {
                setSelectedSlot(2);
            } else if (selectedSlot == 1 && actual > 3) {
                setSelectedSlot(3);
            } else {
                // From bottom row -> Page to next 4 cameras
                int col = selectedSlot % 2;
                offset = (offset + 4) % n;
                int actualNew = Math.min(4, n);
                selectedSlot = Math.min(col, actualNew - 1);
                applyMode();
            }
        }
    }

    private void handleNavigateUp() {
        int n = cameraList.size();
        if (n == 0) return;

        if (currentMode == MODE_1) {
            handleNavigateLeft();
            return;
        }

        if (currentMode == MODE_2) {
            // In 2-cam mode, Up/Down toggles slot or pages
            handleNavigateLeft();
            return;
        }

        if (currentMode == MODE_4) {
            if (selectedSlot == 2) {
                setSelectedSlot(0);
            } else if (selectedSlot == 3) {
                setSelectedSlot(1);
            } else {
                // From top row -> Page to previous 4 cameras
                int col = selectedSlot % 2;
                offset = (offset - 4 % n + n) % n;
                int actualNew = Math.min(4, n);
                selectedSlot = Math.min(2 + col, actualNew - 1);
                applyMode();
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Audio Focus & Selection State
    // ─────────────────────────────────────────────────────────────────────────

    private void setSelectedSlot(int newSlot) {
        this.selectedSlot = newSlot;

        for (int i = 0; i < gridCells.size(); i++) {
            View cell = gridCells.get(i);
            CameraStreamController controller = gridControllers.get(i);
            boolean isSelected = (i == selectedSlot);

            if (cell != null) {
                cell.setSelected(isSelected);
                View audioBadge = cell.findViewById(R.id.badge_audio_container);
                if (audioBadge != null) {
                    audioBadge.setVisibility(isSelected ? View.VISIBLE : View.GONE);
                }
            }

            if (controller != null) {
                // ONLY the selected camera has audio enabled!
                controller.setMuted(!isSelected);
            }
        }

        int n = cameraList.size();
        int activeCamIndex = (offset + selectedSlot) % n;
        CameraItem cam = cameraList.get(activeCamIndex);
        showHud(cam.name + " 🔊 [AUDIO LIVE]", hintFor(currentMode), 2000);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Render current mode
    // ─────────────────────────────────────────────────────────────────────────

    private void applyMode() {
        releaseAllPlayers();
        gridContainer.removeAllViews();
        gridCells.clear();

        switch (currentMode) {
            case MODE_1: renderSingle();  break;
            case MODE_2: renderGrid(2);   break;
            case MODE_4: renderGrid(4);   break;
        }
    }

    // ── 1-camera fullscreen ───────────────────────────────────────────────────

    private void renderSingle() {
        gridContainer.setVisibility(View.GONE);
        playerSingleContainer.setVisibility(View.VISIBLE);

        CameraItem cam = cameraList.get(offset);
        singleController = new CameraStreamController(
                this,
                handler,
                cam,
                loadingSpinner,
                errorSingleContainer,
                tvSingleErrorTitle,
                tvSingleErrorDetail,
                tvSingleRetryStatus,
                null
        );

        playerSingle.setPlayer(singleController.getPlayer());
        singleController.setMuted(false); // Audio is active in fullscreen
        singleController.start();

        int n = cameraList.size();
        String title = cam.name + " 🔊  [" + (offset + 1) + "/" + n + "]";
        showHud(title, hintFor(MODE_1), 3500);
    }

    // ── 2-cam or 4-cam grid ───────────────────────────────────────────────────

    private void renderGrid(int slots) {
        playerSingleContainer.setVisibility(View.GONE);
        loadingSpinner.setVisibility(View.GONE);

        int n       = cameraList.size();
        int actual  = Math.min(slots, n);

        int cols = (actual == 2) ? 2 : 2;
        int rows = (actual <= 2) ? 1 : 2;

        gridContainer.setColumnCount(cols);
        gridContainer.setRowCount(rows);

        // Clamp selectedSlot to valid range
        if (selectedSlot >= actual) {
            selectedSlot = 0;
        }

        LayoutInflater inflater = LayoutInflater.from(this);

        for (int slot = 0; slot < actual; slot++) {
            int camIndex = (offset + slot) % n;
            CameraItem cam = cameraList.get(camIndex);

            View cell = inflater.inflate(R.layout.item_camera_grid, gridContainer, false);
            PlayerView  pv          = cell.findViewById(R.id.player_cell);
            TextView    label       = cell.findViewById(R.id.tv_cell_label);
            TextView    badge       = cell.findViewById(R.id.tv_cell_badge);
            ProgressBar spin        = cell.findViewById(R.id.spinner_cell);
            View        errCont     = cell.findViewById(R.id.error_cell_container);
            TextView    errTitle    = cell.findViewById(R.id.tv_cell_error_title);
            TextView    errDetail   = cell.findViewById(R.id.tv_cell_error_detail);
            TextView    retryStatus = cell.findViewById(R.id.tv_cell_retry_status);

            label.setText(cam.name);

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width        = 0;
            lp.height       = 0;
            lp.columnSpec   = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            lp.rowSpec      = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            cell.setLayoutParams(lp);

            CameraStreamController controller = new CameraStreamController(
                    this,
                    handler,
                    cam,
                    spin,
                    errCont,
                    errTitle,
                    errDetail,
                    retryStatus,
                    badge
            );

            pv.setPlayer(controller.getPlayer());

            // Only the selectedSlot will play audio
            boolean isSelected = (slot == selectedSlot);
            controller.setMuted(!isSelected);
            cell.setSelected(isSelected);

            View audioBadge = cell.findViewById(R.id.badge_audio_container);
            if (audioBadge != null) {
                audioBadge.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            }

            final int currentSlot = slot;
            cell.setOnClickListener(v -> setSelectedSlot(currentSlot));

            controller.start();
            gridControllers.put(slot, controller);
            gridCells.add(cell);

            gridContainer.addView(cell);
        }

        gridContainer.setVisibility(View.VISIBLE);

        String modeLabel = (actual == 2) ? "2-CAM" : "4-CAM";
        int activeCamIndex = (offset + selectedSlot) % n;
        CameraItem activeCam = cameraList.get(activeCamIndex);
        String title = modeLabel + "  [" + (offset + 1) + "–"
                + ((offset + actual - 1) % n + 1) + " of " + n + "]  •  " + activeCam.name + " 🔊";
        showHud(title, hintFor(currentMode), 3500);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HUD
    // ─────────────────────────────────────────────────────────────────────────

    private String hintFor(int mode) {
        switch (mode) {
            case MODE_1: return "◀ ▶  switch cam   •   OK  grid   •   BACK  exit";
            case MODE_2: return "◀ ▶  select / next   •   OK  fullscreen   •   BACK  exit";
            case MODE_4: return "▲ ▼ ◀ ▶  select / next   •   OK  fullscreen   •   BACK  exit";
            default:     return "";
        }
    }

    private void showHud(String title, String hint, int durationMs) {
        handler.post(() -> {
            if (hideHudRunnable != null) handler.removeCallbacks(hideHudRunnable);
            tvCamTitle.setText(title);
            tvCamHint.setText(hint);
            hudOverlay.setAlpha(1f);
            hudOverlay.setVisibility(View.VISIBLE);
            hideHudRunnable = () -> hudOverlay.animate().alpha(0f).setDuration(400)
                    .withEndAction(() -> hudOverlay.setVisibility(View.GONE)).start();
            handler.postDelayed(hideHudRunnable, durationMs);
        });
    }

    private void showHud(String title, int durationMs) {
        showHud(title, hintFor(currentMode), durationMs);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Player lifecycle helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void resumeAllActivePlayers() {
        if (singleController != null) singleController.play();
        for (CameraStreamController c : gridControllers.values()) {
            if (c != null) c.play();
        }
    }

    private void pauseAllActivePlayers() {
        if (singleController != null) singleController.pause();
        for (CameraStreamController c : gridControllers.values()) {
            if (c != null) c.pause();
        }
    }

    private void releaseAllPlayers() {
        if (singleController != null) {
            singleController.release();
            singleController = null;
        }
        for (CameraStreamController c : gridControllers.values()) {
            if (c != null) c.release();
        }
        gridControllers.clear();
        gridCells.clear();
    }
}
