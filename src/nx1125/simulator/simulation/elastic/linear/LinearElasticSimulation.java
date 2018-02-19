package nx1125.simulator.simulation.elastic.linear;

import nx1125.simulator.simulation.Simulator;
import nx1125.simulator.simulation.elastic.ElasticSimulation;

public class LinearElasticSimulation extends ElasticSimulation {

    private double mTension = 1;

    public LinearElasticSimulation(int frameRate) {
        super(frameRate);
    }

    public LinearElasticSimulation() {
    }

    @Override
    public Simulator createSimulator() {
        return new LinearElasticSimulator(this);
    }

    public double getTension() {
        return mTension;
    }

    public void setTension(double tension) {
        mTension = tension;
    }
}
