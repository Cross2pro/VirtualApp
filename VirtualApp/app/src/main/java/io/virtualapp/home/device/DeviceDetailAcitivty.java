package io.virtualapp.home.device;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VDeviceManager;
import com.lody.virtual.remote.VDeviceInfo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import io.virtualapp.R;
import io.virtualapp.abs.ui.VActivity;
import io.virtualapp.home.models.DeviceData;

public class DeviceDetailAcitivty extends VActivity {

    private static final String TAG = "DeviceData";

    public static void open(Fragment fragment, DeviceData data, int position) {
        Intent intent = new Intent(fragment.getContext(), DeviceDetailAcitivty.class);
        intent.putExtra("title", data.name);
        intent.putExtra("pkg", data.packageName);
        intent.putExtra("user", data.userId);
        intent.putExtra("pos", position);
        fragment.startActivityForResult(intent, 1001);
    }

    private String mPackageName;
    private String mTitle;
    private int mUserId;
    private int mPosition;
    private VDeviceInfo mDeviceInfo;
    private TelephonyManager mTelephonyManager;
    private WifiManager mWifiManager;
    private EditText edt_androidId, edt_imei, edt_imsi, edt_mac;
    private EditText edt_brand, edt_model, edt_name, edt_device, edt_board, edt_display, edt_id, edt_serial, edt_manufacturer, edt_fingerprint;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        setContentView(R.layout.activity_mock_device);
        Toolbar toolbar = bind(R.id.top_toolbar);
        setSupportActionBar(toolbar);
        enableBackHome();
        edt_androidId = (EditText) findViewById(R.id.edt_androidId);
        edt_imei = (EditText) findViewById(R.id.edt_imei);
        edt_imsi = (EditText) findViewById(R.id.edt_imsi);
        edt_mac = (EditText) findViewById(R.id.edt_mac);

