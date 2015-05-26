package com.assignment.schlewinow.assignment3;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

/**
 * Base class to sensor views.
 * Avoids some otherwise redundant code.
 */
public abstract class SensorBaseView extends View
{
    /**
     * Desired view width.
     */
    private int mDesiredWidth = 500;

    /**
     * Desired view height.
     */
    private int mDesiredHeight = 500;

    /**
     * Constructor necessary
     * @param context Context provided when the view is initialized.
     */
    public SensorBaseView(Context context)
    {
        super(context);
    }

    /**
     * Constructor necessary when used within XML.
     * @param context Context provided when the view is initialized.
     * @param attributes Passed attributes.
     */
    public SensorBaseView(Context context, AttributeSet attributes)
    {
        super(context, attributes);
    }

    /**
     * Too lazy to write two setters.
     * @param width New desired width;
     * @param height New desired height;
     */
    public void setDesiredViewSize(int width, int height)
    {
        this.mDesiredWidth = width;
        this.mDesiredHeight = height;

        // Value of 0 produces exception, value below 10 still makes no sense at all.
        // (Well, at least 100 would be nice, anyway).
        if(this.mDesiredWidth < 10)
        {
            this.mDesiredWidth = 10;
        }

        if(this.mDesiredHeight < 10)
        {
            this.mDesiredHeight = 10;
        }
    }

    /**
     * The docs say this call is important.
     * Specifies the size of the view.
     * @param widthMeasureSpec View width.
     * @param heightMeasureSpec View height.
     */
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        // Taken from: http://stackoverflow.com/questions/12266899/onmeasure-custom-view-explanation
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;

        //Measure Width
        if (widthMode == MeasureSpec.EXACTLY)
        {
            //Must be this size
            width = widthSize;
        }
        else if (widthMode == MeasureSpec.AT_MOST)
        {
            //Can't be bigger than...
            width = Math.min(this.mDesiredWidth, widthSize);
        }
        else
        {
            //Be whatever you want
            width = this.mDesiredWidth;
        }

        //Measure Height
        if (heightMode == MeasureSpec.EXACTLY)
        {
            //Must be this size
            height = heightSize;
        }
        else if (heightMode == MeasureSpec.AT_MOST)
        {
            //Can't be bigger than...
            height = Math.min(this.mDesiredHeight, heightSize);
        }
        else
        {
            //Be whatever you want
            height = this.mDesiredHeight;
        }

        //MUST CALL THIS
        setMeasuredDimension(width, height);
    }
}
