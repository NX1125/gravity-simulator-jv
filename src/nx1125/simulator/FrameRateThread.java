package nx1125.simulator;

/**
 * Created by guilh on 01/10/2017.
 */

public class FrameRateThread extends Thread {

    private static final String TAG = "FrameRateThread";

    private static final boolean DEBUG = false;

    private final int mFrameRate;

    private final int mMillisecondsByFrame;
    private final OnFrameUpdateListener mOnFrameUpdateListener;
    private int mFrame;
    private boolean mInvalidated;
    private boolean mPlay = true;

    private int mStatesByCycle;

    private long mActualFrameTimeLength;

    private int mStateCount;

    public FrameRateThread(int frameRate, int statesByCycle, int stateCount,
                           OnFrameUpdateListener onFrameUpdateListener) {
        super("FrameRateThread");

        mStateCount = stateCount;
        mFrameRate = frameRate;
        mStatesByCycle = statesByCycle;

        mOnFrameUpdateListener = onFrameUpdateListener;

        mMillisecondsByFrame = 1000 / mFrameRate;

        debug("FrameRateThread: Time of a frame: " + mMillisecondsByFrame);
    }

    public int getFrame() {
        return mFrame;
    }

    @Override
    public void run() {
        long lastFrameTime = System.currentTimeMillis();

        mFrame = 0;

        while (!Thread.interrupted()) {
            if (!mPlay) {
                debug("run: Entering pause loop and listen to invalidate events only. Actual frame = " + mFrame);
                mOnFrameUpdateListener.onPause();
                while (!mPlay) {
                    if (Thread.interrupted()) {
                        return;
                    }
                    revalidate();
                }
                debug("run: Exiting pause loop and starting play loop. Frame = " + mFrame);
                mOnFrameUpdateListener.onPlay();
            }

            if (DEBUG & (mFrame % mFrameRate) == 0) {
                debug("onUpdateFrame: Frame = " + mFrame);
            }

            mOnFrameUpdateListener.onFrameChanged(this, mFrame);

            // debug("run: Time = " + lastFrameTime);

            long time;
            do {
                revalidate();

                time = System.currentTimeMillis();
            } while ((time - lastFrameTime) < mMillisecondsByFrame);

            mActualFrameTimeLength = time - lastFrameTime;

            lastFrameTime = time;

            mFrame++;
        }
    }

    private void revalidate() {
        if (mInvalidated) {
            if (DEBUG) debug("onRevalidate frame rate thread");

            mInvalidated = false;
            mOnFrameUpdateListener.onInvalidate(this);
        }
    }

    public void setPlay(boolean play) {
        if (play != mPlay) {
            mPlay = play;

            debug("setPlay: Changing state of FrameRateThread to " + play);
        }
    }

    public boolean isPlaying() {
        return mPlay;
    }

    public int getFrameRate() {
        return mFrameRate;
    }

    public void pause() {
        setPlay(false);
    }

    public void play() {
        setPlay(true);
    }

    /**
     * Return true if the frame started a new cycle. A cycle occurs at each {@link #getFrameRate()}
     * frames. The initial frame does count as a cycle frame.
     */
    public boolean isCycleStart() {
        return mFrame % mFrameRate == 0;
    }

    public boolean isInitialFrame() {
        return mFrame == 0;
    }

    public long getActualFrameTimeLength() {
        return mActualFrameTimeLength;
    }

    private void postInvalidate() {
        mInvalidated = true;
    }

    public int getCycle() {
        return mFrame / mFrameRate;
    }

    public void setCycle(int time) {
        int frame = time * mFrameRate;

        if (frame != mFrameRate) {
            // debug("Changing actual frame to " + frame + ". (Not defined from FrameRateThread)");

            mFrame = frame;
            postInvalidate();
        }
    }

    public int getSimulationState() {
        return Math.min((mFrame / mFrameRate) * mStatesByCycle +
                (((mFrame % mFrameRate) * mStatesByCycle) / mFrameRate), mStateCount);
    }

    private static void debug(String msg) {
        System.out.println(TAG + "/D " + msg);
    }

    public interface OnFrameUpdateListener {

        void onPlay();

        void onPause();

        /**
         * Called when the frame changes.
         */
        void onFrameChanged(FrameRateThread thread, int frameIndex);

        /**
         * Called when the thread is invalidated after {@link #onFrameChanged(FrameRateThread, int)} is called.
         */
        void onInvalidate(FrameRateThread thread);
    }
}
