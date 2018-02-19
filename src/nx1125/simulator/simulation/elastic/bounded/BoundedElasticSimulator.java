package nx1125.simulator.simulation.elastic.bounded;

import nx1125.simulator.simulation.Planet;
import nx1125.simulator.simulation.PlanetState;
import nx1125.simulator.simulation.field.FieldSimulator;

public class BoundedElasticSimulator extends FieldSimulator {

    public BoundedElasticSimulator(BoundedSimulation simulation) {
        super(simulation);
    }

    @Override
    protected void computeForces(Planet[] planets, PlanetState[] actualStates) {
        // The variables a0, a1 and h are functions of both planets that interact with each other.
        // The force between two particles are proportional to the distance.
        // The problem so far is to decide to use spin for a particle that behaves as an atom.
        // A propose is that the electrons originally were in pairs, but recent thoughts
        // shows that each electron has an individual energy...
        //
    }

    @Override
    public double getPotentialEnergy() {
        return 0;
    }
}
