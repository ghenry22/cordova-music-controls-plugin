package com.homerours.musiccontrols;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

import org.apache.cordova.CallbackContext;

public class MusicControlsBroadcastReceiver extends BroadcastReceiver {
    private CallbackContext cb;
    private final MusicControlsPlugin musicControlsPlugin;


    public MusicControlsBroadcastReceiver(MusicControlsPlugin musicControlsPlugin) {
        this.musicControlsPlugin = musicControlsPlugin;
    }

    public void setCallback(CallbackContext cb) {
        this.cb = cb;
    }

    public void stopListening() {
        if (this.cb != null) {
            sendMessage("music-controls-stop-listening");
            this.cb = null;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (this.cb == null) return;
        String message = intent.getAction();
        switch (message) {
            case Intent.ACTION_HEADSET_PLUG:
                // Headphone plug/unplug
                int state = intent.getIntExtra("state", -1);
                switch (state) {
                    case 0:
                        sendMessage("music-controls-headset-unplugged");
                        this.musicControlsPlugin.unregisterMediaButtonEvent();
                        break;
                    case 1:
                        sendMessage("music-controls-headset-plugged");
                        this.musicControlsPlugin.registerMediaButtonEvent();
                        break;
                    default:
                        break;
                }
                break;
            case "music-controls-media-button":
                KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
                    int keyCode = event.getKeyCode();
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_MEDIA_NEXT:
                            sendMessage("music-controls-media-button-next");
                            break;
                        case KeyEvent.KEYCODE_MEDIA_PAUSE:
                            sendMessage("music-controls-media-button-pause");
                            break;
                        case KeyEvent.KEYCODE_MEDIA_PLAY:
                            sendMessage("music-controls-media-button-play");
                            break;
                        case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                            sendMessage("music-controls-media-button-play-pause");
                            break;
                        case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                            sendMessage("music-controls-media-button-previous");
                            break;
                        case KeyEvent.KEYCODE_MEDIA_STOP:
                            sendMessage("music-controls-media-button-stop");
                            break;
                        case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                            sendMessage("music-controls-media-button-fast-forward");
                            break;
                        case KeyEvent.KEYCODE_MEDIA_REWIND:
                            sendMessage("music-controls-media-button-rewind");
                            break;
                        case KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD:
                            sendMessage("music-controls-media-button-skip-backward");
                            break;
                        case KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD:
                            sendMessage("music-controls-media-button-skip-forward");
                            break;
                        case KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD:
                            sendMessage("music-controls-media-button-step-backward");
                            break;
                        case KeyEvent.KEYCODE_MEDIA_STEP_FORWARD:
                            sendMessage("music-controls-media-button-step-forward");
                            break;
                        case KeyEvent.KEYCODE_META_LEFT:
                            sendMessage("music-controls-media-button-meta-left");
                            break;
                        case KeyEvent.KEYCODE_META_RIGHT:
                            sendMessage("music-controls-media-button-meta-right");
                            break;
                        case KeyEvent.KEYCODE_MUSIC:
                            sendMessage("music-controls-media-button-music");
                            break;
                        case KeyEvent.KEYCODE_VOLUME_UP:
                            sendMessage("music-controls-media-button-volume-up");
                            break;
                        case KeyEvent.KEYCODE_VOLUME_DOWN:
                            sendMessage("music-controls-media-button-volume-down");
                            break;
                        case KeyEvent.KEYCODE_VOLUME_MUTE:
                            sendMessage("music-controls-media-button-volume-mute");
                            break;
                        case KeyEvent.KEYCODE_HEADSETHOOK:
                            sendMessage("music-controls-media-button-headset-hook");
                            break;
                        default:
                            sendMessage(message);
                            break;
                    }
                }
                break;
            case "music-controls-destroy":
                // Close Button
                sendMessage("music-controls-destroy");
                this.musicControlsPlugin.destroyPlayerNotification();
                break;
            default:
                sendMessage(message);
                break;
        }
    }

    private void sendMessage(String message) {
        if (this.cb != null) {
            this.cb.success("{\"message\": \"" + message + "\"}");
        }
        this.cb = null;
    }
}
