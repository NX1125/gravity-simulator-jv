package nx1125.simulator.simulation;

public abstract class DefaultSimulator extends Thread implements Simulator {

    private static final String TAG = "SimulatorThread";

    private static final boolean DEBUG = true;

    private final int mPlanetCount;

    private final int mStepCount;

    private final Planet[] planets;

    private final Simulation mSimulation;
    private final CubicFunction mCubicFunction = new CubicFunction();
    private final QuadraticFunction mQuadraticFunction = new QuadraticFunction();
    private PlanetState[][] mPlanetStates;
    private PlanetState[] mActualStep;
    private int mStepIndex;

    private int mDoneState;

    private Simulator.OnSimulatorEventListener mOnSimulatorEventListener;

    private double[] mCachedCollisionRootsArray;

    public DefaultSimulator(Simulation simulation) {
        super("SimulatorThread");

        mStepCount = simulation.getStatesCount();

        planets = simulation.getPlanets();
        mPlanetCount = planets.length;
        mSimulation = simulation;
    }

    @Override
    public void run() {
        if (mOnSimulatorEventListener == null) {
            throw new IllegalStateException("There is no OnSimulatorEventListener for the simulation. Terminating it.");
        }

        info("Starting simulator to compute " + mStepCount + " steps with " + mPlanetCount + " planets");

        debug("Initialing caches");
        onCreateCache();

        mActualStep = mPlanetStates[0];

        debug("Creating initial states");
        onCreateInitialStates();

        mOnSimulatorEventListener.onSimulationStarted(this);

        final double timeInterval = mSimulation.getTimeInterval() / mSimulation.getStatesPerCycle();

        int count = mStepCount - 1;

        mDoneState = -1;
        mActualStep = mPlanetStates[0];

        CollisionResult result = new CollisionResult();

        int statesPerCycle = mSimulation.getStatesPerCycle();

        debug("doInBackground: Starting simulation");

        long initialTime = System.currentTimeMillis();

        if (isCollisionEnabled()) {
            for (mStepIndex = 0; mStepIndex < count && !Thread.interrupted(); ) {
                double remainingTime = timeInterval;

                computeAccelerations(planets, mActualStep);

                nextStep();
                copyFromLastState();

                mDoneState++;

                // deliver last step after copy, so it won't be used anymore
                mOnSimulatorEventListener.onSimulationProgressUpdated(this, mDoneState);

                while (checkCollisions(result, remainingTime, mPlanetCount, planets, mActualStep) && !isInterrupted()) {
                    advance(result.getCollisionTime());
                    remainingTime -= result.getCollisionTime();

                    collide(result, mActualStep);

                    result.clear();
                }

                advance(remainingTime);
            }
        } else {
            // same loop as the true if case, but without the collision part
            long lastCycleTime = getTime();

            for (mStepIndex = 0; mStepIndex < count && !Thread.interrupted(); ) {
                computeAccelerations(planets, mActualStep);

                nextStep();
                copyFromLastState();

                mDoneState++;

                if (DEBUG && (mDoneState % statesPerCycle) == 0) {
                    debug("A new cycle is complete: " + (mDoneState / statesPerCycle) + " with " + (getTime() - lastCycleTime) + " milliseconds");
                    lastCycleTime = getTime();
                }

                // deliver last step after copy, so it won't be used anymore
                mOnSimulatorEventListener.onSimulationProgressUpdated(this, mDoneState);

                advance(timeInterval);
            }
        }

        // compute accelerations for last step which is outside the loop because there is no need
        // to check for collisions as it is done in the loop
        computeAccelerations(planets, mActualStep);

        if (mStepIndex < count) {
            info("Simulator interrupted at step " + mDoneState);
        }

        info("End of simulation. Took " + (getTime() - initialTime) + " milliseconds to complete");

        debug("doInBackground: Creating simulation results and delivering it");

        mOnSimulatorEventListener.onSimulationFinish(this);
    }

    private long getTime() {
        return System.currentTimeMillis();
    }

    protected Planet getPlanet(int index) {
        return planets[index];
    }

    public int getPlanetCount() {
        return mPlanetCount;
    }

    public abstract boolean isCollisionEnabled();

    @Override
    public void setOnSimulationEventListener(Simulator.OnSimulatorEventListener l) {
        mOnSimulatorEventListener = l;
    }

    public PlanetState[][] getPlanetStates() {
        return mPlanetStates;
    }

    private void nextStep() {
        mStepIndex++;
        mActualStep = mPlanetStates[mStepIndex];
    }

    protected void onCreateCache() {
        mPlanetStates = new PlanetState[mStepCount][mPlanetCount];

        // a polynomial with 4 as the highest power, has four solutions
        mCachedCollisionRootsArray = new double[4];
    }

