package com.assignment.schlewinow.assignment3;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.util.AttributeSet;

/**
 * Visualize accelerometer data using FFT.
 */
public class SensorFFTView extends SensorBaseView
{
    /**
     * Current fourier transform data.
     */
    private double[] mFourierData;

    /**
     * Bars to visualize fourier transform.
     */
    private ShapeDrawable[] mBars;

    /**
     * Constructor necessary
     * @param context Context provided when the view is initialized.
     */
    public SensorFFTView(Context context)
    {
        super(context);
    }

    /**
     * Constructor necessary when used within XML.
     * @param context Context provided when the view is initialized.
     * @param attributes Passed attributes.
     */
    public SensorFFTView(Context context, AttributeSet attributes)
    {
        super(context, attributes);
    }

    /**
     * Setter magnitude values.
     * @param values The new magnitude values.
     */
    public void setFourierData(double[] values)
    {
        if(this.mFourierData == null || this.mFourierData.length != values.length)
        {
            this.mBars = new ShapeDrawable[values.length];
            for(int index = 0; index < this.mBars.length; ++index)
            {
                ShapeDrawable shape = new ShapeDrawable(new RectShape());
                shape.getPaint().setColor(Color.rgb(255, 0, 255));
                this.mBars[index] = shape;
            }
        }

        this.mFourierData = values;
    }

    /**
     * Android draw call.
     * @param canvas Used to draw stuff to the view. Provided by App.
     */
    protected void onDraw(Canvas canvas)
    {
        // Happened sometimes when testing code.
        if(this.mFourierData == null)
        {
            return;
        }

        // Some pre calculation due to view size.
        int barWidth = 0;
        int factor = 0;
        while (barWidth == 0)
        {
            factor++;
            barWidth = this.getWidth() / (this.mFourierData.length / factor);
        }

        // Update bars.
        for(int index = 0; index < this.mFourierData.length; ++index)
        {
            if(index % factor == 0)
            {
                ShapeDrawable shape = this.mBars[index];

                int x = barWidth * index;
                int y = 0;
                shape.setBounds(x, y, x + barWidth, y + (int)(this.mFourierData[index]));
                shape.draw(canvas);
            }
        }
    }
}
