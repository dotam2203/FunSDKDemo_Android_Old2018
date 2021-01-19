package com.lib.sdk.bean;

import com.lib.funsdk.support.utils.MyUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MotionDetectIPC {

    public static final String RET = "Ret";
    public static final String NAME = "Name";
    public static final String LEVEL = "Level";
    public static final String REGION = "Region";
    public static final String ENABLE = "Enable";
    public static final String SESSIONID = "SessionID";
    public static final String EVENTHANDLER = "EventHandler";
    public static final String MESSAGEENABLE = "MessageEnable";
    public static final String RECORDENABLE = "RecordEnable";
    public static final String RECORDMASK = "RecordMask";
    public static final String SNAPENABLE = "SnapEnable";
    public static final String SNAPSHOTMASK = "SnapShotMask";

    public static final String TIP_ENABLE = "TipEnable";
    public static final String VOICE_ENABLE = "VoiceEnable";
    public static final String VOICE_TYPE = "VoiceType";

    private boolean mEnable;
    private boolean mMessage;
    private boolean mSnap;
    private boolean mRecord;
    private int mLevel = -1;
    private String mRecordMask;
    private String mSnapShotMask;
    private String mName;
    private JSONObject mEventHandler;
    private JSONArray mRegion;

    private boolean mTipEnable;
    private boolean mVoiceEnable;
    private int mVoiceType;

    public boolean isTipEnable() {
        return mTipEnable;
    }

    public void setTipEnable(boolean tipEnable) {
        this.mTipEnable = tipEnable;
    }

    public boolean isVoiceEnable() {
        return mVoiceEnable;
    }

    public void setVoiceEnable(boolean voiceEnable) {
        this.mVoiceEnable = voiceEnable;
    }

    public int getVoiceType() {
        return mVoiceType;
    }

    public void setVoiceType(int voiceType) {
        this.mVoiceType = voiceType;
    }

    public boolean onParse(String json, String name, int channel) {
        try {
            JSONObject obj = new JSONObject(json);
            mName = MyUtils.getKey(name, channel);
            JSONObject c_obj = obj.optJSONObject(mName);
            if (c_obj != null) {
                if (c_obj.has(LEVEL)) {
                    mLevel = c_obj.getInt(LEVEL);
                }
                if (c_obj.has(REGION)) {
                    mRegion = c_obj.optJSONArray(REGION);
                }
                if (c_obj.has(ENABLE)) {
                    mEnable = c_obj.getBoolean(ENABLE);
                }
                if (c_obj.has(EVENTHANDLER)) {
                    mEventHandler = c_obj.optJSONObject(EVENTHANDLER);
                    if (mEventHandler.has(TIP_ENABLE)) {
                        mTipEnable = mEventHandler.getBoolean(TIP_ENABLE);
                    }
                    if (mEventHandler.has(VOICE_ENABLE)) {
                        mVoiceEnable = mEventHandler.getBoolean(VOICE_ENABLE);
                    }
                    if (mEventHandler.has(VOICE_TYPE)) {
                        mVoiceType = mEventHandler.getInt(VOICE_TYPE);
                    }
                    return true;
                } else {
                    return false;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    public String getSendMsg() {
        JSONObject jsonObj = new JSONObject();
        try {
            jsonObj.put(NAME, mName);
            jsonObj.put(SESSIONID, "0x0000000A");
            JSONObject c_jsonObj = new JSONObject();
            if (mEventHandler != null) {
                mEventHandler.put(TIP_ENABLE, mTipEnable);
                mEventHandler.put(VOICE_ENABLE, mVoiceEnable);
                mEventHandler.put(VOICE_TYPE, mVoiceType);
            }
            c_jsonObj.put(EVENTHANDLER, mEventHandler);
            c_jsonObj.put(ENABLE, mEnable);
            if (mLevel != -1) {
                c_jsonObj.put(LEVEL, mLevel);
            }
            if (mRegion != null) {
                c_jsonObj.put(REGION, mRegion);
            }
            jsonObj.put(mName, c_jsonObj);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return jsonObj.toString();
    }
}
