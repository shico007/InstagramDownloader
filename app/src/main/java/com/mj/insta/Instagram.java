package com.mj.insta;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;


public class Instagram {


    public static final String BASE_URL = "http://insta-code-x.appspot.com/main.php?anchor=";

    public static ArrayList<String> getUrls(String str) {
        ArrayList<String>  urls =  new ArrayList<String>();
        try
        {
            JSONArray array = new JSONArray(str);
            for (int x=0; x<array.length(); x++) {
                urls.add(array.getString(x));
            }

        } catch (JSONException je) {
            System.out.println(je.toString());
        }
        return urls;
    }





    public static void saveImage(Bitmap bitmap, File file) {
        try {
            FileOutputStream fout = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fout);
            fout.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Instagram.logger("Failed to save image : " + e.getLocalizedMessage());
        }
        catch (IOException e) {
            e.printStackTrace();
            Instagram.logger("Failed to save image : " + e.getLocalizedMessage());
        }
    }

    public static void saveVideo(String video_url, File file) {
        try {
            URL url = new URL(video_url);
            Instagram.saveUrl(url, file);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            Instagram.logger("Failed to save video : " + e.getLocalizedMessage());
        } catch (IOException e) {
            e.printStackTrace();
            Instagram.logger("Failed to save video : " + e.getLocalizedMessage());
        }
    }



    public static void makingFolder(String path) {
        File foda = new File(path);
        if(!foda.exists())
            foda.mkdirs();
    }




    public static String getAnchor(String urlstr) {
        //sample input = https://instagram.com/p/hu38njH/
        String[] chunks = urlstr.split("/");
        int urefu = chunks.length;
        return (urefu > 0) ? chunks[urefu-2] : "";

    }

    public static void saveUrl(final URL url, final File file) throws IOException {
        BufferedInputStream in = null;
        FileOutputStream fout = null;
        try {
            in = new BufferedInputStream(url.openStream());
            fout = new FileOutputStream(file);

            final byte data[] = new byte[1024];
            int count;
            while ((count = in.read(data, 0, 1024)) != -1) {
                fout.write(data, 0, count);
            }

            if (in != null)
                in.close();

            if (fout != null)
                fout.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Instagram.logger(e.getLocalizedMessage());
        } catch (IOException e) {
            e.printStackTrace();
            Instagram.logger(e.getLocalizedMessage());
        }
    }

    public static void logger(String msg) {
        Log.e("MJ", msg);
    }

    public static void toast(Context ctx, String s) {
        Toast.makeText(ctx, s, Toast.LENGTH_SHORT).show();
    }


    public static boolean isInstaLink(String str) {
        boolean c1 = str.contains("https://instagram.com/");
        boolean c2 = str.length() < 50 && str.length() > 30;
        return  c1 && c2;
    }
}
