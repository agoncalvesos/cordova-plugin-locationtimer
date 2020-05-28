package com.cordova.plugin.locationtimer;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class echoes a string called from JavaScript.
 */
public class LocationTimer extends CordovaPlugin {

    private String trackedUUID;
    private String postURL;
    private int timerString;
    private static CallbackContext callbackContext;
    private static final int PERMISSIONS_REQUEST_FINE_LOCATION = 1;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;

        if (action.equals("init")) {
            //Checking if parameters are valid

            //UUID
            trackedUUID =  args.getString(0);
            if (trackedUUID == null || trackedUUID.isEmpty()){
                sendError("trackedUUID input parameter cannot be empty");
            }

            //URL
            postURL =  args.getString(1);
            if (postURL == null || postURL.isEmpty()){
                sendError("postURL input parameter cannot be empty");
            }

            //Timer
            timerString =  args.getInt(2);
            if (timerString == 0){
                sendError("timerString input parameter should be > 0");
            }

            this.startService();
            return true;
        }
        return false;
    }

    private void startService() {

        if (!hasLocationPermissions()) {
            requestNeededPermissions();
        } else {
            //Set preferences to be used by the WristbandsSerice
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(cordova.getContext());
            ;
            SharedPreferences.Editor editor = preferences.edit();

            editor.putString("LocationTimer_trackedUUID", trackedUUID); // value to store
            editor.putString("LocationTimer_postURL", postURL); // value to store
            editor.putInt("LocationTimer_timerString", timerString); // value to store
            editor.commit();

            Intent intent = new Intent(cordova.getContext(), LocationTimerSv.class);
            cordova.getActivity().startService(intent);
        }
    }

    private boolean hasLocationPermissions(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check 
            return cordova.getActivity().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }

        return true;
    }

    private void requestNeededPermissions(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check 
            if (!hasLocationPermissions()) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(cordova.getContext());
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener((DialogInterface dialog) -> {
                    cordova.requestPermissions(this, PERMISSIONS_REQUEST_FINE_LOCATION, new String[]{Manifest.permission.ACCESS_FINE_LOCATION});
                });
                builder.show();
            }
        }
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        super.onRequestPermissionResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSIONS_REQUEST_FINE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("WristbandsPlugin", "fine location permission granted");
                    startService();
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(cordova.getContext());
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener((DialogInterface dialog) -> {

                    });
                    builder.show();
                }
                return;
            }
        }
    }

    private void sendError(String message) {
        callbackContext.error(message);

    }

    private void sendSuccess(String message) {
        PluginResult result = new PluginResult(PluginResult.Status.OK, message);
        result.setKeepCallback(true);
        callbackContext.sendPluginResult(result);
    }
}
