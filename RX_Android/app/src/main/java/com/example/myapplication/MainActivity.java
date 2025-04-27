package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.graphics.drawable.BitmapDrawable;

public class MainActivity extends AppCompatActivity {
    private TextView server_ip_kj,server_port_kj,recv_data_kj,textView_frequency,lastTextView;
    private Button connect_btn_kj,send_btn_kj,send_btn_kj2,clearButton,save_img_button,copy_button;
    private EditText editTextRb, editTextVpp,send_data_kj;
    private RadioGroup radioGroup;
    private LinearLayout linearLayout;
    private ScrollView scrollView;
    private Socket socket;
    private ImageView imageView;
    InputStream inputStream;
    OutputStream outputStream;
    MyHandle myHandle;
    int[] f_1234;
    char receive_mode='n';//状态变量，a表示上一个接收到字符标志“zf,”;b表示上一个接收到图像标志“tp,”;n表示上一个接收到其他
    byte[] str_expected={122,102,44};//字符信息标志“zf,”
    byte[] img_expected={116,112,44};//图像信息标志“tp,”
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        server_ip_kj = findViewById(R.id.server_ip_kj);
        connect_btn_kj = findViewById(R.id.connect_btn_kj);
        send_btn_kj = findViewById(R.id.send_btn_kj);
        editTextRb = findViewById(R.id.editTextRb);
        radioGroup = findViewById(R.id.radioGroup);
        textView_frequency=findViewById(R.id.textView_frequency);
        recv_data_kj = findViewById(R.id.recv_data_kj);
        linearLayout = findViewById(R.id.linearLayout);
        scrollView = findViewById(R.id.scrollView);
        imageView = findViewById(R.id.imageView);
        clearButton = findViewById(R.id.clear_button);
        save_img_button = findViewById(R.id.save_img_button);
        copy_button = findViewById(R.id.copy_button);
        lastTextView = null;
        myHandle = new MyHandle();

        //Spinner控件代码
        Spinner spinner = findViewById(R.id.spinner);
        String[] options = {"1385,1467,1554,1695", "1289,1450,1628,1847"};
        updateSpinnerItems(options);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // 获取选中项的数据
                String selectedItem = parent.getItemAtPosition(position).toString();
                // 处理选中事件
                if(position==0)
                {
                    Toast.makeText(MainActivity.this, "Frequency group 1 selected", Toast.LENGTH_SHORT).show();
                    f_1234= new int[]{1385, 1467, 1554, 1695};
                }
                else if(position==1)
                {
                    Toast.makeText(MainActivity.this, "Frequency group 2 selected", Toast.LENGTH_SHORT).show();
                    f_1234= new int[]{1289, 1450, 1628, 1847};
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 无选项被选中时的处理
            }
        });

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(checkedId==R.id.radioButton4FSK) {
                    //选择4FSK
                    myHandle.post(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            textView_frequency.setText("f1,f2,f3,f4(Hz):");
                            updateSpinnerItems(new String[]{"1385,1467,1554,1695", "1289,1450,1628,1847"});
                        }
                    });
                }
                else if(checkedId==R.id.radioButton2FSK){
                    //2fsk，两个频率无效
                    myHandle.post(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            textView_frequency.setText("f1,f4(Hz):");
                            updateSpinnerItems(new String[]{"1385,1695", "1289,1847"});
                        }
                    });
                }
            }
        });
//        连接服务器
        connect_btn_kj.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (socket != null && socket.isConnected())
                            {
                                // 如果已经有一个活动的连接，那么我们先关闭这个连接
                                socket.close();
                            }
                            socket = new Socket("192.168.4.1",1122);
                            if(socket.isConnected()){
                                //连接成功
                                outputStream = socket.getOutputStream();
                                inputStream = socket.getInputStream();
                                Log.i("Connect","OK");
                                Message msg = new Message();
                                msg.what = 0;
                                msg.obj = "Connection successful";
                                myHandle.sendMessage(msg);
                                myHandle.post(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        //启用按钮控件
                                        send_btn_kj.setEnabled(true);
                                    }
                                });
                                RecvData();
                            }else{
                                Message msg = new Message();
                                msg.what = 1;
                                msg.obj = "Connection fail!";
                                myHandle.sendMessage(msg);
                                //连接失败
                                Log.i("Connect","Fail");
                            }
                        } catch (IOException e) {
                            //连接失败
                            Message msg = new Message();
                            msg.what = 1;
                            msg.obj = "Connection fail!";
                            myHandle.sendMessage(msg);
                            Log.i("Connect","Fail");
                            //throw new RuntimeException(e);
                        }
                    }
                }).start();
            }
        });
