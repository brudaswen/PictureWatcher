package de.brudaswen.picturewatcher.app;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.ortiz.touch.TouchImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Comparator;

import de.brudaswen.picturewatcher.app.service.PictureWatcherService;


public class MainActivity extends FragmentActivity {

    private FileObserver observer;
    private File folder;
    private PicturePagerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        folder = new File(Environment.getExternalStorageDirectory(), "DCIM/Camera");

        ViewPager pager = (ViewPager) findViewById(R.id.view_pager);
        adapter = new PicturePagerAdapter(getSupportFragmentManager());
        pager.setAdapter(adapter);

        Intent service = new Intent(this, PictureWatcherService.class);
        startService(service);

        observer = new FileObserver(folder.getAbsolutePath(), FileObserver.CREATE) {
            @Override
            public void onEvent(int event, String path) {
                updateState();
            }
        };
        observer.startWatching();

        updateState();
    }

    private void updateState() {
        File[] pictures = folder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".JPG") || filename.endsWith(".JPEG");
            }
        });
        Arrays.sort(pictures, new Comparator<File>() {
            @Override
            public int compare(File lhs, File rhs) {
                if(lhs.lastModified() > rhs.lastModified()) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });
        adapter.setPictures(pictures);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        observer.startWatching();
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

        public PicturePagerAdapter(FragmentManager fm) {
            super(fm);
            pictures = new File[0];
        }

        @Override
        public Fragment getItem(int position) {
            return PictureFragment.newInstance(pictures[position]);
        }

        @Override
        public int getCount() {
            return pictures.length;
        }

        public void setPictures(File[] pictures) {
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

            picture = getArguments().getString("picture");
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_gallery, container, false);
            imageView = (TouchImageView) view.findViewById(R.id.img);
            return view;
        }

        @Override
        public void onResume() {
            super.onResume();

            Bitmap bitmap = decodeSampledBitmapFromUri(
                    Uri.fromFile(new File(picture)),
                    1280, 1280);

            imageView.setImageBitmap(bitmap);
        }

        /*
         *  How to "Loading Large Bitmaps Efficiently"?
         *  Refer: http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
         */
        public Bitmap decodeSampledBitmapFromUri(Uri uri, int reqWidth, int reqHeight) {

            Bitmap bm = null;

            try {
                ContentResolver resolver = getActivity().getContentResolver();

                // First decode with inJustDecodeBounds=true to check dimensions
                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(resolver.openInputStream(uri), null, options);

                // Calculate inSampleSize
                options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

                // Decode bitmap with inSampleSize set
                options.inJustDecodeBounds = false;
                bm = BitmapFactory.decodeStream(resolver.openInputStream(uri), null, options);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            return bm;
        }

        public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
            // Raw height and width of image
            final int height = options.outHeight;
            final int width = options.outWidth;
            int inSampleSize = 1;

            if (height > reqHeight || width > reqWidth) {
                if (width > height) {
                    inSampleSize = Math.round((float) height / (float) reqHeight);
                } else {
                    inSampleSize = Math.round((float) width / (float) reqWidth);
                }
            }
            return inSampleSize;
        }

        public static PictureFragment newInstance(File picture) {
            PictureFragment pictureFragment = new PictureFragment();
            Bundle args = new Bundle();
            args.putString("picture", picture.getAbsolutePath());
            pictureFragment.setArguments(args);
            return pictureFragment;
        }
    }
}