package com.unnkai.location_test;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.widget.EditText;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.Criteria;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;
import android.util.Log;
import android.provider.Settings;
/*import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;*/
import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.http.entity.StringEntity;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


// https://www.cnblogs.com/android-blogs/p/5718479.html
// http://blog.csdn.net/u013334392/article/details/52459635
public class MainActivity extends AppCompatActivity {
    public final static String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";

    private EditText editText;
    private LocationManager lm;
    private static final String TAG = "GpsActivity";
    private int cnt = 0;
    private long lastGpsTime = 0;
    private locInfoFile latlnLog = null;


    //private Handler handler=null;
    //在handler中更新UI
    private Handler mHandler = new Handler(){
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
                newLoc = getBestLocation(lm);
                editText.setText(cnt+"设备位置信息\n\n经度：");
                editText.append(String.valueOf(newLoc.getLongitude()));
                editText.append("\n纬度：");
                editText.append(String.valueOf(newLoc.getLatitude()));
                editText.append("\ndata from network");
                String strAdde = getPositionByGeocoder(newLoc);
                editText.append("\n\n地址："+strAdde);
                locType = "net";
            }
            String buf2File = String.valueOf(newLoc.getLongitude())+","+String.valueOf(newLoc.getLatitude()+","+locType+","+strDate);
            Log.d("file",buf2File);
            latlnLog.writeInerFile(buf2File+"\n");
            if (cnt%3 == 0) {
                Log.d("file",latlnLog.readInerFile());
            }
            cnt++;
        };
    };

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
        Log.i(TAG, "onCreate");
        latlnLog = new locInfoFile("location_test_log.txt",this);

        editText = (EditText) findViewById(R.id.editText);
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        MyQueryLocationThread myQueryThread = new MyQueryLocationThread();
        myQueryThread.start();


        // 判断GPS是否正常启动
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "请开启GPS导航...", Toast.LENGTH_SHORT).show();
            // 返回开启GPS导航设置界面
            Intent intent_gps_set = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(intent_gps_set, 0);
            return;
        }

        // 为获取地理位置信息时设置查询条件
        String bestProvider = lm.getBestProvider(getCriteria(), true);
        Toast.makeText(this, "bestProvider:"+bestProvider, Toast.LENGTH_SHORT).show();
        // 获取位置信息
        // 如果不设置查询要求，getLastKnownLocation方法传人的参数为LocationManager.GPS_PROVIDER
        if(!checkLocationFinePermission()) {
            Toast.makeText(this, "gps permission check failed...", Toast.LENGTH_SHORT).show();
            return;
        }
        // Location location = lm.getLastKnownLocation(bestProvider);
        Location location = getBestLocation(lm);
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

        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10*1000, 0, locationListener);
        latlnLog.writeInerFile("红鸡公尾巴红。。。\n");
        String locInof = latlnLog.readInerFile();
        Log.d("file",locInof);
    }

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

        /**
         * 位置信息变化时触发
         */
        @Override
        public void onLocationChanged(Location location) {
            Log.i(TAG, "onLocationChanged");
            Toast.makeText(getApplicationContext(), "on location changed...", Toast.LENGTH_SHORT).show();
            //updateView(location);
            Log.i(TAG, "时间：" + location.getTime());
            Log.i(TAG, "经度：" + location.getLongitude());
            Log.i(TAG, "纬度：" + location.getLatitude());
            Log.i(TAG, "海拔：" + location.getAltitude());
        }

        /**
         * GPS状态变化时触发
         */
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.i(TAG, "onStatusChanged");
            switch (status) {
                // GPS状态为可见时
                case LocationProvider.AVAILABLE:
                    Log.i(TAG, "当前GPS状态为可见状态");
                    break;
                // GPS状态为服务区外时
                case LocationProvider.OUT_OF_SERVICE:
                    Log.i(TAG, "当前GPS状态为服务区外状态");
                    break;
                // GPS状态为暂停服务时
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    Log.i(TAG, "当前GPS状态为暂停服务状态");
                    break;
            }
        }

        /**
         * GPS开启时触发
         */
        @Override
        public void onProviderEnabled(String provider) {
            Log.i(TAG, "onProviderEnabled");
            if(!checkLocationFinePermission()) {
                return;
            }
            Location location = lm.getLastKnownLocation(provider);
            updateView(location);
        }

        /**
         * GPS禁用时触发
         */
        @Override
        public void onProviderDisabled(String provider) {
            updateView(null);
        }

    };

    /**
     * 实时更新文本内容
     *
     * @param location
     */
    private void updateView(Location location) {
        if (location != null) {
            //Toast.makeText(this, "provider test123:"+LocationManager.GPS_PROVIDER, Toast.LENGTH_SHORT).show();
            editText.setText("设备位置信息\n\n经度：");
            editText.append(String.valueOf(location.getLongitude()));
            editText.append("\n纬度：");
            editText.append(String.valueOf(location.getLatitude()));
        } else {
            // 清空EditText对象
            editText.getEditableText().clear();
            editText.setText("location is null...");
            /*if (!lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                return;
            }
            if(!checkLocationCoarsePermission()) {
                Toast.makeText(this, "network permission check failed...", Toast.LENGTH_SHORT).show();
                return;
            }
            Location location_network = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location_network != null) {
                Toast.makeText(this, "provider net:"+LocationManager.NETWORK_PROVIDER, Toast.LENGTH_SHORT).show();
                editText.setText("设备位置信息\n\n经度：");
                editText.append(String.valueOf(location_network.getLongitude()));
                editText.append("\n纬度：");
                editText.append(String.valueOf(location_network.getLatitude()));
            }*/
        }
    }

    /**
     * 返回查询条件
     *
     * @return
     */
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
    private Location getBestLocation(LocationManager locationManager) {
        Location result = null;
        if (locationManager != null) {
            if(!checkLocationFinePermission()) {
                Toast.makeText(this, "gps permission check failed...", Toast.LENGTH_SHORT).show();
                return null;
            }
            result = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (result != null) {
                //Toast.makeText(this, "return gps location", Toast.LENGTH_SHORT).show();
                return result;
            } else {
                if(!checkLocationCoarsePermission()) {
                    Toast.makeText(this, "network permission check failed...", Toast.LENGTH_SHORT).show();
                    return null;
                }
                result = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                //Toast.makeText(this, "return network location", Toast.LENGTH_SHORT).show();
                return result;
            }
        }
        return result;
    }
    /** Called when the user clicks the Send button */
    /*public void sendMessage(View view) {
        // Do something in response to button
        Intent intent = new Intent(this, DisplayMessageActivity.class);
        EditText editText = (EditText) findViewById(R.id.edit_message);
        String message = editText.getText().toString();
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }*/

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
            /*int add_cnt = 0;
            for(String addr : addressFragments) {
                Toast.makeText(this,"geo pos "+add_cnt+":"+addr,Toast.LENGTH_SHORT).show();
            }*/
        }
        return strPositon;
    }

    public class MyQueryLocationThread extends Thread {
        /*MyQueryLocationThread(Context context) {
            m_mainContext = context;

        }
        Context m_mainContext;*/
        //继承Thread类，并改写其run方法
        private final static String TAG = "My Thread ===> ";
        public void run(){
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "run");
            for(int i = 0; i<10000; i++)
            {
                Log.e(TAG, Thread.currentThread().getName() + "run tread i =  " + i);
                if(!checkLocationFinePermission()) {
                    continue;
                }
                Location newLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                Message message = new Message();
                message.obj = newLoc;
                mHandler.sendMessage(message);
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

        }
    }
}
