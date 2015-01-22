/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements. See the NOTICE file
distributed with this work for additional information
regarding copyright ownership. The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License. You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied. See the License for the
specific language governing permissions and limitations
under the License.
*/
package com.physicaroid.pocketduino.cordova;

import java.util.HashMap;
import java.util.Iterator;
import java.io.IOException;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.CordovaInterface;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.physicaloid.lib.Boards;
import com.physicaloid.lib.Physicaloid;
import com.physicaloid.lib.Physicaloid.UploadCallBack;
import com.physicaloid.lib.usb.driver.uart.ReadLisener;

public class PocketDuino extends CordovaPlugin {

    private static final String POCKETDUINO = "PocketDuino Plugin";
    private static final String HANDLER_PREFIX = "pocketduino";

    private UsbManager mUsbManager;
    BroadcastReceiver mUsbReceiver;
    CallbackContext usbCallbackContext;
    CallbackContext dataCallbackContext;
    Physicaloid mPhysicaloid;


    /**
     * Constructor.
     */
    public PocketDuino() {
        this.mUsbManager = null;
        this.mUsbReceiver = null;
        this.usbCallbackContext = null;
        this.dataCallbackContext = null;
        this.mPhysicaloid = null;
    }

    /**
     * Sets the context of the Command. This can then be used to do things like
     * get file paths associated with the Activity.
     *
     * @param cordova The context of the main Activity.
     * @param webView The CordovaWebView Cordova is running in.
     */
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        // initialize Physicaloid
        this.mPhysicaloid = new Physicaloid(cordova.getActivity().getApplicationContext());
    }

    /**
    * Executes the request and returns PluginResult.
    *
    * @param action The action to execute.
    * @param args JSONArry of arguments for the plugin.
    * @param callbackId The callback id used when calling back into JavaScript.
    * @return A PluginResult object with a status and message.
    */
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("setInfo")) {
            try {
                showToast(callbackContext, cordova.getActivity(), args);
                return true;    // exec()はtrueを返さないと successCallbackの後にerrorCallbackが走ってしまう
            } catch(Exception e) {
                Log.e(POCKETDUINO, "Exception: " + e.getMessage());
                callbackContext.error(e.getMessage());
            }
        } else if (action.equals("setCallback")) {
            setCallbackTest(callbackContext);
            return true;    // exec()はtrueを返さないと successCallbackの後にerrorCallbackが走ってしまう
        } else if (action.equals("listenDevice")) {
            listenUsbDevice(callbackContext);
            return true;    // exec()はtrueを返さないと successCallbackの後にerrorCallbackが走ってしまう
        } else if (action.equals("loadHexFile")) {
            uploadHexFile(callbackContext, args);
            return true;    // exec()はtrueを返さないと successCallbackの後にerrorCallbackが走ってしまう
        } else if (action.equals("startReceive")) {
            listenData(callbackContext);
            return true;    // exec()はtrueを返さないと successCallbackの後にerrorCallbackが走ってしまう
        } else if (action.equals("openDevice")) {
            openDevice(callbackContext);
            return true;    // exec()はtrueを返さないと successCallbackの後にerrorCallbackが走ってしまう
        } else if (action.equals("closeDevice")) {
            closeDevice(callbackContext);
            return true;    // exec()はtrueを返さないと successCallbackの後にerrorCallbackが走ってしまう
        } else {
            PluginResult dataResult = new PluginResult(PluginResult.Status.INVALID_ACTION, "Invalid Action");
            callbackContext.sendPluginResult(dataResult);
        }
        return false;
    }

    private void showToast(CallbackContext callbackContext, Activity activity, JSONArray args) {
        try {
            android.widget.Toast.makeText(activity, args.getString(0), 5000).show();
            callbackContext.success();
        } catch (Exception e) {
            Log.e(POCKETDUINO, "Exception: " + e.getMessage());
            callbackContext.error(e.toString());
        }
    }

    private void setCallbackTest (CallbackContext callbackContext) {
        try {
            String json = "{\"message\": \"conntected\"}";
            JSONObject parameter = new JSONObject(json);
            PluginResult dataResult = new PluginResult(PluginResult.Status.OK, parameter);
            dataResult.setKeepCallback(true);
            callbackContext.sendPluginResult(dataResult);
        } catch (JSONException e) {
            Log.e(POCKETDUINO, "Exception: " + e.getMessage());
            callbackContext.error(e.toString());
        }
    }

    /**
     * USBデバイスのアタッチ
     */
    private void listenUsbDevice (CallbackContext callbackContext) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        this.usbCallbackContext = callbackContext;
        if (this.mUsbReceiver == null) {
            this.mUsbReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String handlerName = null;
                    String action = intent.getAction();
                    if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                        // plugin result of USB attached
                        handlerName = HANDLER_PREFIX + "." + "attached";
                    }
                    if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                        // plugin result of USB detached
                        handlerName = HANDLER_PREFIX + "." + "detached";
                    }
                    try {
                        String json = "{\"handlerName\":" + handlerName + " }";
                        JSONObject parameter = new JSONObject(json);
                        PluginResult dataResult = new PluginResult(PluginResult.Status.OK, parameter);
                        dataResult.setKeepCallback(true);
                        usbCallbackContext.sendPluginResult(dataResult);
                    } catch (JSONException e) {
                        Log.e(POCKETDUINO, "Exception: " + e.getMessage());
                        usbCallbackContext.error(e.toString());
                    }
                }
            };
            webView.getContext().registerReceiver(this.mUsbReceiver, intentFilter);
        }
    }

    /**
     * PocketDuinoデバイスをオープンする
     */
    private void openDevice(CallbackContext callbackContext) {
        try {
            if(this.mPhysicaloid.open()) {
                // もしかしたら事前にuploadが必要なのかもしれない
                PluginResult dataResult = new PluginResult(PluginResult.Status.OK);
                dataResult.setKeepCallback(true);
                callbackContext.sendPluginResult(dataResult);
            } else {
                PluginResult dataResult = new PluginResult(PluginResult.Status.ERROR);
                dataResult.setKeepCallback(true);
                callbackContext.sendPluginResult(dataResult);
            }
        } catch (RuntimeException e) {
            PluginResult dataResult = new PluginResult(PluginResult.Status.ERROR);
            dataResult.setKeepCallback(true);
            callbackContext.sendPluginResult(dataResult);
        }
    }

    /**
     * PocketDuinoデバイスをクローズする
     */
    private void closeDevice(CallbackContext callbackContext) {
        try {
            if(this.mPhysicaloid.close()) {
                PluginResult dataResult = new PluginResult(PluginResult.Status.OK);
                dataResult.setKeepCallback(true);
                callbackContext.sendPluginResult(dataResult);
            } else {
                PluginResult dataResult = new PluginResult(PluginResult.Status.ERROR);
                dataResult.setKeepCallback(true);
                callbackContext.sendPluginResult(dataResult);
            }
        } catch (RuntimeException e) {
            PluginResult dataResult = new PluginResult(PluginResult.Status.ERROR);
            dataResult.setKeepCallback(true);
            callbackContext.sendPluginResult(dataResult);
        }
    }

    /**
     * hexファイルをアップロードする
     */
    private void uploadHexFile(CallbackContext callbackContext, JSONArray args) {
        try {
            String fileName = args.getString(0);
            Log.d(POCKETDUINO, "!!!--- " + fileName + " ---!!!");
            // upload()の第3引数は callback アップロードエラーが返るので捕捉する必要がある。
            // このへんの書き方がよくわかってない
            mPhysicaloid.upload(Boards.POCKETDUINO, cordova.getActivity().getResources().getAssets().open(fileName), null);
            PluginResult dataResult = new PluginResult(PluginResult.Status.OK);
            dataResult.setKeepCallback(true);
            callbackContext.sendPluginResult(dataResult);
        } catch (RuntimeException e) {
            try {
                String json = "{\"message\":" + e.toString() + " }";
                JSONObject parameter = new JSONObject(json);
                PluginResult dataResult = new PluginResult(PluginResult.Status.ERROR, parameter);
                dataResult.setKeepCallback(true);
                callbackContext.sendPluginResult(dataResult);
            } catch (JSONException exc) {
                Log.e(POCKETDUINO, exc.toString());
            }
        } catch (IOException e) {
            try {
                String json = "{\"message\":" + e.toString() + " }";
                JSONObject parameter = new JSONObject(json);
                PluginResult dataResult = new PluginResult(PluginResult.Status.ERROR, parameter);
                dataResult.setKeepCallback(true);
                callbackContext.sendPluginResult(dataResult);
            } catch (JSONException exc) {
                Log.e(POCKETDUINO, exc.toString());
            }
        } catch (JSONException e) {
            try {
                String json = "{\"message\":" + e.toString() + " }";
                JSONObject parameter = new JSONObject(json);
                PluginResult dataResult = new PluginResult(PluginResult.Status.ERROR, parameter);
                dataResult.setKeepCallback(true);
                callbackContext.sendPluginResult(dataResult);
            } catch (JSONException exc) {
                Log.e(POCKETDUINO, exc.toString());
            }
        }
    }

    private void listenData (CallbackContext callbackContext) {
        this.dataCallbackContext = callbackContext;
        if(mPhysicaloid.isOpened()) {
            mPhysicaloid.addReadListener(new ReadLisener() {
                // callback when reading one or more size buffer
                @Override
                public void onRead(int size) {
                    byte[] buf = new byte[size];
                    int readSize = mPhysicaloid.read(buf, size);
                    //Log.d(POCKETDUINO, String.format("%02d ", size));
                    if(readSize > 2) {
                        try {
                            String hexString = bytesToHex(buf);
                            String handlerName = "pocketduino.receive";
                            String json = "{\"handlerName\":" + handlerName +  ", \"data\":" +  hexString  + " }";
                            //String json = "{\"handlerName\":" + handlerName + " }";
                            JSONObject parameter = new JSONObject(json);
                            PluginResult dataResult = new PluginResult(PluginResult.Status.OK, parameter);
                            dataResult.setKeepCallback(true);
                            dataCallbackContext.sendPluginResult(dataResult);
                        } catch (JSONException e) {
                            Log.e(POCKETDUINO, "Exception: " + e.getMessage());
                            dataCallbackContext.error(e.toString());
                        }
                    }
                }
            });
        } else {
            PluginResult dataResult = new PluginResult(PluginResult.Status.ERROR);
            dataResult.setKeepCallback(true);
            this.dataCallbackContext.sendPluginResult(dataResult);
            this.dataCallbackContext = null;
        }
    }

    // USB Manager
    // ちょっとデバイス名を取得してみるテスト - つかってない
    private void getDeviceName (CallbackContext callbackContext) {
        Activity activity = cordova.getActivity();
        UsbManager mUsbManager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> map = mUsbManager.getDeviceList();
        Iterator<UsbDevice> it = map.values().iterator();
        while(it.hasNext()) {
            UsbDevice device = it.next();
            // use device info
            Log.v(POCKETDUINO, "!!!--- USB Device Name ---!!!");
            Log.v(POCKETDUINO, device.getDeviceName());
            Log.v(POCKETDUINO, "!!!--- USB Device Product ID ---!!!");
            Log.v(POCKETDUINO, Integer.toString(device.getProductId()));
            Log.v(POCKETDUINO, "!!!--- USB Device Vendor ID ---!!!");
            Log.v(POCKETDUINO, Integer.toString(device.getVendorId()));
            Log.v(POCKETDUINO, "!!!--- USB Device Hash Code ---!!!");
            Log.v(POCKETDUINO, Integer.toString(device.hashCode()));
        }
    }

    // Bufferを16進文字列に変換する
    protected final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
      char[] hexChars = new char[bytes.length * 2];
      for ( int j = 0; j < bytes.length; j++ ) {
        int v = bytes[j] & 0xFF;
        hexChars[j * 2] = hexArray[v >>> 4];
        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
      }
      return new String(hexChars);
    }

    public void onDestroy() {
        removeReceiver();
    }

    public void onReset() {
        removeReceiver();
    }

    private void removeReceiver() {
        if(this.mUsbReceiver != null) {
            try {
                webView.getContext().unregisterReceiver(this.mUsbReceiver);
                this.mUsbReceiver = null;
            } catch(Exception e) {
                Log.e(POCKETDUINO, e.getMessage());
            }
        }
    }

}

