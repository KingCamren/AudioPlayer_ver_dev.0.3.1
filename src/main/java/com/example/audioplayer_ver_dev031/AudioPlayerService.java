package com.example.audioplayer_ver_dev031;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

public class AudioPlayerService extends Service {

    // ======
    // START OF FIELDS
    // ======
    private final String TAG = "AudioPlayer_ver_dev.0.31_service";

    private final IBinder binderToReturnServiceInstance = new BinderToReturnServiceInstance();
    public class BinderToReturnServiceInstance extends Binder {
        AudioPlayerService getService() {
            // return a service instance so MainActivity.java can call public methods
            return AudioPlayerService.this;
        }
    }

    final int AUDIO_PLAYER_SERVICE_NOTIFICATION_ID = 101;

    private ExoPlayer exoPlayer;
    // could implement these with shared preferences?
    private boolean playWhenReady;
    private int currentItem;
    //    private long playbackPosition;
    private final Player.Listener playbackStateListener = new PlaybackStateListener();

    private final String CHANNEL_ID = "audio_player_service";

    // =============
    // END OF FIELDS
    // =============

    // invoked by calling startService()
    // when run, service is started and runs in background indefinitely
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(this, AudioPlayerService.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        createNotificationChannel();
        // how to update the notification
        // https://stackoverflow.com/questions/5528288/how-do-i-update-the-notification-text-for-a-foreground-service-in-android
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentTitle(getString(R.string.audio_service_notif_title))
                .setContentText(getString(R.string.audio_service_notif_text))
                .setSmallIcon(R.drawable.ic_baseline_audiotrack_24)
                .setContentIntent(pendingIntent)
                .setTicker(getString(R.string.audio_service_notif_ticker))
                .setShowWhen(false)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .build();
        Log.d(TAG, "notification created");
        startForeground(AUDIO_PLAYER_SERVICE_NOTIFICATION_ID, notification);

        return START_STICKY;
    }

    // invoked by calling bindService()
    // clients communicate with service using IBinder, an interface
    // "return null;" indicates binding isn't implemented
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binderToReturnServiceInstance;
    }

    // perform one-time setup when service is initially created
    // called before onBind() and onStartCommand()
    @Override
    public void onCreate() {
        playWhenReady = true;
        currentItem = 0;
//        playbackPosition = 0L;
        initializePlayer();
    }

    // called when service isn't needed anymore and is destroyed
    // clean up resources with this method
    @Override
    public void onDestroy() {
        releasePlayer();
        // removes service from foreground ONLY
        // boolean removeNotification (true indicates service's notification will be removed by this method)
        stopForeground(STOP_FOREGROUND_REMOVE);
        // stops service
        stopSelf();
    }

    private void initializePlayer() {
        exoPlayer = new ExoPlayer.Builder(this).build();
        MediaItem mediaItem = MediaItem.fromUri(getString(R.string.media_url_mp3));
        exoPlayer.setMediaItem(mediaItem);

        exoPlayer.setPlayWhenReady(playWhenReady);
        exoPlayer.addListener(playbackStateListener);
        exoPlayer.seekTo(currentItem, 0L);
        exoPlayer.prepare();
        Log.d(TAG, "player initialized");
    }

    private void releasePlayer() {
//        playbackPosition = exoPlayer.getCurrentPosition();
        currentItem = exoPlayer.getCurrentMediaItemIndex();
//        playWhenReady = exoPlayer.getPlayWhenReady();
        exoPlayer.removeListener(playbackStateListener);
        exoPlayer.release();
        exoPlayer = null;
        Log.d(TAG, "player released");
    }

    public ExoPlayer getExoPlayer() {
        return exoPlayer;
    }

    // create service notification's notification channel
    // only required on api 26+, which added notification channels that could be muted to disable specific notification *types*
    private void createNotificationChannel() {
        CharSequence notificationChannelName = getString(R.string.notification_channel_name);
        NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, notificationChannelName, NotificationManager.IMPORTANCE_DEFAULT);
        notificationChannel.setDescription(getString(R.string.notification_channel_description));
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(notificationChannel);
        Log.d(TAG, "notification channel created");
    }
}
class PlaybackStateListener implements Player.Listener {

    private final String TAG = "AudioPlayer_dev.0.3";
    String state;

    @Override
    public void onPlaybackStateChanged(int playbackState) {
        switch (playbackState) {
            // has been instantiated, but ExoPlayer.prepare() hasn't been called
            case ExoPlayer.STATE_IDLE:
                state = "ExoPlayer.STATE_IDLE      -";
                break;
            // player's data buffer has run out, and must load more data
            case ExoPlayer.STATE_BUFFERING:
                state = "ExoPlayer.STATE_BUFFERING -";
                break;
            // media being played has finished
            case ExoPlayer.STATE_ENDED:
                state = "ExoPlayer.STATE_ENDED     -";
                break;
            // ready to play from current position, and will play automatically if playWhenReady = true
            case ExoPlayer.STATE_READY:
                state = "ExoPlayer.STATE_READY     -";
                break;
            default:
                state = "UNKNOWN_STATE             -";
                break;
        }
        Log.d(TAG, "changed state to " + state);
    }
}
