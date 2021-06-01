package com.homerours.musiccontrols;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;

public class MusicControlsNotificationKillerService extends Service {

    private final IBinder mBinder = new KillBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d("MusicControls", "DESTROY");
        NotificationManagerCompat.from(this).cancelAll();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d("MusicControls", "GONE");
        NotificationManagerCompat.from(this).cancelAll();
    }

    class KillBinder extends Binder {
        MusicControlsNotificationKillerService getService() {
            return MusicControlsNotificationKillerService.this;
        }
    }
}
