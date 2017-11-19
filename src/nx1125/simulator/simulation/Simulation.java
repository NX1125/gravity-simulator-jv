package nx1125.simulator.simulation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A gravity is a process that compute successive states of each planet in a universe
 * which is the simulated universe. The simulated universe, which is called as gravity,
 * contains properties and actions that changes the planet state. Actions are forces or
 * algorithms that occurs as an interaction between two planets. The interaction depends
 * on the gravity properties that are constants like gravity constant that affects the
 * gravity force. The interaction also depends of the planet properties.
 * <p>
 * The force is used to affect the acceleration of a planet, which the acceleration is used
 * to affect the velocity of the planet, which the velocity is used to affect the position
 * of the planet. After all these properties are updated, gravity advances in time, by
 * {@link #getTimeInterval()} as a interval, and the next step, the calculation part start
 * again.
 * <p>
 * Created by guilh on 28/09/2017.
 */

public abstract class Simulation implements Serializable, Cloneable {

    private final List<Planet> mPlanetList = new ArrayList<>();
    /**
     * The number of frames to show in one second or one cycle.
     */
    private int mFrameRate;
    /**
     * The total of calculations to be done in this gravity. Each calculation, the gravity
     * advances mTimeInterval in time dimension.
     */
    private int mStatesCount = 60000; // Default value as 60 cycles of gravity
    /**
     * The number of calculations by one frame rate cycle. The frame rate cycle is the cycle of one
     * second. The time equivalent to one cycle is mStatesByCycle * mTimeInterval.
     */
    private int mStatesByCycle = 1000;
    /**
     * The simulation time of one state. This is used to determine how much time in the
     * simulation is equivalent to one second. The term <code>cycle</code> is used to
     * represent the time interval of one second.
     */
    private double mTimeInterval = 0.1 / mStatesByCycle;

    public Simulation() {
        this(24);
    }

    public Simulation(int frameRate) {
        mFrameRate = frameRate;
    }

    public void addPlanet(Planet planet) {
        mPlanetList.add(planet);
    }

    public void removePlanet(Planet planet) {
        mPlanetList.remove(planet);
    }

    public void removePlanet(int index) {
        mPlanetList.remove(index);
    }

    public Planet[] getPlanets() {
        return mPlanetList.toArray(new Planet[mPlanetList.size()]);
    }

    public Planet getPlanet(int index) {
        return mPlanetList.get(index);
    }

    /**
     * Return the time interval that the gravity advances in each calculation. The gravity
     * time by one cycle, or time density, can be computed with {@link #getTimeDenstity()}.
     */
    public double getTimeInterval() {
        return mTimeInterval;
    }

    public void setTimeInterval(double timeInterval) {
        mTimeInterval = timeInterval;
    }

    /**
     * Return the gravity time that fits in one cycle or one second. This is the same as the
     * product between the {@linkplain #getTimeInterval() time interval} and how many
     * {@linkplain #getStatesPerCycle() calculations by cycle}.
     */
    public double getTimeDenstity() {
        return mTimeInterval * mStatesByCycle;
    }

    /**
     * Return how many calculations the gravity must do
     */
    public int getStatesCount() {
        return mStatesCount;
    }

    public void setStatesCount(int statesCount) {
        mStatesCount = statesCount;
    }

    /**
     * Return the count of calculations that fits in one cycle. This is a frame rate property.
     */
    public int getStatesPerCycle() {
        return mStatesByCycle;
    }

    public void setStatesByCycle(int statesByCycle) {
        mStatesByCycle = statesByCycle;
    }

    public int getFrameRate() {
        return mFrameRate;
    }

    public void setFrameRate(int frameRate) {
        mFrameRate = frameRate;
    }

    public abstract Simulator createSimulator();

    @Override
    public String toString() {
        return "Simulation{" +
                ", timeInterval=" + mTimeInterval +
                ", calculationCount=" + mStatesCount +
                ", calculationsByCycle=" + mStatesByCycle +
                '}';
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }
}
