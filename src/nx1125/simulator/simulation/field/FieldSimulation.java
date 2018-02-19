package nx1125.simulator.simulation.field;

import nx1125.simulator.simulation.Simulation;
import nx1125.simulator.simulation.elastic.bounded.BoundsSimulation;

public abstract class FieldSimulation extends Simulation implements BoundsSimulation {

    public FieldSimulation(int frameRate) {
        super(frameRate);
    }
}
