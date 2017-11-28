package nx1125.simulator.elastic;

import nx1125.simulator.simulation.Planet;
import nx1125.simulator.simulation.PlanetState;

import java.util.Arrays;

public class LinearElasticSimulator extends ElasticSimulator {

    private double[] mRingNetForces;
    private double[] mRingDirection;
    private double[] mRingRelativePosition;

    public LinearElasticSimulator(LinearElasticSimulation simulation) {
        super(simulation);
    }

    @Override
    protected void onCreateCache() {
        super.onCreateCache();

        // each planet has 2 extreme points for the ring and so it is need four coordinates
        mRingNetForces = new double[getPlanetCount() * 4];
        mRingDirection = new double[getPlanetCount() * 2];
        mRingRelativePosition = new double[getPlanetCount() * 2];
    }

    @Override
    protected PlanetState[] onCreateInitialStates() {
        PlanetState[] states = super.onCreateInitialStates();

        for (int i = 1, count = states.length - 1; i < count; i++) {
            PlanetState s0 = states[i - 1];
            PlanetState s = states[i];
            PlanetState s1 = states[i + 1];

            // the ring has the same direction as the vector coming from the planet center with an equals angle between
            // both of the planets connections

            s.ringAngle = (atan2(s.x - s1.x, s.y - s1.y) + atan2(s.x - s0.x, s.y - s0.y)) * 0.5;
        }

        states[0].ringAngle = states[1].ringAngle;
        states[states.length - 1].ringAngle = states[states.length - 2].ringAngle;

        return states;
    }

    private void updateRingTrigonometricValues(PlanetState[] states) {
        int count = states.length - 1;

        for (int i = 1, coordinateIndex = 0; i < count; i++) {
            PlanetState state = states[i];

            PlanetState s0 = states[i - 1];
            PlanetState s1 = states[i + 1];

            state.ringAngle = (atan2(s1.x - state.x, s1.y - state.y)
                    + atan2(s0.x - state.x, s0.y - state.y)) * 0.5;

            double r = getPlanet(i).getRadius();

            double cos = Math.cos(state.ringAngle);
            double sin = Math.sin(state.ringAngle);

            mRingDirection[coordinateIndex] = cos;
            mRingRelativePosition[coordinateIndex++] = cos * r;

            mRingDirection[coordinateIndex] = sin;
            mRingRelativePosition[coordinateIndex++] = sin * r;
        }
    }

    @Override
    public void computeAccelerations(Planet[] planets, PlanetState[] states) {
        clearAccelerations(states);

        // updateRingTrigonometricValues(states);

        computeStringAccelerations(planets, states);

        // computeRingNetForces(planets, states);
        // applyRingForceToCenter(states);

        // computeRingAxialAcceleration(states, planets);

        for (PlanetState state : states) computeAccelerationFriction(state);

        lateComputeAcceleration(planets, states);
    }

    protected void computeStringAccelerations(Planet[] planets, PlanetState[] states) {
        int count = planets.length - 1;
        for (int i = 0; i < count; i++) {
            computeAccelerationBetween(i, planets[i], states[i], i + 1, planets[i + 1], states[i + 1]);
            commitAcceleration(states[i]);
        }

        commitAcceleration(states[states.length - 1]);
    }

    protected void applyRingForceToCenter(PlanetState[] states) {
        for (int i = 0, netForceCoordinateIndex = 0; i < states.length; i++) {
            states[i].ax += mRingNetForces[netForceCoordinateIndex++];
            states[i].ay += mRingNetForces[netForceCoordinateIndex++];
            states[i].ax += mRingNetForces[netForceCoordinateIndex++];
            states[i].ay += mRingNetForces[netForceCoordinateIndex++];
        }
    }

    protected void computeRingNetForces(Planet[] planets, PlanetState[] states) {
        int count = planets.length - 1;
        for (int i = 0, netForceCoordinateIndex = 0, relativeCoordinateIndex = 0; i < count; i++) {
            PlanetState state = states[i];
            PlanetState next = states[i + 1];

            double dx = next.x - state.x;
            double dy = next.y - state.y;

            double rx = mRingRelativePosition[relativeCoordinateIndex] - mRingRelativePosition[relativeCoordinateIndex + 2];
            double ry = mRingRelativePosition[relativeCoordinateIndex + 1] - mRingRelativePosition[relativeCoordinateIndex + 3];

            computeRingSideAcceleration(dx + rx, dy + ry, netForceCoordinateIndex);
            computeRingSideAcceleration(dx - rx, dy - ry, netForceCoordinateIndex + 2);

            netForceCoordinateIndex += 4;
            relativeCoordinateIndex += 2;
        }
    }

    private void computeRingSideAcceleration(double dx, double dy, int netForceIndex) {
        double radius = Math.hypot(dx, dy);

        double force = getElasticForceWithoutConstant(radius) / radius;

        mRingNetForces[netForceIndex] += force * dx;
        mRingNetForces[netForceIndex + 1] += force * dy;

        mRingNetForces[netForceIndex + 4] -= force * dx;
        mRingNetForces[netForceIndex + 5] -= force * dy;
    }

    protected void computeRingAxialAcceleration(PlanetState[] states, Planet[] planets) {
        for (int i = 0, netForceIndex = 0, ringDirectionIndex = 0; i < states.length; i++) {
            Planet p = planets[i];

            double ax = (mRingNetForces[netForceIndex + 2] - mRingNetForces[netForceIndex]) / p.getMass();
            double ay = (mRingNetForces[netForceIndex + 3] - mRingNetForces[netForceIndex + 1]) / p.getMass();

            // multiply by the orthogonal of the radius direction to get the projection of the difference force
            // into the radius vector
            double dx = mRingDirection[ringDirectionIndex++];
            double dy = mRingDirection[ringDirectionIndex++];

            // the orthogonal is (-dy, dx)
            states[i].ringAngleAcceleration = (dx * ay - dy * ax) / p.getRadius();

            netForceIndex += 4;
        }
    }

    public double getRingTrigonometryPart(int index) {
        return mRingRelativePosition[index];
    }

    @Override
    public double getPotentialEnergy() {
        double potentials = 0;

        double k = getElasticConstant();
        double resting = getRestingDistance();

        int count = getPlanetCount();
        PlanetState[] states = getLastComputedStates();
        for (int i = 1; i < count; i++) {
            PlanetState s0 = states[i - 1];
            PlanetState s1 = states[i];

            double r = Math.hypot(s1.x - s0.x, s1.y - s0.y) - resting;

            potentials += 0.5 * k * r * r;
        }

        // System.out.println("Potential: " + potentials);

        return potentials;
    }

    @Override
    protected void clearAccelerations(PlanetState[] states) {
        super.clearAccelerations(states);

        Arrays.fill(mRingNetForces, 0);
    }

    @Override
    protected void advance(double time, PlanetState[] states) {

    }

    private void computeTension(PlanetState s0, PlanetState s1) {
//        double dx = s1.x - s0.x;
//        double dy = s1.y - s0.y;

//        double r = Math.hypot(dx, dy);

//        s0.ax += (dx * mNaturalTension) / r;
//        s0.ay += (dy * mNaturalTension) / r;
    }

    private static double atan2(double x, double y) {
        double atan2 = Math.atan2(y, x);
        return atan2 < 0 ? 2 * Math.PI + atan2 : atan2;
    }
}
