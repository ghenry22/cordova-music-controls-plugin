package com.homerours.musiccontrols;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MusicControlsPlugin extends CordovaPlugin {
    public static final int NOTIFICATION_ID = 7824;

    private MusicControlsBroadcastReceiver mMessageReceiver;
    private MusicControlsNotification notification;
    private MediaSessionCompat mediaSessionCompat;
    private PendingIntent mediaButtonPendingIntent;

    private final MediaSessionCallback mMediaSessionCallback = new MediaSessionCallback();
    private PlaybackStateCompat lastPlaybackState;
    private boolean hasScrubber = false;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        final Activity activity = cordova.getActivity();
        final Context context = activity.getApplicationContext();

        mediaSessionCompat = new MediaSessionCompat(context, "cordova-music-controls-media-session", null, mediaButtonPendingIntent);
        mediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        notification = new MusicControlsNotification(activity, NOTIFICATION_ID, mediaSessionCompat.getSessionToken());
        mMessageReceiver = new MusicControlsBroadcastReceiver(this);
        registerBroadcaster(mMessageReceiver);

        setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN);
        mediaSessionCompat.setActive(true);

        mediaSessionCompat.setCallback(mMediaSessionCallback);

        // Register media (headset) button event receiver
        try {
            Intent headsetIntent = new Intent("music-controls-media-button");
            mediaButtonPendingIntent = PendingIntent.getBroadcast(context, 0, headsetIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            registerMediaButtonEvent();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Remove notification when activity is destroyed
        ServiceConnection mConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder binder) {
                ((MusicControlsNotificationKillerService.KillBinder) binder)
                    .getService()
                    .startService(new Intent(activity, MusicControlsNotificationKillerService.class));
            }

            public void onServiceDisconnected(ComponentName className) {
            }
        };
        Intent startServiceIntent = new Intent(activity, MusicControlsNotificationKillerService.class);
        activity.bindService(startServiceIntent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public boolean execute(final String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        final Context context = cordova.getActivity().getApplicationContext();

        switch (action) {
            case "create":
                final MusicControlsInfoModel infos = new MusicControlsInfoModel(args);
                final MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();

                cordova.getThreadPool().execute(() -> {
                    notification.updateNotification(infos);

                    // track title
                    metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, infos.track);
                    // artists
                    metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, infos.artist);
                    //album
                    metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, infos.album);

                    if (infos.cover != null && infos.cover.isEmpty()) {
                        Bitmap art = BitmapUtils.getBitmapCover(context, infos.cover);
                        if (art != null) {
                            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, art);
                            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, art);
                        }
                    }

                    long playbackPosition = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;
                    hasScrubber = infos.hasScrubber;
                    if (hasScrubber) {
                        metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, infos.duration);
                        playbackPosition = infos.elapsed;
                    }

                    mediaSessionCompat.setMetadata(metadataBuilder.build());

                    if (infos.isPlaying)
                        setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING, playbackPosition);
                    else
                        setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED, playbackPosition);

                    callbackContext.success("success");
                });
                break;
            case "updateIsPlaying":
                cordova.getThreadPool().execute(() -> {
                    try {
                        final JSONObject params = args.getJSONObject(0);
                        final boolean isPlaying = params.getBoolean("isPlaying");
                        updateNotification(isPlaying, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        callbackContext.error(e.getLocalizedMessage());
                    }
                });
                break;
            case "updateDismissable":
                cordova.getThreadPool().execute(() -> {
                    try {
                        final JSONObject params = args.getJSONObject(0);
                        final boolean dismissable = params.getBoolean("dismissable");
                        notification.updateDismissable(dismissable);
                        callbackContext.success("success");
                    } catch (JSONException e) {
                        e.printStackTrace();
                        callbackContext.error(e.getLocalizedMessage());
                    }
                });
                break;
            case "destroy":
                cleanUp();
                callbackContext.success("success");
                break;
            case "watch":
                cordova.getThreadPool().execute(() -> {
                    registerMediaButtonEvent();
                    mMediaSessionCallback.setCallback(callbackContext);
                    mMessageReceiver.setCallback(callbackContext);
                });
                break;
            case "updateElapsed":
                cordova.getThreadPool().execute(() -> {
                    try {
                        final JSONObject params = args.getJSONObject(0);
                        long playbackPosition = params.getLong("elapsed");
                        final boolean isPlaying = params.getBoolean("isPlaying");
                        updateNotification(isPlaying, playbackPosition);
                        callbackContext.success("success");
                    } catch (JSONException e) {
                        e.printStackTrace();
                        callbackContext.error(e.getLocalizedMessage());
                    }
                });
                break;
        }
        return true;
    }

    @Override
    public void onDestroy() {
        cleanUp();
        super.onDestroy();
    }

    @Override
    public void onReset() {
        cleanUp();
        super.onReset();
    }

    public void registerMediaButtonEvent() {
        mediaSessionCompat.setMediaButtonReceiver(mediaButtonPendingIntent);
    }

    public void unregisterMediaButtonEvent() {
        mediaSessionCompat.setMediaButtonReceiver(null);
    }

    public void destroyPlayerNotification() {
        notification.destroy();
    }

    private void registerBroadcaster(MusicControlsBroadcastReceiver mMessageReceiver) {
        final Context context = cordova.getActivity().getApplicationContext();
        context.registerReceiver(mMessageReceiver, new IntentFilter("music-controls-previous"));
        context.registerReceiver(mMessageReceiver, new IntentFilter("music-controls-pause"));
        context.registerReceiver(mMessageReceiver, new IntentFilter("music-controls-play"));
        context.registerReceiver(mMessageReceiver, new IntentFilter("music-controls-next"));
        context.registerReceiver(mMessageReceiver, new IntentFilter("music-controls-media-button"));
        context.registerReceiver(mMessageReceiver, new IntentFilter("music-controls-destroy"));
        context.registerReceiver(mMessageReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
    }

    private void setMediaPlaybackState(int state) {
        setMediaPlaybackState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN);
    }

    private void setMediaPlaybackState(int state, long playbackPosition) {
        long actions = PlaybackStateCompat.ACTION_PLAY_PAUSE |
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
            PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID |
            PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH;

        if (state == PlaybackStateCompat.STATE_PLAYING) {
            actions |= PlaybackStateCompat.ACTION_PAUSE;
        } else {
            actions |= PlaybackStateCompat.ACTION_PLAY;
        }

        if (hasScrubber) {
            actions |= PlaybackStateCompat.ACTION_SEEK_TO;
            // We only want to update
            if (playbackPosition == PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN && lastPlaybackState != null) {
                if (state == PlaybackStateCompat.STATE_PLAYING) {
                    playbackPosition = lastPlaybackState.getPosition();
                } else {
                    long delta = SystemClock.elapsedRealtime() - lastPlaybackState.getLastPositionUpdateTime();
                    playbackPosition = lastPlaybackState.getPosition() + delta;
                }
            }
        }

        PlaybackStateCompat playbackState = new PlaybackStateCompat.Builder()
            .setActions(actions)
            .setState(state, playbackPosition, 1.0f)
            .build();
        mediaSessionCompat.setPlaybackState(playbackState);
        lastPlaybackState = playbackState;
    }

    private void updateNotification(boolean isPlaying, long playbackPosition) {
        notification.updateIsPlaying(isPlaying);

        if (isPlaying) {
            setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING, playbackPosition);
        } else {
            setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED, playbackPosition);
        }
    }

    private void cleanUp() {
        notification.destroy();
        mMessageReceiver.stopListening();
        unregisterMediaButtonEvent();
    }
}