        edt_brand = (EditText) findViewById(R.id.edt_brand);
        edt_model = (EditText) findViewById(R.id.edt_model);
        edt_name = (EditText) findViewById(R.id.edt_name);
        edt_device = (EditText) findViewById(R.id.edt_device);
        edt_board = (EditText) findViewById(R.id.edt_board);
        edt_display = (EditText) findViewById(R.id.edt_display);
        edt_id = (EditText) findViewById(R.id.edt_id);
        edt_serial = (EditText) findViewById(R.id.edt_serial);
        edt_manufacturer = (EditText) findViewById(R.id.edt_manufacturer);
        edt_fingerprint = (EditText) findViewById(R.id.edt_fingerprint);
        mWifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        mTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if (TextUtils.isEmpty(mTitle)) {
            mPackageName = getIntent().getStringExtra("pkg");
            mUserId = getIntent().getIntExtra("user", 0);
            mTitle = getIntent().getStringExtra("title");
        }
        setTitle(mTitle);
        mDeviceInfo = VDeviceManager.get().getDeviceInfo(mUserId);
        updateInfos();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mPackageName = intent.getStringExtra("pkg");
        mUserId = intent.getIntExtra("user", 0);
        mTitle = intent.getStringExtra("title");
        mPosition = intent.getIntExtra("pos", -1);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_device, menu);
        return true;
    }

    private void killApp() {
        if (TextUtils.isEmpty(mPackageName)) {
            VirtualCore.get().killAllApps();
        } else {
            VirtualCore.get().killApp(mPackageName, mUserId);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save: {
                fillInfos();
                updateInfos();
                VDeviceManager.get().updateDeviceInfo(mUserId, mDeviceInfo);
                Intent intent = new Intent();
                intent.putExtra("pkg", mPackageName);
                intent.putExtra("user", mUserId);
                intent.putExtra("pos", mPosition);
                intent.putExtra("result", "save");
                setResult(RESULT_OK, intent);
                if (TextUtils.isEmpty(mPackageName)) {
                    VirtualCore.get().killAllApps();
                } else {
                    VirtualCore.get().killApp(mPackageName, mUserId);
                }
                killApp();
                Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
            }
            break;
            case R.id.action_reset:
                new AlertDialog.Builder(this)
                        .setMessage(R.string.dlg_reset_device)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            mDeviceInfo.empty();
                            VDeviceManager.get().updateDeviceInfo(mUserId, mDeviceInfo);

                            Intent intent = new Intent();
                            intent.putExtra("pkg", mPackageName);
                            intent.putExtra("user", mUserId);
                            intent.putExtra("pos", mPosition);
                            intent.putExtra("result", "reset");
                            setResult(RESULT_OK, intent);
                            killApp();
                            updateInfos();
                        })
                        .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                            dialog.dismiss();
                        })
                        .setCancelable(false)
                        .show();

                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private String getValue(EditText text) {
        return text.getText().toString().trim();
    }

    private void setValue(EditText text, String value, String defValue) {
        if (TextUtils.isEmpty(value)) {
            text.setText(defValue);
            return;
        }
        text.setText(value);
    }

    private void fillInfos() {
        mDeviceInfo.setBrand(getValue(edt_brand));
        mDeviceInfo.setModel(getValue(edt_model));
        mDeviceInfo.setProduct(getValue(edt_name));
        mDeviceInfo.setDevice(getValue(edt_device));
        mDeviceInfo.setBoard(getValue(edt_board));
        mDeviceInfo.setDisplay(getValue(edt_display));
        mDeviceInfo.setID(getValue(edt_id));
        mDeviceInfo.setSerial(getValue(edt_serial));
        mDeviceInfo.setManufacturer(getValue(edt_manufacturer));
        mDeviceInfo.setFingerprint(getValue(edt_fingerprint));

        mDeviceInfo.setDeviceId(getValue(edt_imei));
        mDeviceInfo.setIccId(getValue(edt_imsi));
        mDeviceInfo.setWifiMac(getValue(edt_mac));
        mDeviceInfo.setAndroidId(getValue(edt_androidId));
    }

    @SuppressLint("HardwareIds")
    private void updateInfos() {
        setValue(edt_brand, mDeviceInfo.getBrand(), Build.BRAND);
        setValue(edt_model, mDeviceInfo.getModel(), Build.MODEL);
        setValue(edt_name, mDeviceInfo.getProduct(), Build.PRODUCT);
        setValue(edt_device, mDeviceInfo.getDevice(), Build.DEVICE);
        setValue(edt_board, mDeviceInfo.getBoard(), Build.BOARD);
        setValue(edt_display, mDeviceInfo.getDisplay(), Build.DISPLAY);
        setValue(edt_id, mDeviceInfo.getID(), Build.ID);
        setValue(edt_serial, mDeviceInfo.getSerial(), Build.SERIAL);
        setValue(edt_manufacturer, mDeviceInfo.getManufacturer(), Build.MANUFACTURER);
        setValue(edt_fingerprint, mDeviceInfo.getFingerprint(), Build.FINGERPRINT);

        setValue(edt_imei, mDeviceInfo.getDeviceId(), mTelephonyManager.getDeviceId());
        setValue(edt_imsi, mDeviceInfo.getIccId(), mTelephonyManager.getSimSerialNumber());
        setValue(edt_mac, mDeviceInfo.getWifiMac(), getDefaultWifiMac());
        setValue(edt_androidId, mDeviceInfo.getAndroidId(), Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
    }

    @SuppressLint("HardwareIds")
    private String getDefaultWifiMac() {
        String[] files = {"/sys/class/net/wlan0/address", "/sys/class/net/eth0/address", "/sys/class/net/wifi/address"};
        String mac = mWifiManager.getConnectionInfo().getMacAddress();
        if (TextUtils.isEmpty(mac)) {
            for (String file : files) {
                try {
                    mac = loadFileAsString(file);
                } catch (IOException e) {
                    Log.w("kk", "read mac", e);
                }
                if (!TextUtils.isEmpty(mac)) {
                    break;
                }
            }
        }
        return mac;
    }

    private String loadFileAsString(String filePath)
            throws java.io.IOException {
        StringBuffer fileData = new StringBuffer(1000);
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        char[] buf = new char[1024];
        int numRead = 0;
        while ((numRead = reader.read(buf)) != -1) {
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
        }
        reader.close();
        return fileData.toString().trim();
    }
}