//        发送控制信息
        send_btn_kj.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int selectedId = radioGroup.getCheckedRadioButtonId();
                RadioButton radioButton = findViewById(selectedId);
                String modulationType = radioButton.getText().toString();
                // 初始化

                String f1 = String.valueOf(f_1234[0]);
                String f2 = String.valueOf(f_1234[1]);
                String f3 = String.valueOf(f_1234[2]);
                String f4 = String.valueOf(f_1234[3]);
                String Rb = editTextRb.getText().toString();
                //String Vpp = editTextVpp.getText().toString();
// 补零操作
                f1 = String.format("%04d", Integer.valueOf(f1));
                f2 = String.format("%04d", Integer.valueOf(f2));
                f3 = String.format("%04d", Integer.valueOf(f3));
                f4 = String.format("%04d", Integer.valueOf(f4));
                Rb = String.format("%03d", Integer.valueOf(Rb));
                //Vpp = String.format("%02d", Integer.valueOf(Vpp));
                String result;
                if (modulationType.equals("4FSK")) {
                    result = "1,";
                    result += f1 + ",";
                    result += f2 + ",";
                    result += f3 + ",";
                    result += f4 + ",";
                } else {
                    result = "2,";
                    result += f1 + ",";
                    result += f4 + ",";
                }

                //result += Rb + ",";
                result += Rb;
                //result += Vpp ;//不需要帧长信息
                String message_send=result;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //向byte数组msg的最后添加'\n'

                            byte[] msg = message_send.getBytes();
