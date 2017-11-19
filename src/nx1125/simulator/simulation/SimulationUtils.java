package nx1125.simulator.simulation;

public class SimulationUtils {

    public static void advance(int planetCount, double time, PlanetState[] states) {
        double halfTimeSqr = time * time * 0.5;

        for (int i = 0; i < planetCount; i++) {
            PlanetState state = states[i];

            // advance position
            state.x += state.vx * time + state.ax * halfTimeSqr;
            state.y += state.vy * time + state.ay * halfTimeSqr;

            // advance velocity
            state.vx += state.ax * time;
            state.vy += state.ay * time;
        }
    }
}
