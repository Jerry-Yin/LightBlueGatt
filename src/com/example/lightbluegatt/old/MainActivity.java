package com.example.lightbluegatt.old;


import java.util.List;

import com.example.lightbluegatt.R;
import com.example.lightbluegatt.old.BluetoothLeClass.OnConnectListener;
import com.example.lightbluegatt.old.BluetoothLeClass.OnDataAvailableListener;
import com.example.lightbluegatt.old.BluetoothLeClass.OnDisconnectListener;
import com.example.lightbluegatt.old.BluetoothLeClass.OnServiceDiscoverListener;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

// 搜索BLE终端
public class MainActivity extends Activity implements OnSeekBarChangeListener,
OnClickListener,OnConnectListener,OnDisconnectListener,
OnDataAvailableListener,OnServiceDiscoverListener{
	private static final String TAG = MainActivity.class.getSimpleName();
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothLeClass mBLE;
	/**
	 * 写数据的特征值UUID
	 */
	public static String UUID_CHAR = "0000fff6-0000-1000-8000-00805f9b34fb";
	private static BluetoothGattCharacteristic gattCharacteristic1 = null;
	private Button button;
	private TextView text;
	private TextView textAddress;
	private TextView textSeekbar;
	private SeekBar seekBar;
	private boolean mScanning=false;
	private int barValue=0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initBluetothAdapter();
		initView();
		mBLE = new BluetoothLeClass(this);
		if (!mBLE.initialize()) {
			finish();
		}
		mBLE.setOnServiceDiscoverListener(this);
		mBLE.setOnDataAvailableListener(this);
		mBLE.setOnConnectListener(this);
		mBLE.setOnDisconnectListener(this);
	}
	/**
	 * 初始化蓝牙适配器
	 * 手机系统要求需要在4.3以上
	 * 否则退出
	 */
	private void initBluetothAdapter(){
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			finish();
		}
		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();

		if (mBluetoothAdapter == null) {
			finish();
			return;
		}
		if(!mBluetoothAdapter.isEnabled()){
			mBluetoothAdapter.enable();//开启蓝牙
		}
	}
	private void initView(){
		text=(TextView)findViewById(R.id.textView);
		textAddress=(TextView)findViewById(R.id.text_address);
		textSeekbar=(TextView)findViewById(R.id.text_seekbar);
		button=(Button)findViewById(R.id.button);
		seekBar=(SeekBar)findViewById(R.id.seek_bar);
		button.setOnClickListener(this);
		seekBar.setOnSeekBarChangeListener(this);
	}
	
	/**
	 * 扫描设备
	 * enable 为true 表示开始扫描设备
	 * @param enable
	 */
	private void scanLeDevice(final boolean enable) {
		if (enable) {

			mScanning = true;
			mBluetoothAdapter.startLeScan(mLeScanCallback);
		} else {
			mScanning = false;
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
		}
	}
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

		@Override
		public void onLeScan(final BluetoothDevice device, int rssi,
				byte[] scanRecord) {
			
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					String deviceAddrss=device.getAddress();
					textAddress.setText(deviceAddrss);
				}
			});
		}
	};

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.button:
			if(mScanning==false){//没在扫描
				scanLeDevice(true);
				button.setText("停止扫描");
				text.setText("正在扫描设备");
			}else{
				scanLeDevice(false);
				text.setText("停止扫描设备");
				button.setText("开始扫描");
			}
			break;
		case R.id.text_address:
			String address=textAddress.getText().toString();
			if(address==null||address.length()<17){
				mBLE.connect(address);//连接设备
			}

		default:
			break;
		}
	}
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		barValue=progress;
	}
	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		
	}
	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		//停止滑动
		textSeekbar.setText("seekBar的值等于"+barValue);
		
	}
	/**
	 * 扫描到的服务
	 * @param gattServices
	 */
	private void displayGattServices(List<BluetoothGattService> gattServices) {
		if (gattServices == null)
			return;

		for (BluetoothGattService gattService : gattServices) {
			int type = gattService.getType();
			Log.i(TAG, "-->service uuid:" + gattService.getUuid());

			List<BluetoothGattCharacteristic> gattCharacteristics = gattService
					.getCharacteristics();
			for (final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
				Log.e(TAG, "---->char uuid:" + gattCharacteristic.getUuid());

				int permission = gattCharacteristic.getPermissions();

				int property = gattCharacteristic.getProperties();

				byte[] data = gattCharacteristic.getValue();

				if (gattCharacteristic.getUuid().toString().equals(UUID_CHAR)) {
					gattCharacteristic1 = gattCharacteristic;
					mBLE.setCharacteristicNotification(gattCharacteristic, true);
				}
			}
		}
	}
	@Override
	public void onServiceDiscover(BluetoothGatt gatt) {
		displayGattServices(mBLE.getSupportedGattServices());
		//发现服务
	}
	@Override
	public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
		//读数据回调
	}
	@Override
	public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
		//写数据之后回调
	}
	@Override
	public void onDisconnect(BluetoothGatt gatt) {
		//设备连接失败
	}
	@Override
	public void onConnect(BluetoothGatt gatt) {
		//设备连接成功
	}
}
