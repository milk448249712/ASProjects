package com.unnkai.location_test;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class locInfoFile extends AppCompatActivity {
    File m_file;
    String m_filename;
    FileOutputStream m_outputStream;
    locInfoFile(String filename) {
        m_filename = filename;
        try {
            m_file = new File(m_filename);
            // Make sure log file is exists
            if (!m_file.exists()) {
                Log.d("file", "file not exsit...");
                boolean result; // 文件是否创建成功
                try {
                    result = m_file.createNewFile();
                } catch (IOException e) {
                    Log.d("file", "ioexc",e);
                    e.printStackTrace();
                    return;
                }
                if (!result) {
                    Log.d("file", "file create failed...");
                    return;
                }
            }
            m_outputStream = openFileOutput(m_filename, this.MODE_PRIVATE);
        } catch (Exception e) {
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
    public void writeFile(String buffer) {
        try {
            Log.d("file", "write...");
            m_outputStream.write(buffer.getBytes());
            m_outputStream.flush();
            m_outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
