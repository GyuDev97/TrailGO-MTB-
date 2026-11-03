package com.bike.trailgo.net;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class HttpClient {

    private static final String WWW_FORM = "application/x-www-form-urlencoded";

    private int httpStatusCode;
    private String body;

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    public String getBody() {
        return body;
    }

    private Builder builder;

    private void setBuilder(Builder builder) {
        this.builder = builder;
    }

    public void request() {
        HttpURLConnection conn = getConnection();
        httpStatusCode = -1;
        body = "";
        if (conn == null) {
            return;
        }
        try {
            setHeader(conn);
            setBody(conn);
            httpStatusCode = getStatusCode(conn);
            body = readStream(conn, httpStatusCode);
        } catch (RuntimeException error) {
            Log.w("HttpClient", "Request setup failed", error);
        } finally {
            conn.disconnect();
        }
    }

    private HttpURLConnection getConnection() {
        try {
            return Server.openUrlConnection(builder.getUrl());
        } catch (MalformedURLException e) {
            Log.w("HttpClient", "Invalid request URL", e);
        } catch (IOException e) {
            Log.w("HttpClient", "Unable to open server connection", e);
        }
        return null;
    }

    private void setHeader(HttpURLConnection connection) {
        setContentType(connection);
        setRequestMethod(connection);

        connection.setConnectTimeout(Server.CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(Server.READ_TIMEOUT_MILLIS);
        connection.setDoOutput(true);
        connection.setDoInput(true);
    }

    private void setContentType(HttpURLConnection connection) {
        connection.setRequestProperty("Content-Type", WWW_FORM);
    }

    private void setRequestMethod(HttpURLConnection connection) {
        try {
            connection.setRequestMethod(builder.getMethod());
        } catch (ProtocolException e) {
            e.printStackTrace();
        }
    }

    private void setBody(HttpURLConnection connection) {

        String parameter = builder.getParameters();
        if (parameter != null && parameter.length() > 0) {
            OutputStream outputStream = null;
            try {
                outputStream = connection.getOutputStream();
                outputStream.write(parameter.getBytes("UTF-8"));
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (outputStream != null)
                        outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private int getStatusCode(HttpURLConnection connection) {
        try {
            return connection.getResponseCode();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return -10;
    }

    private String readStream(HttpURLConnection connection, int statusCode) {
        StringBuilder result = new StringBuilder();
        BufferedReader reader = null;
        try {
            java.io.InputStream stream = statusCode >= 400
                    ? connection.getErrorStream() : connection.getInputStream();
            if (stream == null) {
                return "";
            }
            reader = new BufferedReader(new InputStreamReader(stream));
            String line = null;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
        } catch (IOException e) {
            Log.w("HttpClient", "Unable to read server response", e);
        } finally {
            try {
                if (reader != null)
                    reader.close();
            } catch (IOException e) {
            }
        }

        return result.toString();
    }

    public static class Builder {

        private Map<String, Object> parameters;
        private String method;
        private String url;

        public String getMethod() {
            return method;
        }

        public String getUrl() {
            return url;
        }
        public Builder(String method, String url) {
            if (method == null) {
                method = "GET";
            }
            this.method = method;
            this.url = Server.getUrl(url);
            this.parameters = new HashMap<String, Object>();
        }

        public void addOrReplace(String key, String value) {
            this.parameters.put(key, value);
        }

        public void addAllParameters(Map<String, String> param) {
            this.parameters.putAll(param);
        }

        public String getParameters() {
            return generateParameters();
        }

        public Object getParameter(String key) {
            return this.parameters.get(key);
        }

        private String generateParameters() {
            StringBuffer parameters = new StringBuffer();

            Iterator<String> keys = getKeys();

            String key = "";
            while (keys.hasNext()) {
                key = keys.next();
                parameters.append(String.format("%s=%s", key, this.parameters.get(key)));
                parameters.append("&");
            }

            String params = parameters.toString();
            if (params.length() > 0) {
                params = params.substring(0, params.length() - 1);
            }

            return params;
        }

        private Iterator<String> getKeys() {
            return this.parameters.keySet().iterator();
        }

        public HttpClient create() {
            HttpClient client = new HttpClient();
            client.setBuilder(this);
            return client;
        }

    }
}
