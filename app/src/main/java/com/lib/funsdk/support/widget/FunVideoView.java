package com.lib.funsdk.support.widget;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.opengl.GLSurfaceView;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.basic.G;
import com.lib.EFUN_ATTR;
import com.lib.EUIMSG;
import com.lib.FunSDK;
import com.lib.IFunSDKResult;
import com.lib.MsgContent;
import com.lib.funsdk.support.FunError;
import com.lib.funsdk.support.FunLog;
import com.lib.funsdk.support.FunPath;
import com.lib.funsdk.support.models.FunStreamType;
import com.lib.funsdk.support.utils.MyUtils;
import com.lib.sdk.struct.H264_DVR_FILE_DATA;
import com.lib.sdk.struct.H264_DVR_FINDINFO;
import com.lib.sdk.struct.SDK_FishEyeFrame;
import com.lib.sdk.struct.SDK_FishEyeFrameCM;
import com.lib.sdk.struct.SDK_FishEyeFrameSW;
import com.vatics.dewarp.FecCenter;
import com.vatics.dewarp.GL2JNIView.FecType;
import com.video.opengl.GLSurfaceView20;
import com.xmgl.vrsoft.VRSoftDefine.XMVRType;
import com.xmgl.vrsoft.VRSoftGLView;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.lib.EFUN_ATTR.EOA_MEDIA_YUV_USER;

//import com.android.gl2jni.GL2JNIView;

public class FunVideoView extends LinearLayout implements IFunSDKResult {

	private final String TAG = "FunVideoView";
	
	private final int STAT_STOPPED = 0;
	private final int STAT_PLAYING = 1;
	private final int STAT_PAUSED = 2;
	
	private int mPlayStat = STAT_STOPPED;
	private FunStreamType mStreamType = FunStreamType.STREAM_SECONDARY;
	private String mVideoUrl = null;
    private H264_DVR_FILE_DATA mVideoFile = null;
    private String mDeviceSn = null;
	private int mPlayerHandler = 0;
	private int mUserID = -1;
	private GLSurfaceView mSufaceView = null;
	private boolean mInited = false;
    public boolean bRecord = false;
    private String mFilePath;
    
    private int mPlayStartPos = 0;
    private int mPlayEndPos = 0;
    private int mPlayPosition = 0;
    private boolean mIsPrepared = false;
    private int mChannel = 0; 
    
    private OnPreparedListener mPreparedListener = null;
    private OnCompletionListener mCompletionListener = null;
    private OnErrorListener mErrorListener = null;
    private OnInfoListener mInfoListener = null;
    private OnTouchListener mOnTouchListener = null;
    
    private float fistXLocation;
    private float fistYlocation;
    private boolean Istrigger;
    private final int LENTH = 1;
    private long time;
    private Context mContext;

    // ??????????????????????????????????????????
	private boolean mIsPlaying = false;
	
	// ????????????????????????
	private boolean mIsFishEyeEnable = false;
	
	private SDK_FishEyeFrame mFishEyeFrame = null;
	private boolean mIsGetYUVData;
	private OnYUVDataListener mOnYUVDataListener;
	public FunVideoView(Context context) {
		super(context);
        mContext = context;
		init();
	}
	
	public FunVideoView(Context context, AttributeSet attrs) {
		super(context, attrs);
        mContext = context;
		init();
	}
	
