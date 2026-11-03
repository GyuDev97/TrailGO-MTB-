package com.bike.trailgo.gpx;

import android.os.AsyncTask;

import com.bike.trailgo.net.Server;

import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

public class FetchAndParseGPXTask extends AsyncTask<Void, Void, Gpx> {

    final String mGpxUrl;
    final GpxFetchedAndParsed mListener;
    final GPXParser mParser = new GPXParser();

    public FetchAndParseGPXTask(String gpxUrl, GpxFetchedAndParsed listener) {
        mGpxUrl = gpxUrl;
        mListener = listener;
    }

    @Override
    protected Gpx doInBackground(Void... unused) {
        Gpx parsedGpx = null;
        HttpURLConnection client = null;
        try {
            client = Server.openUrlConnection(mGpxUrl);
            InputStream in = new BufferedInputStream(client.getInputStream());
            parsedGpx = mParser.parse(in);
        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
        } finally {
            if (client != null) {
                client.disconnect();
            }
        }
        return parsedGpx;
    }

    @Override
    protected void onPostExecute(Gpx gpx) {
        mListener.onGpxFetchedAndParsed(gpx);
    }
}
