package com.unnkai.location_test;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import org.apache.http.util.EncodingUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;


public class locInfoFile extends AppCompatActivity {
    File m_file;
    String m_filename;
    FileOutputStream m_outputStream;
    Context mainCtx;
    locInfoFile(String filename,Context ctx) {
        m_filename = filename;
        mainCtx = ctx;
        Log.d("file",m_filename);
        try {
            m_file = new File(mainCtx.getFilesDir().getPath().toString()+"/"+m_filename);
            Log.d("file",m_file.getPath());
            m_outputStream = null;
            try {
                m_outputStream = new FileOutputStream(m_file );
            } catch (FileNotFoundException e) {
                Log.e("file", "FileNotFoundException",e);
            }
        } catch (Exception e) {
            Log.e("file", "ioexc",e);
            e.printStackTrace();
        }
    }
    protected void destroy() {
        try {
            Log.d("file", "delete the file object...");
            m_outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void writeInerFile(String buffer) {
        try {
            m_outputStream.write(buffer.getBytes());
            m_outputStream.flush();
            Log.d("file", "write file...");
        } catch (Exception e) {
            Log.e("file", "Exception",e);
            e.printStackTrace();
        }
    }
    public String readInerFile() {
        try {
            FileInputStream fin = mainCtx.openFileInput(m_filename);
            int length = fin.available();
            byte [] buffer = new byte[length];
            fin.read(buffer);
            String res = EncodingUtils.getString(buffer, "UTF-8");
            fin.close();
            return res;
        } catch (Exception e) {
            Log.d("file", "FileNotFoundException readfile",e);
            return "error\n";
        }
    }
}