	public FunVideoView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
        mContext = context;
		init();
	}
	
	public void setOnPreparedListener(OnPreparedListener listener) {
		mPreparedListener = listener;
	}
	
	public void setOnCompletionListener(OnCompletionListener listener) {
		mCompletionListener = listener;
	}
	
	public void setOnErrorListener(OnErrorListener listener) {
		mErrorListener = listener;
	}
	
	public void setOnInfoListener(OnInfoListener listener) {
		mInfoListener = listener;
	}
	
	public void setFishEye(boolean enable) {
		mIsFishEyeEnable = true;
	}
	
	private void init() {
        if (!isInEditMode()) {
            if ( mUserID == -1 ) {
                mUserID = FunSDK.RegUser(this);
            }
            
            mIsPlaying = false;
        }
	}
	
	private void initSurfaceView() {

//		DecoderManaer.SetEnableHDec(true);  //Open harddecode???

		if ( null == mSufaceView ) {
			// 1. ???????????????????????????
     		mSufaceView = new GLSurfaceView20(getContext());
     		mSufaceView.setLongClickable(true);

             LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                     LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
             this.addView(mSufaceView, lp);
            //???????????????????????????7.0 ??????????????????????????????????????????SufaceView
            mSufaceView.requestLayout();
         }
	}
	
	private void switchSurfaceview(SDK_FishEyeFrame fishEyeFrame) {
		if ( null == fishEyeFrame ) {
			if ( null == mSufaceView 
					|| !(mSufaceView instanceof GLSurfaceView20) ) {
				// ???????????????????????????
				FunSDK.SetIntAttr(mPlayerHandler, EFUN_ATTR.EOA_SET_MEDIA_VIEW_VISUAL, 0);
				
				if ( null != mSufaceView ) {
					this.removeView(mSufaceView);
				}
				mSufaceView = new GLSurfaceView20(getContext());
				mSufaceView.setLongClickable(true);

				LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
	                     LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
	             this.addView(mSufaceView, lp);
	             
				FunSDK.MediaSetPlayView(mPlayerHandler, mSufaceView, 0);
				FunSDK.SetIntAttr(mPlayerHandler, EFUN_ATTR.EOA_SET_MEDIA_VIEW_VISUAL, 1);
			}
		} else {
			if ( null == mSufaceView
					|| !(mSufaceView instanceof VRSoftGLView )) {
				// ?????????????????????
				FunSDK.SetIntAttr(mPlayerHandler, EFUN_ATTR.EOA_SET_MEDIA_VIEW_VISUAL, 0);

				if ( null != mSufaceView ) {
					this.removeView(mSufaceView);
                }
                mSufaceView = new VRSoftGLView(getContext());
                if (fishEyeFrame instanceof SDK_FishEyeFrameSW) {
                    FecCenter fecCenter = new FecCenter(((SDK_FishEyeFrameSW)fishEyeFrame).st_5_imageWidth,
                            ((SDK_FishEyeFrameSW)fishEyeFrame).st_6_imageHeight,
                            ((SDK_FishEyeFrameSW)fishEyeFrame).st_2_centerOffsetX,
                            ((SDK_FishEyeFrameSW)fishEyeFrame).st_3_centerOffsetY,
                            ((SDK_FishEyeFrameSW)fishEyeFrame).st_4_radius);

                    if (((SDK_FishEyeFrameSW)fishEyeFrame).st_1_lensType
                            == SDK_FishEyeFrameSW.FISHEYE_LENS_TYPE_E.SDK_FISHEYE_LENS_180VR) {
                        ((VRSoftGLView) mSufaceView).setType(XMVRType.XMVR_TYPE_180D);
                        ((VRSoftGLView) mSufaceView).setFecParams(FecType.GENERAL_180VR, fecCenter);
                    } else {
                        // ????????????360VR??????
                        ((VRSoftGLView) mSufaceView).setType(XMVRType.XMVR_TYPE_360D);
                        ((VRSoftGLView) mSufaceView).setFecParams(FecType.GENERAL_360VR, fecCenter);
                    }

                    //????????????
//					((VRSoftGLView) mSufaceView).setShape(VRSoftDefine.XMVRShape.Shape_Ball);
                    //????????????
//                    ((VRSoftGLView) mSufaceView).setCameraMount(VRSoftDefine.XMVRMount.Wall);
                }else {
                    if (fishEyeFrame instanceof SDK_FishEyeFrameCM) {
                        ((VRSoftGLView)mSufaceView).setType(XMVRType.XMVR_TYPE_SPE_CAM01);
                    }
                }
				LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
	                     LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
	             this.addView(mSufaceView, lp);

				FunSDK.MediaSetPlayView(mPlayerHandler, mSufaceView, 0);
				FunSDK.SetIntAttr(mPlayerHandler, EFUN_ATTR.EOA_SET_MEDIA_VIEW_VISUAL, 1);
			}
		}

	}
	
	private int getUserId() {
		return mUserID;
	}
	
	private void release() {
		stopPlayback();
		
		if ( -1 != mUserID ) {
			FunSDK.UnRegUser(mUserID);
			mUserID = -1;
		}
		
		mSufaceView = null;
	}
	
	/**
	 * ??????????????????,???JSON????????????
	 * @return
	 */
	public String getFishEyeFrameJSONString() {
		try {
			if ( null != mFishEyeFrame 
					&& (mFishEyeFrame instanceof SDK_FishEyeFrameSW ) ) {
				SDK_FishEyeFrameSW frame = (SDK_FishEyeFrameSW)mFishEyeFrame;
				JSONObject jsonObj = new JSONObject();
				jsonObj.put("lens", frame.st_1_lensType);
				jsonObj.put("x", frame.st_2_centerOffsetX);
				jsonObj.put("y", frame.st_3_centerOffsetY);
				jsonObj.put("r", frame.st_4_radius);
				jsonObj.put("w", frame.st_5_imageWidth);
				jsonObj.put("h", frame.st_6_imageHeight);
				return jsonObj.toString();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	/**
	 * ??????????????????
	 * @param path
	 */
	public void setVideoPath(String path) {
		mVideoUrl = path;
		mPlayStat = STAT_PLAYING;
		openVideo();
	}

	public void getYuvData(OnYUVDataListener listener) {
		mOnYUVDataListener = listener;
		mPlayStat = STAT_PLAYING;
		mIsGetYUVData = true;
		openVideo();
	}
	
	/**
	 * ???????????????IP(?????????AP????????????)????????????????????????SN(??????????????????????????????)??????????????????
	 * @param devSn ?????????IP(AP?????????)????????????SN(??????????????????)
	 */
	public void setRealDevice(String devSn, int channel) {
		String playUrl = null;
		mChannel = channel;
		if ( MyUtils.isIp(devSn) ) {
			// ???????????????IP??????,??????????????????
			playUrl = "real://" + devSn + ":34567";
		} else {
			playUrl = "real://" + devSn;
		}
		
		mDeviceSn = devSn;
		setVideoPath(playUrl);
	}
	
	/**
	 * ????????????(??????????????????)
	 * @param devSn
	 * @param absTime
	 */
	public void playRecordByTime(String devSn, int absTime) {
		String playUrl = "time://" + Integer.toString(absTime);
		mDeviceSn = devSn;
		setVideoPath(playUrl);
	}
	
	/**
	 * ????????????(????????????-????????????)
	 * @param devSn
	 * @param fromTime
	 * @param toTime
	 */
	public void playRecordByTime(String devSn, int fromTime, int toTime, int channel) {
		mChannel = channel;
		String playUrl = "time://" + Integer.toString(fromTime) + "-" + Integer.toString(toTime);
		mDeviceSn = devSn;
		setVideoPath(playUrl);
	}

    public void playRecordByFile(String devSn, H264_DVR_FILE_DATA file, int channel) {
    	mChannel = channel;
        String playUrl = "file://";
        mDeviceSn = devSn;
        mVideoFile = file;

        setVideoPath(playUrl);
    }

    public void seek(int absTime) {
		if ( mInited && mPlayerHandler != 0 ) {
			FunSDK.MediaSeekToTime(mPlayerHandler, 0, absTime, 0);
		}
	}
    
    public void seekbyfile(int absTime){
    	if ( mInited && mPlayerHandler != 0 ) {
			FunSDK.MediaSeekToPos(mPlayerHandler, absTime, 0);
		}
    }
	
	public void pause() {
		if ( isPlaying() ) {
			FunSDK.MediaPause(mPlayerHandler, 1, 0);
		}
		
		mPlayStat = STAT_PAUSED;
	}
	
	public void resume() {
		if ( mInited && mPlayerHandler != 0 ) {
			FunSDK.MediaPause(mPlayerHandler, 0, 0);
		}
	}
	
	/**
	 * ??????????????????
	 */
	public void stopPlayback() {
		if ( mPlayerHandler != 0 ) {
			FunSDK.MediaStop(mPlayerHandler);
			mPlayerHandler = 0;
		}
		mDeviceSn = null;
		mVideoUrl = null;
		mIsPlaying = false;
	}
	
	/**
	 * ??????????????????????????????
	 * @return
	 */
	public boolean isPaused() {
		return (mPlayStat == STAT_PAUSED);
	}
	
	/**
	 * ????????????????????????
	 * @return
	 */
	public boolean isPlaying() {
		return (mPlayStat == STAT_PLAYING && mInited && mPlayerHandler != 0 );
	}
	
	/**
	 * ???????????????/?????????
	 * @param streamType
	 */
	public void setStreamType(FunStreamType streamType) {
		mStreamType = streamType;
	}
	
	public FunStreamType getStreamType() {
		return mStreamType;
	}
	
	/**
	 * ????????????????????????,?????????
	 * @return
	 */
	public int getPosition() {
		return mPlayPosition;
	}
	
	/**
	 * ??????????????????????????????/??????,?????????
	 * @return
	 */
	public int getStartTime() {
		return mPlayStartPos;
	}
	
	/**
	 * ??????????????????????????????/??????,?????????
	 * @return
	 */
	public int getEndTime() {
		return mPlayEndPos;
	}
	
	private String getPlayPath(String url) {
		if ( url.contains("://") ) {
			return url.substring(mVideoUrl.indexOf("://") + 3);
		}
		return url;
	}
	
	private void openVideo() {
		if ( !mInited
				|| null == mVideoUrl 
				|| mPlayStat != STAT_PLAYING
				|| null == mSufaceView ) {
			return;
		}

		mFishEyeFrame = null;
		
		mIsPrepared = false;
		mPlayPosition = 0;
		
		String playPath = getPlayPath(mVideoUrl);
		if ( mVideoUrl.startsWith("real://") ) {
			if ( !mIsPlaying ) {
				// ??????????????????
				mPlayerHandler = FunSDK.MediaRealPlay(
						getUserId(),
						playPath,
						mChannel,
						mStreamType.getTypeId(), mSufaceView, 0);
//                FunSDK.SetIntAttr(mPlayerHandler, EFUN_ATTR.EOA_SET_MEDIA_DATA_USER, getUserId());
//                FunSDK.MediaSetFluency(mPlayerHandler, SDKCONST.EDECODE_TYPE.EDECODE_REAL_TIME_STREAM0, 0); //????????????????????????<-->?????????
				if (mIsGetYUVData) {
					//??????EOA_MEDIA_YUV_USER ???????????????????????????YUV?????? ????????????EUIMSG.ON_YUV_DATA:
					FunSDK.SetIntAttr(mPlayerHandler, EOA_MEDIA_YUV_USER, mUserID);
				}
			}
			mIsPlaying = true;
		} else if ( mVideoUrl.startsWith("time://") ) {
			if ( !mIsPlaying ) {
				// ????????????
				int fromTime = -1;
				int toTime = -1;
				if ( playPath.contains("-") ) {
					String[] tmStrs = playPath.split("-");
					fromTime = Integer.parseInt(tmStrs[0]);
					toTime = Integer.parseInt(tmStrs[1]);
				} else {
					fromTime = Integer.parseInt(playPath);
				}
				
				Date fromDate = new Date((long)fromTime*1000);
				H264_DVR_FINDINFO fileInfo = new H264_DVR_FINDINFO();
				
				fileInfo.st_0_nChannelN0 = mChannel;
				fileInfo.st_2_startTime.st_0_dwYear = fromDate.getYear()+1900;
				fileInfo.st_2_startTime.st_1_dwMonth = fromDate.getMonth()+1;
				fileInfo.st_2_startTime.st_2_dwDay = fromDate.getDate();
				fileInfo.st_2_startTime.st_3_dwHour = fromDate.getHours();
				fileInfo.st_2_startTime.st_4_dwMinute = fromDate.getMinutes();
				fileInfo.st_2_startTime.st_5_dwSecond = fromDate.getSeconds();
				if ( toTime > 0 && toTime > fromTime ) {
					Date toDate = new Date((long)toTime*1000);
					
					fileInfo.st_3_endTime.st_0_dwYear = toDate.getYear()+1900;
					fileInfo.st_3_endTime.st_1_dwMonth = toDate.getMonth()+1;
					fileInfo.st_3_endTime.st_2_dwDay = toDate.getDate();
					fileInfo.st_3_endTime.st_3_dwHour = toDate.getHours();
					fileInfo.st_3_endTime.st_4_dwMinute = toDate.getMinutes();
					fileInfo.st_3_endTime.st_5_dwSecond = toDate.getSeconds();
				} else {
					fileInfo.st_3_endTime.st_0_dwYear = fromDate.getYear()+1900;
					fileInfo.st_3_endTime.st_1_dwMonth = fromDate.getMonth()+1;
					fileInfo.st_3_endTime.st_2_dwDay = fromDate.getDate();
					fileInfo.st_3_endTime.st_3_dwHour = 23;
					fileInfo.st_3_endTime.st_4_dwMinute = 59;
					fileInfo.st_3_endTime.st_5_dwSecond = 59;
				}
				fileInfo.st_6_StreamType = mStreamType.getTypeId();
				
				mPlayerHandler = FunSDK.MediaNetRecordPlayByTime(
						getUserId(),
						mDeviceSn,
						G.ObjToBytes(fileInfo),
						mSufaceView, 0);
            }
            mIsPlaying = true;
		}else if ( mVideoUrl.startsWith("file://")) {
            if (!mIsPlaying) {
                mPlayerHandler = FunSDK.MediaNetRecordPlay(
                        getUserId(),
                        mDeviceSn,
                        G.ObjToBytes(mVideoFile),
                        mSufaceView, 0);
            }
            mIsPlaying = true;
        }
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
        if (changed || mInited == false) {
        	
        	initSurfaceView();
        	
            mInited = true;
            
            if ( mPlayStat == STAT_PLAYING
                    && null != mVideoUrl ) {
                openVideo();
            }
        }
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
	
	@Override
	protected void onDetachedFromWindow() {
		this.release();
		super.onDetachedFromWindow();
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		return super.onTouchEvent(event);
	}
	
	@Override
	public void setOnTouchListener(OnTouchListener l) {
		// TODO Auto-generated method stub
		mOnTouchListener = l;
		super.setOnTouchListener(l);
	}
	
	//here to intercept evention by some condition, to show or hide the button bar
	@Override  
    public boolean onInterceptTouchEvent(MotionEvent ev) {  
        // TODO Auto-generated method stub  
          
          
        int deltaX = 0;
        int deltaY = 0;
        long deltime = 0;
        int count = ev.getPointerCount();
        long times = ev.getEventTime();
        final float x = ev.getX();
        final float y = ev.getY();
  
        switch (ev.getAction()) {  
        case MotionEvent.ACTION_MOVE:
			fingerTouchMove(fistXLocation,fistYlocation,x,y);
            return super.onInterceptTouchEvent(ev);
        case MotionEvent.ACTION_DOWN:
			fistXLocation = x;
            fistYlocation = y;
            time = ev.getDownTime();
            if(getScaleY()<-400){
                System.out.println(getScaleY());
            }

            return  super.onInterceptTouchEvent(ev);
  
        case MotionEvent.ACTION_CANCEL:  
        case MotionEvent.ACTION_UP:  
        	System.out.println("TTTT-----ActionUP");
        	deltaX = (int)(fistXLocation - x);
            deltaY = (int)(fistYlocation - y);
            deltime = times - time;
            if (count == 1) {
				if (deltime < 100) {
	            	if (Math.abs(deltaY) < LENTH
	            			&& Math.abs(deltaX) < LENTH) {
	            		if (mOnTouchListener != null) {
	            			mOnTouchListener.onTouch(this, ev);
						}
	            	}  
				}
            }
            break;
        default:
            break;
        }
      return super.onInterceptTouchEvent(ev);  
    }  


    public void setMediaSound(boolean bSound) {
        FunSDK.MediaSetSound(mPlayerHandler, bSound ? 100 : 0, 0);
    }

    /**
	 * ???????????? 
	 * @param path
	 */
	public String captureImage(String path) {
		if ( 0 != mPlayerHandler ) {
			if ( null == path ) {
				// ????????????????????????
				path = FunPath.getCapturePath();
			}
			
			int result = FunSDK.MediaSnapImage(mPlayerHandler, path, 0);
			if ( result == 0 ) {
				return path;
			}
			return null;
		}
		
		return null;
	}

	/**
	 * ???????????????????????????
	 * @param path
	 */
    public void startRecordVideo(String path) {
        if (0 != mPlayerHandler) {

            if (!bRecord) {
                if (null == path) {
                    path = FunPath.getRecordPath();
                }
                mFilePath = path;
                bRecord = true;
                FunSDK.MediaStartRecord(mPlayerHandler,
                        mFilePath, 0);
            }
        }
    }

    public void stopRecordVideo() {
        if (0 != mPlayerHandler) {
            if (bRecord) {
                bRecord = false;
                FunSDK.MediaStopRecord(mPlayerHandler, 0);
            }

        }
    }
    
    public String getFilePath() {
    	return mFilePath;
    }

    private int parsePlayPosition(String str) {
    	try {
    		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
    		return (int)(sdf.parse(str).getTime()/1000);
    	} catch (Exception e) {
    		
    	}
    	return 0;
    }
    
    private int parsePlayBeginTime(String str) {
    	try {
    		if ( str.contains("=") ) {
    			str = str.substring(str.indexOf("=")+1);
    		}
    		
    		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.ENGLISH);
    		return (int)(sdf.parse(str).getTime()/1000);
    	} catch (Exception e) {
    		
    	}
    	return 0;
    }
    
    
	@Override
	public int OnFunSDKResult(Message msg, MsgContent msgContent) {
		FunLog.d(TAG, "msg.what : " + msg.what);
		FunLog.d(TAG, "msg.arg1 : " + msg.arg1 + " [" + FunError.getErrorStr(msg.arg1) + "]");
		FunLog.d(TAG, "msg.arg2 : " + msg.arg2);
		if ( null != msgContent ) {
			FunLog.d(TAG, "msgContent.sender : " + msgContent.sender);
			FunLog.d(TAG, "msgContent.seq : " + msgContent.seq);
			FunLog.d(TAG, "msgContent.str : " + msgContent.str);
			FunLog.d(TAG, "msgContent.arg3 : " + msgContent.arg3);
            FunLog.d(TAG, "msgContent.pData : " + msgContent.pData);
		}
		
		switch(msg.what) {
		case EUIMSG.START_PLAY:
			{
				FunLog.i(TAG, "EUIMSG.START_PLAY");
				if ( msg.arg1 >= FunError.EE_OK ) {
					// ????????????
					if ( null != msgContent.str ) {
						String[] infos = msgContent.str.split(";");
						
						if ( infos.length > 2 ) {
							mPlayStartPos = parsePlayBeginTime(infos[1]);
							mPlayEndPos = parsePlayBeginTime(infos[2]);
						}
						
					}
                    if ( msgContent.arg3 == 3 ) {
                        // DSS????????????
                        System.out.println("TTTTT------DSS");
                        Toast.makeText(mContext, "DSS", Toast.LENGTH_SHORT).show();
                    } else {
                        // ????????????
                    }
                } else {
					// ????????????
					if ( null != mErrorListener ) {
						mErrorListener.onError(null, 
								MediaPlayer.MEDIA_ERROR_UNKNOWN,
								msg.arg1);
					}
				}
				
			}
			break;
		case EUIMSG.STOP_PLAY:
			break;
		case EUIMSG.PAUSE_PLAY:
			break;
		case EUIMSG.SEEK_TO_TIME:
			{
				FunLog.i(TAG, "EUIMSG.SEEK_TO_TIME");
			}
			break;
		case EUIMSG.SEEK_TO_POS:
			{
				FunLog.i(TAG, "EUIMSG.SEEK_TO_POS");
			}
			break;
		case EUIMSG.ON_PLAY_INFO:
			{
				FunLog.i(TAG, "EUIMSG.ON_PLAY_INFO");
				if ( null != msgContent.str ) {
					String[] infos = msgContent.str.split(";");
					
					if ( infos.length > 0 ) {
						// ??????????????????
						mPlayPosition = parsePlayPosition(infos[0]);
					}
				}
			}
			break;
		case EUIMSG.ON_PLAY_END:
			{
				FunLog.i(TAG, "EUIMSG.ON_PLAY_END");
				if ( null != mCompletionListener ) {
					mCompletionListener.onCompletion(null);
				}
			}
			break;
		case EUIMSG.ON_PLAY_BUFFER_BEGIN:
			{
				FunLog.i(TAG, "EUIMSG.ON_PLAY_BUFFER_BEGIN");
				if ( null != mInfoListener ) {
					mInfoListener.onInfo(null, MediaPlayer.MEDIA_INFO_BUFFERING_START, mChannel);
				}
			}
			break;
		case EUIMSG.ON_PLAY_BUFFER_END:
			{
				FunLog.i(TAG, "EUIMSG.ON_PLAY_BUFFER_END");
				if ( null != mInfoListener ) {
					mInfoListener.onInfo(null, MediaPlayer.MEDIA_INFO_BUFFERING_END, mChannel);
				}
				
				if ( !mIsPrepared ) {
					mIsPrepared = true;
					if ( null != mPreparedListener ) {
						mPreparedListener.onPrepared(null);
					}
				}
			}
			break;
		case EUIMSG.SAVE_IMAGE_FILE:
			{
				FunLog.i(TAG, "EUIMSG.SAVE_IMAGE_FILE"); //???????????????????????????

			}
			break;
        case EUIMSG.START_SAVE_MEDIA_FILE:
            {
                FunLog.i(TAG, "EUIMSG.START_SAVE_MEDIA_FILE");
            }
            break;
        case EUIMSG.STOP_SAVE_MEDIA_FILE:
            {
                FunLog.i(TAG, "EUIMSG.START_SAVE_MEDIA_FILE");
            }
            break;
        case EUIMSG.ON_FRAME_USR_DATA:
            {
            	SDK_FishEyeFrame fishFrame = null;
            	// ??????????????????,??????VR??????
    			if (msgContent.pData != null && msgContent.pData.length > 8) {
    				if (msg.arg2 == 0x4) {
    					// // ????????????????????????
    					//dump(msgContent.pData);

    					SDK_FishEyeFrameSW fp = new SDK_FishEyeFrameSW();
    					byte[] pFishParam = new byte[msgContent.pData.length - 8];
    					System.arraycopy(msgContent.pData, 8, pFishParam, 0, pFishParam.length);

    					G.BytesToObj(fp, pFishParam);

    					fishFrame = fp;
    				}else if (msg.arg2 == 0x5){
						//??????????????????
						SDK_FishEyeFrameCM fp = new SDK_FishEyeFrameCM();
						byte[] pFishParam = new byte[msgContent.pData.length - 8];
						System.arraycopy(msgContent.pData, 8, pFishParam, 0, pFishParam.length);

						G.BytesToObj(fp, pFishParam);

						fishFrame = fp;
					}
    			}
    			
    			if ( null != fishFrame ) {
    				if (fishFrame instanceof SDK_FishEyeFrameSW) {
    					// ???????????????????????????
    					mFishEyeFrame = fishFrame;
    				}
                    switchSurfaceview(fishFrame);
                }
            }
        	break;
            case EUIMSG.ON_YUV_DATA:	// YUV CallBack, FunSDK.MediaRealPlay()???View???null????????????YUV??????
			{
				if (mOnYUVDataListener != null) {
					mOnYUVDataListener.onYUVData(msgContent.pData,msg.arg2,msgContent.arg3);
				}
			}
			break;
        default:
            break;
		}
		
		return 0;
	}
	
//	static int __frame_count = 0;
	public interface OnYUVDataListener {
    	void onYUVData(byte[] data,int width,int height);
	}

	private void fingerTouchMove(float downX,float downY,float moveX, float moveY) {
		float dx = Math.abs(moveX - downX);
		float dy = Math.abs(moveY - downY);
		float ratio = dy / dx;
		if (dy >= 50 || dx >= 50) {
			if (moveY > downY && moveX > downX) {
				if (ratio > 1.5) {
					Toast.makeText(mContext,"DOWN",Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(mContext,"RIGHT",Toast.LENGTH_LONG).show();
				}
			} else if (moveY > downY && moveX < downX) {
				if (ratio > 1.5) {
					Toast.makeText(mContext,"DOWN",Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(mContext,"LEFT",Toast.LENGTH_LONG).show();
				}
			} else if (moveY < downY && moveX < downX) {
				if (ratio > 1.5) {
					Toast.makeText(mContext,"UP",Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(mContext,"LEFT",Toast.LENGTH_LONG).show();
				}
			} else if (moveY < downY && moveX > downX) {
				if (ratio > 1.5) {
					Toast.makeText(mContext,"UP",Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(mContext,"RIGHT",Toast.LENGTH_LONG).show();
				}
			}
		}
	}
}
