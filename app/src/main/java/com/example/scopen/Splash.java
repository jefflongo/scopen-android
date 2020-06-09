package com.example.scopen;

import androidx.appcompat.app.AppCompatActivity;
import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;

public class Splash extends AppCompatActivity {
    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        Boolean isFirstRun = getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                .getBoolean("isFirstRun", true);
        if (isFirstRun) {
            setContentView(R.layout.activity_splash);
            int secondsDelayed = 4;
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    getSharedPreferences("PREFERENCE", MODE_PRIVATE).edit()
                            .putBoolean("isFirstRun", false).commit();
                    Intent goToMainActivity = new Intent(Splash.this, MainActivity.class);
                    ActivityOptions options = ActivityOptions.makeCustomAnimation(Splash.this,android.R.anim.fade_in,android.R.anim.fade_out);
                    goToMainActivity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(goToMainActivity,options.toBundle());
                    finish();
                }
            }, secondsDelayed * 1000);

        }
        else{
            setContentView(R.layout.activity_splash);
            Intent goToMainActivity = new Intent(Splash.this, MainActivity.class);
            goToMainActivity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(goToMainActivity);
            finish();

        }

        ImageView imageView = (ImageView) findViewById(R.id.imageView);
        Glide.with(this).asGif().load(R.drawable.scopen_logo).transition(withCrossFade()).apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE)).into(imageView);
    }

}
