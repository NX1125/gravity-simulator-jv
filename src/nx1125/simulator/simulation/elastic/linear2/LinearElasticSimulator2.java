package nx1125.simulator.simulation.elastic.linear2;

import nx1125.simulator.FrameRateThread;
import nx1125.simulator.simulation.*;
import nx1125.simulator.simulation.elastic.AbstractLinearElasticSimulator;

import java.util.LinkedList;
import java.util.List;

public class LinearElasticSimulator2 implements Simulator, AbstractLinearElasticSimulator {

    protected final int mPlanetCount;
    protected final double mTimeInterval;
    private final Object mLockObject = new Object();
    private final LinearElasticSimulation2 mElasticSimulation;
    // need to be index because of the States the are created in time
    private final List<Integer> mLockedPlanets = new LinkedList<>();
    private final double mDoubleTimeIntervalSqr;
    protected double mFrictionByVelocity;
    protected double mElasticConstant;
    protected double mRestingDistance;
    protected PlanetState[] mLastStates;
    protected PlanetState[] mActualStates;
    protected PlanetState[] mNextStates;
    protected Planet[] mPlanets;
    /**
     * Cache array to be used when computing the distance.
     */
    private double[] mCachedDistances;
    private double[] mCachedNumeratorConstants;
    private double[] mCachedDenominatorConstants;

    public LinearElasticSimulator2(LinearElasticSimulation2 simulation) {
        mElasticSimulation = simulation;

        mPlanets = simulation.getPlanets();
        mPlanetCount = mPlanets.length;

        mFrictionByVelocity = simulation.getFrictionByVelocity();
        mElasticConstant = simulation.getElasticConstant();
        mRestingDistance = simulation.getRestingRadius();

        mTimeInterval = simulation.getTimeInterval() / (simulation.getFrameRate() * FrameRateThread.INNER_STATES_COUNT);
        mDoubleTimeIntervalSqr = 2.0 * mTimeInterval * mTimeInterval;

        System.out.println("Time interval : " + mTimeInterval);
    }

    @Override
    public void onCreate() {
        mCachedDistances = new double[mPlanetCount - 1];

        mLastStates = new PlanetState[mPlanetCount];
        mActualStates = new PlanetState[mPlanetCount];
        mNextStates = new PlanetState[mPlanetCount];

        createInitialStates();
        createSecondStates();
        copyActualToNext();

        mCachedNumeratorConstants = new double[mPlanetCount];
        mCachedDenominatorConstants = new double[mPlanetCount];

        invalidateCaches();
    }

