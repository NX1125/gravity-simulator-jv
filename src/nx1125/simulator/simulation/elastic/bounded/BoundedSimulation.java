package nx1125.simulator.simulation.elastic.bounded;

import nx1125.simulator.simulation.Simulator;
import nx1125.simulator.simulation.field.FieldSimulation;

public class BoundedSimulation extends FieldSimulation implements BoundsSimulation {

    private double mCenterX;
    private double mCenterY;

    private double mBoundsRadius;

    public BoundedSimulation(int frameRate) {
        super(frameRate);
    }

    @Override
    public Simulator createSimulator() {
        return new BoundedElasticSimulator(this);
    }

    @Override
    public double getCenterX() {
        return mCenterX;
    }

    public void setCenterX(double centerX) {
        mCenterX = centerX;
    }

    @Override
    public double getCenterY() {
        return mCenterY;
    }

    public void setCenterY(double centerY) {
        mCenterY = centerY;
    }

    @Override
    public double getBoundsRadius() {
        return mBoundsRadius;
    }

    public void setBoundsRadius(double boundsRadius) {
        mBoundsRadius = boundsRadius;
    }
}
