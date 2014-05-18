package de.brudaswen.picturewatcher.app.service;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.FileObserver;
import android.os.IBinder;

import java.io.File;

public class PictureWatcherService extends Service {

    private FileObserver observer;

    public PictureWatcherService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        final File folder = new File("/sdcard/DCIM/Camera");
        observer = new FileObserver(folder.getAbsolutePath(), FileObserver.CREATE) {
            @Override
            public void onEvent(int event, String path) {
                System.out.println("PictureWatcherService.onEvent path=" + path);
                Uri uri = Uri.fromFile(new File(folder, path));
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "image/*");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        };
        observer.startWatching();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        observer.startWatching();
    }
}
