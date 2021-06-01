package com.homerours.musiccontrols;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;

public class MusicControlsService extends Service {

    private final IBinder binder = new MusicControlsBinder();
    private PowerManager.WakeLock wakeLock;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stop();
        super.onDestroy();
    }

    public void start(int notificationId, Notification notification) {
        startForeground(notificationId, notification);

        PowerManager powerManager = ((PowerManager) getSystemService(POWER_SERVICE));
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MusicControlsService::lock");
        wakeLock.acquire();
    }

    public void stop() {
        try {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
                wakeLock = null;
            }
            stopForeground(true);
            stopSelf();
        } catch (Exception e) {
            String errorMessage = e.getLocalizedMessage();
            Log.d("MusicControlsService", errorMessage);
            e.printStackTrace();
        }
    }

    class MusicControlsBinder extends Binder {
        MusicControlsService getService() {
            return MusicControlsService.this;
        }
    }
}