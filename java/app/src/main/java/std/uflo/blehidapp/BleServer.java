package std.uflo.blehidapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

class BleServer {
    private static final String TAG = "BleServer";
    private static final int DEFAULT_MTU_MAX = 64;
    private static final String DEFAULT_MANUFACTURER_NAME = "UFLO Studio";
    private static final String DEFAULT_LOCAL_NAME = "UFLO Keyboard";

    private static final UUID BATTERY_STATUS_SERVICE_UUID = UUID.fromString("0000180F-0000-1000-8000-00805F9B34FB");
    private static final UUID BATTERY_STATUS_POWER_LEVEL_UUID = UUID.fromString("00002A19-0000-1000-8000-00805F9B34FB");

    private static final UUID DEVICE_INFORMATION_SERVICE_UUID = UUID.fromString("0000180A-0000-1000-8000-00805F9B34FB");
    private static final UUID DEVICE_INFORMATION_MANUFACTURER_NAME_UUID = UUID.fromString("00002A29-0000-1000-8000-00805F9B34FB");
    private static final UUID DEVICE_INFORMATION_MODEL_NUMBER_UUID = UUID.fromString("00002A24-0000-1000-8000-00805F9B34FB");
    private static final UUID DEVICE_INFORMATION_FIRMWARE_REVISION_UUID = UUID.fromString("00002A26-0000-1000-8000-00805F9B34FB");

    private static final UUID HID_SERVICE_UUID = UUID.fromString("00001812-0000-1000-8000-00805F9B34FB");
    private static final UUID HID_INFORMATION_UUID = UUID.fromString("00002A4A-0000-1000-8000-00805F9B34FB");
    private static final UUID HID_REPORT_MAP_UUID = UUID.fromString("00002A4B-0000-1000-8000-00805F9B34FB");
    private static final UUID HID_CONTROL_POINT_UUID = UUID.fromString("00002A4C-0000-1000-8000-00805F9B34FB");
    private static final UUID HID_REPORT_UUID = UUID.fromString("00002A4D-0000-1000-8000-00805F9B34FB");
    private static final UUID CLIENT_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final UUID BLE_REPORT_REFERENCE_DESCRIPTOR_UUID = UUID.fromString("00002908-0000-1000-8000-00805f9b34fb");

    private static final UUID GAP_APPERENCE = UUID.fromString("00002A01-0000-1000-8000-00805f9b34fb");
/*
*
#define HUMAN_INTERFACE_DEVICE_SERVICE_UUID 0x1812
/* I don't plane to do boot protocol keyboard *//*
//#define HUMAN_INTERFACE_DEVICE_SERVICE_BOOT_KEYBOARD_INPUT_UUID 0x2A22 // Read, Write(o), Notify
//#define HUMAN_INTERFACE_DEVICE_SERVICE_BOOT_KEYBOARD_OUTPUT_UUID 0x2A32 // Read, Write, WriteWithoutResponse
#define HUMAN_INTERFACE_DEVICE_SERVICE_HID_INFORMATION_UUID 0x2A4A // Read
#define HUMAN_INTERFACE_DEVICE_SERVICE_REPORT_MAP_UUID 0x2A4B // Read
#define HUMAN_INTERFACE_DEVICE_SERVICE_HID_CONTROL_POINT_UUID 0x2A4C // WriteWithoutResponse
/*
input report: Read, Write(o), Notify
output report: Read, Write, WriteWithoutResponse
feature report: Read, Write
*//*
#define HUMAN_INTERFACE_DEVICE_SERVICE_REPORT_UUID 0x2A4D
/* I don't plane to do boot protocol keyboard *//*
//#define HUMAN_INTERFACE_DEVICE_PROTOCOL_MODE_UUID 0x2A4E

//#define CLIENT_CHARACTERISTIC_CONFIGURATION 0x2902
#define BLE_REPORT_REFERENCE_DESCRIPTOR_UUID 0x2908
* */