    protected void onCreateInitialStates() {
        PlanetState[] states = mPlanetStates[0];

        for (int i = 0; i < mPlanetCount; i++) {
            states[i] = new PlanetState(planets[i]);
        }
    }

    public abstract void computeAccelerations(Planet[] planets, PlanetState[] actualStates);

    /**
     * Can be called only when {@link #mStepIndex} is higher than 0.
     */
    private void copyFromLastState() {
        PlanetState[] last = mPlanetStates[mStepIndex - 1];
        for (int i = 0; i < mPlanetCount; i++) {
            // TODO: 01/10/2017 Is better clone or copy constructor?
            mActualStep[i] = last[i].clone();
        }
    }

    @Override
    public int getStatesPerCycle() {
        return mSimulation.getStatesPerCycle();
    }

    /**
     * Check for each planet if it collides with another planet. If no planet collides, then
     * {@code null} is returned.
     */
    public boolean checkCollisions(CollisionResult result, double remainingTime,
                                   int planetCount, Planet[] planets, PlanetState[] actualStates) {
        boolean found = false;

        double smallestTime = remainingTime;

        for (int i = 0; i < planetCount && !isInterrupted(); i++) {
            PlanetState s0 = actualStates[i];
            Planet p0 = planets[i];

            double r0 = p0.getRadius();

            for (int j = i + 1; j < planetCount && !isInterrupted(); j++) {
                PlanetState s1 = actualStates[j];
                Planet p1 = planets[j];

                double dx = s1.x - s0.x;
                double dy = s1.y - s0.y;

                double dvx = s1.vx - s0.vx;
                double dvy = s1.vy - s0.vy;

                double dax = s1.ax - s0.ax;
                double day = s1.ay - s0.ay;

                double r = p1.getRadius() + r0;

                int count = solveQuadratic(
                        (dax * dax + day * day) * 0.25,
                        dax * dvx + day * dvy,
                        dax * dx + day * dy + dvx * dvx + dvy * dvy,
                        2.0 * (dvx * dx + dvy * dy),
                        dx * dx + dy * dy - r * r,
                        mCachedCollisionRootsArray
                );

                for (int k = 0; k < count; k++) {
                    double t = mCachedCollisionRootsArray[k];
                    if (t >= 0.0 && t <= smallestTime) {
                        smallestTime = t;
                        result.setCollision(p0, s0, p1, s1, t);
                        result.setIndexes(i, j);
                        found = true;
                    }
                }
            }
        }

        return found;
    }

    public void collide(CollisionResult result, PlanetState[] actualStates) {
        // get the reference in array instead of CollisionResult so it can save the state for later
        PlanetState s0 = actualStates[result.getIndex0()];
        PlanetState s1 = actualStates[result.getIndex1()];

        double m0 = result.getPlanet0().getMass();
        double m1 = result.getPlanet1().getMass();

        double dx = s1.x - s0.x;
        double dy = s1.y - s0.y;

        double ir = 1.0 / Math.hypot(dx, dy);

        dx *= ir;
        dy *= ir;

        double u0 = s0.vx * dx + s0.vy * dy;
        double u1 = s1.vx * dx + s1.vy * dy;

        double u0f = (2.0 * m1 * u1 + u0 * (m0 - m1)) / (m1 + m0);
        double u1f = u0 + u0f - u1;

        s0.vx += dx * (u0f - u0);
        s0.vy += dy * (u0f - u0);

        s1.vx += dx * (u1f - u1);
        s1.vy += dy * (u1f - u1);
    }

    private int solveCubic(double a, double b, double c, double d, double[] roots) {
        if (a == 0) {
            return solveSquare(b, c, d, roots);
        }

        if (d == 0) {
            // one of the roots is zero

            int count = solveSquare(a, b, c, roots);
            roots[count] = 0.0;

            return count + 1;
        }

        mCubicFunction.setEquation(a, b, c, d);

        double x = 0;
        do {
            x = newtonRaphson(mCubicFunction, x + 1, Function.DEFAULT_LOOP_LIMIT, Function.DEFAULT_TOLERANCE);
        } while (mCubicFunction.getState() == Function.STATE_ZERO_DERIVATIVE);

        if (mCubicFunction.getState() == Function.STATE_COMPUTED) {
            int count = solveSquare(a, b + a * x, -d / x, roots);
            roots[count] = x;
            return count + 1;
        }

        return 0;
    }

