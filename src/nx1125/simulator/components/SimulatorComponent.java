package nx1125.simulator.components;

import nx1125.simulator.simulation.Planet;
import nx1125.simulator.simulation.PlanetState;
import nx1125.simulator.simulation.Simulator;
import nx1125.simulator.simulation.elastic.AbstractElasticSimulator;
import nx1125.simulator.simulation.elastic.linear.LinearElasticSimulator;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.util.function.Consumer;

/**
 * TODO: document your custom view class.
 */
public class SimulatorComponent extends CartesianComponent {

    private static final String TAG = "SimulatorView";
    private static final float TOLERANCE_CLICK = 20;

    private final float[] mCachedClickPointToInverseTransform = new float[2];

    private Color mPlanetBackgroundColor;
    private Color mPlanetForegroundColor;

    private float mPlanetBorderWidth;
    private Stroke mPlanetBorderStroke;
    private int mPlanetCount = -1;
    // must four times than mPlanetCount
    private float[] mCachedPointArray;
    // must two times than mPlanetCount
    private float[] mTransformArray;
    private PlanetState[] mPlanetStateArray;
    private Planet[] mPlanetArray;
    private int mCartesianHorizontalMargin = 20;
    private int mCartesianVerticalMargin = 20;
    private Ellipse2D.Float mOvalCache = new Ellipse2D.Float();

    private OnPlanetClickedListener mOnPlanetClickedListener;
    private OnPlanetMouseMotionListener mOnPlanetMouseMotionListener;

    private int mPressedPlanetIndex = -1;
    private int mReleasedPlanetIndex = -1;

    private LineConnectionShape mLineConnectionShape;

    private Color mLinePathColor;

    private AbstractElasticSimulator mElasticSimulator;
    private Color mRestingRadiusBorderColor = Color.red;
    private Color mRingColor = Color.blue.brighter();

    public SimulatorComponent(Simulator simulator) {
        init();

        if (simulator instanceof AbstractElasticSimulator) {
            mElasticSimulator = (AbstractElasticSimulator) simulator;
        }
    }

    private void init() {
        mPlanetBackgroundColor = Color.gray.brighter();
        mPlanetForegroundColor = Color.black;

        mLinePathColor = Color.red;

        mPlanetBorderWidth = 1;
        mPlanetBorderStroke = new BasicStroke(mPlanetBorderWidth);

        mLineConnectionShape = new LineConnectionShape();
    }

    public void setPlanetArray(Planet[] planetArray) {
        mPlanetArray = planetArray;
        mPlanetCount = planetArray.length;
        mTransformArray = new float[mPlanetCount << 1];
        mCachedPointArray = new float[mPlanetCount << 2];
    }

    public void setPlanetStateArray(PlanetState[] states) {
        mPlanetStateArray = states;
    }

    public int getPlanetIndexAt(int pixelX, int pixelY) {
        setCachedPointArray(pixelX, pixelY);
        invert(mCachedClickPointToInverseTransform);

        float x = mCachedClickPointToInverseTransform[0];
        float y = mCachedClickPointToInverseTransform[1];

        // System.out.println("Checking clicked point (" + x + ", " + y + ')');

        float closestDistanceSqr = 0;
        PlanetState closestState = null;
        int closestStateIndex = 0;

        for (int i = 0; i < mPlanetCount; i++) {
            PlanetState s = mPlanetStateArray[i];

            float d = s.distanceSqr(x, y);

            if (closestState == null || closestDistanceSqr > d) {
                closestState = s;
                closestDistanceSqr = d;
                closestStateIndex = i;
            }
        }

        if (closestState != null) {
            float r = mPlanetArray[closestStateIndex].getRadius() + TOLERANCE_CLICK / getCartesianScale();

            if (closestDistanceSqr <= r * r) {
                return closestStateIndex;
            }
        }

        return -1;
    }

