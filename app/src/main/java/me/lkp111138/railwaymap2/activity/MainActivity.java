package me.lkp111138.railwaymap2.activity;

import android.annotation.SuppressLint;
import android.graphics.PointF;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import org.json.JSONException;

import java.io.IOException;
import java.util.Locale;

import me.lkp111138.railwaymap2.R;
import me.lkp111138.railwaymap2.helpers.CacheManager;
import me.lkp111138.railwaymap2.helpers.StationDetector;

public class MainActivity extends AppCompatActivity {
    private ViewGroup loading_dialog;
    private StationDetector.Station selected_station;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // we check if we have the image cached
        loadMap("tokyo_rosen_ct_1803");
    }

    @SuppressLint("ClickableViewAccessibility")
    private void onImageResponse(String filename) {
        runOnUiThread(() -> {
            // run ui actions in ui thread
            // and remove the loading dialog here
            ViewGroup home = (ViewGroup) getWindow().getDecorView().getRootView();
            home.removeView(loading_dialog);
            loading_dialog = null; // we no longer need this view
            SubsamplingScaleImageView map = findViewById(R.id.map_view);
            map.setMaxScale(4.0f);
            SearchView search_field = findViewById(R.id.search_field);
            search_field.requestFocus();
            map.setOnTouchListener((v, event) -> {
                // these coords are relative to the top-left edge of the view
                if (event.getPointerCount() > 1) {
                    // it may be a gesture and not a tap, so ignore
                    return false;
                }
                if (event.getAction() != MotionEvent.ACTION_UP) {
                    // the touch isnt finalized, abort
                    return false;
                }
                double w = map.getWidth();
                double h = map.getHeight();
                PointF map_center = map.getCenter();
                double center_x = w / 2.0;
                double center_y = h / 2.0;
                if (map_center != null) {
                    double click_x = map_center.x + (event.getX() - center_x) / map.getScale();
                    double click_y = map_center.y + (event.getY() - center_y) / map.getScale();
                    System.out.printf("clicked on %f %f\n", click_x, click_y);
                    StationDetector.Station station = StationDetector.fromCoords(click_x, click_y);
                    // store no matter what since it is intended to be null if the
                    // user clicks away
                    selected_station = station;
                    if (station != null) {
                        // the user clicked on a station, handle it and consume the event
                        System.out.printf("clicked on %s\n", station);
                        search_field.setQuery(station.name, true);
                        return true;
                    }
                }
                return false;
            });
            map.setImage(ImageSource.uri(filename));
        });
    }

    private void onJsonResponse(String filename) {
        runOnUiThread(() -> {
            // run ui actions in ui thread
            // and remove the loading dialog here
            ViewGroup home = (ViewGroup) getWindow().getDecorView().getRootView();
            home.removeView(loading_dialog);
            loading_dialog = null; // we no longer need this view
            try {
                StationDetector.load(filename);
            } catch (IOException|JSONException e) {
                e.printStackTrace();
                // it turns out the json file is invalid, lets download it again on next launch
                CacheManager.invalidate(this, filename);
                Toast.makeText(this, "Unable to load station data", Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }

    private void onDownloadProgress(long bytes_read, long bytes_total) {
        runOnUiThread(() -> {
            TextView progress_tv = loading_dialog.findViewById(R.id.download_progress);
            progress_tv.setText(String.format(Locale.getDefault(), "%dK / %dK", bytes_read >> 10, bytes_total >> 10));
            ProgressBar bar = loading_dialog.findViewById(R.id.download_bar);
            bar.setProgress((int) (bar.getMax() * bytes_read / bytes_total));
        });
    }

    private void onDownload() {
        // show the loading dialog
        runOnUiThread(() -> {
            ViewGroup home = (ViewGroup) getWindow().getDecorView().getRootView();
            // you need the 3rd arg to have loading_dialog store the correct view
            loading_dialog = (ViewGroup) getLayoutInflater().inflate(R.layout.loading_dialog, home, false);
            home.addView(loading_dialog);
        });
    }

    private void loadMap(String map) {
        CacheManager.getCachedFile(this, "https://lkp111138.me/" + map + ".jpg", new CacheManager.CacheCallback() {
            @Override
            public void onResponse(String filename) {
                onImageResponse(filename);
                loadJson(map);
            }

            @Override
            public void onError(IOException e) {
                Toast.makeText(MainActivity.this, "Unable to load map", Toast.LENGTH_SHORT)
                        .show();
                Log.w("MainActivity", "Unable to load map image: " + e.getMessage());
            }

            @Override
            public void onProgress(long bytes_read, long bytes_total) {
                onDownloadProgress(bytes_read, bytes_total);
            }

            @Override
            public void onStartDownload() {
                // show the loading dialog
                onDownload();
            }
        });
    }

    private void loadJson(String map) {
        CacheManager.getCachedFile(this, "https://lkp111138.me/" + map + ".json", new CacheManager.CacheCallback() {
            @Override
            public void onResponse(String filename) {
                onJsonResponse(filename);
            }

            @Override
            public void onError(IOException e) {
                Toast.makeText(MainActivity.this, "Unable to load station data", Toast.LENGTH_SHORT)
                        .show();
                Log.w("MainActivity", "Unable to load station data: " + e.getMessage());
            }

            @Override
            public void onProgress(long bytes_read, long bytes_total) {
                onDownloadProgress(bytes_read, bytes_total);
            }

            @Override
            public void onStartDownload() {
                // show the loading dialog
                onDownload();
            }
        });
    }
}
