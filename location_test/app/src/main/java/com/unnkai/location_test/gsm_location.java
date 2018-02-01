package com.unnkai.location_test;
import android.content.Context;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.SQLException;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
/**
 * Created by Administrator on 2017/12/13.
 * http://blog.csdn.net/qian_xiao_lj/article/details/52163184
 */

//  a funny name to http
public class gsm_location {

    public String get(String strURL, String getData){
        HttpURLConnection connection=null;
        String response = "";
        URL url= null;
        try {
            url = new URL(strURL);
            connection=(HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            InputStream in=connection.getInputStream();
            //下面对获取到的输入流进行读取
            BufferedReader bufr=new BufferedReader(new InputStreamReader(in));
            //StringBuilder response=new StringBuilder();

            String line=null;
            while((line=bufr.readLine())!=null){
                // response.append(line);
                response += line;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    public String post(String strURL, String getData){
        String response = "";
        HttpURLConnection connection=null;
        URL url= null;
        try {
            url = new URL(strURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setRequestProperty("Charset", "utf-8");
            // 设置容许输出
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            OutputStream outStrm = connection.getOutputStream();
            ObjectOutputStream objOutputStrm = new ObjectOutputStream(outStrm);
            // writeObject should be a json object
            // objOutputStrm.writeObject(new String("我是测试数据"));
            //String sendData = "test我是测47548aaaa试数据hhh";
            //sendData = URLEncoder.encode(sendData,"utf-8");
            //byte[] byteArray = sendData.getBytes();
            JSONObject locJson = new JSONObject();
            try {
                locJson.put("lat",123.456);
                locJson.put("addr","北京");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            String sendData = locJson.toString();

            objOutputStrm.writeUTF(sendData);
            objOutputStrm.flush();
            objOutputStrm.close();

            // 将内存缓冲区中封装好的完整的HTTP请求电文发送到服务端。
            InputStream inStrm = connection.getInputStream(); // <===注意，实际发送请求的代码段就在这里
            BufferedReader bufr=new BufferedReader(new InputStreamReader(inStrm));
            String line=null;
            while((line=bufr.readLine())!=null){
                // response.append(line);
                response += line;
            }
        }catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }
}
