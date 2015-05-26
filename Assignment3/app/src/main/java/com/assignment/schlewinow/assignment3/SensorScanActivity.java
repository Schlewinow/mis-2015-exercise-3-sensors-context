package com.assignment.schlewinow.assignment3;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Display;
import android.view.ViewGroup;
import android.widget.SeekBar;

/**
 * Activity to visualize acceleration sensor output.
 */
public class SensorScanActivity extends Activity
{
    public static int INSTANCE_COUNTER = 0;

    /**
     * View to output sensor data in some bars.
     */
    @SuppressWarnings("all")
    private SensorBarView mSensorBarOutput;

    /**
     * View to output magnitude as fourier transform.
     */
    private SensorFFTView mSensorFFTOutput;

    /**
     * Receiver to receive broadcast messages.
     */
    private SensorReceiver mReceiver = null;

    /**
     * Filter used when receiving broadcast messages.
     */
    private IntentFilter mFilter = null;

    /**
     * Creation callback of this activity.
     * @param savedInstanceState Saved state of the activity instance.
     */
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_scan);

        // http://stackoverflow.com/questions/1016896/get-screen-dimensions-in-pixels
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int longerSize = size.y;

        this.mSensorBarOutput = (SensorBarView)this.findViewById(R.id.SensorOutput);
        this.mSensorFFTOutput = (SensorFFTView)this.findViewById(R.id.SensorFFT);

        // Dynamic views size depending on resolution, yeah baby!
        this.mSensorBarOutput.setDesiredViewSize(longerSize * 2 / 5, longerSize * 2 / 5);

        SeekBar sampleRateSeekBar = (SeekBar)this.findViewById(R.id.SampleRateBar);
        sampleRateSeekBar.setMax(3);
        sampleRateSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                int newSampleRate = 0;
                switch (seekBar.getProgress()) {
                    case 0:
                        newSampleRate = SensorManager.SENSOR_DELAY_FASTEST; // 0 microseconds
                        break;
                    case 1:
                        newSampleRate = SensorManager.SENSOR_DELAY_GAME;    // 20.000 microseconds
                        break;
                    case 2:
                        newSampleRate = SensorManager.SENSOR_DELAY_UI;      // 60.000 microseconds
                        break;
                    case 3:
                        newSampleRate = SensorManager.SENSOR_DELAY_NORMAL;  // 200.000 microseconds
                        break;
                }

                SensorScanActivity.this.updateSampleRate(newSampleRate);
            }
        });

        SeekBar windowSizeSeekBar = (SeekBar)this.findViewById(R.id.WindowSizeBar);
        windowSizeSeekBar.setMax(longerSize * 2 / 5);
        windowSizeSeekBar.setProgress(windowSizeSeekBar.getMax() / 2);
        windowSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                SensorScanActivity.this.updateFTTViewSize(seekBar.getProgress());
            }
        });
        this.mSensorFFTOutput.setDesiredViewSize(windowSizeSeekBar.getProgress(), windowSizeSeekBar.getProgress());

        this.mReceiver = new SensorReceiver(this.mSensorBarOutput, this.mSensorFFTOutput);
        this.mFilter = new IntentFilter(Constants.ACTION_UPDATE);

        // Some stuff should really be just done once.
        if(INSTANCE_COUNTER == 0)
        {
            // Initialize background service.
            Intent serviceIntend = new Intent(this, SensorNotificationService.class);
            this.startService(serviceIntend);
        }
        INSTANCE_COUNTER++;
    }

    /**
     * Update the sample rate of the acceleration sensor.
     * @param sampleRate The new sample rate to be used from now on.
     */
    private void updateSampleRate(int sampleRate)
    {
        Intent localIntent = new Intent(Constants.ACTION_SAMPLERATE);
        localIntent.putExtra(Constants.DATA_SAMPLERATE, sampleRate);
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    /**
     * Update size of the FFT-view.
     * @param size The desired size of the FTT view. Will try to match, but no promises.
     */
    private void updateFTTViewSize(int size)
    {
        mSensorFFTOutput.setDesiredViewSize(size, size);
        mSensorFFTOutput.invalidate();

        // Somehow came to this conclusion due to multiple ideas.
        // http://stackoverflow.com/questions/5151056/is-it-possible-to-change-the-layout-width-and-height-in-android-at-run-time
        // http://stackoverflow.com/questions/2963152/android-how-to-resize-a-custom-view-programmatically
        // Actually, the new size should be set here, but instead I use setDesiredViewSize.
        // These calls are still necessary for the UI to update the view size.
        ViewGroup.LayoutParams params = SensorScanActivity.this.mSensorFFTOutput.getLayoutParams();
        SensorScanActivity.this.mSensorFFTOutput.setLayoutParams(params);
    }

    /**
     * Due to Android docs, it is necessary to reset listener of sensor after pause.
     */
    protected void onResume()
    {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(this.mReceiver, this.mFilter);
    }

    /**
     * Due to Android docs, it is necessary to reset listener of sensor after pause.
     */
    protected void onPause()
    {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(this.mReceiver);
    }

    /**
     * Receiver for data from the SensorNotificationIntend.
     */
    private class SensorReceiver extends BroadcastReceiver
    {
        /**
         * SensorBarView to be updated by this receiver.
         */
        private SensorBarView mSensorBarView = null;

        /**
         * SensorFFTView to be updated by this receiver.
         */
        private SensorFFTView mFourierView = null;

        /**
         * Constructor needs views handled by the receiver.
         * @param barView SensorBarView to be updated by this receiver.
         * @param fourierView SensorFFTView to be updated by this receiver.
         */
        public SensorReceiver(SensorBarView barView, SensorFFTView fourierView)
        {
            super();
            this.mSensorBarView = barView;
            this.mFourierView = fourierView;
        }

        /**
         * Callback for incoming data.
         * @param context Context.
         * @param intent Intent send from the broadcaster.
         */
        public void onReceive(Context context, Intent intent)
        {
            if(intent.getAction().equals(Constants.ACTION_UPDATE))
            {
                // Update SensorBarView.
                this.mSensorBarView.setX(intent.getExtras().getInt(Constants.DATA_X));
                this.mSensorBarView.setY(intent.getExtras().getInt(Constants.DATA_Y));
                this.mSensorBarView.setZ(intent.getExtras().getInt(Constants.DATA_Z));
                this.mSensorBarView.setMagnitude(intent.getExtras().getInt(Constants.DATA_M));
                this.mSensorBarView.invalidate();

                double[] fourierData =intent.getExtras().getDoubleArray(Constants.DATA_FOURIER);
                this.mFourierView.setFourierData(fourierData);
                this.mFourierView.invalidate();
            }
        }
    }
}
