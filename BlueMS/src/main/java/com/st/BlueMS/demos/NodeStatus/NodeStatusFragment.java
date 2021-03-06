/*
 * Copyright (c) 2017  STMicroelectronics – All rights reserved
 * The STMicroelectronics corporate logo is a trademark of STMicroelectronics
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions
 *   and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of
 *   conditions and the following disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name nor trademarks of STMicroelectronics International N.V. nor any other
 *   STMicroelectronics company nor the names of its contributors may be used to endorse or
 *   promote products derived from this software without specific prior written permission.
 *
 * - All of the icons, pictures, logos and other images that are provided with the source code
 *   in a directory whose title begins with st_images may only be used for internal purposes and
 *   shall not be redistributed to any third party or modified in any way.
 *
 * - Any redistributions in binary form shall not include the capability to display any of the
 *   icons, pictures, logos and other images that are provided with the source code in a directory
 *   whose title begins with st_images.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 */

package com.st.BlueMS.demos.NodeStatus;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.st.BlueMS.R;
import com.st.BlueSTSDK.Feature;
import com.st.BlueSTSDK.Features.FeatureBattery;
import com.st.BlueSTSDK.Features.Field;
import com.st.BlueSTSDK.Node;
import com.st.BlueSTSDK.gui.demos.DemoDescriptionAnnotation;
import com.st.BlueSTSDK.gui.demos.DemoFragment;

/**
 * Display the battery status and the rssi value
 */
@DemoDescriptionAnnotation(name="Rssi & Battery",iconRes=R.drawable.demo_battery)
public class NodeStatusFragment extends DemoFragment implements Node.BleConnectionParamUpdateListener {

    private static final String BATTERY_CAPACITY = NodeStatusFragment.class.getName()+".BATTERY_CAPACITY";

    private static long RSSI_UPDATE_PERIOD_MS=1000;

    private Handler mUpdateRssiRequestQueue;
    private TextView mRssiText;
    private Runnable mAskNewRssi = new Runnable() {
        @Override
        public void run() {
            Node n = getNode();
            if(n!=null){
                n.readRssi();
                mUpdateRssiRequestQueue.postDelayed(mAskNewRssi,RSSI_UPDATE_PERIOD_MS);
            }//if
        }//run
    };

    private FeatureBattery mBatteryFeature;
    private TextView mBatteryStatusText;
    private TextView mBatteryPercentageText;
    private TextView mBatteryVoltageText;
    private ImageView mBatteryIcon;
    private TypedArray mBatteryChargingImagesArray;
    private TypedArray mBatteryChargeImagesArray;
    private TextView mBatteryCurrentText;

    private float mBatteryCapacity;

    private TextView mRemainingTime;

    /**
     * compute the remaing time in seconds
     * @param batteryCapacity battery capacity in mA/h
     * @param current current used by the system in mA
     * @return remaining time in seconds
     */
    private static float getRemainingTimeMinutes(float batteryCapacity, float current){
        if(current<0)
            return (batteryCapacity/(-current))*(60);
        return Float.NaN;
    }

