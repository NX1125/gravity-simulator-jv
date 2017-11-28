package nx1125.simulator.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;

/**
 * This view paints the Cartesian system.
 * <p>
 * The plane has a origin and a line in vertical and horizontal is painted at the origin as a cross.
 * The origin can be moved to a position of the screen and does not need to be inside the view
 * bounds.
 * <p>
 * The Cartesian plane has a scale in both axis, The {@link CartesianComponent} has an scale that is
 * equals in both axis. The scale is used to paint inner lines and transform points from the plane
 * to the coordinates in pixels in the view.
 * <p>
 * {@link CartesianComponent} paint lines of unit. An unit is a demarcation of the plane that is a power
 * of ten. At each unit in horizontal or vertical with the origin as reference, a line is painted
 * at it. The unit is defined as the power of ten with the index as the logarithm of the division
 * between a factor and the scale.
 */
public class CartesianComponent extends JComponent {

    private static final float MIN_SCALE = 1.0E-2f;
    private static final float MAX_SCALE = 1.0E+4f;

    private static final int LINE_COUNT = 20;

    private static final int INNER_UNIT_LINE_ALPHA = 255;

    private static final String TAG = "CartesianView";

    private static final boolean DEBUG = true;

    private static final int INNER_AXIS_COMPOSITE_RULE = AlphaComposite.SRC_OVER;

    private static final float SCALE_PROPORTION = 1.1f;

    /*
     * invalidate origin
     *  invalidate indexes
     *  invalidate matrix
     * invalidate scale
     *  invalidate matrix
     *  invalidate unit
     *   invalidate indexes
     *
     */

    private float mLogarithmFactor = (float) Math.log10(150) + 1;

    private float mOriginX;
    private float mOriginY;

    private float mScale;

    private float mInverseScale;

    private AffineTransform mMatrix;

    private Color mBackgroundColor;

    private Stroke mOriginAxisStroke;
    private Stroke mUnitAxisStroke;
    private Stroke mInnerAxisStroke;

    private Color mOriginAxisColor;
    private Color mUnitAxisColor;
    private Color mInnerAxisColor;

    private float mOriginAxisWidth;
    private float mUnitAxisWidth;
    private float mInnerAxisWidth;

    private AlphaComposite mInnerAxisAlphaComposite;

    private int mCachedLogarithmIntegerPart = Integer.MAX_VALUE;

    private float mUnit;
    private float mInnerUnit;

    private int mLowMultipleXIndex;
    private int mTransformedLowMultipleX;

    private int mHighMultipleXIndex;
    private int mTransformedHighMultipleX;

    private int mHighMultipleYIndex;
    private int mTransformedHighMultipleY;

    private int mLowMultipleYIndex;
    private int mTransformedLowMultipleY;

    private int mXIndexDifference;
    private int mYIndexDifference;

    private float mUnitLengthPixels;

    private int mCachedBottomY;
    private int mCachedRightX;

    private int mCachedContentWidth;
    private int mCachedContentHeight;

    private float mInnerUnitLengthPixels;

    private boolean mMovementEnabled = true;

    private boolean mMovementTriggered = false;

    /**
     * The translate reference meaning depends on the time of translation type.
     * <p>
     * The simple translation type occurs when the user uses only one finger to move the origin of
     * the CartesianView. Since there is no change in scale, the reference is the distance between
     * the origin and the down event touch position. In the following events of move touch, the
     * origin is the sum of the reference and the touch position. mTranslateReferenceX is the
     * distance in the x axis.
     * <p>
     * The scale and translation type occurs when the user uses two fingers to translate and scale
     * the CartesianView. Since the scale changes, saving the distance in pixels does not make sense
     * because the distance in pixels between two points in the plane depends of the scale.
     * The reference is the plane point of the middle point between of the line between both fingers
     * when the second finger started as down. mTranslateReferenceX is the x coordinate of the
     * finger in the plane domain.
     */
    private float mTranslateReferenceX;

