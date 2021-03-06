package com.cordova.plugin.locationtimer;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class LocationTimerSv extends Service implements LocationListener {

    private String trackedUUID;
    private String postURL;
    private int timerString;

    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        trackedUUID = preferences.getString("LocationTimer_trackedUUID", "");
        postURL = preferences.getString("LocationTimer_postURL", "");
        timerString = preferences.getInt("LocationTimer_timerString", 10);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, timerString * 1000, 0, this);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, timerString * 1000, 0, this);


            new Timer().scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        postDataToServer(locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER));
                    }
                }
            },0, timerString * 1000);

        }

    }

    @Override
    public void onLocationChanged(Location location) {
        postDataToServer(location);
    }

    private void postDataToServer(Location location){
        Thread thread = new Thread(() -> {
            JSONObject jo = new JSONObject();

            try {
                //UUID address
                jo.put("uuid", trackedUUID);

                //lat
                jo.put("latitude", location.getLatitude());

                //lon
                jo.put("longitude", location.getLongitude());

                //timestamp
                jo.put("timeStamp", DateFormat.format("dd-MM-yyyy HH:mm:ss", new Date()));

                URL url = new URL(postURL);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    urlConnection.setDoOutput(true);
                    urlConnection.setChunkedStreamingMode(0);
                    urlConnection.setRequestProperty("Content-Type", "application/json; utf-8");

                    //OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
                    try (OutputStream os = urlConnection.getOutputStream()) {
                        byte[] input = jo.toString(2).getBytes("utf-8");
                        os.write(input, 0, input.length);
                    }

                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(urlConnection.getInputStream(), "utf-8"))) {
                        StringBuilder response = new StringBuilder();
                        String responseLine = null;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                        System.out.println(response.toString());
                    }

                } finally {
                    urlConnection.disconnect();
                }

            } catch (Exception e) {
                Log.d("post error", e.getMessage());
            }
        });
        thread.start();
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
