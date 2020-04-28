package com.unnkai.location_test;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Handler;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.Criteria;
import android.support.v4.app.ActivityCompat;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import android.provider.Settings;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.LinkedList;

// https://www.cnblogs.com/android-blogs/p/5718479.html
// http://blog.csdn.net/u013334392/article/details/52459635

public class MainActivity extends AppCompatActivity {
    public final static String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";
    public String deviceModel = Build.MODEL;
    public String brand = Build.BRAND;
    // public String display = Build.DISPLAY;
    //public String serial = "";
    public String deviceId = deviceModel + "-" + brand;

    private EditText editText;
    private EditText editText_log;
    private TextView tx_backend_log;
    private TextView tx_loc_listener;
    private LocationManager lm;
    private static final String TAG = "GpsActivity";
    private int cnt = 0;
    private int listen_cnt = 0;
    private int getLocInThreadCnt = 0;
    private long lastGpsTime = 0;
    private locInfoFile latlnLog = null;
    gsm_location getHttp = new gsm_location();
    // final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private Location nowLocation = null;
    LinkedList<Location> locList = new LinkedList<Location>();

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {//这里只加入了读写和相机权限，还可以加入其他权限
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    //private Handler handler=null;
    //在handler中更新UI
    /*private Handler mHandler = new Handler(){
        public void handleMessage(Message msg) {
            Location newLoc = (Location)msg.obj;
            editText.setText(cnt+"设备位置信息\n\n经度：");
            String locType = "";
            SimpleDateFormat formatter   =   new   SimpleDateFormat   ("yyyy-MM-dd HH:mm:ss");
            Date curDate =  new Date(System.currentTimeMillis());
            String strDate = formatter.format(curDate);
            if (newLoc!=null && newLoc.getTime()!=lastGpsTime) {
                editText.append(String.valueOf(newLoc.getLongitude()));
                editText.append("\n纬度：");
                editText.append(String.valueOf(newLoc.getLatitude()));
                editText.append("\ndata from gps");
                lastGpsTime = newLoc.getTime();
                String strAdde = getPositionByGeocoder(newLoc);
                editText.append("\n\n地址："+strAdde);
                locType = "gps";
            } else {
                editText.setText(cnt+"设备位置信息\n\n经度：");
                editText.append(String.valueOf(newLoc.getLongitude()));
                editText.append("\n纬度：");
                editText.append(String.valueOf(newLoc.getLatitude()));
                editText.append("\ndata from network");
                String strAdde = getPositionByGeocoder(newLoc);
                editText.append("\n\n地址："+strAdde);
                locType = "net";
            }
            tx_backend_log.setText("lastGpsTime:"+lastGpsTime);
            String buf2File = String.valueOf(newLoc.getLongitude())+","+String.valueOf(newLoc.getLatitude()+","+locType+","+strDate);
            Log.d("file",buf2File);
            //connectServerWithTCPSocket("test123");
            latlnLog.writeInerFile(buf2File+"\n");
            if (cnt%3 == 0) {
                Log.d("file",latlnLog.readInerFile());
            }
            cnt++;
        };
    };*/
    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        //lm.removeUpdates(locationListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        latlnLog = new locInfoFile("location_test_log.txt", this);
        editText = (EditText) findViewById(R.id.editText);
        editText_log = (EditText) findViewById(R.id.editText_log);
        tx_backend_log = (TextView) findViewById(R.id.tx_backend_log);
        tx_loc_listener = (TextView) findViewById(R.id.tx_loc_listener);
        /*try {
            serial = Build.class.getField("SERIAL").get(null).toString(); //厂商分配的序列号
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }*/
        deviceId += "-" + Build.PRODUCT;
        verifyStoragePermissions(this);
        try {
            deviceId = URLEncoder.encode(deviceId, "UTF-8");
            deviceId = URLEncoder.encode(deviceId, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, String.valueOf(e));
            e.printStackTrace();
        }
        Log.d("httpStr", deviceId);
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (true) {
            new Thread(networkTask).start();
        }
        // 判断GPS是否正常启动
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // 用户不需要感知
            Toast.makeText(this, "[test]GPS location not open...", Toast.LENGTH_SHORT).show();
            editText_log.setText("GPS location not open.\n");
            // 返回开启GPS导航设置界面
            // Intent intent_gps_set = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            // startActivityForResult(intent_gps_set, 0);
            //return;
        }
        // 为获取地理位置信息时设置查询条件
        String bestProvider = lm.getBestProvider(getCriteria(), true);
        editText_log.append("bestProvider:" + bestProvider + "\n");
        // 获取位置信息r.GPS_PROVIDER
        if (!checkLocationFinePermission()) {
        // 如果不设置查询要求，getLastKnownLocation方法传人的参数为LocationManage
            Toast.makeText(this, "gps permission check failed...", Toast.LENGTH_SHORT).show();
            if (!checkLocationCoarsePermission()) {
                Toast.makeText(this, "gps and BS permission all check failed...", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        LocLogs tmpLoclog = new LocLogs();
        Location location = getBestLocation(lm, tmpLoclog);
        updateView(location);
        // 监听状态
        // lm.addGpsStatusListener(listener);
        // 绑定监听，有4个参数
        // 参数1，设备：有GPS_PROVIDER和NETWORK_PROVIDER两种
        // 参数2，位置信息更新周期，单位毫秒
        // 参数3，位置变化最小距离：当位置距离变化超过此值时，将更新位置信息
        // 参数4，监听
        // 备注：参数2和3，如果参数3不为0，则以参数3为准；参数3为0，则通过时间来定时更新；两者为0，则随时刷新
        // 1秒更新一次，或最小位移变化超过1米更新一次；
        // 注意：此处更新准确度非常低，推荐在service里面启动一个Thread，在run中sleep(10000);然后执行handler.sendMessage(),更新位置
        // lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10*1000, 0, locationListener);
        lm.requestLocationUpdates(bestProvider, 10 * 1000, 0.0f, locationListener);
        latlnLog.writeInerFile("红鸡公尾巴红。。。\n");
        String locInof = latlnLog.readInerFile();
    }

    Handler handlerSocket = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            locInfoDetail newLoc = (locInfoDetail) msg.obj;

            tx_backend_log.setText(null);
            // tx_backend_log.append(newLoc.loclog.log_tx_backend);
            // upload location data
            // String httpStr = getHttp.get("http://24059ef2.nat123.net/transmit.php",newLoc);
            String httpStr = newLoc.httpResp;
            tx_backend_log.append("\nreq to LHS. resp:" + httpStr);  // location handle service
        }
    };
    /*Thread httpThread = new Thread() {
        @Override
        public void run() {
            super.run();
            //初始化Looper,一定要写在Handler初始化之前
            Looper.prepare();
            //在子线程内部初始化handler即可，发送消息的代码可在主线程任意地方发送
            handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    //所有的事情处理完成后要退出looper，即终止Looper循环
                    //这两个方法都可以，有关这两个方法的区别自行寻找答案
                    handler.getLooper().quit();
                    handler.getLooper().quitSafely();
                }
            };

            //启动Looper循环，否则Handler无法收到消息
            Looper.loop();
        }
    };*/

