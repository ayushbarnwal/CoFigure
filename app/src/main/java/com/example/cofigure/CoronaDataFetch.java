package com.example.cofigure;

import static androidx.core.content.PackageManagerCompat.LOG_TAG;

import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;


public final class CoronaDataFetch {

    public CoronaDataFetch(){}


    /**
     * Returns new URL object from the given string URL.
     */
    private static URL createUrl(String stringUrl) {
        URL url = null;
        try {
            url = new URL(stringUrl);
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "Problem building the URL ", e);
        }
        return url;
    }

    /**
     * Make an HTTP request to the given URL and return a String as the response.
     */
    private static String makeHttpRequest(URL url) throws IOException {
        String jsonResponse = "";

        // If the URL is null, then return early.
        if (url == null) {
            return jsonResponse;
        }

        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(10000 /* milliseconds */);
            urlConnection.setConnectTimeout(15000 /* milliseconds */);
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // If the request was successful (response code 200),
            // then read the input stream and parse the response.
            if (urlConnection.getResponseCode() == 200) {
                inputStream = urlConnection.getInputStream();
                jsonResponse = readFromStream(inputStream);
            } else {
                Log.e(LOG_TAG, "Error response code: " + urlConnection.getResponseCode());
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Problem retrieving the earthquake JSON results.", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (inputStream != null) {
                // Closing the input stream could throw an IOException, which is why
                // the makeHttpRequest(URL url) method signature specifies than an IOException
                // could be thrown.
                inputStream.close();
            }
        }
        return jsonResponse;
    }

    /**
     * Convert the {@link InputStream} into a String which contains the
     * whole JSON response from the server.
     */
    private static String readFromStream(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        if (inputStream != null) {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line = reader.readLine();
            while (line != null) {
                output.append(line);
                line = reader.readLine();
            }
        }
        return output.toString();
    }


    public static ArrayList<coronaDetails> fetchCovidData(String requestUrl){

        // Create URL object
        URL url = createUrl(requestUrl);

        // Perform HTTP request to the URL and receive a JSON response back
        String jsonResponse = null;
        try {
            jsonResponse = makeHttpRequest(url);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Problem making the HTTP request.", e);
        }

        // Extract relevant fields from the JSON response and create a list of {@link Earthquake}s
        ArrayList<coronaDetails> coronaDetailsList = extractFeatureFromJson(jsonResponse);

        // Return the list of {@link Earthquake}s
        return coronaDetailsList;
    }



    /**
     * Return a list of {@link coronaDetails} objects that has been built up from
     * parsing the given JSON response.
     */
    private static ArrayList<coronaDetails> extractFeatureFromJson(String covidDetailsJSON) {
        // If the JSON string is empty or null, then return early.
        if (TextUtils.isEmpty(covidDetailsJSON)) {
            return null;
        }

        // Create an empty ArrayList that we can start adding earthquakes to
        ArrayList<coronaDetails> coronaDetailsList = new ArrayList<>();

        // Try to parse the JSON response string. If there's a problem with the way the JSON
        // is formatted, a JSONException exception object will be thrown.
        // Catch the exception so the app doesn't crash, and print the error message to the logs.
        try {

            JSONObject baseJsonResponse = new JSONObject(covidDetailsJSON);

            String lastupdated = baseJsonResponse.getString("lastRefreshed");

            JSONObject dataObject = baseJsonResponse.getJSONObject("data");

            JSONArray coronaArray = dataObject.getJSONArray("regional");

            for (int i = 0; i < coronaArray.length(); i++) {

                JSONObject currentCovidDetail = coronaArray.getJSONObject(i);

                String stateName =currentCovidDetail.getString("loc");

                int confirmedCases = currentCovidDetail.getInt("totalConfirmed");

                int dischargedCases = currentCovidDetail.getInt("discharged");

                int death = currentCovidDetail.getInt("deaths");

                coronaDetails coronadetail = new coronaDetails(stateName,lastupdated,confirmedCases,dischargedCases,death);


                coronaDetailsList.add(coronadetail);
            }

        } catch (JSONException e) {
            // If an error is thrown when executing any of the above statements in the "try" block,
            // catch the exception here, so the app doesn't crash. Print a log message
            // with the message from the exception.
            Log.e("QueryUtils", "Problem parsing the CoronaCases JSON results", e);
        }

        // Return the list of earthquakes
        return coronaDetailsList;
    }

}
