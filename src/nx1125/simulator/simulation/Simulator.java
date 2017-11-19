package nx1125.simulator.simulation;

/**
 * Created by guilh on 01/10/2017.
 */

public interface Simulator {

    Simulation getSimulation();

    PlanetState[] getPlanetStates(int state);

    PlanetState[][] getPlanetStates();

    Planet[] getPlanets();

    boolean isStateDone(int state);

    int getLastAvailableState();

    int getStatesPerCycle();

    void start();

    void interrupt();

    void setOnSimulationEventListener(OnSimulatorEventListener l);

    interface OnSimulatorEventListener {

        /**
         * Called when the first state is done.
         */
        void onSimulationStarted(Simulator simulator);

        void onSimulationFinish(Simulator simulator);

        void onSimulationProgressUpdated(Simulator simulator, int step);
    }
}
