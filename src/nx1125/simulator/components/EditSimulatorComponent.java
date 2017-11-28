package nx1125.simulator.components;

import nx1125.simulator.simulation.Planet;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * TODO: document your custom view class.
 */
public class EditSimulatorComponent extends CartesianComponent {

    private static final int LINE_COUNT = 16;
    private static final int POINT_COUNT = LINE_COUNT * 2;

    private static final int VECTOR_HEAD_RADIUS = 3;
    private final ArrayList<PlanetInfo> mPlanetInfoList = new ArrayList<>();
    private final Line2D.Float mLineCache = new Line2D.Float();
    private final Ellipse2D.Float mOvalCache = new Ellipse2D.Float();
    private Color mPlanetBackgroundColor;
    private Color mPlanetForegroundColor;
    private Color mCapPlanetForegroundColor;
    private float mPlanetBorderWidth;
    private Stroke mPlanetBorderStroke;
    private Color mVelocityColor;
    private float mVelocityStickWidth;
    private Stroke mVelocityStickStroke;
    private Rectangle2D.Float mRect = new Rectangle2D.Float();
    private Rectangle2D.Float mRectAddCache = new Rectangle2D.Float();
    private int mStepCount;
    private int mStepCountRest;
    private float[] mCachedPointArray = new float[POINT_COUNT * 2];
    private float[] mCachedVectorArray = new float[POINT_COUNT * 4];
    private int mPointArrayIndex;
    private Iterator<PlanetInfo> mIterator;
    private float[] mCacheGetPlanetAt = new float[2];

    private float mPlanetClickTolerance = 5;

    private OnPlanetPressedListener mOnPlanetPressedListener;

    private OnCaptureClickListener mOnCaptureClickListener;

    private OnEmptyLongClickListener mOnEmptyLongClickListener;

    private OnPlanetClickedListener mOnPlanetClickedListener;

    public EditSimulatorComponent() {
        init();
    }

    private void init() {
        mPlanetBackgroundColor = Color.gray.brighter();
        mPlanetForegroundColor = Color.black;
        mCapPlanetForegroundColor = Color.green.darker();
        mPlanetBorderWidth = 1;
        mPlanetBorderStroke = new BasicStroke(mPlanetBorderWidth);

        mVelocityColor = Color.green;
        mVelocityStickWidth = 1;
        mVelocityStickStroke = new BasicStroke(mVelocityStickWidth);
    }

    public void setOnPlanetClickedListener(OnPlanetClickedListener onPlanetClickedListener) {
        mOnPlanetClickedListener = onPlanetClickedListener;
    }

    public void setVelocityStickWidth(float width) {
        if (width != mVelocityStickWidth) {
            mVelocityStickWidth = width;
            mVelocityStickStroke = new BasicStroke(width);

            repaint();
        }
    }

    public float getPlanetClickTolerance() {
        return mPlanetClickTolerance;
    }

    public void setPlanetClickTolerance(float tolerance) {
        mPlanetClickTolerance = tolerance;
    }

    public void setOnPlanetPressedListener(OnPlanetPressedListener l) {
        mOnPlanetPressedListener = l;
    }

    public void setOnEmptyLongClickListener(OnEmptyLongClickListener l) {
        mOnEmptyLongClickListener = l;
    }

    private void onEmptyLongClick(float px, float py) {
        if (mOnEmptyLongClickListener != null)
            mOnEmptyLongClickListener.onLongClick(px, py);
    }

    public void setOnCaptureClickListener(OnCaptureClickListener l) {
        mOnCaptureClickListener = l;
    }

    private void updatePointArrayCache(MouseEvent event) {
        setCachedPointArray(event);
        invert(mCachedPointArray);
    }

    @Override
    protected void onMousePressed(MouseEvent event) {
        if (event.getButton() == MouseEvent.BUTTON1) {
            updatePointArrayCache(event);
            if (mOnCaptureClickListener != null) {
                mOnCaptureClickListener.onDrag(this, mCachedPointArray[0], mCachedPointArray[1]);
            } else {
                Planet planet = getPlanetAtNoConversion(mCachedPointArray[0], mCachedPointArray[1], 0);
                if (planet != null && mOnPlanetPressedListener != null) {
                    mOnPlanetPressedListener.onPlanetPressed(planet, mCachedPointArray[0], mCachedPointArray[1]);
                }
                super.onMousePressed(event);
            }
        } else {
            super.onMousePressed(event);
        }
    }

