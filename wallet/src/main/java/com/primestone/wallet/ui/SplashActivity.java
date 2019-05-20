package com.primestone.wallet.ui;

import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.VideoView;
import com.primestone.wallet.R;

public class SplashActivity extends AppCompatActivity {
    private MediaPlayer.OnCompletionListener mCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            Intent intent = new Intent(SplashActivity.this, WalletActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            SplashActivity.this.startActivity(intent);
            SplashActivity.this.finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splesh);
//        if (VERSION.SDK_INT > 21) {
//            getWindow().setStatusBarColor(getResources().getColor(R.color.black));
//        }
        /*
        VideoView videoView = (VideoView) findViewById(R.id.videoView);
        videoView.setVideoURI(Uri.parse("android.resource://com.primestone.wallet/" + R.raw.new_video_9));
        videoView.setOnCompletionListener(mCompletionListener);
        videoView.start();
        */
        // Remove this code at the time of completion
        Intent intent = new Intent(SplashActivity.this, WalletActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        SplashActivity.this.startActivity(intent);
        SplashActivity.this.finish();
    }
}
