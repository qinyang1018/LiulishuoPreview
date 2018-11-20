package me.wangyuwei.liulishuopreview;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;

public class MainActivity extends AppCompatActivity {

    private PreviewVideoView mVideoView;
    private ViewPager mVpImage;
    private PreviewIndicator mIndicator;
    private ImageView imageView;

    private List<View> mViewList = new ArrayList<>();
    private int[] mImageResIds = new int[]{R.mipmap.intro_text_1, R.mipmap.intro_text_2, R.mipmap.intro_text_3};
    private CustomPagerAdapter mAdapter;

    private int mCurrentPage = 0;
    private Subscription mLoop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mVideoView = (PreviewVideoView) findViewById(R.id.vv_preview);
        mVpImage = (ViewPager) findViewById(R.id.vp_image);
        mIndicator = (PreviewIndicator) findViewById(R.id.indicator);
        imageView = (ImageView) findViewById(R.id.image_view);

        mVideoView.setVideoURI(Uri.parse(getVideoPath()));

        for (int i = 0; i < mImageResIds.length; i++) {
            View view = LayoutInflater.from(this).inflate(R.layout.preview_item, null, false);
            ((ImageView) view.findViewById(R.id.iv_intro_text)).setImageResource(mImageResIds[i]);
            mViewList.add(view);
        }

        mAdapter = new CustomPagerAdapter(mViewList);
        mVpImage.setAdapter(mAdapter);
        mVpImage.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                mCurrentPage = position;
                mIndicator.setSelected(mCurrentPage);
                startLoop();
//                start();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        MediaMetadataRetriever media = new MediaMetadataRetriever();
        media.setDataSource(this, Uri.parse(getVideoPath()));
        //获取第一帧
        Bitmap bitmap = media.getFrameAtTime(1, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
        imageView.setImageBitmap(bitmap);
        media.release();

//      start();
        startLoop();

        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
                    @Override
                    public boolean onInfo(MediaPlayer mediaPlayer, int what, int i1) {

                        //开始播放时，就把显示第一帧的ImageView gone 掉
                        if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                            // video started; hide the placeholder.
                            imageView.setVisibility(View.GONE);
                            //videoView.seekTo(0);

                            return true;
                        }
                        return false;
                    }
                });
            }
        });


    }

    /**
     * 获取video文件的路径
     *
     * @return 路径
     */
    private String getVideoPath() {
        return "android.resource://" + this.getPackageName() + "/" + R.raw.intro_video;
    }

    /**
     * 开启轮询
     */
    private void startLoop() {
        if (null != mLoop) {
            mLoop.unsubscribe();
        }
        mLoop = Observable.interval(0, 6 * 1000, TimeUnit.MILLISECONDS)
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        if (0 == mCurrentPage) {
                            mVideoView.seekTo(mCurrentPage * 6 * 1000);
                        } else {
                            mVideoView.seekTo((1 + mCurrentPage * 6) * 1000);
                        }
                        if (!mVideoView.isPlaying()) {
                            mVideoView.start();
                        }
                    }
                });
    }

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (0 == mCurrentPage) {
                mVideoView.seekTo(mCurrentPage * 6 * 1000);
            } else {
                mVideoView.seekTo((1 + mCurrentPage * 6) * 1000);
            }
            if (!mVideoView.isPlaying()) {
                mVideoView.start();
            }
        }
    };

    private Timer timer;
    private TimerTask task;

    private void start() {
        if (null != timer) {
            timer.cancel();
        }
        task = new TimerTask() {
            @Override
            public void run() {
                mHandler.sendEmptyMessage(0);
            }
        };

        timer = new Timer();
        // 参数：
        // 1000，延时1秒后执行。
        // 2000，每隔2秒执行1次task。
        timer.schedule(task, 0, 6000);

    }

    @Override
    protected void onDestroy() {
        if (null != mLoop) {
            mLoop.unsubscribe();
        }
        super.onDestroy();
    }

    public static class CustomPagerAdapter extends PagerAdapter {

        private List<View> mViewList;

        public CustomPagerAdapter(List<View> viewList) {
            mViewList = viewList;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            container.addView(mViewList.get(position));
            return mViewList.get(position);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView(mViewList.get(position));
        }

        @Override
        public int getCount() {
            return mViewList.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
    }

}
