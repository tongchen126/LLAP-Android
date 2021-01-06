package com.example.llap_android;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
public class MotionSensorRecord implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor motionSensor;
    private onSensorChangedEvent sensorChangedEvent;
    public MotionSensorRecord(SensorManager sm,int sensor_type){
            sensorManager = sm;
            motionSensor = sm.getDefaultSensor(sensor_type);
            sensorManager.registerListener(this,motionSensor,1000);
    }
    public interface onSensorChangedEvent {
        public void onSensorChanged(SensorEvent event);
        public void onAccuracyChanged(Sensor sensor, int accuracy);
    }
    public void registeronSensorChangedEvent(onSensorChangedEvent onChangedevent){
        sensorChangedEvent = onChangedevent;
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        sensorChangedEvent.onSensorChanged(event);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        sensorChangedEvent.onAccuracyChanged(sensor,accuracy);
    }
    public void unregisteronSensorChangedEvent(){
        sensorManager.unregisterListener(this);
    }
}
