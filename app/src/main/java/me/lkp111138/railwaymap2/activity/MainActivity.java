package me.lkp111138.railwaymap2.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import java.io.IOException;
import java.util.Locale;

import me.lkp111138.railwaymap2.R;
import me.lkp111138.railwaymap2.helpers.CacheManager;

public class MainActivity extends AppCompatActivity {
    private static String IMG_URL = "https://lkp111138.me/rosen_ct_1803.jpg";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // we check if we have the image cached
        CacheManager.getCachedFile(this, IMG_URL, new CacheManager.CacheCallback() {
            ViewGroup loading_dialog;

            @Override
            public void onResponse(String filename) {
                MainActivity.this.runOnUiThread(() -> {
                    // run ui actions in ui thread
                    // and remove the loading dialog here
                    ViewGroup home = (ViewGroup) getWindow().getDecorView().getRootView();
                    home.removeView(loading_dialog);
                    SubsamplingScaleImageView imageView = findViewById(R.id.imageView);
                    imageView.setMaxScale(4.0f);
                    imageView.setImage(ImageSource.uri(filename));
                });
            }

            @Override
            public void onError(IOException e) {
                Toast.makeText(MainActivity.this, "Unable to load map", Toast.LENGTH_SHORT)
                        .show();
                Log.w("MainActivity", "Unable to load map image: " + e.getMessage());
            }

            @Override
            public void onProgress(long bytes_read, long bytes_total) {
                MainActivity.this.runOnUiThread(() -> {
                    TextView progress_tv = loading_dialog.findViewById(R.id.download_progress);
                    progress_tv.setText(String.format(Locale.getDefault(), "%dK / %dK", bytes_read >> 10, bytes_total >> 10));
                });
            }

            @Override
            public void onStartDownload() {
                // show the loading dialog
                MainActivity.this.runOnUiThread(() -> {
                    ViewGroup home = (ViewGroup) getWindow().getDecorView().getRootView();
                    loading_dialog = (ViewGroup) getLayoutInflater().inflate(R.layout.loading_dialog, home);
                });
            }
        });
    }
}
