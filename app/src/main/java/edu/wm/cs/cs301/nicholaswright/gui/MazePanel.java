package edu.wm.cs.cs301.nicholaswright.gui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import edu.wm.cs.cs301.nicholaswright.R;

/**
 * Graphics container for the maze game. Implements a View for PlayManuallyActivity and
 * PlayAnimationActivity on which different components can draw the graphics.
 */
public class MazePanel extends View {

    // for logging
    private static final String TAG = "MazePanel";

    // drawing objects
    private Paint paint;
    private Bitmap drawing_bitmap;
    private Canvas panel_canvas;
    private Shader shader_walls;
    private Shader shader_floor;

    private FirstPersonView firstPersonView;
    private Map mapView;

    /**
     * Required default constructor.
     * @param context
     * @param attrs
     */
    public MazePanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * does the drawing
     * @param canvas
     */
    @Override
    public void onDraw(Canvas canvas){
        Log.v(TAG, "Drawing");
        super.onDraw(canvas);

        canvas.drawBitmap(drawing_bitmap, new Matrix(), paint);
    }

    /**
     * Creates the drawing objects. This should be called just once, so we only
     * instantiate stuff once.
     */
    private void init(){
        paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(5);

        drawing_bitmap = Bitmap.createBitmap(1200, 1200, Bitmap.Config.ARGB_8888);
        panel_canvas = new Canvas(drawing_bitmap);

        Bitmap shader_bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.cheese_walls);
        shader_walls = new BitmapShader(shader_bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        shader_bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.cheese_floor);
        shader_floor = new BitmapShader(shader_bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);

    }

    /**
     * Method to draw the buffer image on a graphics object that is
     * obtained from the superclass.
     * Warning: do not override getGraphics() or drawing might fail.
     */
    public void update() {
        if(panel_canvas == null){
            return;
        }

        invalidate();

    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////// METHODS FOR AWT WRAPPER CLASS ////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Sets the graphics color given RGB values in the range {0-255, 0-255, 0-255}
     * @param r red, 0-255
     * @param g green, 0-255
     * @param b blue, 0-255
     */
    public void setColor(int r, int g, int b) {
        paint.setColor(Color.rgb(r,g,b));
    }

    /**
     * Draws a line from (x1, y1) to (x2, y2)
     * @param x1 x-coordinate of the first point
     * @param y1 y-coordinate of the first point
     * @param x2 x-coordinate of the second point
     * @param y2 y-coordinate of the second point
     */
    public void drawLine(int x1, int y1, int x2, int y2) {
        panel_canvas.drawLine(x1, y1, x2, y2, paint);
    }

    /**
     * Draws and fills an oval
     * @param x x-coordinate of the upper-left corner of the oval
     * @param y y-coordinate of the upper-left corner of the oval
     * @param width the width of the oval to be drawn
     * @param height the height of the oval to be drawn
     */
    public void fillOval(int x, int y, int width, int height) {
        panel_canvas.drawOval(x, y, x+width, y+width, paint);
    }

    /**
     * Draws and fills a picture of a mouse (at drawable/mouse_icon)
     * @param x x-coordinate of the upper-left corner of the icon
     * @param y y-coordinate of the upper-left corner of the icon
     * @param width the width of the icon to be drawn
     * @param height the height of the icon to be drawn
     */
    public void drawMouseIcon(int x, int y, int width, int height) {
        Bitmap mouse_bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mouse_icon);
        Rect r = new Rect(x, y, x+width, y+height);
        panel_canvas.drawBitmap(mouse_bitmap,null,r,paint);
    }

    /**
     * Fills the specified rectangle. The left and right edges of the rectangle are at
     * x and x + width - 1. The top and bottom edges are at y and y + height - 1. The
     * resulting rectangle covers an area width pixels wide by height pixels tall. The
     * rectangle is filled using the graphics context's current color.
     * @param x the x-coordinate of the rectangle
     * @param y the y-coordinate of the rectangle
     * @param width the width of the rectangle to be drawn
     * @param height the height of the rectangle to be drawn
     */
    public void fillRect(int x, int y, int width, int height) {
        panel_canvas.drawRect(x, y, x+width, y+height, paint);
    }

    /**
     * Fills the specified rectangle. The left and right edges of the rectangle are at
     * x and x + width - 1. The top and bottom edges are at y and y + height - 1. The
     * resulting rectangle covers an area width pixels wide by height pixels tall. The
     * rectangle is filled using the graphics context's current color.
     *
     * Does all this with a texture specified by shader_floor.
     * @param x the x-coordinate of the rectangle
     * @param y the y-coordinate of the rectangle
     * @param width the width of the rectangle to be drawn
     * @param height the height of the rectangle to be drawn
     */
    public void fillRectTexture(int x, int y, int width, int height) {

        paint.setShader(shader_floor);

        panel_canvas.drawRect(x, y, x+width, y+height, paint);

        paint.setShader(null);
    }

    /**
     * Draws a closed polygon defined by arrays of x and y coordinates. Each pair of
     * (x, y) coordinates defines a point.
     * Does all this with a texture specified by shader_walls.
     * @param xPoints an array of x coordinates.
     * @param yPoints a an array of y coordinates.
     * @param nPoints the total number of points.
     */
    public void fillPolygonTexture(int[] xPoints, int[] yPoints, int nPoints) {

        paint.setShader(shader_walls);

        Path path = new Path();
        path.moveTo(xPoints[0], yPoints[0]);
        for(int i = 1; i < nPoints; i++){
            path.lineTo(xPoints[i], yPoints[i]);
        }
        path.lineTo(xPoints[0], yPoints[0]);

        panel_canvas.drawPath(path, paint);

        paint.setShader(null);
    }

    /**
     * Draws a closed polygon defined by arrays of x and y coordinates. Each pair of
     * (x, y) coordinates defines a point.
     * @param xPoints an array of x coordinates.
     * @param yPoints a an array of y coordinates.
     * @param nPoints the total number of points.
     */
    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        Path path = new Path();
        path.moveTo(xPoints[0], yPoints[0]);
        for(int i = 1; i < nPoints; i++){
            path.lineTo(xPoints[i], yPoints[i]);
        }
        path.lineTo(xPoints[0], yPoints[0]);

        panel_canvas.drawPath(path, paint);
    }

    /**
     * Returns the RGB value representing the color in the default sRGB ColorModel from a
     * tuple of rgb components
     * @param rgb {red, green, blue}, each from 0-255
     * @return the RGB value representing the color in the default sRGB ColorModel
     */
    public static int getRGBIntFromSeparateValues(int[] rgb) {
        return Color.rgb(rgb[0], rgb[1], rgb[2]);
    }

    /**
     * Returns an array {red, green, blue} of RGB components given an
     * RGB value representing a color in the default sRGB ColorModel
     * @param rgb the GB value representing the color in the default sRGB ColorModel
     * @return {red, green, blue}, each from 0-255
     */
    public static int[] getRGBValuesFromSingleInt(int rgb) {
        return new int[] {Color.red(rgb), Color.green(rgb), Color.blue(rgb)};
    }

    /**
     * Test method for graphics. Draws a few figures with different shapes
     */
    private void drawTest(){
        // red circle
        setColor(255, 0, 0);
        fillOval(20, 20, 100, 100);

        // green circle
        setColor(0, 255, 0);
        fillOval(120, 120, 100, 100);

        // yellow triangle
        setColor(255, 255, 0);
        int[] x = {500, 250, 750};
        int[] y = {500, 750, 750};
        fillPolygon(x, y, 3);


        setColor(0, 0, 255);
        fillRect(1000, 200, 200, 800);
    }

}
