package nx1125.simulator.simulation.field;

import nx1125.simulator.FrameRateThread;
import nx1125.simulator.simulation.*;

public abstract class FieldSimulator implements Simulator {

    protected final double[] mActualPositions;
    protected final double[] mActualForces;
    protected final double[] mActualAccelerations;
    protected final int mPlanetCount;
    protected final double mTimeInterval;
    private final Object mLockObject = new Object();
    private final FieldSimulation mElasticSimulation;
    private final double mCenterX;
    private final double mCenterY;
    private final double mBoundsRadius;
    protected PlanetState[] mLastStates;
    protected PlanetState[] mActualStates;
    protected PlanetState[] mNextStates;
    protected Planet[] mPlanets;
    /**
     * Cache array to be used when computing the distance. All distances
     * between the planets are saved here.
     */
    private double[][] mCachedDistances;
    private double mDoubleTimeInterval;
    private double mTimeIntervalSqr;

    public FieldSimulator(FieldSimulation simulation) {
        mElasticSimulation = simulation;

        mPlanets = simulation.getPlanets();
        mPlanetCount = mPlanets.length;

        mTimeInterval = simulation.getTimeInterval() / (simulation.getFrameRate() * FrameRateThread.INNER_STATES_COUNT);

        mActualPositions = new double[mPlanetCount << 1];
        mActualForces = new double[mActualPositions.length];
        mActualAccelerations = new double[mActualPositions.length];

        System.out.println("Time interval : " + mTimeInterval);

        mCenterX = simulation.getCenterX();
        mCenterY = simulation.getCenterY();

        mBoundsRadius = simulation.getBoundsRadius();
    }

    @Override
    public void onCreate() {
        mCachedDistances = new double[mPlanetCount][];

        mLastStates = new PlanetState[mPlanetCount];
        mActualStates = new PlanetState[mPlanetCount];
        mNextStates = new PlanetState[mPlanetCount];

        createInitialStates();
        createSecondStates();
        copyActualToNext();

        int count = mPlanetCount - 1;
        for (int i = 0; i < count; i++) {
            mCachedDistances[i] = new double[i + 1];
        }

        mTimeIntervalSqr = mTimeInterval * mTimeInterval;
        mDoubleTimeInterval = 2.0 * mTimeInterval;
    }

    protected double getDistanceBetween(int i, int j) {
        if (i == j) return 0.0;
        if (i < j) {
            return mCachedDistances[j][i];
        } else {
            return mCachedDistances[i][j];
        }
    }

    private void createInitialStates() {
        for (int i = 0; i < mPlanetCount; i++) {
            mLastStates[i] = new PlanetState(mPlanets[i]);
        }
    }

    private void createSecondStates() {
        double halfTimeSqr = mTimeInterval * mTimeInterval * 0.5;

        for (int i = 0; i < mPlanetCount; i++) {
            PlanetState s = new PlanetState(mLastStates[i]);
            mActualStates[i] = s;

            s.x += mTimeInterval * s.vx + halfTimeSqr * s.ax;
            s.y += mTimeInterval * s.vy + halfTimeSqr * s.ay;

            s.vx += mTimeInterval * s.ax;
            s.vy += mTimeInterval * s.ay;
        }
    }

    private void copyActualToNext() {
        for (int i = 0; i < mPlanetCount; i++) {
            mNextStates[i] = new PlanetState(mActualStates[i]);
        }
    }

    private void clearForces() {
        for (int i = 0; i < mActualForces.length; i++) {
            mActualForces[i] = 0;
        }
    }

    private void copyToPositionCache() {
        for (int i = 0, j = 0; i < mPlanetCount; i++) {
            PlanetState s = mActualStates[i];

            mActualPositions[j++] = s.x;
            mActualPositions[j++] = s.y;
        }
    }

    @Override
    public PlanetState[] getLastComputedStates() {
        return mActualStates;
    }

    @Override
    public PlanetState[] computeStates() {
        synchronized (mLockObject) {
            SimulationUtils.clearAccelerations(mActualStates);

            clearForces();
            copyToPositionCache();

            computeDistances();

            computeForces(mPlanets, mActualStates);

            for (int i = 0; i < mPlanetCount; i++) {
                advance(i);
            }

            lateAdvance(mPlanets, mActualStates);

            swapStates();

            return mActualStates;
        }
    }

    private void swapStates() {
        // the last state is not needed anymore and so dump the array to be used in the next state
        PlanetState[] aux = mLastStates;

        mLastStates = mActualStates;
        mActualStates = mNextStates;
        mNextStates = aux;
    }

    private void computeDistances() {
        for (int i = 1; i < mCachedDistances.length; i++) {
            PlanetState s0 = mActualStates[i];
            double[] distances = mCachedDistances[i];
            for (int j = 0; j < i; j++) {
                distances[j] = s0.distance(mActualStates[j]);
            }
        }
    }

    protected void advance(int index) {
        PlanetState last = mLastStates[index];
        PlanetState actual = mActualStates[index];
        PlanetState next = mNextStates[index];

        double mass = mPlanets[index].getMass();

        actual.ax = actual.forceX / mass;
        actual.ay = actual.forceY / mass;

        next.x = mTimeIntervalSqr * actual.ax + 2.0 * actual.x + last.x;
        next.y = mTimeIntervalSqr * actual.ay + 2.0 * actual.y + last.y;

        next.vx = (next.x - last.x) / mDoubleTimeInterval;
        next.vy = (next.y - last.y) / mDoubleTimeInterval;
    }

    protected void lateAdvance(Planet[] planets, PlanetState[] states) {
    }

    protected abstract void computeForces(Planet[] planets, PlanetState[] actualStates);

    @Override
    public Simulation getSimulation() {
        return mElasticSimulation;
    }

    @Override
    public Planet[] getPlanets() {
        return mPlanets;
    }

    @Override
    public abstract double getPotentialEnergy();

    @Override
    public void restart() {
        for (int i = 0; i < mPlanetCount; i++) {
            Planet p = mPlanets[i];

            mLastStates[i].setState(p);
            mNextStates[i].setState(p);
            mActualStates[i].setState(p);
        }
    }
}
