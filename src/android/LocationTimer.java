package com.cordova.plugin.locationtimer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

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

        //Set preferences to be used by the WristbandsSerice
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(cordova.getContext());;
        SharedPreferences.Editor editor = preferences.edit();

        editor.putString("trackedUUID", trackedUUID); // value to store
        editor.putString("postURL", postURL); // value to store
        editor.putInt("timerString", timerString); // value to store
        editor.commit();

        Intent intent = new Intent(cordova.getContext(), LocationTimerSv.class);
        cordova.getActivity().startService(intent);
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
