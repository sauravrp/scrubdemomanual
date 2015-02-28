package com.test.scrubdemo.fragments;


import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.test.scrubdemo.activities.MainActivity;
import com.test.scrubdemo.fragments.base.BaseFragment;
import com.test.scrubdemo.utils.Utils;
import com.test.scrubdemo.R;

import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by sauravrp on 2/25/15.
 */
public class MainFragment extends BaseFragment
{
    private final static String TAG = "MainFragment";

    private final static int DELAY = 150;

    private final String APP_STATE = "test.com.binocular.fragment.Mainfragment.APP_STATE";

    private final String PREFIX = "assets://";

    private final String MATCH_FILE = "frame";


    private static class AppState implements Serializable
    {
        private ArrayList<String> mAssetList;

        private int mSeekBarProgressIndex;

        private LruCache<String, Bitmap> mMemoryCache;

        public AppState()
        {
            mAssetList = new ArrayList<String>();
            mSeekBarProgressIndex = 0;

            // Get max available VM memory, exceeding this amount will throw an
            // OutOfMemory exception. Stored in kilobytes as LruCache takes an
            // int in its constructor.
            final int maxMemory = (int)  Runtime.getRuntime().maxMemory() / 1024;

            // Use 1/8th of the available memory for this memory cache.
            final int cacheSize = maxMemory / 8;

            mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
                @Override
                protected int sizeOf(String key, Bitmap bitmap) {
                    // The cache size will be measured in kilobytes rather than
                    // number of items.
                    return bitmap.getByteCount() / 1024;
                }
            };
        }
    }


    private Handler mHideSeekBarHandler = null;
    private Runnable mHideSeekBarRunnable = null;


    @InjectView(R.id.seek_bar)
    SeekBar mSeekBar;

    @InjectView(R.id.main_frame)
    ImageView mImageView;

    private AppState mAppState;

    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mAppState.mMemoryCache.put(key, bitmap);
        }
    }

    public Bitmap getBitmapFromMemCache(String key) {
        return mAppState.mMemoryCache.get(key);
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null)
        {
            mAppState = new AppState();
            try
            {
                initAssetArrayList();

            } catch (IOException e)
            {
                Utils.LogError("TAG", e.getMessage());
            }
        }
        else
        {
            mAppState = (AppState) savedInstanceState.getSerializable(APP_STATE);
        }
    }

   /* private void initImageLoader()
    {
        DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
          //  .imageScaleType(ImageScaleType.EXACTLY)
            .cacheInMemory(true)
            .cacheOnDisk(true)
            .build();

        ImageLoaderConfiguration configuration = new ImageLoaderConfiguration.Builder(getActivity())
           // .writeDebugLogs()
            .threadPriority(Thread.MAX_PRIORITY) // default
            .defaultDisplayImageOptions(defaultOptions).build();

        ImageLoader.getInstance().init(configuration);

        mImageLoader = ImageLoader.getInstance();
    }*/


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        ButterKnife.inject(this, view);
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        //update progress index
        mAppState.mSeekBarProgressIndex = mSeekBar.getProgress();

        outState.putSerializable(APP_STATE, mAppState);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        showAsset(mAppState.mSeekBarProgressIndex);
    }

    @Override
    public void onStart()
    {
        super.onStart();

        mSeekBar.setMax(mAppState.mAssetList.size() - 1);

        mHideSeekBarRunnable = new Runnable()
        {
            @Override
            public void run()
            {
                setSeekBarVisibility(false);
            }
        };

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                showAsset(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {

            }
        });


        setSeekBarVisibility(false);

        // reference http://developer.android.com/training/gestures/detector.html
        mImageView.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                int action = event.getActionMasked();


                mSeekBar.onTouchEvent(event);

                switch (action)
                {
                    case (MotionEvent.ACTION_DOWN):
                        Utils.LogDebug(TAG, "Action was DOWN");
                        setSeekBarVisibility(true);
                        return true;

                    case (MotionEvent.ACTION_UP):
                        Utils.LogDebug(TAG, "Action was UP");
                        scheduleSeekBarHide();
                        return true;

                    case (MotionEvent.ACTION_MOVE):
                        //   setSeekBarVisibility(true);
                        Utils.LogDebug(TAG, "Action was MOVE");
                        return true;

                    case (MotionEvent.ACTION_CANCEL):
                        scheduleSeekBarHide();
                        Utils.LogDebug(TAG, "Action was CANCEL");
                        return true;

                    case (MotionEvent.ACTION_OUTSIDE):
                        scheduleSeekBarHide();
                        Utils.LogDebug(TAG, "Movement occurred outside bounds " +
                            "of current screen element");
                        return true;

                    default:
                        return false;

                }
            }
        });
        int location[] = new int[2];
        mImageView.getLocationInWindow(location);

    }

    private void scheduleSeekBarHide()
    {
        // if not null, its already scheduled
        if (mHideSeekBarHandler == null)
        {
            mHideSeekBarHandler = new Handler();

            mHideSeekBarHandler.postDelayed(mHideSeekBarRunnable, DELAY);
        }
    }

    private void cancelSeekBarHide()
    {
        if (mHideSeekBarHandler != null)
        {
            mHideSeekBarHandler.removeCallbacks(mHideSeekBarRunnable);
            mHideSeekBarHandler = null;
        }
    }

    @Override
    public void onDetach()
    {
        super.onDetach();

        if (mHideSeekBarHandler != null)
        {
            mHideSeekBarHandler.removeCallbacks(mHideSeekBarRunnable);
        }

    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        if (mHideSeekBarHandler != null)
        {
            mHideSeekBarHandler.removeCallbacks(mHideSeekBarRunnable);
            mHideSeekBarHandler = null;
            mHideSeekBarRunnable = null;
        }

     /*   if(mImageLoader.isInited())
        {
            mImageLoader.destroy();
        }*/

    }

    private void setSeekBarVisibility(boolean visible)
    {
        if (visible)
        {
            Utils.LogDebug(TAG, "seek bar visible");
            cancelSeekBarHide();
            mSeekBar.setVisibility(View.VISIBLE);
        }
        else
        {
            Utils.LogDebug(TAG, "seek bar invisible");
            mSeekBar.setVisibility(View.INVISIBLE);
        }
    }

    // contains files other than frame*.png
    private void initAssetArrayList() throws IOException
    {
        String asset[] = getResources().getAssets().list("");

        for (int i = 0; i < asset.length; i++)
        {
            if (asset[i].contains(MATCH_FILE))
            {
                mAppState.mAssetList.add(asset[i]);
            }
        }

        /**
         * Reference: http://codereview.stackexchange.com/questions/37192/number-aware-string-sorting-with-comparator
         */
        Collections.sort(mAppState.mAssetList, new Comparator<String>()
        {
            @Override
            public int compare(String lhs, String rhs)
            {
                String[] lhsParts = lhs.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
                String[] rhsParts = rhs.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
                if (lhsParts.length == 3 && rhsParts.length == 3)
                {
                    int lhsInt = Integer.parseInt(lhsParts[1]);
                    int rhsInt = Integer.parseInt(rhsParts[1]);

                    return lhsInt - rhsInt;
                }
                return 0;
            }
        });
    }

    private String getAssetFileName(int index)
    {
        StringBuilder builder = new StringBuilder(PREFIX);

        String path = mAppState.mAssetList.get(index);
        Utils.LogDebug(TAG, "showAsset called for path = " + path);

        builder.append(path);

        String uri = builder.toString();
        Utils.LogDebug(TAG, "asset uri path = " + uri);

        return uri;
    }

    private void showAsset(int index)
    {
        if (mAppState.mAssetList != null && index < mAppState.mAssetList.size())
        {
            String fileName = mAppState.mAssetList.get(index);

            final Bitmap bitmap = getBitmapFromMemCache(fileName);
            if(bitmap != null)
            {
                Log.d(TAG, "loading " + fileName + " from cache");
                mImageView.setImageBitmap(bitmap);
            }
            else {

                if (cancelPotentialWork(fileName, mImageView))
                {
                    BitmapWorkerTask workerTask = new BitmapWorkerTask(getActivity(), mImageView, fileName);
                    final AsyncDrawable asyncDrawable =
                        new AsyncDrawable(workerTask);
                    mImageView.setImageDrawable(asyncDrawable);
                    Log.d(TAG, "Executing workerTask to download fileName = " + fileName);
                    workerTask.execute(fileName);
                }
            }



            // Bitmap bitmap = Utils.decodeSampledBitmapFromAsset(getResources(), fileName, mImageView.getWidth(), mImageView.getHeight());
            // mImageView.setImageBitmap(bitmap);

        }

    }

    private void showTime(int index, Long time)
    {
        getActivity().setTitle("Frame # : " + Integer.toString(index) + "," + Long.toString(time));
    }


    private class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap>
    {
        private WeakReference<Activity> activityWeakReference;
        private WeakReference<ImageView> imageViewWeakReference;

        private String mFileName;

        private long mStartTime;

        public BitmapWorkerTask(Activity activity, ImageView imageView, String filename)
        {
            activityWeakReference = new WeakReference<Activity>(activity);
            imageViewWeakReference = new WeakReference<ImageView>(imageView);
            mFileName = filename;
        }

        public String getFileName()
        {
            return mFileName;
        }

        @Override
        protected void onPreExecute()
        {
            mStartTime = System.currentTimeMillis();
        }

        @Override
        protected void onPostExecute(Bitmap bitmap)
        {
            Log.d(TAG, "onPostExecute for filename = " + mFileName);

            if (isCancelled())
            {
                bitmap = null;
            }

            if (imageViewWeakReference != null && bitmap != null)
            {
                final ImageView imageView = imageViewWeakReference.get();
                final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

                if ((this == bitmapWorkerTask) && imageView != null)
                {
                    imageView.setImageBitmap(bitmap);


                }
            }

            if (activityWeakReference != null)
            {
                Activity activity = activityWeakReference.get();
                if (activity != null && activity instanceof MainActivity)
                {
                    long timeMillis = System.currentTimeMillis() - mStartTime;
                    ((MainActivity) activity).showTime(timeMillis);
                }
            }

        }

        @Override
        protected Bitmap doInBackground(String... params)
        {
            Log.d(TAG, "doInBackground => loading filename = " + params[0]);

            try
            {
                final Bitmap bitmap = Utils.decodeSampledBitmapFromAsset(
                    activityWeakReference.get().getResources(),
                    params[0],
                    imageViewWeakReference.get().getWidth(),
                    imageViewWeakReference.get().getHeight());

                Log.d(TAG, "loaded bitmap size = " + bitmap.getByteCount() / 1024 + " kilobytes");

                addBitmapToMemoryCache(mFileName, bitmap);
                return bitmap;
            } catch (IOException e)
            {
                e.printStackTrace();
            }

            return null;
        }
    }

    static class AsyncDrawable extends BitmapDrawable
    {
        private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskWeakReference;

        public AsyncDrawable(BitmapWorkerTask bitmapWorkerTaskWeakReference)
        {
            super(Utils.getBitmap());
            this.bitmapWorkerTaskWeakReference = new WeakReference<BitmapWorkerTask>(bitmapWorkerTaskWeakReference);
        }

        public BitmapWorkerTask getBitmapWorkerTask()
        {
            return bitmapWorkerTaskWeakReference.get();
        }
    }

    private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView)
    {
        if (imageView != null)
        {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable)
            {
                AsyncDrawable downloadedDrawable = (AsyncDrawable) drawable;
                return downloadedDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    public static boolean cancelPotentialWork(String aboutToDownloadFileName, ImageView imageView)
    {
        BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
        if (bitmapWorkerTask != null)
        {
            String downloadingFileName = bitmapWorkerTask.getFileName();

            if ((downloadingFileName == null) || (!downloadingFileName.equals(aboutToDownloadFileName)))
            {
                Log.d(TAG, "cancelling task for filename = " + downloadingFileName);
                bitmapWorkerTask.cancel(true);
            }
            else
            {
                // same file is already downloading
                return false;
            }
        }

        return true;
    }


}
