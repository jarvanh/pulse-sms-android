/*
 * Copyright (C) 2016 Jacob Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.klinker.giphy;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import com.lapism.arrow.ArrowDrawable;
import com.lapism.searchview.view.SearchView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import xyz.klinker.giphy.R;

public class GiphyActivity extends Activity {

    public static final String EXTRA_API_KEY = "api_key";
    public static final String EXTRA_SIZE_LIMIT = "size_limit";

    private GiphyHelper helper;
    private ImageView backArrow;

    private RecyclerView recycler;
    private View progressSpinner;

    private GifSearchAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent().getExtras() == null ||
                !getIntent().getExtras().containsKey(EXTRA_API_KEY)) {
            throw new RuntimeException("EXTRA_API_KEY is required!");
        }

        helper = new GiphyHelper(getIntent().getExtras().getString(EXTRA_API_KEY),
                getIntent().getExtras().getLong(EXTRA_SIZE_LIMIT, GiphyHelper.NO_SIZE_LIMIT));

        try {
            getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        } catch (Exception e) {

        }

        setContentView(R.layout.giffy_search_activity);

        recycler = (RecyclerView) findViewById(R.id.recycler_view);
        progressSpinner = findViewById(R.id.list_progress);
        backArrow = (ImageView) findViewById(R.id.imageView_arrow_back);
        SearchView toolbar = (SearchView) findViewById(R.id.searchView);

        final ArrowDrawable drawable = new ArrowDrawable(this);
        drawable.animate(ArrowDrawable.STATE_ARROW);
        backArrow.setImageDrawable(drawable);

        toolbar.setOnSearchMenuListener(new SearchView.SearchMenuListener() {
            @Override
            public void onMenuClick() {
                setResult(Activity.RESULT_CANCELED);
                finish();
            }
        });

        toolbar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                executeQuery(query);
                backArrow.performClick();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        recycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                adapter.releaseVideo();
            }
        });

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                drawable.animate(ArrowDrawable.STATE_ARROW);
                loadTrending();
            }
        }, 750);
    }

    private void loadTrending() {
        progressSpinner.setVisibility(View.VISIBLE);
        helper.trends(new GiphyHelper.Callback() {
            @Override
            public void onResponse(List<GiphyHelper.Gif> gifs) {
                setAdapter(gifs);
            }
        });
    }

    private void executeQuery(String query) {
        progressSpinner.setVisibility(View.VISIBLE);

        helper.search(query, new GiphyHelper.Callback() {
            @Override
            public void onResponse(List<GiphyHelper.Gif> gifs) {
                setAdapter(gifs);
            }
        });
    }

    private void setAdapter(List<GiphyHelper.Gif> gifs) {
        progressSpinner.setVisibility(View.GONE);

        if (adapter != null) {
            adapter.releaseVideo();
        }

        adapter = new GifSearchAdapter(gifs, new GifSearchAdapter.Callback() {
            @Override
            public void onClick(final GiphyHelper.Gif item) {
                new DownloadVideo(GiphyActivity.this, item.gifUrl).execute();
            }
        });

        recycler.setLayoutManager(new LinearLayoutManager(GiphyActivity.this));
        recycler.setAdapter(adapter);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (adapter != null) {
            adapter.releaseVideo();
        }
    }

    private static class DownloadVideo extends AsyncTask<Void, Void, Uri> {

        Activity activity;
        String video;
        ProgressDialog dialog;

        DownloadVideo(Activity activity, String videoLink) {
            this.activity = activity;
            this.video = videoLink;
        }

        @Override
        public void onPreExecute() {
            dialog = new ProgressDialog(activity);
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);
            dialog.setMessage(activity.getString(R.string.downloading));
            dialog.show();
        }

        @Override
        protected Uri doInBackground(Void... arg0) {
            try {
                return saveGiffy(activity, video);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Uri downloadedTo) {
            if (downloadedTo != null) {
                activity.setResult(Activity.RESULT_OK, new Intent().setData(downloadedTo));
                activity.finish();

                try {
                    dialog.dismiss();
                } catch (Exception e) { }
            } else {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    Toast.makeText(activity, R.string.error_downloading_gif,
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(activity, R.string.error_downloading_gif_permission,
                            Toast.LENGTH_SHORT).show();
                }
            }
        }

        private Uri saveGiffy(Context context, String videoUrl) throws Exception {
            final File file = new File(context.getFilesDir().getPath(),
                    "giphy_" + System.currentTimeMillis() + ".gif");
            if (!file.createNewFile()) {
                // file already exists
            }

            URL url = new URL(videoUrl);
            URLConnection connection = url.openConnection();
            connection.setReadTimeout(5000);
            connection.setConnectTimeout(30000);

            InputStream is = connection.getInputStream();
            BufferedInputStream inStream = new BufferedInputStream(is, 1024 * 5);
            FileOutputStream outStream = new FileOutputStream(file);

            byte[] buffer = new byte[1024 * 5];
            int len;
            while ((len = inStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, len);
            }

            outStream.flush();
            outStream.close();
            inStream.close();

            return Uri.fromFile(file);
        }

    }
}