    Runnable networkTask = new Runnable() {
        @Override
        public void run() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            while(true) {
                /*if (!checkLocationFinePermission()) {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }
                Location newLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                Message message = new Message();
                LocLogs llog = new LocLogs();
                newLoc = getBestLocation(lm, llog);
                locInfoDetail stlocInfoTmp = parseLocToUI(newLoc);
                stlocInfoTmp.loclog = llog;
                stlocInfoTmp.deviceId = deviceId;
                String httpStr = getHttp.get("http://24059ef2.nat123.net/transmit.php", stlocInfoTmp);
                stlocInfoTmp.httpResp = httpStr;
                message.obj = stlocInfoTmp;*/
                // tx_backend_log.setText("networkTask cnt:"+ cnt++ + "\n");

                // Location tmpLoc = nowLocation;
                // nowLocation = null;
                Location tmpLoc = locList.poll();
                if (tmpLoc!=null) {
                    // tx_backend_log.append("networkTask cnt:"+ cnt + " loc not null\n");
                    // Toast.makeText(getApplicationContext(), "networkTask cnt:"+ cnt + " loc not null", Toast.LENGTH_SHORT).show();
                    locInfoDetail stlocInfoTmp = parseLocToUI(tmpLoc);
                    String httpStr = getHttp.get("http://24059ef2.nat123.net/transmit.php", stlocInfoTmp);
                    Message message = new Message();
                    cnt++;
                    stlocInfoTmp.httpResp = "cnt:[" + String.valueOf(cnt) + "]" + httpStr;
                    message.obj = stlocInfoTmp;
                    handlerSocket.sendMessage(message);
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        /*Handler handlerHttp = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                locInfoDetail newLoc = (locInfoDetail) msg.obj;
                String httpStr = getHttp.get("http://24059ef2.nat123.net/transmit.php",newLoc);
                //String httpStr = newLoc.httpResp;
                tx_backend_log.append("\nreq to LHS. resp:" + httpStr);  // location handle service
            }
        };*/
    };

    private boolean checkLocationFinePermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return false;
        } else {
            return true;
        }
    }
    private boolean checkLocationCoarsePermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return false;
        } else {
            return true;
        }
    }

    // 位置监听
    private LocationListener locationListener = new LocationListener() {
        /** 位置信息变化时触发*/
        @Override
        public void onLocationChanged(Location location) {
            /*Log.i(TAG, "onLocationChanged");
            Toast.makeText(getApplicationContext(), "on location changed...", Toast.LENGTH_SHORT).show();
            Log.i(TAG, "时间：" + location.getTime());
            Log.i(TAG, "经度：" + location.getLongitude());
            Log.i(TAG, "纬度：" + location.getLatitude());
            Log.i(TAG, "海拔：" + location.getAltitude());
            tx_loc_listener.setText(null);
            tx_loc_listener.append("[" + listen_cnt++ + "]" + "on location changend：");
            tx_loc_listener.append("时间：" + location.getTime());
            tx_loc_listener.append(",经度：" + location.getLongitude());
            tx_loc_listener.append(",纬度：" + location.getLatitude());
            tx_loc_listener.append(",海拔：" + location.getAltitude());*/
            updateView(location);

            // String httpStr = getHttp.get("http://24059ef2.nat123.net/transmit.php",stlocInfoTmp);
            // stlocInfoTmp.httpResp = httpStr;
            // nowLocation = location;
            locList.offer(location);
        }
        /**GPS状态变化时触发*/
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.i(TAG, "onStatusChanged");
            tx_loc_listener.setText(null);
            switch (status) {
                // GPS状态为可见时
                case LocationProvider.AVAILABLE:
                    Log.i(TAG, "当前GPS状态为可见状态");
                    tx_loc_listener.append("[" + listen_cnt++ + "]" + "on status changend AVAIABLE.provider：" + provider);
                    break;
                // GPS状态为服务区外时
                case LocationProvider.OUT_OF_SERVICE:
                    Log.i(TAG, "当前GPS状态为服务区外状态");
                    tx_loc_listener.append("[" + listen_cnt++ + "]" + "on status changend OUT_OF_SERVICE.provider：" + provider);
                    break;
                // GPS状态为暂停服务时
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    Log.i(TAG, "当前GPS状态为暂停服务状态");
                    tx_loc_listener.append("[" + listen_cnt++ + "]" + "on status changend TEMPORARILY_UNAVAILABLE.provider：" + provider);
                    break;
            }
        }
        /** GPS开启时触发*/
        @Override
        public void onProviderEnabled(String provider) {
            Log.i(TAG, "onProviderEnabled");
            if(!checkLocationFinePermission()) {
                return;
            }
            Location location = lm.getLastKnownLocation(provider);
        }
        /*** GPS禁用时触发*/
        @Override
        public void onProviderDisabled(String provider) {
            updateView(null);
        }
    };

    /**实时更新文本内容* @param location*/
    private void updateView(Location location) {
        if (location != null) {
            long locTs = location.getTime();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date LocDate =  new Date(locTs);
            String strDate = formatter.format(LocDate);
            // 解析位置信息
            String strAddress= getPositionByGeocoder(location);

            editText.setText("设备["+deviceId+"]位置信息:\n\n经度：");
            editText.append(String.valueOf(location.getLongitude()));
            editText.append("\n纬度：");
            editText.append(String.valueOf(location.getLatitude()));
            editText.append("\n海拔：");
            editText.append(String.valueOf(location.getAltitude()));
            editText.append("\n获取该位置的时间：");
            editText.append(strDate);
            editText.append("\n"+String.valueOf(locTs));
            editText.append("\n具体位置信息：");
            editText.append("\n"+strAddress);
            editText.append("\n位置队列大小："+locList.size());
        } else {
            // 清空EditText对象
            editText.getEditableText().clear();
            editText.setText("location is null...");
        }
    }

    /*** 返回查询条件*/
    private Criteria getCriteria() {
        Criteria criteria = new Criteria();
        // 设置定位精确度 Criteria.ACCURACY_COARSE比较粗略，Criteria.ACCURACY_FINE则比较精细
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        // 设置是否要求速度
        criteria.setSpeedRequired(false);
        // 设置是否允许运营商收费
        criteria.setCostAllowed(false);
        // 设置是否需要方位信息
        criteria.setBearingRequired(false);
        // 设置是否需要海拔信息
        criteria.setAltitudeRequired(false);
        // 设置对电源的需求
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        return criteria;
    }
    private Location getBestLocation(LocationManager locationManager, LocLogs llog) {
        // gps, network, passive
        llog.log_tx_backend = "[" + getLocInThreadCnt++ + "]getBestLocation:\n";
        Location result = null;
        if (locationManager != null) {
            if(!checkLocationFinePermission()) {
                Toast.makeText(this, "gps permission check failed...", Toast.LENGTH_SHORT).show();
                llog.log_tx_backend = llog.log_tx_backend + "gps permission check failed...\n";
                return null;
            }
            result = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (result != null && result.getTime() != lastGpsTime) {
                llog.log_tx_backend = llog.log_tx_backend + "return gps location, time:"+result.getTime()+"\n";
                lastGpsTime = result.getTime();
                return result;
            } else {
                if(!checkLocationCoarsePermission()) {
                    Toast.makeText(this, "network permission check failed...", Toast.LENGTH_SHORT).show();
                    return null;
                }
                result = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (result != null ) {
                    llog.log_tx_backend = llog.log_tx_backend + "return network location, time:"+result.getTime()+"\n";
                    return result;
                } else {
                    result = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                    if (result != null) {
                        llog.log_tx_backend = llog.log_tx_backend + "return passive location, time:"+result.getTime()+"\n";
                        return result;
                    }
                }
            }
        }
        return result;
    }
    /*基站信息结构体 */
    public class SCell {
        public int MCC;
        public int MNC;
        public int LAC;
        public int CID;
    }
    /**经纬度信息结构体 */
    public class SItude{
        public String latitude;
        public String longitude;
    }

    private String getPositionByGeocoder(Location location) {
        String strPositon = "";
        String errorMessage = "";
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    // In this sample, get just a single address.
                    1);
        } catch (IOException ioException) {
            // Catch network or other I/O problems.
            errorMessage = "service_not_available";
            Log.e(TAG, errorMessage, ioException);
        } catch (IllegalArgumentException illegalArgumentException) {
            // Catch invalid latitude or longitude values.
            errorMessage = "invalid_lat_long_used";
            Log.e(TAG, errorMessage + ". " +
                    "Latitude = " + location.getLatitude() +
                    ", Longitude = " +
                    location.getLongitude(), illegalArgumentException);
        }
        // Handle case where no address was found.
        if (addresses == null || addresses.size()  == 0) {
            if (errorMessage.isEmpty()) {
                errorMessage = "no_address_found";
                Log.e(TAG, errorMessage);
            }
            return "";
        } else {
            Address address = addresses.get(0);
            ArrayList<String> addressFragments = new ArrayList<String>();
            // Fetch the address lines using getAddressLine,
            // join them, and send them to the thread.
            for(int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                addressFragments.add(address.getAddressLine(i));
            }
            Log.i(TAG, "address_found");
            if(addressFragments.size() > 0) {
                strPositon = addressFragments.get(0);
            }
        }
        return strPositon;
    }

    public class locInfoDetail{
        public double longitude;
        public double latitude;
        public String deviceId;
        public long locTime;
        public String httpResp;
        public String locType;
        public String strAddr;
        public String strDate;
        public LocLogs loclog;
    }

    private locInfoDetail parseLocToUI(Location newLoc) {
        locInfoDetail stLocInfoDetail = new locInfoDetail();
        String locType = "";
        SimpleDateFormat formatter   =   new   SimpleDateFormat   ("yyyy-MM-dd HH:mm:ss");
        Date curDate =  new Date(System.currentTimeMillis());
        String strDate = formatter.format(curDate);
        // !!! 第一次由于lastGpsTime 为初始值0，所以必定会用gps定位，可能会导致定位不准
        /*if (newLoc!=null && newLoc.getTime()!=lastGpsTime) {
            Log.d("gps1", newLoc.getTime()+","+String.valueOf(lastGpsTime));
            stLocInfoDetail.latitude = newLoc.getLatitude();
            stLocInfoDetail.longitude = newLoc.getLongitude();
            stLocInfoDetail.locType = "gps";
            stLocInfoDetail.strAddr = getPositionByGeocoder(newLoc);
            stLocInfoDetail.strDate = strDate;
            lastGpsTime = newLoc.getTime();
        } else {
            if (newLoc!=null) {
                Log.d("gps2", newLoc.getTime() + "," + String.valueOf(lastGpsTime));
                stLocInfoDetail.latitude = newLoc.getLatitude();
                stLocInfoDetail.longitude = newLoc.getLongitude();
                stLocInfoDetail.locType = "network";
                stLocInfoDetail.strAddr = getPositionByGeocoder(newLoc);
                stLocInfoDetail.strDate = strDate;
            } else {
                Log.d("obj null err:","get nullptr newLoc obj.");
            }
        }*/
        if (newLoc != null) {
            stLocInfoDetail.latitude = newLoc.getLatitude();
            stLocInfoDetail.longitude = newLoc.getLongitude();
            stLocInfoDetail.deviceId = deviceId;
            stLocInfoDetail.locTime = newLoc.getTime() / 1000;  // ms -> s
            stLocInfoDetail.locType = newLoc.getProvider();
            stLocInfoDetail.strAddr = getPositionByGeocoder(newLoc);
            stLocInfoDetail.strDate = strDate;
            lastGpsTime = newLoc.getTime();
        }
        return stLocInfoDetail;
    }

    private void connectServerWithTCPSocket(locInfoDetail locInfo) {
        Socket socket;
        try {// 创建一个Socket对象，并指定服务端的IP及端口号
            socket = new Socket("192.168.0.104", 6666);
            /** 或创建一个报文，使用BufferedWriter写入,看你的需求 **/
            String socketData = String.valueOf(locInfo.longitude)+";"+String.valueOf(locInfo.latitude)+";"+locInfo.strAddr+";"+locInfo.locType+";"+locInfo.strDate;
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),"gb2312"));
            writer.write(socketData);
            writer.flush();
        } catch (Exception e) {
            Log.e("socket error 1", String.valueOf(e),e);
            e.printStackTrace();
        }
    }

     public static void verifyStoragePermissions(Activity activity) {
        for (String per : PERMISSIONS_STORAGE) {
            int permission = ActivityCompat.checkSelfPermission(activity,
                    per);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,
                        REQUEST_EXTERNAL_STORAGE);
                break;
            }
        }
    }

    private class LocLogs{
        public String log_tx_backend;
        public String externed;
    }
}
