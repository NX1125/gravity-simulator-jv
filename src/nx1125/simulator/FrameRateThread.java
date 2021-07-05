package nx1125.simulator;

import nx1125.simulator.simulation.Simulator;
import nx1125.simulator.windows.EnergyStatsDialog;
import nx1125.simulator.windows.SimulationDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by guilh on 01/10/2017.
 */

public class FrameRateThread {

    public static final int INNER_STATES_COUNT = 100;

    private static final String TAG = "FrameRateThread";

    private static final boolean DEBUG = false;

    private final Object mProcessingLock = new Object();

    private final int mFrameRate;

    private final OnFrameUpdateListener mOnFrameUpdateListener;

    private int mFrame;
    private boolean mInvalidated;
    private boolean mPlay = true;

    private Simulator mSimulator;

    private EnergyStatsDialog mEnergyStatsDialog;

    private final Timer mTimer = new Timer(0, new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {

            if ((mFrame % mFrameRate) == 0) {
                debug("onUpdateFrame: New cycle at frame = " + mFrame);
            }

            mOnFrameUpdateListener.onFrameChanged(FrameRateThread.this, mFrame);

            mFrame++;
        }
    });

    public FrameRateThread(Simulator simulator,
                           OnFrameUpdateListener onFrameUpdateListener,
                           SimulationDialog mainWindow) {
        mSimulator = simulator;
        mFrameRate = simulator.getSimulation().getFrameRate();

        mOnFrameUpdateListener = onFrameUpdateListener;

        int millisecondsByFrame = 1000 / mFrameRate;

        mTimer.setRepeats(true);
        mTimer.setDelay(millisecondsByFrame);

        debug("FrameRateThread: Time of a frame: " + millisecondsByFrame);

        mEnergyStatsDialog = new EnergyStatsDialog(1000, mainWindow);
    }

    public void stop() {
        mTimer.stop();
    }

    public int getFrame() {
        return mFrame;
    }

//    public void setInnerStates(int innerStates) {
//        mInnerStates = innerStates;
//    }

    public void restart() {
        synchronized (mProcessingLock) {
            mSimulator.restart();
        }
    }

    public Object getProcessingLock() {
        return mProcessingLock;
    }

    public EnergyStatsDialog getEnergyStatsDialog() {
        return mEnergyStatsDialog;
    }

    protected void onInterrupt() {
        debug("Thread interrupted. Exiting loop and finishing thread.");
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

            if (play) mOnFrameUpdateListener.onPlay();
            else mOnFrameUpdateListener.onPause();

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

    @Deprecated
    public int getSimulationState() {
        return mFrame;
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
         * Called when the thread is invalidated after {@link #onFrameChanged(FrameRateThread, int)}
         * is called.
         */
        void onInvalidate(FrameRateThread thread);
    }
}