    @Override
    protected void onMouseDragged(MouseEvent e) {
        if (mOnCaptureClickListener != null) {
            updatePointArrayCache(e);
            mOnCaptureClickListener.onDrag(this, mCachedPointArray[0], mCachedPointArray[1]);
        } else {
            super.onMouseDragged(e);
        }
    }

    @Override
    protected void onMouseReleased(MouseEvent e) {
        if (mOnCaptureClickListener != null) {
            updatePointArrayCache(e);
            if (mOnCaptureClickListener.onRelease(this, mCachedPointArray[0], mCachedPointArray[1])) {
                mOnCaptureClickListener = null;
            }
        } else {
            super.onMouseReleased(e);
        }
    }

    @Override
    protected void onMouseClicked(MouseEvent e) {
        if (mOnPlanetClickedListener != null) {
            updatePointArrayCache(e);
            Planet planet = getPlanetAtNoConversion(mCachedPointArray[0], mCachedPointArray[1], 0);

            if (planet != null) {
                mOnPlanetClickedListener.onPlanetClicked(planet);
            } else {
                super.onMouseClicked(e);
            }
        } else {
            super.onMouseClicked(e);
        }
    }

    private void setCachedPointArray(MouseEvent event) {
        mCachedPointArray[0] = event.getX();
        mCachedPointArray[1] = event.getY();
    }

    public void setPlanetBackgroundColor(Color background) {
        mPlanetBackgroundColor = background;

        repaint();
    }

    public void setPlanetForegroundColor(Color foreground) {
        mPlanetForegroundColor = foreground;

        repaint();
    }

    public void setPlanetBorderWidth(float width) {
        mPlanetBorderWidth = width;
        mPlanetBorderStroke = new BasicStroke(width);

        repaint();
    }

    public void addPlanet(Planet planet) {
        PlanetInfo info = new PlanetInfo(planet);

        mPlanetInfoList.add(info);

        invalidateCounters();
        repaint();
    }

    public void setPlanets(List<Planet> planets) {
        // reuse each PlanetInfo
        int diff = planets.size() - mPlanetInfoList.size();

        if (diff > 0) {
            // there are more planets than there are info instances
            mPlanetInfoList.ensureCapacity(planets.size());
            for (int i = 0; i < diff; i++) mPlanetInfoList.add(new PlanetInfo());
        } else if (diff < 0) {
            // there are instances that are no longer need
            mPlanetInfoList.subList(planets.size(), mPlanetInfoList.size() - 1).clear();
        }

        Iterator<Planet> planetIterator = planets.iterator();
        Iterator<PlanetInfo> infoIterator = mPlanetInfoList.iterator();

        while (planetIterator.hasNext()) {
            infoIterator.next().setPlanet(planetIterator.next());
        }

        if (diff != 0) invalidateCounters();

        repaint();
    }

    public Planet getPlanetAt(float x, float y) {
        return getPlanetAt(x, y, mPlanetClickTolerance);
    }

    public Planet getPlanetAt(float x, float y, float pixelTolerance) {
        mCacheGetPlanetAt[0] = x;
        mCacheGetPlanetAt[1] = y;

        invert(mCacheGetPlanetAt);

        x = mCacheGetPlanetAt[0];
        y = mCacheGetPlanetAt[1];

        return getPlanetAtNoConversion(x, y, pixelTolerance);
    }

    private Planet getPlanetAtNoConversion(float x, float y, float pixelTolerance) {
        pixelTolerance /= getCartesianScale();

        for (PlanetInfo planetInfo : mPlanetInfoList) {
            float r = planetInfo.mPlanet.getRadius() + pixelTolerance;
            if (getDistanceSqr(x - planetInfo.mPlanet.getX(), y - planetInfo.mPlanet.getY()) <= r * r) {
                return planetInfo.mPlanet;
            }
        }

        return null;
    }

    public void removePlanet(int index) {
        mPlanetInfoList.remove(index);

        // the new size was the last index of mPlanetInfoList before the planet is removed
        if (index < mPlanetInfoList.size()) {
            invalidatePlanets(index, mPlanetInfoList.size() - index - 1);
        }

        invalidateCounters();

        repaint();
    }

    private void invalidateCounters() {
        // each circumference uses two points that is the count as a line uses
        mStepCount = mPlanetInfoList.size() / LINE_COUNT;
        mStepCountRest = mPlanetInfoList.size() % LINE_COUNT;
    }

    public void invalidatePlanet(int index) {
        mPlanetInfoList.get(index).invalidatePlanet();

        repaint();
    }

