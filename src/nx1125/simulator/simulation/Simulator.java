package nx1125.simulator.simulation;

/**
 * Created by guilh on 01/10/2017.
 */

public interface Simulator {

    void onCreate();

    PlanetState[] getLastComputedStates();

    PlanetState[] computeStates();

    Simulation getSimulation();

    Planet[] getPlanets();

    default double getTotalEnergy() {
        return getKineticEnergy() + getPotentialEnergy();
    }

    default double getKineticEnergy() {
        double kinetics = 0;
        PlanetState[] states = getLastComputedStates();
        Planet[] planets = getPlanets();
        for (int i = 0; i < planets.length; i++) {
            PlanetState state = states[i];
            Planet planet = planets[i];

            kinetics += 0.5 * planet.getMass() * (state.vx * state.vx + state.vy * state.vy);
        }
        return kinetics;
    }

    double getPotentialEnergy();

    void restart();

    // sharkboy, lavagirl
}
