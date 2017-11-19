package nx1125.simulator.elastic;

import nx1125.simulator.simulation.Simulation;
import nx1125.simulator.simulation.Simulator;

public class ElasticSimulation extends Simulation {

    private double mFriction = 1;
    private double mElasticConstant = 1;
    private double mRestingRadius = 1;

    public ElasticSimulation(int frameRate) {
        super(frameRate);
    }

    public ElasticSimulation() {
    }

    public double getFrictionConstant() {
        return mFriction;
    }

    public void setFriction(double friction) {
        mFriction = friction;
    }

    public double getElasticConstant() {
        return mElasticConstant;
    }

    public void setElasticConstant(double elasticConstant) {
        mElasticConstant = elasticConstant;
    }

    public double getRestingRadius() {
        return mRestingRadius;
    }

    public void setRestingRadius(double radius) {
        mRestingRadius = radius;
    }

    @Override
    public Simulator createSimulator() {
        return new ElasticSimulator(this);
    }
}
