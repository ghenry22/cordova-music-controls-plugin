package com.homerours.musiccontrols;

import android.content.Intent;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.KeyEvent;

import org.apache.cordova.CallbackContext;

public class MediaSessionCallback extends MediaSessionCompat.Callback {

    private CallbackContext cb;

    public void setCallback(CallbackContext cb) {
        this.cb = cb;
    }

    @Override
    public void onPlay() {
        super.onPlay();
        sendMessage("music-controls-media-button-play");
    }

    @Override
    public void onPause() {
        super.onPause();
        sendMessage("music-controls-media-button-pause");
    }

    @Override
    public void onSkipToNext() {
        super.onSkipToNext();
        sendMessage("music-controls-media-button-next");
    }

    @Override
    public void onSkipToPrevious() {
        super.onSkipToPrevious();
        sendMessage("music-controls-media-button-previous");
    }

    @Override
    public void onSeekTo(long pos) {
        super.onSeekTo(pos);
        if (this.cb != null) {
            this.cb.success("{" +
                "\"message\": \"music-controls-seek-to\"," +
                "\"position\": " + pos +
                "}");
            this.cb = null;
        }
    }

    @Override
    public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
        final KeyEvent event = (KeyEvent) mediaButtonIntent.getExtras().get(Intent.EXTRA_KEY_EVENT);

        if (event == null) {
            return super.onMediaButtonEvent(mediaButtonIntent);
        }

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            final int keyCode = event.getKeyCode();
            switch (keyCode) {
                case KeyEvent.KEYCODE_MEDIA_PAUSE:

                    sendMessage("music-controls-media-button-pause");
                    break;
                case KeyEvent.KEYCODE_MEDIA_PLAY:

                    sendMessage("music-controls-media-button-play");
                    break;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:

                    sendMessage("music-controls-media-button-previous");
                    break;
                case KeyEvent.KEYCODE_MEDIA_NEXT:

                    sendMessage("music-controls-media-button-next");
                    break;
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:

                    sendMessage("music-controls-media-button-play-pause");
                    break;
                case KeyEvent.KEYCODE_MEDIA_STOP:

                    sendMessage("music-controls-media-button-stop");
                    break;
                case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:

                    sendMessage("music-controls-media-button-forward");
                    break;
                case KeyEvent.KEYCODE_MEDIA_REWIND:

                    sendMessage("music-controls-media-button-rewind");
                    break;
                default:
                    sendMessage("music-controls-media-button-unknown-" + keyCode);
                    return super.onMediaButtonEvent(mediaButtonIntent);
            }
        }

        return true;
    }

    private void sendMessage(String message) {
        if (this.cb != null) {
            this.cb.success("{\"message\": \"" + message + "\"}");
            this.cb = null;
        }
    }
}

