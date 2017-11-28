package nx1125.simulator;

import nx1125.simulator.simulation.PlanetState;
import nx1125.simulator.simulation.Simulation;
import nx1125.simulator.simulation.Simulator;

import java.io.*;

public class SimulationRecorder {

    private final Simulation mSimulation;

    private final Simulator mSimulator;

    public SimulationRecorder(Simulation simulation) {
        mSimulation = simulation;
        mSimulator = simulation.createSimulator();
    }

    public void simulate(OutputStream output, int statesCount) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(output);
        oos.writeInt(statesCount);
        for (int i = 0; i < statesCount; i++) {
            PlanetState[] states = mSimulator.computeStates();
            oos.writeUnshared(states);
        }
    }

    public static PlanetState[][] readPlanetStates(InputStream stream) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(stream);
        PlanetState[][] states = new PlanetState[ois.readInt()][];
        for (int i = 0; i < states.length; i++) {
            states[i] = (PlanetState[]) ois.readUnshared();
        }
        return states;
    }
}