    private static final byte[] KEYBOARD_HID_DESCRIPTOR = new byte[] {
        0x05, 0x01,         // Usage Page (Generic Desktop)
        0x09, 0x06,         // Usage (Keyboard)
        (byte) 0xA1, 0x01,         // Collection (Application)
        0x05, 0x08,         //   Usage Page(LEDs)
        0x19, 0x01,         //   Usage Minimum (1)
        0x29, 0x03,         //   Usage Maxmum (3)
        0x15, 0x00,         //   Logical Minimum (0)
        0x25, 0x01,         //   Logical Maximum (1)
        0x75, 0x01,         //   Report Size (1)
        (byte) 0x95, 0x03,         //   Report Count (3)
        (byte) 0x91, 0x02,         //   Output(Absolute, Variable, Data) 1 bit x3
        (byte) 0x95, 0x05,         //   Report Count (5)
        (byte) 0x91, 0x01,         //   Output(Absolute, Array, Constant) 1 bit x5
        0x05, 0x07,         //   Usage Page(Keyboard)
        0x19, (byte) 0xE0,         //   Usage Minimum(224)
        0x29, (byte) 0xE7,         //   Usage Maxmum(231)
        (byte) 0x95, 0x08,         //   Report Count (8)
        (byte) 0x81, 0x02,         //   Input(Absolute, Variable, Data) 1 bit x8
        0x75, 0x08,         //   Report Size (8)
        (byte) 0x95, 0x01,         //   Report Count (1)
        (byte) 0x81, 0x01,         //   Input(Absolute, Array, Constant) 1 Byte x1
        0x19, 0x00,         //   Usage Minimum(0)
        0x29, (byte) 0xDD,         //   Usage Maxmum(221)
        0x26, (byte) 0xFF, 0x00,   //   Logical Maximum (255)
        (byte) 0x95, 0x06,         //   Report Count(6)
        (byte) 0x81, 0x00,         //   Input(Absolute, Array, Data) 1 Byte x6
        (byte) 0xC0,               // End Collection
    };

    private static final byte[] INPUT_REPORT_REFERENCE_DESCRIPTOR = new byte[] {0x00, 0x01};
    private static final byte[] OUTPUT_REPORT_REFERENCE_DESCRIPTOR = new byte[] {0x00, 0x02};
    private static final byte[] CLEAN_KEY_REPORT = new byte[] {0, 0, 0, 0, 0, 0, 0, 0};

    private Context mContext;
    private BluetoothGattServer mBatteryServer;
    private BluetoothGattServer mDeviceInfoServer;
    private BluetoothGattServer mHidServer;
    private BluetoothDevice mCurrentCentral;
    private BluetoothAdapter mBtAdapter;
    private BluetoothLeAdvertiser mBLEAdvertiser;
    private BluetoothGattCharacteristic mInputChar;
    private BluetoothGattCharacteristic mOutputChar;
    private BleStatusCallbackListener mCallbackListener;
    private HidKeyboard mKeyboard;
//    private TextView mBleStatusView;

    private BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            if (newState == BluetoothGattServer.STATE_CONNECTED) {
                Log.d(TAG, String.format("Server %s(%s) connected", device.getName(), device.getAddress()));
                mCurrentMTU = DEFAULT_MTU_MAX;
                mCurrentCentral = device;
//                boolean tres = device.createBond();
//                if (tres)
//                    Log.d(TAG, String.format("test binding success"));
//                else
//                    Log.d(TAG, String.format("test binding fail"));
                if (mCallbackListener != null)
                    mCallbackListener.onBleStatusCallback(true);
            }
            else if (newState == BluetoothGattServer.STATE_DISCONNECTED) {
                Log.d(TAG, "Server " + device.getName() + " disconnected");
                mCurrentCentral = null;
                if (mCallbackListener != null)
                    mCallbackListener.onBleStatusCallback(false);
            }
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            UUID uuid = service.getUuid();
            if (uuid.equals(HID_SERVICE_UUID)) {
                Log.d(TAG, (status == BluetoothGatt.GATT_SUCCESS) ? "Added HID service." : "Failed to add HID service.");
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, String.format("onCharacteristicReadRequest(): name: %s, address: %s, requestId: %d, offset: %d", device.getName(), device.getAddress(), requestId, offset));
            UUID characteristicUUID = characteristic.getUuid();
            if (characteristicUUID.equals(DEVICE_INFORMATION_MANUFACTURER_NAME_UUID)) {
                byte[] value = DEFAULT_MANUFACTURER_NAME.getBytes(StandardCharsets.UTF_8);
                mDeviceInfoServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            }
            else if (characteristicUUID.equals(DEVICE_INFORMATION_MODEL_NUMBER_UUID)) {
                byte[] value = Build.MODEL.getBytes(StandardCharsets.UTF_8);
                mDeviceInfoServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            }
            else if (characteristicUUID.equals(DEVICE_INFORMATION_FIRMWARE_REVISION_UUID)) {
                byte[] value = mAppVersionName.getBytes(StandardCharsets.UTF_8);
                mDeviceInfoServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            }
            else if (characteristicUUID.equals(BATTERY_STATUS_POWER_LEVEL_UUID)) {
                byte[] value = {(byte) 90};
                mBatteryServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            }

