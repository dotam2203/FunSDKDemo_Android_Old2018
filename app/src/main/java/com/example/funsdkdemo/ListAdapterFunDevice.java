package com.example.funsdkdemo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.funsdkdemo.manager.SysAbilityManager;
import com.lib.funsdk.support.FunSupport;
import com.lib.funsdk.support.models.FunDevStatus;
import com.lib.funsdk.support.models.FunDevice;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import static com.example.funsdkdemo.manager.SysAbilityManager.CLOUD_EXPIRED;
import static com.example.funsdkdemo.manager.SysAbilityManager.CLOUD_NORMAL;
import static com.example.funsdkdemo.manager.SysAbilityManager.CLOUD_NOT_OPEND;
import static com.example.funsdkdemo.manager.SysAbilityManager.CLOUD_NOT_SUPPORT;

public class ListAdapterFunDevice extends BaseExpandableListAdapter {

	public interface OnFunDeviceItemClickListener {
		void onFunDeviceRenameClicked(FunDevice funDevice);

		void onFunDeviceConnectClicked(FunDevice funDevice);

		void onFunDeviceControlClicked(FunDevice funDevice);

		void onFunDeviceAlarmClicked(FunDevice funDevice);

		void onFunDeviceTransComClicked(FunDevice funDevice);

		void onFunDeviceRemoveClicked(FunDevice funDevice);

		void onFunDevice433AddSub(FunDevice funDevice);

		void onFunDevice433Control(FunDevice funDevice);

		void onFunDeviceWakeUp(FunDevice funDevice);

		void onFunDeviceCloud(FunDevice funDevice);

		void onFunDeviceTest(FunDevice funDevice);
	}

	private Context mContext = null;
	private LayoutInflater mInflater;
	private List<FunDevice> mListDevs;

	private boolean mCanRemoved = true;
	private boolean mCanRenamed = true;
	private boolean mNeedConnectAP = true;

	private OnFunDeviceItemClickListener mListener = null;

	public ListAdapterFunDevice(Context context, List<FunDevice> devList) {
		mContext = context;
		mInflater = LayoutInflater.from(context);
		mListDevs = devList;
	}

	/**
	 * ????????????????????????????????????????????????
	 * 
	 * @param removable
	 */
	public void setCanRemoved(boolean removable) {
		mCanRemoved = removable;
	}

	/**
	 * ??????????????????????????????WIFI(?????????????????????????????????)
	 * 
	 * @param needConnect
	 */
	public void setNeedConnectAP(boolean needConnect) {
		mNeedConnectAP = needConnect;
	}

	/**
	 * ?????????????????????????????????????????????????????????
	 * 
	 */
	public void setCanRenamed(boolean canBeRenamed) {
		mCanRenamed = canBeRenamed;
	}

	public void setOnFunDeviceItemClickListener(OnFunDeviceItemClickListener l) {
		mListener = l;
	}

	public Object getChild(int groupPosition, int childPosition) {
		return mListDevs.get(groupPosition).devIp;
	}

	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	public int getChildrenCount(int groupPosition) {
		return 1;
	}

