package com.unnkai.location_test;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
/**
 * Created by Administrator on 2017/12/13.
 * http://blog.csdn.net/qian_xiao_lj/article/details/52163184
 */

public class gsm_location {


    /*基站信息结构体 */
    public class SCell {
        public int MCC;
        public int MNC;
        public int LAC;
        public int CID;
    }
    /**经纬度信息结构体 */
    public class SItude{
        public String
                latitude;
        public String
                longitude;
    }

    /**
     * 获取基站信息
     *
     * @throws Exception
     */
    private SCell getCellInfo() throws Exception {
        SCell cell = new SCell();

        /**
         调用API获取基站信息 */
        TelephonyManager
                mTelNet = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        GsmCellLocation
                location = (GsmCellLocation) mTelNet.getCellLocation();
        if (location == null)
            throw new Exception("获取基站信息失败");

        String operator = mTelNet.getNetworkOperator();
        int mcc = Integer.parseInt(operator.substring(0, 3));
        int mnc = Integer.parseInt(operator.substring(3));
        int cid = location.getCid();
        int lac = location.getLac();

        /**
         将获得的数据放到结构体中 */
        cell.MCC = mcc;
        cell.MNC = mnc;
        cell.LAC  = lac;
        cell.CID = cid;

        return cell;
    }
}