    public void invalidatePlanets(int offset, int count) {
        count += offset;
        while (offset < count) {
            mPlanetInfoList.get(offset).invalidatePlanet();
            offset++;
        }

        repaint();
    }

    public void lookAtPlanets(int horizontal, int vertical) {
        if (!mPlanetInfoList.isEmpty()) {
            Iterator<PlanetInfo> iterator = mPlanetInfoList.iterator();

            PlanetInfo planetInfo = iterator.next();

            mRect.setRect(planetInfo.mLeft, planetInfo.mTop, planetInfo.mRight, planetInfo.mBottom);

            while (iterator.hasNext()) {
                planetInfo = iterator.next();
                mRectAddCache.setRect(planetInfo.mLeft, planetInfo.mTop, planetInfo.mRight, planetInfo.mBottom);
                mRect.add(mRectAddCache);
            }
        } else {
            mRect.setRect(-0.5f, -0.5f, 0.5f, 0.5f);
        }

        lookAt(mRect.x, mRect.y, mRect.x + mRect.width, mRect.y + mRect.height, horizontal, vertical);
    }

    public void lookAtPlanet(int index, int horizontal, int vertical) {
        PlanetInfo planetInfo = mPlanetInfoList.get(index);

        lookAt(planetInfo.mLeft, planetInfo.mTop, planetInfo.mRight,
                planetInfo.mBottom, horizontal, vertical);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D canvas = (Graphics2D) g;

        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        mPointArrayIndex = 0;

        mIterator = mPlanetInfoList.iterator();

        for (int i = 0; i < mStepCount; i++) onPaintCircumferences(canvas, LINE_COUNT);
        if (mStepCountRest > 0) onPaintCircumferences(canvas, mStepCountRest);

        if (mPlanetInfoList.size() >= 2) {
            canvas.setColor(mCapPlanetForegroundColor);
            canvas.setStroke(mPlanetBorderStroke);

            mPointArrayIndex = 0;

            fillArrayWithPlanetCircumference(mPlanetInfoList.get(0));
            fillArrayWithPlanetCircumference(mPlanetInfoList.get(mPlanetInfoList.size() - 1));

            getCartesianMatrix().transform(mCachedPointArray, 0, mCachedPointArray, 0, 4);

            paintOvals(2, canvas::draw);
        }

        canvas.setColor(mVelocityColor);

        int offset = 0;
        for (int i = mPlanetInfoList.size() / POINT_COUNT; i > 0; i--, offset += POINT_COUNT) {
            onPaintVelocities(canvas, offset, POINT_COUNT);
        }
        onPaintVelocities(canvas, offset, mPlanetInfoList.size() % POINT_COUNT);
    }

    private void onPaintVelocities(Graphics2D canvas, int offset, int count) {
        int k = 0;
        int effectiveCount = 0;
        for (int i = 0; i < count; i++) {
            PlanetInfo info = mPlanetInfoList.get(i + offset);

            if (info.mHasVelocity) {
                mCachedVectorArray[k++] = info.getX();
                mCachedVectorArray[k++] = info.getY();
                mCachedVectorArray[k++] = info.mVelocityX;
                mCachedVectorArray[k++] = info.mVelocityY;

                effectiveCount++;
            }
        }

        if (effectiveCount > 0) {
            transformPoints(mCachedVectorArray, 0, effectiveCount << 1);

            // canvas.drawLines(mCachedVectorArray, 0, k, mVelocityPaint);
            Stroke stroke = canvas.getStroke();
            canvas.setStroke(mVelocityStickStroke);
            k = 0;
            for (int i = 0; i < effectiveCount; i++) {
                mLineCache.setLine(mCachedVectorArray[k++], mCachedVectorArray[k++],
                        mCachedVectorArray[k++], mCachedVectorArray[k++]);
                canvas.draw(mLineCache);
            }

            canvas.setStroke(stroke);
            for (int i = 0; i < count; i++) {
                if (mPlanetInfoList.get(i + offset).mHasVelocity) {
                    int arrayIndex = i << 2; // offset = i * 4;
                    float x = mCachedVectorArray[arrayIndex + 2];
                    float y = mCachedVectorArray[arrayIndex + 3];
                    mOvalCache.setFrame(x - VECTOR_HEAD_RADIUS, y - VECTOR_HEAD_RADIUS,
                            2 * VECTOR_HEAD_RADIUS, 2 * VECTOR_HEAD_RADIUS);
                    canvas.fill(mOvalCache);
                }
            }
        }
    }

