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
import java.text.DateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;


import com.nordicsemi.nrfUARTv2.UartService;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
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

    TextView mRemoteRssiVal;
    RadioGroup mRg;
    private int mState = UART_PROFILE_DISCONNECTED;
    private UartService mService = null;
    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBtAdapter = null;
    private ListView messageListView;
    private ArrayAdapter<String> listAdapter;
    private Button btnConnectDisconnect, btnSend, upSend,SureButton,restcmdButton,quercmdbutton,oldcmdButton,deoldcmdButton;
    private TextView PackTotal,pakenumber;
    //private EditText edtMessage;

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


    public String readFileSdcardFile(String fileName){
        String res="";
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
    private final Timer timer = new Timer();
    private TimerTask task;
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

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
                        if(sendpackg==(binLenth/128))
                        {
                            byte [] blbuf = {0x04};
                            mService.writeRXCharacteristic (blbuf);
                            timer.cancel();
                            upSend.setEnabled(true);
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
        upSend = (Button) findViewById (R.id.upButton);
        PackTotal  = (TextView) findViewById(R.id.PackLabel);
        pakenumber  = (TextView) findViewById(R.id.pakeMassge);
        SureButton = (Button) findViewById(R.id.sureButton);
        restcmdButton = (Button) findViewById(R.id.restButton);
        quercmdbutton = (Button) findViewById(R.id.querButton3);
        oldcmdButton = (Button) findViewById(R.id.oldButton3);
        deoldcmdButton = (Button) findViewById(R.id.deolde);
        // edtMessage = (EditText) findViewById(R.id.sendText);
        service_init();



        // Handler Disconnect & Connect button
        btnConnectDisconnect.setOnClickListener (new View.OnClickListener()
        {
            @Override
            public void onClick (View v)
            {
                if (!mBtAdapter.isEnabled() )
                {
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
                        //Disconnect button pressed
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

                //EditText editText = (EditText) findViewById(R.id.sendText);

                String message = "升级命令";
                //(byte) 0xFE, (byte) 0x01, (byte) 0x81, (byte) 0x85, 0x04, 0x01, 0x01, 0x01, 0x02, (byte) 0xf1,
                byte[] value = {
//                        (byte) 0xFE, (byte) 0x01, (byte) 0x81, (byte) 0x85, 0x04, 0x01, (byte)0xFF, 0x01, (byte)0xFF, (byte) 0xf6,
//                        (byte) 0xFE, (byte) 0x01, (byte) 0x81, (byte) 0x85, 0x04, 0x02, (byte)0xFF, 0x01, (byte)0xFF, (byte) 0xf5
                        (byte) 0xFE, 0x07,0x01, 0x00, 0x06
                };

                Log.e (TAG, "onClick: send like bool");
                try
                {
                    //send data to service
                    mService.writeRXCharacteristic (value);
                    //Update the log with time stamp
                    String currentDateTimeString = DateFormat.getTimeInstance().format (new Date() );
                    listAdapter.add ("[" + currentDateTimeString + "] TX: " + message);
                    messageListView.smoothScrollToPosition (listAdapter.getCount() - 1);
                    //edtMessage.setText("");
                    //} catch (UnsupportedEncodingException e) {
                }
                catch (Exception e)
                {
                    // TODO Auto-generated catch block
                    //e.printStackTrace();
                }
                SureButton.setEnabled(true);

            }
        });
        // Handler Send button
        restcmdButton.setOnClickListener (new View.OnClickListener()
        {
            @Override
            public void onClick (View v)
            {

                //EditText editText = (EditText) findViewById(R.id.sendText);

                String message = "复位命令";

                byte[] value = {(byte) 0xFE, (byte) 0x01, (byte) 0x81, (byte) 0x82, 0x01, 0x01,(byte) 0xfb};

                Log.e (TAG, "onClick: send like bool");
                try
                {
                    //send data to service
                    mService.writeRXCharacteristic (value);
                    //Update the log with time stamp
                    String currentDateTimeString = DateFormat.getTimeInstance().format (new Date() );
                    listAdapter.add ("[" + currentDateTimeString + "] TX: " + message);
                    messageListView.smoothScrollToPosition (listAdapter.getCount() - 1);
                    //edtMessage.setText("");
                    //} catch (UnsupportedEncodingException e) {
                }
                catch (Exception e)
                {
                    // TODO Auto-generated catch block
                    //e.printStackTrace();
                }

            }
        });

        // Handler Send button
        oldcmdButton.setOnClickListener (new View.OnClickListener()
        {
            @Override
            public void onClick (View v)
            {

                //EditText editText = (EditText) findViewById(R.id.sendText);

                String message = "老化命令";

                byte[] value = {(byte) 0xFE, (byte) 0x01, (byte) 0x81, (byte) 0xF1, 0x01, 0x01,(byte) 0x8C};

                Log.e (TAG, "onClick: send like bool");
                try
                {
                    //send data to service
                    mService.writeRXCharacteristic (value);
                    //Update the log with time stamp
                    String currentDateTimeString = DateFormat.getTimeInstance().format (new Date() );
                    listAdapter.add ("[" + currentDateTimeString + "] TX: " + message);
                    messageListView.smoothScrollToPosition (listAdapter.getCount() - 1);
                    //edtMessage.setText("");
                    //} catch (UnsupportedEncodingException e) {
                }
                catch (Exception e)
                {
                    // TODO Auto-generated catch block
                    //e.printStackTrace();
                }

            }
        });
 // Handler deold button
        deoldcmdButton.setOnClickListener (new View.OnClickListener()
        {
            @Override
            public void onClick (View v)
            {

                //EditText editText = (EditText) findViewById(R.id.sendText);

                String message = "去老化命令";

                byte[] value = {(byte) 0xFE, (byte) 0x01, (byte) 0x81, (byte) 0xF1, 0x01, 0x00,(byte) 0x8D};

                Log.e (TAG, "onClick: send like bool");
                try
                {
                    //send data to service
                    mService.writeRXCharacteristic (value);
                    //Update the log with time stamp
                    String currentDateTimeString = DateFormat.getTimeInstance().format (new Date() );
                    listAdapter.add ("[" + currentDateTimeString + "] TX: " + message);
                    messageListView.smoothScrollToPosition (listAdapter.getCount() - 1);
                    //edtMessage.setText("");
                    //} catch (UnsupportedEncodingException e) {
                }
                catch (Exception e)
                {
                    // TODO Auto-generated catch block
                    //e.printStackTrace();
                }

            }
        });

        // Handler Send button
        quercmdbutton.setOnClickListener (new View.OnClickListener()
        {
            @Override
            public void onClick (View v)
            {

                //EditText editText = (EditText) findViewById(R.id.sendText);

                String message = "查询命令 ";

                byte[] value = {(byte) 0xFE, (byte) 0x01, (byte) 0x81, (byte) 0x01, 0x04, 0x00,0x00,0x00,0x00,(byte) 0x7A};

                Log.e (TAG, "onClick: send like bool");
                try
                {
                    //send data to service
                    mService.writeRXCharacteristic (value);
                    //Update the log with time stamp
                    String currentDateTimeString = DateFormat.getTimeInstance().format (new Date() );
                    listAdapter.add ("[" + currentDateTimeString + "] TX: " + message+mDevice.getAddress());
                    messageListView.smoothScrollToPosition (listAdapter.getCount() - 1);
                    //edtMessage.setText("");
                    //} catch (UnsupportedEncodingException e) {
                }
                catch (Exception e)
                {
                    // TODO Auto-generated catch block
                    //e.printStackTrace();
                }

            }
        });
        // Handler Send button
        SureButton.setOnClickListener (new View.OnClickListener()
        {
            @Override
            public void onClick (View v)
            {
                String message = "确认 1";

                byte[] value = {0x31};

                try
                {
                    mService.writeRXCharacteristic (value);
                    //Update the log with time stamp
                    String currentDateTimeString = DateFormat.getTimeInstance().format (new Date() );
                    listAdapter.add ("[" + currentDateTimeString + "] TX: " + message);
                    messageListView.smoothScrollToPosition (listAdapter.getCount() - 1);
                    //edtMessage.setText("");
                    //} catch (UnsupportedEncodingException e) {
                }
                catch (Exception e)
                {
                    // TODO Auto-generated catch block
                    //e.printStackTrace();
                }

            }
        });

        // Set initial UI state
        // Handler Send button发送按钮线程
        upSend.setOnClickListener (new View.OnClickListener()
        {
            @Override
           public void onClick (View v)
            {
                upSend.setEnabled(false);

                task = new TimerTask() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        Message message = new Message();
                        message.what = 1;
                        handler.sendMessage(message);
                    }
                };

                try{
                    FileInputStream fin = new FileInputStream("/sdcard/OTA/G36_ME32F031C8T6_APP_20211112A.bin");
                    Log.i(TAG,"寻找到bin文件");

                    int length = fin.available();

                    Log.d(TAG,"LENGTH==="+length);

                    fin.read(binBuffer);

                    binLenth = ((length/128)*128);
                    Log.d(TAG,"binLength=="+binLenth);


                    if(length%128!=0x00)
                    {
                        binLenth+=128;
                       for (int ii = length; ii < binLenth; ii++) {
                            binBuffer[ii] = (byte) 0xFF;
                        }
                    }
                    sendpackg = 0x00;
                    blepakgIndex = 0x00;

                    PackTotal.setText("总包数:"+binLenth/128);
                    pakenumber.setText("已发包:"+sendpackg);

                    timer.schedule(task, 1000, 100);  // 最少96全部传输完成 耗时2m20s

                    Log.e(TAG, "onClick_readbinlen: "+length);
                    Log.e(TAG, "onClick_packbinlen: "+binLenth);

                    fin.close();
                }
                catch(Exception e){
                    e.printStackTrace();
                }
            }
      });
    }

    //UART service connected/disconnected
    private ServiceConnection mServiceConnection = new ServiceConnection()
    {
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

    private Handler mHandler = new Handler()
    {
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

            final Intent mIntent = intent;
            //*********************//
            if (action.equals (UartService.ACTION_GATT_CONNECTED) )
            {
                runOnUiThread (new Runnable()
                {
                    public void run()
                    {
                        String currentDateTimeString = DateFormat.getTimeInstance().format (new Date() );
                        Log.d (TAG, "UART_CONNECT_MSG");
                        btnConnectDisconnect.setText ("Disconnect");
                        //  edtMessage.setEnabled(true);
                        btnSend.setEnabled (true);
                        upSend.setEnabled(true);
                        SureButton.setEnabled(true);
                        restcmdButton.setEnabled(true);
                        oldcmdButton.setEnabled(true);
                        quercmdbutton.setEnabled(true);
                        deoldcmdButton.setEnabled(true);
                        ( (TextView) findViewById (R.id.deviceName) ).setText (mDevice.getName() + " - ready");
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
                        String currentDateTimeString = DateFormat.getTimeInstance().format (new Date() );
                        Log.d (TAG, "UART_DISCONNECT_MSG");
                        btnConnectDisconnect.setText ("Connect");
                        // edtMessage.setEnabled(false);
                        btnSend.setEnabled (false);
                        upSend.setEnabled(false);
                        SureButton.setEnabled(false);
                        restcmdButton.setEnabled(false);
                        quercmdbutton.setEnabled(false);
                        oldcmdButton.setEnabled(false);
                        deoldcmdButton.setEnabled(false);
                        ( (TextView) findViewById (R.id.deviceName) ).setText ("Not Connected");
                        listAdapter.add ("[" + currentDateTimeString + "] Disconnected to: " + mDevice.getName() );
                        mState = UART_PROFILE_DISCONNECTED;
                        mService.close();
                        //setUiState();

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
                runOnUiThread (new Runnable()
                {
                    public void run()
                    {
                        try
                        {

                            String text = "";

                            if(packSendFlag==true){

                                if(rxValue[0]==0x06){
                                    packSendFlag=false;
                                     text = "PACK:OK "+String.valueOf(sendpackg);
                                }else{
                                     text = "PACK:FAIL"+ String.valueOf(sendpackg);
                                }
                            }
                            else {
                                 text = new String(rxValue, "UTF-8");
                            }

                            String veiwtext =  bytes2HexString(rxValue);

                            String currentDateTimeString = DateFormat.getTimeInstance().format (new Date() );

                           // Log.e (TAG, "rxValue:" + text);

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
        switch (requestCode)
        {

        case REQUEST_SELECT_DEVICE:
            //When the DeviceListActivity return, with the selected device address
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
        default:
            Log.e (TAG, "wrong request code");
            break;
        }
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