            else if (characteristicUUID.equals(HID_INFORMATION_UUID)) {
                Log.d(TAG, "read hid information");
                byte[] value = new byte[] {
                    0x11, 0x01,     // HID spec v1.11
                    0x00,           // country code 00
                    0x03            // flags: normally_connectable: 0:off 1:on, remote_wake- 0:off 1:on
                };
                mHidServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            }
            else if (characteristicUUID.equals(HID_REPORT_MAP_UUID)) {
                Log.d(TAG, "read hid report map");
                mHidServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, KEYBOARD_HID_DESCRIPTOR);
            }
            else if (characteristicUUID.equals(HID_REPORT_UUID)) {
                Log.d(TAG, "read hid report");
                // how to tell is input report or output report
                if (characteristic.equals(mInputChar)) {
                    Log.d(TAG, "read from input report");
                    mHidServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, CLEAN_KEY_REPORT);
                }
                else if (characteristic.equals(mOutputChar)) {
                    Log.d(TAG, "read from out report");
                    byte[] value = new byte[] {mKeyboard.getKeyboardLock()};
                    mHidServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
                }
                else {
                    Log.d(TAG, "read some else report");
                }
            }

            else {
                Log.e(TAG, "Received read request from unknown characteristic.");
            }
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            Log.d(TAG, String.format("onCharacteristicWriteRequest(): name: %s, address: %s, requestId: %d, preparedWrite: %s, responseNeeded: %s, offset: %d",
                  device.getName(), device.getAddress(), requestId, preparedWrite ? "TRUE" : "FALSE", responseNeeded ? "TRUE" : "FALSE", offset));
            int status = BluetoothGatt.GATT_SUCCESS;
            UUID characteristicUUID = characteristic.getUuid();
            if (characteristicUUID.equals(HID_CONTROL_POINT_UUID)){
                Log.d(TAG, "have write Request to control point");
            }
            else if (characteristicUUID.equals(HID_REPORT_UUID)) {
                Log.d(TAG, "Received write request to keyboard lock status");
                byte lock = value[0];
                mKeyboard.setKeyboardLock(lock);
            }
            else {
                Log.e(TAG, String.format("Received write request from unknown characteristic. (%s)", characteristicUUID.toString()));
                status = BluetoothGatt.GATT_FAILURE;
            }
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            // TODO
            Log.d(TAG, "Server onExecuteWrite()");
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            Log.d(TAG, String.format("onNotificationSent(): device %s(%s) status(%d)", device.getName(), device.getAddress(), status));
//            try {
//                mNotificationLock.unlock();
//            } catch (Exception e) {
//                Log.e(TAG, e.toString());
//            }
//            sendStatusOutgoingData();
            // TODO send
        }

        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            mCurrentMTU = mtu;
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId,
                                            int offset, BluetoothGattDescriptor descriptor) {
            Log.d(TAG, String.format("onDescriptorReadRequest() device(%s), requestId(%d), offset(%d)"
                    , device.getName(), requestId, offset));
            UUID uuid = descriptor.getUuid();
            if (uuid.equals(CLIENT_CONFIG)) {
                Log.d(TAG, "Config descriptor read");
                byte[] returnValue;
                if (mCurrentCentral != null && mCurrentCentral.equals(device)) {
                    Log.d(TAG, "notify register success");
                    returnValue = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
                } else {
                    Log.d(TAG, "notify register fail");
                    returnValue = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
                }
                mHidServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        returnValue);
            }
            else if (uuid.equals(BLE_REPORT_REFERENCE_DESCRIPTOR_UUID)) {
                Log.d(TAG, "read report reference descriptor, this should not happen");
            }
            else {
                Log.w(TAG, "Unknown descriptor read request");
                mHidServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        null);
            }
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                             BluetoothGattDescriptor descriptor,
                                             boolean preparedWrite, boolean responseNeeded,
                                             int offset,  byte[] value) {
            int status = BluetoothGatt.GATT_SUCCESS;
            UUID uuid = descriptor.getUuid();
            if (uuid.equals(CLIENT_CONFIG)) {
                if (Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, value)) {
                    Log.d(TAG, "Subscribe device to notifications: " + device);
                    mCurrentCentral = device;
                } else if (Arrays.equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, value)) {
                    Log.d(TAG, "Unsubscribe device from notifications: " + device);
                    mCurrentCentral = null;
                }
            }
            else {
                Log.d(TAG, "Unknown descriptor write request.");
                status = BluetoothGatt.GATT_FAILURE;
            }