    @Override
    protected void onMouseClicked(MouseEvent e) {
        System.out.println("Checking clicked point (" + e.getX() + ", " + e.getY() + ')');

        if (mOnPlanetClickedListener != null && e.getButton() == MouseEvent.BUTTON1) {
            System.out.println("Checking if there is a planet at the position");
            if (mOnPlanetMouseMotionListener == null) {
                mReleasedPlanetIndex = getPlanetIndexAt(e.getX(), e.getY());
            }

            System.out.println("Clicked planet index: " + mReleasedPlanetIndex);

            if (mReleasedPlanetIndex != -1) {
                mOnPlanetClickedListener.onPlanetClicked(mReleasedPlanetIndex, mPlanetArray[mReleasedPlanetIndex]);
            } else {
                super.onMouseClicked(e);
            }
        } else {
            super.onMouseClicked(e);
        }
    }

    @Override
    protected void onMousePressed(MouseEvent e) {
        if (mOnPlanetMouseMotionListener != null && e.getButton() == MouseEvent.BUTTON1) {
            System.out.println("Checking pressed button (" + e.getX() + ", " + e.getY() + ')');
            mPressedPlanetIndex = getPlanetIndexAt(e.getX(), e.getY());
            mReleasedPlanetIndex = mPressedPlanetIndex;

            System.out.println("Pressed planet index: " + mPressedPlanetIndex);

            if (mPressedPlanetIndex != -1) {
                mOnPlanetMouseMotionListener.onMousePressed(mCachedClickPointToInverseTransform[0],
                        mCachedClickPointToInverseTransform[1],
                        mPressedPlanetIndex, mPlanetStateArray[mPressedPlanetIndex]);
            } else {
                super.onMousePressed(e);
            }
        } else {
            super.onMousePressed(e);
        }
    }

    @Override
    protected void onMouseDragged(MouseEvent e) {
        if (mPressedPlanetIndex != -1) {
            setCachedPointArray(e.getX(), e.getY());
            invert(mCachedClickPointToInverseTransform);

            mOnPlanetMouseMotionListener.onPlanetDragged(mCachedClickPointToInverseTransform[0],
                    mCachedClickPointToInverseTransform[1],
                    mPressedPlanetIndex, mPlanetStateArray[mPressedPlanetIndex]);
        } else {
            super.onMouseDragged(e);
        }
    }

    @Override
    protected void onMouseReleased(MouseEvent e) {
        if (mPressedPlanetIndex != -1) {
            setCachedPointArray(e.getX(), e.getY());
            invert(mCachedClickPointToInverseTransform);

            System.out.println("Released planet index " + mPressedPlanetIndex);

            mOnPlanetMouseMotionListener.onPlanetReleased(mCachedClickPointToInverseTransform[0],
                    mCachedClickPointToInverseTransform[1],
                    mPressedPlanetIndex, mPlanetStateArray[mPressedPlanetIndex]);

            mReleasedPlanetIndex = mPressedPlanetIndex;
            mPressedPlanetIndex = -1;
        } else {
            super.onMouseReleased(e);
        }
    }

    public void setOnPlanetMouseMotionListener(OnPlanetMouseMotionListener l) {
        mOnPlanetMouseMotionListener = l;

        mPressedPlanetIndex = -1;
    }

    public void setOnPlanetClickedListener(OnPlanetClickedListener l) {
        mOnPlanetClickedListener = l;
    }

    private void setCachedPointArray(float x, float y) {
        mCachedClickPointToInverseTransform[0] = x;
        mCachedClickPointToInverseTransform[1] = y;
    }

