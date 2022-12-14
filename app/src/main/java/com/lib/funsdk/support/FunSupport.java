package com.lib.funsdk.support;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.basic.G;
import com.example.funsdkdemo.MyApplication;
import com.example.funsdkdemo.entity.DownloadInfo;
import com.example.funsdkdemo.utils.XUtils;
import com.lib.ECONFIG;
import com.lib.EDEV_ATTR;
import com.lib.EDEV_JSON_ID;
import com.lib.EDEV_OPTERATE;
import com.lib.EFUN_ATTR;
import com.lib.EUIMSG;
import com.lib.FunSDK;
import com.lib.IFunSDKResult;
import com.lib.Mps.MpsClient;
import com.lib.Mps.SMCInitInfo;
import com.lib.Mps.XPMS_SEARCH_ALARMINFO_REQ;
import com.lib.MsgContent;
import com.lib.SDKCONST;
import com.lib.SDKCONST.SDK_CommTypes;
import com.lib.funsdk.support.config.AlarmInfo;
import com.lib.funsdk.support.config.BaseConfig;
import com.lib.funsdk.support.config.DevCmdGeneral;
import com.lib.funsdk.support.config.DevCmdOPFileQueryJP;
import com.lib.funsdk.support.config.DevCmdOPRemoveFileJP;
import com.lib.funsdk.support.config.DevCmdOPSCalendar;
import com.lib.funsdk.support.config.DevCmdSearchFileNumJP;
import com.lib.funsdk.support.config.DeviceGetJson;
import com.lib.funsdk.support.config.OPCompressPic;
import com.lib.funsdk.support.config.SameDayPicInfo;
import com.lib.funsdk.support.config.SystemInfo;
import com.lib.funsdk.support.models.FunDevRecordFile;
import com.lib.funsdk.support.models.FunDevStatus;
import com.lib.funsdk.support.models.FunDevType;
import com.lib.funsdk.support.models.FunDevice;
import com.lib.funsdk.support.models.FunDeviceBuilder;
import com.lib.funsdk.support.models.FunDeviceSocket;
import com.lib.funsdk.support.models.FunLoginType;
import com.lib.funsdk.support.utils.DeviceWifiManager;
import com.lib.funsdk.support.utils.MyUtils;
import com.lib.funsdk.support.utils.SharedParamMng;
import com.lib.funsdk.support.utils.StringUtils;
import com.lib.sdk.bean.AlarmInfoBean;
import com.lib.sdk.bean.DSTimeBean;
import com.lib.sdk.bean.DayLightTimeBean;
import com.lib.sdk.bean.ElectCapacityBean;
import com.lib.sdk.bean.HandleConfigData;
import com.lib.sdk.bean.JsonConfig;
import com.lib.sdk.bean.LocationBean;
import com.lib.sdk.bean.TimeZoneBean;
import com.lib.sdk.struct.H264_DVR_FILE_DATA;
import com.lib.sdk.struct.H264_DVR_FINDINFO;
import com.lib.sdk.struct.ManualSnapModeJP;
import com.lib.sdk.struct.OPRemoveFileJP;
import com.lib.sdk.struct.SDBDeviceInfo;
import com.lib.sdk.struct.SDK_Authority;
import com.lib.sdk.struct.SDK_CONFIG_NET_COMMON_V2;
import com.lib.sdk.struct.SDK_ChannelNameConfigAll;
import com.lib.sdk.struct.SDK_SearchByTime;
import com.lib.sdk.struct.SDK_TitleDot;
import com.lib.sdk.struct.SInitParam;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static com.lib.EUIMSG.DEV_SLEEP;


public class FunSupport implements IFunSDKResult {

    private static final String TAG = "FunSupport";

    // ????????????,????????????????????????????????????????????????4?????????
    private static final String APP_UUID = "e0534f3240274897821a126be19b6d46";
    public static final String APP_KEY = "0621ef206a1d4cafbe0c5545c3882ea8";
    private static final String APP_SECRET = "90f8bc17be2a425db6068c749dee4f5d";
    private static final int APP_MOVECARD = 2;

    private static FunSupport mInstance = null;

    private Context mContext = null;
    private SharedParamMng mSharedParam = null;

    private final String SHARED_PARAM_KEY_AUTOLOGIN = "SHARED_PARAM_KEY_AUTOLOGIN";
    private final String SHARED_PARAM_KEY_SAVEPASSWORD = "SHARED_PARAM_KEY_SAVEPASSWORD";
    private final String SHARED_PARAM_KEY_SAVENATIVEPASSWORD = "SHARED_PARAM_KEY_SAVENATIVEPASSWORD";

    private int mFunUserHandler = -1;

    private String mLoginUserName = null; // ???????????????????????????
    private String mLoginPassword = null; // ???????????????????????????

    private String mTmpLoginUserName = null; // ??????????????????,????????????????????????
    private String mTmpLoginPassword = null; // ????????????,????????????????????????

    private boolean mSavePasswordAfterLogin; // ???????????????????????????
    private boolean mAutoLoginWhenStartup; // App???????????????????????????
    private boolean mSaveNativePassword;   //??????????????????????????????

    private String mLastMpsUserName = null; // ??????????????????????????????????????????????????????

    private FunLoginType mLoginType = null;

    private int mVerificationPassword = 2;
    public String NativeLoginPsw; //????????????

    // ??????????????????
    private List<FunDevice> mDeviceList = new ArrayList<FunDevice>();

    // ?????????????????????
    private List<FunDevice> mAPDeviceList = new ArrayList<FunDevice>();

    // ???????????????????????????
    private List<FunDevice> mLanDeviceList = new ArrayList<FunDevice>();

    // ??????????????????,????????????????????????????????????????????????
    private List<FunDevice> mTmpSNLoginDeviceList = new ArrayList<FunDevice>();

    // ????????????????????????????????????
    public FunDevice mCurrDevice = null;

    public AlarmInfoBean mAlarmInfoBean; //??????????????????info
    // ????????????????????????
    private final int MESSAGE_AP_DEVICE_LIST_CHANGED = 0x1000;
    private final int MESSAGE_GET_DEVICE_CONFIG = 0x1001;
    private final int MESSAGE_GET_DEVICE_CONFIG_TIMEOUT = 0x1002;

    private class DeviceGetConfig {
        FunDevice funDevice;
        String configName;
        int channelNo = -1;

        DeviceGetConfig(FunDevice dev, String cfg) {
            funDevice = dev;
            configName = cfg;
            channelNo = -1;
        }

        DeviceGetConfig(FunDevice dev, String cfg, int channel) {
            funDevice = dev;
            configName = cfg;
            channelNo = channel;
        }
    }

    private List<DeviceGetConfig> mDeviceGetConfigQueue = new ArrayList<DeviceGetConfig>();
    private boolean mIsGettingConfig = false;

    private FunSupport() {

    }

    private List<OnFunListener> mListeners = new ArrayList<OnFunListener>();


    public static synchronized FunSupport getInstance() {
        if (null == mInstance) {
            mInstance = new FunSupport();
        }

        return mInstance;
    }

    public void init(Context context) {
        int result = 0;

        mContext = context;

        // ???????????????
        FunPath.init(context, context.getPackageName());

        mSharedParam = new SharedParamMng(context);
        // ???????????????????????????
        loadParams();
        //p2p?????????ipv4???ipv6?????????????????????
        FunSDK.SetP2PRegionalScope(0);
        // ????????????1
        SInitParam param = new SInitParam();
        param.st_0_nAppType = SInitParam.LOGIN_TYPE_MOBILE;
        FunSDK.Init(0, G.ObjToBytes(param));
        // ???????????????P2P?????????
//        SInitParam param = new SInitParam();
//        param.st_0_nAppType = SInitParam.LOGIN_TYPE_MOBILE;
//        FunSDK.InitExV2(0, G.ObjToBytes(param),0,"","IP??????",8765);
        // ????????????????????????cpu???????????????
        FunSDK.SetApplication((MyApplication)mContext.getApplicationContext());
        // ????????????2
        FunSDK.MyInitNetSDK();

        // ??????????????????????????????
        FunSDK.SetFunStrAttr(EFUN_ATTR.APP_PATH, FunPath.getDefaultPath());
        // ????????????????????????????????????
        FunSDK.SetFunStrAttr(EFUN_ATTR.UPDATE_FILE_PATH, FunPath.getDeviceUpdatePath());
		// ??????SDK??????????????????????????????
		FunSDK.SetFunStrAttr(EFUN_ATTR.CONFIG_PATH,FunPath.getDeviceConfigPath());
        // ?????????????????????????????????
        result = FunSDK.SysInitNet("",0);
        FunLog.i(TAG, "FunSDK.SysInitNet : " + result);

        // ?????????APP??????(APP???????????????????????????)
        FunSDK.XMCloundPlatformInit(
                APP_UUID,        // uuid
                APP_KEY, // App Key
                APP_SECRET, // App Secret
                APP_MOVECARD); // moveCard

        // ??????/???????????????????????????
        mFunUserHandler = FunSDK.RegUser(this);
        FunLog.i(TAG, "FunSDK.RegUser : " + mFunUserHandler);

        // ??????????????????????????????????????????
        result = FunSDK.SetFunIntAttr(EFUN_ATTR.FUN_MSG_HANDLE, mFunUserHandler);
        FunLog.i(TAG, "FunSDK.SetFunIntAttr(EFUN_ATTR.FUN_MSG_HANDLE) : " + result);

        FunSDK.LogInit(mFunUserHandler, "", 1, "", 1);

        /**
         * ????????????????????????????????????????????????
         **/
        //???????????????????????????????????????????????????????????????
        if (getSaveNativePassword()) {
            FunSDK.SetFunStrAttr(EFUN_ATTR.USER_PWD_DB, FunPath.getConfigPassword());
            System.out.println("NativePasswordFileName" + FunPath.getConfigPassword());
        }

        /**
         * ???????????????????????? ????????????????????????????????????????????????????????????????????????
         * ????????????????????????
         */
        FunSDK.SetFunIntAttr(EFUN_ATTR.SUP_RPS_VIDEO_DEFAULT, SDKCONST.Switch.Open);
    }

    public void term() {
        if (mFunUserHandler >= 0) {
            FunSDK.UnRegUser(mFunUserHandler);
            mFunUserHandler = -1;
        }

        FunSDK.MyUnInitNetSDK();
    }

    public Context getContext() {
        return mContext;
    }

    public int getHandler() {
        return mFunUserHandler;
    }

    public String getString(Integer strResId) {
        if (null != mContext) {
            return mContext.getResources().getString(strResId);
        }
        return "";
    }

    public DeviceWifiManager getDeviceWifiManager() {
        return DeviceWifiManager.getInstance(getContext());
    }

    public boolean isAPDeviceConnected(FunDevice funDevice) {
        if (null == funDevice || null == funDevice.devName) {
            return false;
        }

        String curSSID = getDeviceWifiManager().getSSID();
        if (null == curSSID) {
            return false;
        }

        return curSSID.equals(funDevice.devName);
    }

    public void setAppPath(String path) {
        FunSDK.SetFunStrAttr(EFUN_ATTR.APP_PATH, path);
    }

    public void setLoginType(FunLoginType loginType) {
        if (loginType == FunLoginType.LOGIN_BY_AP) {
            FunSDK.SysInitAsAPModle(FunPath.getDeviceApPath());
        } else if (loginType == FunLoginType.LOGIN_BY_LOCAL) {
            FunSDK.SysInitLocal(FunPath.getLocalDB());
        } else {
            FunSDK.SysInitNet("",0);
        }
        this.mLoginType = loginType;
    }

    public FunLoginType getLoginType() {
        return mLoginType;
    }

    public void registerOnFunLoginListener(OnFunLoginListener l) {
        if (!mListeners.contains(l)) {
            mListeners.add(l);
        }
    }

    public void registerOnFunRegisterListener(OnFunRegisterListener l) {
        if (!mListeners.contains(l)) {
            mListeners.add(l);
        }
    }

    public void registerOnFunChangepasswListener(OnFunChangePasswListener l) {
        if (!mListeners.contains(l)) {
            mListeners.add(l);
        }
    }

    public void registerOnFunDeviceListener(OnFunDeviceListener l) {
        if (!mListeners.contains(l)) {
            mListeners.add(l);
        }
    }

    public void registerOnFunDeviceOptListener(OnFunDeviceOptListener l) {
        if (!mListeners.contains(l)) {
            mListeners.add(l);
        }
    }


    public void registerOnFunGetUserInfoListener(OnFunGetUserInfoListener l) {
        if (!mListeners.contains(l)) {
            mListeners.add(l);
        }
    }

    public void registerOnFunCheckPasswListener(OnFunCheckPasswListener l) {
        if (!mListeners.contains(l)) {
            mListeners.add(l);
        }
    }

    public void registerOnFunForgetPasswListener(OnFunForgetPasswListener l) {
        if (!mListeners.contains(l)) {
            mListeners.add(l);
        }
    }

    public void registerOnFunDeviceCaptureListener(OnFunDeviceCaptureListener l) {
        if (!mListeners.contains(l)) {
            mListeners.add(l);
        }
    }

    public void registerOnFunDeviceFileListener(OnFunDeviceFileListener l) {
        if (!mListeners.contains(l)) {
            mListeners.add(l);
        }
    }

    public void registerOnFunDeviceTalkListener(OnFunDeviceTalkListener l) {
        if (!mListeners.contains(l)) {
            mListeners.add(l);
        }
    }

    public void registerOnFunDeviceAlarmListener(OnFunDeviceAlarmListener l) {
        if (!mListeners.contains(l)) {
            mListeners.add(l);
        }
    }

    public void registerOnFunDeviceSerialListener(OnFunDeviceSerialListener l) {
        if (!mListeners.contains(l)) {
            mListeners.add(l);
        }
    }

    public void registerOnFunDeviceWiFiConfigListener(OnFunDeviceWiFiConfigListener l) {
        if (!mListeners.contains(l)) {
            mListeners.add(l);
        }
    }

    public void registerOnFunDeviceRecordListener(OnFunDeviceRecordListener l) {
        if (!mListeners.contains(l)) {
            mListeners.add(l);
        }
    }

    public void registerOnFunDeviceConnectListener(OnFunDeviceConnectListener l) {
        if (!mListeners.contains(l)) {
            mListeners.add(l);
        }
    }

    public void registerOnAddSubDeviceResultListener(OnAddSubDeviceResultListener l) {
        if (!mListeners.contains(l)) {
            mListeners.add(l);
        }
    }

    public void registerOnFunDeviceWakeUpListener(OnFunDeviceWakeUpListener ls) {
        if (!mListeners.contains(ls)) {
            mListeners.add(ls);
        }
    }

    public void registerOnFunDevBatteryLevelListener(OnFunDevBatteryLevelListener ls) {
        if (!mListeners.contains(ls)) {
            mListeners.add(ls);
        }
    }

