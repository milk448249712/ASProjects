package com.unnkai.location_test;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.view.View;
import android.widget.EditText;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.Criteria;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import android.provider.Settings;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.http.entity.StringEntity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


// import com.google.android.gms.location.LocationServices;

// https://www.cnblogs.com/android-blogs/p/5718479.html
// http://blog.csdn.net/u013334392/article/details/52459635
public class MainActivity extends AppCompatActivity {
    public final static String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";

    private EditText editText;
    private LocationManager lm;
    private static final String TAG = "GpsActivity";
    private int cnt = 0;
    private long lastGpsTime = 0;
    //private Handler handler=null;
    //在handler中更新UI
    private Handler mHandler = new Handler(){
        public void handleMessage(Message msg) {
            Location newLoc = (Location)msg.obj;
            //editText.setText("你想变的内容:"+msg.obj);
            editText.setText(cnt+"设备位置信息\n\n经度：");
            if (newLoc!=null && newLoc.getTime()!=lastGpsTime) {
                editText.append(String.valueOf(newLoc.getLongitude()));
                editText.append("\n纬度：");
                editText.append(String.valueOf(newLoc.getLatitude()));
                editText.append("\nLocal is not null");
                lastGpsTime = newLoc.getTime();
                String strAdde = getPositionByGeocoder(newLoc);
                editText.append("\n\n地址："+strAdde);
            } else {
                newLoc = getBestLocation(lm);
                editText.setText(cnt+"设备位置信息\n\n经度：");
                editText.append(String.valueOf(newLoc.getLongitude()));
                editText.append("\n纬度：");
                editText.append(String.valueOf(newLoc.getLatitude()));
                editText.append("\ndata from network");
                String strAdde = getPositionByGeocoder(newLoc);
                editText.append("\n\n地址："+strAdde);
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

    /**
     * 获取基站信息
     * @throws Exception
     */
    private SCell getCellInfo() throws Exception {
        SCell cell = new SCell();
        /** 调用API获取基站信息 */
        TelephonyManager mTelNet = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        GsmCellLocation location = (GsmCellLocation) mTelNet.getCellLocation();
        if (location == null)
            throw new Exception("获取基站信息失败");
        String operator = mTelNet.getNetworkOperator();
        int mcc = Integer.parseInt(operator.substring(0, 3));
        int mnc = Integer.parseInt(operator.substring(3));
        int cid = location.getCid();
        int lac = location.getLac();
        /**将获得的数据放到结构体中 */
        cell.MCC = mcc;
        cell.MNC = mnc;
        cell.LAC  = lac;
        cell.CID = cid;
        return cell;
    }
    /**
     获取经纬度
     @throws Exception
     */
    private SItude getItude(SCell cell) throws Exception
    {
        SItude itude = new SItude();
        /** 采用Android默认的HttpClient */
        HttpClient client = new DefaultHttpClient();
        /** 采用POST方法 */
        HttpPost post = new HttpPost("http://www.google.com/loc/json");
        try {
            /** 构造POST的JSON数据 */
            JSONObject holder = new JSONObject();
            holder.put("version", "1.1.0");
            holder.put("host", "maps.google.com");
            holder.put("address_language", "zh_CN");
            holder.put("request_address", true);
            holder.put("radio_type", "gsm");
            holder.put("carrier",  "HTC");
            JSONObject tower = new JSONObject();
            tower.put("mobile_country_code", cell.MCC);
            tower.put("mobile_network_code", cell.MNC);
            tower.put("cell_id", cell.CID);
            tower.put("location_area_code", cell.LAC);
            JSONArray towerarray = new JSONArray();
            towerarray.put(tower);
            holder.put("cell_towers", towerarray);
            StringEntity query = new StringEntity(holder.toString());
            post.setEntity(query);
            /** 发出POST数据并获取返回数据 */
            HttpResponse response = client.execute(post);
            HttpEntity entity = response.getEntity();
            BufferedReader buffReader = new BufferedReader(new InputStreamReader(entity.getContent()));
            StringBuffer strBuff = new StringBuffer();
            String result = null;
            while ((result = buffReader.readLine()) != null)
            {
                strBuff.append(result);
            }
            /** 解析返回的JSON数据获得经纬度 */
            JSONObject json = new JSONObject(strBuff.toString());
            JSONObject subjosn = new JSONObject(json.getString("location"));
            itude.latitude = subjosn.getString("latitude");
            itude.longitude = subjosn.getString("longitude");
            Log.i("Itude", itude.latitude + itude.longitude);
        }
        catch (Exception e) {
            Log.e(e.getMessage(), e.toString());
            throw new Exception("获取经纬度出现错误:"+e.getMessage());
        }
        finally{
            post.abort();
            client = null;
        }
        return itude;
    }

    /*** 获取地理位置@throws Exception*/
    private String getLocation(SItude itude) throws Exception
    {
        String resultString = "";
        /** 这里采用get方法，直接将参数加到URL上 */
        // String urlString = String.format("http://maps.google.cn/maps/geo?key=abcdefg&q=%s,%s", itude.latitude, itude.longitude);
        String urlString = String.format("http://maps.google.cn/maps/api/geocode/json?latlng=%s,%s&sensor=true&language=zh-CN",itude.latitude, itude.longitude);
        Log.i("URL", urlString);
        /** 新建HttpClient */
        HttpClient client = new DefaultHttpClient();
        /** 采用GET方法 */
        HttpGet get = new HttpGet(urlString);
        try {
            /** 发起GET请求并获得返回数据 */
            HttpResponse response = client.execute(get);
            HttpEntity entity = response.getEntity();
            BufferedReader buffReader = new BufferedReader(new InputStreamReader(entity.getContent()));
            StringBuffer strBuff = new StringBuffer();
            String result = null;
            while ((result = buffReader.readLine()) != null)
            {
                strBuff.append(result);
            }
            resultString = strBuff.toString();
            /** 解析JSON数据，获得物理地址 */
            if (resultString != null && resultString.length() > 0)
            {
                JSONObject jsonobject = new JSONObject(resultString);
                JSONArray jsonArray = new JSONArray(jsonobject.get("Placemark").toString());
                resultString = "";
                for (int i = 0; i < jsonArray.length(); i++) {
                    resultString = jsonArray.getJSONObject(i).getString("address");
                }
            }
        }
        catch (Exception e) {
            throw new Exception("获取物理位置出现错误:" + e.getMessage());
        }
        finally {
            get.abort();
            client = null;
        }
        return resultString;
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
    /** 显示结果 */
    private void showResult(SCell cell, String location,EditText cellText) {
        // TextView cellText = (TextView) findViewById(R.id.editText);
        //cellText.setText(String.format("基站信息：mcc:%d, mnc:%d, lac:%d, cid:%d",
        //    cell.MCC, cell.MNC, cell.LAC, cell.CID));
        cellText.append(String.format("基站信息：mcc:%d, mnc:%d, lac:%d, cid:%d",
                cell.MCC, cell.MNC, cell.LAC, cell.CID));
        // TextView locationText = (TextView) findViewById(R.id.lacationText);
        cellText.append("\n物理位置：" + location);
    }
    private void getLocationFromGsm(EditText editText) {
        ProgressDialog mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("正在获取中...");
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.show();
        try {
            /** 获取基站数据 */
            SCell cell = getCellInfo();
            /** 根据基站数据获取经纬度 */
            SItude itude = getItude(cell);
            /** 获取地理位置 */
            String location = getLocation(itude);
            /** 显示结果 */
            showResult(cell, location,editText);
            /** 关闭对话框 */
            mProgressDialog.dismiss();
        }
        catch (Exception e) {
            /** 关闭对话框 */
            mProgressDialog.dismiss();
            /** 显示错误 */
            // TextView cellText = (TextView) findViewById(R.id.editText);
            editText.setText(e.getMessage());
            Log.e("Error", e.getMessage());
        }
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
