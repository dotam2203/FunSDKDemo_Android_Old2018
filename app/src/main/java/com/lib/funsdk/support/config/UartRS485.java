package com.lib.funsdk.support.config;

import com.lib.funsdk.support.FunLog;

import org.json.JSONException;
import org.json.JSONObject;

public class UartRS485 extends BaseConfig {

    /**
     *  以下CONFIG_NAME的定义必须要有,是为了保持所有的配置可以统一解析
     */
    public static final String CONFIG_NAME = JsonConfig.UART_RS485;

    @Override
    public String getConfigName() {
        return CONFIG_NAME;
    }

    public String getConfigNameOfChannel() {
        return Config_Name_ofchannel;
    }

    @Override
    public boolean onParse(String json) {
        if (!super.onParse(json))
            return false;
        try {
            Config_Name_ofchannel = mJsonObj.getString("Name");
            JSONObject jsonObject = mJsonObj.getJSONObject(Config_Name_ofchannel);
            return json != null && jsonObject.has("Attribute");
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public String getSendMsg() {
       super.getSendMsg();
        try {
            mJsonObj.put("Name", getConfigNameOfChannel());
            mJsonObj.put("SessionID", "0x00001234");

        } catch (JSONException e1) {
            e1.printStackTrace();
        }
        FunLog.d(getConfigNameOfChannel(), "json:" + mJsonObj.toString());
        return mJsonObj.toString();
    }
}
