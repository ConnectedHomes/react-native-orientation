package com.github.yamill.orientation;

import android.app.Activity;
import android.view.OrientationEventListener;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.util.Log;
import android.provider.Settings;
import android.database.ContentObserver;
import android.content.ContentResolver;
import android.os.Handler;
import android.net.Uri;

import com.facebook.common.logging.FLog;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.ReactConstants;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

public class OrientationModule extends ReactContextBaseJavaModule implements LifecycleEventListener {
    private final OrientationEventListener receiver;
    private boolean isOrientationEnabled;
    private String orientationValue;

    public OrientationModule(ReactApplicationContext reactContext) {
        super(reactContext);


        receiver = new OrientationEventListener(reactContext) {
            int orientationInt = getReactApplicationContext().getResources().getConfiguration().orientation;
            String currentSpecificOrientation = OrientationModule.this.getOrientationString(orientationInt);

            @Override
            public void onOrientationChanged(int orientation) {

                String orientationValue = "PORTRAIT";
                String specificOrientationValue = "PORTRAIT";
                if (orientation >= 60 && orientation < 120) {
                    orientationValue = "LANDSCAPE";
                    specificOrientationValue = "LANDSCAPE-LEFT";
                } else if (orientation >= 150 && orientation < 210) {
                    orientationValue = "PORTRAIT";
                    specificOrientationValue = "PORTRAITUPSIDEDOWN";
                } else if (orientation >= 240 && orientation < 300) {
                    orientationValue = "LANDSCAPE";
                    specificOrientationValue = "LANDSCAPE-RIGHT";
                }
                if (currentSpecificOrientation.equals(specificOrientationValue)) {
                    return;
                }
                currentSpecificOrientation = specificOrientationValue;

                WritableMap params = Arguments.createMap();
                params.putString("orientation", orientationValue);
                params.putString("specificOrientation", specificOrientationValue);
                sendNotification("orientationDidChange", params);
            }
        };
        receiver.enable();
    }

    private void sendNotification(String eventName, WritableMap params) {
        final ReactApplicationContext ctx = getReactApplicationContext();
        if (ctx.hasActiveCatalystInstance()) {
            ctx
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(eventName, params);
        }
    }

    @ReactMethod
    public void start(Promise promise) {
        final ContentResolver contentResolver = getReactApplicationContext().getApplicationContext().getContentResolver();
        isOrientationEnabled = getOrientationLockEnabled(contentResolver);

        try {
            Uri setting = Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION);
            ContentObserver observer = new ContentObserver(new Handler()) {
                @Override
                public void onChange(boolean selfChange) {
                    super.onChange(selfChange);
                    isOrientationEnabled = getOrientationLockEnabled(contentResolver);
                    WritableMap params = Arguments.createMap();
                    params.putBoolean("isOrientationEnabled", isOrientationEnabled);

                    sendNotification("orientationLockSettingsDidChanged", params);
                }

                @Override
                public boolean deliverSelfNotifications() {
                    return true;
                }
            };
            contentResolver.registerContentObserver(setting, false, observer);
            promise.resolve("Initialized!");
        } catch (Exception e) {
            promise.reject(e);
        }

    }

    private Boolean getOrientationLockEnabled(ContentResolver contentResolver) {
        try {
            return Settings.System.getInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION) == 1;
        } catch (Settings.SettingNotFoundException e) {
            return false;
        }
    }


    @Override
    public String getName() {
        return "Orientation";
    }

    @ReactMethod
    public void isOrientationLockedInSettings(Callback callback) {
        callback.invoke(null, !isOrientationEnabled);
    }

    @ReactMethod
    public void getOrientation(Callback callback) {
        final int orientationInt = getReactApplicationContext().getResources().getConfiguration().orientation;

        String orientation = this.getOrientationString(orientationInt);

        if (orientation == "null") {
            callback.invoke(orientationInt, null);
        } else {
            WritableMap params = Arguments.createMap();
            params.putString("orientation", orientation);
            callback.invoke(null, params);
        }
    }

    @ReactMethod
    public void lockToPortrait() {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return;
        }
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @ReactMethod
    public void lockToLandscape() {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return;
        }
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
    }

    @ReactMethod
    public void lockToLandscapeRight() {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return;
        }
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    @ReactMethod
    public void lockToLandscapeLeft() {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return;
        }
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
    }

    @ReactMethod
    public void unlockAllOrientations() {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return;
        }
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
    }

    @Override
    public @Nullable
    Map<String, Object> getConstants() {
        HashMap<String, Object> constants = new HashMap<String, Object>();
        int orientationInt = getReactApplicationContext().getResources().getConfiguration().orientation;

        String orientation = this.getOrientationString(orientationInt);
        if (orientation == "null") {
            constants.put("initialOrientation", null);
        } else {
            constants.put("initialOrientation", orientation);
        }

        return constants;
    }

    private String getOrientationString(int orientation) {
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return "LANDSCAPE";
        } else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            return "PORTRAIT";
        } else if (orientation == Configuration.ORIENTATION_UNDEFINED) {
            return "UNKNOWN";
        } else {
            return "null";
        }
    }

    @Override
    public void onHostResume() {
        receiver.enable();
    }

    @Override
    public void onHostPause() {
        receiver.disable();
    }

    @Override
    public void onHostDestroy() {
        receiver.disable();
    }
}
