// Copyright (c) 2012 Wolf Paulus - Tech Casita Productions
package com.techcasita.android.bot3;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.net.URI;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Fetch Stock Quotes using Yahoo Finance API
 * Format Param:
 * n = Name
 * l1 = Last Trade (Price Only)
 * p = Previous Close
 * c = Change & Percent Change
 *
 * @author <a href="mailto:wolf@wolfpaulus.com">Wolf Paulus</a>
 */
public class YQuote extends AsyncTask<String, Void, String> {
    private static final URI SERVICE_WS_URI = URI.create("http://download.finance.yahoo.com/d/quotes.csv?s=");
    private static final String FORMAT_PARM = "&f=nl1pc";
    private static final int TIMEOUT_MS = 7500;
    private final Handler mHandler;

    private final DecimalFormat df2 = new DecimalFormat( "#,###,###,##0.00" );

    public YQuote(Handler mHandler) {
        this.mHandler = mHandler;
    }

    @Override
    protected String doInBackground(final String... symbols) {
        String response;
        if (symbols == null || symbols.length < 1 || symbols[0] == null) {
            response = "For what ticker symbol?";
        } else {
            String s = symbols[0].replace(" ", "");
            try {
                response = executePost(s);
            } catch (Throwable ex) {
                response = ex.getLocalizedMessage();
            }
        }
        return response;
    }

    /**
     * Parse the service response for the response information.
     *
     * @param csv like this: "Google Inc.",624.60,651.01,"-26.41 - -4.06%"
     */
    @Override
    protected void onPostExecute(String csv) {
        Map<String, String> map = new HashMap<String, String>(3);
        map.put("name", null);
        map.put("last", null);
        map.put("previousclose", null);
        map.put("change", null);
        map.put("percentagechange", null);

        final int k = csv.indexOf("\"", 1);  // 2nd Quote character in String
        final String[] sa = csv.substring(k + 2).split(",");
        map.put("name", csv.substring(0, k).replace("\"", ""));

        try {
            if (1 <= sa.length) map.put("last", "$ " + df2.format(Double.valueOf(sa[0].replace("\"", ""))));
            if (2 <= sa.length) map.put("previousclose", "$ " + df2.format(Double.valueOf(sa[1].replace("\"", ""))));
        } catch (NumberFormatException e) {
            // intentionally empty
        }

        if (3 <= sa.length) {
            final String[] saa = sa[2].split(" - ");
            if (1 <= saa.length) map.put("change", saa[0].replace("\"", ""));
            if (2 <= saa.length) map.put("percentagechange", saa[1].replace("\"", ""));
        }
        final String s;
        if (map.containsKey(Exception.class.getSimpleName())) {
            s = "Maybe a Server Problem ?";
        } else if ("$ 0.00".equals(map.get("last"))) {
            s = "I don't think you gave me a valid TICKER symbol.";
        } else {
            s = map.get("name") + " was last traded at "
                    + map.get("last") + " while the previous close was at "
                    + map.get("previousclose") + ". That constitutes a change of "
                    + map.get("change") + " or "
                    + map.get("percentagechange");
        }
        Bundle data = new Bundle();
        data.putString(Bot3.BUNDLE_KEY_NAME_FOR_MSG, s);
        Message msg = new Message();
        msg.setData(data);
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
     * @param symbol <code>String</code> the text for the bot to answer
     * @return <code>String</code> http response body
     * @throws Exception
     */
    private String executePost(final String symbol) throws Exception {
        HttpClient httpClient = new DefaultHttpClient();
        HttpConnectionParams.setConnectionTimeout(httpClient.getParams(), TIMEOUT_MS);
        HttpConnectionParams.setSoTimeout(httpClient.getParams(), TIMEOUT_MS);

        HttpGet httpGet = new HttpGet(SERVICE_WS_URI + symbol + FORMAT_PARM);
        HttpResponse httpResponse = httpClient.execute(httpGet);
        HttpEntity httpEntity = httpResponse.getEntity();
        return EntityUtils.toString(httpEntity, HTTP.UTF_8); // urldecoded csv
    }
}