    private int solveQuadratic(double a, double b, double c, double d, double e, double[] roots) {
        if (a == 0) {
            return solveCubic(b, c, d, e, roots);
        }

        if (e == 0) {
            // one of the roots is zero
            int count = solveCubic(a, b, c, d, roots);
            roots[count] = 0.0;
            return count + 1;
        }

        int count = solveCubic(4 * a, 3 * b, 2 * c, d, roots);
        boolean positiveA = a > 0.0;
        for (int i = 0; i < count; i++) {
            double x = roots[i];
            if (positiveA != ((((a * x + b) * x + c) * x + d) * x + e > 0.0)) {
                mQuadraticFunction.setEquation(a, b, c, d, e);

                x = 0;
                do {
                    x = newtonRaphson(mQuadraticFunction, x + 1, Function.DEFAULT_LOOP_LIMIT, Function.DEFAULT_TOLERANCE);
                } while (mQuadraticFunction.getState() == Function.STATE_ZERO_DERIVATIVE);

                if (mQuadraticFunction.getState() == Function.STATE_COMPUTED) {
                    double t = b + a * x;

                    count = solveCubic(a, b + a * x, c + t * x, -e / x, roots);
                    roots[count] = x;

                    return count + 1;
                } else {
                    break;
                }
            }
        }

        return 0;
    }

    private double newtonRaphson(Function function, double x, int loopLimit, double tolerance) {
        while (loopLimit-- > 0 && !isInterrupted()) {
            function.solve(x);
            double dy = function.getValueDerivative();
            if (dy == 0) {
                function.setState(Function.STATE_ZERO_DERIVATIVE);
                return x;
            }
            double y = function.getSolvedFunctionValue();

            if (-tolerance <= y && y <= tolerance) {
                function.setState(Function.STATE_COMPUTED);
                return x;
            }

            x -= y / dy;
        }

        function.setState(Function.STATE_LOOP_LIMIT_REACHED);

        return x;
    }

    private void advance(double time) {
        SimulationUtils.advance(mPlanetCount, time, mActualStep);
    }

    @Override
    public Simulation getSimulation() {
        return mSimulation;
    }

    @Override
    public PlanetState[] getPlanetStates(int state) {
        return mPlanetStates[state];
    }

    @Override
    public boolean isStateDone(int state) {
        return state < mDoneState;
    }

    @Override
    public int getLastAvailableState() {
        return mDoneState;
    }

    @Override
    public Planet[] getPlanets() {
        return planets;
    }

    private static int solveSquare(double a, double b, double c, double[] roots) {
        if (a == 0) {
            if (b == 0) return 0;
            // linear equation: bx+c = 0 -> x = -c / b
            roots[0] = -c / b;
            return 1;
        }

        double d = b * b - 4.0 * a * c;

        if (d == 0) {
            roots[0] = -b / (2.0 * a);
            return 1;
        }
        if (d < 0) return 0;

        a = 0.5 / a;
        d = Math.sqrt(d);

        roots[0] = (-b - d) * a;
        roots[1] = (-b + d) * a;

        return 2;
    }

    private static void debug(String msg) {
        System.out.println(TAG + "/D " + msg);
    }

    private static void info(String msg) {
        System.out.println(TAG + "/I " + msg);
    }

    private static abstract class Function {

        static final double DEFAULT_TOLERANCE = 1.0E-8;
        static final int DEFAULT_LOOP_LIMIT = 10000;

        static final int STATE_NOT_COMPUTED = -1;

        static final int STATE_COMPUTED = 0;
        static final int STATE_LOOP_LIMIT_REACHED = 1;
        static final int STATE_ZERO_DERIVATIVE = 2;
        double solvedFunctionValue;
        double valueDerivative;
        private int mState = STATE_NOT_COMPUTED;

        public abstract void solve(double x);

        double getValueDerivative() {
            return valueDerivative;
        }

        double getSolvedFunctionValue() {
            return solvedFunctionValue;
        }

        int getState() {
            return mState;
        }

        void setState(int state) {
            mState = state;
        }
    }

    private static class CubicFunction extends Function {

        private double a;
        private double b;
        private double c;
        private double d;

        private double da;
        private double db;

        CubicFunction() {
        }

        @Override
        public void solve(double x) {
            solvedFunctionValue = ((a * x + b) * x + c) * x + d;
            valueDerivative = (da * x + db) * x + c;
        }

        void setEquation(double a, double b, double c, double d) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;

            da = 3.0 * a;
            db = 2.0 * b;
        }
    }

    private static class QuadraticFunction extends Function {

        private double a;
        private double b;
        private double c;
        private double d;
        private double e;

        private double da;
        private double db;
        private double dc;

        QuadraticFunction() {
        }

        @Override
        public void solve(double x) {
            solvedFunctionValue = (((a * x + b) * x + c) * x + d) * x + e;
            valueDerivative = ((da * x + db) * x + dc) * x + d;
        }

        public void setEquation(double a, double b, double c, double d, double e) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
            this.e = e;

            da = 4.0 * a;
            db = 3.0 * b;
            dc = 2.0 * c;
        }
    }
}
