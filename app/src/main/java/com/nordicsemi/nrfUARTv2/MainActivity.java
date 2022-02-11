/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nordicsemi.nrfUARTv2;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URISyntaxException;
import java.sql.Time;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;


import com.nordicsemi.nrfUARTv2.UartService;
import com.nordicsemi.nrfUARTv2.zxing.android.CaptureActivity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.text.LoginFilter;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.util.EncodingUtils;

import static com.nordicsemi.nrfUARTv2.UartService.DEVICE_DOES_NOT_SUPPORT_UART;

public class MainActivity extends Activity implements RadioGroup.OnCheckedChangeListener
{
    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int UART_PROFILE_READY = 10;
    public static final String TAG = "nRFUART";
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;
    private static final int STATE_OFF = 10;
    private static final int REQUEST_SELECT_FILE = 11;   // 返回的文件Code
    private static final String DECODED_CONTENT_KEY = "codedContent";
    private static final int REQUEST_CODE_SCAN = 10;  // 扫描权限返回值
    private static final int REQUEST_CODE_LOCATION = 101;

    TextView mRemoteRssiVal;
    RadioGroup mRg;
    private int mState = UART_PROFILE_DISCONNECTED;
    private UartService mService = null;
    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBtAdapter = null;
    private ListView messageListView;
    private ArrayAdapter<String> listAdapter;
    private Button btnConnectDisconnect, btnSend,selectFile,qrcode_scan,writeMAC;
    private TextView PackTotal,pakenumber,percentage,qrcode_data;
    private String filePath;
    private byte[] bt;

    /**
     * CRC16????
     *
     * @author yangle
     */
    private static final char crctable[] = { 0x0000, 0x1021, 0x2042, 0x3063,
            0x4084, 0x50a5, 0x60c6, 0x70e7, 0x8108, 0x9129, 0xa14a, 0xb16b,
            0xc18c, 0xd1ad, 0xe1ce, 0xf1ef, 0x1231, 0x0210, 0x3273, 0x2252,
            0x52b5, 0x4294, 0x72f7, 0x62d6, 0x9339, 0x8318, 0xb37b, 0xa35a,
            0xd3bd, 0xc39c, 0xf3ff, 0xe3de, 0x2462, 0x3443, 0x0420, 0x1401,
            0x64e6, 0x74c7, 0x44a4, 0x5485, 0xa56a, 0xb54b, 0x8528, 0x9509,
            0xe5ee, 0xf5cf, 0xc5ac, 0xd58d, 0x3653, 0x2672, 0x1611, 0x0630,
            0x76d7, 0x66f6, 0x5695, 0x46b4, 0xb75b, 0xa77a, 0x9719, 0x8738,
            0xf7df, 0xe7fe, 0xd79d, 0xc7bc, 0x48c4, 0x58e5, 0x6886, 0x78a7,
            0x0840, 0x1861, 0x2802, 0x3823, 0xc9cc, 0xd9ed, 0xe98e, 0xf9af,
            0x8948, 0x9969, 0xa90a, 0xb92b, 0x5af5, 0x4ad4, 0x7ab7, 0x6a96,
            0x1a71, 0x0a50, 0x3a33, 0x2a12, 0xdbfd, 0xcbdc, 0xfbbf, 0xeb9e,
            0x9b79, 0x8b58, 0xbb3b, 0xab1a, 0x6ca6, 0x7c87, 0x4ce4, 0x5cc5,
            0x2c22, 0x3c03, 0x0c60, 0x1c41, 0xedae, 0xfd8f, 0xcdec, 0xddcd,
            0xad2a, 0xbd0b, 0x8d68, 0x9d49, 0x7e97, 0x6eb6, 0x5ed5, 0x4ef4,
            0x3e13, 0x2e32, 0x1e51, 0x0e70, 0xff9f, 0xefbe, 0xdfdd, 0xcffc,
            0xbf1b, 0xaf3a, 0x9f59, 0x8f78, 0x9188, 0x81a9, 0xb1ca, 0xa1eb,
            0xd10c, 0xc12d, 0xf14e, 0xe16f, 0x1080, 0x00a1, 0x30c2, 0x20e3,
            0x5004, 0x4025, 0x7046, 0x6067, 0x83b9, 0x9398, 0xa3fb, 0xb3da,
            0xc33d, 0xd31c, 0xe37f, 0xf35e, 0x02b1, 0x1290, 0x22f3, 0x32d2,
            0x4235, 0x5214, 0x6277, 0x7256, 0xb5ea, 0xa5cb, 0x95a8, 0x8589,
            0xf56e, 0xe54f, 0xd52c, 0xc50d, 0x34e2, 0x24c3, 0x14a0, 0x0481,
            0x7466, 0x6447, 0x5424, 0x4405, 0xa7db, 0xb7fa, 0x8799, 0x97b8,
            0xe75f, 0xf77e, 0xc71d, 0xd73c, 0x26d3, 0x36f2, 0x0691, 0x16b0,
            0x6657, 0x7676, 0x4615, 0x5634, 0xd94c, 0xc96d, 0xf90e, 0xe92f,
            0x99c8, 0x89e9, 0xb98a, 0xa9ab, 0x5844, 0x4865, 0x7806, 0x6827,
            0x18c0, 0x08e1, 0x3882, 0x28a3, 0xcb7d, 0xdb5c, 0xeb3f, 0xfb1e,
            0x8bf9, 0x9bd8, 0xabbb, 0xbb9a, 0x4a75, 0x5a54, 0x6a37, 0x7a16,
            0x0af1, 0x1ad0, 0x2ab3, 0x3a92, 0xfd2e, 0xed0f, 0xdd6c, 0xcd4d,
            0xbdaa, 0xad8b, 0x9de8, 0x8dc9, 0x7c26, 0x6c07, 0x5c64, 0x4c45,
            0x3ca2, 0x2c83, 0x1ce0, 0x0cc1, 0xef1f, 0xff3e, 0xcf5d, 0xdf7c,
            0xaf9b, 0xbfba, 0x8fd9, 0x9ff8, 0x6e17, 0x7e36, 0x4e55, 0x5e74,
            0x2e93, 0x3eb2, 0x0ed1, 0x1ef0
    };

