package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private TextView server_ip_kj,server_port_kj,textView_frequency,text_view_img_size;
    private Button connect_btn_kj,send_btn_kj,send_btn_kj2,img_disp_btn,img_send_btn;
    private EditText editTextF1, editTextF2, editTextF3, editTextF4, editTextRb, editTextVpp,send_data_kj,editText_JPG;
    private RadioGroup radioGroup;
    private ImageView imageView;
    private Socket socket;
    InputStream inputStream;
    OutputStream outputStream;
    MyHandle myHandle;
    int[] f_1234;
    byte[] byteArray;
    boolean able_to_transmit;
    private static final int REQUEST_CODE_PICK_IMAGE=1;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        server_ip_kj = findViewById(R.id.server_ip_kj);
        //server_port_kj = findViewById(R.id.server_port_kj);
        connect_btn_kj = findViewById(R.id.connect_btn_kj);
        send_btn_kj = findViewById(R.id.send_btn_kj);
        send_btn_kj2 = findViewById(R.id.send_btn_kj2);
        send_data_kj = findViewById(R.id.send_data_kj);
        img_disp_btn = findViewById(R.id.img_disp_btn);
        img_send_btn = findViewById(R.id.img_send_btn);
        text_view_img_size=findViewById(R.id.text_view_img_size);
        imageView = findViewById(R.id.imageView);
        editTextRb = findViewById(R.id.editTextRb);
        editTextVpp = findViewById(R.id.editTextVpp);
        editText_JPG= findViewById(R.id.editText_JPG);
        textView_frequency=findViewById(R.id.textView_frequency);
        radioGroup = findViewById(R.id.radioGroup);
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

        //连接服务器
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
                            socket = new Socket("192.168.4.1",1122);//注：ip是string变量，端口是int变量
                            //socket = new Socket(server_ip_kj.getText().toString(),Integer.valueOf(server_port_kj.getText().toString()));
                            if(socket.isConnected()){
                                //连接成功
                                outputStream = socket.getOutputStream();
                                inputStream = socket.getInputStream();
                                Log.i("Connect","OK");
                                Message msg = new Message();
                                msg.what = 0;
                                msg.obj = "Connection successful!";
                                myHandle.sendMessage(msg);
                                btn_renew();
                                //RecvData();
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
        //发送控制信号
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
                String Vpp = editTextVpp.getText().toString();
// 补零操作
                f1 = String.format("%04d", Integer.valueOf(f1));
                f2 = String.format("%04d", Integer.valueOf(f2));
                f3 = String.format("%04d", Integer.valueOf(f3));
                f4 = String.format("%04d", Integer.valueOf(f4));
                Rb = String.format("%03d", Integer.valueOf(Rb));
                Vpp = String.format("%02d", Integer.valueOf(Vpp));
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

                result += Rb + ",";
                result += Vpp ;
                String message_send=result;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //向byte数组msg的最后添加'\n'

                            byte[] msg = message_send.getBytes();
//                            msg数组的最后添加'\n'结束标志 存在tmp数组中
                            byte[] tmp = new byte[msg.length +4];
                            for(int i=0;i<msg.length;i++){
                                tmp[i] = msg[i];
                            }
                            tmp[msg.length] = '\r';
                            tmp[msg.length+1] = '\n';
                            tmp[msg.length+2] = '\r';
                            tmp[msg.length+3] = '\n';
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
        //发送信息信号
        send_btn_kj2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(TextUtils.isEmpty(send_data_kj.getText().toString()))
                {
                    //输入框为空，提示禁止发送空字符(发送空字符会导致esp32假死)
                    Toast.makeText(MainActivity.this, "Can not send empty message!", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                //向byte数组msg的最后添加'\n'

                                byte[] msg = send_data_kj.getText().toString().getBytes();
//                            msg数组的最后添加'\n'结束标志 存在tmp数组中
                                byte[] tmp = new byte[msg.length +6];
                                tmp[0]='0';
                                tmp[1]=',';
                                for(int i=2;i<msg.length+2;i++)
                                {
                                    tmp[i] = msg[i-2];
                                }
                                tmp[msg.length+2] = '\r';
                                tmp[msg.length+3] = '\n';
                                tmp[msg.length+4] = '\r';
                                tmp[msg.length+5] = '\n';

                                outputStream.write(tmp);
                                Log.i("sendmsg","OK");
                            } catch (IOException e) {
                                Log.i("sendmsg","Fail");
                                throw new RuntimeException(e);
                            }
                        }
                    }).start();
                }
            }
        });
        //选择图片
        img_disp_btn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                // 这里是点击后要执行的代码
                Toast.makeText(MainActivity.this, "Begin to select image", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
            }
        });
        //发送图片
        img_send_btn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            //向服务器发送byte数组byteArray
                            byte[] sending_array = new byte[byteArray.length +6];
                            sending_array[0]='0';
                            sending_array[1]=',';
                            for(int i=2;i<byteArray.length+2;i++)
                            {
                                sending_array[i] = byteArray[i-2];
                            }
                            sending_array[byteArray.length+2] = '\r';
                            sending_array[byteArray.length+3] = '\n';
                            sending_array[byteArray.length+4] = '\r';
                            sending_array[byteArray.length+5] = '\n';
                            outputStream.write(sending_array);


                            //在子线程更新ui，这样的方法是必要的
                            myHandle.post(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    //将byte类型的数组解压缩为图像并显示
                                    Bitmap bitmap_decode = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                                    imageView.setImageBitmap(bitmap_decode);
                                    Toast.makeText(MainActivity.this, "Image has been sent out", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        catch(IOException e)
                        {
                            Log.i("img_send","Fail");
                            throw new RuntimeException(e);
                        }
                    }
                }).start();
            }
        });
    }
    public class MyHandle extends Handler
    {
        @Override
        public void handleMessage(@NonNull Message msg)
        {
            super.handleMessage(msg);
            switch (msg.what){
                case 0:
                    Toast.makeText(MainActivity.this,msg.obj.toString(),Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    Toast.makeText(MainActivity.this,msg.obj.toString(),Toast.LENGTH_SHORT).show();
                    break;
                case 2:

                    Toast.makeText(MainActivity.this,msg.obj.toString(),Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }

    }
    private void btn_renew()
    {
        //这个方法专门为了在子线程更新ui而写……
        myHandle.post(new Runnable()
        {
            @Override
            public void run()
            {
                //启用四个按钮控件
                send_btn_kj.setEnabled(true);
                send_btn_kj2.setEnabled(true);
                img_disp_btn.setEnabled(true);
            }
        });
    }
    // 在子线程更新Spinner控件选项的字符串的方法
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
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        //选择图片界面，点击确定以后执行以下代码块
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == RESULT_OK && data != null)
        {
            Uri selectedImageUri = data.getData();
            try
            {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                // 假设已经在activity_main.xml中定义了一个ImageView控件来显示图片
                //imageView = findViewById(R.id.imageView);
                // 然后，我们将图片转换为字节流
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, Integer.valueOf(editText_JPG.getText().toString()), stream);//通过jpeg压缩，将图像编码为byte类型的数组
                byteArray = stream.toByteArray();
                int byte_length=byteArray.length;
                text_view_img_size.setText(String.valueOf(byte_length)+"B");

                //判断传输上限，具体大小可更改
                if(byte_length>32000)
                {
                    able_to_transmit=false;
                    img_send_btn.setEnabled(false);
                    imageView.setImageBitmap(null);
                    Toast.makeText(MainActivity.this, "The size of the selected image is too big,ESP32 refuse to send", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    able_to_transmit=true;
                    img_send_btn.setEnabled(true);
                    Bitmap bitmap_decode = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                    imageView.setImageBitmap(bitmap_decode);
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

}