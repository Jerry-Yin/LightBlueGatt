package com.example.lightbluegatt.yin;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lightbluegatt.R;
import com.example.lightbluegatt.old.MainActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by JerryYin on 5/9/16.
 */
public class MainActivity2 extends Activity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, SeekBar.OnSeekBarChangeListener {

    private static final String TAG = "MainActivity2";
    private static final int REQUEST_ENABLE_BT = 0X00; // 请求打开蓝牙（int）>0 即可
    static final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mSocket;
    private ArrayAdapter<String> mArrayAdapter;
    private List<String> mPairedDeviceList = new ArrayList<>();
    private String mCurBondDevice;  //当前配对的设备

    private OutputStream mOutputStream = null;
    private InputStream mInputStream = null;

    private Button mBtnDiscovery;
    private TextView mTxtCurProgress, mTxtCurDevice;
    private SeekBar mSeekBar;
    private ProgressBar mProgressBar;
    private Switch mASwitch;

    private AlertDialog mDialog = null;



    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = null;
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                    //未配对的设备
                    String str = device.getName() + "| 未配对|" + "\n" + device.getAddress();
                    if (mPairedDeviceList.indexOf(str) == -1) {
                        // 防止重复添加
                        mPairedDeviceList.add(str); // 获取设备名称和mac地址
                        mArrayAdapter.notifyDataSetChanged();
                    }
                } else if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    mPairedDeviceList.add(device.getName() + "| 已配对|" + "\n" + device.getAddress());
                    mArrayAdapter.notifyDataSetChanged();
                }
                // Add the name and address to an array adapter to show in a ListView
//                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
//                mPairedDeviceList.add(device.getName() + "\n" + device.getAddress());
//                Log.d(TAG, "device.name(receiver) = " + device.getName() + " device.address = " + device.getAddress());

            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                switch (device.getBondState()) {
                    case BluetoothDevice.BOND_BONDING:
                        Log.d("BlueToothTestActivity", "正在配对......");
                        break;
                    case BluetoothDevice.BOND_BONDED:
                        Log.d("BlueToothTestActivity", "完成配对");
                        connect(device, null);//连接设备
                        break;
                    case BluetoothDevice.BOND_NONE:
                        Log.d("BlueToothTestActivity", "取消配对");
                    default:
                        break;
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_main2);

        initViews();
        initEvents();
    }

    private void initViews() {
        mBtnDiscovery = (Button) findViewById(R.id.btn_discovery);
        mTxtCurProgress = (TextView) findViewById(R.id.text_cur_progress);
        mTxtCurDevice = (TextView) findViewById(R.id.text_cur_device);
        mSeekBar = (SeekBar) findViewById(R.id.seek_bar);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mASwitch = (Switch) findViewById(R.id.switch_on);
        mASwitch.setOnCheckedChangeListener(this);
        mBtnDiscovery.setOnClickListener(this);
        mSeekBar.setOnSeekBarChangeListener(this);

        mArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mPairedDeviceList);
//        mArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
    }


    private void initEvents() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Log.d(TAG, "adapter = " + mBluetoothAdapter);

        // 初始化开关状态
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            mASwitch.setChecked(true);
        } else {
            mASwitch.setChecked(false);
        }
    }

    /**
     * 开关事件
     *
     * @param buttonView
     * @param isChecked
     */
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            if (mBluetoothAdapter == null) {
                Log.d(TAG, "mBluetoothAdapter is null");

            }

            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        } else {
            mBluetoothAdapter.disable();

        }
    }

    /**
     * 查询已经配对过的蓝牙设备
     */
    private void queryPairedDevices() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        // If there are paired devices
        Log.d(TAG, "size = " + pairedDevices.size());
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
//                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                mPairedDeviceList.add(device.getName() + "| 已配对|" + "\n" + device.getAddress());
                mArrayAdapter.notifyDataSetChanged();
