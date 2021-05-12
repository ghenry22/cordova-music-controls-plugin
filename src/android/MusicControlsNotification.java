package com.homerours.musiccontrols;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.core.app.NotificationCompat;

public class MusicControlsNotification {
    private static final String CHANNEL_ID = "cordova-music-channel-id";

    private final Context context;
    private final NotificationManager notificationManager;
    private final int notificationID;
    private MusicControlsInfoModel infos;
    private Bitmap bitmapCover;
    private final MediaSessionCompat.Token mediaSession;

    public MusicControlsNotification(Context context, int id, MediaSessionCompat.Token mediaSesion) {
        this.notificationID = id;
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.mediaSession = mediaSesion;

        // use channelid for Oreo and higher
        if (Build.VERSION.SDK_INT >= 26) {
            // The user-visible name of the channel.
            CharSequence name = "Audio Controls";
            // The user-visible description of the channel.
            String description = "Control Playing Audio";

            int importance = NotificationManager.IMPORTANCE_LOW;

            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);

            // Configure the notification channel.
            mChannel.setDescription(description);

            this.notificationManager.createNotificationChannel(mChannel);
        }
    }

    public void updateNotification(MusicControlsInfoModel newInfos) {
        if (!newInfos.cover.isEmpty() && (this.infos == null || !newInfos.cover.equals(this.infos.cover))) {
            bitmapCover = BitmapUtils.getBitmapCover(context, newInfos.cover);
        }
        this.infos = newInfos;
        Notification notif = buildNotification();
        this.notificationManager.notify(this.notificationID, notif);
    }

    public void updateIsPlaying(boolean isPlaying) {
        this.infos.isPlaying = isPlaying;
        Notification notif = buildNotification();
        this.notificationManager.notify(this.notificationID, notif);
    }

    public void updateDismissable(boolean dismissable) {
        this.infos.dismissable = dismissable;
        Notification notif = buildNotification();
        this.notificationManager.notify(this.notificationID, notif);
    }

    private Notification buildNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID);

        //Configure builder
        builder.setContentTitle(infos.track);
        if (!infos.artist.isEmpty()) {
            builder.setContentText(infos.artist);
        }
        builder.setWhen(0);

        // set if the notification can be destroyed by swiping
        if (infos.dismissable) {
            builder.setOngoing(false);
            Intent dismissIntent = new Intent("music-controls-destroy");
            PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(context, 1, dismissIntent, 0);
            builder.setDeleteIntent(dismissPendingIntent);
        } else {
            builder.setOngoing(true);
        }
        if (!infos.ticker.isEmpty()) {
            builder.setTicker(infos.ticker);
        }

        builder.setPriority(Notification.PRIORITY_MAX);

        builder.setVisibility(Notification.VISIBILITY_PUBLIC);

        //Set SmallIcon
        boolean usePlayingIcon = infos.notificationIcon.isEmpty();
        if (!usePlayingIcon) {
            int resId = this.getResourceId(infos.notificationIcon, 0);
            usePlayingIcon = resId == 0;
            if (!usePlayingIcon) {
                builder.setSmallIcon(resId);
            }
        }

        if (usePlayingIcon) {
            if (infos.isPlaying) {
                builder.setSmallIcon(this.getResourceId(infos.playIcon, android.R.drawable.ic_media_play));
            } else {
                builder.setSmallIcon(this.getResourceId(infos.pauseIcon, android.R.drawable.ic_media_pause));
            }
        }

        //Set LargeIcon
        if (!infos.cover.isEmpty() && this.bitmapCover != null) {
            builder.setLargeIcon(this.bitmapCover);
        }

        //Open app if tapped
        Intent resultIntent = new Intent(context, context.getClass());
        resultIntent.setAction(Intent.ACTION_MAIN);
        resultIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, 0);
        builder.setContentIntent(resultPendingIntent);

        //Controls
        int nbControls = 0;
        /* Previous  */
        if (infos.hasPrev) {
            nbControls++;
            Intent previousIntent = new Intent("music-controls-previous");
            PendingIntent previousPendingIntent = PendingIntent.getBroadcast(context, 1, previousIntent, 0);
            builder.addAction(this.getResourceId(infos.prevIcon, android.R.drawable.ic_media_previous), "", previousPendingIntent);
        }
        if (infos.isPlaying) {
            /* Pause  */
            nbControls++;
            Intent pauseIntent = new Intent("music-controls-pause");
            PendingIntent pausePendingIntent = PendingIntent.getBroadcast(context, 1, pauseIntent, 0);
            builder.addAction(this.getResourceId(infos.pauseIcon, android.R.drawable.ic_media_pause), "", pausePendingIntent);
        } else {
            /* Play  */
            nbControls++;
            Intent playIntent = new Intent("music-controls-play");
            PendingIntent playPendingIntent = PendingIntent.getBroadcast(context, 1, playIntent, 0);
            builder.addAction(this.getResourceId(infos.playIcon, android.R.drawable.ic_media_play), "", playPendingIntent);
        }
        /* Next */
        if (infos.hasNext) {
            nbControls++;
            Intent nextIntent = new Intent("music-controls-next");
            PendingIntent nextPendingIntent = PendingIntent.getBroadcast(context, 1, nextIntent, 0);
            builder.addAction(this.getResourceId(infos.nextIcon, android.R.drawable.ic_media_next), "", nextPendingIntent);
        }
        /* Close */
        if (infos.hasClose) {
            nbControls++;
            Intent destroyIntent = new Intent("music-controls-destroy");
            PendingIntent destroyPendingIntent = PendingIntent.getBroadcast(context, 1, destroyIntent, 0);
            builder.addAction(this.getResourceId(infos.closeIcon, android.R.drawable.ic_menu_close_clear_cancel), "", destroyPendingIntent);
        }

        int[] args = new int[nbControls];
        for (int i = 0; i < nbControls; ++i) {
            args[i] = i;
        }

        builder.setStyle(
            new androidx.media.app.NotificationCompat.DecoratedMediaCustomViewStyle()
                .setMediaSession(mediaSession)
                .setShowActionsInCompactView(args)
        );

        return builder.build();
    }

    private int getResourceId(String name, int fallback) {
        try {
            if (name.isEmpty()) {
                return fallback;
            }

            int resId = context.getResources().getIdentifier(name, "drawable", context.getPackageName());
            return resId == 0 ? fallback : resId;
        } catch (Exception ex) {
            return fallback;
        }
    }

    public void destroy() {
        this.notificationManager.cancel(this.notificationID);
    }
}
