package com.assignment.schlewinow.assignment3;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Background service to read sensor data and write notifications.
 * Data are broadcasted so that the views can be updated properly.
 */
public class SensorNotificationService extends IntentService
{
    /**
     *
     */
    private SensorManager mSenseMan;

    /**
     *
     */
    private Sensor mAccelerometer;

    /**
     * Listener of the accelerometer.
     */
    private AccelerationSensorListener mSensorListener;

    /**
     * Magnitude values collected over time. Used with FFT-view.
     */
    private double[] mMagnitudeValues = null;

    /**
     * Used to calculate fourier transform values.
     */
    private FFT mFourier = null;

    /**
     * Has to be power of 2.
     * Some other students mentioned that this might actually be the "FFT window size",
     * and not the view size. Well... sh*t happens. In that case it should have been called
     * "FFT interval size" instead.
     * Also: https://www.youtube.com/watch?v=XYRDsfxIhwc
     * (The part with the iHouse, especially.)
     */
    private final int mFourierSize = 256;

    /**
     * specific fparts of the fourier transform collected over time.
     * Used to analyze user's current activity.
     */
    private double[][] mFourierOverTime = null;

    /**
     * Notification ID. As long as a constant ID is used, the same notification may be updated.
     */
    private final int mID = 42;       // any number would do, so I picked 42

    /**
     * Used to block notification spam.
     */
    String mCurrentMessage = "";

    /**
     * Default constructor. Necessary, exception otherwise.
     */
    @SuppressWarnings("unused")
    public SensorNotificationService()
    {
        this("SensorNotificationService");
    }

    /**
     * Overwritten constructor from IntendService
     * @param name Name of the service.
     */
    public SensorNotificationService(String name)
    {
        super(name);
    }

