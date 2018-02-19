package nx1125.simulator.simulation.elastic;

import nx1125.simulator.simulation.Simulation;
import nx1125.simulator.simulation.Simulator;

public class ElasticSimulation extends Simulation {

    private double mFriction = 0.001;
    private double mElasticConstant = 1.364;
    private double mRestingRadius = 0.194;

    public ElasticSimulation(int frameRate) {
        super(frameRate);
    }

    public ElasticSimulation() {
    }

    public double getFrictionConstant() {
        return mFriction;
    }

    public void setFrictionByVelocity(double frictionByVelocity) {
        mFriction = frictionByVelocity;
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
