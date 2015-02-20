package be.shouldit.proxy.lib.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

public class HttpAnswer
{
    private HttpURLConnection httpURLConnection;
    private int status;
    private String body;
    private String contentType;

    public int getStatus()
    {
        return status;
    }

    public String getBody()
    {
        return body;
    }

    public String getContentType()
    {
        return contentType;
    }

    public HttpAnswer(HttpURLConnection connection) throws IOException
    {
        httpURLConnection = connection;
        status = httpURLConnection.getResponseCode();
        if (status == HttpURLConnection.HTTP_OK)
        {
            contentType = httpURLConnection.getContentType();

            // Response successful
            InputStream inputStream = httpURLConnection.getInputStream();

            // Parse it line by line
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String temp;
            StringBuilder sb = new StringBuilder();
            while ((temp = bufferedReader.readLine()) != null)
            {
                // LogWrapper.d(TAG, temp);
                sb.append(temp);
            }

            body = sb.toString();
        }
    }


}