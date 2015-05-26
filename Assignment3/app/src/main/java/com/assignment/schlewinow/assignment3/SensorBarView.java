package com.assignment.schlewinow.assignment3;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.util.AttributeSet;

/**
 * View to visualize accelerometer sensor input in four bars.
 */
public class SensorBarView extends SensorBaseView
{
    /**
     * Bar to visualize the x-axis acceleration.
     */
    private ShapeDrawable mBarX = null;

    /**
     * Bar to visualize the y-axis acceleration.
     */
    private ShapeDrawable mBarY = null;

    /**
     * Bar to visualize the z-axis acceleration.
     */
    private ShapeDrawable mBarZ = null;

    /**
     * Bar to visualize the magnitude of the three sensor outputs.
     */
    private ShapeDrawable mBarMagnitude = null;

    /**
     * Values to be visualized.
     */
    protected int mCurrentX, mCurrentY, mCurrentZ, mCurrentMagnitude;

    /**
     * Constructor necessary
     * @param context Context provided when the view is initialized.
     */
    public SensorBarView(Context context)
    {
        super(context);
    }

    public SensorBarView(Context context, AttributeSet attributes)
    {
        super(context, attributes);
    }

    /**
     * Arrange elements when focus changes.
     * @param hasFocus Whether we have the focus or not.
     */
    public void onWindowFocusChanged(boolean hasFocus)
    {
        this.initializeDrawables();
    }

    /**
     * Arrange elements when config changes (e.g. screen orientation).
     * @param newConfig The new configuration.
     */
    public void onConfigurationChanged(Configuration newConfig)
    {
        this.initializeDrawables();
    }

    /**
     * Helper to initialize the drawable objects.
     */
    private void initializeDrawables()
    {
        this.mBarX = this.initializeBar(Color.RED, 1, 4);
        this.mBarY = this.initializeBar(Color.GREEN, 2, 4);
        this.mBarZ = this.initializeBar(Color.BLUE, 3, 4);
        this.mBarMagnitude = this.initializeBar(Color.WHITE, 4, 4);
    }

    /**
     * Helper to initialize a single sensor output bar.
     * @param color The color the created shape will have.
     * @param barIndex The index of the bar. Between 1 and totalBars.
     * @param totalBars The total number of bars (in this code: 4 - X, Y, Z, Magnitude)
     * @return Reference to the created shape.
     */
    private ShapeDrawable initializeBar(int color, int barIndex, int totalBars)
    {
        ShapeDrawable shape = new ShapeDrawable(new RectShape());
        shape.getPaint().setColor(color);

        int barWidth = (this.getWidth() / (totalBars * 2 + 1));
        int x = barWidth + barWidth * (barIndex-1) * 2;
        int y = this.getMinBarHeight();
        shape.setBounds(x, y, x + barWidth, y + this.getMaxBarHeight());

        return shape;
    }

    /**
     * Necessary when updating bar with sensor values.
     * @return The height where an output bar starts.
     */
    private int getMinBarHeight()
    {
        return this.getHeight() / 2;
    }

    /**
     * Necessary when updating bar with sensor values.
     * @return The height an output bar reaches at maximum.
     */
    private int getMaxBarHeight()
    {
        return (this.getHeight() / 10) * 8;
    }

    /**
     * Setter x-value.
     * @param x The new z-value.
     */
    public void setX(int x)
    {
        this.mCurrentX = x;
    }

    /**
     * Setter y-value.
     * @param y The new z-value.
     */
    public void setY(int y)
    {
        this.mCurrentY = y;
    }

    /**
     * Setter z-value.
     * @param z The new z-value.
     */
    public void setZ(int z)
    {
        this.mCurrentZ = z;
    }

    /**
     * Setter Magnitude. (Could be calculated internally also.)
     * @param magnitude The new magnitude.
     */
    public void setMagnitude(int magnitude)
    {
        this.mCurrentMagnitude = magnitude;
    }

    /**
     * Android draw call.
     * @param canvas Used to draw stuff to the view. Provided by App.
     */
    protected void onDraw(Canvas canvas)
    {
        this.drawUpdatedBar(this.mBarX, this.mCurrentX, canvas);
        this.drawUpdatedBar(this.mBarY, this.mCurrentY, canvas);
        this.drawUpdatedBar(this.mBarZ, this.mCurrentZ, canvas);
        this.drawUpdatedBar(this.mBarMagnitude, this.mCurrentMagnitude, canvas);
    }

    /**
     * Helper to draw a bar with updated input values.
     * @param shape The bar to be drawn.
     * @param value The value of the bar.
     * @param canvas The canvas to draw to.
     */
    private void drawUpdatedBar(ShapeDrawable shape, int value, Canvas canvas)
    {
        // May happen in some cases.
        if(shape == null)
        {
            return;
        }

        if(value > 0)
        {
            // Draw bar above imaginary 0-line.
            shape.setBounds(shape.getBounds().left,
                    this.getMinBarHeight() - value,
                    shape.getBounds().right,
                    this.getMinBarHeight());
        }
        else
        {
            // Draw bar below imaginary 0-line.
            shape.setBounds(shape.getBounds().left,
                    this.getMinBarHeight(),
                    shape.getBounds().right,
                    this.getMinBarHeight() - value);
        }

        shape.draw(canvas);
    }
}