    /**
     * Creation callback. Initialize sensor.
     */
    public void onCreate()
    {
        super.onCreate();

        this.mSenseMan = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        this.mAccelerometer = this.mSenseMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        this.mSensorListener = new AccelerationSensorListener();

        // Size must be power of 2.
        this.mMagnitudeValues = new double[this.mFourierSize];
        for(int index = 0; index < this.mMagnitudeValues.length; ++index)
        {
            // Not necessary, since 0.0 should be default value, bet well, it won't hurt.
            this.mMagnitudeValues[index] = 0.0;
        }
        this.mFourier = new FFT(this.mFourierSize);

        this.mFourierOverTime = new double[4][this.mFourierSize];
        for(int index = 0; index < mFourierOverTime.length; ++index)
        {
            this.mFourierOverTime[index] = new double[this.mFourierSize];
            for(int innerIndex = 0; innerIndex < this.mFourierOverTime[index].length; ++innerIndex)
            {
                this.mFourierOverTime[index][innerIndex] = 0.0;
            }
        }

        // This line does also set the listener for the sensor.
        this.updateSampleRate(0);

        // Make sure sample rate can be updated.
        BroadcastReceiver receiver = new SampleRateReceiver(this);
        IntentFilter filter = new IntentFilter(Constants.ACTION_SAMPLERATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
    }

    /**
     * Actually never used. Still necessary due to abstract base class.
     * @param intent The intend that was send.
     */
    protected void onHandleIntent(Intent intent)
    {
    }

    /**
     * Update the sample rate of the acceleration sensor.
     * @param sampleRate The new sample rate to be used from now on.
     */
    public void updateSampleRate(int sampleRate)
    {
        this.mSenseMan.unregisterListener(this.mSensorListener);
        this.mSenseMan.registerListener(this.mSensorListener, this.mAccelerometer, sampleRate);
    }

    /**
     * Add a new magnitude value to the current magnitude array.
     * @param newMagnitude The magnitude to be added to the current array.
     */
    private void updateMagnitudeValueArray(double newMagnitude)
    {
        // Update current magnitude value collection.
        double temp = 0.0;
        for(int index = 0; index < mMagnitudeValues.length; ++index)
        {
            if(index == 0)
            {
                temp = mMagnitudeValues[0];
                mMagnitudeValues[0] = newMagnitude;
            }
            else
            {
                double newTemp = mMagnitudeValues[index];
                mMagnitudeValues[index] = temp;
                temp = newTemp;
            }
        }
    }

    /**
     * Calculate the absolute values of fourier transform from provided magnitude values.
     * @param magnitudeValues Magnitude values to get into fourier transform.
     * @param fourier Fourier object (hopefully initialized with proper size) to calculate stuff.
     * @return The absolute fourier transform values.
     */
    private double[] calculateFourierData(double[] magnitudeValues, FFT fourier)
    {
        double[] xArray = new double[magnitudeValues.length];
        double[] yArray = new double[magnitudeValues.length];

        // Do not alter magnitude value array, instead create a copy.
        // Also, initialize second array.
        for(int index = 0; index < magnitudeValues.length; ++index)
        {
            xArray[index] = magnitudeValues[index];
            yArray[index] = 0.0;
        }

        fourier.fft(xArray, yArray);

        // Calculate and store absolute values.
        double[] fourierAbsolutes = new double[magnitudeValues.length];
        for(int index = 0; index < fourierAbsolutes.length; ++index)
        {
            double result = Math.sqrt(Math.pow(xArray[index], 2) + Math.pow(yArray[index], 2));
            fourierAbsolutes[index] = result;
        }

        return fourierAbsolutes;
    }

    /**
     * Analyze given fourier data and update notification.
     * @param fourierData The fourier data to analyze.
     */
    private void updateNotification(double[] fourierData)
    {
        int unit = fourierData.length / 8;
        double fourierAverage1 = this.calculateAverage(fourierData, unit * 7, unit * 8);
        double fourierAverage2 = this.calculateAverage(fourierData, unit * 6, unit * 7);
        double fourierAverage3 = this.calculateAverage(fourierData, unit * 5, unit * 6);
        double fourierAverage4 = this.calculateAverage(fourierData, unit * 4, unit * 5);

        // Update current fourier value collection.
        double[] temp = new double[this.mFourierOverTime.length];
        for(int index = 0; index < this.mFourierSize; ++index)
        {
            double[] newTemp = new double[this.mFourierOverTime.length];
            {
                for(int areaIndex = 0; areaIndex < newTemp.length; ++areaIndex)
                {
                    newTemp[areaIndex] = this.mFourierOverTime[areaIndex][index];
                }
            }

            if(index == 0)
            {
                this.mFourierOverTime[0][0] = fourierAverage1;
                this.mFourierOverTime[1][0] = fourierAverage2;
                this.mFourierOverTime[2][0] = fourierAverage3;
                this.mFourierOverTime[3][0] = fourierAverage4;
            }
            else
            {
                for(int areaIndex = 0; areaIndex < this.mFourierOverTime.length; ++areaIndex)
                {
                    this.mFourierOverTime[areaIndex][index] = temp[areaIndex];
                }
            }

            temp = newTemp;
        }

        double fourierAverageOverTime1 = this.calculateAverage(this.mFourierOverTime[0], 0, this.mFourierSize);
        double fourierAverageOverTime2 = this.calculateAverage(this.mFourierOverTime[1], 0, this.mFourierSize);
        double fourierAverageOverTime3 = this.calculateAverage(this.mFourierOverTime[2], 0, this.mFourierSize);
        double fourierAverageOverTime4 = this.calculateAverage(this.mFourierOverTime[3], 0, this.mFourierSize);

        //System.out.println(fourierAverageOverTime1 + " - " + fourierAverageOverTime2 + " - " + fourierAverageOverTime3 + " - " + fourierAverageOverTime4);

        String title = "I know what you did last semester...";
        String oldMessage = this.mCurrentMessage;
        if(fourierAverageOverTime1 > 120.0 && fourierAverageOverTime1 < 210.0 &&
                fourierAverageOverTime2 > 15.0 && fourierAverageOverTime2 < 30.0 &&
                fourierAverageOverTime3 > 5.0 && fourierAverageOverTime3 < 25.0 &&
                fourierAverageOverTime4 > 5.0 && fourierAverageOverTime4 < 15.0)
        {
            this.mCurrentMessage = "... running from me!";
        }
        else if(fourierAverageOverTime1 > 40.0 && fourierAverageOverTime1 < 70.0 &&
                fourierAverageOverTime2 > 5.0 && fourierAverageOverTime2 < 10.0 &&
                fourierAverageOverTime3 > 0.0 && fourierAverageOverTime3 < 5.0 &&
                fourierAverageOverTime4 > 0.0 && fourierAverageOverTime4 < 5.0)
        {
            this.mCurrentMessage = "... walking away!";
        }
        else if(fourierAverageOverTime1 > 0.0 && fourierAverageOverTime1 < 15.0 &&
                fourierAverageOverTime2 > 0.0 && fourierAverageOverTime2 < 3.0 &&
                fourierAverageOverTime3 > 0.0 && fourierAverageOverTime3 < 3.0 &&
                fourierAverageOverTime4 > 0.0 && fourierAverageOverTime4 < 3.0)
        {
            this.mCurrentMessage = "... sitting around!";
        }
        else if(fourierAverageOverTime1 > 20.0 && fourierAverageOverTime1 < 40.0 &&
                fourierAverageOverTime2 > 0.0 && fourierAverageOverTime2 < 5.0 &&
                fourierAverageOverTime3 > 0.0 && fourierAverageOverTime3 < 5.0 &&
                fourierAverageOverTime4 > 0.0 && fourierAverageOverTime4 < 5.0)
        {
            this.mCurrentMessage = "... swimming with a Smartphone!?";
        }
        else if(fourierAverageOverTime1 > 230.0 && fourierAverageOverTime1 < 270.0 &&
                fourierAverageOverTime2 > 30.0 && fourierAverageOverTime2 < 50.0 &&
                fourierAverageOverTime3 > 10.0 && fourierAverageOverTime3 < 25.0 &&
                fourierAverageOverTime4 > 0.0 && fourierAverageOverTime4 < 15.0)
        {
            this.mCurrentMessage = "... rope skipping like hell!";
        }
        else if(fourierAverageOverTime1 > 15.0 && fourierAverageOverTime1 < 25.0 &&
                fourierAverageOverTime2 > 0.0 && fourierAverageOverTime2 < 10.0 &&
                fourierAverageOverTime3 > 0.0 && fourierAverageOverTime3 < 3.0 &&
                fourierAverageOverTime4 > 0.0 && fourierAverageOverTime4 < 3.0)
        {
            this.mCurrentMessage = "... escaping by bike!";
        }
        else
        {
            this.mCurrentMessage = "... nothing we wanted to know!";
        }

        if(!this.mCurrentMessage.equals(oldMessage))
        {
            this.writeNotification(title, this.mCurrentMessage);
        }
    }

    /**
     * Calculate the average value of (a part of) an array.
     * @param array Array to take values from.
     * @param startIndex Index to start taking values from.
     * @param endIndex Index to end taking values from.
     * @return The average value.
     */
    private double calculateAverage(double[] array, int startIndex, int endIndex)
    {
        double average = 0.0;
        for(int index = startIndex; index < endIndex; ++index)
        {
            average += array[index];
        }
        average = average / (endIndex - startIndex);
        return average;
    }

    /**
     * Pretty much copy-paste code from the docs. Yikes!
     * @param title The title of the notification.
     * @param text The text of the notification.
     */
    private void writeNotification(String title, String text)
    {
        // https://developer.android.com/guide/topics/ui/notifiers/notifications.html (actually provided in PDF)
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.logo)
                        .setContentTitle(title)
                        .setContentText(text);

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, SensorScanActivity.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(SensorScanActivity.class);

        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // mId allows you to update the notification later on.
        notificationManager.notify(this.mID, builder.build());
    }

