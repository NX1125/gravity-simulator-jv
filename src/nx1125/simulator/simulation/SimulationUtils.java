package nx1125.simulator.simulation;

public class SimulationUtils {

    private static final int BIG_VALUES_COUNT = 16 * 1024;
    private static final int SMALL_VALUES_COUNT = 1024 * 1024 * 2;

    private static final double[] SQRT_BIG = new double[16384];
    private static final double[] SQRT_LOW = new double[SMALL_VALUES_COUNT];

    static {
        for (int i = 0; i < BIG_VALUES_COUNT; i++) {
            SQRT_BIG[i] = Math.sqrt(i);
        }
        float div = 2f / SMALL_VALUES_COUNT;
        for (int i = 0; i < SMALL_VALUES_COUNT; i++) {
            SQRT_LOW[i] = Math.sqrt(i * div);
        }
    }

    public static double hypot(double dx, double dy) {
        return sqrt(dx * dx + dy * dy);
    }

    public static double sqrt(double value) {
        return Math.sqrt(value);
//        int highSqr = (int) value + 1;
//        double lowSqr = value / highSqr;
//
//        return SQRT_BIG[highSqr] * SQRT_LOW[(int) (lowSqr*0.5 * SMALL_VALUES_COUNT)];
    }

    public static double atan2(double x, double y) {
        double atan2 = Math.atan2(y, x);
        return atan2 < 0 ? 2 * Math.PI + atan2 : atan2;
    }

    public static void clearAccelerations(PlanetState[] states) {
        for (PlanetState state : states) {
            state.clearAcceleration();
        }
    }
}
