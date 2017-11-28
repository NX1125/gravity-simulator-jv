package nx1125.simulator;

import nx1125.simulator.simulation.PlanetState;
import nx1125.simulator.simulation.Simulator;
import nx1125.simulator.windows.EnergyStatsDialog;

import java.awt.*;

/**
 * Created by guilh on 01/10/2017.
 */

public class FrameRateThread extends Thread {

    public static final int INNER_STATES_COUNT = 100;

    private static final String TAG = "FrameRateThread";

    private static final boolean DEBUG = false;

    private final int mFrameRate;

    private final int mMillisecondsByFrame;
    private final OnFrameUpdateListener mOnFrameUpdateListener;
    private int mFrame;
    private boolean mInvalidated;
    private boolean mPlay = true;

    private long mActualFrameTimeLength;

    private Simulator mSimulator;

    private volatile boolean mProcessingSimulation;

    private EnergyStatsDialog mEnergyStatsDialog;

    public FrameRateThread(Simulator simulator,
                           OnFrameUpdateListener onFrameUpdateListener, Window mainWindow) {
        super("FrameRateThread");

        mSimulator = simulator;
        mFrameRate = simulator.getSimulation().getFrameRate();

        mOnFrameUpdateListener = onFrameUpdateListener;

        mMillisecondsByFrame = 1000 / mFrameRate;

        debug("FrameRateThread: Time of a frame: " + mMillisecondsByFrame);

        mEnergyStatsDialog = new EnergyStatsDialog(1000, mainWindow);
    }

    public int getFrame() {
        return mFrame;
    }

//    public void setInnerStates(int innerStates) {
//        mInnerStates = innerStates;
//    }

    @Override
    public void run() {
        debug("Starting frame rate thread");

        mEnergyStatsDialog.setVisible(true);

        long lastFrameTime = System.currentTimeMillis();

        mFrame = 0;

        debug("Calling onCreate of Simulator");
        mSimulator.onCreate();

        double lastEnergy = 0;
        boolean increased = false;

        debug("Starting loop of frame rate");
        while (!interrupted()) {
            if (!mPlay) {
                debug("run: Entering pause loop and listen to invalidate events only. Actual frame = " + mFrame);
                mOnFrameUpdateListener.onPause();
                while (!mPlay) {
                    if (interrupted()) {
                        onInterrupt();
                        return;
                    }
                    revalidate();
                }
                debug("run: Exiting pause loop and starting play loop. Frame = " + mFrame);
                mOnFrameUpdateListener.onPlay();
            }

            if ((mFrame % mFrameRate) == 0) {
                debug("onUpdateFrame: New cycle at frame = " + mFrame);
                System.out.println("Energy of the system: " + mSimulator.getTotalEnergy());
            }

            mProcessingSimulation = true;
            PlanetState[] states = null;
            for (int i = 0; i < INNER_STATES_COUNT; i++) {
                states = mSimulator.computeStates();
            }
            mProcessingSimulation = false;

            double kinetic = mSimulator.getKineticEnergy();
            double potential = mSimulator.getPotentialEnergy();

            double energy = kinetic + potential;
            if (lastEnergy < energy && !increased) {
                increased = true;
                debug("Energy is now increasing (" + lastEnergy + " -> " + energy + ')');
            } else if (lastEnergy > energy && increased) {
                increased = false;
                debug("Energy is now decreasing (" + lastEnergy + " -> " + energy + ')');
            }
            lastEnergy = energy;
            mEnergyStatsDialog.addValue(kinetic, potential);

            mOnFrameUpdateListener.onFrameChanged(this, mFrame, states);

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

        onInterrupt();
    }

    protected void onInterrupt() {
        debug("Thread interrupted. Exiting loop and finishing thread.");
        mEnergyStatsDialog.setVisible(false);
    }

    public boolean isProcessingSimulation() {
        return mProcessingSimulation;
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
        void onFrameChanged(FrameRateThread thread, int frameIndex, PlanetState[] states);

        /**
         * Called when the thread is invalidated after {@link #onFrameChanged(FrameRateThread, int, PlanetState[])}
         * is called.
         */
        void onInvalidate(FrameRateThread thread);
    }
}