    public void lookAtPlanets() {
        if (mPlanetStateArray != null && mPlanetArray != null) {
            PlanetState state = mPlanetStateArray[0];

            float r = mPlanetArray[0].getRadius();

            //mRect.set((float) state.x - r, (float) state.y - r, (float) state.x + r, (float) state.y + r);

            float left = (float) state.x - r;
            float right = (float) state.x + r;
            float top = (float) state.y - r;
            float bottom = (float) state.y + r;

            for (int i = 1; i < mPlanetCount; i++) {
                state = mPlanetStateArray[i];
                r = mPlanetArray[i].getRadius();

                //mRect.union((float) state.x - r, (float) state.y - r, (float) state.x + r, (float) state.y + r);

                left = Math.min(left, (float) state.x - r);
                right = Math.max(right, (float) state.x + r);
                top = Math.min(top, (float) state.y - r);
                bottom = Math.max(bottom, (float) state.y + r);
            }

            lookAtWithoutAnimation(left, top, right, bottom,
                    mCartesianHorizontalMargin, mCartesianVerticalMargin, false);
        } else {
            lookAtWithoutAnimation(-0.5f, -0.5f, 0.5f, 0.5f,
                    mCartesianHorizontalMargin, mCartesianVerticalMargin, false);
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (mPlanetArray != null && mPlanetStateArray != null) {
            AffineTransform matrix = getCartesianMatrix();
            Graphics2D canvas = (Graphics2D) g;

            prepareCircumferenceArray(matrix);

            canvas.setColor(mPlanetBackgroundColor);
            onPaintCircumferences(canvas::fill);

            // draw lines between all planets in sequence
            canvas.setColor(mLinePathColor);
            canvas.draw(mLineConnectionShape);

            canvas.setStroke(mPlanetBorderStroke);

            if (mElasticSimulator != null) {
                float radius = (float) (getCartesianScale() * mElasticSimulator.getRestingDistance());
                float radius2 = radius * 2;

                canvas.setColor(mRestingRadiusBorderColor);
                for (int i = 0, j = 0; i < mPlanetStateArray.length; i++) {
                    mOvalCache.setFrame(mTransformArray[j++] - radius, mTransformArray[j++] - radius,
                            radius2, radius2);

                    canvas.draw(mOvalCache);
                }

                if (mElasticSimulator instanceof LinearElasticSimulator) {
                    LinearElasticSimulator linear = (LinearElasticSimulator) mElasticSimulator;

                    canvas.setColor(mRingColor);

                    float scale = getCartesianScale();

                    for (int i = 0, j = 0; i < mPlanetStateArray.length; i++) {
                        float dx = (float) (linear.getRingTrigonometryPart(j)) * scale;
                        float x = mTransformArray[j++];
                        float dy = (float) (linear.getRingTrigonometryPart(j)) * scale;
                        float y = mTransformArray[j++];

                        canvas.drawLine((int) (x - dx), (int) (y - dy), (int) (x + dx), (int) (y + dy));
                    }
                }

                radius = (float) (getCartesianScale() * mElasticSimulator.getRestingDistance());
                radius2 = radius * 2;

                canvas.setColor(mRestingRadiusBorderColor);
                for (int i = 0, j = 0; i < mPlanetStateArray.length; i++) {
                    mOvalCache.setFrame(mTransformArray[j++] - radius, mTransformArray[j++] - radius,
                            radius2, radius2);

                    canvas.draw(mOvalCache);
                }
            }

            canvas.setColor(mPlanetForegroundColor);
            onPaintCircumferences(canvas::draw);

        }
    }

    /**
     * Fill {@link #mCachedPointArray} with the left, top, right and bottom of each planet and transform
     * it.
     */
    private void prepareCircumferenceArray(AffineTransform matrix) {
        // long time = System.currentTimeMillis();

        for (int i = 0, k = 0; i < mPlanetCount; i++) {
            PlanetState state = mPlanetStateArray[i];

            mTransformArray[k++] = (float) state.x;
            mTransformArray[k++] = (float) state.y;
        }

        // Log.d(TAG, "prepareCircumferenceArray: Time to get state position = " + (System.currentTimeMillis() - time));
        // time = System.currentTimeMillis();

        matrix.transform(mTransformArray, 0, mTransformArray, 0, mTransformArray.length >>> 1);

        // Log.d(TAG, "prepareCircumferenceArray: Time to transform points = " + (System.currentTimeMillis() - time));
        // time = System.currentTimeMillis();

        float scale = getCartesianScale();

        // compute each bounds because it will be used twice
        for (int i = 0, centerIndex = 0, pointIndex = 0; i < mPlanetCount; i++) {
            float r = mPlanetArray[i].getRadius() * scale;

            float x = mTransformArray[centerIndex++];
            float y = mTransformArray[centerIndex++];

            // left, top, right, bottom
            mCachedPointArray[pointIndex++] = x - r;
            mCachedPointArray[pointIndex++] = y - r;
            mCachedPointArray[pointIndex++] = x + r;
            mCachedPointArray[pointIndex++] = y + r;
        }

        // Log.d(TAG, "prepareCircumferenceArray: Time to populate mCachedPointArray: " + (System.currentTimeMillis() - time));
    }

    private void onPaintCircumferences(Consumer<Shape> drawer) {
        // long time = System.currentTimeMillis();
        for (int i = 0, k = 0; i < mPlanetCount; i++) {
            float x = mCachedPointArray[k++];
            float y = mCachedPointArray[k++];

            mOvalCache.setFrame(x, y,
                    mCachedPointArray[k++] - x, mCachedPointArray[k++] - y);
            drawer.accept(mOvalCache);
        }
        // Log.d(TAG, "onPaintCircumferences: Time to paint circumferences: " + (System.currentTimeMillis() - time));
    }

    /**
     * Move values from {@link #mTransformArray} to {@link #mCachedPointArray}. The value at index i
     * in {@link #mTransformArray} is moved to the index ((i / 2) * 4 + offset + (i % 2)) of
     * {@link #mCachedPointArray}.
     */
    private void stridePointArray(int offset) {
        for (int i = 0, transformIndex = 0, pointIndex = offset; i < mPlanetCount; i++) {
            mCachedPointArray[pointIndex] = mTransformArray[transformIndex++];
            mCachedPointArray[pointIndex + 1] = mTransformArray[transformIndex++];

            pointIndex += 4;
        }
    }

    public PlanetState[] getPlanetStates() {
        return mPlanetStateArray;
    }

    public interface OnPlanetClickedListener {

        void onPlanetClicked(int index, Planet planet);
    }

    public interface OnPlanetMouseMotionListener {

        void onMousePressed(float x, float y, int index, PlanetState planet);

        void onPlanetDragged(float x, float y, int index, PlanetState planet);

        void onPlanetReleased(float x, float y, int index, PlanetState planet);
    }

    private class LineConnectionShape implements Shape {

        @Override
        public Rectangle getBounds() {
            return getBounds2D().getBounds();
        }

        @Override
        public Rectangle2D getBounds2D() {
            Rectangle2D.Float bounds = new Rectangle2D.Float(mTransformArray[0], mTransformArray[1], 0, 0);

            for (int i = 1, j = 2; i < mPlanetCount; i++) {
                bounds.add(mTransformArray[j++], mTransformArray[j++]);
            }

            return bounds;
        }

        @Override
        public boolean contains(double x, double y) {
            return false;
        }

        @Override
        public boolean contains(Point2D p) {
            return false;
        }

        @Override
        public boolean intersects(double x, double y, double w, double h) {
            return true;
        }

        @Override
        public boolean intersects(Rectangle2D r) {
            return true;
        }

        @Override
        public boolean contains(double x, double y, double w, double h) {
            return false;
        }

        @Override
        public boolean contains(Rectangle2D r) {
            return false;
        }

        @Override
        public PathIterator getPathIterator(AffineTransform at) {
            return new PathIterator() {

                private final float[] mArray = mTransformArray.clone();

                private int mPointIndex;

                @Override
                public int getWindingRule() {
                    return PathIterator.WIND_NON_ZERO;
                }

                @Override
                public boolean isDone() {
                    return mPointIndex >= mPlanetCount;
                }

                @Override
                public void next() {
                    mPointIndex++;
                }

                @Override
                public int currentSegment(float[] coords) {
                    int i = mPointIndex << 1;

                    coords[0] = mArray[i];
                    coords[1] = mArray[i + 1];

                    if (mPointIndex == 0) {
                        return PathIterator.SEG_MOVETO;
                    }

                    return PathIterator.SEG_LINETO;
                }

                @Override
                public int currentSegment(double[] coords) {
                    int i = mPointIndex << 1;

                    coords[0] = mArray[i];
                    coords[1] = mArray[i + 1];

                    if (mPointIndex == 0) {
                        return PathIterator.SEG_MOVETO;
                    }

                    return PathIterator.SEG_LINETO;
                }
            };
        }

        @Override
        public PathIterator getPathIterator(AffineTransform at, double flatness) {
            return getPathIterator(at);
        }
    }
}
