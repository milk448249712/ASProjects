package com.unnkai.location_test;
import android.content.Context;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.SQLException;
import java.net.HttpURLConnection;
import java.net.URL;
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
}