    public void removeOnFunLoginListener(OnFunLoginListener l) {
        if (mListeners.contains(l)) {
            mListeners.remove(l);
        }
    }

    public void removeOnFunRegisterListener(OnFunRegisterListener l) {
        if (mListeners.contains(l)) {
            mListeners.remove(l);
        }
    }

    public void removeOnFunChangePasswListener(OnFunChangePasswListener l) {
        if (mListeners.contains(l)) {
            mListeners.remove(l);
        }
    }

    public void removeOnFunDeviceListener(OnFunDeviceListener l) {
        if (mListeners.contains(l)) {
            mListeners.remove(l);
        }
    }

    public void removeOnFunDeviceOptListener(OnFunDeviceOptListener l) {
        if (mListeners.contains(l)) {
            mListeners.remove(l);
        }
    }

    public void removeOnFunGetUserInfoListener(OnFunGetUserInfoListener l) {
        if (mListeners.contains(l)) {
            mListeners.remove(l);
        }
    }

    public void removeOnFunCheckPasswListener(OnFunCheckPasswListener l) {
        if (mListeners.contains(l)) {
            mListeners.remove(l);
        }
    }

    public void removeOnFunForgetPasswListener(OnFunForgetPasswListener l) {
        if (mListeners.contains(l)) {
            mListeners.remove(l);
        }
    }

    public void removeOnFunDeviceCaptureListener(OnFunDeviceCaptureListener l) {
        if (mListeners.contains(l)) {
            mListeners.remove(l);
        }
    }

    public void removeOnFunDeviceFileListener(OnFunDeviceFileListener l) {
        if (mListeners.contains(l)) {
            mListeners.remove(l);
        }
    }

    public void removeOnFunDeviceTalkListener(OnFunDeviceTalkListener l) {
        if (mListeners.contains(l)) {
            mListeners.remove(l);
        }
    }

    public void removeOnFunDeviceAlarmListener(OnFunDeviceAlarmListener l) {
        if (mListeners.contains(l)) {
            mListeners.remove(l);
        }
    }

    public void removeOnFunDeviceSerialListener(OnFunDeviceSerialListener l) {
        if (mListeners.contains(l)) {
            mListeners.remove(l);
        }
    }

    public void removeOnFunDeviceWiFiConfigListener(OnFunDeviceWiFiConfigListener l) {
        if (mListeners.contains(l)) {
            mListeners.remove(l);
        }
    }

    public void removeOnFunDeviceRecordListener(OnFunDeviceRecordListener l) {
        if (mListeners.contains(l)) {
            mListeners.remove(l);
        }
    }

    public void removeOnFunDeviceConnectListener(OnFunDeviceConnectListener l) {
        if (mListeners.contains(l)) {
            mListeners.remove(l);
        }
    }

    public void removeOnAddSubDeviceResultListener(OnAddSubDeviceResultListener l) {
        if (!mListeners.contains(l)) {
            mListeners.remove(l);
        }
    }

    public void removeOnFunDeviceWakeUpListener(OnFunDeviceWakeUpListener ls) {
        if (!mListeners.contains(ls)) {
            mListeners.remove(ls);
        }
    }

    public void removeOnFunDevBatteryLevelListener(OnFunDevBatteryLevelListener ls) {
        if (!mListeners.contains(ls)) {
            mListeners.remove(ls);
        }
    }

    /**
     * ????????????,?????????????????????????????????
     *
     * @param auto ??????????????????
     */
    public void setAutoLogin(boolean auto) {
        mAutoLoginWhenStartup = auto;
        mSharedParam.setBooleanUserValue(
                SHARED_PARAM_KEY_AUTOLOGIN, auto);
    }

    /**
     * ????????????????????????
     *
     * @return
     */
    public boolean getAutoLogin() {
        return mAutoLoginWhenStartup;
    }

    /**
     * ???????????????????????????????????????????????????
     *
     * @param is
     */
    public void setSaveNativePassword(boolean is) {
        mSaveNativePassword = is;
        mSharedParam.setBooleanUserValue(
                SHARED_PARAM_KEY_SAVENATIVEPASSWORD, is);
    }

    /**
     * ????????????????????????????????????
     *
     * @return
     */
    public boolean getSaveNativePassword() {
        return mSaveNativePassword;
    }

    /**
     * ????????????,???????????????????????????
     *
     * @param save
     */
    public void setSavePasswordAfterLogin(boolean save) {
        mSavePasswordAfterLogin = save;
        mSharedParam.setBooleanUserValue(
                SHARED_PARAM_KEY_SAVEPASSWORD, save);
    }

    /**
     * ????????????????????????
     *
     * @return
     */
    public boolean getSavePasswordAfterLogin() {
        return mSavePasswordAfterLogin;
    }

    /**
     * ????????????????????????????????????
     *
     * @return
     */
    public String getSavedUserName() {
        return FunLoginHistory.getInstance().getLastLoginUserName();
    }

    /**
     * ??????????????????????????????????????????
     *
     * @return
     */
    public String getSavedPassword() {
        return getSavedPassword(getSavedUserName());
    }

    /**
     * ?????????????????????????????????
     *
     * @return
     */
    public List<String> getSavedUserNames() {
        return FunLoginHistory.getInstance().getAllUserNames();
    }

    /**
     * ?????????????????????(??????????????????)
     *
     * @param userName
     * @return
     */
    public String getSavedPassword(String userName) {
        return FunLoginHistory.getInstance().getPassword(userName);
    }

