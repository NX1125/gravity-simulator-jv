package nx1125.simulator.elastic;

import nx1125.simulator.simulation.DefaultSimulator;
import nx1125.simulator.simulation.Planet;
import nx1125.simulator.simulation.PlanetState;

import java.util.LinkedList;
import java.util.List;

public class ElasticSimulator extends DefaultSimulator {

    private final ElasticSimulation mElasticSimulation;

    // need to be index because of the States the are created in time
    private final List<Integer> mLockedPlanets = new LinkedList<>();

    public ElasticSimulator(ElasticSimulation simulation) {
        super(simulation);

        mElasticSimulation = simulation;
    }

    public synchronized boolean addLockedPlanet(int index) {
        return mLockedPlanets.add(index);
    }

    public synchronized boolean removeLockedPlanet(int index) {
        return mLockedPlanets.remove((Integer) index);
    }

    @Override
    protected void onCreateCache() {
        super.onCreateCache();
    }

    @Override
    public boolean isCollisionEnabled() {
        return false;
    }

    @Override
    public void computeAccelerations(Planet[] planets, PlanetState[] states) {
        for (PlanetState state : states) {
            state.clearAcceleration();
        }

        double friction = mElasticSimulation.getFrictionConstant();
        double k = mElasticSimulation.getElasticConstant();
        double r0 = mElasticSimulation.getRestingRadius();

        for (int i = 0; i < planets.length; i++) {
            PlanetState s0 = states[i];

            for (int j = i + 1; j < planets.length; j++) {
                PlanetState s1 = states[j];

                double dx = s1.x - s0.x;
                double dy = s1.y - s0.y;

                double r = Math.hypot(dx, dy);

                double a = 1 - r0 / r;

                s0.ax += a * dx;
                s0.ay += a * dy;

                s1.ax -= a * dx;
                s1.ay -= a * dy;
            }

            s0.ax *= k;
            s0.ay *= k;

            s0.ax -= s0.vx * friction;
            s0.ay -= s0.vy * friction;
        }

        for (int index : mLockedPlanets) {
            // after the compute, late update
            PlanetState s = states[index];

            s.vx = s.vy = 0;
            s.ax = s.ay = 0;
        }
    }
}