    private void onPaintCircumferences(Graphics2D canvas, int count) {
        mPointArrayIndex = 0;
        for (int i = 0; i < count; i++) {
            PlanetInfo planetInfo = mIterator.next();

            fillArrayWithPlanetCircumference(planetInfo);
        }

        AffineTransform matrix = getCartesianMatrix();

        count = mPointArrayIndex >> 2;

        matrix.transform(mCachedPointArray, 0, mCachedPointArray, 0, count << 1);

        canvas.setColor(mPlanetBackgroundColor);
        paintOvals(count, canvas::fill);

        canvas.setColor(mPlanetForegroundColor);
        canvas.setStroke(mPlanetBorderStroke);
        paintOvals(count, canvas::draw);
    }

    private void paintOvals(int count, Consumer<Shape> drawer) {
        for (int i = 0, k = 0; i < count; i++) {
            float x = mCachedPointArray[k++];
            float y = mCachedPointArray[k++];

            mOvalCache.setFrame(x, y, mCachedPointArray[k++] - x, mCachedPointArray[k++] - y);

            drawer.accept(mOvalCache);
        }
    }

    private void fillArrayWithPlanetCircumference(PlanetInfo planetInfo) {
        // check if the planet is inside the bounds of the screen
        // before transforming it to pixel coordinates
//        if ((planetInfo.mTop > mBottom) & (planetInfo.mBottom < mTop)
//                & (planetInfo.mLeft < mRight) & (planetInfo.mRight > mLeft)) {
        // the planet circumference has a high probability of intersecting the view bounds.
        // The circumference rectangular bounds can intersect with the view bounds, but
        // when only one of the coordinates of the rectangular bounds is inside the view
        // the circumference does not always intersect with the view bounds and not always
        // will be visible in the screen

        // must be in same order as the parameters
        // in Canvas#drawOval(float,float,float,float,Paint)

        // left, top, right, bottom

        // top and bottom are inverted because the assumption is that the scale in y is always
        // negative
        mCachedPointArray[mPointArrayIndex++] = planetInfo.mLeft;
        mCachedPointArray[mPointArrayIndex++] = planetInfo.mTop;
        mCachedPointArray[mPointArrayIndex++] = planetInfo.mRight;
        mCachedPointArray[mPointArrayIndex++] = planetInfo.mBottom;
//        }
    }

    public void clearPlanets() {
        mPlanetInfoList.clear();

        invalidateCounters();
        repaint();
    }

    private static double getDistanceSqr(double x, double y) {
        return x * x + y * y;
    }

    public interface OnCaptureClickListener {

        void onDrag(EditSimulatorComponent view, float x, float y);

        boolean onRelease(EditSimulatorComponent view, float x, float y);
    }

    public interface OnPlanetPressedListener {

        void onPlanetPressed(Planet planet, float x, float y);
    }

    public interface OnEmptyLongClickListener {

        void onLongClick(float x, float y);
    }

    public interface OnPlanetClickedListener {

        void onPlanetClicked(Planet planet);
    }

    private static class PlanetInfo {

        private Planet mPlanet;

        private float mTop;
        private float mLeft;

        private float mBottom;
        private float mRight;

        private float mVelocityX;
        private float mVelocityY;

        private boolean mHasVelocity;

        PlanetInfo(Planet planet) {
            setPlanet(planet);
        }

        PlanetInfo() {
        }

        void setPlanet(Planet planet) {
            mPlanet = planet;

            invalidatePlanet();
        }

        private void invalidatePlanet() {
            float r = mPlanet.getRadius();

            mLeft = (float) (mPlanet.getX() - r);
            mBottom = (float) (mPlanet.getY() + r);

            mTop = (float) (mPlanet.getY() - r);
            mRight = (float) (mPlanet.getX() + r);

            mHasVelocity = mPlanet.getVx() != 0 || mPlanet.getVy() != 0;

            if (mHasVelocity) {
                mVelocityX = (float) (mPlanet.getVx() + mPlanet.getX());
                mVelocityY = (float) (mPlanet.getVy() + mPlanet.getY());
            }
        }

        float getX() {
            return (float) mPlanet.getX();
        }

        float getY() {
            return (float) mPlanet.getY();
        }

        @Override
        public String toString() {
            return "PlanetInfo{" +
                    "mPlanet=" + mPlanet +
                    ", mTop=" + mTop +
                    ", mLeft=" + mLeft +
                    ", mBottom=" + mBottom +
                    ", mRight=" + mRight +
                    '}';
        }
    }
}