    /**
     * Listener used to get input from accelerometer.
     * Updates the sensor visualizations.
     */
    private class AccelerationSensorListener implements SensorEventListener
    {
        /**
         * Callback when sensor data change.
         * @param sensorEvent The data of the sensor.
         */
        public void onSensorChanged(SensorEvent sensorEvent)
        {
            // Read sensor data.
            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];
            double magnitude = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));

            // Update fourier data.
            updateMagnitudeValueArray(magnitude);
            double[] fourierData = calculateFourierData(mMagnitudeValues, mFourier);
            updateNotification(fourierData);

            // Send data to SensorScanActivity.
            Intent localIntent = new Intent(Constants.ACTION_UPDATE);
            localIntent.putExtra(Constants.DATA_X, (int) (x) * 20);
            localIntent.putExtra(Constants.DATA_Y, (int) (y) * 20);
            localIntent.putExtra(Constants.DATA_Z, (int) (z) * 20);
            localIntent.putExtra(Constants.DATA_M, (int) (magnitude) * 20);
            localIntent.putExtra(Constants.DATA_FOURIER, fourierData);
            LocalBroadcastManager.getInstance(SensorNotificationService.this).sendBroadcast(localIntent);
        }

        /**
         * Unused.
         * @param sensor Sensor that changed accuracy.
         * @param i New accuracy.
         */
        public void onAccuracyChanged(Sensor sensor, int i)
        {
        }
    }

    /**
     * Communication tool with SensorScanActivity.
     */
    private class SampleRateReceiver extends BroadcastReceiver
    {
        /**
         * Reference to the managed service.
         */
        private SensorNotificationService mService = null;

        /**
         * Constructor.
         * @param service The service to keep up to date.
         */
        public SampleRateReceiver(SensorNotificationService service)
        {
            this.mService = service;
        }

        /**
         * Callback for receiving data.
         * @param context Context.
         * @param intent The intent send with the broadcast.
         */
        public void onReceive(Context context, Intent intent)
        {
            if(intent.getAction().equals(Constants.ACTION_SAMPLERATE))
            {
                this.mService.updateSampleRate(intent.getExtras().getInt(Constants.DATA_SAMPLERATE));
            }
        }
    }
}
