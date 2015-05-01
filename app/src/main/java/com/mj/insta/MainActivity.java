package com.mj.insta;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;


public class MainActivity extends ActionBarActivity implements View.OnClickListener {

    private static final int MEDIA_TYPE_VIDEO = 0xFF08;
    private static final int MEDIA_TYPE_IMAGE = 0xFF02;
    private static final String FOLDER_PATH = Environment.getExternalStorageDirectory().
            getAbsolutePath()+"/InstaDownloads/";
    //views
    private TextView search_tv_btn, percentTv;
    private ImageView imgView;

    //variables
    private String current_video_url, current_anchor;
    private Bitmap current_image_bitmap;
    private File current_d_file;
    private int media_type;
    private ProgressBar pb;
    private RequestQueue requestQueue;
    private long t2;
    private boolean readyToDownload = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Instagram.makingFolder(FOLDER_PATH);


        search_tv_btn = (TextView) findViewById(R.id.button);
        percentTv = (TextView) findViewById(R.id.percent_tv);
        imgView = (ImageView) findViewById(R.id.imageView);
        pb = (ProgressBar) findViewById(R.id.progressBar);

        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.action_bar)));

        search_tv_btn.setOnClickListener(this);


        String instaLink = getInstaLinkFromClipBoard();
        if(instaLink.isEmpty())
            search_tv_btn.setText("No text from clipboard,\nOpen Instagram app and copy share url.");
        else
            search_tv_btn.setText(instaLink+"\nTAP TO LOAD URL");

        requestQueue = Volley.newRequestQueue(this);
        ImageLoader imageLoader = new ImageLoader(requestQueue, new BitmapLruCache());
    }

    @Override
    protected void onResume() {
        super.onResume();
        String instaLink = getInstaLinkFromClipBoard();

        if(instaLink.isEmpty())
            search_tv_btn.setText("No text from clipboard,\nOpen Instagram app and copy share url.");
        else
            search_tv_btn.setText(instaLink +"\nLOAD URL");
    }

    private void loadInstagram(String link) {
        long t = System.currentTimeMillis();
        current_anchor = Instagram.getAnchor(link);
        NetworkListenerVolleyForJson responseListener = new NetworkListenerVolleyForJson();
        JsonArrayRequest jar = new JsonArrayRequest(Instagram.BASE_URL+current_anchor, responseListener, responseListener);
        requestQueue.add(jar);
        Instagram.toast(getApplicationContext(), "Loading media");

    }

    private String getInstaLinkFromClipBoard() {
        //should return link or empty string
        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        String r = "";
        if(cm.hasPrimaryClip()) {
            ClipData data = cm.getPrimaryClip();
            ClipData.Item item = data.getItemAt(0);
            r = item.getText().toString();

            if (!Instagram.isInstaLink(r))
                r = "";
        }

        return r;
    }


    private void registerFileToMediaDb(File file) {
        MediaScannerConnection.scanFile(this,
                new String[] { file.getAbsolutePath()}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Instagram.logger("Media scanner completed");
                    }
                });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button:
                if(!readyToDownload)
                {
                    //if everything is ready for download...
                    String link = search_tv_btn.getText().toString();
                    if (!link.isEmpty() && Instagram.isInstaLink(link))
                        loadInstagram(link);
                    else
                        Instagram.toast(getApplicationContext(), "You should provide the shareable link");

                }
                else
                    saveMedia();

                break;

            default:
                break;
        }
    }

    private class NetworkListenerVolleyForBitmap implements Response.ErrorListener, Response.Listener<Bitmap> {

        @Override
        public void onErrorResponse(VolleyError error) {
            Instagram.logger("Failed to load bitmap : "+ error.getLocalizedMessage());
        }

        @Override
        public void onResponse(Bitmap response) {
            imgView.setImageBitmap(response);
            current_image_bitmap = response;
            Instagram.toast(getApplicationContext(), "Loaded in : "+(System.currentTimeMillis() - t2));
            //the picture determines what to be written on the button
            //after it has loaded, there's always a delay be careful...
            if (media_type == MEDIA_TYPE_IMAGE) {
                search_tv_btn.setText("SAVE PICTURE");
            }
            if (media_type == MEDIA_TYPE_VIDEO) {
                search_tv_btn.setText("SAVE VIDEO");
            }
            readyToDownload = true;

        }
    }

    private class NetworkListenerVolleyForJson implements Response.ErrorListener, Response.Listener<JSONArray> {

        @Override
        public void onErrorResponse(VolleyError error) {
            Instagram.toast(getApplicationContext(), "A network error occured\n"+error.getMessage());
        }

        @Override
        public void onResponse(JSONArray response) {
            parseTheJSONArray(response);
            Instagram.logger("Got the json array");
        }
    }

    private void parseTheJSONArray(JSONArray array) {
        switch (array.length()) {
            case 2:
                loadImage(array);
                loadVideo(array);
                media_type = MEDIA_TYPE_VIDEO;
                break;

            case 1:
                loadImage(array);
                media_type = MEDIA_TYPE_IMAGE;
                break;

            case 0:
                Instagram.toast(getApplicationContext(), "Got 0 length array..");
                break;

            default: break;
        }
    }

    private void loadVideo(JSONArray array) {
        try {
            String video_url = array.getString(1);
            current_video_url = video_url;

        } catch (JSONException e) {
            Instagram.logger("Failed to get url from json array");
            e.printStackTrace();
        }
    }

    private void loadImage(JSONArray array) {
        try {
            t2 = System.currentTimeMillis();
            String img_url = array.getString(0);
            NetworkListenerVolleyForBitmap nvl = new NetworkListenerVolleyForBitmap();
            ImageRequest ir = new ImageRequest(img_url, nvl, 0,0, null, nvl);
            requestQueue.add(ir);

        } catch (JSONException e) {
            Instagram.logger("Failed to get url from json array");
            e.printStackTrace();
        }
    }

    private void saveMedia() {
        switch (this.media_type) {
            case MEDIA_TYPE_IMAGE :
                current_d_file = new File(FOLDER_PATH+"img_"+current_anchor+".jpg");
                Instagram.toast(getApplicationContext(),
                        "The picture has been saved to :\n"+current_d_file.getAbsolutePath());
                Instagram.saveImage(current_image_bitmap, current_d_file);
                registerFileToMediaDb(current_d_file);
                readyToDownload = false;
                break;

            case MEDIA_TYPE_VIDEO:
                current_d_file = new File(FOLDER_PATH+"vid_"+current_anchor+".mp4");
                Instagram.toast(getApplicationContext(),"Started to download video");
                new LazyDownloader().execute(current_video_url);
                readyToDownload = false;
                break;

            default:
                Instagram.toast(getApplicationContext(), "CANNOT SAVE UNKNOWN MEDIA TYPE");
                break;
        }
    }


    class LazyDownloader extends AsyncTask<String, Long, Long> {

        private long t1, t2, t_getSize;
        private int file_length;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            t1 = System.currentTimeMillis();
            pb.setVisibility(View.VISIBLE);
            percentTv.setVisibility(View.VISIBLE);
            percentTv.setText("0%");
            pb.setMax(100);
            pb.setProgress(0);
            Instagram.logger("Downloading video...");

        }

        @Override
        protected void onProgressUpdate(Long... values) {
            super.onProgressUpdate(values);
            int progress = (int)(values[0]/(float)file_length)*100;
            Instagram.logger("Progress : " + progress + " was : "+(values[0]/(float)file_length)*1024);
            pb.setProgress(progress);
            percentTv.setText(String.valueOf(progress) + "%");
        }

        @Override
        protected Long doInBackground(String... params) {

            try {
                URL url =  new URL(current_video_url);
                URLConnection con = url.openConnection();

                t_getSize = System.currentTimeMillis();
                file_length = con.getContentLength();
                file_length = (file_length > 0) ? file_length : 2048;
                t_getSize = System.currentTimeMillis() - t_getSize;
                Instagram.logger("Got file size in :"+t_getSize+"ms" + "size : "+file_length);


                BufferedInputStream in = new BufferedInputStream(con.getInputStream());
                FileOutputStream  fout = new FileOutputStream(current_d_file);

                final byte data[] = new byte[1024];
                int count; long progress = 0;
                while ((count = in.read(data, 0, 1024)) != -1) {
                    fout.write(data, 0, count);
                    publishProgress(progress+=1024);
                }

                in.close();
                fout.close();

            } catch (IOException e) {
                Instagram.logger("Failed to download video from"+params[0]+"\n"+e.getLocalizedMessage());
                e.printStackTrace();
            }


            return  0L;

        }

        @Override
        protected void onPostExecute(Long integer) {
            registerFileToMediaDb(current_d_file);
            Instagram.logger("Done downloading the video");

        }


    }


}
