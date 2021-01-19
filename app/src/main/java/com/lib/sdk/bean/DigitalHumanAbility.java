package com.lib.sdk.bean;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DigitalHumanAbility implements Serializable {
    private boolean HumanDection; //人形检测
    private boolean SupportAlarmVoiceTips; //报警声
    private boolean SupportAlarmLinkLight; //报警灯
    private boolean SupportAlarmVoiceTipsType; //报警声类型

    public boolean isHumanDection() {
        return HumanDection;
    }

    public boolean isSupportAlarmVoiceTips() {
        return SupportAlarmVoiceTips;
    }

    public boolean isSupportAlarmLinkLight() {
        return SupportAlarmLinkLight;
    }

    public boolean isSupportAlarmVoiceTipsType() {
        return SupportAlarmVoiceTipsType;
    }

    public boolean onParse(String json, String name, int channel) {
        try {
            JSONObject jsonObject = new JSONObject(json);
            JSONObject object = jsonObject.optJSONObject(getKey(name, channel));
            if (object != null) {
                if (object.has("HumanDection")) {
                    HumanDection = object.getBoolean("HumanDection");
                }
                if (object.has("SupportAlarmVoiceTips")) {
                    SupportAlarmVoiceTips = object.getBoolean("SupportAlarmVoiceTips");
                }
                if (object.has("SupportAlarmLinkLight")) {
                    SupportAlarmLinkLight = object.getBoolean("SupportAlarmLinkLight");
                }
                if (object.has("SupportAlarmVoiceTipsType")) {
                    SupportAlarmVoiceTipsType = object.getBoolean("SupportAlarmVoiceTipsType");
                }
                return true;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String getKey(String name, int channel) {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(".").append("[")
                .append(channel).append("]");
        return sb.toString();
    }

    private List<DigitalHumanAbility> mDigitalHumanAbilityList;

    public List<DigitalHumanAbility> getDigitalHumanAbilityList() {
        return mDigitalHumanAbilityList;
    }

    public boolean onParseArray(String json, String name) {
        try {
            JSONObject jsonObject = new JSONObject(json);
            JSONArray object = jsonObject.optJSONArray(name);
            if (object != null) {
                mDigitalHumanAbilityList = new ArrayList<>();
                for (int i = 0; i < object.length(); i++) {
                    DigitalHumanAbility ability = new DigitalHumanAbility();
                    //数组中可能返回null，所以增加是否为对象的判断
                    if (object.get(i) instanceof JSONObject) {
                        JSONObject obj = (JSONObject) object.get(i);
                        if (obj.has("HumanDection")) {
                            ability.HumanDection = obj.getBoolean("HumanDection");
                        }
                        if (obj.has("SupportAlarmVoiceTips")) {
                            ability.SupportAlarmVoiceTips = obj.getBoolean("SupportAlarmVoiceTips");
                        }
                        if (obj.has("SupportAlarmLinkLight")) {
                            ability.SupportAlarmLinkLight = obj.getBoolean("SupportAlarmLinkLight");
                        }
                        if (obj.has("SupportAlarmVoiceTipsType")) {
                            ability.SupportAlarmVoiceTipsType = obj.getBoolean("SupportAlarmVoiceTipsType");
                        }
                    }
                    mDigitalHumanAbilityList.add(ability);
                }
                return true;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }
}