//                Log.d(TAG, "device.name = " + device.getName() + " device.address = " + device.getAddress());
            }
        } else {
            Log.d(TAG, "there is no available paired devices");
        }
    }

    //注册搜索广播
    private void registerDiscoveringBroadcast() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver, filter);
    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_discovery) {
            if (!mBluetoothAdapter.isEnabled()) {
                Toast.makeText(this, "蓝牙未开启，请先打开蓝牙", Toast.LENGTH_SHORT).show();
            } else {
                mArrayAdapter.clear();
                mPairedDeviceList.clear();
                mArrayAdapter.notifyDataSetChanged();
                mProgressBar.setVisibility(View.VISIBLE);
                mTxtCurDevice.setText("正在搜索可用的蓝夜设备...");
//                queryPairedDevices();        //寻找已经配对过的设备

                registerDiscoveringBroadcast();  //注册广播接收器
                boolean hasDevice = mBluetoothAdapter.startDiscovery(); //扫描设备  （扫描过程中系统会自动发送广播）
                Log.d(TAG, "has = " + hasDevice);

                if (hasDevice) {
                    mProgressBar.setVisibility(View.INVISIBLE);
//                    mTxtCurDevice.setText("已经搜索到设备");
                    mTxtCurDevice.setText("正在搜索可用设备");

                    LayoutInflater inflater = LayoutInflater.from(this);
                    View dialogView = inflater.inflate(R.layout.layout_item_device_lists, null);
                    ListView listView = (ListView) dialogView.findViewById(R.id.list_devices);
                    listView.setAdapter(mArrayAdapter);
                    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            // TODO: 5/9/16  连接蓝牙
                            if (mBluetoothAdapter.isDiscovering())
                                mBluetoothAdapter.cancelDiscovery();

                            String str = mPairedDeviceList.get(position);
                            Log.d(TAG, "str = " + str);
                            String[] values = str.split("\\|");
                            String address = values[2].trim();
                            Log.d("address", values[2]);
                            //开始配对
                            boolean connected = createBond(address, null);
                            if (connected) {
                                Toast.makeText(MainActivity2.this, "配对成功！", Toast.LENGTH_SHORT).show();
                                mTxtCurDevice.setText("当前配对设备：" + values[0]);
                                mCurBondDevice = str;
                                if (mDialog != null) {
                                    mDialog.dismiss();
                                }
                            } else {
                                Toast.makeText(MainActivity2.this, "配对失败！", Toast.LENGTH_SHORT).show();
                                mTxtCurDevice.setText("未与任何设备配对：");
                                if (mDialog != null) {
                                    mDialog.dismiss();
                                }
                            }
                        }
                    });

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    mDialog = builder.setTitle("当前可用的蓝牙设备")
                            .setView(dialogView)
                            .create();
                    mDialog.show();


                } else {
                    mProgressBar.setVisibility(View.INVISIBLE);
                    mTxtCurDevice.setText("当前无可用设备...");
                }
            }
        }
    }


    /**
     * 根据address进行配对
     *
     * @param address
     * @return
     */
    private boolean createBond(String address, String data) {
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        Boolean returnValue = false;
        try {
            if (device.getBondState() == BluetoothDevice.BOND_NONE) {   //未配对
                //利用反射方法调用BluetoothDevice.createBond(BluetoothDevice remoteDevice);
                Method createBondMethod = BluetoothDevice.class.getMethod("createBond");
                Log.d("BlueToothTestActivity", "开始配对");
                returnValue = (Boolean) createBondMethod.invoke(device);

            } else if (device.getBondState() == BluetoothDevice.BOND_BONDED) {  //已经配对
                connect(device, data); //
                returnValue = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return returnValue;
    }

    /**
     * 解除 配对
     *
     * @return
     */
    private boolean removeBond(String address) {
        Method removeBondMethod = null;
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        Boolean returnValue = false;
        try {
            removeBondMethod = BluetoothDevice.class.getMethod("removeBond");
            returnValue = (Boolean) removeBondMethod.invoke(device);

        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return returnValue;
    }

    /**
     * 连接 并 通信
     * 蓝牙同时的本质是蓝牙套接字，一个主动发起连接的的设备做客户端，一个监听连接的设备做服务端，
     * 类似sokcet网络编程，利用多线程，读取数据流就可完成蓝牙通信。
     *
     * @param device
     */
    private void connect(BluetoothDevice device, String data) {
        UUID uuid = UUID.fromString(SPP_UUID);
//        UUID uuid = UUID.randomUUID();

        try {
            mSocket = device.createRfcommSocketToServiceRecord(uuid);
            Log.d("BlueToothTestActivity", "开始连接...");
            mSocket.connect();

            if (!TextUtils.isEmpty(data)){
                mOutputStream = mSocket.getOutputStream();
                mInputStream = mSocket.getInputStream();
//            mOutputStream.write("StartOnNet\n".getBytes());
                mOutputStream.write(data.getBytes());

                mOutputStream.flush();
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (!TextUtils.isEmpty(mCurBondDevice)){
            //确保当前有配对设备
            String[] values = mCurBondDevice.split("\\|");
            String address = values[2].trim();
            createBond(address, String.valueOf(progress));
        }
        mTxtCurProgress.setText("当前进度值:"+progress);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
    }

}
