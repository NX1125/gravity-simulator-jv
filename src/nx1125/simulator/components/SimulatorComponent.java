package nx1125.simulator.components;

import nx1125.simulator.simulation.Planet;
import nx1125.simulator.simulation.PlanetState;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.util.function.Consumer;

/**
 * TODO: document your custom view class.
 */
public class SimulatorComponent extends CartesianComponent {

    private static final String TAG = "SimulatorView";
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

    public SimulatorComponent() {
        init();
    }

    private void init() {
        mPlanetBackgroundColor = Color.gray.brighter();
        mPlanetForegroundColor = Color.black;

        mPlanetBorderWidth = 1;
        mPlanetBorderStroke = new BasicStroke(mPlanetBorderWidth);
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

        for (int i = 0; i < mPlanetCount; i++) {
            PlanetState s = mPlanetStateArray[i];

            if (s.distanceSqr(x, y) <= mPlanetArray[i].getRadiusSqr()) {
                return i;
            }
        }

        return -1;
    }

    @Override
    protected void onMouseClicked(MouseEvent e) {
        if (mOnPlanetClickedListener != null) {
            int index = getPlanetIndexAt(e.getX(), e.getY());
            if (index != -1) {
                mOnPlanetClickedListener.onPlanetClicked(index, mPlanetArray[index]);
            } else {
                super.onMouseClicked(e);
            }
        } else {
            super.onMouseClicked(e);
        }
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

            canvas.setColor(mPlanetForegroundColor);
            canvas.setStroke(mPlanetBorderStroke);
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
}