    private void loadParams() {

        try {
            mAutoLoginWhenStartup = mSharedParam.getBooleanUserValue(
                    SHARED_PARAM_KEY_AUTOLOGIN, true);
            mSavePasswordAfterLogin = mSharedParam.getBooleanUserValue(
                    SHARED_PARAM_KEY_SAVEPASSWORD, true);
            mSaveNativePassword = mSharedParam.getBooleanUserValue(
                    SHARED_PARAM_KEY_SAVENATIVEPASSWORD, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateDeviceList(byte[] pData) {
        mDeviceList.clear();
        if (null == pData || pData.length <= 0) {
            return;
        }

        try {
            SDBDeviceInfo info = new SDBDeviceInfo();
            int nItemLen = G.Sizeof(info);
            int nCount = pData.length / nItemLen;
            SDBDeviceInfo infos[] = new SDBDeviceInfo[nCount];
            for (int i = 0; i < nCount; ++i) {
                infos[i] = new SDBDeviceInfo();
            }
            G.BytesToObj(infos, pData);
            for (int i = 0; i < nCount; ++i) {
                FunDevType devType = FunDevType.getType(infos[i].st_7_nType);

                // ????????????????????????????????????
                FunDevice funDevice = FunDeviceBuilder.buildWith(devType);
                // ?????????????????????
                funDevice.initWith(infos[i]);
                // ?????????????????????
                mDeviceList.add(funDevice);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        dumpDeviceList();
    }

    private void updateLanDeviceList(SDK_CONFIG_NET_COMMON_V2[] searchResult) {
        mLanDeviceList.clear();

        if (null != searchResult) {
            for (SDK_CONFIG_NET_COMMON_V2 com : searchResult) {
                addLanDevice(com);
            }
        }
    }

    private FunDevice addLanDevice(SDK_CONFIG_NET_COMMON_V2 comm) {
        FunDevice funDevice = null;
        synchronized (mLanDeviceList) {
            String devSn = G.ToString(comm.st_14_sSn);

            if (null != devSn) {
                if (null == (funDevice = findLanDevice(devSn))) {
                    // ??????????????????,??????????????????
                    FunDevType devType = FunDevType.getType(comm.st_15_DeviceType);

                    funDevice = FunDeviceBuilder.buildWith(devType);

                    funDevice.initWith(comm);

                    mLanDeviceList.add(funDevice);
                }
            }
        }

        return funDevice;
    }

    public void dumpDeviceList() {
        FunLog.d(TAG, "DeviceList:" + mDeviceList.size());
        for (FunDevice devInfo : mDeviceList) {
            FunLog.d(TAG, "     dev : " + devInfo.toString());
        }
    }

    /********************************************************************************
     * ????????????????????????
     */

    /**
     * ??????????????????????????????????????????
     *
     * @return
     */
    public boolean loginByLastUser() {
        String lastUserName = getSavedUserName();
        String lastPassWord = getSavedPassword();
        if (null != lastUserName
                && lastUserName.length() > 0
                && null != lastPassWord
                && lastPassWord.length() > 0) {
            String userName = lastUserName;
            String passWord = lastPassWord;
            return login(userName, passWord);
        }

        return false;
    }

    /**
     * ????????????
     *
     * @param username ?????????
     * @param password ??????
     * @return ??????????????????
     */
    public boolean login(String username, String password) {

        mTmpLoginUserName = username;
        mTmpLoginPassword = password;

//		int result = FunSDK.SysLoginToXM(getHandler(), username, password, 0);
        // return (result == 0);

        // ?????????????????????????????????????????????
        int result = FunSDK.SysGetDevList(getHandler(),
                mTmpLoginUserName, mTmpLoginPassword, 0);
        return (result == 0);
    }

    /**
     * ??????????????????????????????
     *
     * @return
     */
    public boolean hasLogin() {
        return null != mLoginUserName
                && mLoginUserName.length() > 0
                && null != mLoginPassword
                && mLoginPassword.length() > 0;
    }

    public String getUserName() {
        if (null == mLoginUserName) {
            return "";
        }
        return mLoginUserName;
    }

    public String getPassWord() {
        if (null == mLoginPassword) {
            return "";
        }
        return mLoginPassword;
    }


    /********************************************************************************
     * ????????????????????????
     */
    /**
     * ???????????????????????????
     *
     * @param userName ?????????
     * @param phoneNo  ?????????
     * @return
     */
    public boolean requestPhoneMsg(String userName, String phoneNo) {
        int result = FunSDK.SysSendPhoneMsg(getHandler(), userName, phoneNo, 0);
        return (result == 0);
    }

    /**
     * ???????????????????????????
     *
     * @param userName  ?????????
     * @param passWd    ??????
     * @param checkCode ???????????????
     * @param phoneNo   ?????????
     * @return
     */
    public boolean registerByPhone(String userName,
                                   String passWd, String checkCode, String phoneNo) {
        int result = FunSDK.SysRegUserToXM(getHandler(),
                userName, passWd, checkCode, phoneNo, 0);
        return (result == 0);
    }

    /**
     * ?????????????????????????????????
     */
    public boolean checkUserName(String userName) {
        int result = FunSDK.SysCheckUserRegiste(getHandler(), userName, 0);
        return (result == 0);
    }

    /**
     * ????????????????????????
     *
     * @param email
     * @return
     */
    public boolean requestEmailCode(String email) {
        int result = FunSDK.SysSendEmailCode(getHandler(), email, 0);
        return (result == 0);
    }

    /**
     * ??????????????????
     *
     * @param userName
     * @param passWd
     * @param email
     * @param code
     * @return
     */
    public boolean registerByEmail(String userName, String passWd,
                                   String code, String email) {
        int result = FunSDK.SysRegisteByEmail(getHandler(),
                userName, passWd, email, code, 0);
        return (result == 0);
    }

    /********************************************************************************
     * ??????????????????
     */
    /**
     * ??????????????????
     *
     * @param userName ?????????
     * @param oldPassw ?????????
     * @param newPassw ?????????
     * @return
     */
    public boolean changePassw(String userName, String oldPassw, String newPassw) {
//		int result = FunSDK.SysPswChange(getHandler(), userName, oldPassw, newPassw, 0);
        int result = FunSDK.SysEditPwdXM(getHandler(), userName, oldPassw, newPassw, 0);
        return (result == 0);
    }

    /**
     * ?????????????????????????????????
     *
     * @param passw ?????????????????????
     * @return
     */
    public boolean checkPassw(String passw) {
        int result = FunSDK.SysCheckPwdStrength(getHandler(), passw, 0);
        return (result == 0);
    }

    /**
     * ????????????????????????????????????????????????
     *
     * @param email ??????????????????
     * @return
     */
    public boolean requestSendEmailCodeForResetPW(String email) {
        int result = FunSDK.SysSendCodeForEmail(getHandler(), email, 0);
        return (result == 0);
    }

    /**
     * ???????????????
     *
     * @param email      ????????????
     * @param verifyCode ????????????????????????
     * @return
     */
    public boolean requestVerifyEmailCode(String email, String verifyCode) {
        int result = FunSDK.SysCheckCodeForEmail(getHandler(), email, verifyCode, 0);
        return (result == 0);
    }

    /**
     * ?????? Email ????????????
     *
     * @param email    ????????????
     * @param newPassw ?????????
     * @return
     */
    public boolean requestResetPasswByEmail(String email, String newPassw) {
        int result = FunSDK.SysChangePwdByEmail(getHandler(), email, newPassw, 0);
        return (result == 0);
    }

    /**
     * ??????????????????????????????
     *
     * @param phone ??????????????????????????????
     * @return
     */
    public boolean requestSendPhoneMsgForResetPW(String phone) {
        int result = FunSDK.SysForgetPwdXM(getHandler(), phone, 0);
        return (result == 0);
    }

    /**
     * ???????????????
     *
     * @param phone      ????????????
     * @param verifyCode ????????????????????????
     * @return
     */
    public boolean requestVerifyPhoneCode(String phone, String verifyCode) {
        int result = FunSDK.CheckResetCodeXM(getHandler(), phone, verifyCode, 0);
        return (result == 0);
    }

    /**
     * ??????????????????????????????
     *
     * @param phone    ????????????
     * @param newPassw ???????????????
     * @return
     */
    public boolean requestResetPasswByPhone(String phone, String newPassw) {
        int result = FunSDK.ResetPwdXM(getHandler(), phone, newPassw, 0);
        return (result == 0);
    }

    /********************************************************************************
     * ?????????????????????
     */
    /**
     * @return
     */
    public boolean getUserInfo() {
        if (TextUtils.isEmpty(mLoginUserName) || TextUtils.isEmpty(mLoginPassword)) {
            return false;
        }
        int result = FunSDK.SysGetUerInfo(getHandler(), mLoginUserName, mLoginPassword, 0);
        return (result == 0);
    }

    public boolean logout() {
        // int result = FunSDK.SysLogout(getHandler(), 0);
        // return (result ==0);

        // ??????????????????
        mLoginUserName = null;
        mLoginPassword = null;
        mDeviceList.clear();

        // ?????????????????????false
        setAutoLogin(false);

        // ??????
        for (OnFunListener l : mListeners) {
            if (l instanceof OnFunLoginListener) {
                ((OnFunLoginListener) l).onLogout();
            }
        }

        return true;
    }

    /********************************************************************************
     * ????????????????????????
     */
    /**
     * ??????????????????????????????
     *
     * @return ??????????????????
     */
    public boolean requestDeviceList() {
        FunLog.d(TAG, "---> call requestDeviceList()");
        int result = FunSDK.SysGetDevList(getHandler(),
                mLoginUserName, mLoginPassword, 0);
        return (result == 0);
    }

    /**
     * ???????????????
     *
     * @param funDevice
     * @param newDevName
     * @return
     */
    public boolean requestDeviceRename(FunDevice funDevice, String newDevName) {
        funDevice.devName = newDevName;
        SDBDeviceInfo devInfo = funDevice.toSDBDeviceInfo();
        int result = FunSDK.SysChangeDevInfo(getHandler(),
                G.ObjToBytes(devInfo),
                mLoginUserName, mLoginPassword, funDevice.getId());
        return (result == 0);
    }


    public boolean requestDeviceAddSubDev(FunDevice funDevice, String Command, String pconfig) {
        System.out.println("zyy----------pconfig   " + pconfig.toString());
        FunSDK.DevSetConfigByJson(getHandler(), funDevice.devSn, Command, pconfig, -1, 5000, funDevice.getId());
        return true;
    }


    public void requestControlSubDevice(FunDevice funDevice, String Command, String pconfig) {
        System.out.println("zyy----------pconfig   " + pconfig.toString());
        FunSDK.DevSetConfigByJson(getHandler(), funDevice.devSn, Command, pconfig, -1, 5000, funDevice.getId());
    }

    /**
     * ??????????????????
     *
     * @param funDevice
     * @return
     */
    public boolean requestDeviceRemove(FunDevice funDevice) {
        int result = FunSDK.SysDeleteDev(getHandler(), funDevice.devMac,
                mLoginUserName, mLoginPassword, 0);
        return (result == 0);
    }

    /**
     * ??????????????????
     *
     * @param funDevice
     * @return
     */
    public boolean requestDeviceAdd(FunDevice funDevice) {
        SDBDeviceInfo devInfo = new SDBDeviceInfo();

        if (StringUtils.isStringNULL(funDevice.getDevSn())) {
            Log.e(TAG, "Error Device SN.");
            return false;
        }

        // ????????????
        devInfo.st_7_nType = funDevice.devType.getDevIndex();
        // ???????????????(MAC/SN)
        G.SetValue(devInfo.st_0_Devmac, funDevice.getDevSn());

        // ????????????
        if (StringUtils.isStringNULL(funDevice.devName)) {
            G.SetValue(devInfo.st_1_Devname, funDevice.getDevSn());
        } else {
            G.SetValue(devInfo.st_1_Devname, funDevice.devName);
        }

        // ????????????,??????34567
        devInfo.st_6_nDMZTcpPort = 34567;
        // ?????????????????????,??????admin
        if (StringUtils.isStringNULL(funDevice.loginName)) {
            G.SetValue(devInfo.st_4_loginName, "admin");
        } else {
            G.SetValue(devInfo.st_4_loginName, funDevice.loginName);
        }
        // ??????????????????, ????????????
        if (StringUtils.isStringNULL(funDevice.loginPsw)) {
            G.SetValue(devInfo.st_5_loginPsw, "");
        } else {
            G.SetValue(devInfo.st_5_loginPsw, funDevice.loginPsw);
        }

        int result = FunSDK.SysAddDevice(getHandler(),
                G.ObjToBytes(devInfo), mLoginUserName, mLoginPassword, 0);
        return (result == 0);
    }

    /**
     * ?????????????????????AP????????????
     *
     * @return
     */
    public boolean requestAPDeviceList() {
        new Thread() {

            @Override
            public void run() {

                // ??????WIFI?????????,?????????WIFI
                if (!DeviceWifiManager.getInstance(getContext()).isWiFiEnabled()) {
                    DeviceWifiManager.getInstance(getContext()).openWifi();
                }

                // ??????WIFI??????10???,??????????????????????????????,???????????????
                final int scanSecond = 10;
                final int ONE_SECOND = 1000;

                for (int i = 0; i < scanSecond; i++) {
                    int nAPDevChanged = 0;

                    DeviceWifiManager.getInstance(getContext()).startScan(
                            DeviceWifiManager.WIFI_TYPE.DEV_AP, ONE_SECOND);

                    List<ScanResult> scanResults = DeviceWifiManager.getInstance(getContext()).getWifiList();
                    // ???????????????????????????????????????WiFi??????
                    for (int iDev = mAPDeviceList.size() - 1; iDev >= 0; iDev--) {
                        FunDevice funDevice = mAPDeviceList.get(iDev);
                        boolean isExist = false;
                        for (ScanResult scanResult : scanResults) {
                            if (scanResult.SSID.equals(funDevice.devName)) {
                                // ??????
                                isExist = true;
                                break;
                            }
                        }

                        if (!isExist) {
                            // ????????????,???????????????
                            FunLog.e("AP", "AP Deice offline : " + funDevice.devName);
                            mAPDeviceList.remove(iDev);
                            nAPDevChanged++;
                        }
                    }

                    String currSSID = DeviceWifiManager.getInstance(getContext()).getSSID();
                    String currGwIp = DeviceWifiManager.getInstance(getContext()).getGatewayIp();
                    for (ScanResult scanResult : scanResults) {
                        String ssid = scanResult.SSID.trim();
                        String bssid = scanResult.BSSID;
                        FunDevice funDevice = findAPDevice(ssid);
                        if (null != funDevice) {
                            // ???????????????,????????????????????????????????????????????????
                        } else {
                            // ???????????????????????????AP???????????????
                            int typeIndex = DeviceWifiManager.getXMDeviceAPType(ssid);
                            FunDevType devType = FunDevType.getType(typeIndex);
                            funDevice = FunDeviceBuilder.buildWith(devType);
                            funDevice.initWith(devType, ssid, bssid);
                            if (ssid.equals(currSSID) && null != currGwIp) {
                                funDevice.devIp = currGwIp;
                            }

//							SDBDeviceInfo devInf = funDevice.toSDBDeviceInfo();
//							FunLog.i("test", "AddDevice:" + devInf.toString());
                            // FunSDK.SysAddDevice(getHandler(),
                            // G.ObjToBytes(devInf),
//									funDevice.loginName, funDevice.loginPsw, mAPDeviceList.size());

                            mAPDeviceList.add(funDevice);
                            nAPDevChanged++;
                        }
                    }

                    // ?????????nAPDevChanged????????????????????????
                    if (i == 0 || nAPDevChanged > 0) {
                        // ????????????
                        if (null != mHandler) {
                            mHandler.sendEmptyMessage(MESSAGE_AP_DEVICE_LIST_CHANGED);
                        }
                    }
                }
            }

        }.start();


        return true;
    }

    public boolean requestLanDeviceList() {
        int result = FunSDK.DevSearchDevice(getHandler(), 10000, 0);
        return (result == 0);
    }

    /**
     * ??????WiFi????????????
     *
     * @param ssid
     * @param data
     * @param info
     * @param gw_ipaddr
     * @param type
     * @param isbroad
     * @param mac
     * @param nTimeout
     * @return
     */
    public boolean startWiFiQuickConfig(String ssid,
                                        String data, String info, String gw_ipaddr, int type, int isbroad, String mac, int nTimeout) {
        int nGetType = 2; // 2?????????
        int result = FunSDK.DevStartAPConfig(getHandler(),
                nGetType,
                ssid, data, info, gw_ipaddr, type, isbroad, mac, nTimeout);
        return (result == 0);
    }

    /**
     * ??????????????????
     */
    public void stopWiFiQuickConfig() {
        FunSDK.DevStopAPConfig();
    }

    /**
     * ??????????????????-????????????
     *
     * @param funDevice
     * @return
     */
    public boolean transportSerialOpen(FunDevice funDevice) {
        int result = FunSDK.DevOption(getHandler(), // userId??????
                funDevice.getDevSn(), // ???????????????
                EDEV_OPTERATE.EDOPT_DEV_OPEN_TANSPORT_COM, // ??????????????????
                null,
                0,
                SDK_CommTypes.SDK_COMM_TYPES_RS485,
                //			SDK_CommTypes.SDK_COMM_TYPES_RS232,
                0,
                0,
                "COM_OPEN",
                funDevice.getId());
        return (result == 0);
    }

    /**
     * ??????????????????-????????????
     *
     * @param funDevice
     * @return
     */
    public boolean transportSerialClose(FunDevice funDevice) {
        int result = FunSDK.DevOption(getHandler(),
                funDevice.getDevSn(),
                EDEV_OPTERATE.EDOPT_DEV_CLOSE_TANSPORT_COM,
                null, 0,
                SDK_CommTypes.SDK_COMM_TYPES_RS485, 0, 0,
                "COM_CLOSE", funDevice.getId());
        return (result == 0);
    }

    /**
     * ??????????????????-????????????
     *
     * @param funDevice
     * @param pData
     * @return
     */
    public boolean transportSerialWrite(FunDevice funDevice, byte[] pData) {
        int result = FunSDK.DevOption(getHandler(),
                funDevice.getDevSn(),
                EDEV_OPTERATE.EDOPT_DEV_TANSPORT_COM_WRITE,
                pData, pData.length,
                SDK_CommTypes.SDK_COMM_TYPES_RS485,
                //			SDK_CommTypes.SDK_COMM_TYPES_RS232,
                getHandler(), 0,
                "COM_WRITE", funDevice.getId());
        return (result == 0);
    }

    /**
     * ??????????????????-????????????(???????????????)
     *
     * @param funDevice
     * @param cmdStr
     * @return
     */
    public boolean transportSerialWrite(FunDevice funDevice, String cmdStr) {
        return transportSerialWrite(funDevice, cmdStr.getBytes());
    }

    /**
     * ????????????(??????)
     *
     * @param funDevice
     * @return
     */
    public boolean requestDeviceLogin(FunDevice funDevice) {
        if (null == funDevice) {
            return false;
        }

        String loginName = (null == funDevice.loginName) ? "admin" : funDevice.loginName;
        String loginPsd = (null == funDevice.loginPsw) ? "" : funDevice.loginPsw;
        // ???????????????????????????,??????????????????,?????????????????????,????????????????????????,DEMO?????????????????????????????????,??????FunDevicePassword.java
        String devicePasswd = FunDevicePassword.getInstance().getDevicePassword(
                funDevice.getDevSn());

        if (devicePasswd != null) {
            loginPsd = devicePasswd;
        }

//			switch (mVerificationPassword) {
//			case 2:					//??????????????????
//				if (devicePasswd != null) {
//				    loginPsd = devicePasswd;
//				}
//				if (funDevice.servicepsd()) {
//					mVerificationPassword--;
//				}
//				mVerificationPassword--;
//				break;
//			case 1:					//??????funDevice??????
//				mVerificationPassword--;
//				funDevice.setServicepsd(true);		//funDevice?????????????????????
//				break;
//			default:
//				break;
//			}

//			if (loginPsd.equals("") && null != devicePasswd) {
//
//				loginPsd = devicePasswd;
////				funDevice.loginPsw = loginPsd;
//			}

        NativeLoginPsw = loginPsd;

        if (null == findDeviceById(funDevice.getId())) {
            // ??????????????????????????????,???????????????????????????????????????,????????????????????????
            mTmpSNLoginDeviceList.add(funDevice);
        }
        System.out.println("TTTTT----->>>password = " + loginPsd);
        FunSDK.DevSetLocalPwd(funDevice.getDevSn(),loginName,loginPsd);
        int result = FunSDK.DevLogin(getHandler(),
                funDevice.getDevSn(),
                loginName, loginPsd,
                funDevice.getId());

        return (result == 0);
    }

    /**
     * ??????????????????
     *
     * @param
     */
    public void requestGetDevChnName(FunDevice funDevice) {
        FunSDK.DevGetChnName(getHandler(), funDevice.getDevSn(), "", "", funDevice.getId());

    }


    /**
     * ????????????(???????????????????????????)
     *
     * @param devMac
     * @param loginName
     * @param loginPasswd
     * @return
     */
    public boolean requestDeviceLogin(String devMac,
                                      String loginName, String loginPasswd) {
        if (null == loginName || loginName.length() == 0) {
            loginName = "admin";
        }

        if (null == loginPasswd) {
            loginPasswd = "";
        }

        // ????????????FunDevice??????,????????????Demo???????????????,??????????????????
        // ??????????????????????????????FunSDK.DevLogin()?????????????????????
        FunDevice funDevice = findDeviceBySn(devMac);// findTempDevice(devMac);
        if (null == funDevice) {
            funDevice = new FunDevice();
            funDevice.devMac = devMac;
            funDevice.devName = devMac;
            funDevice.devIp = "0.0.0.0";
            funDevice.loginName = loginName;
            funDevice.loginPsw = loginPasswd;
            funDevice.devStatus = FunDevStatus.STATUS_UNKNOWN;
            funDevice.isRemote = true;
            mTmpSNLoginDeviceList.add(funDevice);
        }

        int result = FunSDK.DevLogin(getHandler(),
                devMac, loginName, loginPasswd, funDevice.getId());
        return (result == 0);
    }

    /**
     * ????????????
     *
     * @param funDevice
     * @return
     */
    public boolean requestDeviceLogout(FunDevice funDevice) {
        int result = FunSDK.DevLogout(getHandler(),
                funDevice.getDevSn(),
                funDevice.getId());

        // ???????????????????????????
        setDeviceHasLogin(funDevice.getDevSn(), false);

        // ????????????????????????
        mCurrDevice = null;

        return (result == 0);
    }

    /**
     * ?????????????????????????????????
     *
     * @return
     */
    public boolean requestAllDeviceStatus() {
        int result = FunSDK.SysGetDevState(getHandler(), getAllDeviceSns(), 0);
        return (result == 0);
    }

    /**
     * ?????????????????????????????????????????????
     *
     * @return
     */
    public boolean requestAllLanDeviceStatus() {
        int result = FunSDK.SysGetDevState(getHandler(), getAllLanDeviceSns(), 0);
        return (result == 0);
    }

    /**
     * ?????????????????????????????????
     *
     * @param funDevice
     * @return
     */
    public boolean requestDeviceStatus(FunDevice funDevice) {
        if (null == funDevice) {
            return false;
        }

        int result = FunSDK.SysGetDevState(getHandler(),
                funDevice.getDevSn(), funDevice.getId());
        return (result == 0);
    }

    /**
     * ?????????????????????????????????
     *
     * @param devType ????????????(????????????)
     * @param devMac  ???????????????
     * @return
     */
    public boolean requestDeviceStatus(FunDevType devType, String devMac) {
        // ????????????FunDevice??????,????????????Demo???????????????,??????????????????
        FunDevice funDevice = buildTempDeivce(devType, devMac);

        funDevice.devStatus = FunDevStatus.STATUS_UNKNOWN;

        int result = FunSDK.SysGetDevState(getHandler(),
                funDevice.getDevSn(), funDevice.getId());
        return (result == 0);
    }

    /**
     * ??????????????????????????????????????????
     *
     * @param funDevice
     * @return
     */
    public boolean requestDeviceConfig(FunDevice funDevice) {
        SDK_Authority authority = new SDK_Authority();
        int result = FunSDK.DevGetConfig(getHandler(), funDevice.getDevSn(), ECONFIG.CFG_ATHORITY,
                G.Sizeof(authority), -1, 5000, funDevice.getId());
        return (result == 0);
    }

    /**
     * ?????????????????????
     *
     * @param funDevice  ??????
     * @param configName ????????????
     * @return
     */
    public boolean requestDeviceConfig(FunDevice funDevice, String configName) {
//		int result = FunSDK.DevGetConfigByJson(getHandler(), funDevice.getDevSn(),
        // configName, 4096, -1, 10000, funDevice.getId());
        // return (result == 0);

        // ???????????????????????????????????????
        // synchronized (mDeviceGetConfigQueue) {
//			mDeviceGetConfigQueue.add(new DeviceGetConfig(funDevice, configName));
        // }
        // mHandler.removeMessages(MESSAGE_GET_DEVICE_CONFIG);
        // mHandler.sendEmptyMessageDelayed(MESSAGE_GET_DEVICE_CONFIG, 50);
        // return true;
        return requestDeviceConfig(funDevice, configName, -1);
    }

    /**
     * ?????????????????????
     *
     * @param funDevice  ??????
     * @param configName ????????????
     * @param channelNo  ?????????
     * @return
     */
    public boolean requestDeviceConfig(FunDevice funDevice, String configName, int channelNo) {

        // ???????????????????????????????????????
        synchronized (mDeviceGetConfigQueue) {
            mDeviceGetConfigQueue.add(new DeviceGetConfig(funDevice, configName, channelNo));
        }
        mHandler.removeMessages(MESSAGE_GET_DEVICE_CONFIG);
        mHandler.sendEmptyMessageDelayed(MESSAGE_GET_DEVICE_CONFIG, 50);
        return true;
    }

    private void resetTimeoutDeviceConfigFromQueue(DeviceGetConfig config) {
        mHandler.removeMessages(MESSAGE_GET_DEVICE_CONFIG);
        mHandler.removeMessages(MESSAGE_GET_DEVICE_CONFIG_TIMEOUT);
        synchronized (mDeviceGetConfigQueue) {
            if (mDeviceGetConfigQueue.contains(config)) {
                mDeviceGetConfigQueue.remove(config);
            }
            mIsGettingConfig = false;
        }
        mHandler.sendEmptyMessage(MESSAGE_GET_DEVICE_CONFIG);
    }

    private boolean requestDeviceConfigFromQueue() {
        DeviceGetConfig devCfg = null;

        mHandler.removeMessages(MESSAGE_GET_DEVICE_CONFIG);

        if (mIsGettingConfig) {
            mHandler.sendEmptyMessageDelayed(MESSAGE_GET_DEVICE_CONFIG, 50);
            return true;
        }

        synchronized (mDeviceGetConfigQueue) {
            if (mDeviceGetConfigQueue.size() > 0 && !mIsGettingConfig) {
                devCfg = mDeviceGetConfigQueue.get(0);
                mIsGettingConfig = true;

                // ????????????????????????,??????SDK??????????????????,??????????????????
                mHandler.removeMessages(MESSAGE_GET_DEVICE_CONFIG_TIMEOUT);
                Message toutMsg = new Message();
                toutMsg.what = MESSAGE_GET_DEVICE_CONFIG_TIMEOUT;
                toutMsg.obj = devCfg;
                mHandler.sendMessageDelayed(toutMsg, 30000);
            }
        }

        if (null != devCfg) {
            int result = FunSDK.DevGetConfigByJson(getHandler(), devCfg.funDevice.getDevSn(),
                    devCfg.configName, 4096, devCfg.channelNo, 10000, devCfg.funDevice.getId());
            mHandler.sendEmptyMessageDelayed(MESSAGE_GET_DEVICE_CONFIG, 10);
            return (result == 0);
        }

        return true;
    }

    private void requestDeviceConfigDone(FunDevice funDevice, String configName) {
        DeviceGetConfig devCfg = null;
        synchronized (mDeviceGetConfigQueue) {
            // ????????????????????????
            mHandler.removeMessages(MESSAGE_GET_DEVICE_CONFIG_TIMEOUT);

            for (int i = 0; i < mDeviceGetConfigQueue.size(); i++) {
                DeviceGetConfig tmpCfg = mDeviceGetConfigQueue.get(i);
                if (tmpCfg.configName.equals(configName)
                        && tmpCfg.funDevice.getId() == funDevice.getId()) {
                    devCfg = tmpCfg;
                    mDeviceGetConfigQueue.remove(i);
                    break;
                }
            }
        }

        if (devCfg == null) {
            FunLog.e(TAG, "Error!!! must not be null here!");
        } else if (!devCfg.configName.equals(configName)) {
            FunLog.e(TAG, "Error!!! must be the same configName here! [" + devCfg.configName + "] != [" + configName + "]");
        } else {
            synchronized (mDeviceGetConfigQueue) {
                mIsGettingConfig = false;
            }
            mHandler.removeMessages(MESSAGE_GET_DEVICE_CONFIG);
            mHandler.sendEmptyMessage(MESSAGE_GET_DEVICE_CONFIG);
        }
    }

    /**
     * ????????????(??????)
     *
     * @param funDevice ??????
     * @param parmObj   ????????????
     * @return
     */
    public boolean requestDeviceSetConfig(FunDevice funDevice, Object parmObj) {
        int result = -1;

        FunLog.i("test", "requestDeviceSetConfig : " + funDevice.getId());

        if (parmObj instanceof BaseConfig) {
            BaseConfig baseConfig = (BaseConfig) parmObj;
            result = FunSDK.DevSetConfigByJson(getHandler(),
                    funDevice.getDevSn(),
                    baseConfig.getConfigName(),
                    baseConfig.getSendMsg(),
                    funDevice.CurrChannel,
                    60000,
                    funDevice.getId());
        }
        return (result == 0);
    }

    /**
     * ???????????????????????????
     *
     * @param funDevice
     * @return
     */
    public boolean requestSportCmdGeneral(FunDevice funDevice, ManualSnapModeJP data) {
        int result = FunSDK.DevCmdGeneral(getHandler(), funDevice.getDevSn(),
                EDEV_JSON_ID.NET_MANUALSNAP_MODE_REQ,
                ManualSnapModeJP.CLASSNAME, -1, 0, data
                        .getSendMsg().getBytes(), -1, ManualSnapModeJP.CAPTURE);
        return (result == 0);
    }

    /**
     * ???????????????????????????(DevCmdGeneral,?????????DevGetConfigByJson())
     *
     * @param funDevice
     * @param cmd
     * @return
     */
    public boolean requestDeviceCmdGeneral(FunDevice funDevice, DevCmdGeneral cmd) {
        int result = FunSDK.DevCmdGeneral(getHandler(),
                funDevice.getDevSn(),
                cmd.getJsonID(), cmd.getConfigName(), 0, 10000,
                null != cmd.getSendMsg() ? cmd.getSendMsg().getBytes() : null, -1, funDevice.getId());
        return (result == 0);
    }

    public boolean requestDeviceFileNum(FunDevice funDevice,
                                        DevCmdSearchFileNumJP cmd, int nSeq) {
        int result = FunSDK.DevCmdGeneral(getHandler(),
                funDevice.getDevSn(),
                cmd.getJsonID(), cmd.getConfigName(), 1024, 10000,
                cmd.getSendMsg().getBytes(), -1, nSeq);
        return (result == 0);
    }

    public boolean requestDeviceFileList(FunDevice funDevice,
                                         H264_DVR_FINDINFO info) {
        int result = FunSDK.DevFindFile(getHandler(),
                funDevice.getDevSn(), G.ObjToBytes(info), 1000,
                20000, funDevice.getId());
        Log.i("SDK_LOG", "--> DevFindFile : info = " + info.toString());
        return (result == 0);
    }

    public boolean requestDeviceFileListByTime(FunDevice funDevice,
                                               SDK_SearchByTime info) {
        int result = FunSDK.DevFindFileByTime(getHandler(),
                funDevice.getDevSn(), G.ObjToBytes(info),
                10000, 0);
        Log.i("SDK_LOG", "--> DevFindFileByTime : info = " + info.toString());
        return (result == 0);
    }

    public boolean requestDeviceSearchPicture(FunDevice funDevice,
                                              OPCompressPic opCompressPic, String filePath, int seq) {
        int result = FunSDK.DevSearchPicture(getHandler(),
                funDevice.getDevSn(),
                EDEV_JSON_ID.COMPRESS_PICTURE_REQ, 50000, 2000,
                opCompressPic.getSendMsg().getBytes(),
                20, -1,
                filePath, seq);
        return (result == 0);
    }

    public boolean requestDeviceRemovePicture(FunDevice funDevice,
                                              DevCmdOPRemoveFileJP cmdOPRemoveFileJP) {
        int result = FunSDK.DevCmdGeneral(getHandler(),
                funDevice.getDevSn(), cmdOPRemoveFileJP.getJsonID(),
                cmdOPRemoveFileJP.getConfigName(), cmdOPRemoveFileJP.getFileNum() * 256,
                cmdOPRemoveFileJP.getFileNum() * 2000,
                cmdOPRemoveFileJP.getSendMsg().getBytes(), -1, 0);
        return (result == 0);
    }

    public boolean requestDeviceRemoveFile(FunDevice funDevice,
                                           OPRemoveFileJP mOPRemoveFileJP) {
        int result = FunSDK.DevCmdGeneral(getHandler(), funDevice.getDevSn(),
                EDEV_JSON_ID.FILERMOVE_REQ, OPRemoveFileJP.CLASSNAME, -1,
                mOPRemoveFileJP.getFileNum() * 5000, mOPRemoveFileJP
                        .getSendMsg().getBytes(), -1, 0);
        return (result == 0);
    }

    public boolean requestDeviceSearchPicByPath(FunDevice funDevice,
                                                DevCmdOPFileQueryJP cmdOPFileQueryJP, int seq) {
        int result = FunSDK.DevCmdGeneral(getHandler(),
                funDevice.getDevSn(), cmdOPFileQueryJP.getJsonID(),
                cmdOPFileQueryJP.getConfigName(), -1, 1000,
                cmdOPFileQueryJP.getSendMsg().getBytes(), -1, seq);

        return (result == 1);
    }

    /**
     * ????????????????????????
     *
     * @param funDevice
     * @param info
     * @return
     */
    public boolean requestDeviceSearchAlarmInfo(FunDevice funDevice,
                                                XPMS_SEARCH_ALARMINFO_REQ info) {
        int result = MpsClient.SearchAlarmInfo(
                getHandler(), G.ObjToBytes(info), funDevice.getId());
        return (result == 1);
    }

    /**
     * ????????????????????????
     *
     * @param funDevice ??????????????????????????????
     * @return
     */
    public boolean requestDeviceCapture(FunDevice funDevice) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String strDate = sdf.format(new Date());
        strDate = strDate + System.currentTimeMillis();

        int result = FunSDK.DevOption(getHandler(),
                funDevice.getDevSn(),
                EDEV_OPTERATE.EDOPT_DEV_GET_IMAGE,
                null, 0, 0, 0, 0,
                FunPath.PATH_CAPTURE_TEMP + File.separator + strDate + ".jpg", funDevice.getId());
        return (result == 1);

    }

    /**
     * ????????????????????????
     *
     * @param funDevice ??????????????????????????????
     * @return
     */
    public boolean requestDeviceCapture(FunDevice funDevice, String path) {

        int result = FunSDK.DevOption(getHandler(),
                funDevice.getDevSn(),
                EDEV_OPTERATE.EDOPT_DEV_GET_IMAGE,
                null, 0, 0, 0, 0,
                path, funDevice.getId());
        return (result == 1);

    }

    /**
     * ?????????????????????????????????
     *
     * @param funDevices          ????????????
     * @param pH264_DVR_FILE_DATA ???????????????H264_DVR_FILE_DATA ???????????????
     * @param szFilePath          ??????????????????+?????????
     * @return
     */
    public boolean requestDeviceDownloadByFile(FunDevice funDevices, byte[] pH264_DVR_FILE_DATA, String szFilePath, int position) {
        int result = FunSDK.DevDowonLoadByFile(getHandler(),
                funDevices.getDevSn(), pH264_DVR_FILE_DATA, szFilePath, position);
        return (result == 1);
    }

    /**
     * ???????????????????????????????????????
     * EUIMSG.ON_FILE_DOWNLOAD:????????????????????????
     * EUIMSG.ON_FILE_DLD_POS:????????????????????????
     * EUIMSG.ON_FILE_DLD_COMPLETE:????????????????????????
     *
     * @return
     */
    public boolean requestDeviceDownloadByTime(DownloadInfo<H264_DVR_FINDINFO> info) {
        Object obj = info.getObj();
        int result = FunSDK.DevDowonLoadByTime(getHandler(), info.getSn(),
                G.ObjToBytes(obj), info.getFileName(), -1);
        return (result == 1);
    }

    /**
     *???????????????????????????
     * @param funDevices
     * @param chnId ?????????
     * @param streamType ???????????? ??????Main?????????Sub???
     * @param sTime ???????????? ??????????????? FunSDK.ToTimeType?????????
     * @param eTime ??????????????????????????? FunSDK.ToTimeType?????????
     * @param szFilePath ??????????????????
     ** EUIMSG.ON_FILE_DOWNLOAD:????????????????????????
     ** EUIMSG.ON_FILE_DLD_POS:????????????????????????
     ** EUIMSG.ON_FILE_DLD_COMPLETE:????????????????????????
     * @return
     */
    public boolean requestDeviceDownloadByCloudFile(FunDevice funDevices,int chnId,String streamType,
                                                    int sTime,int eTime,String szFilePath) {
        int result = FunSDK.MediaCloudRecordDownload(getHandler(),funDevices.getDevSn(),
                chnId,streamType, sTime,eTime,szFilePath,-1);
        return (result == 1);
    }

    /**
     * EMSG_ON_FILE_DOWNLOAD:
     * ????????????????????????
     *
     * @param funDevice
     * @return
     */
    public int requestDeviceStartTalk(FunDevice funDevice) {
        return FunSDK.DevStarTalk(getHandler(), funDevice.getDevSn(),0,0, 0);
    }

    public void requestDeviceStopTalk(int hTalker) {
        FunSDK.DevStopTalk(hTalker);
    }

    public int requestDeviceSendTalkData(FunDevice funDevice, byte[] data, int size) {
        return FunSDK.DevSendTalkData(funDevice.getDevSn(), data, size);
    }

    /**
     * ????????????????????????
     *
     * @param funDevice
     * @param nPTZCommand ??????EPTZCMD.TILT_UP,EPTZCMD.TILT_DOWN,EPTZCMD.PAN_RIGHT,EPTZCMD.PAN_LEFT
     * @param bStop
     * @return
     */
    public boolean requestDevicePTZControl(FunDevice funDevice,
                                           int nPTZCommand, boolean bStop, int channel) {
        int result = FunSDK.DevPTZControl(getHandler(),
                funDevice.getDevSn(), channel, nPTZCommand, bStop ? 1 : 0, 4, funDevice.getId());
        return (result == 1);
    }

    /**
     * ??????/???????????????????????????,?????????????????????,????????????????????????????????????,??????????????????????????????????????????,??????????????????
     *
     * @return
     */
    public boolean startDeviceLanAlarmListener() {
        int result = FunSDK.DevSetAlarmListener(getHandler(), 0);
        return (result == 1);
    }

    /**
     * ????????????/?????????????????????????????????
     *
     * @param devSn
     * @param enable
     * @return
     */
    public boolean requestDeviceLanAlarmEnable(String devSn,
                                               boolean enable) {
        int nValue = enable ? 1 : 0; // 0????????? ??????0?????????????????????1???
        int result = FunSDK.DevSetAttrAlarmByInt(getHandler(),
                devSn,
                EDEV_ATTR.EDA_OPT_ALARM,
                nValue,
                0);
        return (result == 1);
    }

    /**
     * ????????????????????????
     */
    public boolean requestDeviceUpdateCheck(String devSn) {
        int result = FunSDK.DevCheckUpgrade(getHandler(), devSn, 0);
        return (result == 0);
    }

    public boolean requestDeviceTitleDot(FunDevice funDevice,
                                         SDK_TitleDot titleDot) {
        int result = FunSDK.DevCmdGeneral(getHandler(), funDevice.getDevSn(),
                EDEV_JSON_ID.CONFIG_CHANNELTILE_DOT_SET,
                "TitleDot", 1024, 5000, G.ObjToBytes(titleDot),
                -1, 0);
        return (result == 0);
    }

    /**
     * ????????????ID????????????
     *
     * @param devId
     * @return
     */
    public FunDevice findDeviceById(int devId) {
        for (FunDevice funDev : mDeviceList) {
            if (devId == funDev.getId()) {
                return funDev;
            }
        }

        for (FunDevice funDevice : mAPDeviceList) {
            if (devId == funDevice.getId()) {
                return funDevice;
            }
        }

        for (FunDevice funDevice : mLanDeviceList) {
            if (devId == funDevice.getId()) {
                return funDevice;
            }
        }

        for (FunDevice funDevice : mTmpSNLoginDeviceList) {
            if (devId == funDevice.getId()) {
                return funDevice;
            }
        }

        return null;
    }

    /**
     * ????????????(MAC)????????????
     *
     * @param devSn
     * @return
     */
    public FunDevice findDeviceBySn(String devSn) {
        if (null == devSn) {
            return null;
        }

        for (FunDevice funDev : mDeviceList) {
            if (devSn.equals(funDev.getDevSn())) {
                return funDev;
            }
        }

        for (FunDevice funDevice : mAPDeviceList) {
            if (devSn.equals(funDevice.getDevSn())) {
                return funDevice;
            }
        }

        for (FunDevice funDevice : mLanDeviceList) {
            if (devSn.equals(funDevice.getDevSn())) {
                return funDevice;
            }
        }

        for (FunDevice funDevice : mTmpSNLoginDeviceList) {
            if (devSn.equals(funDevice.getDevSn())) {
                return funDevice;
            }
        }

        return null;
    }

    private void setDeviceStatus(String devSn, FunDevStatus status, int statusValue) {
        // ???????????????????????????,???????????????????????????????????????????????????????????????????????????????????????,??????????????????????????????
        if (null == devSn) {
            return;
        }

        for (FunDevice funDev : mDeviceList) {
            if (devSn.equals(funDev.getDevSn())) {
                funDev.devStatus = status;
                funDev.devStatusValue = statusValue;
                break;
            }
        }

        for (FunDevice funDevice : mAPDeviceList) {
            if (devSn.equals(funDevice.getDevSn())) {
                funDevice.devStatus = status;
                funDevice.devStatusValue = statusValue;
                break;
            }
        }

        for (FunDevice funDevice : mLanDeviceList) {
            if (devSn.equals(funDevice.getDevSn())) {
                funDevice.devStatus = status;
                funDevice.devStatusValue = statusValue;
                break;
            }
        }

        for (FunDevice funDevice : mTmpSNLoginDeviceList) {
            if (devSn.equals(funDevice.getDevSn())) {
                funDevice.devStatus = status;
                funDevice.devStatusValue = statusValue;
                break;
            }
        }
    }

    private void setDeviceStatus(String devSn, FunDevStatus status) {
        // ???????????????????????????,???????????????????????????????????????????????????????????????????????????????????????,??????????????????????????????
        if (null == devSn) {
            return;
        }

        for (FunDevice funDev : mDeviceList) {
            if (devSn.equals(funDev.getDevSn())) {
                funDev.devStatus = status;
                break;
            }
        }

        for (FunDevice funDevice : mAPDeviceList) {
            if (devSn.equals(funDevice.getDevSn())) {
                funDevice.devStatus = status;
                break;
            }
        }

        for (FunDevice funDevice : mLanDeviceList) {
            if (devSn.equals(funDevice.getDevSn())) {
                funDevice.devStatus = status;
                break;
            }
        }

        for (FunDevice funDevice : mTmpSNLoginDeviceList) {
            if (devSn.equals(funDevice.getDevSn())) {
                funDevice.devStatus = status;
                break;
            }
        }
    }

    public void setDeviceHasLogin(String devSn, boolean login) {
        // ???????????????????????????,???????????????????????????????????????????????????????????????????????????????????????,??????????????????????????????
        if (null == devSn) {
            return;
        }

        for (FunDevice funDev : mDeviceList) {
            if (devSn.equals(funDev.getDevSn())) {
                funDev.setHasLogin(login);
                break;
            }
        }

        for (FunDevice funDevice : mAPDeviceList) {
            if (devSn.equals(funDevice.getDevSn())) {
                funDevice.setHasLogin(login);
                break;
            }
        }

        for (FunDevice funDevice : mLanDeviceList) {
            if (devSn.equals(funDevice.getDevSn())) {
                funDevice.setHasLogin(login);
                break;
            }
        }

        for (FunDevice funDevice : mTmpSNLoginDeviceList) {
            if (devSn.equals(funDevice.getDevSn())) {
                funDevice.setHasLogin(login);
                break;
            }
        }
    }

    private void setDeviceHasConnected(String devSn, boolean connected) {
        // ???????????????????????????,???????????????????????????????????????????????????????????????????????????????????????,??????????????????????????????
        if (null == devSn) {
            return;
        }

        for (FunDevice funDev : mDeviceList) {
            if (devSn.equals(funDev.getDevSn())) {
                funDev.setConnected(connected);
                break;
            }
        }

        for (FunDevice funDevice : mAPDeviceList) {
            if (devSn.equals(funDevice.getDevSn())) {
                funDevice.setConnected(connected);
                break;
            }
        }

        for (FunDevice funDevice : mLanDeviceList) {
            if (devSn.equals(funDevice.getDevSn())) {
                funDevice.setConnected(connected);
                break;
            }
        }

        for (FunDevice funDevice : mTmpSNLoginDeviceList) {
            if (devSn.equals(funDevice.getDevSn())) {
                funDevice.setConnected(connected);
                break;
            }
        }
    }

    private String getAllDeviceSns() {
        String devIds = "";
        for (FunDevice funDev : mDeviceList) {
            if (devIds.length() > 0) {
                devIds += ";";
            }
            devIds += funDev.getDevSn();
        }
        return devIds;
    }

    private String getAllLanDeviceSns() {
        String devIds = "";
        for (FunDevice funDev : mLanDeviceList) {
            if (devIds.length() > 0) {
                devIds += ";";
            }
            devIds += funDev.getDevSn();
        }
        return devIds;
    }

    /**
     * ????????????????????????????????????
     *
     * @return ??????????????????
     */
    public List<FunDevice> getDeviceList() {
        return mDeviceList;
    }

    /**
     * ?????????????????????AP????????????
     *
     * @return AP????????????
     */
    public List<FunDevice> getAPDeviceList() {
        return mAPDeviceList;
    }

    public FunDevice findAPDevice(String ssid) {
        if (null == ssid || ssid.length() == 0) {
            return null;
        }

        for (FunDevice funDevice : mAPDeviceList) {
            if (ssid.equals(funDevice.devName)) {
                return funDevice;
            }
        }

        return null;
    }

    public FunDevice findLanDevice(String devSn) {
        for (FunDevice funDevice : mLanDeviceList) {
            if (devSn.equals(funDevice.getDevSn())) {
                return funDevice;
            }
        }
        return null;
    }

    /**
     * ?????????????????????????????????
     *
     * @return
     */
    public List<FunDevice> getLanDeviceList() {
        return mLanDeviceList;
    }

    public FunDevice findTempDevice(String devMac) {
        for (FunDevice funDevice : mTmpSNLoginDeviceList) {
            if (devMac.equals(funDevice.devMac)) {
                return funDevice;
            }
        }

        return null;
    }

    /**
     * ?????????????????????,????????????????????????
     *
     * @param devType ????????????(????????????)
     * @param devMac  ???????????????
     * @return
     */
    public FunDevice buildTempDeivce(FunDevType devType,
                                     String devMac) {
        FunDevice funDevice = findDeviceBySn(devMac);// findTempDevice(devMac);
        if (null == funDevice) {
        	if (devType == FunDevType.EE_DEV_INTELLIGENTSOCKET) {
        		funDevice = new FunDeviceSocket();
			}else {
				funDevice = new FunDevice();
			}
            funDevice.devMac = devMac;
            funDevice.devName = devMac;
            funDevice.devIp = "0.0.0.0";
            funDevice.devType = devType;
            funDevice.devStatus = FunDevStatus.STATUS_UNKNOWN;
            funDevice.isRemote = true;
            mTmpSNLoginDeviceList.add(funDevice);
        }
        return funDevice;
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_AP_DEVICE_LIST_CHANGED: {
                    // AP????????????????????????
                    for (OnFunListener l : mListeners) {
                        if (l instanceof OnFunDeviceListener) {
                            ((OnFunDeviceListener) l).onAPDeviceListChanged();
                        }
                    }
                }
                break;
                case MESSAGE_GET_DEVICE_CONFIG: {
                    requestDeviceConfigFromQueue();
                }
                break;
                case MESSAGE_GET_DEVICE_CONFIG_TIMEOUT: {
                    resetTimeoutDeviceConfigFromQueue((DeviceGetConfig) msg.obj);
                }
                break;
            }
        }

    };


    private void onUserInfoSavedAfterLoginSuccess() {
        // ???????????????????????????????????????
        mLoginUserName = mTmpLoginUserName;
        mLoginPassword = mTmpLoginPassword;

        // ???????????????????????????,????????????????????????
        if (getSavePasswordAfterLogin()) {
            FunLoginHistory.getInstance().saveLoginInfo(mLoginUserName, mLoginPassword);
        } else {
            FunLoginHistory.getInstance().saveLoginInfo(mLoginUserName, "");
        }

        // ???????????????,?????????????????????/??????????????????
        mpsInit();
    }

    /**
     * ?????????????????????/??????????????????(?????????????????????????????????)
     */
    private void mpsInit() {
        // ???????????????????????????
        try {
            if (null != mLoginUserName
                    && !mLoginUserName.equals(mLastMpsUserName)) {
                String pushToken = MyUtils.getMpsPushToken(getContext());

                SMCInitInfo info = new SMCInitInfo();
                G.SetValue(info.st_0_user, mLoginUserName);
                G.SetValue(info.st_1_password, mLoginPassword);
                G.SetValue(info.st_2_token, pushToken);

                MpsClient.Init(getHandler(), G.ObjToBytes(info), 0);

                mLastMpsUserName = mLoginUserName;

                FunLog.i(TAG, "MpsClient init with user : " + mLastMpsUserName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ??????/???????????????????????????????????????
     *
     * @param funDevice
     */
    public void mpsLinkDevice(FunDevice funDevice) {
        if (null != funDevice) {
            FunLog.d(TAG, "LinkDev : " + funDevice.getDevSn()
                    + ", " + funDevice.loginName + ", " + funDevice.loginPsw);
            MpsClient.LinkDev(getHandler(),
                    funDevice.getDevSn(),
                    funDevice.loginName, funDevice.loginPsw, 0);
        }
    }

    /**
     * ??????/???????????????????????????????????????
     *
     * @param devSn
     */
    public void mpsUnLinkDevice(String devSn) {
        if (null != devSn) {
            MpsClient.UnlinkDev(getHandler(),
                    devSn, 0);
        }
    }

    public void requestDevWakeUp(FunDevice funDevice) {
        if (null != funDevice) {
            FunSDK.DevWakeUp(getHandler(), funDevice.getDevSn(), funDevice.getId());
        }
    }

    public void requestDevSleep(FunDevice funDevice) {
        if (null != funDevice) {
            if (funDevice.hasLogin()) {
                requestDeviceLogout(funDevice);
            }else {
                FunSDK.DevLogout(getHandler(), funDevice.getDevSn(), funDevice.getId());
            }
        }
    }

    public void requestRegisterDevBatteryLevel(FunDevice funDevice) {
        if (null != funDevice) {
            FunSDK.DevStartUploadData(getHandler(), funDevice.getDevSn(),
                    SDKCONST.UploadDataType.SDK_ELECT_STATE, 3);
        }
    }

    public void requestCancelDevBatteryLevel(FunDevice funDevice) {
        if (null != funDevice) {
            FunSDK.DevStopUploadData(getHandler(), funDevice.getDevSn(),
                    SDKCONST.UploadDataType.SDK_ELECT_STATE, 0);
        }
    }

    //????????????
    public void requestSyncDevZone(FunDevice funDevice,int zone) {
        TimeZoneBean timeZoneBean = new TimeZoneBean();
        timeZoneBean.timeMin = zone;
        timeZoneBean.FirstUserTimeZone = 0;
        FunSDK.DevSetConfigByJson(getHandler(), funDevice.getDevSn(),
                JsonConfig.SYSTEM_TIMEZONE,
                HandleConfigData.getSendData(JsonConfig.SYSTEM_TIMEZONE,
                        "0x1", timeZoneBean),
                -1, 5000, funDevice.getId());
        FunSDK.DevGetConfigByJson(getHandler(),funDevice.getDevSn(),
                JsonConfig.GENERAL_LOCATION,
                1024,-1,5000,funDevice.getId());
    }

    private void parseBatteryState(String jsonStr) {
        if (TextUtils.isEmpty(jsonStr))
            return;
        try {
            JSONObject jsonObject = new JSONObject(jsonStr);
            jsonObject = jsonObject.optJSONObject(ElectCapacityBean.CLASSNAME);
            ElectCapacityBean electCapacityBean = new ElectCapacityBean();
            electCapacityBean.devStorageStatus = jsonObject.optInt("DevStorageStatus", -2);
            electCapacityBean.electable = jsonObject.optInt("electable", 3);
            electCapacityBean.percent = jsonObject.optInt("percent", 4);
            for (OnFunListener l : mListeners) {
                if (l instanceof OnFunDevBatteryLevelListener) {
                    ((OnFunDevBatteryLevelListener) l).onBatteryLevel(
                            electCapacityBean.devStorageStatus,
                            electCapacityBean.electable,
                            electCapacityBean.percent);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int OnFunSDKResult(Message msg, MsgContent msgContent) {
        FunLog.d(TAG, "msg.what : " + msg.what);
        FunLog.d(TAG, "msg.arg1 : " + msg.arg1 + " [" + FunError.getErrorStr(msg.arg1) + "]");
        FunLog.d(TAG, "msg.arg2 : " + msg.arg2);
        if (null != msgContent) {
            FunLog.d(TAG, "msgContent.sender : " + msgContent.sender);
            FunLog.d(TAG, "msgContent.seq : " + msgContent.seq);
            FunLog.d(TAG, "msgContent.str : " + msgContent.str);
            FunLog.d(TAG, "msgContent.arg3 : " + msgContent.arg3);
            FunLog.d(TAG, "msgContent.pData : " + msgContent.pData);
        }
        switch (msg.what) {
            case EUIMSG.SYS_GET_DEV_INFO_BY_USER_XM: {
                FunLog.i(TAG, "EUIMSG.SYS_GET_DEV_INFO_BY_USER_XM");

                if (msg.arg1 == FunError.EE_OK) {
                    // ??????????????????

                    // ???????????????????????????????????????
                    onUserInfoSavedAfterLoginSuccess();

                    // ????????????????????????????????????????????????
                    requestDeviceList();

                    // ??????
                    for (OnFunListener l : mListeners) {
                        if (l instanceof OnFunLoginListener) {
                            ((OnFunLoginListener) l).onLoginSuccess();
                        }
                    }
                } else {
                    // ??????????????????

                    // ??????
                    for (OnFunListener l : mListeners) {
                        if (l instanceof OnFunLoginListener) {
                            ((OnFunLoginListener) l).onLoginFailed(msg.arg1);
                        }
                    }
                }
            }
            break;
            case EUIMSG.SYS_CHECK_USER_REGISTE: {
                FunLog.i(TAG, "EUIMSG.SYS_CHECK_USER_REGISTE");
                if (msg.arg1 == FunError.EE_OK) {
                    //??????
                    for (OnFunListener l : mListeners) {
                        if (l instanceof OnFunRegisterListener) {
                            ((OnFunRegisterListener) l).onUserNameFine();
                        }
                    }
                } else {
                    for (OnFunListener l : mListeners) {
                        if (l instanceof OnFunRegisterListener) {
                            ((OnFunRegisterListener) l).onUserNameUnfine(msg.arg1);
                        }
                    }
                }
            }
            break;
            case EUIMSG.SYS_LOGOUT_TO_XM: {
                FunLog.i(TAG, "EUIMSG.SYS_LOGOUT_TO_XM");
                FunDevStatus devStatus = null;
                int idrState = FunSDK.GetDevState(msgContent.str, SDKCONST.EFunDevStateType.IDR);
                if (idrState == SDKCONST.EFunDevState.SLEEP) {
                    devStatus = FunDevStatus.STATUS_SLEEP;
                }else if (idrState == 3) {
                    devStatus = FunDevStatus.STATUS_CAN_NOT_WAKE_UP;
                }else {
                    devStatus = FunDevStatus.STATUS_ONLINE;
                }
                if (msg.arg1 == FunError.EE_OK) {

                    // ??????
                    for (OnFunListener l : mListeners) {
                        if (l instanceof OnFunGetUserInfoListener) {
                            ((OnFunGetUserInfoListener) l).onLogoutSuccess();
                        }
                        if (l instanceof OnFunDeviceWakeUpListener) {
                            ((OnFunDeviceWakeUpListener) l).onSleepResult(true,devStatus);
                        }
                    }
                } else {
                    for (OnFunListener l : mListeners) {
                        if (l instanceof OnFunGetUserInfoListener) {
                            ((OnFunGetUserInfoListener) l).onLogoutFailed(msg.arg1);
                        }
                    }
                }
            }
            break;
            case EUIMSG.SYS_SEND_EMAIL_CODE:
                FunLog.i(TAG, "EUIMSG.SYS_SEND_EMAIL_CODE");
            case EUIMSG.SYS_GET_PHONE_CHECK_CODE: {
                FunLog.i(TAG, "EUIMSG.SYS_GET_PHONE_CHECK_CODE");

                if (msg.arg1 == FunError.EE_OK) {
                    // ???????????????????????????
                    for (OnFunListener l : mListeners) {
                        if (l instanceof OnFunRegisterListener) {
                            ((OnFunRegisterListener) l).onRequestSendCodeSuccess();
                        }
                    }
                } else {
                    // ???????????????????????????
                    for (OnFunListener l : mListeners) {
                        if (l instanceof OnFunRegisterListener) {
                            ((OnFunRegisterListener) l).onRequestSendCodeFailed(msg.arg1);
                        }
                    }
                }
            }
            break;

            case EUIMSG.SYS_REGISTE_BY_EMAIL:
                FunLog.i(TAG, "EUIMSG.SYS_REGISTE_BY_EMAIL");
            case EUIMSG.SYS_REGISER_USER_XM: {
                FunLog.i(TAG, "EUIMSG.SYS_REGISER_USER_XM");

                // ???????????????????????????
                if (msg.arg1 == FunError.EE_OK) {
                    // ?????????????????????????????????
                    for (OnFunListener l : mListeners) {
                        if (l instanceof OnFunRegisterListener) {
                            ((OnFunRegisterListener) l).onRegisterNewUserSuccess();
                        }
                    }
                } else {
                    // ?????????????????????????????????
                    for (OnFunListener l : mListeners) {
                        if (l instanceof OnFunRegisterListener) {
                            ((OnFunRegisterListener) l).onRegisterNewUserFailed(msg.arg1);
                        }
                    }
                }
            }
            break;

            case EUIMSG.SYS_EDIT_PWD_XM: {
                FunLog.i(TAG, "EUIMSG.SYS_EDIT_PWD_XM");

                if (msg.arg1 == FunError.EE_OK) {
                    // ??????????????????
                    for (OnFunListener l : mListeners) {
                        if (l instanceof OnFunChangePasswListener) {
                            ((OnFunChangePasswListener) l).onChangePasswSuccess();
                        }
                    }
                } else {
                    // ??????????????????
                    for (OnFunListener l : mListeners) {
                        if (l instanceof OnFunChangePasswListener) {
                            ((OnFunChangePasswListener) l).onChangePasswFailed(msg.arg1);
                        }
                    }
                }

            }
            break;
            case EUIMSG.SYS_CHECK_PWD_STRENGTH: {
                FunLog.i(TAG, "EUIMSG.SYS_CHECK_PWD_STRENGTH");

                if (msg.arg1 == FunError.EE_OK) {
                    // ????????????????????????
                    for (OnFunListener l : mListeners) {
                        if (l instanceof OnFunCheckPasswListener) {
                            ((OnFunCheckPasswListener) l).onCheckPasswSuccess(msgContent.str);

                        }
                    }
                } else {
                    // ???????????????????????????
                    for (OnFunListener l : mListeners) {
                        if (l instanceof OnFunCheckPasswListener) {
                            ((OnFunCheckPasswListener) l).onCheckPasswFailed(msg.arg1, msgContent.str);
                        }
                    }
                }
            }
            break;
            case EUIMSG.SYS_SEND_EMAIL_FOR_CODE:
                FunLog.i(TAG, "EUIMSG.SYS_SEND_EMAIL_FOR_CODE");
            case EUIMSG.SYS_FORGET_PWD_XM: {
                FunLog.i(TAG, "EUIMSG.SYS_FORGET_PWD_XM");

                if (msg.arg1 == FunError.EE_OK) {
                    // ???????????????????????????????????????
                    for (OnFunListener l : mListeners) {
                        if (l instanceof OnFunForgetPasswListener) {
                            ((OnFunForgetPasswListener) l).onRequestCodeSuccess();
                        }
                    }

                } else {
                    // ???????????????????????????????????????
                    for (OnFunListener l : mListeners) {
                        if (l instanceof OnFunForgetPasswListener) {
                            ((OnFunForgetPasswListener) l).onRequestCodeFailed(msg.arg1);
                        }
                    }
                }
            }
            break;
            case EUIMSG.SYS_CHECK_CODE_FOR_EMAIL:
                FunLog.i(TAG, "EUIMSG.SYS_CHECK_CODE_FOR_EMAIL");
            case EUIMSG.SYS_REST_PWD_CHECK_XM: {
                FunLog.i(TAG, "EUIMSG.SYS_REST_PWD_CHECK_XM");

                if (msg.arg1 == FunError.EE_OK) {
                    // ?????????????????????
                    for (OnFunListener l : mListeners) {
                        if (l instanceof OnFunForgetPasswListener) {
                            ((OnFunForgetPasswListener) l).onVerifyCodeSuccess();
                        }
                    }

                } else {
                    // ?????????????????????
                    for (OnFunListener l : mListeners) {
                        if (l instanceof OnFunForgetPasswListener) {
                            ((OnFunForgetPasswListener) l).onVerifyFailed(msg.arg1);
                        }
                    }
                }
            }
            break;
            case EUIMSG.SYS_PSW_CHANGE_BY_EMAIL:
                FunLog.i(TAG, "EUIMSG.SYS_PSW_CHANGE_BY_EMAIL");
            case EUIMSG.SYS_RESET_PWD_XM: {
                FunLog.i(TAG, "EUIMSG.SYS_RESET_PWD_XM");

                if (msg.arg1 == FunError.EE_OK) {
                    // ??????????????????
                    for (OnFunListener l : mListeners) {
                        if (l instanceof OnFunForgetPasswListener) {
                            ((OnFunForgetPasswListener) l).onResetPasswSucess();
                        }
                    }

                } else {
                    // ??????????????????
                    for (OnFunListener l : mListeners) {
                        if (l instanceof OnFunForgetPasswListener) {
                            ((OnFunForgetPasswListener) l).onResetPasswFailed(msg.arg1);
                        }
                    }
                }
            }
            break;
            case EUIMSG.SYS_GET_USER_INFO: // ??????????????????
            {
                FunLog.i(TAG, "EUIMSG.SYS_GET_USER_INFO");

                if (msg.arg1 == FunError.EE_OK) {
                    // ????????????????????????,??????????????????????????????????????????
                    for (OnFunListener l : mListeners) {
                        if (l instanceof OnFunGetUserInfoListener) {
                            ((OnFunGetUserInfoListener) l).onGetUserInfoSuccess(msgContent.str);
                        }
                    }
                } else {
                    // ????????????????????????,??????????????????
                    for (OnFunListener l : mListeners) {
                        if (l instanceof OnFunGetUserInfoListener) {
                            ((OnFunGetUserInfoListener) l).onGetUserInfoFailed(msg.arg1);
                        }
                    }
                }
            }
            break;
            case EUIMSG.SYS_GET_DEV_STATE: // ????????????????????????
            {
                FunLog.i(TAG, "EUIMSG.SYS_GET_DEV_STATE");

                int seq = msgContent.seq;
                String devSn = msgContent.str;
                boolean isOnline = (msg.arg1 > 0);
                FunDevStatus devStatus = isOnline ? FunDevStatus.STATUS_ONLINE : FunDevStatus.STATUS_OFFLINE;
                FunDevice funDev = null;
                if (seq != 0) {
                    funDev = findDeviceById(seq);
                } else {
                    funDev = findDeviceBySn(devSn);
                }

                if (null != funDev) {
                    if (devStatus == FunDevStatus.STATUS_ONLINE) {
                        int idrState = FunSDK.GetDevState(msgContent.str, SDKCONST.EFunDevStateType.IDR);
                        if (idrState == SDKCONST.EFunDevState.SLEEP) {
                            devStatus = FunDevStatus.STATUS_SLEEP;
                        }else if (idrState == 3) {
                            devStatus = FunDevStatus.STATUS_CAN_NOT_WAKE_UP;
                        }
                    }
                    setDeviceStatus(devSn, devStatus, msg.arg1);
                    for (OnFunListener l : mListeners) {
                        if (l instanceof OnFunDeviceListener) {
                            ((OnFunDeviceListener) l).onDeviceStatusChanged(funDev);
                        }
                        if (l instanceof OnFunDeviceWakeUpListener) {
                            ((OnFunDeviceWakeUpListener) l).onDeviceState(devStatus);
                        }
                    }
                }
            }
            break;

            case EUIMSG.SYS_GET_DEV_INFO_BY_USER: // ????????????????????????
            {
                FunLog.i(TAG, "EUIMSG.SYS_GET_DEV_INFO_BY_USER");

                if (msg.arg1 >= 0) {

                    onUserInfoSavedAfterLoginSuccess();

                    // ??????
                    for (OnFunListener l : mListeners) {
                        if (l instanceof OnFunLoginListener) {
                            ((OnFunLoginListener) l).onLoginSuccess();
                        }
                    }

                    // ?????????????????????msg.arg1
                    updateDeviceList(msgContent.pData);

                    for (OnFunListener l : mListeners) {
                        if (l instanceof OnFunDeviceListener) {
                            ((OnFunDeviceListener) l).onDeviceListChanged();
                        }
                    }
                } else {
                    // ???????????????????????????????????????????????????????????????
                    // ??????
                    for (OnFunListener l : mListeners) {
                        if (l instanceof OnFunLoginListener) {
                            ((OnFunLoginListener) l).onLoginFailed(msg.arg1);
                        }
                    }

                }
            }
            break;

            case EUIMSG.SYS_ADD_DEVICE: // ??????????????????
            {
                if (msg.arg1 == FunError.EE_OK) {
                    // ????????????????????????
                    for (OnFunListener l : mListeners) {
                        if (l instanceof OnFunDeviceListener) {
                            ((OnFunDeviceListener) l).onDeviceAddedSuccess();
                        }
                    }
                } else {
                    // ????????????????????????
                    for (OnFunListener l : mListeners) {
                        if (l instanceof OnFunDeviceListener) {
                            ((OnFunDeviceListener) l).onDeviceAddedFailed(msg.arg1);
                        }
                    }
                }
            }
            break;
            case EUIMSG.SYS_DELETE_DEV: // ??????????????????
            {
                if (msg.arg1 == FunError.EE_OK) {
                    // ????????????????????????
                    for (OnFunListener l : mListeners) {
                        if (l instanceof OnFunDeviceListener) {
                            ((OnFunDeviceListener) l).onDeviceRemovedSuccess();
                        }
                    }
                } else {
                    // ????????????????????????
                    for (OnFunListener l : mListeners) {
                        if (l instanceof OnFunDeviceListener) {
                            ((OnFunDeviceListener) l).onDeviceRemovedFailed(msg.arg1);
                        }
                    }
                }
            }
            break;

            case EUIMSG.SYS_CHANGEDEVINFO: // ??????????????????(????????????)
            {
                FunDevice funDev = findDeviceById(msgContent.seq);
                if (null != funDev) {
                    if (msg.arg1 == FunError.EE_OK) {
                        for (OnFunListener l : mListeners) {
                            if (l instanceof OnFunDeviceOptListener) {
                                ((OnFunDeviceOptListener) l).onDeviceChangeInfoSuccess(funDev);
                            }
                        }
                    } else {
                        for (OnFunListener l : mListeners) {
                            if (l instanceof OnFunDeviceOptListener) {
                                ((OnFunDeviceOptListener) l).onDeviceChangeInfoFailed(funDev, msg.arg1);
                            }
                        }
                    }
                }
            }
            break;

            case EUIMSG.DEV_AP_CONFIG:
                if (msg.arg1 >= 0) {
                    SDK_CONFIG_NET_COMMON_V2 common = new SDK_CONFIG_NET_COMMON_V2();
                    G.BytesToObj(common, msgContent.pData);
                    FunDevice funDevice = addLanDevice(common);

                    if (null != funDevice) {
                        // ??????,????????????WiFi????????????
                        for (OnFunListener l : mListeners) {
                            if (l instanceof OnFunDeviceWiFiConfigListener) {
                                ((OnFunDeviceWiFiConfigListener) l).onDeviceWiFiConfigSetted(funDevice);
                            }
                        }
                    }
                }
                break;

            case EUIMSG.DEV_SEARCH_DEVICES: // ??????????????????????????????
            {
                int length = msg.arg2;
                if (length > 0) {
                    SDK_CONFIG_NET_COMMON_V2[] searchResult = new SDK_CONFIG_NET_COMMON_V2[length];
                    for (int i = 0; i < searchResult.length; i++) {
                        searchResult[i] = new SDK_CONFIG_NET_COMMON_V2();
                    }
                    G.BytesToObj(searchResult, msgContent.pData);
                    for (SDK_CONFIG_NET_COMMON_V2 sdk_config_net_common_v2 : searchResult) {
                        if (sdk_config_net_common_v2.st_15_DeviceType == -1) {
                            sdk_config_net_common_v2.st_15_DeviceType = 0;
                        }
                    }
                    updateLanDeviceList(searchResult);
                } else {
                    updateLanDeviceList(null);
                }

                for (OnFunListener l : mListeners) {
                    if (l instanceof OnFunDeviceListener) {
                        ((OnFunDeviceListener) l).onLanDeviceListChanged();
                    }
                }
            }
            break;

            case EUIMSG.DEV_LOGIN: // ????????????
            {
                FunLog.i(TAG, "EUIMSG.DEV_LOGIN");

                int seq = msgContent.seq;
                FunDevice funDev = findDeviceById(seq);
                if (null != funDev) {
                    if (msg.arg1 == FunError.EE_OK) {

                        // ????????????????????????
                        setDeviceHasLogin(funDev.getDevSn(), true);

                        // ?????????????????????????????????
                        mCurrDevice = funDev;

//					mVerificationPassword = 2;

                        for (OnFunListener l : mListeners) {
                            if (l instanceof OnFunDeviceOptListener) {
                                ((OnFunDeviceOptListener) l).onDeviceLoginSuccess(funDev);
                            }
                        }
                    } else {

                        mCurrDevice = null;
//					if (mVerificationPassword == 0) {
//						//???????????????
//						mVerificationPassword = 2;
                        for (OnFunListener l : mListeners) {
                            if (l instanceof OnFunDeviceOptListener) {
                                ((OnFunDeviceOptListener) l).onDeviceLoginFailed(funDev, msg.arg1);
                            }
                        }
//					}else {
//						//???????????????????????????
//						requestDeviceLogin(funDev);
//					}
                    }
                } else {
                    FunLog.e(TAG, "Recive -> EUIMSG.DEV_LOGIN, but no device matched.");
                }
            }
            break;
            case EUIMSG.DEV_GET_CONFIG: {
                FunLog.i(TAG, "EUIMSG.DEV_GET_CONFIG");

                if (msg.arg1 >= 0 && msgContent.arg3 == ECONFIG.CFG_ATHORITY) {
                    SDK_Authority authority = new SDK_Authority();
                    G.BytesToObj(authority, msgContent.pData);
                    if ((authority.st_1_Ability & 1) != 1) {

                    }
                }
            }
            break;

            case EUIMSG.DEV_GET_JSON: // ??????????????????
            case EUIMSG.DEV_CMD_EN: {

                FunLog.i(TAG, "EUIMSG.DEV_GET_JSON");

                FunDevice funDevice = findDeviceById(msgContent.seq);
                if (null == funDevice) {
                    funDevice = mCurrDevice;
                }

                if (null != funDevice) {

                    //???????????????????????????????????????????????????
                    if (ManualSnapModeJP.CLASSNAME.equals(msgContent.str)) {
                        if (msg.arg1 < 0) {
                            for (OnFunListener l : mListeners) {
                                if (l instanceof OnFunDeviceOptListener) {
                                    ((OnFunDeviceOptListener) l).onDeviceGetConfigFailed(
                                            funDevice, msg.arg1);
                                }
                            }
                        }
                        break;
                    }
                    requestDeviceConfigDone(funDevice, msgContent.str);
                    if (msg.arg1 < 0) {
                        for (OnFunListener l : mListeners) {
                            if (l instanceof OnFunDeviceOptListener) {
                                ((OnFunDeviceOptListener) l).onDeviceGetConfigFailed(
                                        funDevice, msg.arg1);
                            }
                        }
                    } else if (null != msgContent.pData) {
                        // ??????JSON
                        String json = G.ToString(msgContent.pData);
                        FunLog.i(TAG, "EUIMSG.DEV_GET_JSON --> json: " + json);
                        FunLog.i(TAG, "configName = " + msgContent.str);

                        if (StringUtils.contrast(msgContent.str, JsonConfig.GENERAL_LOCATION)) {
                            //??????????????????????????? ????????????????????????
                            HandleConfigData data = new HandleConfigData();
                            if (data.getDataObj(G.ToString(msgContent.pData), LocationBean.class)) {
                                LocationBean locationBean = (LocationBean) data.getObj();
                                if (locationBean != null) {
                                    DayLightTimeBean dayLightTimeBean = XUtils.getDayLightTimeInfo(TimeZone.getDefault());
                                    if (dayLightTimeBean != null) {
                                        if (dayLightTimeBean.useDLT) {
                                            locationBean.setdSTRule("On");
                                            DSTimeBean dstStart = new DSTimeBean();
                                            dstStart.setYear(dayLightTimeBean.year);
                                            dstStart.setMonth(dayLightTimeBean.beginMonth);
                                            dstStart.setDay(dayLightTimeBean.beginDay);
                                            DSTimeBean dstEnd = new DSTimeBean();
                                            dstEnd.setYear(dayLightTimeBean.beginMonth > dayLightTimeBean.endMonth
                                                    ? dayLightTimeBean.year + 1 : dayLightTimeBean.year);
                                            dstEnd.setMonth(dayLightTimeBean.endMonth);
                                            dstEnd.setDay(dayLightTimeBean.endDay);
                                            locationBean.setdSTStart(dstStart);
                                            locationBean.setdSTEnd(dstEnd);
                                        } else {
                                            locationBean.setdSTRule("Off");
                                        }
                                    }
                                    FunSDK.DevSetConfigByJson(getHandler(), funDevice.getDevSn(),
                                            JsonConfig.GENERAL_LOCATION,
                                            HandleConfigData.getSendData(JsonConfig.GENERAL_LOCATION,
                                                    "0x02", locationBean),
                                            -1, 5000, funDevice.getId());
                                }
                            }

                            for (OnFunListener l : mListeners) {
                                if (l instanceof OnFunDeviceOptListener) {
                                    ((OnFunDeviceOptListener) l)
                                            .onDeviceGetConfigSuccess(funDevice, msgContent.str, msgContent.seq);
                                }
                            }
                        }else if (DeviceGetJson.onParse(funDevice, msgContent.str, json)) {
                            // ??????????????????,?????????SystemInfo,msg.arg2???????????????
                            if (SystemInfo.CONFIG_NAME.equals(msgContent.str)) {
                                funDevice.setNetConnectType(msg.arg2);
                            }

                            for (OnFunListener l : mListeners) {
                                if (l instanceof OnFunDeviceOptListener) {
                                    ((OnFunDeviceOptListener) l)
                                            .onDeviceGetConfigSuccess(funDevice, msgContent.str, msgContent.seq);
                                }
                            }
                        } else {
                            for (OnFunListener l : mListeners) {
                                if (l instanceof OnFunDeviceOptListener) {
                                    ((OnFunDeviceOptListener) l)
                                            .onDeviceGetConfigFailed(funDevice,
                                                    FunError.EE_DVR_DEV_VER_NOMATCH);
                                }
                            }
                        }
                    }
                } else {
                    // ????????????????????????????????????,???????????????
                    FunLog.e(TAG, "Recive -> EUIMSG.DEV_GET_JSON, but no device matched.");
                }
            }
            break;
            case EUIMSG.DEV_SET_JSON: {
                FunLog.i(TAG, "EUIMSG.DEV_SET_JSON");

                FunDevice funDevice = findDeviceById(msgContent.seq);
                if (null != funDevice) {
                    if (msg.arg1 < 0) {
                        for (OnFunListener l : mListeners) {
                            if (l instanceof OnFunDeviceOptListener) {
                                ((OnFunDeviceOptListener) l).onDeviceSetConfigFailed(
                                        funDevice, msgContent.str, msg.arg1);
                            }
                            if (l instanceof OnAddSubDeviceResultListener) {
                                ((OnAddSubDeviceResultListener) l).onAddSubDeviceFailed(funDevice, msgContent);
                            }
                        }
                    } else {
                        for (OnFunListener l : mListeners) {
                            if (l instanceof OnFunDeviceOptListener) {
                                ((OnFunDeviceOptListener) l).onDeviceSetConfigSuccess(
                                        funDevice, msgContent.str);
                            }
                            if (l instanceof OnAddSubDeviceResultListener) {
                                ((OnAddSubDeviceResultListener) l).onAddSubDeviceSuccess(funDevice, msgContent);
                            }
                        }
                    }
                } else {
                    // ????????????????????????????????????,???????????????
                    FunLog.e(TAG, "Recive -> EUIMSG.DEV_SET_JSON, but no device matched.");
                }
            }
            break;
            case EUIMSG.DEV_GET_CHN_NAME: {                    //??????????????????
                FunLog.i(TAG, "EUIMSG.DEV_GET_CHN_NAME");

                FunDevice funDevice = findDeviceById(msgContent.seq);

                if (msg.arg1 > 0) {
                    if (msgContent.pData != null && msgContent.pData.length > 0) {
                        SDK_ChannelNameConfigAll Channel = new SDK_ChannelNameConfigAll();
                        G.BytesToObj(Channel, msgContent.pData);
                        Channel.nChnCount = msg.arg1;
                        funDevice.setChannel(Channel);
                    }
                }
            }
            break;
            case EUIMSG.DEV_OPTION: {
                FunLog.i(TAG, "EUIMSG.DEV_OPTION");

                FunDevice funDevice = findDeviceById(msgContent.seq);

                if (EDEV_OPTERATE.EDOPT_DEV_GET_IMAGE == msgContent.arg3) {
                    if (msg.arg1 < 0) {
                        for (OnFunListener l : mListeners) {
                            if (l instanceof OnFunDeviceCaptureListener) {
                                ((OnFunDeviceCaptureListener) l).onCaptureFailed(msg.arg1);
                            }
                        }
                    } else {
                        for (OnFunListener l : mListeners) {
                            if (l instanceof OnFunDeviceCaptureListener) {
                                ((OnFunDeviceCaptureListener) l).onCaptureSuccess(msgContent.str);
                            }
                        }
                    }
                } else if (EDEV_OPTERATE.EDOPT_DEV_OPEN_TANSPORT_COM == msgContent.arg3) {
                    if (msg.arg1 < 0) {
                        for (OnFunListener l : mListeners) {
                            if (l instanceof OnFunDeviceSerialListener) {
                                ((OnFunDeviceSerialListener) l)
                                        .onDeviceSerialOpenFailed(funDevice, msg.arg1);
                            }
                        }
                    } else {
                        for (OnFunListener l : mListeners) {
                            if (l instanceof OnFunDeviceSerialListener) {
                                ((OnFunDeviceSerialListener) l)
                                        .onDeviceSerialOpenSuccess(funDevice);
                            }
                        }
                    }
                } else if (EDEV_OPTERATE.EDOPT_DEV_TANSPORT_COM_READ == msgContent.arg3) {

                } else if (EDEV_OPTERATE.EDOPT_DEV_TANSPORT_COM_WRITE == msgContent.arg3) {
                    if (msg.arg1 < 0) {
                        for (OnFunListener l : mListeners) {
                            if (l instanceof OnFunDeviceSerialListener) {
                                ((OnFunDeviceSerialListener) l)
                                        .onDeviceSerialWriteFailed(funDevice, msg.arg1);
                            }
                        }
                    } else {
                        for (OnFunListener l : mListeners) {
                            if (l instanceof OnFunDeviceSerialListener) {
                                ((OnFunDeviceSerialListener) l)
                                        .onDeviceSerialWriteSuccess(funDevice);
                            }
                        }
                    }
                } else {
                    if (null != funDevice) {
                        if (msg.arg1 < 0) {
                            for (OnFunListener l : mListeners) {
                                if (l instanceof OnFunDeviceOptListener) {
                                    ((OnFunDeviceOptListener) l)
                                            .onDeviceOptionFailed(funDevice, msgContent.str, msg.arg1);
                                }
                            }
                        } else {
                            for (OnFunListener l : mListeners) {
                                if (l instanceof OnFunDeviceOptListener) {
                                    ((OnFunDeviceOptListener) l)
                                            .onDeviceOptionSuccess(funDevice, msgContent.str);
                                }
                            }
                        }
                    }
                }
            }
            break;

            case EUIMSG.DEV_FIND_FILE: // ????????????????????????(search file by file)
            {
                FunLog.i(TAG, "EUIMSG.DEV_FIND_FILE");

                FunDevice funDevice = findDeviceById(msgContent.seq);
                int fileNum = msg.arg1;
                if (fileNum < 0) {
                    for (OnFunListener l : mListeners) {
                        if (l instanceof OnFunDeviceRecordListener) {
                            ((OnFunDeviceRecordListener) l).onRequestRecordListFailed(msg.arg1);
                        }
                    }
                }
                DevCmdOPSCalendar cmdCalendar = (DevCmdOPSCalendar) funDevice.getConfig(DevCmdOPSCalendar.CONFIG_NAME);
                if (null != funDevice
                        && fileNum > 0
                        && null != msgContent.pData
                        && null != cmdCalendar) {
                    H264_DVR_FILE_DATA datas[] = new H264_DVR_FILE_DATA[msg.arg1];
                    for (int i = 0; i < datas.length; i++) {
                        datas[i] = new H264_DVR_FILE_DATA();
                    }

                    G.BytesToObj(datas, msgContent.pData);
                    SameDayPicInfo picInfo = cmdCalendar.getDayData(datas[0].st_3_beginTime);

                    if (null != picInfo) {
                        for (int i = 0; i < datas.length; i++) {
                            // FunLog.d("test", datas[i].toString());
                            picInfo.setPicData(datas[i]);
                        }

                        for (OnFunListener l : mListeners) {
                            if (l instanceof OnFunDeviceOptListener) {
                                ((OnFunDeviceOptListener) l).onDeviceFileListChanged(funDevice);
                            }
                        }
                    } else {
                        FunLog.e("DEV_FIND_FILE", "search file error!");
                    }

                } else if (cmdCalendar == null) {
                    if (msg.arg1 >= 0) {
                        H264_DVR_FILE_DATA[] datas = new H264_DVR_FILE_DATA[msg.arg1];
                        for (int i = 0; i < datas.length; i++) {
                            datas[i] = new H264_DVR_FILE_DATA();
                        }

                        G.BytesToObj(datas, msgContent.pData);

                        for (OnFunListener l : mListeners) {
                            if (l instanceof OnFunDeviceOptListener) {
                                ((OnFunDeviceOptListener) l).onDeviceFileListChanged(funDevice, datas);
                            }
                        }
                    } else {
                        for (OnFunListener l : mListeners) {
                            if (l instanceof OnFunDeviceOptListener) {
                                ((OnFunDeviceOptListener) l).onDeviceFileListGetFailed(funDevice);
                            }
                        }
                    }
                }
            }
            break;

            case EUIMSG.DEV_FIND_FILE_BY_TIME: //(search file by time)
            {
                FunLog.i(TAG, "DEV_FIND_FILE_BY_TIME");

                if (msg.arg1 < 0) {
                    for (OnFunListener l : mListeners) {
                        if (l instanceof OnFunDeviceRecordListener) {
                            ((OnFunDeviceRecordListener) l).onRequestRecordListFailed(msg.arg1);
                        }
                    }
                } else {

                    List<FunDevRecordFile> files = FunDevRecordFile.parseDevFilesByResult(msgContent.pData);

                    for (OnFunListener l : mListeners) {
                        if (l instanceof OnFunDeviceRecordListener) {
                            ((OnFunDeviceRecordListener) l).onRequestRecordListSuccess(files);
                        }
                    }
                }
            }
            break;
            case EUIMSG.DEV_SEARCH_PIC: {
                FunLog.i(TAG, "EUIMSG.DEV_SEARCH_PIC");

                if (msg.arg1 == FunError.EE_OK) {
                    // ??????????????????
                    String path = msgContent.str;
                    for (OnFunListener l : mListeners) {
                        if (l instanceof OnFunDeviceFileListener) {
                            ((OnFunDeviceFileListener) l).onDeviceFileDownCompleted(mCurrDevice,
                                    path, msgContent.seq);
                        }
                    }
                }
            }
            break;

            case EUIMSG.DEV_START_TALK: {
                FunLog.i(TAG, "EUIMSG.DEV_START_TALK");

                if (msg.arg1 < 0) {
                    for (OnFunListener l : mListeners) {
                        if (l instanceof OnFunDeviceTalkListener) {
                            ((OnFunDeviceTalkListener) l).onDeviceStartTalkFailed(msg.arg1);
                        }
                    }
                } else {
                    for (OnFunListener l : mListeners) {
                        if (l instanceof OnFunDeviceTalkListener) {
                            ((OnFunDeviceTalkListener) l).onDeviceStartTalkSuccess();
                        }
                    }
                }
            }
            break;
            case EUIMSG.DEV_STOP_TALK: {
                FunLog.i(TAG, "EUIMSG.DEV_STOP_TALK");

                if (msg.arg1 < 0) {
                    for (OnFunListener l : mListeners) {
                        if (l instanceof OnFunDeviceTalkListener) {
                            ((OnFunDeviceTalkListener) l).onDeviceStopTalkFailed(msg.arg1);
                        }
                    }
                } else {
                    for (OnFunListener l : mListeners) {
                        if (l instanceof OnFunDeviceTalkListener) {
                            ((OnFunDeviceTalkListener) l).onDeviceStopTalkSuccess();
                        }
                    }
                }
            }
            break;

            case EUIMSG.SAVE_IMAGE_FILE: {
                FunLog.i(TAG, "EUIMSG.SAVE_IMAGE_FILE");

                if (msg.arg1 == FunError.EE_OK) {
                    for (OnFunListener l : mListeners) {
                        if (l instanceof OnFunDeviceCaptureListener) {
                            ((OnFunDeviceCaptureListener) l).onCaptureSuccess(msgContent.str);
                        }
                    }
                } else {
                    for (OnFunListener l : mListeners) {
                        if (l instanceof OnFunDeviceCaptureListener) {
                            ((OnFunDeviceCaptureListener) l).onCaptureFailed(msg.arg1);
                        }
                    }
                }
            }
            break;
            // ????????????????????????
            case EUIMSG.ON_FILE_DLD_COMPLETE: {
                FunLog.i(TAG, "EUIMSG.ON_FILE_DLD_COMPLETE");

                for (OnFunListener l : mListeners) {
                    if (l instanceof OnFunDeviceFileListener) {
                        ((OnFunDeviceFileListener) l).onDeviceFileDownCompleted(
                                mCurrDevice, msgContent.str, msgContent.seq);
                    }
                }
            }
            break;
            // ????????????????????????
            case EUIMSG.ON_FILE_DLD_POS: {
                FunLog.i(TAG, "EUIMSG.ON_FILE_DLD_POS");

                for (OnFunListener l : mListeners) {
                    if (l instanceof OnFunDeviceFileListener) {
                        ((OnFunDeviceFileListener) l).onDeviceFileDownProgress(
                                msg.arg1, msg.arg2, msgContent.seq);
                    }
                }
            }
            break;
            // ????????????????????????
            case EUIMSG.ON_FILE_DOWNLOAD: {
                FunLog.i(TAG, "EUIMSG.ON_FILE_DOWNLOAD");

                for (OnFunListener l : mListeners) {
                    if (l instanceof OnFunDeviceFileListener) {
                        ((OnFunDeviceFileListener) l).onDeviceFileDownStart(
                                msg.arg1 == 0, msgContent.seq);
                    }
                }
            }
            break;

            // ??????????????????????????????
            case EUIMSG.DEV_ON_RECONNECT: {
                FunLog.i(TAG, "EUIMSG.DEV_ON_RECONNECT");
                if (msg.arg1 == FunError.EE_OK) {
                    FunDevice funDev = findDeviceBySn(msgContent.str);
                    if (null != funDev) {
                        setDeviceHasConnected(msgContent.str, true);
                        for (OnFunListener l : mListeners) {
                            if (l instanceof OnFunDeviceConnectListener) {
                                ((OnFunDeviceConnectListener) l).onDeviceReconnected(funDev);
                            }
                        }
                    }
                }
            }
            break;
            // ????????????????????????
            case EUIMSG.DEV_ON_DISCONNECT: {
                FunLog.i(TAG, "EUIMSG.DEV_ON_DISCONNECT");
                if (msg.arg1 == FunError.EE_OK) {
                    FunDevice funDev = findDeviceBySn(msgContent.str);
                    if (null != funDev) {
                        setDeviceHasConnected(msgContent.str, false);
                        for (OnFunListener l : mListeners) {
                            if (l instanceof OnFunDeviceConnectListener) {
                                ((OnFunDeviceConnectListener) l).onDeviceDisconnected(funDev);
                            }
                        }
                    }
                }
            }
            break;

            case EUIMSG.DEV_ON_TRANSPORT_COM_DATA: {
                // ??????????????????
                FunLog.i(TAG, "EUIMSG.DEV_ON_TRANSPORT_COM_DATA");

                FunDevice funDevice = findDeviceById(msgContent.seq);

                for (OnFunListener l : mListeners) {
                    if (l instanceof OnFunDeviceSerialListener) {
                        ((OnFunDeviceSerialListener) l).onDeviceSerialTransmitData(funDevice, msgContent.pData);
                    }
                }
            }
            break;

            // ??????????????????
            case EUIMSG.MC_ON_PictureCb:
            case EUIMSG.MC_ON_AlarmCb: {
                FunLog.e(TAG, TAG + "EUIMSG.MC_ON_AlarmCb");

                FunDevice funDev = findDeviceBySn(msgContent.str);
                AlarmInfo alarmInfo = null;
                if (msgContent.pData != null) {
                    alarmInfo = new AlarmInfo();
                    alarmInfo.onParse(G.ToString(msgContent.pData));
                }
                if (null != funDev) {
                    // ??????????????????
                    for (OnFunListener l : mListeners) {
                        if (l instanceof OnFunDeviceAlarmListener) {
                            ((OnFunDeviceAlarmListener) l).onDeviceAlarmReceived(funDev,alarmInfo);
                        }
                    }
                }
            }
            break;
            case EUIMSG.MC_LinkDev: // ????????????????????????
            {
                FunLog.i(TAG, "EUIMSG.MC_LinkDev");
            }
            break;
            case EUIMSG.MC_UnlinkDev: // ??????????????????
            {
                FunLog.i(TAG, "EUIMSG.MC_UnlinkDev");
            }
            break;
            case EUIMSG.MC_SearchAlarmInfo: // ??????????????????????????????
            {
                FunLog.i(TAG, "EUIMSG.MC_SearchAlarmInfo");

                FunDevice funDevice = findDeviceById(msgContent.seq);

                if (null != funDevice) {
                    if (msg.arg1 < 0) {
                        for (OnFunListener l : mListeners) {
                            if (l instanceof OnFunDeviceAlarmListener) {
                                ((OnFunDeviceAlarmListener) l).onDeviceAlarmSearchFailed(funDevice, msg.arg1);
                            }
                        }
                    } else {
                        List<AlarmInfo> infos = new ArrayList<AlarmInfo>();
                        AlarmInfo info = null;
                        int nNext[] = new int[1];
                        nNext[0] = 0;
                        int nStart = 0;
                        for (int i = 0; i < msgContent.arg3; ++i) {
                            String ret = G.ArrayToString(msgContent.pData, nStart, nNext);
                            nStart = nNext[0];
                            info = new AlarmInfo();
                            if (!info.onParse(ret)) {
                                if (!info.onParse("{" + ret))
                                    break;
                            }
                            // if (dateStr.equals(info.getStartTime().substring(0,
                            // 10))) {
                            infos.add(info);
                            // }
                        }

                        for (OnFunListener l : mListeners) {
                            if (l instanceof OnFunDeviceAlarmListener) {
                                ((OnFunDeviceAlarmListener) l).onDeviceAlarmSearchSuccess(funDevice, infos);
                            }
                        }
                    }
                }
            }
            break;


            // ?????????????????????
            case EUIMSG.DEV_SET_ATTR: {
                FunLog.i(TAG, "EUIMSG.DEV_SET_ATTR");
            }
            break;
            case EUIMSG.DEV_GET_LAN_ALARM: {
                // ???????????????????????????
                FunLog.i(TAG, "EUIMSG.DEV_GET_LAN_ALARM");

                // ??????????????????
                String devSn = msgContent.str;
                // ?????????????????????????????????????????????,??????????????????????????????????????????
                FunDevice funDevice = findDeviceBySn(devSn);

                if (null != funDevice) {
                    try {
                        String json = G.ToString(msgContent.pData);
                        FunLog.d(TAG, json);

                        // ??????????????????
                        AlarmInfo alarmInfo = new AlarmInfo();
                        alarmInfo.onParse(json);

                        // ??????,????????????????????????????????????????????????
                        for (OnFunListener l : mListeners) {
                            if (l instanceof OnFunDeviceAlarmListener) {
                                ((OnFunDeviceAlarmListener) l).onDeviceLanAlarmReceived(
                                        funDevice, alarmInfo);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
            case EUIMSG.DEV_WAKEUP: {
                FunDevice funDevice = null;
                if (msgContent.seq != 0) {
                    funDevice = findDeviceById(msgContent.seq);
                }
                FunDevStatus devStatus = null;
                if (funDevice != null) {
                    int idrState = FunSDK.GetDevState(funDevice.getDevSn(), SDKCONST.EFunDevStateType.IDR);
                    if (idrState == SDKCONST.EFunDevState.SLEEP) {
                        devStatus = FunDevStatus.STATUS_SLEEP;
                    } else if (idrState == 3) {
                        devStatus = FunDevStatus.STATUS_CAN_NOT_WAKE_UP;
                    } else {
                        devStatus = FunDevStatus.STATUS_ONLINE;
                    }
                    if (msg.arg1 >= 0) {
                        setDeviceHasLogin(funDevice.getDevSn(), true);
                    }
                }
                for (OnFunListener l : mListeners) {
                    if (l instanceof OnFunDeviceWakeUpListener) {
                        ((OnFunDeviceWakeUpListener) l)
                                .onWakeUpResult(msg.arg1 >= 0, devStatus);
                    }
                }
            }
                break;
            case DEV_SLEEP: {
                FunDevStatus devStatus = null;
                int idrState = FunSDK.GetDevState(msgContent.str, SDKCONST.EFunDevStateType.IDR);
                if (idrState == SDKCONST.EFunDevState.SLEEP) {
                    devStatus = FunDevStatus.STATUS_SLEEP;
                } else if (idrState == 3) {
                    devStatus = FunDevStatus.STATUS_CAN_NOT_WAKE_UP;
                } else {
                    devStatus = FunDevStatus.STATUS_ONLINE;
                }
                for (OnFunListener l : mListeners) {
                    if (l instanceof OnFunDeviceWakeUpListener) {
                        ((OnFunDeviceWakeUpListener) l)
                                .onSleepResult(msg.arg1 >= 0, devStatus);
                    }
                }
            }
                break;
            case EUIMSG.EMSG_DEV_START_UPLOAD_DATA:
                for (OnFunListener l : mListeners) {
                    if (l instanceof OnFunDevBatteryLevelListener) {
                        ((OnFunDevBatteryLevelListener) l).onRegister(msg.arg1 >= 0);
                    }
                }
                break;
            case EUIMSG.EMSG_DEV_STOP_UPLOAD_DATA:
                break;
            case EUIMSG.EMSG_DEV_ON_UPLOAD_DATA:
                parseBatteryState(G.ToString(msgContent.pData));
                break;
            default:
                break;
        }

        return 0;
    }

}
