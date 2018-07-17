package de.brudaswen.picturewatcher.app;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.ortiz.touch.TouchImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;


public class MainActivity extends FragmentActivity {

    private FileObserver observer;
    private File folder;
    private PicturePagerAdapter adapter;
    private Handler handler = new Handler();
    private ViewPager pager;

    @Override
    protected void onStop() {
        super.onStop();
        App.get().setInForeground(false);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        App.get().setInForeground(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        App.get().activity = this;

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    42);
        } else {

            setContentView(R.layout.activity_main);

            folder = new File(Environment.getExternalStorageDirectory(), "DCIM/DSLR");

            pager = findViewById(R.id.view_pager);
            adapter = new PicturePagerAdapter(getSupportFragmentManager());
            pager.setAdapter(adapter);

            observer = new FileObserver(folder.getAbsolutePath(), FileObserver.CREATE) {
                @Override
                public void onEvent(final int event, final String path) {
                    handler.postDelayed(() -> updateState(), 2000);
                }
            };
            observer.startWatching();

            updateState();
        }
    }

    private void updateState() {
        File[] pictures = folder.listFiles((dir, filename) -> filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".JPG") || filename.endsWith(".JPEG"));
        //noinspection ComparatorCombinators minSdk
        Arrays.sort(pictures, (lhs, rhs) -> Long.compare(lhs.lastModified(), rhs.lastModified()));
        adapter.setPictures(pictures);
        pager.setCurrentItem(pictures.length > 0 ? pictures.length - 1 : 0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        App.get().activity = null;
        observer.stopWatching();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static class PicturePagerAdapter extends FragmentStatePagerAdapter {

        private File[] pictures;

        PicturePagerAdapter(FragmentManager fm) {
            super(fm);
            pictures = new File[0];
        }

        @Override
        public Fragment getItem(int position) {
            return PictureFragment.newInstance(pictures[position]);
        }

        //        @Override
        //        public Object instantiateItem(ViewGroup container, int position) {
        //            return getItem(position);
        //        }

        @Override
        public int getCount() {
            return pictures.length;
        }

        void setPictures(File[] pictures) {
            this.pictures = pictures;
            notifyDataSetChanged();
        }
    }

    public static class PictureFragment extends Fragment {

        private String picture;
        private TouchImageView imageView;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (getArguments() != null) {
                picture = getArguments().getString("picture");
            }
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_gallery, container, false);
            imageView = view.findViewById(R.id.img);
            return view;
        }

        @Override
        public void onResume() {
            super.onResume();

            Bitmap bitmap = decodeSampledBitmapFromUri(Uri.fromFile(new File(picture)));

            imageView.setImageBitmap(bitmap);
        }

        /*
         *  How to "Loading Large Bitmaps Efficiently"?
         *  Refer: http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
         */
        Bitmap decodeSampledBitmapFromUri(Uri uri) {

            Bitmap bm = null;

            try {
                ContentResolver resolver = App.get().getContentResolver();

                // First decode with inJustDecodeBounds=true to check dimensions
                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(resolver.openInputStream(uri), null, options);

                // Calculate inSampleSize
                options.inSampleSize = calculateInSampleSize(options);

                // Decode bitmap with inSampleSize set
                options.inJustDecodeBounds = false;
                bm = BitmapFactory.decodeStream(resolver.openInputStream(uri), null, options);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            return bm;
        }

        int calculateInSampleSize(BitmapFactory.Options options) {
            // Raw height and width of image
            final int height = options.outHeight;
            final int width = options.outWidth;
            int inSampleSize = 1;

            if (height > 1280 || width > 1280) {
                if (width > height) {
                    inSampleSize = Math.round((float) height / (float) 1280);
                } else {
                    inSampleSize = Math.round((float) width / (float) 1280);
                }
            }
            return inSampleSize;
        }

        static PictureFragment newInstance(File picture) {
            PictureFragment pictureFragment = new PictureFragment();
            Bundle args = new Bundle();
            args.putString("picture", picture.getAbsolutePath());
            pictureFragment.setArguments(args);
            return pictureFragment;
        }
    }
}
