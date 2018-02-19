package nx1125.simulator.simulation.elastic.linear2;

import nx1125.simulator.simulation.Simulator;
import nx1125.simulator.simulation.elastic.ElasticSimulation;

public class LinearElasticSimulation2 extends ElasticSimulation {

    private double mFrictionByVelocity = 100;
    private double mElasticConstant = 1;
    private double mRestingRadius = 0.194;

    public LinearElasticSimulation2(int frameRate) {
        super(frameRate);
    }

    @Override
    public Simulator createSimulator() {
        return new LinearElasticSimulator2(this);
    }

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
    public double getRestingRadius() {
        return mRestingRadius;
    }

    @Override
    public void setRestingRadius(double restingRadius) {
        mRestingRadius = restingRadius;
    }
}