    public static char calc (byte[] bytes)
    {
        char crc = 0x0000;
        for (byte b : bytes) {
            crc = (char) ( (crc << 8) ^ crctable[ ( (crc >> 8) ^ b) & 0x00ff]);
        }
        return (char) (crc);
    }


    // 读取SD卡中的文件
    public String readFileSdcardFile(String fileName){
        String res="";
        Log.d(TAG,"选择SD卡中的文件");
        try{
            FileInputStream fin = new FileInputStream(fileName);

            int length = fin.available();

            byte [] buffer = new byte[length];

            fin.read(buffer);

            res = EncodingUtils.getString(buffer, "UTF-8");

            fin.close();
        }

        catch(Exception e){
            e.printStackTrace();
        }
        return res;
    }


    // 16进制转换
    public static String bytes2HexString(byte[] array) {
        StringBuilder builder = new StringBuilder();
        for (byte b : array) {
            String hex = Integer.toHexString(b & 0xFF);

            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            builder.append(hex);
        }

        return builder.toString().toUpperCase();
    }

    private byte [] binBuffer = new byte[30000];
    private byte [] send128Buffer = new byte[133];
    private int binLenth;
    private int sendpackg;
    private int blepakgIndex;
    private boolean packSendFlag = false;

    private int name;
    private Timer timer;
    private TimerTask task;
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            // 得到用户的选择权限结果
            try{
                if(blepakgIndex==0x00){
                    send128Buffer[0] = 0x01;
                    send128Buffer[1] = (byte)(sendpackg+1) ;
                    send128Buffer[2] = (byte)(~(sendpackg+1)) ;
                    System.arraycopy(binBuffer, sendpackg*128, send128Buffer, 3, 128);
                    byte [] buf = new byte[128];
                    System.arraycopy(binBuffer, sendpackg*128, buf, 0, 128);
                    char crcVuale = calc(buf);
                    send128Buffer[131] =  (byte) (crcVuale>>8);
                    send128Buffer[132] =  (byte) (crcVuale);
                }

                byte [] bleSendbuf = new byte[20];

                Log.e (TAG, "blepakgIndex="+blepakgIndex);

                if(blepakgIndex>6) {
                    blepakgIndex++;
                    if((blepakgIndex==40)||(packSendFlag==false)){
                        //  if(blepakgIndex==40) {
                        if(blepakgIndex<40) {
                            sendpackg++;
                        }

                        blepakgIndex = 0x00;
                        pakenumber.setText("已发包:"+sendpackg);
                        // 百分比
                        NumberFormat numberFormat = NumberFormat.getNumberInstance();   // 创建一个数值格式化对象
                        numberFormat.setMaximumFractionDigits(2);    // 保留后两位
                        String result = numberFormat.format((float) sendpackg / (float) (binLenth/128) * 100);
                        percentage.setText("进度："+ result + "%");
                        Log.d(TAG,"result===="+result);
                        if(sendpackg==(binLenth/128))
                        {
                            String message = "04";
                            byte [] blbuf = {0x04};
                            mService.writeRXCharacteristic (blbuf);
                            Log.d(TAG,"OTA has complete,should disconntent devices");

                            String currentDateTimeString = DateFormat.getTimeInstance().format (new Date() );
                            listAdapter.add ("[" + currentDateTimeString + "] TX: " + message);
                            messageListView.smoothScrollToPosition (listAdapter.getCount() - 1);
                            timer.cancel();
                            listAdapter.add ("[" + currentDateTimeString + "] 设备"+mDevice.getName()+"升级成功！！！");
                            messageListView.smoothScrollToPosition (listAdapter.getCount() - 1);
                        }
                    }
                }
                else if(blepakgIndex==6) {
                    packSendFlag = true;
                    byte [] blebuf = new byte[13];
                    System.arraycopy(send128Buffer, blepakgIndex * 20, blebuf, 0, 13);
                    mService.writeRXCharacteristic (blebuf);
                    blepakgIndex++;
                }else{
                    System.arraycopy(send128Buffer, blepakgIndex*20, bleSendbuf, 0, 20);
                    mService.writeRXCharacteristic (bleSendbuf);
                    blepakgIndex++;
                }
            }
            catch(Exception e){
                e.printStackTrace();
            }
            // 想要执行的事件
            super.handleMessage(msg);
        }
    };


    @Override
    public void onCreate (Bundle savedInstanceState)
    {
        super.onCreate (savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView (R.layout.main);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null)
        {
            Toast.makeText (this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        messageListView = (ListView) findViewById (R.id.listMessage);
        listAdapter = new ArrayAdapter<String> (this, R.layout.message_detail);
        messageListView.setAdapter (listAdapter);
        messageListView.setDivider (null);
        btnConnectDisconnect = (Button) findViewById (R.id.btn_select);
        btnSend = (Button) findViewById (R.id.sendButton2);
        PackTotal  = (TextView) findViewById(R.id.PackLabel);
        pakenumber  = (TextView) findViewById(R.id.pakeMassge);
        selectFile = (Button) findViewById(R.id.select_file);
        percentage = (TextView) findViewById(R.id.percentage);
        qrcode_scan = (Button) findViewById(R.id.qrcode_scan);
        writeMAC = (Button) findViewById(R.id.writeMAC);
        qrcode_data = (TextView) findViewById(R.id.qrcode_data);
        service_init();
        requestLocationPerminssion();

        // 扫描二维码
        qrcode_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.qrcode_scan:
                        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
                            goScan();
                        } else {
                            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.CAMERA},1);
                        }
                        break;
                    default:
                        break;
                }
            }
        });

        writeMAC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = "写入MAC地址";

                try
                {
                    mService.writeRXCharacteristic (bt);
                    String currentDateTimeString = DateFormat.getTimeInstance().format (new Date() );
                    listAdapter.add ("[" + currentDateTimeString + "] TX: " + message);
                    listAdapter.add ("[" + currentDateTimeString + "] TX: " + bytes2HexString(bt));
                    messageListView.smoothScrollToPosition (listAdapter.getCount() - 1);
                }
                catch (Exception e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

        // OnClick select a file
        selectFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isGrantExternalRW()) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("*/*");   // 选择任意类型
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(intent,REQUEST_SELECT_FILE);
                }
            }
        });

        // Handler Disconnect & Connect button
        btnConnectDisconnect.setOnClickListener (new View.OnClickListener()
        {
            @Override
            public void onClick (View v)
            {
                if (!mBtAdapter.isEnabled() )
                {
                    // 表示蓝牙不可用
                    Log.i (TAG, "onClick - BT not enabled yet");
                    Intent enableIntent = new Intent (BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult (enableIntent, REQUEST_ENABLE_BT);
                }
                else
                {
                    if (btnConnectDisconnect.getText().equals ("Connect") )
                    {

                        //Connect button pressed, open DeviceListActivity class, with popup windows that scan for devices

                        Intent newIntent = new Intent (MainActivity.this, DeviceListActivity.class);
                        startActivityForResult (newIntent, REQUEST_SELECT_DEVICE);
                    }
                    else
                    {
                        // Disconnect button pressed 点击进行设备断连
                        if (mDevice != null)
                        {
                            mService.disconnect();
                        }
                    }
                }
            }
        });

        // Handler Send button
        btnSend.setOnClickListener (new View.OnClickListener()
        {
            @Override
            public void onClick (View v)
            {
                timer = new Timer();
                String message = "升级命令";
                byte[] value = {
                        (byte) 0xFE, 0x07,0x01, 0x00, 0x06,  // G36
                        (byte) 0xFE, (byte) 0x0D ,0x01 ,0x00,(byte) 0xF2   // VMS

                };

                Log.e (TAG, "onClick: send like bool");

                try
                {
                    //send data to service
                    mService.writeRXCharacteristic (value);
                    //Update the log with time stamp
                    String currentDateTimeString = DateFormat.getTimeInstance().format (new Date() );
                    listAdapter.add ("[" + currentDateTimeString + "] TX: " + message);
                    listAdapter.add ("[" + currentDateTimeString + "] TX: " + bytes2HexString(value));
                    messageListView.smoothScrollToPosition (listAdapter.getCount() - 1);
                }
                catch (Exception e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
    }

    // 调用扫描
    private void goScan(){
        Intent intent = new Intent(MainActivity.this,CaptureActivity.class);
        startActivityForResult(intent,REQUEST_CODE_SCAN);
    }

    private void requestLocationPerminssion () {
        boolean foreground = ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (foreground) {
        } else {
            ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.ACCESS_COARSE_LOCATION},2);
        }
    }

    // 调用存储权限
    private boolean isGrantExternalRW() {
        int storagePermission = ActivityCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (storagePermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE},3);
            return false;
        }
        return  true;
    }


    // 去掉字符串中的：
    private static String replaceString(String str){
        String regEx = "[:]";
        String rep = "";
        String newString =  str.replaceAll(regEx,rep);
//        Log.d(TAG,newString);
        return newString;
    }

    // 转成十六进制
    public static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