//            if (responseNeeded) mU2FServer.sendResponse(device, requestId, status, 0, null);
        }
    };

    private BluetoothGattServerCallback mBasicServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            UUID uuid = service.getUuid();
            if (uuid.equals(BATTERY_STATUS_SERVICE_UUID)) {
                Log.d(TAG, (status == BluetoothGatt.GATT_SUCCESS) ? "Added Battery service." : "Failed to add Battery service.");
            }
            else if (uuid.equals(DEVICE_INFORMATION_SERVICE_UUID)) {
                Log.d(TAG, (status == BluetoothGatt.GATT_SUCCESS) ? "Added Device Information service." : "Failed to add Device Information service.");
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, String.format("onCharacteristicReadRequest(): name: %s, address: %s, requestId: %d, offset: %d", device.getName(), device.getAddress(), requestId, offset));
            UUID characteristicUUID = characteristic.getUuid();
            if (characteristicUUID.equals(DEVICE_INFORMATION_MANUFACTURER_NAME_UUID)) {
                byte[] value = DEFAULT_MANUFACTURER_NAME.getBytes(StandardCharsets.UTF_8);
                mDeviceInfoServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            }
            else if (characteristicUUID.equals(DEVICE_INFORMATION_MODEL_NUMBER_UUID)) {
                byte[] value = Build.MODEL.getBytes(StandardCharsets.UTF_8);
                mDeviceInfoServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            }
            else if (characteristicUUID.equals(DEVICE_INFORMATION_FIRMWARE_REVISION_UUID)) {
                byte[] value = mAppVersionName.getBytes(StandardCharsets.UTF_8);
                mDeviceInfoServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            }
            else if (characteristicUUID.equals(BATTERY_STATUS_POWER_LEVEL_UUID)) {
                byte[] value = {(byte) 90};
                mBatteryServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            }
        }
    };

    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.d(TAG, "Advertising started");
        }

        @Override
        public void onStartFailure(int errorCode) {
            String err = "Unknown error";
            switch(errorCode) {
                case AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED:
                    err = "Already start advertising";
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE:
                    err = "data over 31 bytes";
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                    err = "not support ble advertising";
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR:
                    err = "internal error";
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                    err = "too many advertiser";
                    break;
            }
            Log.d(TAG, "Advertising failed: " + err);
            // TODO: start advertising again?
        }
    };

    private boolean mServerInited;
    private boolean mAdvertisingStart;
    private int mCurrentMTU;
    private String mAppVersionName;