    /**
     * The translate reference meaning depends on the time of translation type.
     * <p>
     * The simple translation type occurs when the user uses only one finger to move the origin of
     * the CartesianView. Since there is no change in scale, the reference is the distance between
     * the origin and the down event touch position. In the following events of move touch, the
     * origin is the sum of the reference and the touch position. mTranslateReferenceY is the
     * distance in the y axis.
     * <p>
     * The scale and translation type occurs when the user uses two fingers to translate and scale
     * the CartesianView. Since the scale changes, saving the distance in pixels does not make sense
     * because the distance in pixels between two points in the plane depends of the scale.
     * The reference is the plane point of the middle point between of the line between both fingers
     * when the second finger started as down. mTranslateReferenceY is the y coordinate of the
     * finger in the plane domain.
     */
    private float mTranslateReferenceY;

    private float mLastReleaseX = 0;
    private float mLastReleaseY = 0;

    private float mLowX;
    private float mHighX;

    private float mLowY;
    private float mHighY;

    private boolean mInvalidateWhenMatrixIsInvalidatedEnabled = true;

    public CartesianComponent() {
        init();
    }

    private void init() {
        mInnerAxisColor = Color.black;
        mInnerAxisWidth = 1;
        mInnerAxisStroke = new BasicStroke(mInnerAxisWidth);
        mInnerAxisAlphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1);

        mUnitAxisColor = new Color(Color.black.getRed(), Color.black.getGreen(),
                Color.black.getBlue(), INNER_UNIT_LINE_ALPHA);
        mUnitAxisWidth = 1;
        mUnitAxisStroke = new BasicStroke(mInnerAxisWidth);

        mOriginAxisColor = Color.red;
        mOriginAxisWidth = 2;
        mOriginAxisStroke = new BasicStroke(mInnerAxisWidth);

        mOriginX = 320;
        mOriginY = 250;

        mScale = 20;

        mMatrix = new AffineTransform();

        invalidateScale();
        invalidateOrigin();

        initListeners();