    private FeatureBattery.FeatureBatteryListener mBatteryListener =
            new FeatureBattery.FeatureBatteryListener() {
                @Override
                public void onCapacityRead(FeatureBattery featureBattery, int batteryCapacity) {
                    mBatteryCapacity=batteryCapacity;
                }

                @Override
                public void onMaxAssorbedCurrentRead(FeatureBattery featureBattery, float current) {
                }

                @Override
                public void onUpdate(Feature f,Feature.Sample data) {
                    final Field[] fieldsDesc = f.getFieldsDesc();
                    final Resources res = NodeStatusFragment.this.getResources();
                    float percentage = FeatureBattery.getBatteryLevel(data);
                    final FeatureBattery.BatteryStatus status = FeatureBattery.getBatteryStatus(data);
                    float voltage = FeatureBattery.getVoltage(data);
                    float current = FeatureBattery.getCurrent(data);
                    int batteryIconId;
                    if(status== FeatureBattery.BatteryStatus.Charging) {
                        int iconIndex = (((int) percentage) * mBatteryChargingImagesArray.length()) / 100;
                        batteryIconId = mBatteryChargingImagesArray.getResourceId(iconIndex,-1);
                    }else {
                        int iconIndex = (((int) percentage) * mBatteryChargeImagesArray.length()) / 100;
                        batteryIconId = mBatteryChargeImagesArray.getResourceId(iconIndex,-1);
                    }
                                        
                    final Drawable icon = ContextCompat.getDrawable(NodeStatusFragment.this.getActivity(),batteryIconId);
                    final String batteryStatus = "Status: "+status;

                    final String batteryPercentage = res.getString(R.string.nodeStatus_battery_percentage,
                            percentage,fieldsDesc[FeatureBattery.PERCENTAGE_INDEX].getUnit());
                    final String batteryVoltage = res.getString(R.string.nodeStatus_battery_voltage,
                            voltage, fieldsDesc[FeatureBattery.VOLTAGE_INDEX].getUnit());
                    final String batteryCurrent = res.getString(R.string.nodeStatus_battery_current,
                            current, fieldsDesc[FeatureBattery.CURRENT_INDEX].getUnit());

                    float remainingBattery = mBatteryCapacity * (percentage/100.0f);
                    float remainingTime = getRemainingTimeMinutes(remainingBattery,current);
                    final String remainingTimeStr = Float.isNaN(remainingTime) ? "" :
                            res.getString(R.string.nodeStatus_battery_remainingTime,
                            remainingTime);

                    updateGui(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                mBatteryStatusText.setText(batteryStatus);
                                mBatteryPercentageText.setText(batteryPercentage);
                                mBatteryIcon.setImageDrawable(icon);
                                mBatteryVoltageText.setText(batteryVoltage);
                                mBatteryCurrentText.setText(batteryCurrent);
                                if(displayRemainingTime(status)) {
                                    mRemainingTime.setText(remainingTimeStr);
                                }else{
                                    mRemainingTime.setText("");
                                }
                            }catch (NullPointerException e){
                                //this exception can happen when the task is run after the fragment is
                                // destroyed
                            }
                        }
                    });
                }//onUpdate
    };

    private boolean displayRemainingTime(FeatureBattery.BatteryStatus status) {
        return status!= FeatureBattery.BatteryStatus.Charging;
    }

    public NodeStatusFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate (Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        HandlerThread looper = new HandlerThread(NodeStatusFragment.class.getName()+".UpdateRssi");
        looper.start();
        mUpdateRssiRequestQueue = new Handler(looper.getLooper());

        loadBatteryCapacity(savedInstanceState);
    }

    private void loadStdConsumedCurrent() {
        mBatteryFeature.readMaxAbsorbedCurrent();

    }

    private void loadBatteryCapacity(@Nullable Bundle savedInstanceState) {
        if(savedInstanceState==null) {
            mBatteryCapacity = Float.NaN;
        }else{
            mBatteryCapacity = savedInstanceState.getFloat(BATTERY_CAPACITY,Float.NaN);
        }
    }

    private void loadBatteryCapacity() {
        if(!Float.isNaN(mBatteryCapacity))
            return;
        mBatteryFeature.readBatteryCapacity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        Resources res = getResources();
        View root = inflater.inflate(R.layout.fragment_node_status, container, false);

        mRssiText = (TextView) root.findViewById(R.id.rssiText);

        mBatteryPercentageText = (TextView) root.findViewById(R.id.batteryPercentageText);
        mBatteryStatusText = (TextView) root.findViewById(R.id.batteryStatusText);
        mBatteryVoltageText = (TextView) root.findViewById(R.id.batteryVoltageText);
        mBatteryCurrentText = (TextView) root.findViewById(R.id.batteryCurrentText);
        mBatteryIcon = (ImageView) root.findViewById(R.id.batteryImage);
        mBatteryChargingImagesArray = res.obtainTypedArray(R.array.batteryChargingIcon);
        mBatteryChargeImagesArray = res.obtainTypedArray(R.array.batteryChargeIcon);
        mRemainingTime  = (TextView) root.findViewById(R.id.batteryRemainingTimeText);

        //ask to add our option to the menu
        setHasOptionsMenu(true);

        return root;
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_node_status_demo, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.batteryInfo_showInfo) {
            showBatteryInfoDialog();
            return true;
        }//else
        return super.onOptionsItemSelected(item);
    }

    private void showBatteryInfoDialog(){

        final BatteryInfoDialogFragment dialog = BatteryInfoDialogFragment.newInstance(getNode());
        dialog.show(getFragmentManager(),"batteryInfoDialog");

    }

    @Override
    protected void enableNeededNotification(@NonNull Node node) {
        mBatteryFeature = node.getFeature(FeatureBattery.class);
        node.addBleConnectionParamListener(this);
        mUpdateRssiRequestQueue.postDelayed(mAskNewRssi, RSSI_UPDATE_PERIOD_MS);
        if(mBatteryFeature!=null){
            mBatteryFeature.addFeatureListener(mBatteryListener);
            node.enableNotification(mBatteryFeature);
            loadBatteryCapacity();
            loadStdConsumedCurrent();
        }else
            mBatteryVoltageText.setText(R.string.nodeStatus_battery_notFound);
    }

    @Override
    protected void disableNeedNotification(@NonNull Node node) {
        node.removeBleConnectionParamListener(this);
        mUpdateRssiRequestQueue.removeCallbacks(mAskNewRssi);
        if(mBatteryFeature!=null){
            mBatteryFeature.removeFeatureListener(mBatteryListener);
            node.disableNotification(mBatteryFeature);
        }
    }

    @Override
    public void onRSSIChanged(Node node, final int newRSSIValue) {
        updateGui(new Runnable() {
            @Override
            public void run() {
                mRssiText.setText("Rssi: "+newRSSIValue+" [dbm]");
            }//run
        });
    }//onRSSIChanged

    @Override
    public void onTxPowerChange(Node node, int newPower) {}

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putFloat(BATTERY_CAPACITY,mBatteryCapacity);
    }
}
