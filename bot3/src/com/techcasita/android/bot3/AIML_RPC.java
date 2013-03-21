// Copyright (c) 2012-201 Wolf Paulus - Tech Casita Productions
package com.techcasita.android.bot3;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class AIML_RPC extends AsyncTask<String, Void, String> {
    private static final String LOG_TAG = AIML_RPC.class.getSimpleName();

    private static final URI WS_URI = URI.create("http://vega.techcasita.com:8080/talk-xml");
    private static final String BOT_ID = "xght%^30YH6404uII9";
    private static final String PARAM_NAME_BOT = "botid";
    private static final String PARAM_NAME_TEXT = "input";
    private static final String PARAM_NAME_SESSION = "custid";

    private static final String START_TAG = "<that>";
    private static final String END_TAG = "</that>";
    private static final String START_ID = "custid=\"";
    private static final String END_ID = "\"";

    private static final int TIMEOUT_MS = 7500;

    private static String SessionId = null;
    private Handler mHandler;

    public AIML_RPC(Handler mHandler) {
        this.mHandler = mHandler;
    }

    @Override
    protected String doInBackground(final String... text) {
        String response;
        try {
            response = executePost(text[0]);
        } catch (Throwable ex) {
            Log.e(LOG_TAG, "error: " + ex.getMessage());
            response = ex.getLocalizedMessage();
        }
        return response;
    }

    /**
     * Parse the service response for the response text and the session id
     *
     * @param result <code>String</code> my still contain HTML or META info
     */
    @Override
    protected void onPostExecute(String result) {
        String s = null;
        if (result != null) {
            result = result.trim();

            int i = result.indexOf(START_TAG) + START_TAG.length();
            int j = result.indexOf(END_TAG, i);
            if (0 < i && i < j) {
                s = result.substring(i, j);
            }
            i = result.indexOf(START_ID) + START_ID.length();
            j = result.indexOf(END_ID, i + START_ID.length());
            if (0 < i && i < j) {
                SessionId = result.substring(i, j);
            }
            if (s != null) {  // clean up response, e.g. removing HTML tags etc.
                s = Html.fromHtml(s).toString();
                s = s.replaceAll("\\<.*?>", "");
            } else {
                s = "There might be a connection problem";
            }
        } else {
            s = "There might be a server problem";
        }

        Bundle bundle = new Bundle();
        bundle.putString(Bot3.BUNDLE_KEY_NAME_FOR_MSG, s);
        Message msg = new Message();
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /**
     * Runs in the <i>UI Thread</i> instead of #onPostExecute, in case execution gets canceled.
     */
    @Override
    protected void onCancelled() {
        super.onCancelled();
    }

    /**
     * <code>executePost</code> is the part where this <code>AsyncTask</code> does most of its work.
     * Here the HTTP request gets generated and eventually posted.
     * The name/value pairs are assembled and once the response comes in, the payload is read into a string.
     *
     * @param text <code>String</code> the text for the bot to answer
     * @return <code>String</code> http response body
     * @throws Exception
     */
    private String executePost(final String text) throws Exception {
        HttpClient httpClient;
        httpClient = new DefaultHttpClient();
        HttpConnectionParams.setConnectionTimeout(httpClient.getParams(), TIMEOUT_MS);
        HttpConnectionParams.setSoTimeout(httpClient.getParams(), TIMEOUT_MS);

        HttpPost httpPost = new HttpPost(WS_URI);

        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair(PARAM_NAME_BOT, BOT_ID));
        nameValuePairs.add(new BasicNameValuePair(PARAM_NAME_TEXT, text));
        if (SessionId != null) {
            nameValuePairs.add(new BasicNameValuePair(PARAM_NAME_SESSION, SessionId));
        }
        httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        HttpResponse response = httpClient.execute(httpPost);

        //
        // Read response body
        //
        BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()), 4096);
        String line;
        StringBuilder sb = new StringBuilder();
        while ((line = rd.readLine()) != null && !isCancelled()) {
            sb.append(line);
        }
        rd.close();
        return sb.toString();
    }
}
