
package de.patwoz.rn.bluetoothstatemanager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.modules.core.DeviceEventManagerModule;
/*
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;*/

import java.util.*;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;

/*import java.util.WritableArray;*/


public class RNBluetoothStateManagerModule extends ReactContextBaseJavaModule {

  private final ReactApplicationContext reactContext;

  private final static int REQUEST_ENABLE_BT = 795;
  private Promise requestToEnablePromise;

  private final Intent INTENT_OPEN_BLUETOOTH_SETTINGS = new Intent(
          android.provider.Settings.ACTION_BLUETOOTH_SETTINGS
  );
  private final Intent INTENT_REQUEST_ENABLE_BLUETOOTH = new Intent(
          BluetoothAdapter.ACTION_REQUEST_ENABLE
  );

  private final static String EVENT_BLUETOOTH_STATE_CHANGE = "EVENT_BLUETOOTH_STATE_CHANGE";

  public RNBluetoothStateManagerModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;

    if (BluetoothUtils.isBluetoothSupported()) {
      this.startListenForBluetoothStateChange();
    }
  }

  private void destroy() {
    if (BluetoothUtils.isBluetoothSupported()) {
      this.stopListenForBluetoothStateChange();
    }
    this.removeRequestToEnableListener();
  }

  @Override
  public String getName() {
    return "RNBluetoothStateManager";
  }

  @Override
  public Map<String, Object> getConstants() {
    final Map<String, Object> constants = new HashMap<>();
    constants.put("EVENT_BLUETOOTH_STATE_CHANGE", EVENT_BLUETOOTH_STATE_CHANGE);
    return constants;
  }

  @Override
  public void onCatalystInstanceDestroy() {
    super.onCatalystInstanceDestroy();
    this.destroy();
  }

  // --------------------------------------------------------------------------------------------- -
  // BLUETOOTH STATE

  @ReactMethod
  public void getState(Promise promise) {
    if (this.handleIfBluetoothNotSupported(promise, false)) {
      promise.resolve(Constants.BluetoothState.UNSUPPORTED);
      return;
    }

    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    promise.resolve(BridgeUtils.fromBluetoothState(bluetoothAdapter.getState()));
  }

  // --------------------------------------------------------------------------------------------- -
  // PROGRAMMATICALLY CHANGE BLUETOOTH STATE (not recommended)

  @SuppressLint("MissingPermission")
  private void setBluetooth(boolean enable) {
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    if (enable) {
      bluetoothAdapter.enable();
    } else {
      bluetoothAdapter.disable();
    }
  }

  @ReactMethod
  public void enable(Promise promise) {
    if (this.handleIfBluetoothNotSupported(promise)) { return; }
    Activity currentActivity = this.handleCurrentActivity(promise);
    if (currentActivity == null) { return; }

    if (BluetoothUtils.hasBluetoothAdminPermission(currentActivity)) {
      this.setBluetooth(true);
      promise.resolve(null);
    } else {
      promise.reject("UNAUTHORIZED", "You are not authorized to do this.");
    }
  }

  @ReactMethod
  public void disable(Promise promise) {
    if (this.handleIfBluetoothNotSupported(promise)) { return; }
    Activity currentActivity = this.handleCurrentActivity(promise);
    if (currentActivity == null) { return; }

    if (BluetoothUtils.hasBluetoothAdminPermission(currentActivity)) {
      this.setBluetooth(false);
      promise.resolve(null);
    } else {
      promise.reject("UNAUTHORIZED", "You are not authorized to do this.");
    }
  }

  // --------------------------------------------------------------------------------------------- -
  // OPEN SETTINGS

  @ReactMethod
  public void openSettings(Promise promise) {
    if (this.handleIfBluetoothNotSupported(promise)) { return; }
    Activity currentActivity = this.handleCurrentActivity(promise);
    if (currentActivity == null) { return; }

    currentActivity.startActivity(INTENT_OPEN_BLUETOOTH_SETTINGS);
    promise.resolve(null);
  }
  
  @ReactMethod
  public void supportedFeatures(Promise promise) {
    try {    
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
        /*
        WritableMap map = new WritableNativeMap();
        map.putBoolean("teszt",false);
        promise.resolve(map);
        */
        List<Boolean> returnSet = new ArrayList<Boolean>();
        if (Integer.valueOf(android.os.Build.VERSION.SDK) >= 21) {
            returnSet.add(bluetoothAdapter.isMultipleAdvertisementSupported());
            returnSet.add(bluetoothAdapter.isOffloadedFilteringSupported());
            returnSet.add(bluetoothAdapter.isOffloadedScanBatchingSupported());
        }
        else {
            returnSet.add(false);
            returnSet.add(false);
            returnSet.add(false);
        }        
        if (Integer.valueOf(android.os.Build.VERSION.SDK) >= 26) {
            returnSet.add(bluetoothAdapter.isLeCodedPhySupported());
            returnSet.add(bluetoothAdapter.isLeExtendedAdvertisingSupported());
            returnSet.add(bluetoothAdapter.isLePeriodicAdvertisingSupported());
        }
        else {
            returnSet.add(false);
            returnSet.add(false);
            returnSet.add(false);
        }
        
        Boolean[] returnArray = new Boolean[returnSet.size()];
        returnArray = returnSet.toArray(returnArray);
        
        WritableArray promiseArray=Arguments.createArray();
        for(int i=0;i<returnArray.length;i++){
            promiseArray.pushBoolean(returnArray[i]);
        }
        
        promise.resolve(promiseArray);
    }
    catch(Exception e){
        promise.reject(e);
    }    
    /*promise.resolve(bluetoothAdapter.isLe2MPhySupported());*/
    /*
    api lvl 26:    
    isLeCodedPhySupported() 
    isLeExtendedAdvertisingSupported()
    isLePeriodicAdvertisingSupported() 
    api lvl 21:            
    isMultipleAdvertisementSupported() 
    isOffloadedFilteringSupported() 
    isOffloadedScanBatchingSupported() 
    */
  }
  

  // --------------------------------------------------------------------------------------------- -
  // BLUETOOTH STATE CHANGE

  private void startListenForBluetoothStateChange() {
    IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
    reactContext.registerReceiver(mReceiver, filter);
  }

  private void stopListenForBluetoothStateChange() {
    reactContext.unregisterReceiver(mReceiver);
  }

  private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      final String action = intent.getAction();

      if (action != null && action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
        final int state = intent.getIntExtra(
                BluetoothAdapter.EXTRA_STATE,
                BluetoothAdapter.ERROR
        );
        sendEvent(EVENT_BLUETOOTH_STATE_CHANGE, BridgeUtils.fromBluetoothState(state));
      }
    }
  };

  // --------------------------------------------------------------------------------------------- -
  // REQUEST TO ENABLE BLUETOOTH

  private void addRequestToEnableListener(Promise promise) {
    this.requestToEnablePromise = promise;
    this.reactContext.addActivityEventListener(this.requestToEnableListener);
  }

  private void removeRequestToEnableListener() {
    this.reactContext.removeActivityEventListener(this.requestToEnableListener);
    this.requestToEnablePromise = null;
  }

  @ReactMethod
  public void requestToEnable(Promise promise) {
    Activity currentActivity = this.handleCurrentActivity(promise);
    if (currentActivity == null) { return; }

    this.addRequestToEnableListener(promise);
    currentActivity.startActivityForResult(INTENT_REQUEST_ENABLE_BLUETOOTH, REQUEST_ENABLE_BT);
  }

  private final ActivityEventListener requestToEnableListener = new BaseActivityEventListener() {
    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
      if (requestCode != REQUEST_ENABLE_BT) { return; }

      if (requestToEnablePromise == null) {
        Log.w(
                "RNBluetoothStateManager",
                "onActivityResult() :: Result code:" + resultCode + " ::'requestToEnablePromise' should be defined!"
        );
      } else {
        if (resultCode == Activity.RESULT_CANCELED) {
          requestToEnablePromise.reject("CANCELED", "The user canceled the action.");
        } else if (resultCode == Activity.RESULT_OK) {
          requestToEnablePromise.resolve(null);
        } else {
          Log.w(
                  "RNBluetoothStateManager",
                  "onActivityResult() :: Result code:" + resultCode + " :: Unhandled result code"
          );
        }
      }

      removeRequestToEnableListener();
    }
  };

  // --------------------------------------------------------------------------------------------- -
  // HELPERS

  private void sendEvent(String eventName, @Nullable Object params) {
    this.getReactApplicationContext()
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(eventName, params);
  }

  private boolean handleIfBluetoothNotSupported(Promise promise, boolean reject)  {
    if (!BluetoothUtils.isBluetoothSupported()) {
      if (reject) {
        promise.reject("BLUETOOTH_NOT_SUPPORTED", "This device doesn't support Bluetooth");
      }
      return true;
    }
    return false;
  }

  private boolean handleIfBluetoothNotSupported(Promise promise)  {
    return this.handleIfBluetoothNotSupported(promise, true);
  }

  private Activity handleCurrentActivity(Promise promise) {
    Activity currentActivity = getCurrentActivity();
    if (currentActivity == null) {
      promise.reject("INTERNAL_ERROR", "There is no activity");
    }
    return currentActivity;
  }

}