	private boolean isDeviceConnected(FunDevice funDevice) {
		return FunSupport.getInstance().isAPDeviceConnected(funDevice);
	}

	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView,
			ViewGroup parent) {
		ChildViewHolder holder = null;
		if (null == convertView) {
			convertView = mInflater.inflate(R.layout.layout_device_list_item_more, null);

			holder = new ChildViewHolder();

			holder.txtDevType = (TextView) convertView.findViewById(R.id.txtDevType);
			holder.txtDevMac = (TextView) convertView.findViewById(R.id.txtDevMac);
			holder.txtDevSn = (TextView) convertView.findViewById(R.id.txtDevSn);
			holder.txtDevIp = (TextView) convertView.findViewById(R.id.txtDevIp);
			holder.txtDevStatusMore = (TextView) convertView.findViewById(R.id.txtDevStatusMore);

			holder.btnRename = (Button) convertView.findViewById(R.id.btnDevRename);
			holder.btnConnect = (Button) convertView.findViewById(R.id.btnDevConnect);
			holder.btnControl = (Button) convertView.findViewById(R.id.btnDevControl);
			holder.btnAlarm = (Button) convertView.findViewById(R.id.btnDevAlarm);
			holder.btnTransCom = (Button) convertView.findViewById(R.id.btnDevTransCom);
			holder.btnRemove = (Button) convertView.findViewById(R.id.btnDevRemove);
			holder.btn433Control = (Button) convertView.findViewById(R.id.btnDev433_setting);
			holder.btn433Add = (Button) convertView.findViewById(R.id.btnDev433Add);
			holder.btnDevWakeUp = convertView.findViewById(R.id.btnDevWakeUp);
			holder.btnTest = convertView.findViewById(R.id.btnTest);
			holder.ivCloud = convertView.findViewById(R.id.ivCloud);
			holder.tvCloud = convertView.findViewById(R.id.tvCloud);
			holder.llCloud = convertView.findViewById(R.id.llCloud);
			holder.btnRename.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					// ???????????????
					FunDevice funDevice = (FunDevice) v.getTag();
					if (null != funDevice && null != mListener) {
						mListener.onFunDeviceRenameClicked(funDevice);
					}
				}
			});
			holder.btnConnect.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					// ????????????
					FunDevice funDevice = (FunDevice) v.getTag();
					if (null != funDevice && null != mListener) {
						mListener.onFunDeviceConnectClicked(funDevice);
					}
				}
			});
			holder.btnControl.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					// ????????????
					FunDevice funDevice = (FunDevice) v.getTag();
					if (null != funDevice && null != mListener) {
						mListener.onFunDeviceControlClicked(funDevice);
					}
				}
			});
			holder.btnAlarm.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					// ??????????????????
					FunDevice funDevice = (FunDevice) v.getTag();
					if (null != funDevice && null != mListener) {
						mListener.onFunDeviceAlarmClicked(funDevice);
					}
				}
			});
			holder.btnTransCom.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					// ??????????????????
					FunDevice funDevice = (FunDevice) v.getTag();
					if (null != funDevice && null != mListener) {
						mListener.onFunDeviceTransComClicked(funDevice);
					}
				}
			});
			holder.btnRemove.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					// ????????????
					FunDevice funDevice = (FunDevice) v.getTag();
					if (null != funDevice && null != mListener) {
						mListener.onFunDeviceRemoveClicked(funDevice);
					}
				}
			});
			holder.btn433Control.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					FunDevice funDevice = (FunDevice) v.getTag();
					if (null != funDevice && null != mListener) {
						mListener.onFunDevice433Control(funDevice);
					}
				}
			});
			holder.btn433Add.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					FunDevice funDevice = (FunDevice) v.getTag();
					if (null != funDevice && null != mListener) {
						mListener.onFunDevice433AddSub(funDevice);
					}
				}
			});
			holder.btnDevWakeUp.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					FunDevice funDevice = (FunDevice) view.getTag();
					if (null != funDevice && null != mListener) {
						mListener.onFunDeviceWakeUp(funDevice);
					}
				}
			});
			holder.llCloud.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					FunDevice funDevice = (FunDevice) view.getTag();
					if (null != funDevice && null != mListener) {
						if (funDevice.getCloudState() == CLOUD_NOT_OPEND) {
							Toast.makeText(mContext,R.string.cloud_not_opend,Toast.LENGTH_LONG).show();
						}else if (funDevice.getCloudState() == CLOUD_NOT_SUPPORT) {
							Toast.makeText(mContext,R.string.cloud_unsupport,Toast.LENGTH_LONG).show();
						}else {
							mListener.onFunDeviceCloud(funDevice);
						}
					}
				}
			});
			holder.btnTest.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					// ????????????
					FunDevice funDevice = (FunDevice) view.getTag();
					if (null != funDevice && null != mListener) {
						mListener.onFunDeviceTest(funDevice);
					}
				}
			});
			if (mCanRenamed) {
				holder.btnRename.setVisibility(View.VISIBLE);
			} else {
				holder.btnRename.setVisibility(View.GONE);
			}

			if (mCanRemoved) {
				holder.btnRemove.setVisibility(View.VISIBLE);
			} else {
				holder.btnRemove.setVisibility(View.GONE);
			}

			if (mNeedConnectAP) {
				holder.btnConnect.setVisibility(View.VISIBLE);
			} else {
				holder.btnConnect.setVisibility(View.GONE);
			}

			convertView.setTag(holder);
		} else {
			holder = (ChildViewHolder) convertView.getTag();
		}

		FunDevice funDevice = mListDevs.get(groupPosition);

		holder.txtDevType.setText(funDevice.devType.getTypeStrId());
		holder.txtDevMac.setText(funDevice.getDevMac());
		holder.txtDevSn.setText(funDevice.getDevSn());
		holder.txtDevIp.setText(funDevice.getDevIP());
		holder.txtDevStatusMore.setText(funDevice.getStatusMore());

		holder.btnRename.setTag(funDevice);
		holder.btnConnect.setTag(funDevice);
		holder.btnControl.setTag(funDevice);
		holder.btnAlarm.setTag(funDevice);
		holder.btnTransCom.setTag(funDevice);
		holder.btnRemove.setTag(funDevice);
		holder.btn433Control.setTag(funDevice);
		holder.btn433Add.setTag(funDevice);
		holder.btnDevWakeUp.setTag(funDevice);
		holder.llCloud.setTag(funDevice);
		holder.btnTest.setTag(funDevice);
		if (isDeviceConnected(funDevice)) {
			holder.btnConnect.setText(R.string.device_opt_disconnect);
		} else {
			holder.btnConnect.setText(R.string.device_opt_connect);
		}

		if (funDevice.isRemote) {
			holder.btnAlarm.setVisibility(View.VISIBLE);
		} else {
			holder.btnAlarm.setVisibility(View.GONE);
		}

		updateCloudState(holder,funDevice);

		return convertView;
	}

	// group method stub
	public Object getGroup(int groupPosition) {
		return mListDevs.get(groupPosition);
	}

	public int getGroupCount() {
		return mListDevs.size();
	}

	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		GroupViewHolder holder = null;

		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.layout_device_list_item, null);

			holder = new GroupViewHolder();
			holder.imgDevIcon = (ImageView) convertView.findViewById(R.id.imgDevIcon);
			holder.txtDevName = (TextView) convertView.findViewById(R.id.txtDevName);
			holder.txtDevStatus = (TextView) convertView.findViewById(R.id.txtDevStatus);
			holder.imgArrowIcon = (ImageView) convertView.findViewById(R.id.imgArrowIcon);

			convertView.setTag(holder);
		} else {
			holder = (GroupViewHolder) convertView.getTag();
		}

		FunDevice funDevice = mListDevs.get(groupPosition);

		holder.imgDevIcon.setImageResource(funDevice.devType.getDrawableResId());
		holder.txtDevName.setText(funDevice.getDevName());

		if (isDeviceConnected(funDevice)) {
			// convertView.setBackgroundColor(mContext.getResources().getColor(R.color.item_selected));
			holder.txtDevName.setTextColor(mContext.getResources().getColorStateList(R.drawable.common_text_selector));
		} else {
			// convertView.setBackgroundColor(mContext.getResources().getColor(R.color.transparent));
			holder.txtDevName.setTextColor(mContext.getResources().getColorStateList(R.drawable.common_title_color));
		}

		holder.txtDevStatus.setText(funDevice.devStatus.getStatusResId());
		if (funDevice.devStatus == FunDevStatus.STATUS_ONLINE) {
			holder.txtDevStatus.setTextColor(0xff177fca);
		} else if (funDevice.devStatus == FunDevStatus.STATUS_OFFLINE) {
			holder.txtDevStatus.setTextColor(0xffda202e);
		} else {
			holder.txtDevStatus.setTextColor(mContext.getResources().getColor(R.color.demo_desc));
		}

		if (isExpanded) {
			holder.imgArrowIcon.setImageResource(R.drawable.icon_arrow_up);
		} else {
			holder.imgArrowIcon.setImageResource(R.drawable.icon_arrow_down);
		}

		return convertView;
	}

	private class GroupViewHolder {
		ImageView imgDevIcon;
		TextView txtDevName;
		TextView txtDevStatus;
		ImageView imgArrowIcon;
	}

	private class ChildViewHolder {
		TextView txtDevType;
		TextView txtDevMac;
		TextView txtDevSn;
		TextView txtDevIp;
		TextView txtDevStatusMore;
		Button btnRename;
		Button btnConnect;
		Button btnControl;
		Button btnAlarm;
		Button btnTransCom;
		Button btnRemove;
		Button btn433Control;
		Button btn433Add;
		Button btnDevWakeUp;
		Button btnTest;
		ImageView ivCloud;
		LinearLayout llCloud;
		TextView tvCloud;
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}

	@Override
	public boolean isChildSelectable(int arg0, int arg1) {
		return true;
	}

	/**
	 * ??? ??????
	 */
	private void updateCloudState(final ChildViewHolder viewHolder,final FunDevice funDevice) {

		SysAbilityManager.getInstance().isSupports(mContext,funDevice.getDevSn(),false,
				new SysAbilityManager.OnSysAbilityResultLisener<Map<String, Object>>() {
					@Override
					public void onSupportResult(Map<String, Object> isSupports) {
						if (isSupports != null) {
							boolean isCloudSupport = false;
							boolean isCloudEnable = false;
							boolean isCloudNormal = false;
							int cloudExpired = 0;
							if(isSupports.containsKey("xmc.service.support")) {
								isCloudSupport = (boolean) isSupports.get("xmc.service.support");
							}
							if (isSupports.containsKey("xmc.service.enable")) {
								isCloudEnable = (boolean) isSupports.get("xmc.service.enable");
							}
							if (isSupports.containsKey("xmc.service.normal")) {
								isCloudNormal = (boolean) isSupports.get("xmc.service.normal");
							}
							if (isSupports.containsKey("xmc.service.expiretime")) {
								cloudExpired = (int) isSupports.get("xmc.service.expiretime");
							}

							int cloudState = CLOUD_NOT_SUPPORT;
							if (isCloudNormal) {
								cloudState = CLOUD_NORMAL;
							}else if (isCloudEnable) {
								cloudState = CLOUD_EXPIRED;
							}else if (isCloudSupport) {
								cloudState = CLOUD_NOT_OPEND;
							}else {
								cloudState = CLOUD_NOT_SUPPORT;
							}

							funDevice.setCloudState(cloudState);
							funDevice.setCloudExpired(cloudExpired);

							changeCloudState(viewHolder,funDevice);
						}
					}
				},"xmc.service");
	}

	private void changeCloudState(final ChildViewHolder viewHolder,FunDevice funDevice) {
		if (viewHolder == null) {
			return;
		}
		switch (funDevice.getCloudState()) {
			case CLOUD_NORMAL: //??????????????????????????????
				viewHolder.ivCloud.setImageResource(R.drawable.cloud_using);
				StringBuffer sb = new StringBuffer();
				sb.append(mContext.getString(R.string.cloud_using));

				if (funDevice.getCloudExpired() > 0) {
					long expireTime = funDevice.getCloudExpired() * 1000L;
					Calendar calendar = Calendar.getInstance();
					calendar.setTimeInMillis(expireTime);
					SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");

					sb.append(mContext.getString(R.string.cloud_expire) +
							":" + format.format(calendar.getTime()));
				}

				viewHolder.tvCloud.setText(sb.toString());
				break;
			case CLOUD_EXPIRED: //??????????????????????????????
				viewHolder.ivCloud.setImageResource(R.drawable.cloud_warning);
				viewHolder.tvCloud.setText(R.string.cloud_warning);
				break;
			case -1:  //??????
			case CLOUD_NOT_SUPPORT:  //?????????
				viewHolder.ivCloud.setImageResource(R.drawable.cloud_unsupport);
				viewHolder.tvCloud.setText(R.string.cloud_unsupport);
				break;
			case CLOUD_NOT_OPEND: //???????????????
				viewHolder.ivCloud.setImageResource(R.drawable.cloud_unsupport);
				viewHolder.tvCloud.setText(R.string.cloud_not_opend);
				break;
		}
	}
}
