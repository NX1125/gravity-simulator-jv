package nx1125.simulator.simulation.elastic;

import nx1125.simulator.simulation.DefaultSimulator;
import nx1125.simulator.simulation.Planet;
import nx1125.simulator.simulation.PlanetState;
import nx1125.simulator.simulation.SimulationUtils;

import java.util.LinkedList;
import java.util.List;

public class ElasticSimulator extends DefaultSimulator implements AbstractElasticSimulator {

    private final ElasticSimulation mElasticSimulation;

    // need to be index because of the States the are created in time
    private final List<Integer> mLockedPlanets = new LinkedList<>();

    private double mFrictionByVelocity;
    private double mElasticConstant;
    private double mRestingDistance;

    public ElasticSimulator(ElasticSimulation simulation) {
        super(simulation);

        mElasticSimulation = simulation;

        mFrictionByVelocity = mElasticSimulation.getFrictionConstant();
        mElasticConstant = mElasticSimulation.getElasticConstant();
        mRestingDistance = mElasticSimulation.getRestingRadius();
    }

    @Override
    public boolean addLockedPlanet(int index) {
        return !mLockedPlanets.contains(index) && mLockedPlanets.add(index);
    }

    @Override
    public void removeLockedPlanet(int index) {
        mLockedPlanets.remove((Integer) index);
    }

    @Override
    public double getPotentialEnergy() {
        return 0;
    }

    @Override
    public void computeAccelerations(Planet[] planets, PlanetState[] states) {
        SimulationUtils.clearAccelerations(states);

        for (int i = 0; i < planets.length; i++) {
            PlanetState s0 = states[i];

            for (int j = i + 1; j < planets.length; j++) {
                PlanetState s1 = states[j];

                computeAccelerationBetween(i, planets[i], s0, j, planets[j], s1);
            }

            commitAcceleration(s0);
            computeAccelerationFriction(s0);
        }

        lateComputeAcceleration(planets, states);
    }

    protected void computeAccelerationBetween(int index0, Planet p0, PlanetState s0, int index1, Planet p1, PlanetState s1) {
        double dx = s1.x - s0.x;
        double dy = s1.y - s0.y;

        double r = SimulationUtils.hypot(dx, dy);

        double f = getElasticForceWithoutConstant(r) / r;

        double a = f / p0.getMass();

        s0.ax += a * dx;
        s0.ay += a * dy;

        a = f / p1.getMass();

        s1.ax -= a * dx;
        s1.ay -= a * dy;
    }

    protected double getElasticForce(double radius) {
        return mElasticConstant * getElasticForceWithoutConstant(radius);
    }

    protected double getElasticForceWithoutConstant(double radius) {
        return radius - mRestingDistance;
    }

    protected void commitAcceleration(PlanetState state) {
        state.ax *= mElasticConstant;
        state.ay *= mElasticConstant;
    }

    protected void computeAccelerationFriction(PlanetState state) {
        state.ax -= state.vx * mFrictionByVelocity;
        state.ay -= state.vy * mFrictionByVelocity;
    }

    protected boolean lateComputeAcceleration(Planet[] planets, PlanetState[] states) {
        for (int index : mLockedPlanets) {
            // after the compute, late update
            PlanetState s = states[index];

            s.vx = s.vy = 0;
            s.ax = s.ay = 0;
        }

        return false;
    }

    @Override
    public double getFrictionByVelocity() {
        return mFrictionByVelocity;
    }

    @Override
    public void setFrictionByVelocity(double frictionByVelocity) {
        mFrictionByVelocity = frictionByVelocity;
    }

    @Override
    public double getElasticConstant() {
        return mElasticConstant;
    }

    @Override
    public void setElasticConstant(double elasticConstant) {
        mElasticConstant = elasticConstant;
    }

    @Override
    public double getRestingDistance() {
        return mRestingDistance;
    }

    @Override
    public void setRestingDistance(double restingDistance) {
        mRestingDistance = restingDistance;
    }
}