//                            msg数组的最后添加'\n'结束标志 存在tmp数组中
                            byte[] tmp = new byte[msg.length +2];
                            for(int i=0;i<msg.length;i++){
                                tmp[i] = msg[i];
                            }
                            tmp[msg.length] = '\r';
                            tmp[msg.length+1] = '\n';
                            outputStream.write(tmp);
                            Log.i("sendmsg","OK");
                        } catch (IOException e) {
                            Log.i("sendmsg","Fail");
                            throw new RuntimeException(e);
                        }
                    }
                }).start();
            }
        });
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 清空TextView里的文本信息和ImageView里的图像信息
                linearLayout.removeAllViews();
                imageView.setImageBitmap(null);
            }
        });
        save_img_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ImageView imageView = findViewById(R.id.imageView);
                Drawable drawable = imageView.getDrawable();
                if (drawable != null && drawable instanceof BitmapDrawable) {
                    BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
                    Bitmap bitmap = bitmapDrawable.getBitmap();
                    if (bitmap != null) {
                        saveImageToGallery(bitmap);
                    } else {
                        // 弹窗提示
                        Toast.makeText(MainActivity.this, "Current image area is null", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // 弹窗提示
                    Toast.makeText(MainActivity.this, "Current image area is null", Toast.LENGTH_SHORT).show();
                }
            }
        });
        copy_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (lastTextView != null) {
                    // 获取TextView中的文本
                    CharSequence textToCopy = lastTextView.getText();

                    // 获取剪贴板管理器
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    // 创建ClipData对象
                    ClipData clip = ClipData.newPlainText("copied_text", textToCopy);
                    // 将ClipData内容设置到系统剪贴板上
                    clipboard.setPrimaryClip(clip);

                    // 可选：显示文本已复制的提示
                    Toast.makeText(MainActivity.this, "Message has copied to clipboard", Toast.LENGTH_SHORT).show();
                } else {
                    // 可选：如果没有文本可以复制，显示提示
                    Toast.makeText(MainActivity.this, "No message can be copied", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    private void saveImageToGallery(Bitmap bitmap) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, "image_" + System.currentTimeMillis());
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            Toast.makeText(this, "image has saved to gallery", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "fail to save image", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void RecvData(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(socket != null && socket.isConnected() == true){
                    byte[] recv_buff = new byte[32000];
                    try {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();

                        Message msg = new Message();
                        msg.what = 2;
                        long startTime = System.currentTimeMillis();
                        boolean foundEnd = false;
                        while (!foundEnd) {
                            int len = inputStream.read(recv_buff);
                             if (len > 0) {
                                baos.write(recv_buff, 0, len);
                            } else {
                                foundEnd=true;
                            }
                            try {
                                Thread.sleep(1000); // 等待 2 秒
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            byte[] tmp2 = baos.toByteArray();
                            if (tmp2[tmp2.length-2] == '\r' && tmp2[tmp2.length-1] == '\n') {
                                foundEnd=true;
                            }
                        }

                        byte[] tmp1 = baos.toByteArray();
                        int len = tmp1.length;
                        Log.i("RecvDataLength", String.valueOf(len)); // len是接收到的字符个数
                        Log.i("RecvData", new String(tmp1));
                        if (tmp1.length > 0) {

                            if (tmp1[0] == 'z' && tmp1[1] == 'f' && tmp1[2] == ',') {
                                myHandle.sendMessage(msg);
                                byte[] byteArray2 = new byte[len];
                                System.arraycopy(tmp1, 3, byteArray2, 0, len-3);
                                msg.obj = new String(byteArray2);
                            } else if (tmp1[0] == 't' && tmp1[1] == 'p' && tmp1[2] == ',') {
                                byte[] byteArray = new byte[len];
                                System.arraycopy(tmp1, 3, byteArray, 0, len-3);
                                Bitmap bitmap_decode = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length - 2);
                                //在子线程更新ui，这样的方法是必要的
                                myHandle.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        imageView.setImageBitmap(bitmap_decode);
                                        Toast.makeText(MainActivity.this, "Received an image", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } else {
                                if (len < 100) {
                                    myHandle.sendMessage(msg);
                                    msg.obj = new String(tmp1);
                                } else if (len >= 100) {
                                    byte[] byteArray = new byte[len];
                                    System.arraycopy(tmp1, 0, byteArray, 0, len);
                                    Bitmap bitmap_decode = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length - 2);
                                    //在子线程更新ui，这样的方法是必要的
                                    myHandle.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            imageView.setImageBitmap(bitmap_decode);
                                            Toast.makeText(MainActivity.this, "Received an image", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            }
                        }
                    }
                    catch (IOException e)
                    {
                        //throw new RuntimeException(e);
                        myHandle.post(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                //启用按钮控件
                                send_btn_kj.setEnabled(false);
                            }
                        });
                        break;
                    }
                }
            }
        }).start();
    }
    public class MyHandle extends Handler
    {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 0:
                    Toast.makeText(MainActivity.this,msg.obj.toString(),Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    Toast.makeText(MainActivity.this,msg.obj.toString(),Toast.LENGTH_SHORT).show();
                    break;
                case 2:
                    //recv_data_kj.setText(msg.obj.toString());
                    if (msg.obj != null)
                    {
                        displayMessage(msg.obj.toString());
                        //Toast.makeText(MainActivity.this,msg.obj.toString(),Toast.LENGTH_SHORT).show();
                    }
                    break;
                default:
                    break;
            }
        }
    }
    private void displayMessage(String message) {
        // 创建一个新的 TextView 来显示消息
        TextView textView = new TextView(this);
        textView.setText(message);

        // 将 TextView 添加到 LinearLayout 中
        linearLayout.addView(textView);

        // 更新最后一个 TextView 的引用
        lastTextView = textView;

        // 滚动 ScrollView 到最底部
        linearLayout.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    protected void onDestroy(){
        super.onDestroy();
        try {
            socket.close();
            inputStream.close();
            outputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
    private void updateSpinnerItems(final String[] items)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                ArrayAdapter<String> newAdapter = new ArrayAdapter<>(MainActivity.this,
                        android.R.layout.simple_spinner_item, items);
                newAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                Spinner spinner = findViewById(R.id.spinner);
                spinner.setAdapter(newAdapter);
            }
        });
    }
}