//        Log.d(TAG,bytes2HexString(d));
        return d;
    }
    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    // 两个byte[]的拼接
    static byte[] bt1 = {
            (byte)0xFE, (byte)0xF0, 0x06, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,0x00
    };

    public static byte[] newByte (byte[] bt2) {
        System.arraycopy(bt2, 0, bt1, 3, 6);
        Log.d(TAG,bytes2HexString(bt1));

        byte[] bt4 = { 0x00 };
        System.arraycopy(bt4,0,bt1,9,1);

        byte[] bt3 = hexStringToBytes(makeChecksum(bytes2HexString(bt1)));  //拼接后直接在此方法中调用makeChecksum方法计算校验值
        System.arraycopy(bt3,0,bt1,9,1);    // 将计算的校验码得到替换掉最后一位
//        Log.d(TAG,"拼接校验后的byte指令"+bytes2HexString(bt1));
        return bt1;
    }


    // 校验码计算
    public static String makeChecksum(String hexdata) {
        if (hexdata == null || hexdata.equals("")) {
            return "00";
        }
        hexdata = hexdata.replaceAll(" ", "");
        int total = 0;
        int len = hexdata.length();
        if (len % 2 != 0) {
            return "00";
        }
        int num = 0;
        while (num < len) {
            String s = hexdata.substring(num, num + 2);
            total += Integer.parseInt(s, 16);
            num = num + 2;
        }
//        Log.d(TAG,"校验码======"+hexInt((total & 0xff) << 8));
//        Log.d(TAG,"校验码======"+hexInt(total));
        return hexInt((total & 0xff) << 8);
    }

    private static String hexInt(int total) {
        int a = total / 256;
        int b = total % 256;
        if (a > 255) {
            return hexInt(a) + format(b);
        }
        return format(a) + format(b);
    }

    private static String format(int hex) {
        String hexa = Integer.toHexString(hex);
        int len = hexa.length();
        if (len < 2) {
            hexa = "0" + hexa;
        }
        return hexa;
    }

    // 返回调用相机CAMER权限值
    public void onRequestPermissionsResult(int requsetCode,String[] permissions,int[] grantResults) {
        switch (requsetCode) {
            case 1:
                Log.d(TAG,"Line 581 当前的grantResults[0]="+requsetCode);
//                需要使用core3.3.0及以上的jar包版本 才能得到此回调
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    goScan();
                } else {
                    Toast.makeText(this, "拒绝相机权限申请！！！", Toast.LENGTH_SHORT).show();
                }
                break;
            case 2 :
                boolean foreground = false;
                if (grantResults.length > 0 && grantResults[0] >= 0) {
                    foreground = true;
                    Toast.makeText(getApplicationContext(),"地理位置已被允许",Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(),"地理位置不被允许，将无法搜索到设备！！！",Toast.LENGTH_SHORT).show();
                }
                break;
            case 3:
                if (grantResults.length > 0 &&grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this,"获得存储权限",Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "拒绝访问存储，将无法选择文件！！！", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

    //UART service connected/disconnected
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected (ComponentName className, IBinder rawBinder)
        {
            mService = ( (UartService.LocalBinder) rawBinder).getService();
            Log.d (TAG, "onServiceConnected mService= " + mService);
            if (!mService.initialize() )
            {
                Log.e (TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }

        public void onServiceDisconnected (ComponentName classname)
        {
            ////     mService.disconnect(mDevice);
            mService = null;
        }
    };

    private Handler mHandler = new Handler() {
        @Override

        //Handler events that received from UART service
        public void handleMessage (Message msg)
        {

        }
    };

    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver()
    {

        public void onReceive (Context context, Intent intent)
        {
            String action = intent.getAction();
            Log.d(TAG,"ACTION==="+action);
            final Intent mIntent = intent;
            //*********************//
            if (action.equals (UartService.ACTION_GATT_CONNECTED) )
            {
                runOnUiThread (new Runnable()
                {
                    public void run()
                    {
                        Log.d (TAG, "UART_CONNECT_MSG");
                        btnConnectDisconnect.setText ("Disconnect");
                        selectFile.setEnabled(true);
                        ( (TextView) findViewById (R.id.deviceName) ).setText (mDevice.getName() + " - ready");
                        String currentDateTimeString = DateFormat.getTimeInstance().format (new Date() );
                        listAdapter.add ("[" + currentDateTimeString + "] Connected to: " + mDevice.getName() );
                        messageListView.smoothScrollToPosition (listAdapter.getCount() - 1);
                        mState = UART_PROFILE_CONNECTED;
                    }
                });
            }

            //*********************//
            if (action.equals (UartService.ACTION_GATT_DISCONNECTED) )
            {
                runOnUiThread (new Runnable()
                {
                    public void run()
                    {
                        Log.d (TAG, "UART_DISCONNECT_MSG");
                        btnConnectDisconnect.setText ("Connect");
//                        btnSend.setEnabled (false);
                        selectFile.setEnabled(false);
                        ( (TextView) findViewById (R.id.deviceName) ).setText ("Not Connected");
                        String currentDateTimeString = DateFormat.getTimeInstance().format (new Date() );
                        listAdapter.add ("[" + currentDateTimeString + "] Disconnected to: " + mDevice.getName() );
                        mState = UART_PROFILE_DISCONNECTED;
                        mService.close();
                    }
                });
            }


            //*********************//
            if (action.equals (UartService.ACTION_GATT_SERVICES_DISCOVERED) )
            {
                mService.enableTXNotification();
            }
            //*********************//
            if (action.equals (UartService.ACTION_DATA_AVAILABLE) )
            {

                final byte[] rxValue = intent.getByteArrayExtra (UartService.EXTRA_DATA);
                Log.d(TAG,"此处的rxvalue==" + bytes2HexString(rxValue));
                runOnUiThread (new Runnable()
                {
                    public void run()
                    {
                        try
                        {
                            String text = "";

                            if (rxValue[0]== 0x43) {
                                String message = "开始命令";

                                task = new TimerTask() {
                                    @Override
                                    public void run() {
//                                         TODO Auto-generated method stub
                                        Message message = new Message();
                                        message.what = 1;
                                        handler.sendMessage(message);
                                    }
                                };

                                try {

                                    //Update the log with time stamp
                                    String currentDateTimeString = DateFormat.getTimeInstance().format (new Date() );
                                    listAdapter.add("[" + currentDateTimeString + "] TX: " + message);
                                    messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
                                    FileInputStream fin = new FileInputStream(filePath);
                                    if (filePath == null){
                                        Log.d(TAG,"请选择一个文件");
                                        Toast.makeText(getApplicationContext(), "Please chose a file", Toast.LENGTH_SHORT).show();
                                    }else {
                                        int length = fin.available();
                                        fin.read(binBuffer);
                                        binLenth = ((length / 128) * 128);
                                        Log.d(TAG, "binLength==" + binLenth);
                                        if (length % 128 != 0x00) {
                                            binLenth += 128;
                                            for (int ii = length; ii < binLenth; ii++) {
                                                binBuffer[ii] = (byte) 0xFF;
                                            }
                                        }
                                        sendpackg = 0x00;
                                        blepakgIndex = 0x00;

                                        PackTotal.setText("总包数:" + binLenth / 128);
                                        pakenumber.setText("已发包:" + sendpackg);

                                        timer.schedule(task, 1000, 104);  // 最少96全部传输完成 耗时2m20

                                        Log.e(TAG, "onClick_readbinlen: " + length);
                                        Log.e(TAG, "onClick_packbinlen: " + binLenth);

                                        fin.close();
                                    }
                                }
                                catch(Exception e){
                                    e.printStackTrace();
                                }
                            }

                            if(packSendFlag==true){
                                if(rxValue[0]==0x06 || rxValue[0] == 0x04){
                                    packSendFlag=false;
                                    text = "PACK:OK "+String.valueOf(sendpackg);
                                } else{
                                    text = "PACK:FAIL"+ String.valueOf(sendpackg);
                                    // 此处是异常处理（没有收到04 || 06）时做的操作--->直接进行取消OTA
                                    timer.cancel();
                                    mService.disconnect();
                                    String currentDateTimeString = DateFormat.getTimeInstance().format (new Date() );
                                    listAdapter.add ("[" + currentDateTimeString + "] 升级异常，已退出升级，请重新升级");
                                    messageListView.smoothScrollToPosition (listAdapter.getCount() - 1);
                                }
                            }
                            else {
                                text = new String(rxValue, "UTF-8");
                            }

                            String veiwtext =  bytes2HexString(rxValue);
//                            Log.d(TAG,veiwtext);
                            String currentDateTimeString = DateFormat.getTimeInstance().format (new Date() );
                            listAdapter.add ("[" + currentDateTimeString + "] RX: " + veiwtext );
                            messageListView.smoothScrollToPosition (listAdapter.getCount() - 1);

                        }
                        catch (Exception e)
                        {
                            Log.e (TAG, e.toString() );
                        }
                    }
                });
            }
            //*********************//
            if (action.equals (DEVICE_DOES_NOT_SUPPORT_UART) )
            {
                showMessage ("Device doesn't support UART. Disconnecting");
                mService.disconnect();
            }
        }
    };

    private void service_init()
    {
        Intent bindIntent = new Intent (this, UartService.class);
        bindService (bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        LocalBroadcastManager.getInstance (this).registerReceiver (UARTStatusChangeReceiver, makeGattUpdateIntentFilter() );
    }
    private static IntentFilter makeGattUpdateIntentFilter()
    {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction (UartService.ACTION_GATT_CONNECTED);
        intentFilter.addAction (UartService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction (UartService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction (UartService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction (DEVICE_DOES_NOT_SUPPORT_UART);
        return intentFilter;
    }
    @Override
    public void onStart()
    {
        super.onStart();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        Log.d (TAG, "onDestroy()");

        try
        {
            LocalBroadcastManager.getInstance (this).unregisterReceiver (UARTStatusChangeReceiver);
        }
        catch (Exception ignore)
        {
            Log.e (TAG, ignore.toString() );
        }
        unbindService (mServiceConnection);
        mService.stopSelf();
        mService = null;
    }

    @Override
    protected void onStop()
    {
        Log.d (TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onPause()
    {
        Log.d (TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onRestart()
    {
        super.onRestart();
        Log.d (TAG, "onRestart");
    }

    @Override
    public void onResume()
    {
        super.onResume();
        Log.d (TAG, "onResume");
        if (!mBtAdapter.isEnabled() )
        {
            Log.i (TAG, "onResume - BT not enabled yet");
            Intent enableIntent = new Intent (BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult (enableIntent, REQUEST_ENABLE_BT);
        }

    }

    @Override
    public void onConfigurationChanged (Configuration newConfig)
    {
        super.onConfigurationChanged (newConfig);
    }

    @Override
    public void onActivityResult (int requestCode, int resultCode, Intent data)
    {

        Log.d(TAG,"返回的result_code=="+requestCode);
        switch (requestCode)
        {
            case REQUEST_SELECT_DEVICE:
                //When the DeviceListActivity return, with the selected device address
                // 当DeviceListActivity返回时，带有选择的设备地址
                if (resultCode == Activity.RESULT_OK && data != null)
                {
                    String deviceAddress = data.getStringExtra (BluetoothDevice.EXTRA_DEVICE);
                    mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice (deviceAddress);

                    Log.d (TAG, "... onActivityResultdevice.address==" + mDevice + "mserviceValue" + mService);
                    ( (TextView) findViewById (R.id.deviceName) ).setText (mDevice.getName() + " - connecting");
                    mService.connect (deviceAddress);

                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK)
                {
                    Toast.makeText (this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();

                }
                else
                {
                    // User did not enable Bluetooth or an error occurred
                    Log.d (TAG, "BT not enabled");
                    Toast.makeText (this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            case REQUEST_SELECT_FILE:
                if (resultCode == Activity.RESULT_OK && data != null){
                    Uri uri = data.getData();
                    Log.d(TAG,"FILE URI=="+uri.toString());
                    // 得到path及文件名
                    String path = null;
                    String pathName = "";
                    try {
                        path = fileUtils.getPath(this,uri);
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                    filePath = path;
                    Log.d(TAG,"当前的PATH==="+filePath);
                    pathName = path.substring(path.lastIndexOf("/")+1);     // 取最后一个/后的字符
                    ((TextView)findViewById(R.id.file_name)).setText(pathName);
                    if (pathName  == null || pathName.equals("")) {
                        btnSend.setEnabled(false);
                    } else {
                        btnSend.setEnabled(true);
                    }
                }
                break;
            case REQUEST_CODE_SCAN:
                if (resultCode == Activity.RESULT_OK){
                    if (data != null) {
                        String content  = data.getStringExtra(DECODED_CONTENT_KEY);
                        String request_data = replaceString(content.substring(content.indexOf("&m=") + 3));
                        bt = newByte(hexStringToBytes(request_data));
                        qrcode_data.setText(bytes2HexString(bt));
                    }
                }
                break;
            default:
                Log.e (TAG, "wrong request code");
                break;
        }
        super.onActivityResult(requestCode,resultCode,data);
    }

    @Override
    public void onCheckedChanged (RadioGroup group, int checkedId)
    {
    }


    private void showMessage (String msg)
    {
        Toast.makeText (this, msg, Toast.LENGTH_SHORT).show();

    }

    @Override

    public void onBackPressed()
    {
        if (mState == UART_PROFILE_CONNECTED)
        {
            Intent startMain = new Intent (Intent.ACTION_MAIN);
            startMain.addCategory (Intent.CATEGORY_HOME);
            startMain.setFlags (Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity (startMain);
            showMessage ("nRFUART's running in background.\n             Disconnect to exit");
        }
        else
        {
            new AlertDialog.Builder (this)
                    .setIcon (android.R.drawable.ic_dialog_alert)
                    .setTitle (R.string.popup_title)
                    .setMessage (R.string.popup_message)
                    .setPositiveButton (R.string.popup_yes, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick (DialogInterface dialog, int which)
                        {
                            finish();
                        }
                    })
                    .setNegativeButton (R.string.popup_no, null)
                    .show();
        }
    }
}