    private void invalidateCaches() {
        if (mCachedNumeratorConstants != null) {
            for (int i = 0; i < mPlanetCount; i++) {
                double c = mFrictionByVelocity * mTimeInterval / mPlanets[i].getMass();

                mCachedNumeratorConstants[i] = c - 2;
                mCachedDenominatorConstants[i] = c + 2;
            }
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

    @Override
    public PlanetState[] getLastComputedStates() {
        return mActualStates;
    }

    @Override
    public PlanetState[] computeStates() {
        synchronized (mLockObject) {
            SimulationUtils.clearAccelerations(mActualStates);

            computeForces();

            for (int i = 0; i < mPlanetCount; i++) {
                advance(i);
            }

            for (int i : mLockedPlanets) {
                PlanetState actual = mActualStates[i];

                mNextStates[i].setState(actual);
//            mLastStates[i].setState(actual);
            }

            // the last state is not needed anymore and so dump the array to be used in the next state
            PlanetState[] aux = mLastStates;

            mLastStates = mActualStates;
            mActualStates = mNextStates;
            mNextStates = aux;

            return mActualStates;
        }
    }

    protected void advance(int index) {
        PlanetState last = mLastStates[index];
        PlanetState actual = mActualStates[index];
        PlanetState next = mNextStates[index];

        double mass = mPlanets[index].getMass();

        actual.ax /= mass;
        actual.ay /= mass;

        double n = mCachedNumeratorConstants[index];
        double d = mCachedDenominatorConstants[index];

        next.x = (mDoubleTimeIntervalSqr * actual.ax + 4.0 * actual.x + last.x * n) / d;
        next.y = (mDoubleTimeIntervalSqr * actual.ay + 4.0 * actual.y + last.y * n) / d;

        next.vx = (next.x - last.x) / mTimeInterval;
        next.vy = (next.y - last.y) / mTimeInterval;
    }

    protected void computeForces() {
        int count = mPlanetCount - 1;

        for (int i = 0; i < count; i++) {
            PlanetState pi = mActualStates[i];
            PlanetState pj = mActualStates[i + 1];

            double dx = pj.x - pi.x;
            double dy = pj.y - pi.y;

            double r = SimulationUtils.hypot(dx, dy);

            double force = (1.0 - mRestingDistance / r) * mElasticConstant;

            mCachedDistances[i] = r;

            pi.ax += force * dx;
            pi.ay += force * dy;

            pj.ax -= force * dx;
            pj.ay -= force * dy;
        }
    }

    protected void getForceBetween(int i0, int i1, double radius) {
        PlanetState pi = mActualStates[i0];
        PlanetState pj = mActualStates[i1];

        double dx = pj.x - pi.x;
        double dy = pj.y - pi.y;

        double r = SimulationUtils.hypot(dx, dy);

        double force = (1.0 - mRestingDistance / r) * mElasticConstant;

        mCachedDistances[i0] = r;

        pi.ax += force * dx;
        pi.ay += force * dy;

        pj.ax -= force * dx;
        pj.ay -= force * dy;
    }

    @Override
    public Simulation getSimulation() {
        return mElasticSimulation;
    }

    @Override
    public Planet[] getPlanets() {
        return mPlanets;
    }

    @Override
    public double getPotentialEnergy() {
        double potentials = 0;

        for (int i = 1; i < mPlanetCount; i++) {
            double r = mCachedDistances[i - 1] - mRestingDistance;

            potentials += 0.5 * mElasticConstant * r * r;
        }

        // System.out.println("Potential: " + potentials);

        return potentials;
    }

    @Override
    public double getFrictionByVelocity() {
        return mFrictionByVelocity;
    }

    @Override
    public void setFrictionByVelocity(double frictionByVelocity) {
        synchronized (mLockedPlanets) {
            mFrictionByVelocity = frictionByVelocity;
            invalidateCaches();
        }
    }

    @Override
    public double getElasticConstant() {
        return mElasticConstant;
    }

    @Override
    public void setElasticConstant(double elasticConstant) {
        synchronized (mLockedPlanets) {
            mElasticConstant = elasticConstant;
            invalidateCaches();
        }
    }

    @Override
    public double getRestingDistance() {
        return mRestingDistance;
    }

    @Override
    public void setRestingDistance(double restingDistance) {
        synchronized (mLockedPlanets) {
            mRestingDistance = restingDistance;
            invalidateCaches();
        }
    }

    @Override
    public int getPlanetCount() {
        return mPlanetCount;
    }

    @Override
    public void restart() {
        for (int i = 0; i < mPlanetCount; i++) {
            Planet p = mPlanets[i];

            mLastStates[i].setState(p);
            mNextStates[i].setState(p);
            mActualStates[i].setState(p);
        }
    }

    @Override
    public boolean addLockedPlanet(int index) {
        synchronized (mLockObject) {
            if (mNextStates != null) {
                mNextStates[index].setState(mActualStates[index]);
                mLastStates[index].setState(mActualStates[index]);
            }

            return !mLockedPlanets.contains(index) && mLockedPlanets.add(index);
        }
    }

    @Override
    public void setPlanetLocation(int index, double x, double y) {
        synchronized (mLockObject) {
            mActualStates[index].setLocation(x, y);
            mNextStates[index].setLocation(x, y);
            mLastStates[index].setLocation(x, y);
        }
    }

    @Override
    public void removeLockedPlanet(int index) {
        mLockedPlanets.remove((Integer) index);
    }
}