        setPreferredSize(new Dimension(640, 500));
    }

    private String toReferenceTranslateString() {
        return toPositionString(mTranslateReferenceX, mTranslateReferenceY);
    }

    public float getLowX() {
        return mLowX;
    }

    public float getHighX() {
        return mHighX;
    }

    public float getLowY() {
        return mLowY;
    }

    public float getHighY() {
        return mHighY;
    }

    public void invert(float[] point) {
        float x = point[0];
        float y = point[1];

        point[0] = (x - mOriginX) / mScale;
        point[1] = (y - mOriginY) / mScale;
    }

    private void onSimpleTranslationTriggered(MouseEvent event) {
        mTranslateReferenceX = mOriginX - event.getX();
        mTranslateReferenceY = mOriginY - event.getY();

//        Log.d(TAG, "onSimpleTranslationTriggered: Distance between origin and pointer = " + toReferenceTranslateString());
    }

    private void initListeners() {
        MouseAdapter adapter = new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                onMousePressed(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                onMouseReleased(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                onMouseDragged(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                onMouseClicked(e);
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                onMouseWheelMoved(e);
            }
        };

        addMouseListener(adapter);
        addMouseMotionListener(adapter);
        addMouseWheelListener(adapter);
    }

    protected void onMousePressed(MouseEvent event) {
        if (event.getButton() == MouseEvent.BUTTON1 || event.getButton() == MouseEvent.BUTTON2) {
            // first touch on screen. This triggers the simple translation by touch
            debug("Starting simple translation from point (" + event.getX() + ", " + event.getY() + ')');

            mMovementTriggered = true;

            onSimpleTranslationTriggered(event);

            onCartesianMovementStarted();
        }
    }

    protected void onMouseDragged(MouseEvent e) {
        if (mMovementTriggered) {
            // debug("onTouchEvent: Simple translation event");
            // simple translation

            mOriginX = mTranslateReferenceX + e.getX();
            mOriginY = mTranslateReferenceY + e.getY();

            invalidateOrigin();

            onCartesianMoved();

            invalidateIfEnabled();
        }
    }

    protected void onMouseReleased(MouseEvent e) {
//                Log.d(TAG, "onTouchEvent: End of translation at " + toPositionString(event));

        if (mMovementTriggered) {
            mMovementTriggered = false;

            mLastReleaseX = e.getX();
            mLastReleaseY = e.getY();

            onCartesianMovementEnded();
        }
    }

    protected void onMouseClicked(MouseEvent e) {
    }

    protected void onMouseWheelMoved(MouseWheelEvent e) {
        float translateReferenceX = (e.getX() - mOriginX) * mInverseScale;
        float translateReferenceY = (e.getY() - mOriginY) * mInverseScale;

        mScale *= e.getWheelRotation() > 0 ? 1.0 / SCALE_PROPORTION : SCALE_PROPORTION;

        mOriginX = e.getX() - mScale * translateReferenceX;
        mOriginY = e.getY() - mScale * translateReferenceY;

        // does not need to invalidateOrigin because invalidateScale also
        // invalidate origin dependencies
        invalidateScale();

        onCartesianMoved();

        invalidateIfEnabled();
    }

    protected void onCartesianMovementStarted() {
    }

    protected void onCartesianMoved() {
    }

    protected void onCartesianMovementEnded() {
    }

    public void setInvalidateWhenMatrixIsInvalidatedEnabled(boolean enabled) {
        mInvalidateWhenMatrixIsInvalidatedEnabled = enabled;
    }

    private void invalidateIfEnabled() {
        if (mInvalidateWhenMatrixIsInvalidatedEnabled)
            repaint();
    }

    public AffineTransform getCartesianMatrix() {
        return mMatrix;
    }

    public void transformPoints(float[] points, int offset, int count) {
        mMatrix.transform(points, offset, points, offset, count);
    }

    public void transformPoints(float[] points) {
        mMatrix.transform(points, 0, points, 0, points.length / 2);
    }

    public float getLastReleaseX() {
        return mLastReleaseX;
    }

    public float getLastReleaseY() {
        return mLastReleaseY;
    }

    public boolean isMovementEnabled() {
        return mMovementEnabled;
    }

    public void setMovementEnabled(boolean enabled) {
        mMovementEnabled = enabled;
        mMovementTriggered &= enabled;

        debug("setMovementEnabled: state = " + enabled);
    }

    public boolean isMovementTriggered() {
        return mMovementTriggered;
    }

    public void setOriginLineWidth(float width) {
        mOriginAxisWidth = width;
        mOriginAxisStroke = new BasicStroke(width);

        repaint();
    }

    public void setOriginLineColor(Color color) {
        mOriginAxisColor = color;

        repaint();
    }

    public void setOrigin(float x, float y) {
        if (x != mOriginX || y != mOriginY) {
            mOriginX = x;
            mOriginY = y;

            invalidateOrigin();

            invalidateIfEnabled();
        }
    }

    /**
     * Invalidates the actual scale and update fields according to the new scale. The invalidate of
     * scale contains the invalidation of the origin.
     */
    private void invalidateScale() {
//        if (BuildConfig.DEBUG) {
//            Log.d(TAG, "invalidateScale: scale = " + mScale);
//        }

        mInverseScale = 1.0f / mScale;

        invalidateMatrix();
        invalidateUnit();
    }

    private void invalidateOrigin() {
        invalidateMatrix();
        invalidateMultiples();
    }

    private void invalidateMultiples() {
        mLowX = -mOriginX * mInverseScale;
        mHighX = (mCachedContentWidth - mOriginX) * mInverseScale;

        mLowY = -mOriginY * mInverseScale;
        mHighY = (mCachedContentHeight - mOriginY) * mInverseScale;

        float inverseUnit = 1.0f / mUnit;

        mLowMultipleXIndex = (int) (mLowX * inverseUnit);
        mHighMultipleXIndex = (int) (mHighX * inverseUnit);

        mLowMultipleYIndex = (int) (mLowY * inverseUnit);
        mHighMultipleYIndex = (int) (mHighY * inverseUnit);

        mTransformedLowMultipleX = transformX(mLowMultipleXIndex * mUnit);
        mTransformedHighMultipleX = transformX(mHighMultipleXIndex * mUnit);

        mTransformedLowMultipleY = transformY(mLowMultipleYIndex * mUnit);
        mTransformedHighMultipleY = transformY(mHighMultipleYIndex * mUnit);

        mUnitLengthPixels = mUnit * mScale;
        mInnerUnitLengthPixels = mInnerUnit * mScale;

        mXIndexDifference = Math.abs(mLowMultipleXIndex - mHighMultipleXIndex) + 1;
        mYIndexDifference = Math.abs(mLowMultipleYIndex - mHighMultipleYIndex) + 1;
    }

    private void invalidateUnit() {
        // negative because it need the unit side not the pixel side, and so the logarithm of
        // the inverse of scale is the same as the negative of the logarithm of scale
        // which is faster
        float logarithm = mLogarithmFactor - (float) Math.log10(mScale);
        int i = (int) logarithm;
        float alpha = 1 - Math.abs(logarithm - i);

        if (i != mCachedLogarithmIntegerPart) {
            mUnit = power10(i);
            mInnerUnit = mUnit * 0.1f;

            if (DEBUG) debug(" invalidateUnit: New unit: " + mUnit);

            mCachedLogarithmIntegerPart = i;
        }

        invalidateMultiples();

        mInnerAxisAlphaComposite = AlphaComposite.getInstance(INNER_AXIS_COMPOSITE_RULE, alpha);
    }

    public void lookAt(float left, float top, float right, float bottom, int horizontal, int vertical) {
        float s = Math.min(
                (getWidth() - 2 * horizontal) / (right - left),
                (getHeight() - 2 * vertical) / (bottom - top)
        );

        float h = horizontal / s;
        float v = vertical / s;

        setPortViewBounds(left - h, top - v, right + h, bottom + v);

        debug("setPortViewBounds: Looking at CartesianView with scale " + s);
    }

    public void lookAtWithoutAnimation(float left, float top, float right, float bottom, int horizontal, int vertical, boolean invalidate) {
        float s = Math.min(
                (getWidth() - 2 * horizontal) / (right - left),
                (getHeight() - 2 * vertical) / (bottom - top)
        );

        float h = horizontal / s;
        float v = vertical / s;

        setPortViewBounds0(left - h, top - v, right + h, bottom + v, invalidate);

        debug("setPortViewBounds: Looking at CartesianView with scale " + s);
    }

    public void setPortViewBounds(float left, float top, float right, float bottom) {
        setPortViewBounds0(left, top, right, bottom, true);
    }

    protected void setPortViewBounds0(float left, float top, float right, float bottom, boolean invalidate) {
        mScale = Math.min(mCachedContentWidth / (right - left), mCachedContentHeight / (bottom - top));

        // the middle of the simulation is also the middle of the passed parameters

        mOriginX = (mCachedContentWidth - mScale * (left + right)) * 0.5f;
        mOriginY = (mCachedContentHeight - mScale * (top + bottom)) * 0.5f;

        invalidateScale();

        if (invalidate) repaint();
    }

    private void invalidateMatrix() {
        mMatrix.setToTranslation(mOriginX, mOriginY);
        // this must be a pre scale
        mMatrix.scale(mScale, mScale);
    }

    public float getCartesianScale() {
        return mScale;
    }

    public void setCartesianScale(float scale) {
        scale = clipScale(scale);
        if (mScale != scale) {
            mScale = scale;

            invalidateScale();

            invalidateIfEnabled();
        }
    }

    @Override
    public void doLayout() {
        mCachedContentWidth = getWidth();
        mCachedContentHeight = getHeight();

        mCachedBottomY = mCachedContentHeight;
        mCachedRightX = mCachedContentWidth;

        invalidateMultiples();
    }

    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);

        mBackgroundColor = bg;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D canvas = (Graphics2D) graphics;
        //long time = System.currentTimeMillis();

        if (isOpaque()) {
            canvas.setBackground(mBackgroundColor);
            canvas.clearRect(0, 0, getWidth(), getHeight());
        }

        // for each unit, nine inner lines in vertical and horizontal are painted. Nine because
        // the inner unit is 10 times smaller than mUnit and so there is ten spaces and between
        // two spaces there is an inner unit. Since there is ten spaces, there are nine lines.
        // The origin lines are also not drawn because it is handled as a unit line.

        // set the inner axis paint
        canvas.setColor(mInnerAxisColor);
        canvas.setStroke(mInnerAxisStroke);

        Composite oldComposite = canvas.getComposite();
        canvas.setComposite(mInnerAxisAlphaComposite);

        // draw each vertical inner line
        float innerX = mTransformedLowMultipleX - mUnitLengthPixels;
        // int k;
        for (int i = mLowMultipleXIndex - 1, limit = mHighMultipleXIndex + 1; i < limit; i++) {
            // each unit, draw 9 inner lines in it
            innerX += mInnerUnitLengthPixels;
            int k = 0;
            while (k < 9) {
                canvas.drawLine((int) innerX, 0, (int) innerX, mCachedBottomY);

                innerX += mInnerUnitLengthPixels;

                k++;
            }
        }

        // draw each horizontal inner line
        float innerY = mTransformedLowMultipleY - mUnitLengthPixels;
        for (int i = mLowMultipleYIndex - 1, limit = mHighMultipleYIndex + 1; i < limit; i++) {
            // each unit, draw 9 inner lines in it
            innerY += mInnerUnitLengthPixels;
            int k = 0;
            while (k < 9) {
                canvas.drawLine(0, (int) innerY, mCachedRightX, (int) innerY);

                innerY += mInnerUnitLengthPixels;

                k++;
            }
        }

        canvas.setComposite(oldComposite);

        // Log.d(TAG, "onDraw: Time to paint inner lines = " + (System.currentTimeMillis() - innerLinesOffsetTime));

        canvas.setStroke(mUnitAxisStroke);
        canvas.setColor(mUnitAxisColor);

        // draw vertical unit lines
        if (mLowMultipleXIndex <= 0 && mHighMultipleXIndex >= 0) {
            // paint each vertical line between
            // mLowMultipleXIndex and mHighMultipleXIndex except the center axis
            // there is a sequence before the origin and after the origin
            if (mLowMultipleXIndex < 0) {
                // there is a sequence before the origin
                drawVerticalLinesSequence(mTransformedLowMultipleX, mUnitLengthPixels,
                        -mLowMultipleXIndex, canvas);
            }
            if (mHighMultipleXIndex > 0) {
                drawVerticalLinesSequence(mTransformedHighMultipleX, -mUnitLengthPixels,
                        mHighMultipleXIndex, canvas);
            }
        } else {
            // the origin axis is not in screen
            drawVerticalLinesSequence(mTransformedLowMultipleX, mUnitLengthPixels,
                    mXIndexDifference, canvas);
        }

        // draw horizontal unit lines
        if (mLowMultipleYIndex <= 0 && mHighMultipleYIndex >= 0) {
            // paint each horizontal line between
            // mLowMultipleXIndex and mHighMultipleXIndex except the center axis
            // there is a sequence before the origin and after the origin
            if (mLowMultipleYIndex < 0) {
                // there is a sequence before the origin
                drawHorizontalLinesSequence(mTransformedLowMultipleY, mUnitLengthPixels,
                        -mLowMultipleYIndex, canvas);
            }
            if (mHighMultipleYIndex > 0) {
                drawHorizontalLinesSequence(mTransformedHighMultipleY, -mUnitLengthPixels,
                        mHighMultipleYIndex, canvas);
            }
        } else {
            // the origin axis is not in screen
            drawHorizontalLinesSequence(mTransformedLowMultipleY, mUnitLengthPixels,
                    mYIndexDifference, canvas);
        }

        canvas.setColor(mOriginAxisColor);
        canvas.setStroke(mOriginAxisStroke);

        // draw vertical axis
        if (mLowMultipleXIndex <= 0 && mHighMultipleXIndex >= 0) {
            // the horizontal line is inside the view bounds
            canvas.drawLine((int) mOriginX, 0,
                    (int) mOriginX, mCachedBottomY);
        }
        // draw horizontal axis and unit lines
        if (mLowMultipleYIndex <= 0 && mHighMultipleYIndex >= 0) {
            // the vertical line is inside the view bounds so draw it
            canvas.drawLine(0, (int) mOriginY,
                    mCachedRightX, (int) mOriginY);
        }

        //long innerLinesOffsetTime = System.currentTimeMillis();
        //debug("onDraw: Time to paint axis and unit lines: " + (innerLinesOffsetTime - time));
    }

    private int transformX(float x) {
        return (int) (x * mScale + mOriginX);
    }

    private int transformY(float y) {
        return (int) (y * mScale + mOriginY);
    }

    private void drawHorizontalLinesSequence(float y, float dy, int count,
                                             Graphics2D canvas) {
        int steps = count / LINE_COUNT;
        while (steps-- > 0) {
            for (int i = 0; i < LINE_COUNT; i++) {
                canvas.drawLine(0, (int) y, mCachedRightX, (int) y);

                y += dy;
            }
        }
        count %= LINE_COUNT;
        int i = count % LINE_COUNT;
        while (i-- > 0) {
            canvas.drawLine(0, (int) y, mCachedRightX, (int) y);

            y += dy;
        }
    }

    private void drawVerticalLinesSequence(float x, float dx, int count,
                                           Graphics2D canvas) {
        int steps = count / LINE_COUNT;
        while (steps-- > 0) {
            for (int i = 0; i < LINE_COUNT; i++) {
                canvas.drawLine((int) x, 0, (int) x, mCachedBottomY);

                x += dx;
            }
        }
        count %= LINE_COUNT;
        int i = count % LINE_COUNT;
        while (i-- > 0) {
            canvas.drawLine((int) x, 0, (int) x, mCachedBottomY);

            x += dx;
        }
    }

    private static String toPositionString(MouseEvent event) {
        return toPositionString(event.getX(), event.getY());
    }

    private static String toPositionString(float x, float y) {
        return '(' + (x + ", " + y) + ')';
    }

    private static void debug(String msg) {
        System.out.println(TAG + "/D " + msg);
    }

    private static float getDistanceSqr(float x, float y) {
        return x * x + y * y;
    }

    private static float clipScale(float scale) {
        if (scale < MIN_SCALE) {
            return MIN_SCALE;
        } else if (scale > MAX_SCALE) {
            return MAX_SCALE;
        } else {
            return scale;
        }
    }

    private static float power10(int i) {
        float p;
        float step;

        if (i > 0) {
            step = 10;
        } else if (i < 0) {
            step = 0.1f;
            i = -i;
        } else {
            return 1;
        }

        p = 1;
        while (i > 0) {
            p *= step;
            i--;
        }
        return p;

    }

    private static float getLinearValue(float v0, float v1, float t) {
        return v0 + (v1 - v0) * t;
    }
}
