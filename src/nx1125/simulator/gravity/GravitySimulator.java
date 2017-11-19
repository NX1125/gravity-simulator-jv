package nx1125.simulator.gravity;

import nx1125.simulator.simulation.DefaultSimulator;
import nx1125.simulator.simulation.Planet;
import nx1125.simulator.simulation.PlanetState;

/**
 * Created by guilh on 01/10/2017.
 */

public class GravitySimulator extends DefaultSimulator {

    private static final String TAG = "SimulatorThread";

    private static final boolean DEBUG = true;

    private final GravitySimulation mSimulation;

    private double[] mCachedElectricFieldMultipliersFromPlanets;
    private double[] mCachedMagneticFieldMultipliersFromPlanets;

    private AccelerationCache[] mAccelerationCaches;

    public GravitySimulator(GravitySimulation simulation) {
        super(simulation);

        mSimulation = simulation;
    }

    @Override
    public boolean isCollisionEnabled() {
        return mSimulation.isCollisionEnabled();
    }

    @Override
    protected void onCreateCache() {
        super.onCreateCache();

        int planetCount = getPlanetCount();

        mAccelerationCaches = new AccelerationCache[planetCount];

        mCachedMagneticFieldMultipliersFromPlanets = new double[planetCount];
        mCachedElectricFieldMultipliersFromPlanets = new double[planetCount];

        mAccelerationCaches = new AccelerationCache[planetCount];

        for (int i = 0; i < planetCount; i++) {
            mAccelerationCaches[i] = new AccelerationCache();
        }
    }

    @Override
    protected void onCreateInitialStates() {
        super.onCreateInitialStates();

        int count = getPlanetCount();

        double pi4 = Math.PI * 4.0;

        double electricMultiplier = 1.0 / (mSimulation.getPermittivityConstant() * pi4);
        double magneticMultiplier = mSimulation.getPermeabilityConstant() / pi4;

        for (int i = 0; i < count; i++) {
            Planet p = getPlanet(i);

            double ratio = p.getCharge() / p.getMass();

            mCachedElectricFieldMultipliersFromPlanets[i] = ratio * electricMultiplier;
            mCachedMagneticFieldMultipliersFromPlanets[i] = ratio * magneticMultiplier;
        }
    }

    public void computeAccelerations(Planet[] planets, PlanetState[] actualStates) {
        double g = mSimulation.getGravityConstant();

        for (int i = 0; i < planets.length; i++) {
            Planet p0 = planets[i];
            PlanetState s0 = actualStates[i];
            AccelerationCache cache0 = mAccelerationCaches[i];

            for (int j = i + 1; j < planets.length; j++) {
                Planet p1 = planets[j];
                PlanetState s1 = actualStates[j];

                AccelerationCache cache1 = mAccelerationCaches[j];

                double dx = s1.x - s0.x;
                double dy = s1.y - s0.y;

                double ir3 = Math.pow(dx * dx + dy * dy, -1.5);

                // compute p0 acceleration part
                cache0.add(dx, dy, ir3, p1, s1);

                // compute p1 acceleration part
                cache1.add(-dx, -dy, ir3, p0, s0);
            }

            // gravity + electric + magnetic
            s0.ax = cache0.mGravitySumX * g
                    + mCachedElectricFieldMultipliersFromPlanets[i] * cache0.mElectricSumX
                    + mCachedMagneticFieldMultipliersFromPlanets[i] * s0.vy * cache0.mMagneticSum;
            s0.ay = cache0.mGravitySumY * g
                    + mCachedElectricFieldMultipliersFromPlanets[i] * cache0.mElectricSumY
                    - mCachedMagneticFieldMultipliersFromPlanets[i] * s0.vx * cache0.mMagneticSum;

            cache0.clear();
        }
    }

    private static void debug(String msg) {
        System.out.println(TAG + "/D " + msg);
    }

    private static void info(String msg) {
        System.out.println(TAG + "/I " + msg);
    }

    private static class AccelerationCache {

        private double mGravitySumX;
        private double mGravitySumY;

        private double mElectricSumX;
        private double mElectricSumY;

        private double mMagneticSum;

        AccelerationCache() {
        }

        void clear() {
            mGravitySumX = mGravitySumY = mElectricSumX = mElectricSumY = mMagneticSum = 0;
        }

        void add(double dx, double dy, double ir3, Planet p,
                 PlanetState state) {
            double mass = ir3 * p.getMass();
            double charge = ir3 * p.getCharge();

            mGravitySumX += mass * dx;
            mGravitySumY += mass * dy;

            mElectricSumX += charge * dx;
            mElectricSumY += charge * dy;

            mMagneticSum += charge * (state.vx * dy - dx * state.vy);
        }
    }
}
