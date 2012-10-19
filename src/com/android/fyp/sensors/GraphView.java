package com.android.fyp.sensors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;

/**
 * A basic graph view. Can be set to either fit the graph bounds to
 * all the given points, or use user-defined bounds. It displays the
 * mean of the points, as well as 
 * @author Stephan Williams
 *
 */
public class GraphView extends View implements OnTouchListener {
        private List<PointF> points;
        private float yInt;
        private float xInt;
        private RectF window;
        private boolean fitToScreen;
        private float touchAt;

        public GraphView(Context context, AttributeSet attrs) {
                super(context, attrs);
                this.setOnTouchListener(this);
                points = new ArrayList<PointF>();
                yInt = 1;
                xInt = 1;
                window = new RectF(-10, -10, 10, 10);
                fitToScreen = true;
                touchAt = 1000;
        }
        @Override
        public void onDraw(Canvas canvas) {
                Paint white = new Paint() {{ setColor(0xFFFFFFFF); setTextSize(15); setAntiAlias(true); }};
                Paint gray = new Paint() {{ setColor(0xFFDDDDDD); }};
                Paint redAA = new Paint() {{ setColor(0xFFFF0000); setTextSize(15); setAntiAlias(true); }};
                Paint blackAA = new Paint() {{ setColor(0xFF000000); setTextSize(15); setAntiAlias(true); }};
                Paint red = new Paint() {{ setColor(0xFFFF0000); setTextSize(15); }};
                Paint black = new Paint() {{ setColor(0xFF000000); setTextSize(15); }};

//              bounds represents the coordinate system of the graph,
//              clip represents the graph relative to the screen.
                RectF bounds = fitToScreen ? createBoundsRect(points, 1) : window;
                RectF clip = new RectF(50, 50, 320, 455);

                canvas.drawRect(clip, white);
//              prevent NullPointerExceptions
                if (points.size() == 0) return;

//              draw y axis labels
                canvas.rotate(90, 160, 160);
                for (float i = Math.min(bounds.top, bounds.bottom);
   i <= Math.max(bounds.top, bounds.bottom);
   i += fitToScreen ? (Math.abs(bounds.top - bounds.bottom) / 15f) : Math.abs(yInt)) {
                        float off = mapPoint(new PointF(0, i), bounds, clip).x;
                        canvas.drawText("" + round(i), 10, 325 - off, white);
                        canvas.drawLine(48, 320 - off, 455, 320 - off, gray);
                }

                canvas.rotate(-90, 160, 160);

//              draw x axis labels
                for (float i = Math.min(bounds.left, bounds.right);
   i <= Math.max(bounds.left, bounds.right);
   i += fitToScreen ? (Math.abs(bounds.left - bounds.right) / 25) : Math.abs(xInt)) {
                        float off = mapPoint(new PointF(i, 0), bounds, clip).y;
                        canvas.drawText("" + round(i), 10, off + 5, white);
                        canvas.drawLine(48, off, 320, off, gray);
                }

                canvas.clipRect(clip);
//              draw mean
                float temp = mapPoint(new PointF(0, getAverageY()), bounds, clip).x;
                canvas.drawLine((int)temp, clip.top, (int)temp, clip.bottom, red);
                canvas.rotate(90, 160, 160);
                canvas.drawText("" + round(getAverageY()), clip.top + 2, clip.right - temp - 2, redAA);
                canvas.rotate(-90, 160, 160);

//              draw graph
                for (int i = 1; i < points.size(); i++) {
                        PointF p1 = mapPoint(points.get(i - 1), bounds, clip);
                        PointF p2 = mapPoint(points.get(i), bounds, clip);
                        canvas.drawLine(p1.x, p1.y, p2.x, p2.y, new Paint() {{ setColor(0xFF0000FF); }});
                }

//              draw trace line
                temp = mapPoint(new PointF(touchAt, 0), clip, bounds).y;
                canvas.drawLine(touchAt, clip.top, touchAt, clip.bottom, black);
                canvas.rotate(90, 160, 160);
                canvas.drawText(String.format("%3.2f", temp), clip.top + 2, clip.right - touchAt - 2, blackAA);
                canvas.rotate(-90, 160, 160);

        }
        /**
         * Replaces the existing points in the graph with
         * ones from a list
         * @param p the list of points
         */
        public void setPoints(List<PointF> p) {
                points = p;
                sortPoints();
        }
        /**
         * Adds a list of points to the existing points
         * @param p this list of points to add
         */
        public void addPoints(List<PointF> p) {
                points.addAll(p);
                sortPoints();
        }
        /**
         * Adds a single point
         * @param p the point to add
         */
        public void addPoint(PointF p) {
                points.add(p);
                sortPoints();
        }
        /**
         * Add a point from x and y coordinates, instead
         * of from a PointF object
         * @param x
         * @param y
         */
        public void addPoint(float x, float y) {
                points.add(new PointF(x, y));
                sortPoints();
        }
        /**
         * Removes all points from the graph
         */
        public void clearPoints() {
                points.clear();
        }
        /**
         * Sets the user-defined x interval for the graph.
         * @see #setFitToScreen(boolean)
         * @param xint
         */
        public void setXInterval(float xint) {
                xInt = Math.abs(xint);
        }
        /**
         * @see #setXInterval(float)
         * @see #setFitToScreen(boolean)
         * @return the user-defined x interval for the graph
         */
        public float getXInterval() {
                return xInt;
        }
        /**
         * Sets the user-defined y interval for the graph.
         * @see #setFitToScreen(boolean)
         * @param yint
         */
        public void setYInterval(float yint) {
                yInt = Math.abs(yint);
        }
        /**
         * @see #setYInterval(float)
         * @see #setFitToScreen(boolean)
         * @return the user-defined y interval for the graph
         */
        public float getYInterval() {
                return yInt;
        }
        /**
         * Sorts the list of points in ascending order, by x value
         */
        private void sortPoints() {
                Collections.sort(points, new Comparator<PointF>() {
                        @Override
                        public int compare(PointF p1, PointF p2) {
                                return Float.compare(p1.x, p2.x);
                        }
                });
        }
        /**
         * Creates a rectangle that completely encloses all points
         * in the given list, with the specified border around them.
         * The border is taken as the same units as the graph.
         * @param points a list of points
         * @param border the width of the border to put around the points
         * @return a rectangle enclosing all given points
         */
        private RectF createBoundsRect(List<PointF> points, float border) {
                if (points.size() == 0) return new RectF();
                float ymin = points.get(0).y;
                float ymax = points.get(0).y;
                for (PointF p : points) {
                        if (p.y < ymin) ymin = p.y;
                        if (p.y > ymax) ymax = p.y;
                }
                return new RectF(points.get(0).x - border,
                                                 ymin - 2 *border,
                                                 points.get(points.size() - 1).x + border,
                                                 ymax + 2 * border);
        }
        /**
         * Maps a point from one rectangle to another, so it appears in
         * the same relative spot
         * @param p the point to map
         * @param srcRect
         * @param destRect
         * @return the mapped point
         */
        private PointF mapPoint(PointF p, RectF srcRect, RectF destRect) {
                return new PointF((p.y - srcRect.top) / (srcRect.bottom - srcRect.top) *
                                (destRect.right - destRect.left) + destRect.left,
                                (p.x - srcRect.left) / (srcRect.right - srcRect.left) *
                                (destRect.bottom - destRect.top) + destRect.top);
        }
        /**
         * 
         * @return the average y value of the points in the graph
         */
        public float getAverageY() {
                if (points.size() == 0) return 0;
                float temp = 0;
                for (PointF pf : points) temp += pf.y;
                return temp / points.size();
        }
        /**
         * Sets whether the graph should fit all the points on the
         * graph, or use the user-defined bounds and intervals.
         * @param fit
         */
        public void setFitToScreen(boolean fit) {
                fitToScreen = fit;
        }
        /**
         * @see #setFitToScreen(boolean)
         * @return
         */
        public boolean getFitToScreen() {
                return fitToScreen;
        }
        /**
         * Sets the user-defined viewing window for the graph.
         * @see #setFitToScreen(boolean)
         * @param rect
         */
        public void setWindow(RectF rect) {
                window = rect;
                window.sort();
        }
        /**
         * @see #setFitToScreen(boolean)
         * @see #setWindow(RectF)
         * @return the user-defined window for the graph
         */
        public RectF getWindow() {
                return window;
        }
        @Override
        public boolean onTouch(View view, MotionEvent e) {
                touchAt = e.getX();
                postInvalidate();

                return true;
        }
        private float round(float f) {
                return (int)(f * 100f) / 100f;
        }
}