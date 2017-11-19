package nx1125.simulator.simulation;

/**
 * Created by guilh on 01/10/2017.
 */

public class SimulationResults implements Simulator {

    private final Simulation mSimulation;

    private final PlanetState[][] mPlanetStates;

    private final CollisionResults[] mSimulationResults;

    private final Planet[] mPlanets;

    public SimulationResults(Simulation simulation, PlanetState[][] states, CollisionResults[] collisions, Planet[] planets) {
        mSimulation = simulation;
        mPlanetStates = states;
        mSimulationResults = collisions;
        mPlanets = planets;
    }

    public CollisionResults[] getSimulationResults() {
        return mSimulationResults;
    }

    public PlanetState[][] getPlanetStates() {
        return mPlanetStates;
    }

    public Simulation getSimulation() {
        return mSimulation;
    }

    @Override
    public void setOnSimulationEventListener(OnSimulatorEventListener l) {
    }

    @Override
    public void start() {
    }

    @Override
    public void interrupt() {
    }

    public PlanetState[] getPlanetStates(int index) {
        return mPlanetStates[index];
    }

    @Override
    public boolean isStateDone(int state) {
        return state < mPlanetStates.length;
    }

    @Override
    public int getStatesPerCycle() {
        return mSimulation.getStatesPerCycle();
    }

    @Override
    public int getLastAvailableState() {
        return mPlanetStates.length - 1;
    }

    @Override
    public Planet[] getPlanets() {
        return mPlanets;
    }
}