//    public BleServer(Context context, TextView view) {
    public BleServer(Context context, BleStatusCallbackListener listener) {
        mServerInited = false;
        mAdvertisingStart = false;
        mContext = context;
//        mBleStatusView = view;
        mCallbackListener = listener;

        BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBtAdapter = manager.getAdapter();

        String name = mBtAdapter.getName();
        if (name == null || name.length() == 0) name = DEFAULT_LOCAL_NAME;
        if (!name.equals(mBtAdapter.getName())) mBtAdapter.setName(name);

        mHidServer = null;
        mDeviceInfoServer = null;
        mDeviceInfoServer = null;

        mKeyboard = new HidKeyboard();
    }

    public void startServers() {
        if (mServerInited) return;

        BluetoothManager manager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mHidServer = manager.openGattServer(mContext, mGattServerCallback);

        BluetoothGattService hidService = new BluetoothGattService(HID_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        BluetoothGattCharacteristic hidInfoChar = new BluetoothGattCharacteristic(
            HID_INFORMATION_UUID, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ);
        BluetoothGattCharacteristic reportMapChar = new BluetoothGattCharacteristic(
            HID_REPORT_MAP_UUID, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ);
        BluetoothGattCharacteristic controlPointChar = new BluetoothGattCharacteristic(
            HID_CONTROL_POINT_UUID, BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE, BluetoothGattCharacteristic.PERMISSION_WRITE);
        mInputChar = new BluetoothGattCharacteristic(
            HID_REPORT_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ);
        BluetoothGattDescriptor inputReportReference = new BluetoothGattDescriptor(
            BLE_REPORT_REFERENCE_DESCRIPTOR_UUID, BluetoothGattDescriptor.PERMISSION_READ);
        inputReportReference.setValue(INPUT_REPORT_REFERENCE_DESCRIPTOR);
        mInputChar.addDescriptor(inputReportReference);
        mOutputChar = new BluetoothGattCharacteristic(
            HID_REPORT_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        BluetoothGattDescriptor outputReportReference = new BluetoothGattDescriptor(
                BLE_REPORT_REFERENCE_DESCRIPTOR_UUID, BluetoothGattDescriptor.PERMISSION_READ);
        outputReportReference.setValue(OUTPUT_REPORT_REFERENCE_DESCRIPTOR);
        mOutputChar.addDescriptor(outputReportReference);

        hidService.addCharacteristic(hidInfoChar);
        hidService.addCharacteristic(reportMapChar);
        hidService.addCharacteristic(controlPointChar);
        hidService.addCharacteristic(mInputChar);
        hidService.addCharacteristic(mOutputChar);
        mHidServer.addService(hidService);

        mBatteryServer = manager.openGattServer(mContext, mBasicServerCallback);
        BluetoothGattService batteryService = new BluetoothGattService(
            BATTERY_STATUS_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        BluetoothGattCharacteristic batteryLevelChar = new BluetoothGattCharacteristic(
            BATTERY_STATUS_POWER_LEVEL_UUID, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ);
        byte[] batteryLevelValue = new byte[] {0x5A};
        batteryLevelChar.setValue(batteryLevelValue);
        batteryService.addCharacteristic(batteryLevelChar);
        // [dice36D] for test
        mBatteryServer.addService(batteryService);
        // end test

        mDeviceInfoServer = manager.openGattServer(mContext, mBasicServerCallback);
        BluetoothGattService deviceInfoService = new BluetoothGattService(
            DEVICE_INFORMATION_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        BluetoothGattCharacteristic manufacturerName = new BluetoothGattCharacteristic(DEVICE_INFORMATION_MANUFACTURER_NAME_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ);
        BluetoothGattCharacteristic modelNumber = new BluetoothGattCharacteristic(DEVICE_INFORMATION_MODEL_NUMBER_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ);
        BluetoothGattCharacteristic fwRevision = new BluetoothGattCharacteristic(DEVICE_INFORMATION_FIRMWARE_REVISION_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ);
        deviceInfoService.addCharacteristic(manufacturerName);
        deviceInfoService.addCharacteristic(modelNumber);
        deviceInfoService.addCharacteristic(fwRevision);
        // [dice36D] for test
        mDeviceInfoServer.addService(deviceInfoService);
        // end test

        mServerInited = true;
    }

    public void stopServers() {
        if (!mServerInited) return;

        mHidServer.close();
        mHidServer = null;
//        mDeviceInfoServer.close();
//        mDeviceInfoServer = null;
//        mBatteryServer.close();
//        mBatteryServer = null;
        mServerInited = false;
    }

    public void startAdvertising() {
        if (mAdvertisingStart) return;

        mBLEAdvertiser = mBtAdapter.getBluetoothLeAdvertiser();
        if (mBLEAdvertiser == null) {
            Log.d(TAG, "start advertise fail");
            return;
        }

        AdvertiseSettings advSetting = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(true)
                .setTimeout(0).build();
        AdvertiseData advData = new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(true)
                .addServiceUuid(new ParcelUuid(HID_SERVICE_UUID))
                .addServiceData(new ParcelUuid(GAP_APPERENCE), new byte[] {(byte) 0xC1, 0x03})
//                .addServiceUuid(new ParcelUuid())
                .build();
        AdvertiseData respData = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .build();
        mBLEAdvertiser.startAdvertising(advSetting, advData, respData, mAdvertiseCallback);

        mAdvertisingStart = true;
    }

    public void stopAdvertising() {
        if (!mAdvertisingStart) return;

        mBLEAdvertiser.stopAdvertising(mAdvertiseCallback);
        mAdvertisingStart = true;
    }

    public void sendMessage(String msg) {
        if (mCurrentCentral == null) {
            Log.d(TAG, "no connected and register device");
            return;
        }

        msg = mKeyboard.swapCase(msg);
        char[] msgArray = msg.toCharArray();
        for (int cnt_i = 0; cnt_i < msg.length(); cnt_i++) {
            byte[] keycode = mKeyboard.getKeyCode(msgArray[cnt_i]);

            mInputChar.setValue(keycode);
            mHidServer.notifyCharacteristicChanged(mCurrentCentral, mInputChar, false);
            try {
                Thread.sleep(20);
            } catch (Exception e) {
                Log.w(TAG, "sleep failed for keycode");
            }

            mInputChar.setValue(CLEAN_KEY_REPORT);
            mHidServer.notifyCharacteristicChanged(mCurrentCentral, mInputChar, false);
            try {
                Thread.sleep(20);
            } catch (Exception e) {
                Log.w(TAG, "sleep failed for release key");
            }
        }
    }
}
