package com.lib.sdk.bean;

import android.os.Parcel;
import android.os.Parcelable;

import com.lib.funsdk.support.utils.MyUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AbilityVoiceTip {
    public static final String ABILITY_VOICE_TIP_TYPE = "Ability.VoiceTipType";
    public static final String VOICE_TIP = "VoiceTip";
    public static final String VOICE_ENUM = "VoiceEnum";
    public static final String VOICE_TEXT = "VoiceText";

    public List<VoiceTip> voiceTipList;

    public static class VoiceTip implements Parcelable {
        public int VoiceEnum;
        public String VoiceText;
        public boolean selected;

        public VoiceTip() {
        }

        protected VoiceTip(Parcel in) {
            VoiceEnum = in.readInt();
            VoiceText = in.readString();
            selected = in.readByte() != 0;
        }

        public static final Creator<VoiceTip> CREATOR = new Creator<VoiceTip>() {
            @Override
            public VoiceTip createFromParcel(Parcel in) {
                return new VoiceTip(in);
            }

            @Override
            public VoiceTip[] newArray(int size) {
                return new VoiceTip[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(VoiceEnum);
            dest.writeString(VoiceText);
            dest.writeByte((byte) (selected ? 1 : 0));
        }
    }

    public boolean onParse(String json, String name, int channel) {
        if (json == null || json.length() <= 0) {
            return false;
        }
        try {
            JSONObject jsonObject = new JSONObject(json);
            JSONObject object = jsonObject.optJSONObject(MyUtils.getKey(name, channel));
            if (object != null) {
                if (object.has(VOICE_TIP)) {
                    voiceTipList = new ArrayList<>();
                    JSONArray jsonArray = object.optJSONArray(VOICE_TIP);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject object1 = (JSONObject) jsonArray.get(i);
                        VoiceTip voiceTip = new VoiceTip();
                        voiceTip.VoiceEnum = object1.getInt(VOICE_ENUM);
                        voiceTip.VoiceText = object1.getString(VOICE_TEXT);
                        voiceTipList.add(voiceTip);
                    }
                    return true;
                }
            } else {
                return false;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean onParse(String json) {
        if (json == null || json.length() <= 0) {
            return false;
        }
        try {
            JSONObject jsonObject = new JSONObject(json);
            JSONObject object = jsonObject.optJSONObject(ABILITY_VOICE_TIP_TYPE);
            if (object != null) {
                if (object.has(VOICE_TIP)) {
                    voiceTipList = new ArrayList<>();
                    JSONArray jsonArray = object.optJSONArray(VOICE_TIP);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject object1 = (JSONObject) jsonArray.get(i);
                        VoiceTip voiceTip = new VoiceTip();
                        voiceTip.VoiceEnum = object1.getInt(VOICE_ENUM);
                        voiceTip.VoiceText = object1.getString(VOICE_TEXT);
                        voiceTipList.add(voiceTip);
                    }
                    return true;
                }
            } else {
                return false;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }
}
