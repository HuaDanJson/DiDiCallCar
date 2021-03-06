


package com.tantuo.didicar.DriverLicenseNFC;

import java.io.BufferedReader;

import android.app.AlertDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

import android.nfc.NfcAdapter;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.SoundPool;
import android.net.ConnectivityManager;

import org.json.JSONException;
import org.json.JSONObject;

import com.baidu.location.LocationClient;
import com.google.gson.Gson;
import com.tantuo.didicar.Bean.DriverBean;
import com.tantuo.didicar.MainActivity;
import com.tantuo.didicar.R;
import com.tantuo.didicar.utils.MD5JniUtils;
import com.tantuo.didicar.utils.NfcUtils;

import android.media.AudioManager;

import com.tantuo.didicar.utils.LogUtil;
import com.tantuo.didicar.utils.WebDetailActivityUtils;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class DriverRFIDMainActivity extends AppCompatActivity implements OnClickListener, OnMenuItemClickListener {
    private TextView ifo_NFC, ifo_NFCID;
    private NfcAdapter nfcAdapter;
    private String readResult = "", address = "", province = "", userid = "";
    private String city = "", district = "", street = "", streetNumber = "";
    static public String brand = "", producedDate = "", leaveFactoryDate = "", retailSeller = "", productInformation = "";
    private String producedLocation = "";
    private double longitude, latitude;
    private Float radius;
    static public String CardId = "";
    private PendingIntent pendingIntent;
    private String[][] mTechLists;
    private boolean isFirst = true;
    private Button poiBtn;
    private IntentFilter ndef;
    private IntentFilter[] mFilters;
    public String ip = "";
    private IntentFilter tagDetected;
    int result;
    int returnResult = 0;
    private static final String TAG = "Author:tantuo";
    private TextView mText;
    private TextView mTextPoi;
    private LocationClient mLocationClient = null;
    private ImageView ib_titlebar_back;
    private ImageButton popUpMenu;
    private SoundPool soundPool;
    private Vibrator vibrator;
    private ConnectivityManager manager;
    private pl.droidsonroids.gif.GifImageView gifImageView;
    private static final String URL = "http://139.199.37.235/LBS/check_userid_back_driver_info.php";
    private String iv_bottom_sheet_item_url2 = "https://dpubstatic.udache.com/static/dpubimg/dpub2_project_187481/index_187481.html?TripCountry=CN&access_key_id=2&appid=10000&appversion=5.2.52&area=%E5%8C%97%E4%BA%AC%E5%B8%82&channel=780&city_id=1&cityid=1&datatype=1&deviceid=6cd1d3832da36056681ad4ed7ade2155&dviceid=6cd1d3832da36056681ad4ed7ade2155&flat=40.39293&flng=116.84192&imei=868227037142403854C78AD10B66380C8F28CC6327C3788&lang=zh-CN&lat=40.392355381081394&lng=116.8424214994192&location_cityid=1&location_country=CN&maptype=soso&model=HWI-AL00&origin_id=1&os=9&phone=W471piXc0R0glRFq7nvDow&pid=1_xID-B2_hV&platform=1&susig=e4f80d8df39b46ae679cb58d721db&suuid=A1702CD0DD1175EDF286DE35369DF4CA_780&terminal_id=1&time=1560742235707&trip_cityId=1&trip_cityid=1&trip_country=CN&uid=281867467423745&utc_offset=480&uuid=A0AF094F9D975FBBAE7AD129E96CF26F&";
    public DriverBean driverbean;
    private Button button;
    private TextView textView;
    private ImageView iv_bottom_image;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.rfid_main_activity);


        checkNetworkState();

        checkPhoneNfcsupport();

        //nfc初始化设置
        NfcUtils nfcUtils = new NfcUtils(this);

        initView();    //主界面的初始化工作

    }

    private void checkPhoneNfcsupport() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (nfcAdapter == null) {

            Toast.makeText(DriverRFIDMainActivity.this, "设备不支持NFC！", Toast.LENGTH_LONG).show();
            notSupportNFC();


        }

        if (nfcAdapter != null && !nfcAdapter.isEnabled()) {
            Toast.makeText(DriverRFIDMainActivity.this, "请在系统设置中先启用NFC功能！", Toast.LENGTH_LONG).show();
            setOpenNFC();
        }
    }

    public void playsound(int sound) {
        SoundPool soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 1);
        soundPool.load(this, sound, 1);
        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int i, int i2) {
                soundPool.play(1,  //声音id
                        1, //左声道
                        1, //右声道
                        1, //优先级
                        0, // 0表示不循环，-1表示循环播放
                        1);//播放比率，0.5~2，一般为1
            }
        });
    }


    private void initView() {

        playsound(R.raw.positive);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }


        popUpMenu = (ImageButton) findViewById(R.id.btnPopUpMenu);
        popUpMenu.setOnClickListener(this);

        iv_bottom_image = (ImageView) findViewById(R.id.iv_bottom_sheet_item2);
        iv_bottom_image.setOnClickListener(this);


        ib_titlebar_back = (ImageView) findViewById(R.id.ib_titlebar_back);
        ib_titlebar_back.setOnClickListener(this);


    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    @Override
    protected void onResume() {
        super.onResume();
        // 前台分发系统,用于第二次检测NFC标签时该应用有最高的捕获优先权.
        NfcUtils.mNfcAdapter.enableForegroundDispatch(this, NfcUtils.mPendingIntent, NfcUtils.mIntentFilter, NfcUtils.mTechList);
    }


    @Override
    protected void onPause() {
        super.onPause();
        //关闭前台调度系统
        NfcUtils.mNfcAdapter.disableForegroundDispatch(this);
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        SoundPool soundPool = new SoundPool(21, AudioManager.STREAM_SYSTEM, 10);
        soundPool.load(this, R.raw.positive, 1);
        soundPool.play(1, 1, 1, 0, 0, 1);


        Vibrator vibrator = (Vibrator) getSystemService(this.VIBRATOR_SERVICE);
        vibrator.vibrate(500);

        CardId = "not reach to processIntent";
        try {
            CardId = NfcUtils.readNFCId(intent);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Log.e("log_tag", "Tag读出成功,TagID:" + CardId);
        } else {
            Log.e("log_tag", "on new intent Tag读出失败 ACTION_TAG_DISCOVERED ！equals(intent.getAction())),TagID:" + CardId);
        }

        new GetDataTask().execute();


    }


    @Override
    public void onClick(View v) {


        switch (v.getId()) {
            case R.id.btnPopUpMenu: // 说明点击了微信按钮
                //创建弹出式菜单对象（最低版本11）
                PopupMenu popup = new PopupMenu(this, v);//第二个参数是绑定的那个view
                //获取菜单填充器
                MenuInflater inflater = popup.getMenuInflater();
                //填充菜单
                inflater.inflate(R.menu.popup_menu, popup.getMenu());
                //绑定菜单项的点击事件
                popup.setOnMenuItemClickListener(this);
                //显示(这一行代码不要忘记了)
                popup.show();
                break;

            case R.id.iv_bottom_sheet_item2:
                WebDetailActivityUtils.start_DiDi_info_Activity(DriverRFIDMainActivity.this, iv_bottom_sheet_item_url2);
                break;


            case R.id.ib_titlebar_back:
                finish();
                break;


            default:
                break;
        }


    }

    //弹出式菜单的单击事件处理
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        // TODO Auto-generated method stub
        switch (item.getItemId()) {
            case R.id.item1:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("帮助");
                builder.setMessage("请将您的手机背面靠近司机的识别证件\n\n");
                builder.setPositiveButton("确定", null);
                builder.create().show();
                break;


            default:
                break;
        }
        return false;
    }


    private boolean checkNetworkState() {
        boolean flag = false;
        //得到网络连接信息
        manager = (ConnectivityManager) getSystemService(this.CONNECTIVITY_SERVICE);
        //去进行判断网络是否连接
        if (manager.getActiveNetworkInfo() != null) {
            flag = manager.getActiveNetworkInfo().isAvailable();
        }
        if (!flag) {
            setNetwork();
        }

        return flag;
    }

    private void setNetwork() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("网络设置提示").setMessage("查询需要连接网络，请连接wifi或者移动网络，是否去设置?").setPositiveButton("设置", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                Intent intent = null;
                //判断手机系统的版本  即API大于10 就是3.0或以上版本
                if (android.os.Build.VERSION.SDK_INT > 10) {
                    intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                } else {
                    intent = new Intent();
                    ComponentName component = new ComponentName("com.android.settings", "com.android.settings.WirelessSettings");
                    intent.setComponent(component);
                    intent.setAction("android.intent.action.VIEW");
                }
                startActivity(intent);
            }
        }).setNegativeButton("取消", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        }).show();

    }


    private void setOpenNFC() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("网络设置提示").setMessage("查询需要设置打开手机NFC功能，是否去设置?").setPositiveButton("去设置", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                Intent intent = null;
                intent = new Intent(Settings.ACTION_NFC_SETTINGS);
                startActivity(intent);

            }
        }).setNegativeButton("取消", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        }).show();

    }


    private void notSupportNFC() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LogUtil.i("进入类:DriverRFIDMainActivity, 方法:notSupportNFC()  ");
        builder.setTitle("设备不支持提示").setMessage("很遗憾，您的手机不支持NFC功能").setPositiveButton("我知道了", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                dialog.dismiss();
                finish();
            }
        }).setNegativeButton("取消", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        }).show();

    }

    /**
     * okhttp网络请求部分，使用手机贴近司机的RFID识别芯片，手机读出其中的唯一号码，并将ID发送到我再腾讯云上的服务器端
     * 服务器端会对发送过来的UID号码与数据库进行比对，如果的确是在平台注册过的司机和车辆，会把司机信息和车辆的信息发送给乘客
     * RFID技术广泛用于银行卡和身份识别安全领域，每个RFID芯片ID全球唯一，可以比较好的解决打车过程中一直存在的"人车不一致"问题
     * author
     */
    private class GetDataTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            String result = null;
            OkHttpClient client = new OkHttpClient();
            //这里也可以直接包成GSON发给服务器端，因为参数比较少我直接把RFID号码放到请求体body里面直接发动服务器
            FormBody.Builder builder = new FormBody.Builder();

            //把从滴滴司机RFID识别到的唯一ID发到服务器验证
            builder.add("uid", MD5JniUtils.getMd5(CardId));


            //这里给大家提供一个已经在数据库里注册过的我自己的司机ID，让大家看一下从数据库中返回的真实信息
            builder.add("uid", MD5JniUtils.getMd5("4F96BFDD"));




            //把验证当时的地理位置信息也发送给平台或监管机构，可以给后台的数据分析提供支持
            builder.add("current_location", (MainActivity.startlocation).toString());
            RequestBody body = builder.build();
            Request request = new Request.Builder()
                    .url(URL)
                    .post(body)
                    .build();
            Response response = null;
            try {
                response = client.newCall(request).execute();
                result = response.body().string();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }

        /**
         * 处理从服务器端返回回来的Json数据，里面包含了status字段和司机，车辆的信息
         * status字段表示服务器端查询uid的结果，为0代表滴滴平台数据库中没有此RFID的信息，也就是未能正常注册的驾驶员
         * RFID号码大于0则代表这个RFID信息在平台被查询的次数
         * 从服务器端返回来的数据会被打包成GSON 被 Intent传递到下一个 activity，显示滴滴官方验证的结果和驾驶员，车辆的信息和照片
         * 从服务器请求到数据传递到下一个显示结果的activity有几种方法，可以用Serializable 和 Parcelable序列化，郭霖和徐宜生的Android教程里都有介绍
         * activity间用intent传递数据我这里用的是GSON,记得在app gradle里导GSON包 implementation 'com.google.code.gson:gson:2.8.0'
         *
         * @param
         */
        @Override
        protected void onPostExecute(String s) {


            try {
                JSONObject jsonObject = new JSONObject(s);
                result = jsonObject.getInt("status");
                Gson gson = new Gson();
                driverbean = (DriverBean) gson.fromJson(s, DriverBean.class);


                if (result >= 1) {

                    LogUtil.i("进入类:GetDataTask, 方法:onPostExecute() result >1 ");
                    Intent intent = new Intent(DriverRFIDMainActivity.this, Check_success_activity.class);
                    intent.putExtra("driverJson", new Gson().toJson(driverbean));
                    startActivity(intent);

                } else if (result == -1) {

                    LogUtil.i("进入类:GetDataTask, 方法:onPostExecute() result == -1 ");
                    Intent intent = new Intent(DriverRFIDMainActivity.this, Check_failed_activity.class);
                    startActivity(intent);

                } else if (result == 0) {
                    LogUtil.i("进入类:GetDataTask, 方法:onPostExecute() result == 0 ");

                }
            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(DriverRFIDMainActivity.this, "new run thered failed", Toast.LENGTH_SHORT).show();
            }
            super.onPostExecute(s);
        }
    }


}
