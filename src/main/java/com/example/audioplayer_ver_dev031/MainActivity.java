package com.example.audioplayer_ver_dev031;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.os.HandlerCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

public class MainActivity extends AppCompatActivity implements IBinder.DeathRecipient {
    
    // ======
    // START OF FIELDS
    // ======
    // class-level fields
    private final String TAG = "AudioPlayer_dev.0.31";
    ConstraintLayout layout;

    // player and player UI setup
    AudioPlayerService audioPlayerService;
    boolean boundToService = false; // used so activity cannot call service methods before binding completes
    Intent intent;
    ExoPlayer exoPlayer;
    ImageView buttonPlayGraphic, buttonPauseGraphic;
    private PlayerView playerView; // must be implemented here, but could be combined with service methods

    // service callbacks
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            // MainActivity.java has bound to AudioPlayerService.java
            // cast IBinder argument iBinder to a BinderToReturnServiceInstance and get AudioPlayerService instance to call public methods
            AudioPlayerService.BinderToReturnServiceInstance binderToReturnServiceInstance = (AudioPlayerService.BinderToReturnServiceInstance) iBinder;
            audioPlayerService = binderToReturnServiceInstance.getService();
            boundToService = true;
            exoPlayer = audioPlayerService.getExoPlayer();
            playerView = findViewById(R.id.video_view);
            playerView.setPlayer(exoPlayer);

            Log.d(TAG, "service connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.e("ServiceConnectionCallback", "onServiceDisconnected");
            boundToService = false;
        }
    };

    // app mode selector
    private int appState;
    public final int STATE_HOME = 0;
    public final int STATE_PLAYER_AUDIO = 1;
    public final int STATE_PLAYER_VIDEO = 2;
    public final int STATE_RECORDER = 3;

    // progress bar
    DrawView drawView;

    // =============
    // END OF FIELDS
    // =============

    // initial setup:
    // set layout (TODO: will be moved to when user chooses app mode)
    // request notifications
    // start service
    // assign DrawView for progress bar (TODO: will be moved to when user chooses audio app mode)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        layout = findViewById(R.id.layout);
        Log.d(TAG, "test");
        Log.d(TAG, "check 1: " +
                ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.POST_NOTIFICATIONS) + ", " +
                PackageManager.PERMISSION_GRANTED);

        ActivityResultLauncher<String> requestNotificationsPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (!isGranted) {
                        Toast.makeText(getApplicationContext(),
                                        "Notifications disabled. You can change this permission in your app settings.",
                                        Toast.LENGTH_LONG)
                                .show();
                    }
                });
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestNotificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }

        Log.d(TAG, "check 2: " +
                ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.POST_NOTIFICATIONS) + ", " +
                PackageManager.PERMISSION_GRANTED);

        // before requesting system run foreground service, start service itself
        intent = new Intent(this, AudioPlayerService.class);
        getApplicationContext().startForegroundService(intent);
        buttonPlayGraphic = findViewById(R.id.button_play_graphic);
        buttonPauseGraphic = findViewById(R.id.button_pause_graphic);

        drawView = findViewById(R.id.audio_mode_progress_bar);
    }

    // bind UI to AudioPlayerService
    // hide system UI
    @Override
    protected void onStart() {
        super.onStart();
        // bind to AudioPlayerService.java
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "service bound");
        hideSystemUI();
    }

    // unbind UI from AudioPlayerService
    @Override
    protected void onStop() {
        super.onStop();
        unbindService(serviceConnection);
        Log.d(TAG, "service unbound");
    }

    // required callback for using a binder, required to get AudioPlayerService and its methods
    @Override
    public void binderDied() {
        Log.d(TAG, "binder died");
    }

    // onClick for play/pause button in audio mode
    public void playPause(View view) {
        if (exoPlayer.isPlaying()) {
            exoPlayer.pause();
            buttonPlayGraphic.setVisibility(View.VISIBLE);
            buttonPauseGraphic.setVisibility(View.INVISIBLE);
            Log.d(TAG, "paused");
        } else {
            exoPlayer.play();
            buttonPlayGraphic.setVisibility(View.INVISIBLE);
            buttonPauseGraphic.setVisibility(View.VISIBLE);
            Log.d(TAG, "playing");
        }
    }

    // onClick for next-in-playlist button in audio mode
    public void nextInPlaylist(View view) {
        exoPlayer.seekToNextMediaItem();
        Log.d(TAG, "next in playlist");
    }

    // onClick for prev-in-playlist button in audio mode
    public void previousInPlaylist(View view) {
        exoPlayer.seekToPreviousMediaItem();
        Log.d(TAG, "previous in playlist");
    }

    // returns app mode (audio, video, recorder)
    public int getAppState() {
        return appState;
    }

    // used in the main menu; sets app mode (audio, video, recorder)
    public void setAppState(String state) {
        switch (state) {
            case "player_audio":
                appState = STATE_PLAYER_AUDIO;
            case "player_video":
                appState = STATE_PLAYER_VIDEO;
            case "recorder":
                appState = STATE_RECORDER;
            default:
                appState = STATE_HOME;
        }
    }

    private void hideSystemUI() {
        WindowInsetsControllerCompat windowInsetsControllerCompat = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsControllerCompat.hide(WindowInsetsCompat.Type.systemBars());
        windowInsetsControllerCompat.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }
}